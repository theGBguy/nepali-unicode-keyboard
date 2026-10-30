import SwiftUI
import UIKit

/**
 * The emoji grid: a `UICollectionView`, because it reuses a screenful of cells. A SwiftUI
 * LazyVStack keeps every row it has built, and each emoji cell's layer is ~70 KB, so scrolling
 * the whole list grew the keyboard by ~70 MB, too much for an extension's memory limit.
 *
 * Sections have a small label header. [onTopSection] reports the section at the top as the user
 * scrolls (for the tabs); setting [jump] to a new value scrolls to that section's label.
 */
struct EmojiGridView: UIViewRepresentable {
    struct Section: Equatable {
        let id: String
        let title: String
        let emoji: [String]
    }

    /** A request to scroll to a section; `token` changes on every tap, so the same tab twice works. */
    struct Jump: Equatable {
        let section: Int
        let token: Int
    }

    let sections: [Section]
    let labelColor: UIColor
    let pressedColor: UIColor
    let jump: Jump?
    let onPress: () -> Void
    let onEmoji: (String) -> Void
    let onTopSection: (Int) -> Void

    func makeCoordinator() -> Coordinator { Coordinator() }

    func makeUIView(context: Context) -> UICollectionView {
        let view = UICollectionView(frame: .zero, collectionViewLayout: Self.layout())
        view.backgroundColor = .clear
        view.showsVerticalScrollIndicator = false
        view.delaysContentTouches = false
        view.register(EmojiCell.self, forCellWithReuseIdentifier: EmojiCell.id)
        view.register(LabelHeader.self, forSupplementaryViewOfKind: UICollectionView.elementKindSectionHeader,
                      withReuseIdentifier: LabelHeader.id)
        view.dataSource = context.coordinator
        view.delegate = context.coordinator
        return view
    }

    func updateUIView(_ view: UICollectionView, context: Context) {
        let coordinator = context.coordinator
        coordinator.parent = self
        if coordinator.sections != sections {
            coordinator.sections = sections
            view.reloadData()
        }
        if let jump, jump != coordinator.lastJump, jump.section < sections.count {
            coordinator.lastJump = jump
            view.layoutIfNeeded()
            if let header = view.layoutAttributesForSupplementaryElement(
                ofKind: UICollectionView.elementKindSectionHeader, at: IndexPath(item: 0, section: jump.section)) {
                let maxY = max(view.contentSize.height - view.bounds.height, 0)
                view.setContentOffset(CGPoint(x: 0, y: min(header.frame.minY, maxY)), animated: false)
            }
        }
    }

    /** Cells about as wide as Android's (44 dp) and 46 pt tall, spread evenly across the width. */
    private static func layout() -> UICollectionViewLayout {
        UICollectionViewCompositionalLayout { _, environment in
            let width = environment.container.effectiveContentSize.width - 8
            let columns = max(Int(width / 42), 1)
            let item = NSCollectionLayoutItem(layoutSize: .init(
                widthDimension: .fractionalWidth(1 / CGFloat(columns)), heightDimension: .absolute(46)))
            let group = NSCollectionLayoutGroup.horizontal(
                layoutSize: .init(widthDimension: .fractionalWidth(1), heightDimension: .absolute(46)),
                repeatingSubitem: item, count: columns)
            let section = NSCollectionLayoutSection(group: group)
            section.contentInsets = NSDirectionalEdgeInsets(top: 0, leading: 4, bottom: 0, trailing: 4)
            let header = NSCollectionLayoutBoundarySupplementaryItem(
                layoutSize: .init(widthDimension: .fractionalWidth(1), heightDimension: .absolute(30)),
                elementKind: UICollectionView.elementKindSectionHeader, alignment: .top)
            section.boundarySupplementaryItems = [header]
            return section
        }
    }

    final class Coordinator: NSObject, UICollectionViewDataSource, UICollectionViewDelegate {
        var parent: EmojiGridView?
        var sections: [Section] = []
        var lastJump: Jump?
        private var lastTop = -1

        func numberOfSections(in collectionView: UICollectionView) -> Int { sections.count }

        func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
            sections[section].emoji.count
        }

        func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
            let cell = collectionView.dequeueReusableCell(withReuseIdentifier: EmojiCell.id, for: indexPath) as! EmojiCell
            let emoji = sections[indexPath.section].emoji[indexPath.item]
            cell.label.text = emoji
            cell.pressedColor = parent?.pressedColor ?? .clear
            cell.accessibilityLabel = emoji
            return cell
        }

        func collectionView(_ collectionView: UICollectionView, viewForSupplementaryElementOfKind kind: String,
                            at indexPath: IndexPath) -> UICollectionReusableView {
            let header = collectionView.dequeueReusableSupplementaryView(
                ofKind: kind, withReuseIdentifier: LabelHeader.id, for: indexPath) as! LabelHeader
            header.label.text = sections[indexPath.section].title
            header.label.textColor = parent?.labelColor
            return header
        }

        // The click/tap when the finger goes down, the emoji when it lifts
        func collectionView(_ collectionView: UICollectionView, didHighlightItemAt indexPath: IndexPath) {
            parent?.onPress()
        }

        func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
            collectionView.deselectItem(at: indexPath, animated: false)
            parent?.onEmoji(sections[indexPath.section].emoji[indexPath.item])
        }

        func scrollViewDidScroll(_ scrollView: UIScrollView) {
            guard let collectionView = scrollView as? UICollectionView else { return }
            // The section whose label or emoji is at the top edge
            let probe = CGPoint(x: 20, y: scrollView.contentOffset.y + 1)
            let top = collectionView.indexPathForItem(at: probe)?.section
                ?? collectionView.indexPathsForVisibleItems.map(\.section).min()
            guard let top, top != lastTop else { return }
            lastTop = top
            parent?.onTopSection(top)
        }
    }
}

private final class EmojiCell: UICollectionViewCell {
    static let id = "emoji"
    let label = UILabel()
    var pressedColor: UIColor = .clear

    override init(frame: CGRect) {
        super.init(frame: frame)
        label.font = .systemFont(ofSize: 30)
        label.textAlignment = .center
        label.frame = contentView.bounds
        label.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        contentView.addSubview(label)
        contentView.layer.cornerRadius = 8
        isAccessibilityElement = true
        accessibilityTraits = .keyboardKey
    }

    required init?(coder: NSCoder) { fatalError("not used") }

    override var isHighlighted: Bool {
        didSet { contentView.backgroundColor = isHighlighted ? pressedColor : .clear }
    }
}

private final class LabelHeader: UICollectionReusableView {
    static let id = "label"
    let label = UILabel()

    override init(frame: CGRect) {
        super.init(frame: frame)
        label.font = .systemFont(ofSize: 12)
        label.translatesAutoresizingMaskIntoConstraints = false
        addSubview(label)
        label.accessibilityTraits = .header
        NSLayoutConstraint.activate([
            label.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 12),
            label.bottomAnchor.constraint(equalTo: bottomAnchor, constant: -4)
        ])
    }

    required init?(coder: NSCoder) { fatalError("not used") }
}
