import SwiftUI

/**
 * What the keyboard can and can't do: Android's Privacy screen, told for iOS. iOS keyboards get
 * no microphone, and password fields always use Apple's keyboard, so those parts differ.
 */
struct PrivacyView: View {
    var body: some View {
        InfoPageView(
            title: "Privacy",
            heading: "What this keyboard can and can’t do",
            items: [
                ("Offline unless you turn on handwriting",
                 "Typing, suggestions, clipboard and emoji work without the internet and send nothing. The keyboard uses the internet for handwriting only: turning it on downloads Google’s Nepali handwriting model, and Google’s ML Kit then sends usage diagnostics (phone model, app version, an install ID, timings). Your writing is recognised on the iPhone and isn’t sent."),
                ("Password fields are off-limits",
                 "iOS switches to Apple’s keyboard in password fields, so this keyboard never sees them."),
                ("Your settings stay here",
                 "The keyboard keeps your settings, recently used emoji and your clipboard history on this iPhone. It doesn’t save what you type. Clipboard history only holds text you copied, never items a password manager marks as secret, and you can turn it off in Settings."),
                ("What Full Access is for",
                 "iOS asks before a keyboard can read the clipboard or use the internet. This keyboard needs it only for clipboard history and the handwriting model download.")
            ]
        )
    }
}

/** Credits and licences, as on Android. The word list's CC BY-SA 4.0 attribution is required. */
struct LicencesView: View {
    var body: some View {
        InfoPageView(
            title: "Licences and credits",
            heading: nil,
            items: [
                ("Nepali word list",
                 "Contains word frequencies derived from Nepali Wikipedia (ne.wikipedia.org), available under the Creative Commons Attribution-ShareAlike 4.0 licence (CC BY-SA 4.0)."),
                ("Emoji list",
                 "Emoji list derived from Unicode emoji data, © Unicode, Inc., under the Unicode License v3. Emoji are drawn by your iPhone’s font."),
                ("Bikram Sambat calendar",
                 "Month lengths for BS 2000–2090 from nepali-date-pro-max by l3lackcurtains, under the MIT licence."),
                ("Open-source libraries",
                 "Built with SwiftUI and Kotlin Multiplatform. Kotlin and google-mlkit-swiftpm (handwriting) are available under the Apache License 2.0; Google ML Kit is under Google’s ML Kit terms.")
            ]
        )
    }
}

/** A sheet with a title, an optional heading and titled paragraphs. */
private struct InfoPageView: View {
    let title: LocalizedStringKey
    let heading: LocalizedStringKey?
    let items: [(LocalizedStringKey, LocalizedStringKey)]
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    if let heading {
                        Text(heading)
                            .font(.title2.weight(.bold))
                            .accessibilityAddTraits(.isHeader)
                            .padding(.bottom, 8)
                    }
                    ForEach(items.indices, id: \.self) { i in
                        if i > 0 { Divider() }
                        VStack(alignment: .leading, spacing: 4) {
                            Text(items[i].0).font(.body.weight(.semibold))
                            Text(items[i].1).font(.subheadline).foregroundStyle(.secondary)
                        }
                        .padding(.vertical, 14)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 20)
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
        .tint(Color.crimson)
    }
}

#Preview {
    PrivacyView()
}
