import Carbon
import Cocoa
import NepaliKeyboardShared

/** The Mac's key events → the shared router's [HardKeyEvent]. */
enum MacKeys {
    static func event(_ event: NSEvent) -> HardKeyEvent {
        let flags = event.modifierFlags.intersection(.deviceIndependentFlagsMask)
        let code = Int(event.keyCode)
        return HardKeyEvent(
            key: named[code] ?? .character,
            iso: isoPositions[code],
            text: text(event),
            shift: flags.contains(.shift),
            ctrl: flags.contains(.control),
            meta: flags.contains(.command),
            alt: false,
            // Option is the third-level shift, like AltGr in the Windows layout the table follows
            altGr: flags.contains(.option),
            repeat: event.isARepeat
        )
    }

    /**
     * What the key types (with Shift and Caps Lock), or nil. Arrow, function and other special
     * keys report characters from Apple's private-use range (U+F700…), which aren't text.
     */
    private static func text(_ event: NSEvent) -> String? {
        guard let chars = event.characters, !chars.isEmpty else { return nil }
        if chars.unicodeScalars.contains(where: { (0xF700...0xF8FF).contains($0.value) }) { return nil }
        return chars
    }

    // Virtual key codes from Carbon's Events.h (kVK_…); they name physical keys, like ISO positions
    private static let named: [Int: HardKey] = [
        kVK_Space: .space,
        kVK_Return: .enter,
        kVK_ANSI_KeypadEnter: .enter,
        kVK_Delete: .backspace,
        kVK_Tab: .tab,
        kVK_LeftArrow: .left,
        kVK_RightArrow: .right,
        kVK_Escape: .escape,
    ]

    /** The keys the traditional layout uses, by ISO position (`D01` is Q). */
    static let isoPositions: [Int: String] = {
        var map: [Int: String] = [
            // Left of 1: kVK_ANSI_Grave on ANSI keyboards, kVK_ISO_Section on ISO ones
            kVK_ANSI_Grave: "E00", kVK_ISO_Section: "E00",
            kVK_ANSI_Minus: "E11", kVK_ANSI_Equal: "E12",
            kVK_ANSI_LeftBracket: "D11", kVK_ANSI_RightBracket: "D12",
            kVK_ANSI_Semicolon: "C10", kVK_ANSI_Quote: "C11", kVK_ANSI_Backslash: "C12",
            kVK_ANSI_Comma: "B08", kVK_ANSI_Period: "B09", kVK_ANSI_Slash: "B10",
        ]
        let rows: [(String, [Int])] = [
            ("E", [kVK_ANSI_1, kVK_ANSI_2, kVK_ANSI_3, kVK_ANSI_4, kVK_ANSI_5,
                   kVK_ANSI_6, kVK_ANSI_7, kVK_ANSI_8, kVK_ANSI_9, kVK_ANSI_0]),
            ("D", [kVK_ANSI_Q, kVK_ANSI_W, kVK_ANSI_E, kVK_ANSI_R, kVK_ANSI_T,
                   kVK_ANSI_Y, kVK_ANSI_U, kVK_ANSI_I, kVK_ANSI_O, kVK_ANSI_P]),
            ("C", [kVK_ANSI_A, kVK_ANSI_S, kVK_ANSI_D, kVK_ANSI_F, kVK_ANSI_G,
                   kVK_ANSI_H, kVK_ANSI_J, kVK_ANSI_K, kVK_ANSI_L]),
            ("B", [kVK_ANSI_Z, kVK_ANSI_X, kVK_ANSI_C, kVK_ANSI_V, kVK_ANSI_B,
                   kVK_ANSI_N, kVK_ANSI_M]),
        ]
        for (row, codes) in rows {
            for (i, code) in codes.enumerated() { map[code] = row + String(format: "%02d", i + 1) }
        }
        return map
    }()
}
