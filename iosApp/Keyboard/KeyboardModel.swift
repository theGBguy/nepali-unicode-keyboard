import SwiftUI
import UIKit
import NepaliKeyboardShared

/** Holds the shared controller for the extension and publishes its state to SwiftUI. */
@MainActor
final class KeyboardModel: ObservableObject {
    let controller = KeyboardController()
    @Published private(set) var state: KeyboardState
    /** Whether iOS wants this keyboard to show its own key for the next keyboard. */
    @Published var needsInputModeSwitchKey = false
    /** Full Access, re-checked each time the keyboard opens (the user can change it any time). */
    @Published private(set) var hasFullAccess = false
    /** Something new was copied: the strip offers Paste, read only when tapped. */
    @Published private(set) var canPaste = false
    /** The copy last pasted from the strip, so it isn't offered again. */
    private var pastedCopyId: Int64?

    private var editor: ProxyEditor?
    private var watch: StateWatch?
    private weak var inputController: UIInputViewController?
    /** What the focused field asked for last time, to tell a new field from a text change. */
    private var lastField: FieldInfo?
    private var clipboardSource: PasteboardClipboardSource?
    private var clipboardStorage: GroupClipboardStorage?

    init() {
        state = controller.currentState
    }

    func attach(to inputController: UIInputViewController) {
        self.inputController = inputController
        let editor = ProxyEditor(controller: inputController)
        self.editor = editor
        controller.editor = { editor }
        // The App Group is writable only with Full Access
        controller.prefs = DefaultsKeyboardPrefs(canWriteGroup: { [weak inputController] in
            inputController?.hasFullAccess ?? false
        })
        controller.onSwitchKeyboard = { [weak inputController] in inputController?.advanceToNextInputMode() }
        // Clipboard history needs Full Access: without it the keyboard sees an empty pasteboard
        let fullAccess = { [weak inputController] in inputController?.hasFullAccess ?? false }
        let source = PasteboardClipboardSource(hasFullAccess: fullAccess)
        let storage = GroupClipboardStorage(hasFullAccess: fullAccess)
        clipboardSource = source
        clipboardStorage = storage
        controller.clipboardSource = source
        controller.clipboardStorage = storage
        watch = controller.watchState { [weak self] state in
            self?.state = state
        }
    }

    /** The keyboard came on screen for a field. */
    func inputStarted() {
        guard let inputController else { return }
        hasFullAccess = inputController.hasFullAccess
        // The app may have cleared the history while the keyboard was closed
        clipboardStorage?.reload()
        clipboardSource?.observe { [weak self] in self?.canPaste = true }
        canPaste = recentCopyId().map { $0 != pastedCopyId } ?? false
        let field = FieldInfo.from(inputController.textDocumentProxy)
        lastField = field
        controller.onInputStarted(field: field, restarting: false)
        afterEdit()
    }

    func inputFinished() {
        clipboardSource?.observe(onCopy: nil)
        controller.onInputFinished()
    }

    /** The app changed the text or moved the cursor, or another field got focus. */
    func textChanged() {
        guard let proxy = inputController?.textDocumentProxy, let editor else { return }
        let field = FieldInfo.from(proxy)
        if field != lastField {
            lastField = field
            controller.onInputStarted(field: field, restarting: false)
        }
        // The published state reaches SwiftUI a run-loop turn later; decide on the controller's own
        let buffer = controller.currentState.transliterationBuffer.romanizedText
        let stillComposing = buffer.isEmpty || editor.contextBeforeCursor.hasSuffix(buffer)
        let hasSelection = !(proxy.selectedText ?? "").isEmpty
        controller.onCursorMoved(stillComposing: stillComposing, hasComposing: !editor.composing.isEmpty, hasSelection: hasSelection)
    }

    /**
     * Android refreshes smart suggestions from the cursor update each edit causes. iOS doesn't
     * report the keyboard's own edits, so do it after each one.
     */
    func afterEdit() {
        guard let editor else { return }
        controller.onCursorMoved(stillComposing: true, hasComposing: !editor.composing.isEmpty, hasSelection: false)
    }

    // MARK: Actions from the views

    /** A key went down: click and/or tap, as set in the app. */
    func keyDown() {
        KeyFeedback.keyDown(sound: state.feel.sound, vibrate: state.feel.vibrate)
    }

    func tap(_ key: KeyData) {
        canPaste = false
        controller.onKeyClick(key: key)
        afterEdit()
    }

    func longPress(alternate: String) {
        controller.onKeyLongPress(alternate: alternate)
        afterEdit()
    }

    func suggestion(_ word: String) {
        controller.onSuggestionClick(suggestion: word)
        afterEdit()
    }

    func smartAction(_ action: SmartAction) {
        controller.onSmartActionClick(action: action)
        afterEdit()
    }

    func keepLatin() {
        controller.onKeepLatin()
        afterEdit()
    }

    func backspaceRepeat(_ on: Bool) {
        if on { controller.onBackspaceLongPressStart() } else { controller.onBackspaceLongPressEnd(); afterEdit() }
    }

    // MARK: Emoji

    func openEmoji() {
        EmojiCatalog.shared.loadIfNeeded()
        controller.openEmoji()
    }

    func closeEmoji() {
        controller.closeEmoji()
        afterEdit()
    }

    func emoji(_ emoji: String) {
        controller.onEmojiClick(emoji: emoji)
    }

    // MARK: Clipboard

    private func recentCopyId() -> Int64? {
        clipboardSource?.recentCopyId(withinMillis: 2 * 60 * 1000)?.int64Value
    }

    /** Runs [body] with pasteboard reads allowed: only for taps that need the text. */
    private func reading(_ body: () -> Void) {
        clipboardSource?.readAllowed = true
        body()
        clipboardSource?.readAllowed = false
    }

    func openClipboard() {
        canPaste = false
        reading { controller.openClipboard() }
    }

    func closeClipboard() {
        controller.closeClipboard()
        afterEdit()
    }

    func pasteClip(_ text: String) {
        controller.onClipPaste(text: text)
        afterEdit()
    }

    func pinClip(_ text: String, pinned: Bool) {
        controller.onClipPin(text: text, pinned: pinned)
    }

    func deleteClip(_ text: String) {
        controller.onClipDelete(text: text)
    }

    func clearClips() {
        controller.onClipClearAll()
    }

    /** The strip's Paste: reads the copy now (iOS may ask), pastes it and saves it like the panel does. */
    func pasteSuggestion() {
        canPaste = false
        pastedCopyId = recentCopyId()
        var copied: CopiedText?
        reading { copied = clipboardSource?.read() }
        guard let copied else { return }
        if copied.sensitive {
            inputController?.textDocumentProxy.insertText(copied.text)
        } else {
            controller.onClipPaste(text: copied.text)
        }
        afterEdit()
    }

    func clearPasteOffer() {
        canPaste = false
    }

    func nextKeyboard() {
        inputController?.advanceToNextInputMode()
    }

    func close() {
        clipboardSource?.observe(onCopy: nil)
        watch?.cancel()
        controller.close()
    }
}
