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

    private var editor: ProxyEditor?
    private var watch: StateWatch?
    private weak var inputController: UIInputViewController?
    /** What the focused field asked for last time, to tell a new field from a text change. */
    private var lastField: FieldInfo?

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
        watch = controller.watchState { [weak self] state in
            self?.state = state
        }
    }

    /** The keyboard came on screen for a field. */
    func inputStarted() {
        guard let proxy = inputController?.textDocumentProxy else { return }
        let field = FieldInfo.from(proxy)
        lastField = field
        controller.onInputStarted(field: field, restarting: false)
        afterEdit()
    }

    func inputFinished() {
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

    func nextKeyboard() {
        inputController?.advanceToNextInputMode()
    }

    func close() {
        watch?.cancel()
        controller.close()
    }
}
