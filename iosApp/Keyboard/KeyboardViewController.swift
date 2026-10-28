import SwiftUI
import UIKit
import Combine
import NepaliKeyboardShared

/** The keyboard extension's entry point: hosts the SwiftUI keyboard and passes field events to it. */
final class KeyboardViewController: UIInputViewController {
    private let model = KeyboardModel()
    private var heightConstraint: NSLayoutConstraint?
    private var heightWatch: AnyCancellable?

    override func viewDidLoad() {
        super.viewDidLoad()
        model.attach(to: self)
        WordListLoader.loadIfNeeded()

        let host = UIHostingController(rootView: KeyboardRootView(model: model))
        host.view.backgroundColor = .clear
        host.view.translatesAutoresizingMaskIntoConstraints = false
        addChild(host)
        view.addSubview(host.view)
        NSLayoutConstraint.activate([
            host.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            host.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            host.view.topAnchor.constraint(equalTo: view.topAnchor),
            host.view.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
        host.didMove(toParent: self)

        // The keyboard is as tall as its layout (traditional Nepali has a row more than English)
        let height = view.heightAnchor.constraint(equalToConstant: KeyboardMetrics.height(of: model.state))
        // Below required, so iOS's own constraint wins during rotation instead of breaking ours
        height.priority = UILayoutPriority(999)
        height.isActive = true
        heightConstraint = height
        heightWatch = model.$state
            .map { KeyboardMetrics.height(of: $0) }
            .removeDuplicates()
            .sink { [weak self] height in
                guard let self, let constraint = self.heightConstraint, constraint.constant != height else { return }
                constraint.constant = height
                self.view.setNeedsLayout()
            }
    }

    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        model.needsInputModeSwitchKey = needsInputModeSwitchKey
        model.inputStarted()
    }

    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        model.inputFinished()
    }

    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        model.needsInputModeSwitchKey = needsInputModeSwitchKey
    }

    override func textDidChange(_ textInput: UITextInput?) {
        super.textDidChange(textInput)
        model.textChanged()
    }

    override func selectionDidChange(_ textInput: UITextInput?) {
        super.selectionDidChange(textInput)
        model.textChanged()
    }

    deinit {
        let model = model
        Task { @MainActor in model.close() }
    }
}
