import AudioToolbox
import UIKit

/**
 * The click and tap on key press. Both work without Full Access (checked on an iPhone 14, iOS 26):
 * - the click is `playInputClick()`, which iOS plays only when Settings → Sounds & Haptics →
 *   Keyboard Feedback → Sound is on and the ringer isn't muted, like Apple's keyboard. It needs
 *   the input view to adopt `UIInputViewAudioFeedback` (below);
 * - the tap is system sound 1519, a short "peek" vibration. `UIImpactFeedbackGenerator` does
 *   nothing in a keyboard without Full Access; use it instead once the keyboard asks for that.
 */
enum KeyFeedback {
    private static let peek: SystemSoundID = 1519

    static func keyDown(sound: Bool, vibrate: Bool) {
        if sound { UIDevice.current.playInputClick() }
        if vibrate { AudioServicesPlaySystemSound(peek) }
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
