import AudioToolbox
import UIKit

/**
 * The click and tap on key press (checked on an iPhone 14, iOS 26):
 * - the click is `playInputClick()`, which iOS plays only when Settings → Sounds & Haptics →
 *   Keyboard Feedback → Sound is on and the ringer isn't muted, like Apple's keyboard. It needs
 *   the input view to adopt `UIInputViewAudioFeedback` (below). Works without Full Access;
 * - the tap is a light `UIImpactFeedbackGenerator` impact with Full Access. Without it the
 *   generator does nothing in a keyboard, so it falls back to system sound 1519, a short "peek"
 *   vibration.
 */
@MainActor
enum KeyFeedback {
    private static let peek: SystemSoundID = 1519
    private static var impact: UIImpactFeedbackGenerator?

    static func keyDown(sound: Bool, vibrate: Bool, fullAccess: Bool) {
        if sound { UIDevice.current.playInputClick() }
        guard vibrate else { return }
        if fullAccess {
            let generator = impact ?? UIImpactFeedbackGenerator(style: .light)
            impact = generator
            generator.impactOccurred()
            // Keeps the Taptic Engine ready for the next key
            generator.prepare()
        } else {
            AudioServicesPlaySystemSound(peek)
        }
    }
}

/**
 * Lets the keyboard's root view (the `UIInputView` iOS creates for the controller) play input
 * clicks. Replacing that view with a subclass in `loadView` broke the host app's keyboard frame
 * notifications, so the system's own view adopts the protocol instead; it's the only input view
 * in this process.
 */
extension UIInputView: @retroactive UIInputViewAudioFeedback {
    public var enableInputClicksWhenVisible: Bool { true }
}
