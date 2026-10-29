import Foundation
import NepaliKeyboardShared

/**
 * Keyboard settings. The app's Settings write to the App Group, which the keyboard can only read
 * without Full Access: its writes are dropped silently, though this process's defaults cache
 * still shows them. So:
 * - key height and smart suggestions come from the group;
 * - Nepali style is set in two places (the app's Settings and the keyboard's language menu). Each
 *   copy has a timestamp and the newer one wins. With Full Access the keyboard also writes the
 *   group, so the app shows the keyboard's choice;
 * - what only the keyboard changes (current language, emoji recents...) stays in its own defaults.
 */
final class DefaultsKeyboardPrefs: NSObject, KeyboardPrefs {
    private let local: UserDefaults
    private let group: UserDefaults
    private let canWriteGroup: () -> Bool
    private let now: () -> Date

    init(
        local: UserDefaults = .standard,
        group: UserDefaults = SharedSettings.group,
        canWriteGroup: @escaping () -> Bool,
        now: @escaping () -> Date = Date.init
    ) {
        self.local = local
        self.group = group
        self.canWriteGroup = canWriteGroup
        self.now = now
    }

    // MARK: Shared with the app

    var nepaliStyle: LanguageMode {
        get {
            let key = SharedSettings.Key.nepaliStyle
            let time = SharedSettings.Key.nepaliStyleTime
            let fromApp = group.string(forKey: key)
            let fromKeyboard = local.string(forKey: key)
            let style = local.double(forKey: time) > group.double(forKey: time) ? fromKeyboard : (fromApp ?? fromKeyboard)
            return style == NepaliStyle.traditional.rawValue ? .nepali : .transliteration
        }
        set {
            let value = (newValue == .nepali ? NepaliStyle.traditional : .romanized).rawValue
            let time = now().timeIntervalSince1970
            for defaults in canWriteGroup() ? [local, group] : [local] {
                defaults.set(value, forKey: SharedSettings.Key.nepaliStyle)
                defaults.set(time, forKey: SharedSettings.Key.nepaliStyleTime)
            }
        }
    }

    var keyHeight: KeyHeight {
        get {
            let name = group.string(forKey: SharedSettings.Key.keyHeight)
            return KeyHeight.entries.first { $0.name == name } ?? .normal
        }
        // The app sets it; the keyboard never does
        set { if canWriteGroup() { group.set(newValue.name, forKey: SharedSettings.Key.keyHeight) } }
    }

    var smartSuggestions: Bool {
        get { group.object(forKey: SharedSettings.Key.smartSuggestions) as? Bool ?? true }
        set { if canWriteGroup() { group.set(newValue, forKey: SharedSettings.Key.smartSuggestions) } }
    }

    // MARK: The keyboard's own

    var typeNepali: Bool {
        get { local.object(forKey: "type_nepali") as? Bool ?? true }
        set { local.set(newValue, forKey: "type_nepali") }
    }

    var vibrate: Bool {
        get { local.object(forKey: "vibrate") as? Bool ?? true }
        set { local.set(newValue, forKey: "vibrate") }
    }

    var sound: Bool {
        get { local.bool(forKey: "sound") }
        set { local.set(newValue, forKey: "sound") }
    }

    var emojiRecents: [String] {
        get { EmojiRecents.shared.decode(stored: local.string(forKey: "emoji_recents")) }
        set { local.set(EmojiRecents.shared.encode(recents: newValue), forKey: "emoji_recents") }
    }

    var clipboardHistory: Bool {
        get { local.object(forKey: "clipboard_history") as? Bool ?? true }
        set { local.set(newValue, forKey: "clipboard_history") }
    }

    var handwriting: Bool {
        get { local.bool(forKey: "handwriting") }
        set { local.set(newValue, forKey: "handwriting") }
    }

    var feel: KeyFeel {
        KeyFeel(height: keyHeight, vibrate: vibrate, sound: sound)
    }
}
