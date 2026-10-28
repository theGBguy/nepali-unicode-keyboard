import Foundation
import NepaliKeyboardShared

/**
 * Keyboard settings in the extension's own defaults. Nepali by default, like Android. They move to
 * the App Group's defaults when the app gets a Settings screen.
 */
final class DefaultsKeyboardPrefs: NSObject, KeyboardPrefs {
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    var nepaliStyle: LanguageMode {
        get { defaults.string(forKey: "nepali_style") == "traditional" ? .nepali : .transliteration }
        set { defaults.set(newValue == .nepali ? "traditional" : "romanized", forKey: "nepali_style") }
    }

    var typeNepali: Bool {
        get { defaults.object(forKey: "type_nepali") as? Bool ?? true }
        set { defaults.set(newValue, forKey: "type_nepali") }
    }

    var keyHeight: KeyHeight {
        get {
            let name = defaults.string(forKey: "key_height")
            return KeyHeight.entries.first { $0.name == name } ?? .normal
        }
        set { defaults.set(newValue.name, forKey: "key_height") }
    }

    var vibrate: Bool {
        get { defaults.object(forKey: "vibrate") as? Bool ?? true }
        set { defaults.set(newValue, forKey: "vibrate") }
    }

    var sound: Bool {
        get { defaults.bool(forKey: "sound") }
        set { defaults.set(newValue, forKey: "sound") }
    }

    var emojiRecents: [String] {
        get { EmojiRecents.shared.decode(stored: defaults.string(forKey: "emoji_recents")) }
        set { defaults.set(EmojiRecents.shared.encode(recents: newValue), forKey: "emoji_recents") }
    }

    var clipboardHistory: Bool {
        get { defaults.object(forKey: "clipboard_history") as? Bool ?? true }
        set { defaults.set(newValue, forKey: "clipboard_history") }
    }

    var smartSuggestions: Bool {
        get { defaults.object(forKey: "smart_suggestions") as? Bool ?? true }
        set { defaults.set(newValue, forKey: "smart_suggestions") }
    }

    var handwriting: Bool {
        get { defaults.bool(forKey: "handwriting") }
        set { defaults.set(newValue, forKey: "handwriting") }
    }

    var feel: KeyFeel {
        KeyFeel(height: keyHeight, vibrate: vibrate, sound: sound)
    }
}
