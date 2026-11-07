import SwiftUI
import UIKit

/**
 * The app's only screen. Until the keyboard is added it starts with the setup steps; after that
 * with the settings. The try-it field comes last, so with the keyboard open the settings stay
 * visible above it.
 *
 * Whether the keyboard is added comes from [KeyboardSetup]; if iOS doesn't say, the steps stay
 * until the user taps "I've set it up".
 */
struct SetupView: View {
    @State private var tryText = ""
    @State private var showGuide = false
    @FocusState private var tryFocused: Bool
    @State private var keyboardHeight: CGFloat = 0
    /** Nil when iOS doesn't say whether the keyboard is added. */
    @State private var keyboardAdded = KeyboardSetup.isAdded()
    @AppStorage("setup_confirmed") private var setupConfirmed = false
    /** The user opened the steps again from the link at the bottom. */
    @State private var stepsReopened = false
    @State private var showPrivacy = false
    @AppStorage(SharedSettings.Key.nepaliStyle, store: SharedSettings.group)
    private var style: NepaliStyle = .romanized
    @Environment(\.openURL) private var openURL
    @Environment(\.scenePhase) private var scenePhase

    private var setupDone: Bool {
        keyboardAdded ?? setupConfirmed
    }

    private var showSteps: Bool {
        !setupDone || stepsReopened
    }

