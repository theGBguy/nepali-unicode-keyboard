import Foundation

/**
 * Settings the app and the keyboard share through the App Group. The app writes them; the
 * keyboard reads them each time it opens. Without Full Access iOS gives the keyboard read-only
 * access to the group (its writes are silently dropped), so anything the keyboard itself changes
 * is stored in its own defaults as well (see the keyboard's `DefaultsKeyboardPrefs`).
 *
 * Keys and values match Android's SharedPreferences, so the shared Kotlin code sees the same names.
 */
enum SharedSettings {
    static let groupID = "group.io.github.thegbguy.nepalikeyboard"

    /** The group's defaults; the app's own defaults if the group isn't available (unsigned builds). */
    static let group: UserDefaults = UserDefaults(suiteName: groupID) ?? .standard

    enum Key {
        static let nepaliStyle = "nepali_style"
        /** When [nepaliStyle] was last set, in seconds since 1970, so the newer of two copies wins. */
        static let nepaliStyleTime = "nepali_style_time"
        static let keyHeight = "key_height"
        static let smartSuggestions = "smart_suggestions"
        static let sound = "sound"
        static let vibrate = "vibrate"
        static let clipboardHistory = "clipboard_history"
    }

    /**
     * The keyboard's clipboard history (Android's text format). Only the keyboard writes it, and
     * only with Full Access; the app can delete it.
     */
    static var clipboardFile: URL? {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: groupID)?
            .appendingPathComponent("clipboard.txt")
    }

    static func clearClipboardHistory() {
        guard let file = clipboardFile else { return }
        try? FileManager.default.removeItem(at: file)
    }

    static var hasClipboardHistory: Bool {
        guard let file = clipboardFile else { return false }
        return FileManager.default.fileExists(atPath: file.path)
    }

    /**
     * iOS defaults, which differ from Android's: the click plays only when the system's Keyboard
     * Feedback → Sound is on too, like Apple's keyboard; haptics start off, like Keyboard
     * Feedback → Haptic.
     */
    static let defaultSound = true
    static let defaultVibrate = false
}

/** How Nepali is typed; stored as Android's values. */
enum NepaliStyle: String, CaseIterable {
    case romanized
    case traditional
}

/** Key height presets; stored as the shared `KeyHeight` enum's names. */
enum KeySize: String, CaseIterable {
    case short = "SHORT"
    case normal = "NORMAL"
    case tall = "TALL"
}
