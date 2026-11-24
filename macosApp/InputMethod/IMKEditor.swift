import Cocoa
import InputMethodKit
import NepaliKeyboardShared

/**
 * The focused text field through InputMethodKit's client, for the shared controller.
 *
 * The current word stays marked (underlined) text until the next word starts, the way Indic input
 * methods on the Mac work: the Romanized letters being typed ([composing], Android's composing
 * text) and the text already typed in this word ([held]). The controller sometimes replaces text
 * it typed a moment ago (अ + ा → आ, . → ।, a smart suggestion over "500 "), and several apps,
 * TextEdit among them, ignore an input method's replacement ranges: only marked text can be
 * replaced everywhere. Older text is replaced through ranges, which works in most apps.
 *
 * Apps answer the context questions differently: Cocoa text views give the text around the cursor,
 * some (Terminal, some Electron and Java apps) give nothing. The controller works without it.
 */
final class IMKEditor: NSObject, EditorConnection {
    private let client: () -> (IMKTextInput & NSObjectProtocol)?

    /** The Romanized letters being typed, shown after [held]. */
    private(set) var composing = ""

    /** Text typed in the current word, not given to the app yet. */
    private var held = ""

    /** What this input method shows as marked text. */
    var marked: String { held + composing }

    init(client: @escaping () -> (IMKTextInput & NSObjectProtocol)?) {
        self.client = client
    }

    /** Replace the marked text, or the selection when there is none. */
    private let current = NSRange(location: NSNotFound, length: NSNotFound)

    func commitText(text: String) {
        composing = ""
        // A new word (not a space or punctuation after the last one) gives the held one to the app
        if let first = text.first, !first.isWhitespace, held.last?.isWhitespace == true { flush() }
        if text.isEmpty && held.isEmpty {
            // Nothing typed over the marked text or the selection: delete it
            client()?.insertText("", replacementRange: current)
            return
        }
        held += text
        show()
    }

    func setComposingText(text: String) {
        composing = text
        show()
    }

    func finishComposingText() {
        held += composing
        composing = ""
        show()
    }

    /** Gives the held word to the app: before keys the app handles, clicks, and switches. */
    func flush() {
        let text = marked
        held = ""
        composing = ""
        guard !text.isEmpty else { return }
        client()?.insertText(text, replacementRange: current)
    }

    private func show() {
        guard let client = client() else { return }
        let text = marked
        if text.isEmpty {
            client.setMarkedText("", selectionRange: NSRange(location: 0, length: 0), replacementRange: current)
            return
        }
        let attributed = NSAttributedString(string: text, attributes: [
            .underlineStyle: NSUnderlineStyle.single.rawValue,
            .markedClauseSegment: 0,
        ])
        let end = NSRange(location: (text as NSString).length, length: 0)
        client.setMarkedText(attributed, selectionRange: end, replacementRange: current)
    }

    /** The text before the cursor, with the marked text even where the app leaves it out. */
    func textBeforeCursor(length: Int32) -> String? {
        let shown = marked
        guard let client = client() else { return shown }
        let selection = client.selectedRange()
        var context = ""
        if selection.location != NSNotFound, selection.location > 0 {
            let start = max(selection.location - Int(length), 0)
            context = client.attributedSubstring(from: NSRange(location: start, length: selection.location - start))?.string ?? ""
        }
        if !shown.isEmpty && !context.hasSuffix(shown) { context += shown }
        return String(decoding: Array(context.utf16.suffix(Int(length))), as: UTF16.self)
    }

    func textAfterCursor(length: Int32) -> String? {
        guard let client = client() else { return nil }
        let selection = client.selectedRange()
        guard selection.location != NSNotFound else { return nil }
        let start = selection.location + selection.length
        let available = client.length() - start
        guard available > 0 else { return "" }
        return client.attributedSubstring(from: NSRange(location: start, length: min(Int(length), available)))?.string
    }

    func selectedText() -> String? {
        guard let client = client(), marked.isEmpty else { return "" }
        let selection = client.selectedRange()
        guard selection.location != NSNotFound, selection.length > 0 else { return "" }
        return client.attributedSubstring(from: selection)?.string
    }

    /** Deletes [length] UTF-16 units before the cursor: from the held word, then from the app's text. */
    func deleteBeforeCursor(length: Int32) {
        guard length > 0 else { return }
        held += composing
        composing = ""
        let heldUnits = Array(held.utf16)
        let fromHeld = min(Int(length), heldUnits.count)
        held = String(decoding: heldUnits.dropLast(fromHeld), as: UTF16.self)
        let rest = Int(length) - fromHeld
        if rest > 0, let client = client() {
            // Older text: replace its range, then show the word again after it
            let word = held
            held = ""
            show()
            let selection = client.selectedRange()
            if selection.location != NSNotFound, selection.location > 0 {
                let count = min(rest, selection.location)
                client.insertText("", replacementRange: NSRange(location: selection.location - count, length: count))
            }
            held = word
        }
        show()
    }

    func beginBatchEdit() {}

    func endBatchEdit() {}

    func sendEnterKey() {
        flush()
        client()?.insertText("\n", replacementRange: current)
    }

    /** The Mac has no editor actions: Return goes to the app, which acts on it. */
    func performEditorAction(actionId: Int32) {
        sendEnterKey()
    }

    /** The app committed or dropped the marked text itself (a click elsewhere, focus change). */
    func forget() {
        held = ""
        composing = ""
    }
}
