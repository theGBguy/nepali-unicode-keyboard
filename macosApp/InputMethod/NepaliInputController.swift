import Carbon
import Cocoa
import InputMethodKit
import NepaliKeyboardShared

/**
 * One per text-input client (macOS makes one for each app/document that uses the input method).
 * Physical keys go through the shared [HardwareKeyRouter] to the shared [KeyboardController],
 * exactly like Android's hardware-keyboard path.
 */
@objc(NepaliInputController)
final class NepaliInputController: IMKInputController {
    private let keyboard = KeyboardController()
    private let prefs = MacKeyboardPrefs.shared
    private lazy var editor = IMKEditor { [weak self] in self?.textClient }
    // English is a separate input source on the Mac, so Ctrl+Space stays with the system
    private lazy var router = HardwareKeyRouter(keyboard: { [unowned self] in self.keyboard }, ownsLanguageKey: false)
    private var watch: StateWatch?
    private var active = false

    private var textClient: (IMKTextInput & NSObjectProtocol)? { client() }

    override init!(server: IMKServer!, delegate: Any!, client inputClient: Any!) {
        super.init(server: server, delegate: delegate, client: inputClient)
        keyboard.prefs = prefs
        let editor = self.editor
        keyboard.editor = { editor }
        keyboard.onHardwareKeyboardChanged(connected: true)
        watch = keyboard.watchState { [weak self] state in
            self?.stateChanged(state)
        }
    }

    deinit {
        watch?.cancel()
        keyboard.close()
    }

    // MARK: Session

    override func activateServer(_ sender: Any!) {
        super.activateServer(sender)
        active = true
        // Whatever was marked in the last session, the app has committed it by now
        editor.forget()
        // Keys are read as on a US keyboard: Romanized spelling and the traditional table are by
        // US letters and positions, whatever Latin layout the user types English with
        (sender as? IMKTextInput)?.overrideKeyboard(withKeyboardNamed: "com.apple.keylayout.ABC")
        keyboard.onInputStarted(field: Self.field, restarting: false)
        if let mode = (sender as? IMKTextInput).flatMap(currentMode) { applyMode(mode) }
    }

    override func deactivateServer(_ sender: Any!) {
        commitComposition(sender)
        keyboard.onInputFinished()
        active = false
        MainActor.assumeIsolated { StripPanel.shared.hide() }
        super.deactivateServer(sender)
    }

    /** The user picked the other Nepali input source in the menu. */
    override func setValue(_ value: Any!, forTag tag: Int, client sender: Any!) {
        super.setValue(value, forTag: tag, client: sender)
        if let mode = value as? String { applyMode(mode) }
    }

    /** macOS asks to finish the word: the user clicked elsewhere, or the field lost focus. */
    override func commitComposition(_ sender: Any!) {
        keyboard.onHardwarePassThrough()
        editor.flush()
    }

    override func recognizedEvents(_ sender: Any!) -> Int {
        Int(NSEvent.EventTypeMask.keyDown.rawValue)
    }

    // MARK: Keys

    override func handle(_ event: NSEvent!, client sender: Any!) -> Bool {
        guard let event, event.type == .keyDown else { return false }
        // The app removed or committed the marked text itself (cleared the field after Send…)
        if !editor.marked.isEmpty, textClient?.markedRange().location == NSNotFound {
            editor.forget()
            keyboard.onCursorMoved(stillComposing: false, hasComposing: false, hasSelection: false)
        }
        let key = MacKeys.event(event)
        var handled = router.onKeyDown(event: key)
        if !handled && key.key == .backspace && !editor.marked.isEmpty {
            // The held word is still ours: delete from it like the field would
            editor.deleteBeforeCursor(length: Backspace.shared.length(before: editor.marked))
            handled = true
        } else if !handled {
            // Keys the app handles (Return, arrows, shortcuts) act on the text it has
            editor.flush()
        }
        // The controller refreshes smart suggestions from the cursor update each edit causes;
        // the Mac doesn't report the input method's own edits, so do it after each key
        keyboard.onCursorMoved(stillComposing: true, hasComposing: !editor.composing.isEmpty, hasSelection: false)
        return handled
    }

    // MARK: Input menu

    /** Items under the input source in the menu bar's input menu. */
    override func menu() -> NSMenu! {
        let menu = NSMenu()
        let settings = NSMenuItem(title: String(localized: "Settings…"), action: #selector(showSettings(_:)), keyEquivalent: "")
        settings.target = self
        menu.addItem(settings)
        return menu
    }

    @objc private func showSettings(_ sender: Any?) {
        MainActor.assumeIsolated { SettingsWindow.shared.show() }
    }

    // MARK: Input sources

    /** A plain text field: macOS doesn't tell an input method more (it's off in password fields). */
    private static let field = FieldInfo(
        kind: .text, isPrivate: false, noLearning: false, enterAction: .newline,
        imeActionId: FieldInfo.companion.IME_ACTION_NONE, actionLabel: nil, sendsKeyEvents: false, capsMode: .none
    )

    private func currentMode(_ client: IMKTextInput) -> String? {
        guard let id = Bundle.main.bundleIdentifier else { return nil }
        // The selected source of this bundle, as macOS has it
        guard let source = TISCopyCurrentKeyboardInputSource()?.takeRetainedValue(),
              let current = InputSourceInstaller.property(source, kTISPropertyInputSourceID),
              current.hasPrefix(id) else { return nil }
        return current
    }

    private func applyMode(_ mode: String) {
        let style: LanguageMode = mode.hasSuffix(".Traditional") ? .nepali : .transliteration
        guard style != keyboard.currentState.currentLanguage else { return }
        keyboard.setNepaliStyle(style: style)
    }

    // MARK: Strip

    /** Called on the main thread (the controller's scope is the main dispatcher). */
    private func stateChanged(_ state: KeyboardState) {
        guard active else { return }
        let line = cursorLine()
        MainActor.assumeIsolated {
            let strip = StripPanel.shared
            strip.model.onWord = { [weak self] word in self?.pick { $0.keyboard.onSuggestionClick(suggestion: word) } }
            strip.model.onSmart = { [weak self] action in self?.pick { $0.keyboard.onSmartActionClick(action: action) } }
            strip.model.update(state)
            if strip.model.slots.isEmpty { return strip.hide() }
            strip.show(below: line)
        }
    }

    private func pick(_ action: (NepaliInputController) -> Void) {
        action(self)
        keyboard.onCursorMoved(stillComposing: true, hasComposing: !editor.composing.isEmpty, hasSelection: false)
    }

    /** The line the cursor is on, in screen coordinates (zero if the app doesn't say). */
    private func cursorLine() -> NSRect {
        var rect = NSRect.zero
        guard let client = textClient else { return rect }
        // Index 0 is the start of the marked text, or the cursor when nothing is marked
        _ = client.attributes(forCharacterIndex: 0, lineHeightRectangle: &rect)
        return rect
    }
}
