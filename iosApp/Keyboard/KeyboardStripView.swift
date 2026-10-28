import SwiftUI
import NepaliKeyboardShared

/**
 * The 40 pt row above the keys. Shows Romanized suggestions (best guess in bold, then a smart
 * suggestion or the next guess, then the Latin text as typed) or smart suggestions. It keeps its
 * height when empty so the keyboard never jumps. Emoji, clipboard and settings come later.
 */
struct KeyboardStripView: View {
    @ObservedObject var model: KeyboardModel
    let state: KeyboardState
    let colors: KeyboardColors

    var body: some View {
        let buffer = state.transliterationBuffer
        let latin = state.showSuggestionBar ? buffer.romanizedText : ""
        let smart = state.smartActions

        GeometryReader { geometry in
            let slots = slots(latin: latin, buffer: buffer, smart: smart)
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
        .padding(.horizontal, 4)
    }

    private enum Slot {
        case word(String, bold: Bool)
        case latin(String)
        case smart(SmartAction)
    }

    private struct WeightedSlot {
        let slot: Slot
        let weight: CGFloat
    }

    /**
     * Like Android: word guesses get one share each, a smart suggestion up to 2.5 so dates and
     * amounts in words stay readable; two smart suggestions alone split 60/40 by length.
     */
    private func slots(latin: String, buffer: TransliterationBuffer, smart: [SmartAction]) -> [WeightedSlot] {
        if !latin.isEmpty {
            let primary = buffer.primaryCandidate
            var list: [WeightedSlot] = []
            if !primary.isEmpty { list.append(WeightedSlot(slot: .word(primary, bold: true), weight: 1)) }
            // A smart suggestion takes the second guess's slot
            if let action = smart.first {
                list.append(WeightedSlot(slot: .smart(action), weight: min(max(CGFloat(action.label.count) / 8, 1), 2.5)))
            } else if let next = state.suggestions.first(where: { $0 != primary }) {
                list.append(WeightedSlot(slot: .word(next, bold: false), weight: 1))
            }
            list.append(WeightedSlot(slot: .latin(latin), weight: 1))
            return list
        }
        if smart.count == 1 { return [WeightedSlot(slot: .smart(smart[0]), weight: 1)] }
        let longest = smart.firstIndex { action in smart.allSatisfy { action.label.count >= $0.label.count } }
        return smart.enumerated().map { index, action in
            WeightedSlot(slot: .smart(action), weight: index == longest ? 0.6 : 0.4)
        }
    }

    @ViewBuilder
    private func slotView(_ weighted: WeightedSlot) -> some View {
        switch weighted.slot {
        case let .word(word, bold):
            slot(word, color: colors.keyText, bold: bold, size: 17) { model.suggestion(word) }
        case let .latin(text):
            slot(text, color: colors.hint, bold: false, size: 15) { model.keepLatin() }
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
