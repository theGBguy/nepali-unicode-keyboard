import Foundation

/** Whether the user has added the keyboard, read from the system's keyboard list. */
enum KeyboardSetup {
    /**
     * Whether Nepali Keyboard is in Settings → General → Keyboard → Keyboards, or nil when iOS
     * doesn't say. `AppleKeyboards` is the system's list of added keyboards, which every app sees
     * through the global defaults domain. It isn't documented, so nil (key missing) falls back to
     * asking the user. Re-check when the app comes to the front: the user may have just changed it.
     */
    static func isAdded(
        defaults: UserDefaults = .standard,
        appID: String? = Bundle.main.bundleIdentifier
    ) -> Bool? {
        guard let appID, let keyboards = defaults.stringArray(forKey: "AppleKeyboards") else { return nil }
        return keyboards.contains { $0.hasPrefix(appID + ".") }
    }
}
