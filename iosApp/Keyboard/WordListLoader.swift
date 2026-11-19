import Foundation
import NepaliKeyboardShared

/**
 * Loads the bundled word lists (the same files as Android's) once per process, off the main
 * thread. One after the other, so the extension's memory peak holds one file's text at a time.
 */
enum WordListLoader {
    private static var started = false

    static func loadIfNeeded() {
        guard !started else { return }
        started = true
        DispatchQueue.global(qos: .utility).async {
            if NepaliWordList.shared.index == nil,
               let url = Bundle.main.url(forResource: "words_ne", withExtension: "tsv"),
               let text = try? String(contentsOf: url, encoding: .utf8) {
                let index = WordIndex.companion.parse(text: text)
                DispatchQueue.main.async { NepaliWordList.shared.index = index }
            }
            if EnglishWordList.shared.index == nil,
               let url = Bundle.main.url(forResource: "words_en", withExtension: "tsv"),
               let text = try? String(contentsOf: url, encoding: .utf8) {
                let index = EnglishWordIndex.companion.parse(text: text)
                DispatchQueue.main.async { EnglishWordList.shared.index = index }
            }
        }
    }
}
