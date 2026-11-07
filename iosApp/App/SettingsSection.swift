import SwiftUI
import UIKit

/**
 * The keyboard's typing settings, stored in the App Group for the keyboard to read when it next
 * opens. [onChange] runs after each change (the screen reopens the keyboard to show it).
 *
 * The screen matches Android's Settings: Status, Typing, the try-it field, then App, Help and
 * Legal, with the same rows in the same order. It keeps the iOS look: inset grouped sections and
 * SF Symbols.
 */
struct TypingSettings: View {
    var onChange: () -> Void = {}

    @AppStorage(SharedSettings.Key.nepaliStyle, store: SharedSettings.group)
    private var style: NepaliStyle = .romanized
    @AppStorage(SharedSettings.Key.keyHeight, store: SharedSettings.group)
    private var keySize: KeySize = .normal
    @AppStorage(SharedSettings.Key.smartSuggestions, store: SharedSettings.group)
    private var smartSuggestions = true
    @AppStorage(SharedSettings.Key.sound, store: SharedSettings.group)
    private var sound = SharedSettings.defaultSound
    @AppStorage(SharedSettings.Key.vibrate, store: SharedSettings.group)
    private var vibrate = SharedSettings.defaultVibrate
    @AppStorage(SharedSettings.Key.clipboardHistory, store: SharedSettings.group)
    private var clipboardHistory = true
    @AppStorage(SharedSettings.Key.handwriting, store: SharedSettings.group)
    private var handwriting = false
    @State private var confirmHandwriting = false
    @State private var confirmClear = false
    @State private var showGuide = false
    @Environment(\.openURL) private var openURL

    var body: some View {
        SettingsGroup("Typing") {
            VStack(alignment: .leading, spacing: 8) {
                Text("Nepali typing")
                Picker("Nepali typing", selection: styleBinding) {
                    Text("Romanized").tag(NepaliStyle.romanized)
                    Text("Traditional").tag(NepaliStyle.traditional)
                }
                .pickerStyle(.segmented)
                .labelsHidden()
            }
            .padding(16)
            RowDivider()
            LinkRow(icon: "book", title: "Romanized typing guide", subtitle: "What to type for each letter") { showGuide = true }
                .sheet(isPresented: $showGuide) { TypingGuideView() }
            RowDivider(inset: 16)
            VStack(alignment: .leading, spacing: 8) {
                Text("Key height")
                Picker("Key height", selection: $keySize) {
                    Text("Short").tag(KeySize.short)
                    Text("Normal").tag(KeySize.normal)
                    Text("Tall").tag(KeySize.tall)
                }
                .pickerStyle(.segmented)
                .labelsHidden()
            }
            .padding(16)
            RowDivider(inset: 16)
            SwitchRow(icon: "iphone.radiowaves.left.and.right", title: "Vibrate on key press", isOn: $vibrate)
            RowDivider()
            // playInputClick() is silent unless the system setting is on as well
            SwitchRow(icon: "speaker.wave.2", title: "Sound on key press",
                      subtitle: "When keyboard sound is on in iPhone Settings", isOn: $sound)
            RowDivider()
            SwitchRow(icon: "lightbulb", title: "Smart suggestions",
                      subtitle: "Dates, amounts and units as you type", isOn: $smartSuggestions)
            RowDivider()
            handwritingRow
            RowDivider()
            clipboard
        }
        .onChange(of: keySize) { _ in onChange() }
        .onChange(of: smartSuggestions) { _ in onChange() }
        .onChange(of: sound) { _ in onChange() }
        .onChange(of: vibrate) { _ in onChange() }
        .onChange(of: clipboardHistory) { _ in onChange() }
    }

