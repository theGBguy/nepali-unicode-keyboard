import SwiftUI

/**
 * The Romanized typing chart, opened from Settings and shown in short under the setup try-it
 * field. A copy of the shared `RomanizedGuide.kt` (the app target doesn't link the shared
 * framework); its test checks every entry against the converter, so keep the two in step.
 */
enum TypingGuide {
    struct Entry: Hashable {
        let typed: [String]
        let nepali: String
        init(_ typed: String..., nepali: String) {
            self.typed = typed
            self.nepali = nepali
        }
    }

    struct Section {
        let title: LocalizedStringKey
        let entries: [Entry]
    }

    static let sections: [Section] = [
        Section(title: "Vowels", entries: [
            Entry("a", nepali: "अ"), Entry("aa", "A", nepali: "आ"), Entry("i", nepali: "इ"),
            Entry("ii", "ee", "I", nepali: "ई"), Entry("u", nepali: "उ"), Entry("uu", "oo", "U", nepali: "ऊ"),
            Entry("rri", "Ri", nepali: "ऋ"), Entry("e", nepali: "ए"), Entry("ai", nepali: "ऐ"),
            Entry("o", nepali: "ओ"), Entry("au", "ou", nepali: "औ"), Entry("aM", nepali: "अं"),
        ]),
        Section(title: "Consonants", entries: [
            Entry("ka", nepali: "क"), Entry("kha", nepali: "ख"), Entry("ga", nepali: "ग"), Entry("gha", nepali: "घ"),
            Entry("nga", nepali: "ङ"), Entry("cha", nepali: "च"), Entry("chha", nepali: "छ"), Entry("ja", nepali: "ज"),
            Entry("jha", nepali: "झ"), Entry("yna", nepali: "ञ"), Entry("Ta", nepali: "ट"), Entry("Tha", nepali: "ठ"),
            Entry("Da", nepali: "ड"), Entry("Dha", nepali: "ढ"), Entry("Na", nepali: "ण"), Entry("ta", nepali: "त"),
            Entry("tha", nepali: "थ"), Entry("da", nepali: "द"), Entry("dha", nepali: "ध"), Entry("na", nepali: "न"),
            Entry("pa", nepali: "प"), Entry("pha", "fa", nepali: "फ"), Entry("ba", nepali: "ब"), Entry("bha", nepali: "भ"),
            Entry("ma", nepali: "म"), Entry("ya", nepali: "य"), Entry("ra", nepali: "र"), Entry("la", nepali: "ल"),
            Entry("wa", "va", nepali: "व"), Entry("sha", "Sa", nepali: "श"), Entry("Sha", nepali: "ष"), Entry("sa", nepali: "स"),
            Entry("ha", nepali: "ह"), Entry("ksha", "xa", nepali: "क्ष"), Entry("tra", nepali: "त्र"), Entry("gya", nepali: "ज्ञ"),
        ]),
        Section(title: "Vowel signs and joined letters", entries: [
            Entry("kaa", nepali: "का"), Entry("ki", nepali: "कि"), Entry("kii", nepali: "की"), Entry("ku", nepali: "कु"),
            Entry("kuu", nepali: "कू"), Entry("kRi", nepali: "कृ"), Entry("ke", nepali: "के"), Entry("kai", nepali: "कै"),
            Entry("ko", nepali: "को"), Entry("kau", nepali: "कौ"), Entry("kta", nepali: "क्त"), Entry("rya", nepali: "र्य"),
        ]),
        Section(title: "Marks and numbers", entries: [
            Entry("kaM", nepali: "कं"), Entry("ka~", "ka^", nepali: "कँ"), Entry("kaH", nepali: "कः"),
            Entry(".", nepali: "।"), Entry("123", nepali: "१२३"),
        ]),
    ]

    static let compact: [Entry] = [
        Entry("aa", "A", nepali: "आ"), Entry("ii", "ee", "I", nepali: "ई"), Entry("uu", "oo", "U", nepali: "ऊ"),
        Entry("chha", nepali: "छ"), Entry("Ta", nepali: "ट"), Entry("Na", nepali: "ण"),
        Entry("sha", "Sa", nepali: "श"), Entry("Sha", nepali: "ष"), Entry("kaM", nepali: "कं"),
        Entry("ka~", "ka^", nepali: "कँ"),
    ]
}

/** The full chart as a sheet, like the privacy page. */
struct TypingGuideView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text("Type how the word sounds. Capitals give ट ठ ड ढ ण and ष. Other spellings show in the suggestions.")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    ForEach(TypingGuide.sections.indices, id: \.self) { i in
                        let section = TypingGuide.sections[i]
                        Text(section.title)
                            .font(.footnote.weight(.medium))
                            .foregroundStyle(.secondary)
                            .padding(.top, 22)
                            .padding(.bottom, 8)
                            .padding(.leading, 4)
                        GuideGrid(entries: section.entries)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 20)
                .padding(.bottom, 24)
            }
            .navigationTitle("Romanized typing guide")
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

/** A few common rows under the try-it field, with a link to the full chart. */
struct CompactGuide: View {
    let onOpenGuide: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            GuideGrid(entries: TypingGuide.compact, minCell: 64)
            Button("Full typing guide", action: onOpenGuide)
                .font(.subheadline)
                .padding(.leading, 4)
        }
    }
}

/** Entries in an outlined grid: the Nepali letter large, what to type under it. */
struct GuideGrid: View {
    let entries: [TypingGuide.Entry]
    var minCell: CGFloat = 76

    var body: some View {
        LazyVGrid(columns: [GridItem(.adaptive(minimum: minCell), spacing: 2)], spacing: 4) {
            ForEach(entries, id: \.self) { entry in
                let typed = entry.typed.joined(separator: "/")
                VStack(spacing: 2) {
                    Text(entry.nepali).font(.title2)
                    Text(typed)
                        .font(.caption.monospaced())
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .lineLimit(2)
                }
                .padding(.vertical, 6)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(typed): \(entry.nepali)")
            }
        }
        .padding(.vertical, 6)
        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(Color(uiColor: .separator)))
    }
}
