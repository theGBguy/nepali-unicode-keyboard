import SwiftUI
import NepaliKeyboardShared

/**
 * The 40 pt row above the keys. Shows Romanized suggestions (best guess in bold, a smart
 * suggestion, the other guesses scrolling sideways, then the Latin text as typed) or smart suggestions. It keeps its
 * height when empty so the keyboard never jumps; with nothing to suggest it shows the emoji button.
 */
struct KeyboardStripView: View {
    @ObservedObject var model: KeyboardModel
    let state: KeyboardState
    let colors: KeyboardColors

    /**
     * The gear shows where settings are instead of opening the app: App Review doesn't allow
     * keyboards to launch apps other than Settings.
     */
    @State private var showSettingsHint = false

    var body: some View {
        let buffer = state.transliterationBuffer
        let latin = state.showSuggestionBar ? buffer.romanizedText : ""
        let smart = state.smartActions

        GeometryReader { geometry in
            let slots = slots(smart: smart)
            if !latin.isEmpty {
                composing(latin: latin, buffer: buffer, smart: smart.first)
            } else if slots.isEmpty && showSettingsHint {
                Text("Settings are in the Nepali Keyboard app")
                    .font(.system(size: 15))
                    .foregroundStyle(colors.keyText)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .contentShape(Rectangle())
                    .onTapGesture { showSettingsHint = false }
                    .onAppear { UIAccessibility.post(notification: .announcement, argument: String(localized: "Settings are in the Nepali Keyboard app")) }
                    .task {
                        try? await Task.sleep(nanoseconds: 3_000_000_000)
                        if !Task.isCancelled { showSettingsHint = false }
                    }
            } else if slots.isEmpty {
                // Nothing to suggest: the panel buttons and a recent copy, like Android's strip
                HStack(spacing: 0) {
                    if !state.field.isPrivate && !state.field.prefersNumbers {
                        iconButton("face.smiling", label: String(localized: "Emoji")) { model.openEmoji() }
                    }
                    if !state.field.isPrivate {
                        iconButton("clipboard", label: String(localized: "Clipboard")) { model.openClipboard() }
                        iconButton("pencil.and.scribble", label: String(localized: "Handwriting")) { model.openHandwriting() }
                    }
                    if model.canPaste && !state.field.isPrivate {
                        divider
                        pasteButton
                    }
                    Spacer(minLength: 0)
                    iconButton("gearshape", label: String(localized: "Keyboard settings")) { showSettingsHint = true }
                }
                .frame(maxHeight: .infinity)
            } else {
            let dividers = CGFloat(max(slots.count - 1, 0))
            let unit = (geometry.size.width - dividers) / max(slots.reduce(0) { $0 + $1.weight }, 1)
            HStack(spacing: 0) {
                ForEach(Array(slots.enumerated()), id: \.offset) { index, slot in
                    if index > 0 { divider }
                    slotView(slot).frame(width: unit * slot.weight)
                }
            }
            .frame(maxHeight: .infinity)
            }
        }
        .padding(.horizontal, 4)
    }

    private func iconButton(_ symbol: String, label: String, action: @escaping () -> Void) -> some View {
        Button {
            model.keyDown()
            action()
        } label: {
            Image(systemName: symbol)
                .font(.system(size: 20))
                .foregroundStyle(colors.hint)
                .frame(width: 48, height: KeyboardMetrics.stripHeight)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(label))
    }

