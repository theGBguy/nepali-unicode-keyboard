import SwiftUI

/**
 * Three columns of mini keyboards, one per theme, like Android's Settings. The try-it field below
 * shows the real keyboard; the colours come from the generated `ThemeSwatches.swift`.
 */
struct ThemePicker: View {
    @Binding var selection: KeyboardThemeChoice
    let keyBorders: Bool
    @Environment(\.colorScheme) private var scheme

    var body: some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 10), count: 3), spacing: 10) {
            ForEach(KeyboardThemeChoice.allCases) { theme in
                Button { selection = theme } label: {
                    VStack(spacing: 4) {
                        MiniKeyboard(swatch: theme.swatch(dark: scheme == .dark), keyBorders: keyBorders)
                            .padding(3)
                            .overlay(
                                RoundedRectangle(cornerRadius: 11, style: .continuous)
                                    .stroke(selection == theme ? Color.crimson : .clear, lineWidth: 2)
                            )
                        Text(theme.title)
                            .font(.caption)
                            .foregroundStyle(.primary)
                    }
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(selection == theme ? [.isButton, .isSelected] : .isButton)
            }
        }
    }
}

private struct MiniKeyboard: View {
    let swatch: ThemeSwatch
    let keyBorders: Bool

    var body: some View {
        let key = keyBorders ? Color(hex: swatch.key) : .clear
        let functionKey = keyBorders ? Color(hex: swatch.functionKey) : .clear
        // Flat keys have no face: small marks stand in for the letters
        let mark: Color? = keyBorders ? nil : Color(hex: swatch.keyText).opacity(0.45)
        VStack(spacing: 3) {
            HStack(spacing: 2) { ForEach(0..<7, id: \.self) { _ in MiniKey(face: key, mark: mark) } }
            HStack(spacing: 2) { ForEach(0..<6, id: \.self) { _ in MiniKey(face: key, mark: mark) } }
            HStack(spacing: 2) {
                MiniKey(face: functionKey, mark: mark)
                MiniKey(face: key, mark: nil).frame(maxWidth: .infinity).layoutPriority(3)
                MiniKey(face: Color(hex: swatch.accent), mark: nil).frame(width: 22)
            }
        }
        .padding(4)
        .frame(height: 52)
        .background(Color(hex: swatch.background), in: RoundedRectangle(cornerRadius: 9, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 9, style: .continuous).stroke(Color(uiColor: .separator), lineWidth: 0.5))
    }
}

private struct MiniKey: View {
    let face: Color
    let mark: Color?

    var body: some View {
        RoundedRectangle(cornerRadius: 2)
            .fill(face)
            .overlay {
                if let mark {
                    Capsule().fill(mark).frame(height: 2).padding(.horizontal, 3)
                }
            }
    }
}

extension KeyboardThemeChoice {
    var title: LocalizedStringKey {
        switch self {
        case .system: return "System"
        case .light: return "Light"
        case .dark: return "Dark"
        case .himalaya: return "Himalaya"
        case .laliGurans: return "Lali Gurans"
        case .terai: return "Terai"
        case .midnight: return "Midnight"
        case .dusk: return "Dusk"
        case .paper: return "Paper"
        }
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
