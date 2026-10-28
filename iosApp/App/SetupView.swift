import SwiftUI
import UIKit

/**
 * First screen: how to turn the keyboard on, switch to it and try it. iOS doesn't tell an app
 * whether its keyboard is on, so all three steps stay visible.
 */
struct SetupView: View {
    @State private var tryText = ""
    @FocusState private var tryFocused: Bool
    @State private var keyboardHeight: CGFloat = 0
    @Environment(\.openURL) private var openURL

    private static let privacyPolicy = URL(string: "https://nepali-indie-dev.github.io/nepali-keyboard/privacy-policy.html")!

    var body: some View {
        ScrollViewReader { scroller in
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                Text("Type Nepali\nin any app")
                    .font(.largeTitle.weight(.bold))
                    .padding(.top, 32)
                Text("Romanized or traditional layout. Takes about a minute to set up.")
                    .font(.body)
                    .foregroundStyle(.secondary)
                    .padding(.top, 8)

                VStack(alignment: .leading, spacing: 24) {
                    step(1, title: "Turn on the keyboard",
                         detail: "In Settings, tap Keyboards and turn on Nepali Keyboard.") {
                        Button("Open Settings") {
                            if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(Color.crimson)
                        .padding(.top, 10)
                    }
                    step(2, title: "Switch to it",
                         detail: "In any text field, hold 🌐 below the keyboard and pick Nepali Keyboard.") { EmptyView() }
                    step(3, title: "Try it", detail: "Type “namaste” and press space") {
                        TextField("", text: $tryText, axis: .vertical)
                            .focused($tryFocused)
                            .lineLimit(1...4)
                            .padding(12)
                            .background(RoundedRectangle(cornerRadius: 10).stroke(Color(uiColor: .separator)))
                            .padding(.top, 10)
                            .id(Self.tryField)
                        Text("The अ/A key switches between Nepali and English. Hold it to change this choice later.")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                            .padding(.top, 8)
                            .id(Self.tryHint)
                    }
                }
                .padding(.top, 36)

                Button("How we handle your data") { openURL(Self.privacyPolicy) }
                    .font(.footnote)
                    .tint(.secondary)
                    .padding(.top, 40)
                    .padding(.bottom, 24)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 24)
        }
        .scrollDismissesKeyboard(.interactively)
        // Keep the field above the keyboard, which is taller than Apple's
        // Make room for the keyboard ourselves: SwiftUI's own avoidance doesn't scroll this field
        // into view, and the Nepali keyboard is taller than Apple's
        .safeAreaInset(edge: .bottom, spacing: 0) { Color.clear.frame(height: keyboardHeight) }
        .ignoresSafeArea(.keyboard)
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillChangeFrameNotification)) { note in
            guard let frame = note.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect else { return }
            let screen = UIScreen.main.bounds.height
            keyboardHeight = max(screen - frame.minY, 0)
        }
        // Also after it resizes: a third-party keyboard sets its height only once it's on screen
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardDidChangeFrameNotification)) { _ in
            guard tryFocused else { return }
            withAnimation { scroller.scrollTo(Self.tryHint, anchor: .bottom) }
        }
        }
    }

    private static let tryField = "try-field"
    private static let tryHint = "try-hint"

    private func step<Extra: View>(
        _ number: Int,
        title: LocalizedStringKey,
        detail: LocalizedStringKey,
        @ViewBuilder extra: () -> Extra
    ) -> some View {
        HStack(alignment: .top, spacing: 14) {
            Text(verbatim: "\(number)")
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(.secondary)
                .frame(width: 28, height: 28)
                .overlay(Circle().stroke(Color(uiColor: .separator)))
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.headline)
                Text(detail).font(.subheadline).foregroundStyle(.secondary)
                extra()
            }
        }
    }
}

extension Color {
    /** Nepal crimson, the keyboard's accent. */
    static let crimson = Color(red: 0xB3 / 255, green: 0x12 / 255, blue: 0x2E / 255)
}

#Preview {
    SetupView()
}
