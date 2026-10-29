import Foundation
import UIKit
import NepaliKeyboardShared

/**
 * The system pasteboard for the shared controller. Checked on an iPhone 14 (iOS 26):
 * - without Full Access a keyboard sees an empty pasteboard (`changeCount` 0, no strings) and
 *   its writes are dropped, so everything here is off;
 * - with Full Access `changeCount`, `hasStrings` and `types` are free, but reading the text makes
 *   iOS ask "Allow Paste" for every new copy (Settings → Paste from Other Apps is Ask by default).
 *   So the text is read only after the user taps something ([readAllowed]); a new copy is
 *   offered as a plain Paste without reading it;
 * - right after the keyboard opens, `changeCount` can still read 0, and the real value turns up
 *   a moment later, which is why copies are also watched with a timer while the keyboard is on screen.
 *
 * iOS doesn't say when a copy was made. A copy counts as made when the keyboard first sees its
 * `changeCount`, which is remembered across launches so an old copy isn't offered again.
 */
final class PasteboardClipboardSource: NSObject, ClipboardSource {
    private let pasteboard: UIPasteboard
    private let hasFullAccess: () -> Bool
    private let defaults: UserDefaults
    private let now: () -> Date
    private var timer: Timer?
    /** Set by the keyboard only while handling a tap that needs the text. */
    var readAllowed = false

    init(
        pasteboard: UIPasteboard = .general,
        defaults: UserDefaults = .standard,
        now: @escaping () -> Date = Date.init,
        hasFullAccess: @escaping () -> Bool
    ) {
        self.pasteboard = pasteboard
        self.defaults = defaults
        self.now = now
        self.hasFullAccess = hasFullAccess
    }

    /**
     * Pasteboard types that mean "don't keep this", from the nspasteboard.org conventions that
     * password managers follow (1Password adds its own as well).
     */
    private static let sensitiveTypes: Set<String> = [
        "org.nspasteboard.ConcealedType",
        "org.nspasteboard.TransientType",
        "com.agilebits.onepassword"
    ]

    func read() -> CopiedText? {
        guard readAllowed, hasFullAccess(), pasteboard.hasStrings,
              let text = pasteboard.string, !text.isEmpty else { return nil }
        let sensitive = !Self.sensitiveTypes.isDisjoint(with: pasteboard.types)
        return CopiedText(text: text, sensitive: sensitive)
    }

    func observe(onCopy: (() -> Void)?) {
        timer?.invalidate()
        timer = nil
        guard let onCopy else { return }
        let timer = Timer(timeInterval: 0.5, repeats: true) { [weak self] _ in
            guard let self, self.noteCopy()?.isNew == true else { return }
            onCopy()
        }
        RunLoop.main.add(timer, forMode: .common)
        self.timer = timer
    }

    func recentCopyId(withinMillis: Int64) -> KotlinLong? {
        guard let copy = noteCopy() else { return nil }
        let ageMillis = now().timeIntervalSince(copy.firstSeen) * 1000
        return ageMillis <= Double(withinMillis) ? KotlinLong(value: Int64(copy.id)) : nil
    }

    func clear() {
        guard hasFullAccess() else { return }
        pasteboard.items = []
    }

    // MARK: When a copy was first seen

    private enum Key {
        static let count = "clipboard_seen_count"
        static let time = "clipboard_seen_time"
    }

    /** The current copy's id and when the keyboard first saw it; nil while there's nothing to read. */
    private func noteCopy() -> (id: Int, firstSeen: Date, isNew: Bool)? {
        guard hasFullAccess() else { return nil }
        let count = pasteboard.changeCount
        guard count != 0, pasteboard.hasStrings else { return nil }
        if defaults.object(forKey: Key.count) as? Int == count,
           let seen = defaults.object(forKey: Key.time) as? Double {
            return (count, Date(timeIntervalSince1970: seen), false)
        }
        let seen = now()
        defaults.set(count, forKey: Key.count)
        defaults.set(seen.timeIntervalSince1970, forKey: Key.time)
        return (count, seen, true)
    }
}

/**
 * Clipboard history in `clipboard.txt` in the App Group (Android's format, from the shared
 * `ClipboardHistory`), so the app can clear it. The keyboard can write the group only with Full
 * Access, which is also the only way to have a history, so without it the list is empty.
 * The file is read again each time the keyboard opens, in case the app cleared it.
 */
final class GroupClipboardStorage: NSObject, ClipboardStorage {
    private let file: URL?
    private let hasFullAccess: () -> Bool
    private var cache: [ClipItem]?
    private let writer = DispatchQueue(label: "clipboard-history", qos: .utility)

    init(file: URL? = SharedSettings.clipboardFile, hasFullAccess: @escaping () -> Bool) {
        self.file = file
        self.hasFullAccess = hasFullAccess
    }

    /** Forgets the cached list, so the next load reads the file. */
    func reload() {
        cache = nil
    }

    func load() -> [ClipItem] {
        guard hasFullAccess() else { return [] }
        if let cache { return cache }
        let text = file.flatMap { try? String(contentsOf: $0, encoding: .utf8) }
        let items = ClipboardHistory.shared.decode(data: text)
        cache = items
        return items
    }

    func save(items: [ClipItem]) {
        guard hasFullAccess(), let file else { return }
        cache = items
        let data = ClipboardHistory.shared.encode(items: items)
        writer.async {
            if items.isEmpty {
                try? FileManager.default.removeItem(at: file)
            } else {
                try? data.write(to: file, atomically: true, encoding: .utf8)
            }
        }
    }
}
