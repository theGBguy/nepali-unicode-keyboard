import CoreText
import Foundation
import NepaliKeyboardShared

/**
 * The bundled emoji list (the same `emoji.tsv` as Android, parsed by the shared `EmojiList`),
 * without the emoji this iPhone's font can't draw: iOS 17.0–17.3 lacks Unicode 15.1's, for
 * example. Loaded once per process, in the background, the first time the panel opens.
 */
@MainActor
final class EmojiCatalog: ObservableObject {
    static let shared = EmojiCatalog()

    struct Section {
        let category: EmojiCategory
        let emoji: [String]
    }

    @Published private(set) var sections: [Section] = []
    private var started = false

    func loadIfNeeded() {
        guard !started else { return }
        started = true
        DispatchQueue.global(qos: .userInitiated).async {
            guard let url = Bundle.main.url(forResource: "emoji", withExtension: "tsv"),
                  let text = try? String(contentsOf: url, encoding: .utf8) else { return }
            let font = CTFontCreateWithName("AppleColorEmoji" as CFString, 20, nil)
            let parsed = EmojiList.shared.parse(text: text) { KotlinBoolean(bool: Self.canDraw($0, font: font)) }
            let sections = EmojiCategory.entries.compactMap { category in
                parsed[category].flatMap { $0.isEmpty ? nil : Section(category: category, emoji: $0) }
            }
            DispatchQueue.main.async { self.sections = sections }
        }
    }

    /**
     * Whether the emoji font draws [emoji] as one picture. An unsupported sequence falls apart
     * into its parts (several glyphs that take up space) or into missing glyphs. Supported
     * sequences can add zero-width glyphs (the couple emoji do), so only glyphs with an advance
     * count.
     */
    nonisolated static func canDraw(_ emoji: String, font: CTFont) -> Bool {
        let text = NSAttributedString(string: emoji, attributes: [kCTFontAttributeName as NSAttributedString.Key: font])
        let line = CTLineCreateWithAttributedString(text)
        var visible = 0
        for run in CTLineGetGlyphRuns(line) as? [CTRun] ?? [] {
            let attributes = CTRunGetAttributes(run) as NSDictionary
            guard let runFont = attributes[kCTFontAttributeName] as! CTFont?,
                  (CTFontCopyPostScriptName(runFont) as String).contains("Emoji") else { return false }
            let count = CTRunGetGlyphCount(run)
            var glyphs = [CGGlyph](repeating: 0, count: count)
            var advances = [CGSize](repeating: .zero, count: count)
            CTRunGetGlyphs(run, CFRange(location: 0, length: count), &glyphs)
            CTRunGetAdvances(run, CFRange(location: 0, length: count), &advances)
            if glyphs.contains(0) { return false }
            visible += advances.filter { $0.width > 0 }.count
        }
        return visible == 1
    }
}