    var body: some View {
        ScrollViewReader { scroller in
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                if showSteps {
                    Text("Type Nepali\nin any app")
                        .font(.largeTitle.weight(.bold))
                        .accessibilityAddTraits(.isHeader)
                        .padding(.top, 32)
                    Text("Romanized or traditional layout. Takes about a minute to set up.")
                        .font(.body)
                        .foregroundStyle(.secondary)
                        .padding(.top, 8)
                    steps
                        .padding(.top, 36)
                    privacyPromise
                        .padding(.top, 32)
                } else {
                    Text("Nepali Unicode Keyboard")
                        .font(.largeTitle.weight(.bold))
                        .accessibilityAddTraits(.isHeader)
                        .padding(.top, 32)
                }

                // Same order as Android's Settings: status, Typing, try it, then App, Help, Legal
                if keyboardAdded == true && !showSteps {
                    status.padding(.top, 28)
                }

                TypingSettings(onChange: reopenKeyboard)
                    .padding(.top, showSteps ? 44 : 28)

                tryIt
                    .padding(.top, 12)

                AboutSettings()
                    .padding(.top, 28)

                footer
                    .padding(.top, 24)
                    .padding(.bottom, 24)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 20)
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .scrollDismissesKeyboard(.interactively)
        // There's no navigation bar, so give the status bar a background to scroll under
        .overlay(alignment: .top) {
            Color(uiColor: .systemGroupedBackground).ignoresSafeArea(edges: .top).frame(height: 0)
        }
        // Make room for the keyboard ourselves: SwiftUI's own avoidance doesn't scroll this field
        // into view, and the Nepali keyboard is taller than Apple's
        .safeAreaInset(edge: .bottom, spacing: 0) { Color.clear.frame(height: keyboardHeight) }
        .ignoresSafeArea(.keyboard)
        .onReceive(NotificationCenter.default.publisher(for: UIResponder.keyboardWillChangeFrameNotification)) { note in
            guard let frame = note.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect else { return }
            let screen = UIScreen.main.bounds.height
            keyboardHeight = max(screen - frame.minY, 0)
        }
        // Scroll once the inset for the keyboard's new height is laid out: a third-party keyboard
        // first reports only its bottom strip, then its full height once it's on screen, and a
        // scroll in the same run-loop turn as the inset change still sees the old inset
        .onChange(of: keyboardHeight) { _ in
            guard tryFocused else { return }
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                withAnimation { scroller.scrollTo(Self.tryHint, anchor: .bottom) }
            }
        }
        // Back from Settings: the keyboard may have been added or removed there
        .onChange(of: scenePhase) { phase in
            if phase == .active { keyboardAdded = KeyboardSetup.isAdded() }
        }
        }
    }

    private static let tryHint = "try-hint"

    private var steps: some View {
        VStack(alignment: .leading, spacing: 24) {
            step(1, title: "Turn on the keyboard",
                 detail: "In Settings, tap Keyboards and turn on Nepali Unicode Keyboard.") {
                Button("Open Settings") {
                    if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
                }
                .buttonStyle(.borderedProminent)
                .tint(Color.crimson)
                .padding(.top, 10)
            }
            step(2, title: "Switch to it",
                 detail: "In any text field, hold 🌐 below the keyboard and pick Nepali Unicode Keyboard.") {
                // Only when iOS can't tell us: otherwise the steps hide by themselves
                if keyboardAdded == nil && !setupConfirmed {
                    Button("I’ve set it up") { withAnimation { setupConfirmed = true } }
                        .buttonStyle(.bordered)
                        .tint(Color.crimson)
                        .padding(.top, 10)
                }
            }
            // Android's step 3 is its own screen; here the choice and the field are just below
            step(3, title: "Try it",
                 detail: "Choose how you type Nepali below, then type in the field.") { EmptyView() }
        }
    }

    private var tryIt: some View {
        VStack(alignment: .leading, spacing: 0) {
            TextField(tryPlaceholder, text: $tryText, axis: .vertical)
                .focused($tryFocused)
                .lineLimit(2...4)
                .padding(16)
                .background(Color(uiColor: .secondarySystemGroupedBackground),
                            in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            VStack(alignment: .leading, spacing: 4) {
                if !showSteps {
                    Text("Not showing? Hold 🌐 and pick Nepali Unicode Keyboard.")
                }
                Text("The flag key (🇳🇵 / 🇬🇧) switches between Nepali and English. Hold it to change this choice later.")
            }
            .font(.footnote)
            .foregroundStyle(.secondary)
            .padding(.top, 8)
            .padding(.horizontal, 16)
            .id(Self.tryHint)
            if showSteps && style == .romanized {
                CompactGuide { showGuide = true }
                    .padding(.top, 16)
                    .sheet(isPresented: $showGuide) { TypingGuideView() }
            }
        }
    }

    /** Android's setup hints while the steps show, its Settings hint after. */
    private var tryPlaceholder: LocalizedStringKey {
        guard showSteps else { return "Try your settings here" }
        return style == .traditional ? "Type नमस्ते" : "Type “namaste” and press space"
    }

    /** The same promise as Android's setup screen, told for iOS, with the same link to the details. */
    private var privacyPromise: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Your typing stays on this iPhone")
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.secondary)
                .padding(.bottom, 2)
            promiseLine("wifi.slash", "What you type never leaves the iPhone. Only handwriting, if you turn it on, connects to Google.")
            promiseLine("lock", "Password fields always use Apple’s keyboard, so this one never sees them.")
            Button("How we handle your data") { showPrivacy = true }
                .font(.subheadline)
                .tint(.secondary)
                .padding(.top, 2)
        }
        .sheet(isPresented: $showPrivacy) { PrivacyView() }
    }

    private func promiseLine(_ icon: String, _ text: LocalizedStringKey) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 10) {
            Image(systemName: icon)
                .font(.footnote)
                .frame(width: 18)
                .accessibilityHidden(true)
            Text(text).font(.subheadline)
        }
    }

    /** Like Android's status row: shown once iOS says the keyboard is added. */
    private var status: some View {
        SettingsGroup {
            RowLabel(icon: "keyboard", title: "Keyboard", subtitle: "Added", subtitleColor: .green)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .frame(maxWidth: .infinity, minHeight: 48, alignment: .leading)
        }
    }

    /** Privacy and source links live in Help and Legal; only the way back to the steps stays here. */
    @ViewBuilder
    private var footer: some View {
        if setupDone && !stepsReopened {
            Button("Setup steps") { withAnimation { stepsReopened = true } }
                .font(.footnote)
                .tint(.secondary)
                .frame(maxWidth: .infinity)
        }
    }

    /**
     * The keyboard reads settings when it opens (iOS doesn't let the app tell an open keyboard
     * reliably), so close and reopen it to show a change in the try-it field straight away.
     */
    private func reopenKeyboard() {
        guard tryFocused else { return }
        tryFocused = false
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) { tryFocused = true }
    }

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
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 4) {
                Text(title).font(.headline)
                    .accessibilityLabel(Text("Step \(number): \(Text(title))"))
                    .accessibilityAddTraits(.isHeader)
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