    /**
     * Offered when something new was copied. Plain "Paste" rather than a preview: reading the
     * text makes iOS ask "Allow Paste", so it's read only when tapped.
     */
    private var pasteButton: some View {
        Button {
            model.keyDown()
            model.pasteSuggestion()
        } label: {
            Text("Paste")
                .font(.system(size: 15))
                .foregroundStyle(colors.keyText)
                .padding(.horizontal, 12)
                .frame(maxHeight: .infinity)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    /** Word guesses the strip shows while typing; the rest scroll in from the right. */
    private static let maxWords = 6

    /** Right edge of the scrolling words in the strip's space, to fade the edge while more are off screen. */
    @State private var wordsEnd: CGFloat = 0

    private func words(_ buffer: TransliterationBuffer) -> [String] {
        var list: [String] = []
        let primary = buffer.primaryCandidate
        if !primary.isEmpty { list.append(primary) }
        for word in state.suggestions where !list.contains(word) { list.append(word) }
        return Array(list.prefix(Self.maxWords))
    }

    /**
     * Like Android: the best guess first (bold), then a smart suggestion, then the other guesses
     * at their natural width, scrolling sideways. The next one is cut off and fades out at the
     * edge, which says there's more. The Latin text stays pinned on the right.
     */
    private func composing(latin: String, buffer: TransliterationBuffer, smart: SmartAction?) -> some View {
        let words = words(buffer)
        return HStack(spacing: 0) {
            GeometryReader { viewport in
                ScrollViewReader { proxy in
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 0) {
                            ForEach(Array(words.enumerated()), id: \.offset) { index, word in
                                if index > 0 { divider }
                                wordSlot(word, bold: index == 0)
                                    .id(index)
                                if index == 0, let action = smart {
                                    divider
                                    smartSlot(action).frame(maxWidth: 200)
                                }
                            }
                            if words.isEmpty, let action = smart {
                                smartSlot(action).frame(maxWidth: 200)
                            }
                        }
                        .frame(maxHeight: .infinity)
                        .background(GeometryReader { content in
                            Color.clear.preference(key: WordsEndKey.self, value: content.frame(in: .named("words")).maxX)
                        })
                    }
                    .coordinateSpace(name: "words")
                    // A new key gives new guesses: show them from the best one again.
                    .onChange(of: words) { _ in proxy.scrollTo(0, anchor: .leading) }
                }
                .onPreferenceChange(WordsEndKey.self) { wordsEnd = $0 }
                .mask(
                    HStack(spacing: 0) {
                        Color.black
                        if wordsEnd > viewport.size.width + 1 {
                            LinearGradient(colors: [.black, .clear], startPoint: .leading, endPoint: .trailing)
                                .frame(width: 28)
                        }
                    }
                )
            }
            divider
            slot(latin, color: colors.hint, bold: false, size: 15) { model.keepLatin() }
                .frame(minWidth: 56, maxWidth: 120)
                .fixedSize(horizontal: true, vertical: false)
                .accessibilityHint(Text("Keep as typed"))
        }
        .frame(maxHeight: .infinity)
    }

    /** A word guess at its natural width (at least a comfortable tap target). */
    private func wordSlot(_ word: String, bold: Bool) -> some View {
        Button { model.suggestion(word) } label: {
            Text(word)
                .font(.system(size: 17, weight: bold ? .semibold : .regular))
                .foregroundStyle(colors.keyText)
                .lineLimit(1)
                .padding(.horizontal, 14)
                .frame(minWidth: 56, maxHeight: .infinity)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private enum Slot {
        case smart(SmartAction)
    }

    private struct WeightedSlot {
        let slot: Slot
        let weight: CGFloat
    }

    /**
     * Smart suggestions while nothing is being typed (the composing strip is [composing]):
     * one fills the row, two split 60/40 by length like Android.
     */
    private func slots(smart: [SmartAction]) -> [WeightedSlot] {
        if smart.count == 1 { return [WeightedSlot(slot: .smart(smart[0]), weight: 1)] }
        let longest = smart.firstIndex { action in smart.allSatisfy { action.label.count >= $0.label.count } }
        return smart.enumerated().map { index, action in
            WeightedSlot(slot: .smart(action), weight: index == longest ? 0.6 : 0.4)
        }
    }

    @ViewBuilder
    private func slotView(_ weighted: WeightedSlot) -> some View {
        switch weighted.slot {
        case let .smart(action):
            smartSlot(action)
        }
    }

    private var divider: some View {
        colors.divider.frame(width: 1, height: 18)
    }

    private func slot(_ text: String, color: Color, bold: Bool, size: CGFloat, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(text)
                .font(.system(size: size, weight: bold ? .semibold : .regular))
                .foregroundStyle(color)
                .lineLimit(1)
                .truncationMode(.tail)
                .padding(.horizontal, 6)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    /** A smart suggestion: plain text like a word guess, after a small icon so it reads as a conversion. */
    private func smartSlot(_ action: SmartAction) -> some View {
        Button { model.smartAction(action) } label: {
            HStack(spacing: 4) {
                Image(systemName: smartIcon(action.kind))
                    .font(.system(size: 12))
                    .foregroundStyle(colors.hint)
                    .accessibilityHidden(true)
                Text(action.label)
                    .font(.system(size: 16))
                    .foregroundStyle(colors.keyText)
                    .lineLimit(1)
                    .truncationMode(.tail)
            }
            .padding(.horizontal, 6)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    private func smartIcon(_ kind: SmartAction.Kind) -> String {
        switch kind {
        case .amount: return "123.rectangle"
        case .date: return "calendar"
        default: return "ruler"
        }
    }
}

private struct WordsEndKey: PreferenceKey {
    static var defaultValue: CGFloat = 0
    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) { value = max(value, nextValue()) }
}
