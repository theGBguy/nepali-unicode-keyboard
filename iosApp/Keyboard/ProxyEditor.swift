import UIKit
import NepaliKeyboardShared

/**
 * The focused field through the extension's `textDocumentProxy`, for the shared controller.
 * Romanized words are shown as marked text, the iOS name for Android's composing text.
 */
final class ProxyEditor: NSObject, EditorConnection {
    private weak var controller: UIInputViewController?

    /** The marked text this keyboard set and hasn't committed yet. */
    private(set) var composing = ""

    init(controller: UIInputViewController) {
        self.controller = controller
    }

    private var proxy: UITextDocumentProxy? { controller?.textDocumentProxy }

    /** The text before the cursor, with the marked text even where the app leaves it out. */
    var contextBeforeCursor: String {
        var context = proxy?.documentContextBeforeInput ?? ""
        if !composing.isEmpty && !context.hasSuffix(composing) { context += composing }
        return context
    }

    func commitText(text: String) {
        guard let proxy else { return }
        if text.isEmpty {
            // The controller deletes a selection by typing nothing over it
            if !composing.isEmpty {
                clearMarkedText()
            } else if let selected = proxy.selectedText, !selected.isEmpty {
                proxy.deleteBackward()
            }
            return
        }
        // Typing over marked text replaces it and ends composing
        proxy.insertText(text)
        composing = ""
    }

    func setComposingText(text: String) {
        guard let proxy else { return }
        if text.isEmpty {
            clearMarkedText()
            return
        }
        proxy.setMarkedText(text, selectedRange: NSRange(location: (text as NSString).length, length: 0))
        composing = text
    }

    func finishComposingText() {
        guard !composing.isEmpty else { return }
        proxy?.unmarkText()
        composing = ""
    }

    func textBeforeCursor(length: Int32) -> String? {
        let units = Array(contextBeforeCursor.utf16.suffix(Int(length)))
        return String(decoding: units, as: UTF16.self)
    }

    func selectedText() -> String? {
        proxy?.selectedText
    }

    /**
     * `deleteBackward` removes a whole character as the app sees it, which can be more than one
     * code point. Delete until [length] UTF-16 units are gone, and put back anything removed past
     * that, so क्षि loses only ि like on Android.
     */
    func deleteBeforeCursor(length: Int32) {
        guard let proxy, length > 0 else { return }
        finishComposingText()
        let before = Array((proxy.documentContextBeforeInput ?? "").utf16)
        let target = max(before.count - Int(length), 0)
        for _ in 0..<Int(length) {
            proxy.deleteBackward()
            if (proxy.documentContextBeforeInput ?? "").utf16.count <= target { break }
        }
        let now = (proxy.documentContextBeforeInput ?? "").utf16.count
        if now < target, before.count >= target {
            proxy.insertText(String(decoding: before[now..<target], as: UTF16.self))
        }
    }

    func beginBatchEdit() {}

    func endBatchEdit() {}

    func sendEnterKey() {
        proxy?.insertText("\n")
    }

    /** iOS has no editor actions: Return inserts a line break and the app acts on it. */
    func performEditorAction(actionId: Int32) {
        proxy?.insertText("\n")
    }

    private func clearMarkedText() {
        guard !composing.isEmpty else { return }
        proxy?.setMarkedText("", selectedRange: NSRange(location: 0, length: 0))
        proxy?.unmarkText()
        composing = ""
    }
}
