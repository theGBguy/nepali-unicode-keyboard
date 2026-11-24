import Cocoa
import InputMethodKit

/**
 * The input method's process. macOS starts it when the user picks one of its input sources, and
 * apps talk to it through the IMKServer connection.
 *
 * `--install` registers the bundle with the system and enables both input sources (used by the
 * dev install script and the package's postinstall), then exits.
 */
if CommandLine.arguments.contains("--install") {
    InputSourceInstaller.install(bundleURL: Bundle.main.bundleURL)
    exit(0)
}

// A top-level constant, so the server lives as long as the process
guard let connectionName = Bundle.main.infoDictionary?["InputMethodConnectionName"] as? String,
      let bundleId = Bundle.main.bundleIdentifier,
      let server = IMKServer(name: connectionName, bundleIdentifier: bundleId)
else {
    NSLog("NepaliKeyboard: couldn't start the input method server")
    exit(1)
}

WordListLoader.loadIfNeeded()
withExtendedLifetime(server) {
    NSApplication.shared.run()
}
