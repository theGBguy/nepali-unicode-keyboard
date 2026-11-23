import SwiftUI
import NepaliKeyboardShared

/** The rounded search field: a magnifier, then [content]. */
struct EmojiSearchPill<Content: View>: View {
    let colors: KeyboardColors
    @ViewBuilder let content: () -> Content

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(colors.hint)
            content()
            Spacer(minLength: 0)
        }
        .padding(.horizontal, 12)
        .frame(height: 34)
        .background(Capsule().fill(colors.key))
    }
}

/**
 * Emoji search in place of the strip, like Android: back to the grid, then the box with what's
 * typed (the Romanized word being typed underlined) and a caret.
 */
struct EmojiSearchBar: View {
    @ObservedObject var model: KeyboardModel
    let search: EmojiSearch
    let colors: KeyboardColors

    var body: some View {
        HStack(spacing: 0) {
            Button { model.closeEmojiSearch() } label: {
                Image(systemName: "chevron.backward")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(colors.keyText)
                    .frame(width: 40, height: 40)
                    .contentShape(Rectangle())
            }
            .accessibilityLabel(Text("Back to emoji"))
            EmojiSearchPill(colors: colors) {
                HStack(spacing: 1) {
                    if search.text.isEmpty && search.composing.isEmpty {
                        Text("Search emoji").foregroundStyle(colors.hint)
                    } else {
                        (Text(search.text) + Text(search.composing).underline())
                            .foregroundStyle(colors.keyText)
                            .lineLimit(1)
                            .truncationMode(.head)
                    }
                    Rectangle().fill(colors.accent).frame(width: 1.5, height: 18)
                }
                .font(.system(size: 15))
            }
            .accessibilityElement(children: .combine)
            .padding(.trailing, 4)
        }
    }
}

/** What search found, in one scrolling row in place of the number row; a tap types the emoji. */
struct EmojiResultsRow: View {
    @ObservedObject var model: KeyboardModel
    let search: EmojiSearch
    let colors: KeyboardColors

    var body: some View {
        if search.results.isEmpty {
            HStack {
                if !search.text.trimmingCharacters(in: .whitespaces).isEmpty || !search.composing.isEmpty {
                    Text("No emoji found")
                        .font(.system(size: 13))
                        .foregroundStyle(colors.hint)
                }
                Spacer()
            }
            .padding(.leading, 14)
        } else {
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 0) {
                    ForEach(search.results, id: \.self) { emoji in
                        Button {
                            model.keyDown()
                            model.emoji(emoji)
                        } label: {
                            Text(emoji)
                                .font(.system(size: 28))
                                .frame(width: 44)
                                .frame(maxHeight: .infinity)
                                .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .noScrollEdgeEffect()
        }
    }
}
