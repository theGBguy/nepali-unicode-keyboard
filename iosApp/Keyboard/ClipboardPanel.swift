import SwiftUI
import NepaliKeyboardShared

/**
 * The clipboard panel, drawn in place of the strip and keys at the same height (like the emoji
 * panel). As on Android: a "Clipboard" title, then two columns of tiles, each as tall as its text
 * (up to 4 lines): the phone's current copy first, then pinned items, then recent ones. Tap
 * pastes; long-press shows pin and delete on that tile. The bottom row has ABC, Clear all (tap
 * twice; pinned items stay) and backspace.
 *
 * Without Full Access the keyboard can't see the pasteboard, so the panel only says where to turn it on.
 */
struct ClipboardPanel: View {
    @ObservedObject var model: KeyboardModel
    let state: KeyboardState
    let colors: KeyboardColors
    let height: CGFloat
    let keyHeight: CGFloat

    /** The tile showing pin and delete. */
    @State private var actionsFor: String?
    @State private var confirmClear = false
    @State private var confirmToken = 0

    private struct Tile: Identifiable {
        let text: String
        let label: String
        let muted: Bool
        let pinned: Bool
        /** Saved in the history, so it can be pinned or deleted. */
        let saved: Bool
        var id: String { text }
    }

    private var tiles: [Tile] {
        let items = state.clipItems
        var list: [Tile] = []
        if let copied = state.copied {
            let saved = items.first { $0.text == copied.text }
            list.append(Tile(
                text: copied.text,
                label: copied.sensitive ? String(localized: "Hidden: marked private by the app it came from") : copied.text,
                muted: copied.sensitive,
                pinned: saved?.pinned == true,
                saved: saved != nil
            ))
        }
        let others = items.filter { $0.text != state.copied?.text }
        for item in others.filter(\.pinned) + others.filter({ !$0.pinned }) {
            list.append(Tile(text: item.text, label: item.text, muted: false, pinned: item.pinned, saved: true))
        }
        return list
    }

