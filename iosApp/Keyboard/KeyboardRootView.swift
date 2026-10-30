import SwiftUI
import UIKit
import NepaliKeyboardShared

/** One key as the row draws it: a key from the shared layout, or iOS's next-keyboard key. */
enum RowKey: Identifiable {
    case key(KeyData, flex: CGFloat, index: Int)
    case nextKeyboard(flex: CGFloat)

    var id: String {
        switch self {
        case let .key(key, _, index): return "\(index):\(key.primary)"
        case .nextKeyboard: return "next-keyboard"
        }
    }

    var flex: CGFloat {
        switch self {
        case let .key(_, flex, _): return flex
        case let .nextKeyboard(flex): return flex
        }
    }
}

/** The keyboard: suggestion strip, then the key rows (or the emoji panel in their place). Colours follow light and dark mode. */
struct KeyboardRootView: View {
    @ObservedObject var model: KeyboardModel
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        let state = model.state
        let colors = KeyboardColors.of(scheme)
        let keyHeight = KeyboardMetrics.keyHeight(state)

        ZStack(alignment: .bottomLeading) {
            VStack(spacing: 0) {
                if state.showEmoji {
                    // In place of the strip and rows, at their height
                    EmojiPanel(model: model, state: state, colors: colors,
                               height: KeyboardMetrics.panelHeight(of: state), keyHeight: keyHeight)
                } else if state.showClipboard {
                    ClipboardPanel(model: model, state: state, colors: colors,
                                   height: KeyboardMetrics.panelHeight(of: state), keyHeight: keyHeight)
                } else {
                    KeyboardStripView(model: model, state: state, colors: colors)
                        .frame(height: KeyboardMetrics.stripHeight)
                    VStack(spacing: KeyboardMetrics.rowGap) {
                        ForEach(Array(state.currentLayout.rows.enumerated()), id: \.offset) { _, row in
                            KeyRow(model: model, state: state, row: row, colors: colors, keyHeight: keyHeight)
                        }
                    }
                }
            }
            .padding(.bottom, KeyboardMetrics.bottomPadding + KeyboardMetrics.rowGap)
            .padding(.horizontal, KeyboardMetrics.sidePadding)

            if state.showLanguageMenu {
                LanguageMenu(model: model, state: state, colors: colors)
                    .padding(.leading, 8)
                    .padding(.bottom, keyHeight + KeyboardMetrics.rowGap + 8 + KeyboardMetrics.bottomPadding)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
        .background(colors.background)
    }
}

private struct KeyRow: View {
    @ObservedObject var model: KeyboardModel
    let state: KeyboardState
    let row: [KeyData]
    let colors: KeyboardColors
    let keyHeight: CGFloat

    var body: some View {
        let keys = rowKeys()
        let allCharacters = row.allSatisfy { $0.keyType == .character }
        // Short letter rows (the 9-key home row) are indented by half a key on each side
        let flexSum = keys.reduce(CGFloat(0)) { $0 + $1.flex }
        let indent = allCharacters ? max((10 - flexSum) / 2, 0) : 0
        let height = KeyboardMetrics.rowHeight(row, keyHeight: keyHeight)

        GeometryReader { geometry in
            let gaps = KeyboardMetrics.keyGap * CGFloat(keys.count - 1 + (indent > 0 ? 2 : 0))
            let unit = (geometry.size.width - gaps) / (flexSum + indent * 2)
            HStack(spacing: KeyboardMetrics.keyGap) {
                if indent > 0 { Color.clear.frame(width: unit * indent) }
                ForEach(keys) { rowKey in
                    switch rowKey {
                    case let .key(key, flex, _):
                        KeyView(model: model, key: key, state: state, colors: colors, height: height)
                            .frame(width: unit * flex, height: height)
                    case let .nextKeyboard(flex):
                        NextKeyboardKey(colors: colors, height: height) { model.nextKeyboard() }
                            .frame(width: unit * flex, height: height)
                    }
                }
                if indent > 0 { Color.clear.frame(width: unit * indent) }
            }
        }
        .frame(height: height)
    }

    /** The row's keys, plus iOS's next-keyboard key after the first bottom-row key when iOS asks for one. */
    private func rowKeys() -> [RowKey] {
        var keys = row.enumerated().map { RowKey.key($1, flex: CGFloat($1.flex), index: $0) }
        let spaceIndex = row.firstIndex { $0.primary == " " }
        if model.needsInputModeSwitchKey, let spaceIndex {
            let space = row[spaceIndex]
            keys[spaceIndex] = .key(space, flex: CGFloat(space.flex) - 1, index: spaceIndex)
            keys.insert(.nextKeyboard(flex: 1), at: 1)
        }
        return keys
    }
}

private struct NextKeyboardKey: View {
    let colors: KeyboardColors
    let height: CGFloat
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            RoundedRectangle(cornerRadius: KeyboardMetrics.keyRadius)
                .fill(colors.functionKey)
                .overlay(Image(systemName: "globe").font(.system(size: 18)).foregroundStyle(colors.keyText))
                .shadow(color: colors.keyShadow, radius: 0, x: 0, y: 0.5)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text("Switch keyboard"))
    }
}

/**
 * Long-press menu on the language key, drawn inside the keyboard above the bottom row: choose how
 * to type Nepali, or go to the next keyboard. Tapping anywhere else closes it.
 */
private struct LanguageMenu: View {
    @ObservedObject var model: KeyboardModel
    let state: KeyboardState
    let colors: KeyboardColors

    var body: some View {
        ZStack(alignment: .bottomLeading) {
            Color.black.opacity(0.001)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .onTapGesture { model.controller.dismissLanguageMenu() }
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 0) {
                row(String(localized: "Romanized"), checked: state.nepaliStyle == .transliteration,
                    enabled: state.field.allowsTransliteration) {
                    model.controller.setNepaliStyle(style: .transliteration)
                }
                row(String(localized: "Traditional layout"), checked: state.nepaliStyle == .nepali, enabled: true) {
                    model.controller.setNepaliStyle(style: .nepali)
                }
                colors.divider.frame(height: 1).padding(.vertical, 4)
                row(String(localized: "Switch keyboard"), icon: "keyboard", checked: false, enabled: true) {
                    model.controller.switchKeyboard()
                }
            }
            .padding(.vertical, 6)
            .frame(width: 232)
            .background(RoundedRectangle(cornerRadius: 10).fill(colors.preview).shadow(radius: 8, y: 2))
            .padding(.leading, 8)
            .padding(.bottom, 0)
        }
        // VoiceOver stays in the menu until a choice or the escape gesture closes it
        .accessibilityElement(children: .contain)
        .accessibilityAddTraits(.isModal)
        .accessibilityAction(.escape) { model.controller.dismissLanguageMenu() }
        .onAppear { UIAccessibility.post(notification: .screenChanged, argument: nil) }
    }

    private func row(_ label: String, icon: String? = nil, checked: Bool, enabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Group {
                    if let icon {
                        Image(systemName: icon).foregroundStyle(colors.keyText)
                    } else if checked {
                        Image(systemName: "checkmark").foregroundStyle(colors.accent)
                    } else {
                        Color.clear
                    }
                }
                .font(.system(size: 16, weight: .medium))
                .frame(width: 22, height: 22)
                Text(label)
                    .font(.system(size: 15, weight: checked ? .medium : .regular))
                    .foregroundStyle(enabled ? colors.keyText : colors.hint)
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 14)
            .frame(height: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .disabled(!enabled)
        .accessibilityLabel(Text(label))
        .accessibilityAddTraits(checked ? .isSelected : [])
    }
}
