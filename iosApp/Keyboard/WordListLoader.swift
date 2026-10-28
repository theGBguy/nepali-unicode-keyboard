import Foundation
import NepaliKeyboardShared

/** Loads the bundled word list (the same file as Android's) once per process, off the main thread. */
enum WordListLoader {
    private static var started = false

    static func loadIfNeeded() {
        guard !started, NepaliWordList.shared.index == nil else { return }
        started = true
        DispatchQueue.global(qos: .utility).async {
            guard let url = Bundle.main.url(forResource: "words_ne", withExtension: "tsv"),
                  let text = try? String(contentsOf: url, encoding: .utf8) else { return }
            let index = WordIndex.companion.parse(text: text)
            DispatchQueue.main.async { NepaliWordList.shared.index = index }
        }
    }
}
