import Cocoa
import SwiftUI
import NepaliKeyboardShared

/** What the strip shows; published from the controller's state. */
@MainActor
final class StripModel: ObservableObject {
    struct Slot: Identifiable, Hashable {
        let id: Int
        let label: String
        let smart: SmartAction?
        let highlighted: Bool
    }

    @Published private(set) var slots: [Slot] = []
    var onWord: (String) -> Void = { _ in }
    var onSmart: (SmartAction) -> Void = { _ in }

    /** The words being offered for the Romanized word, with a smart suggestion second, like the phone strip. */
    func update(_ state: KeyboardState) {
        var result: [Slot] = []
        let words = state.stripWords
        if !words.isEmpty {
            let smart = state.stripSmartAction
            var labels: [(String, SmartAction?)] = words.map { ($0, nil) }
            if let smart { labels.insert((smart.label, smart), at: min(1, labels.count)) }
            result = labels.enumerated().map { i, item in
                Slot(id: i, label: item.0, smart: item.1, highlighted: i == Int(state.highlightedSuggestion))
            }
        } else if state.currentLanguage != .english {
            // Conversions offered after a word is committed (English full stop, amounts…): click only
            result = state.smartActions.enumerated().map { i, action in
                Slot(id: i, label: action.label, smart: action, highlighted: false)
            }
        }
        if result != slots { slots = result }
    }

    func click(_ slot: Slot) {
        if let smart = slot.smart { onSmart(smart) } else { onWord(slot.label) }
    }
}

/** The suggestion strip under the cursor: plain text in a small rounded panel, the pick highlighted. */
struct StripView: View {
    @ObservedObject var model: StripModel

    var body: some View {
        HStack(spacing: 2) {
            ForEach(model.slots) { slot in
                Button { model.click(slot) } label: {
                    HStack(spacing: 4) {
                        if let smart = slot.smart, let icon = icon(smart.kind) {
                            Image(systemName: icon)
                                .font(.system(size: 11))
                                .foregroundStyle(slot.highlighted ? Color.white.opacity(0.8) : .secondary)
                        }
                        Text(slot.label)
                            .font(.system(size: 15))
                            .foregroundStyle(slot.highlighted ? Color.white : .primary)
                            .lineLimit(1)
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 4)
                    .background(
                        RoundedRectangle(cornerRadius: 5)
                            .fill(slot.highlighted ? Color.accentColor : Color.clear)
                    )
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(
            RoundedRectangle(cornerRadius: 8).fill(.regularMaterial)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8).strokeBorder(Color.primary.opacity(0.12))
        )
        .fixedSize()
    }

    /** Nil for the English full stop offered after ।, which is plain text. */
    private func icon(_ kind: SmartAction.Kind) -> String? {
        switch kind {
        case .amount: return "123.rectangle"
        case .date: return "calendar"
        case .punctuation: return nil
        default: return "ruler"
        }
    }
}

/** Accepts the first click, so a suggestion is picked without activating the panel first. */
private final class FirstMouseHostingView<Content: View>: NSHostingView<Content> {
    override func acceptsFirstMouse(for event: NSEvent?) -> Bool { true }
}

/**
 * The borderless panel the strip lives in. It never becomes key or activates, so the app keeps
 * focus and the text field keeps its marked text while the user clicks a suggestion.
 */
@MainActor
final class StripPanel {
    static let shared = StripPanel()

    let model = StripModel()
    private let panel: NSPanel

    private init() {
        panel = NSPanel(
            contentRect: .zero,
            styleMask: [.borderless, .nonactivatingPanel],
            backing: .buffered,
            defer: true
        )
        panel.isFloatingPanel = true
        panel.level = .popUpMenu
        panel.hidesOnDeactivate = false
        panel.isOpaque = false
        panel.backgroundColor = .clear
        panel.hasShadow = true
        panel.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .ignoresCycle]
        panel.contentView = FirstMouseHostingView(rootView: StripView(model: model))
    }

    /**
     * Shows the strip below [lineRect] (the line with the cursor, in screen coordinates), or above
     * it when there's no room below. Hides it when there's nothing to offer.
     */
    func show(below lineRect: NSRect) {
        guard !model.slots.isEmpty else { return hide() }
        guard let content = panel.contentView else { return }
        content.layoutSubtreeIfNeeded()
        let size = content.fittingSize
        let screen = NSScreen.screens.first { $0.frame.contains(lineRect.origin) } ?? NSScreen.main
        let visible = screen?.visibleFrame ?? .zero
        var origin = NSPoint(x: lineRect.minX, y: lineRect.minY - size.height - 4)
        if origin.y < visible.minY { origin.y = lineRect.maxY + 4 }
        origin.x = min(max(origin.x, visible.minX), visible.maxX - size.width)
        panel.setFrame(NSRect(origin: origin, size: size), display: true)
        if !panel.isVisible { panel.orderFrontRegardless() }
    }

    func hide() {
        if panel.isVisible { panel.orderOut(nil) }
    }
}
