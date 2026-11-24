import Carbon
import Foundation

/** Registers the input method with the system and turns on its input sources. */
enum InputSourceInstaller {
    static func install(bundleURL: URL) {
        let status = TISRegisterInputSource(bundleURL as CFURL)
        if status != noErr { NSLog("NepaliKeyboard: TISRegisterInputSource failed (\(status))") }
        guard let bundleId = Bundle.main.bundleIdentifier else { return }
        for source in sources(bundleId: bundleId) {
            guard let id = property(source, kTISPropertyInputSourceID), id != bundleId else { continue }
            let enabled = TISEnableInputSource(source)
            if enabled != noErr { NSLog("NepaliKeyboard: couldn't enable \(id) (\(enabled))") }
        }
    }

    /** Every input source from this bundle, including ones not enabled yet. */
    static func sources(bundleId: String) -> [TISInputSource] {
        let filter = [kTISPropertyBundleID as String: bundleId] as CFDictionary
        guard let list = TISCreateInputSourceList(filter, true)?.takeRetainedValue() as? [TISInputSource] else { return [] }
        return list
    }

    static func property(_ source: TISInputSource, _ key: CFString) -> String? {
        guard let raw = TISGetInputSourceProperty(source, key) else { return nil }
        return Unmanaged<CFString>.fromOpaque(raw).takeUnretainedValue() as String
    }
}
