import SwiftUI
import NepaliKeyboardShared

/** Sizes, in points, matching Android's dp values. */
enum KeyboardMetrics {
    static let keyGap: CGFloat = 5
    static let rowGap: CGFloat = 7
    static let stripHeight: CGFloat = 40
    static let sidePadding: CGFloat = 3
    static let bottomPadding: CGFloat = 4
    static let keyRadius: CGFloat = 7
    /** The digit row is this much shorter than the letter rows. */
    static let numberRowInset: CGFloat = 6

    static func isNumberRow(_ row: [KeyData]) -> Bool {
        row.allSatisfy { $0.keyType == .character && $0.primary.count == 1 && $0.primary.first!.isNumber }
    }

    static func keyHeight(_ state: KeyboardState) -> CGFloat {
        CGFloat(state.feel.height.dp)
    }

    static func rowHeight(_ row: [KeyData], keyHeight: CGFloat) -> CGFloat {
        isNumberRow(row) ? keyHeight - numberRowInset : keyHeight
    }

    /** The whole keyboard: strip, rows and their gaps, bottom padding. */
    static func height(of state: KeyboardState) -> CGFloat {
        let keyHeight = keyHeight(state)
        let rows = state.currentLayout.rows.reduce(CGFloat(0)) { $0 + rowHeight($1, keyHeight: keyHeight) + rowGap }
        return stripHeight + rows + bottomPadding
    }
}

/**
 * Android's palette: warm neutrals with Nepal crimson as the only accent (Enter, active shift),
 * following light and dark mode.
 */
struct KeyboardColors {
    let background: Color
    let key: Color
    let functionKey: Color
    let keyText: Color
    let hint: Color
    let accent: Color
    let onAccent: Color
    let divider: Color
    let keyShadow: Color
    let preview: Color

    static let light = KeyboardColors(
        background: Color(hex: 0xECEAE7),
        key: .white,
        functionKey: Color(hex: 0xD6D3CF),
        keyText: Color(hex: 0x1C1B1A),
        hint: Color(hex: 0x8D8883),
        accent: Color(hex: 0xB3122E),
        onAccent: .white,
        divider: Color(hex: 0x8D8883).opacity(0.2),
        keyShadow: Color.black.opacity(0.12),
        preview: .white
    )

    static let dark = KeyboardColors(
        background: Color(hex: 0x161514),
        key: Color(hex: 0x34312F),
        functionKey: Color(hex: 0x262423),
        keyText: Color(hex: 0xEEECE9),
        hint: Color(hex: 0x9A948E),
        accent: Color(hex: 0xC8263F),
        onAccent: .white,
        divider: Color(hex: 0x9A948E).opacity(0.2),
        keyShadow: .clear,
        preview: Color(hex: 0x45413E)
    )

    static func of(_ scheme: ColorScheme) -> KeyboardColors {
        scheme == .dark ? dark : light
    }
}

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}