    var body: some View {
        let tiles = tiles
        VStack(alignment: .leading, spacing: 0) {
            Text("Clipboard")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(colors.keyText)
                .padding(.leading, 12)
                .padding(.top, 12)
                .padding(.bottom, 10)

            if !model.hasFullAccess || tiles.isEmpty {
                Text(model.hasFullAccess ? "Copy text in any app and it shows here." : "Turn on Full Access in the Nepali Keyboard app")
                    .font(.system(size: 14))
                    .foregroundStyle(colors.hint)
                    .multilineTextAlignment(.center)
                    .padding(24)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    columns(tiles)
                        .padding(.horizontal, 4)
                        .padding(.bottom, 4)
                }
            }

            bottomRow(tiles)
                .padding(.top, 4)
        }
        .frame(height: height)
        .task(id: confirmToken) {
            // "Tap again to clear" goes back after three seconds
            guard confirmClear else { return }
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            if !Task.isCancelled { confirmClear = false }
        }
    }

    /**
     * A staggered grid: each tile goes to the shorter column, by its estimated line count. Plain
     * stacks rather than lazy ones: at most about a hundred short tiles.
     */
    private func columns(_ tiles: [Tile]) -> some View {
        var left: [Tile] = []
        var right: [Tile] = []
        var leftHeight = 0
        var rightHeight = 0
        for tile in tiles {
            let lines = estimatedLines(tile.label) + (actionsFor == tile.text ? 2 : 0)
            if leftHeight <= rightHeight {
                left.append(tile)
                leftHeight += lines + 1
            } else {
                right.append(tile)
                rightHeight += lines + 1
            }
        }
        return HStack(alignment: .top, spacing: 6) {
            column(left)
            column(right)
        }
    }

    private func column(_ tiles: [Tile]) -> some View {
        VStack(spacing: 6) {
            ForEach(tiles) { tile in
                tileView(tile)
            }
        }
        .frame(maxWidth: .infinity, alignment: .top)
    }

    private func estimatedLines(_ text: String) -> Int {
        let lines = text.split(separator: "\n", omittingEmptySubsequences: false)
            .reduce(0) { $0 + max(1, ($1.count + 17) / 18) }
        return min(max(lines, 1), 4)
    }

    private func tileView(_ tile: Tile) -> some View {
        let showActions = actionsFor == tile.text && tile.saved
        let pinsFull = ClipboardHistory.shared.pinnedCount(items: state.clipItems) >= ClipboardHistory.shared.MAX_PINNED
        return VStack(spacing: 0) {
            HStack(alignment: .top, spacing: 4) {
                Text(tile.label)
                    .font(.system(size: 14))
                    .lineSpacing(2)
                    .foregroundStyle(tile.muted ? colors.hint : colors.keyText)
                    .lineLimit(4)
                    .truncationMode(.tail)
                    .frame(maxWidth: .infinity, alignment: .leading)
                if tile.pinned && !showActions {
                    Image(systemName: "pin.fill")
                        .font(.system(size: 11))
                        .foregroundStyle(colors.hint)
                        .padding(.top, 2)
                        .accessibilityLabel(Text("Pinned"))
                }
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .frame(minHeight: 44)

            if showActions {
                HStack(spacing: 0) {
                    Spacer(minLength: 0)
                    action(tile.pinned ? "pin.slash" : "pin",
                           label: tile.pinned ? String(localized: "Unpin") : String(localized: "Pin"),
                           tint: tile.pinned ? colors.accent : colors.keyText,
                           enabled: tile.pinned || !pinsFull) {
                        model.pinClip(tile.text, pinned: !tile.pinned)
                        actionsFor = nil
                    }
                    action("trash", label: String(localized: "Delete"), tint: colors.keyText, enabled: true) {
                        model.deleteClip(tile.text)
                        actionsFor = nil
                    }
                }
                .padding(.trailing, 2)
                .padding(.bottom, 2)
            }
        }
        .background(RoundedRectangle(cornerRadius: 8).fill(colors.key))
        .contentShape(RoundedRectangle(cornerRadius: 8))
        .onTapGesture {
            model.keyDown()
            actionsFor = nil
            model.pasteClip(tile.text)
        }
        .onLongPressGesture(minimumDuration: 0.4) {
            guard tile.saved else { return }
            model.keyDown()
            actionsFor = tile.text
        }
        .accessibilityAddTraits(.isButton)
        .accessibilityHint(Text("Paste"))
    }

    private func action(_ symbol: String, label: String, tint: Color, enabled: Bool, perform: @escaping () -> Void) -> some View {
        Button(action: perform) {
            Image(systemName: symbol)
                .font(.system(size: 17))
                .foregroundStyle(enabled ? tint : colors.divider)
                .frame(width: 40, height: 40)
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityLabel(Text(label))
    }

    private func bottomRow(_ tiles: [Tile]) -> some View {
        // Shown while anything would go: unpinned history or an unpinned current copy
        let canClear = model.hasFullAccess && tiles.contains { !$0.pinned }
        return HStack(spacing: 4) {
            Button { model.closeClipboard() } label: {
                RoundedRectangle(cornerRadius: KeyboardMetrics.keyRadius)
                    .fill(colors.functionKey)
                    .overlay(Text(verbatim: "ABC").font(.system(size: 15)).foregroundStyle(colors.keyText))
                    .shadow(color: colors.keyShadow, radius: 0, x: 0, y: 0.5)
            }
            .buttonStyle(.plain)
            .frame(width: 52)
            .accessibilityLabel(Text("Letters"))

            Spacer(minLength: 0)

            if canClear {
                Button {
                    model.keyDown()
                    if confirmClear {
                        confirmClear = false
                        actionsFor = nil
                        model.clearClips()
                    } else {
                        confirmClear = true
                        confirmToken += 1
                    }
                } label: {
                    Text(confirmClear ? "Tap again to clear" : "Clear all")
                        .font(.system(size: 14))
                        .foregroundStyle(confirmClear ? colors.accent : colors.keyText)
                        .padding(.horizontal, 12)
                        .frame(maxHeight: .infinity)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }

            if let backspace = state.currentLayout.rows.flatMap({ $0 }).first(where: { $0.primary == "⌫" }) {
                KeyView(model: model, key: backspace, state: state, colors: colors, height: keyHeight)
                    .frame(width: 52)
            }
        }
        .frame(height: keyHeight)
    }
}
