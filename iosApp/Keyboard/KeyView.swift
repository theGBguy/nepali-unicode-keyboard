import SwiftUI
import NepaliKeyboardShared

/** How a key is drawn: a label or an SF Symbol, an optional corner hint, and its colours. */
struct KeyVisual {
    var text: String?
    var symbol: String?
    var hint: String?
    var background: Color
    var content: Color
    var fontSize: CGFloat = 21
    var description: String?
    var showsPreview = false
}

/**
 * The character a long-press types, shown as the key's corner hint: the Nepali shift character,
 * or the first long-press symbol. Uppercase Latin letters don't count (shift already gives them).
 */
func keyAlternate(_ key: KeyData, isShifted: Bool) -> String? {
    guard key.keyType == .character, !isShifted else { return nil }
    if let shift = key.shift, shift != key.primary, shift != key.primary.uppercased() { return shift }
    return key.longPress?.first { $0 != key.primary }
}

/** What the Return key shows for the field (iOS has no custom action labels). */
func enterVisual(_ field: FieldInfo) -> (symbol: String, description: String) {
    switch field.enterAction {
    case .done: return ("checkmark", String(localized: "Done"))
    case .go: return ("arrow.right", String(localized: "Go"))
    case .search: return ("magnifyingglass", String(localized: "Search"))
    case .send: return ("paperplane", String(localized: "Send"))
    case .next: return ("arrow.right.to.line", String(localized: "Next"))
    case .previous: return ("arrow.left.to.line", String(localized: "Previous"))
    default: return ("return", String(localized: "New line"))
    }
}

func keyVisual(_ key: KeyData, state: KeyboardState, colors: KeyboardColors) -> KeyVisual {
    let nepaliKeys = state.currentLanguage == .nepali
    switch key.primary {
    case "↵":
        let enter = enterVisual(state.field)
        return KeyVisual(symbol: enter.symbol, background: colors.accent, content: colors.onAccent, description: enter.description)
    case "⌫":
        return KeyVisual(symbol: "delete.left", background: colors.functionKey, content: colors.keyText,
                         description: String(localized: "Delete"))
    case "⇧":
        return KeyVisual(
            symbol: state.isCapsLock ? "capslock.fill" : (state.isShifted ? "shift.fill" : "shift"),
            background: colors.functionKey,
            content: state.isShifted ? colors.accent : colors.keyText,
            description: state.isCapsLock ? String(localized: "Caps lock") : String(localized: "Shift")
        )
    case "🌐":
        return KeyVisual(text: "अ/A", background: colors.functionKey, content: colors.keyText, fontSize: 15,
                         description: String(localized: "English, Romanized Nepali or traditional Nepali. Long-press for options."))
    case " ":
        let label: String
        switch state.currentLanguage {
        case .nepali: label = String(localized: "नेपाली · Traditional")
        case .transliteration: label = String(localized: "नेपाली · Romanized")
        default: label = "English"
        }
        return KeyVisual(text: label, background: colors.key, content: colors.hint, fontSize: 12,
                         description: String(localized: "Space"))
    case "?123", SymbolLayout.shared.PAGE_ONE, SymbolLayout.shared.PAGE_TWO:
        return KeyVisual(text: key.primary == "?123" && nepaliKeys ? "?१२३" : key.primary,
                         background: colors.functionKey, content: colors.keyText, fontSize: 14,
                         description: String(localized: "Numbers and symbols"))
    case "ABC":
        return KeyVisual(text: nepaliKeys ? "कखग" : "ABC", background: colors.functionKey, content: colors.keyText,
                         fontSize: 14, description: String(localized: "Letters"))
    default:
        return KeyVisual(
            text: key.getOutput(isShifted: state.isShifted),
            hint: keyAlternate(key, isShifted: state.isShifted),
            background: key.keyType == .character ? colors.key : colors.functionKey,
            content: colors.keyText,
            fontSize: key.primary.count > 2 ? 14 : 21,
            showsPreview: key.keyType == .character
        )
    }
}

