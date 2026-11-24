import Foundation
import NepaliKeyboardShared

/**
 * Keyboard settings on the Mac. Only smart suggestions can be changed (in [SettingsView]); the
 * rest have fixed values. Key names match Android's and iOS's.
 *
 * - Nepali is always on: English is another input source (ABC), picked in the input menu.
 * - The style follows the selected input source ([NepaliInputController] sets it on every switch),
 *   so it isn't stored.
 */
final class MacKeyboardPrefs: NSObject, KeyboardPrefs {
    /** One for the process, so every app's session sees the same style and settings. */
    static let shared = MacKeyboardPrefs()

    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    var nepaliStyle: LanguageMode = .transliteration

    var typeNepali: Bool {
        get { true }
        set {}
    }

    var smartSuggestions: Bool {
        get { bool("smart_suggestions") }
        set { defaults.set(newValue, forKey: "smart_suggestions") }
    }

    private func bool(_ key: String) -> Bool { defaults.object(forKey: key) as? Bool ?? true }

    // MARK: Unused on the Mac: on-screen keys, and English (the system's ABC source)

    var autoCapitalise = false
    var wordSuggestions = false

    var keyHeight: KeyHeight = .normal
    var vibrate = false
    var sound = false
    var theme: KeyboardTheme = .system
    var keyBorders = true
    var emojiRecents: [String] = []
    var clipboardHistory = false
    var handwriting = false

    var feel: KeyFeel {
        KeyFeel(height: keyHeight, vibrate: vibrate, sound: sound, theme: theme, keyBorders: keyBorders)
    }
}