    /**
     * Clipboard history is the one feature that needs Full Access: without it iOS hides the
     * pasteboard from keyboards. The app can't tell whether it's on, so the row just opens the
     * app's page in iPhone Settings (Keyboards → Allow Full Access).
     */
    @ViewBuilder
    private var clipboard: some View {
        SwitchRow(icon: "doc.on.clipboard", title: "Clipboard history",
                  subtitle: "Needs Full Access. Kept on this iPhone only.", isOn: $clipboardHistory)
        RowDivider()
        LinkRow(icon: "lock.open", title: "Full Access") {
            if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
        }
        RowDivider()
        LinkRow(icon: "trash", title: "Clear clipboard history", showsChevron: false) { confirmClear = true }
            .alert("Clear clipboard history?", isPresented: $confirmClear) {
                Button("Clear", role: .destructive) { SharedSettings.clearClipboardHistory() }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("This removes every saved item, including pinned ones.")
            }
    }

    /**
     * Like Android's Settings switch: turning it on asks first (Google model download and ML Kit
     * diagnostics); turning it off makes the keyboard delete the model when it next opens.
     */
    private var handwritingRow: some View {
        SwitchRow(
            icon: "pencil.and.scribble",
            title: "Handwriting (uses Google)",
            subtitle: "Needs Full Access for the model download",
            isOn: Binding(
                get: { handwriting },
                set: { on in
                    if on { confirmHandwriting = true } else { handwriting = false; onChange() }
                }
            )
        )
        .alert("Turn on handwriting?", isPresented: $confirmHandwriting) {
            Button("Turn on") { handwriting = true; onChange() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Writing is recognised on this iPhone. Turning it on downloads Google’s Nepali handwriting model (about 15 MB) once, and Google’s ML Kit then sends Google usage diagnostics such as phone model and app version, never what you write. Turning it off here deletes the model.")
        }
    }

    /** Also stamps the time, so this choice beats an older one made in the keyboard's menu. */
    private var styleBinding: Binding<NepaliStyle> {
        Binding(
            get: { style },
            set: { newValue in
                style = newValue
                SharedSettings.group.set(Date().timeIntervalSince1970, forKey: SharedSettings.Key.nepaliStyleTime)
                onChange()
            }
        )
    }
}

/** App, Help and Legal: the same rows as Android's Settings, minus Rate until the app is on the App Store. */
struct AboutSettings: View {
    @State private var page: InfoPage?
    @Environment(\.openURL) private var openURL

    static let supportEmail = "nepali.indie.dev@gmail.com"
    static let privacyPolicy = URL(string: "https://nepali-indie-dev.github.io/nepali-keyboard/privacy-policy.html")!
    static let terms = URL(string: "https://nepali-indie-dev.github.io/nepali-keyboard/terms-of-service.html")!
    static let sourceCode = URL(string: "https://github.com/theGBguy/nepali-unicode-keyboard")!

    var body: some View {
        VStack(spacing: 0) {
            SettingsGroup("App") {
                // iOS keeps each app's language in the app's page in iPhone Settings
                LinkRow(icon: "globe", title: "App language", value: Self.languageName) { openSettings() }
            }
            SettingsGroup("Help") {
                LinkRow(icon: "checkmark.shield", title: "Privacy", subtitle: "What the keyboard can and can’t do") {
                    page = .privacy
                }
                RowDivider()
                LinkRow(icon: "envelope", title: "Send feedback", subtitle: "Report a wrong word or a bug") {
                    if let url = Self.feedbackURL { openURL(url) }
                }
            }
            .padding(.top, 28)
            SettingsGroup("Legal") {
                LinkRow(icon: "hand.raised", title: "Privacy policy") { openURL(Self.privacyPolicy) }
                RowDivider()
                LinkRow(icon: "doc.text", title: "Terms of use") { openURL(Self.terms) }
                RowDivider()
                LinkRow(icon: "info.circle", title: "Licences and credits", subtitle: "Version \(Self.version)") {
                    page = .licences
                }
                RowDivider()
                LinkRow(icon: "chevron.left.forwardslash.chevron.right", title: "Source code",
                        subtitle: "Open source on GitHub") { openURL(Self.sourceCode) }
            }
            .padding(.top, 28)
        }
        .sheet(item: $page) { page in
            switch page {
            case .privacy: PrivacyView()
            case .licences: LicencesView()
            }
        }
    }

    private func openSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
    }

