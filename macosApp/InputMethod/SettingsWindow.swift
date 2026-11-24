import Cocoa
import SwiftUI
import NepaliKeyboardShared

/**
 * The settings window, opened from the input menu (Settings…). It lives in the input method's own
 * process, so a change applies to the next key without any syncing.
 *
 * Only what applies to a Mac keyboard: the style follows the selected input source, and English
 * (auto-capitals, English word suggestions) is the system's own ABC source.
 */
struct SettingsView: View {
    @AppStorage("smart_suggestions") private var smartSuggestions = true
    @State private var showGuide = false
    @Environment(\.openURL) private var openURL

    var body: some View {
        Form {
            Section("Typing") {
                Toggle(isOn: $smartSuggestions) {
                    Text("Smart suggestions")
                    Text("Dates, amounts and units as you type")
                }
                LabeledContent("Switch to English", value: String(localized: "Globe or Control-Space"))
                LabeledContent("Pick a suggestion", value: String(localized: "Tab, then Space"))
                Button("Romanized typing guide") { showGuide = true }
            }
            Section("Help") {
                Button("Send feedback") { if let url = Links.feedback { openURL(url) } }
                Button("Privacy policy") { openURL(Links.privacyPolicy) }
                Button("Terms of use") { openURL(Links.terms) }
                Button("Source code") { openURL(Links.sourceCode) }
            }
            Section("Licences and credits") {
                Text("Contains word frequencies derived from Nepali Wikipedia (ne.wikipedia.org), available under the Creative Commons Attribution-ShareAlike 4.0 licence (CC BY-SA 4.0).")
                Text("Month lengths for BS 2000–2090 from nepali-date-pro-max by l3lackcurtains, under the MIT licence.")
                Text("Built with Kotlin Multiplatform; Kotlin is available under the Apache License 2.0. This keyboard is free software under the GPL-3.0.")
            }
            .font(.callout)
            .foregroundStyle(.secondary)
            Section {
                LabeledContent("Version", value: Links.version)
            }
        }
        .formStyle(.grouped)
        .buttonStyle(.link)
        .frame(width: 460)
        .frame(minHeight: 420, idealHeight: 600)
        .sheet(isPresented: $showGuide) { GuideView() }
    }
}

/** The Romanized chart from the shared code (the same entries the phone apps show). */
private struct GuideView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text("Type how the word sounds. Capitals give ट ठ ड ढ ण and ष. Other spellings show in the suggestions.")
                        .foregroundStyle(.secondary)
                    ForEach(RomanizedGuide.shared.sections, id: \.kind) { section in
                        Text(title(section.kind))
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(.secondary)
                            .padding(.top, 18)
                            .padding(.bottom, 6)
                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 72), spacing: 2)], spacing: 4) {
                            ForEach(section.entries, id: \.self) { entry in
                                let typed = entry.typed.joined(separator: "/")
                                VStack(spacing: 2) {
                                    Text(entry.nepali).font(.title2)
                                    Text(typed).font(.caption.monospaced()).foregroundStyle(.secondary)
                                }
                                .padding(.vertical, 6)
                                .accessibilityElement(children: .ignore)
                                .accessibilityLabel("\(typed): \(entry.nepali)")
                            }
                        }
                        .padding(.vertical, 4)
                        .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color(nsColor: .separatorColor)))
                    }
                }
                .padding(20)
            }
            Divider()
            HStack {
                Spacer()
                Button("Done") { dismiss() }.keyboardShortcut(.defaultAction)
            }
            .padding(12)
        }
        .frame(width: 460, height: 560)
    }

    private func title(_ kind: RomanizedGuide.Kind) -> LocalizedStringKey {
        switch kind {
        case .vowels: return "Vowels"
        case .consonants: return "Consonants"
        case .signs: return "Vowel signs and joined letters"
        default: return "Marks and numbers"
        }
    }
}

enum Links {
    static let supportEmail = "nepali.indie.dev@gmail.com"
    static let privacyPolicy = URL(string: "https://nepali-indie-dev.github.io/nepali-keyboard/privacy-policy.html")!
    static let terms = URL(string: "https://nepali-indie-dev.github.io/nepali-keyboard/terms-of-service.html")!
    static let sourceCode = URL(string: "https://github.com/theGBguy/nepali-unicode-keyboard")!

    static var version: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? ""
    }

    /** A mail draft with the version and macOS version filled in, as on the phones. */
    static var feedback: URL? {
        let os = ProcessInfo.processInfo.operatingSystemVersion
        var mail = URLComponents()
        mail.scheme = "mailto"
        mail.path = supportEmail
        mail.queryItems = [
            URLQueryItem(name: "subject", value: String(localized: "Nepali Unicode Keyboard feedback")),
            URLQueryItem(name: "body", value: "\n\n—\n\(String(localized: "Version \(version)")) · macOS \(os.majorVersion).\(os.minorVersion).\(os.patchVersion)"),
        ]
        return mail.url
    }
}

/** One settings window for the process, brought to the front when opened again. */
@MainActor
final class SettingsWindow {
    static let shared = SettingsWindow()
    private var window: NSWindow?

    func show() {
        if window == nil {
            let window = NSWindow(contentViewController: NSHostingController(rootView: SettingsView()))
            window.title = String(localized: "Nepali Unicode Keyboard")
            window.styleMask = [.titled, .closable, .miniaturizable, .resizable]
            window.isReleasedWhenClosed = false
            window.center()
            self.window = window
        }
        NSApp.activate(ignoringOtherApps: true)
        window?.makeKeyAndOrderFront(nil)
    }
}
