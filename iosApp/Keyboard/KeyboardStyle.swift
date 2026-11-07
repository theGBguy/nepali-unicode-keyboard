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

    /** What a panel (emoji) replaces: the strip and the rows with the gaps between them. */
    static func panelHeight(of state: KeyboardState) -> CGFloat {
        height(of: state) - bottomPadding - rowGap
    }

    /** The whole keyboard: strip, rows and their gaps, bottom padding. */
    static func height(of state: KeyboardState) -> CGFloat {
        let keyHeight = keyHeight(state)
        let rows = state.currentLayout.rows.reduce(CGFloat(0)) { $0 + rowHeight($1, keyHeight: keyHeight) + rowGap }
        return stripHeight + rows + bottomPadding
    }
}

/**
 * Keyboard colours from the shared theme palette (`KeyboardThemes`), the same as Android's.
 * System follows light and dark mode. With [flatKeys] the keys draw no fill or shadow; panels
 * still use [key] and [functionKey] for their tiles.
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
    let dark: Bool
    var flatKeys = false

    init(_ palette: ThemePalette, flatKeys: Bool = false) {
        func color(_ argb: Int64) -> Color { Color(hex: UInt32(truncatingIfNeeded: argb)) }
        background = color(palette.background)
        key = color(palette.key)
        functionKey = color(palette.functionKey)
        keyText = color(palette.keyText)
        hint = color(palette.hint)
        accent = color(palette.accent)
        onAccent = color(palette.onAccent)
        divider = color(palette.hint).opacity(0.2)
        keyShadow = palette.dark || flatKeys ? .clear : Color.black.opacity(0.12)
        preview = color(palette.preview)
        dark = palette.dark
        self.flatKeys = flatKeys
    }


    static func of(_ feel: KeyFeel, _ scheme: ColorScheme) -> KeyboardColors {
        KeyboardColors(KeyboardThemes.shared.palette(theme: feel.theme, systemDark: scheme == .dark),
                       flatKeys: !feel.keyBorders)
    }

    /** What a key draws at rest: nothing for flat keys, except Enter's accent. */
    func face(_ background: Color) -> Color {
        flatKeys && background != accent ? .clear : background
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