    /** The language the app is showing, named in its own language like Android's. */
    private static var languageName: String {
        Bundle.main.preferredLocalizations.first == "ne" ? "नेपाली" : "English"
    }

    static var version: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
    }

    /** A mail draft with the version and device filled in, to help with bug reports (as on Android). */
    private static var feedbackURL: URL? {
        var system = utsname()
        uname(&system)
        let model = withUnsafeBytes(of: system.machine) { String(decoding: $0.prefix { $0 != 0 }, as: UTF8.self) }
        var mail = URLComponents()
        mail.scheme = "mailto"
        mail.path = supportEmail
        mail.queryItems = [
            URLQueryItem(name: "subject", value: String(localized: "Nepali Keyboard feedback")),
            URLQueryItem(name: "body", value: "\n\n—\n\(String(localized: "Version \(version)")) · iOS \(UIDevice.current.systemVersion) · \(model)")
        ]
        return mail.url
    }
}

enum InfoPage: Identifiable {
    case privacy, licences
    var id: Self { self }
}

// MARK: - Building blocks

/** An inset grouped section, drawn by hand so the screen stays one ScrollView (the try-it field relies on it). */
struct SettingsGroup<Content: View>: View {
    private let title: LocalizedStringKey?
    private let content: Content

    init(_ title: LocalizedStringKey? = nil, @ViewBuilder content: () -> Content) {
        self.title = title
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let title {
                Text(title)
                    .font(.footnote)
                    .textCase(.uppercase)
                    .foregroundStyle(.secondary)
                    .accessibilityAddTraits(.isHeader)
                    .padding(.leading, 16)
                    .padding(.bottom, 7)
            }
            VStack(alignment: .leading, spacing: 0) { content }
                .background(Color(uiColor: .secondarySystemGroupedBackground))
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
    }
}

/** The divider between rows, starting at the title like iOS's own lists. */
struct RowDivider: View {
    var inset: CGFloat = 54
    var body: some View { Divider().padding(.leading, inset) }
}

/** Icon, title and an optional subtitle: the left side of every row. */
struct RowLabel: View {
    let icon: String
    let title: LocalizedStringKey
    var subtitle: LocalizedStringKey?
    var subtitleColor: Color = .secondary

    var body: some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .font(.body)
                .foregroundStyle(.primary)
                .frame(width: 24)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 2) {
                Text(title).foregroundStyle(.primary)
                if let subtitle {
                    Text(subtitle).font(.footnote).foregroundStyle(subtitleColor)
                }
            }
        }
    }
}

struct SwitchRow: View {
    let icon: String
    let title: LocalizedStringKey
    var subtitle: LocalizedStringKey?
    @Binding var isOn: Bool

    var body: some View {
        Toggle(isOn: $isOn) { RowLabel(icon: icon, title: title, subtitle: subtitle) }
            .tint(Color.crimson)
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .frame(minHeight: 48)
    }
}

/** A row that opens something. [value] is shown on the right in grey, like iOS's own value rows. */
struct LinkRow: View {
    let icon: String
    let title: LocalizedStringKey
    var subtitle: LocalizedStringKey?
    var value: String?
    var showsChevron = true
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                RowLabel(icon: icon, title: title, subtitle: subtitle)
                Spacer(minLength: 8)
                if let value {
                    Text(verbatim: value).foregroundStyle(.secondary)
                }
                if showsChevron {
                    Image(systemName: "chevron.right")
                        .font(.footnote.weight(.semibold))
                        .foregroundStyle(.tertiary)
                        .accessibilityHidden(true)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .frame(minHeight: 48)
            .contentShape(Rectangle())
        }
        .buttonStyle(RowButtonStyle())
    }
}

/** The grey press highlight of iOS list rows. */
struct RowButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .background(configuration.isPressed ? Color(uiColor: .systemGray4) : .clear)
    }
}

#Preview {
    ScrollView {
        VStack(spacing: 28) {
            TypingSettings()
            AboutSettings()
        }
        .padding(20)
    }
    .background(Color(uiColor: .systemGroupedBackground))
}