/**
 * One key. Taps fire on release; a long-press types the corner hint (or opens the language menu
 * on अ/A); backspace deletes on touch-down and repeats while held. Character keys show a preview
 * bubble above the finger.
 */
struct KeyView: View {
    @ObservedObject var model: KeyboardModel
    let key: KeyData
    let state: KeyboardState
    let colors: KeyboardColors
    let height: CGFloat

    @State private var pressed = false
    @State private var longPressTask: Task<Void, Never>?
    @State private var longPressed = false

    private static let longPressDelay: UInt64 = 400_000_000

    var body: some View {
        let visual = keyVisual(key, state: state, colors: colors)
        let isBackspace = key.primary == "⌫"
        let isLanguageKey = key.primary == "🌐"
        let alternate = keyAlternate(key, isShifted: state.isShifted)

        RoundedRectangle(cornerRadius: KeyboardMetrics.keyRadius)
            .fill(visual.background)
            // The press tint mixes in the text colour: darker keys in light mode, lighter in dark
            .overlay(RoundedRectangle(cornerRadius: KeyboardMetrics.keyRadius).fill(colors.keyText.opacity(pressed ? 0.12 : 0)))
            .shadow(color: colors.keyShadow, radius: 0, x: 0, y: 0.5)
            .overlay(content(visual))
            .overlay(alignment: .topTrailing) {
                if let hint = visual.hint {
                    Text(hint)
                        .font(.system(size: 10))
                        .foregroundStyle(colors.hint)
                        .padding(.top, 2)
                        .padding(.trailing, 4)
                }
            }
            .scaleEffect(pressed ? 0.95 : 1)
            .animation(.spring(response: 0.2, dampingFraction: 0.55), value: pressed)
            .overlay(alignment: .top) {
                if pressed && visual.showsPreview {
                    Text(visual.text ?? "")
                        .font(.system(size: 28))
                        .foregroundStyle(colors.keyText)
                        .lineLimit(1)
                        .frame(width: 46, height: 52)
                        .background(RoundedRectangle(cornerRadius: 10).fill(colors.preview).shadow(radius: 6, y: 1))
                        .offset(y: -56)
                        .allowsHitTesting(false)
                }
            }
            .zIndex(pressed ? 1 : 0)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in
                        guard !pressed else { return }
                        pressed = true
                        longPressed = false
                        if isBackspace { model.tap(key) }
                        if isBackspace || isLanguageKey || alternate != nil {
                            longPressTask = Task { @MainActor in
                                try? await Task.sleep(nanoseconds: Self.longPressDelay)
                                guard !Task.isCancelled, pressed else { return }
                                longPressed = true
                                if isBackspace {
                                    // Like Android: repeating starts only once the key is held
                                    model.backspaceRepeat(true)
                                } else if isLanguageKey {
                                    model.controller.onLanguageLongPress()
                                } else if let alternate {
                                    model.longPress(alternate: alternate)
                                }
                            }
                        }
                    }
                    .onEnded { _ in
                        pressed = false
                        longPressTask?.cancel()
                        longPressTask = nil
                        if isBackspace {
                            if longPressed { model.backspaceRepeat(false) }
                        } else if !longPressed {
                            model.tap(key)
                        }
                    }
            )
            .accessibilityElement()
            .accessibilityLabel(Text(visual.description ?? visual.text ?? key.primary))
            .accessibilityAddTraits(.isKeyboardKey)
    }

    @ViewBuilder
    private func content(_ visual: KeyVisual) -> some View {
        if let symbol = visual.symbol {
            Image(systemName: symbol)
                .font(.system(size: 19, weight: .regular))
                .foregroundStyle(visual.content)
        } else {
            Text(visual.text ?? "")
                .font(.system(size: visual.fontSize))
                .foregroundStyle(visual.content)
                .lineLimit(1)
                .minimumScaleFactor(0.6)
                .padding(.horizontal, 2)
        }
    }
}
