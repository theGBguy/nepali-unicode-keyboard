import SwiftUI
import NepaliKeyboardShared

/**
 * The emoji panel, drawn in place of the strip and keys at the same height so the keyboard
 * doesn't jump. One scrolling grid ([EmojiGridView]) with a label per category, Recent first once
 * there are any.
 * The bottom row has ABC, a tab per category (follows the scroll, tap to jump) and the layout's
 * own backspace key (deletes on press, repeats while held).
 */
struct EmojiPanel: View {
    @ObservedObject var model: KeyboardModel
    @ObservedObject private var catalog = EmojiCatalog.shared
    let state: KeyboardState
    let colors: KeyboardColors
    let height: CGFloat
    let keyHeight: CGFloat

    /** The section at the top of the grid, for the highlighted tab. */
    @State private var topSection = 0
    @State private var jump: EmojiGridView.Jump?

    private struct Section: Identifiable {
        let category: EmojiCategory
        let emoji: [String]
        var id: String { category.key }
    }

    private var sections: [Section] {
        var list: [Section] = []
        // The recents the panel opened with: they don't move under the finger while typing
        if !state.emojiRecents.isEmpty { list.append(Section(category: .recent, emoji: state.emojiRecents)) }
        list += catalog.sections.map { Section(category: $0.category, emoji: $0.emoji) }
        return list
    }

    var body: some View {
        let sections = sections
        VStack(spacing: 0) {
            EmojiGridView(
                sections: sections.map { .init(id: $0.id, title: label($0.category), emoji: $0.emoji) },
                labelColor: UIColor(colors.hint),
                pressedColor: UIColor(colors.keyText).withAlphaComponent(0.12),
                jump: jump,
                onPress: { model.keyDown() },
                onEmoji: { model.emoji($0) },
                onTopSection: { topSection = $0 }
            )
            .onAppear { catalog.loadIfNeeded() }

            bottomRow(sections)
                .padding(.top, 4)
        }
        .frame(height: height)
    }

    private func bottomRow(_ sections: [Section]) -> some View {
        HStack(spacing: 4) {
            Button { model.closeEmoji() } label: {
                RoundedRectangle(cornerRadius: KeyboardMetrics.keyRadius)
                    .fill(colors.functionKey)
                    .overlay(Text(verbatim: "ABC").font(.system(size: 15)).foregroundStyle(colors.keyText))
                    .shadow(color: colors.keyShadow, radius: 0, x: 0, y: 0.5)
            }
            .buttonStyle(.plain)
            .frame(width: 52)
            .accessibilityLabel(Text("Letters"))

            // Ten tabs with Recent: each gets an equal share, the highlight shrinks to fit it
            GeometryReader { geometry in
                let tabWidth = geometry.size.width / CGFloat(max(sections.count, 1))
                let circle = min(32, tabWidth - 2)
                HStack(spacing: 0) {
                    ForEach(Array(sections.enumerated()), id: \.element.id) { index, section in
                        let selected = index == topSection
                        Button {
                            model.keyDown()
                            jump = EmojiGridView.Jump(section: index, token: (jump?.token ?? 0) + 1)
                            topSection = index
                        } label: {
                            Image(systemName: icon(section.category))
                                .font(.system(size: 16))
                                .foregroundStyle(selected ? colors.keyText : colors.hint)
                                .frame(width: circle, height: circle)
                                .background(Circle().fill(selected ? colors.functionKey : .clear))
                                .frame(width: tabWidth, height: geometry.size.height)
                                .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(Text(label(section.category)))
                    }
                }
            }

            if let backspace = state.currentLayout.rows.flatMap({ $0 }).first(where: { $0.primary == "⌫" }) {
                KeyView(model: model, key: backspace, state: state, colors: colors, height: keyHeight)
                    .frame(width: 52)
            }
        }
        .frame(height: keyHeight)
    }

    private func label(_ category: EmojiCategory) -> String {
        switch category {
        case .recent: return String(localized: "Recent")
        case .smileys: return String(localized: "Smileys and people")
        case .animals: return String(localized: "Animals and nature")
        case .food: return String(localized: "Food and drink")
        case .activities: return String(localized: "Activities")
        case .travel: return String(localized: "Travel and places")
        case .objects: return String(localized: "Objects")
        case .symbols: return String(localized: "Symbols")
        default: return String(localized: "Flags")
        }
    }

    private func icon(_ category: EmojiCategory) -> String {
        switch category {
        case .recent: return "clock"
        case .smileys: return "face.smiling"
        case .animals: return "pawprint"
        case .food: return "fork.knife"
        case .activities: return "soccerball"
        case .travel: return "car"
        case .objects: return "lightbulb"
        case .symbols: return "heart"
        default: return "flag"
        }
    }
}
