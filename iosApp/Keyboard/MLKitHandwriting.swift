import Foundation
import ObjectiveC
import MLKitCommon
import MLKitDigitalInkRecognition
import NepaliKeyboardShared

/**
 * Nepali handwriting with Google ML Kit digital ink recognition, like Android's `MlKitHandwriting`.
 * Recognition runs on the iPhone; the model (`ne`, three files) is downloaded from Google once,
 * which needs Full Access. Google's ML Kit then sends usage diagnostics (device model, app
 * version, timings, error codes); the ink itself isn't sent.
 *
 * Nothing in ML Kit runs until the user turns handwriting on, because this class is only made then.
 */
final class MLKitHandwriting: NSObject, HandwritingEngine {
    static let shared = MLKitHandwriting()

    private static let language = "ne"
    private static let maxCandidates = 5

    private lazy var model: DigitalInkRecognitionModel? = {
        BackgroundSessionFix.install()
        return DigitalInkRecognitionModelIdentifier(forLanguageTag: Self.language).map(DigitalInkRecognitionModel.init)
    }()
    private var recognizer: DigitalInkRecognizer?
    private var downloadCallbacks: [(String?) -> Void] = []
    private var observers: [NSObjectProtocol] = []

    func isModelReady(done: @escaping (KotlinBoolean) -> Void) {
        let ready = model.map { ModelManager.modelManager().isModelDownloaded($0) } ?? false
        done(KotlinBoolean(bool: ready))
    }

    /** Whether the model is on the iPhone, without starting ML Kit when handwriting was never used. */
    var hasModel: Bool {
        guard let support = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else { return false }
        let folder = support.appendingPathComponent("com.google.mlkit.vision.digitalinkrecognition")
        return FileManager.default.fileExists(atPath: folder.path) && model.map { ModelManager.modelManager().isModelDownloaded($0) } == true
    }

    func download(done: @escaping (String?) -> Void) {
        guard let model else { done("No Nepali model"); return }
        let manager = ModelManager.modelManager()
        if manager.isModelDownloaded(model) { done(nil); return }
        downloadCallbacks.append(done)
        // A download is already running: wait for it instead of starting another
        guard downloadCallbacks.count == 1 else { return }
        observeDownload()
        manager.download(model, conditions: ModelDownloadConditions(allowsCellularAccess: true, allowsBackgroundDownloading: false))
    }

    private func observeDownload() {
        let center = NotificationCenter.default
        observers.append(center.addObserver(forName: .mlkitModelDownloadDidSucceed, object: nil, queue: .main) { [weak self] _ in
            self?.finishDownload(error: nil)
        })
        observers.append(center.addObserver(forName: .mlkitModelDownloadDidFail, object: nil, queue: .main) { [weak self] note in
            let error = note.userInfo?[ModelDownloadUserInfoKey.error.rawValue] as? Error
            self?.finishDownload(error: error?.localizedDescription ?? "Download failed")
        })
    }

    private func finishDownload(error: String?) {
        observers.forEach(NotificationCenter.default.removeObserver)
        observers = []
        let callbacks = downloadCallbacks
        downloadCallbacks = []
        callbacks.forEach { $0(error) }
    }

    func recognize(strokes: [[InkPoint]], width: Float, height: Float, preContext: String,
                   done: @escaping ([String]) -> Void) {
        guard let model, !strokes.isEmpty else { done([]); return }
        let client = recognizer ?? DigitalInkRecognizer.digitalInkRecognizer(options: DigitalInkRecognizerOptions(model: model))
        recognizer = client
        let ink = Ink(strokes: strokes.map { stroke in
            Stroke(points: stroke.map { StrokePoint(x: $0.x, y: $0.y, t: Int($0.t)) })
        })
        let context = DigitalInkRecognitionContext(preContext: preContext, writingArea: WritingArea(width: width, height: height))
        client.recognize(ink: ink, context: context) { result, _ in
            let candidates = result?.candidates.prefix(Self.maxCandidates).map(\.text) ?? []
            DispatchQueue.main.async { done(Array(candidates)) }
        }
    }

    func deleteModel(done: @escaping () -> Void) {
        recognizer = nil
        guard let model, ModelManager.modelManager().isModelDownloaded(model) else { done(); return }
        ModelManager.modelManager().deleteDownloadedModel(model) { _ in
            DispatchQueue.main.async { done() }
        }
    }
}

/**
 * ML Kit downloads its models through a background `URLSession`. In an app extension iOS runs a
 * background session only when its configuration names a shared container, which ML Kit doesn't
 * set, so the download never starts (checked on an iPhone 14, iOS 26). This fills in our App
 * Group for background configurations made in this process that have none.
 */
enum BackgroundSessionFix {
    private static var installed = false

    static func install() {
        guard !installed else { return }
        installed = true
        guard let original = class_getClassMethod(URLSessionConfiguration.self, #selector(URLSessionConfiguration.background(withIdentifier:))),
              let replacement = class_getClassMethod(URLSessionConfiguration.self, #selector(URLSessionConfiguration.nk_background(withIdentifier:)))
        else { return }
        method_exchangeImplementations(original, replacement)
    }
}

extension URLSessionConfiguration {
    @objc fileprivate class func nk_background(withIdentifier identifier: String) -> URLSessionConfiguration {
        // The implementations are exchanged, so this calls iOS's own method
        let configuration = nk_background(withIdentifier: identifier)
        if configuration.sharedContainerIdentifier == nil {
            configuration.sharedContainerIdentifier = SharedSettings.groupID
        }
        return configuration
    }
}
