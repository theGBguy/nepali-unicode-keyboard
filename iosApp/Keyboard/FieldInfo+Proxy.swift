import UIKit
import NepaliKeyboardShared

extension FieldInfo {
    /**
     * The shared field description for what the app's text field asks for. iOS uses its own
     * keyboard for passwords and phone pads, so those never reach this keyboard.
     */
    static func from(_ traits: UITextDocumentProxy) -> FieldInfo {
        let kind: FieldKind
        switch traits.keyboardType ?? .default {
        case .emailAddress: kind = .email
        case .URL, .webSearch: kind = .uri
        case .numberPad, .decimalPad, .asciiCapableNumberPad: kind = .number
        case .phonePad, .namePhonePad: kind = .phone
        default: kind = .text
        }
        let isPrivate = traits.isSecureTextEntry ?? false
        let enter: EnterAction
        switch traits.returnKeyType ?? .default {
        case .go, .route, .join: enter = .go
        case .search, .google, .yahoo: enter = .search
        case .send: enter = .send
        case .next, .continue: enter = .next
        case .done: enter = .done
        default: enter = .newline
        }
        return FieldInfo(
            kind: isPrivate ? .password : kind,
            isPrivate: isPrivate,
            noLearning: isPrivate,
            enterAction: enter,
            imeActionId: FieldInfo.companion.IME_ACTION_NONE,
            actionLabel: nil,
            sendsKeyEvents: false
        )
    }
}
