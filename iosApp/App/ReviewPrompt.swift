import Foundation

/**
 * When the app asks for an App Store review: once, on the first launch that starts with the
 * keyboard already set up. The launch where setup is finished doesn't count, so people come back
 * after typing with the keyboard. Never from the keyboard. Mirrors the shared Kotlin
 * `ReviewPrompt` (the app target doesn't link the shared framework); iOS decides whether the
 * sheet appears and limits it to three times a year.
 */
enum ReviewPrompt {
    private static let opensKey = "review_opens"
    private static let askedKey = "review_asked"
    static let opensBeforeAsking = 1

    /** Call once per launch. Returns true when this launch should ask. */
    static func onLaunch(setupDone: Bool, defaults: UserDefaults = .standard) -> Bool {
        guard !defaults.bool(forKey: askedKey) else { return false }
        if setupDone { defaults.set(defaults.integer(forKey: opensKey) + 1, forKey: opensKey) }
        return defaults.integer(forKey: opensKey) >= opensBeforeAsking
    }

    static func markAsked(defaults: UserDefaults = .standard) {
        defaults.set(true, forKey: askedKey)
    }
}
