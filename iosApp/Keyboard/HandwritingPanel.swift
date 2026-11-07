import SwiftUI
import UIKit
import NepaliKeyboardShared

/**
 * Handwriting, drawn in place of the strip and keys at the same height, like Android's panel:
 * candidates on top (best in bold), the writing area, and a bottom row with ABC, ।, space, ?,
 * backspace and Enter. Before the user has agreed, the writing area explains what turning
 * handwriting on means; without Full Access it says that's needed for the download.
 */
struct HandwritingPanel: View {
    @ObservedObject var model: KeyboardModel
    let state: KeyboardState
    let phase: HandwritingPhase
    let colors: KeyboardColors
    let height: CGFloat
    let keyHeight: CGFloat

    var body: some View {
        VStack(spacing: 0) {
            candidates
                .frame(height: KeyboardMetrics.stripHeight)
            area
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(RoundedRectangle(cornerRadius: 10).fill(colors.key))
                .clipShape(RoundedRectangle(cornerRadius: 10))
                .padding(.horizontal, 2)
            bottomRow
                .padding(.top, 6)
        }
        .frame(height: height)
        .onAppear { UIAccessibility.post(notification: .screenChanged, argument: nil) }
    }

    // MARK: Candidates

    private var candidates: some View {
        let list = Array(state.inkCandidates.prefix(4))
        return HStack(spacing: 0) {
            ForEach(Array(list.enumerated()), id: \.offset) { index, text in
                if index > 0 { colors.divider.frame(width: 1, height: 18) }
                Button {
                    model.keyDown()
                    model.inkCandidate(text)
                } label: {
                    Text(verbatim: text)
                        .font(.system(size: 17, weight: index == 0 ? .semibold : .regular))
                        .foregroundStyle(colors.keyText)
                        .lineLimit(1)
                        .truncationMode(.tail)
                        .padding(.horizontal, 6)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.horizontal, 4)
    }

    // MARK: Writing area

    @ViewBuilder
    private var area: some View {
        if !model.hasFullAccess {
            message(String(localized: "Handwriting needs Full Access to download Google’s Nepali handwriting model. Turn it on in Settings → Nepali Unicode Keyboard → Keyboards."))
        } else {
            switch phase {
            case .ready:
                WritingArea(colors: colors, strokes: state.inkStrokes) { stroke, size in
                    model.inkStroke(stroke, width: size.width, height: size.height)
                }
            case .consent:
                consent
            case .downloading:
                message(String(localized: "Downloading Nepali handwriting…")) {
                    ProgressView().tint(colors.accent)
                }
            case .failed:
                message(String(localized: "Couldn’t download. Check your internet connection.")) {
                    panelButton(String(localized: "Try again"), primary: true) { model.retryHandwriting() }
                }
            default:
                Color.clear
            }
        }
    }

    private var consent: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Write by hand")
                .font(.system(size: 15, weight: .medium))
                .foregroundStyle(colors.keyText)
                .accessibilityAddTraits(.isHeader)
            // Scrolls on short key heights, so the buttons always show
            ScrollView {
                Text("Writing is recognised on this iPhone. Turning it on downloads Google’s Nepali handwriting model (about 15 MB) once, and Google’s ML Kit then sends Google usage diagnostics such as phone model and app version, never what you write. Turning it off in the app’s Settings deletes the model.")
                    .font(.system(size: 13))
                    .foregroundStyle(colors.hint)
                    .lineSpacing(2)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            HStack(spacing: 8) {
                Spacer(minLength: 0)
                panelButton(String(localized: "Not now"), primary: false) { model.closeHandwriting() }
                panelButton(String(localized: "Download"), primary: true) { model.acceptHandwriting() }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    private func message(_ text: String) -> some View {
        message(text) { EmptyView() }
    }

    private func message<Below: View>(_ text: String, @ViewBuilder below: () -> Below) -> some View {
        VStack(spacing: 12) {
            Text(text)
                .font(.system(size: 14))
                .foregroundStyle(colors.keyText)
                .multilineTextAlignment(.center)
            below()
        }
        .padding(16)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func panelButton(_ text: String, primary: Bool, action: @escaping () -> Void) -> some View {
        Button {
            model.keyDown()
            action()
        } label: {
            Text(text)
                .font(.system(size: 14, weight: .medium))
                .foregroundStyle(primary ? colors.onAccent : colors.keyText)
                .padding(.horizontal, 16)
                .frame(height: 36)
                .background(Capsule().fill(primary ? colors.accent : Color.clear))
                .contentShape(Capsule())
        }
        .buttonStyle(.plain)
    }

    // MARK: Bottom row

    private var bottomRow: some View {
        let enter = enterVisual(state.field)
        return HStack(spacing: 4) {
            bottomKey(width: 56, label: String(localized: "Letters"), action: model.closeHandwriting) {
                Text(verbatim: "ABC").font(.system(size: 15))
            }
            bottomKey(width: 40, label: "।", action: { model.inkText("।") }) {
                Text(verbatim: "।").font(.system(size: 18))
            }
            bottomKey(width: nil, label: String(localized: "Space"), fill: colors.key, action: model.inkSpace) {
                EmptyView()
            }
            bottomKey(width: 40, label: "?", action: { model.inkText("?") }) {
                Text(verbatim: "?").font(.system(size: 18))
            }
            InkBackspaceKey(model: model, colors: colors)
                .frame(width: 52)
            bottomKey(width: 56, label: enter.description, fill: colors.accent, action: model.inkEnter) {
                Image(systemName: enter.symbol)
                    .font(.system(size: 18))
                    .foregroundStyle(colors.onAccent)
            }
        }
        .frame(height: keyHeight)
    }

    private func bottomKey<Content: View>(
        width: CGFloat?, label: String, fill: Color? = nil, action: @escaping () -> Void,
        @ViewBuilder content: () -> Content
    ) -> some View {
        let face = content()
        return Button {
            model.keyDown()
            action()
        } label: {
            RoundedRectangle(cornerRadius: KeyboardMetrics.keyRadius)
                .fill(fill ?? colors.functionKey)
                .overlay(face.foregroundStyle(colors.keyText))
                .shadow(color: colors.keyShadow, radius: 0, x: 0, y: 0.5)
        }
        .buttonStyle(.plain)
        .frame(width: width)
        .frame(maxWidth: width == nil ? .infinity : nil)
        .accessibilityLabel(Text(label))
        .accessibilityAddTraits(.isKeyboardKey)
    }
}

/**
 * Draws the finished strokes plus the one under the finger, and reports each finished stroke
 * with the area's size (which helps ML Kit tell a small mark from a big letter).
 */
private struct WritingArea: View {
    let colors: KeyboardColors
    let strokes: [[InkPoint]]
    let onStroke: ([InkPoint], CGSize) -> Void

    @State private var current: [InkPoint] = []

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                if strokes.isEmpty && current.isEmpty {
                    Text("Write here")
                        .font(.system(size: 15))
                        .foregroundStyle(colors.hint)
                }
                Canvas { context, _ in
                    for stroke in strokes + [current] where !stroke.isEmpty {
                        if stroke.count == 1 {
                            let p = stroke[0]
                            context.fill(Path(ellipseIn: CGRect(x: CGFloat(p.x) - 2, y: CGFloat(p.y) - 2, width: 4, height: 4)),
                                         with: .color(colors.keyText))
                            continue
                        }
                        var path = Path()
                        path.move(to: CGPoint(x: CGFloat(stroke[0].x), y: CGFloat(stroke[0].y)))
                        for p in stroke.dropFirst() { path.addLine(to: CGPoint(x: CGFloat(p.x), y: CGFloat(p.y))) }
                        context.stroke(path, with: .color(colors.keyText),
                                       style: StrokeStyle(lineWidth: 3, lineCap: .round, lineJoin: .round))
                    }
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in current.append(point(value.location)) }
                    .onEnded { value in
                        current.append(point(value.location))
                        let stroke = current
                        current = []
                        onStroke(stroke, geometry.size)
                    }
            )
            .accessibilityElement()
            .accessibilityLabel(Text("Writing area"))
            .accessibilityHint(Text("Write a word with one finger"))
            .accessibilityAddTraits(.allowsDirectInteraction)
        }
    }

    private func point(_ location: CGPoint) -> InkPoint {
        InkPoint(x: Float(location.x), y: Float(location.y), t: Int64(Date().timeIntervalSince1970 * 1000))
    }
}

/**
 * Backspace removes the last stroke while something is written, else deletes text; held, it
 * keeps deleting text like the keyboard's own backspace.
 */
private struct InkBackspaceKey: View {
    @ObservedObject var model: KeyboardModel
    let colors: KeyboardColors

    @State private var pressed = false
    @State private var repeating = false
    @State private var hold: DispatchWorkItem?

    var body: some View {
        RoundedRectangle(cornerRadius: KeyboardMetrics.keyRadius)
            .fill(pressed ? colors.key : colors.functionKey)
            .overlay(Image(systemName: "delete.left").font(.system(size: 18)).foregroundStyle(colors.keyText))
            .shadow(color: colors.keyShadow, radius: 0, x: 0, y: 0.5)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in
                        guard !pressed else { return }
                        pressed = true
                        model.keyDown()
                        model.inkBackspace()
                        let work = DispatchWorkItem {
                            repeating = true
                            model.backspaceRepeat(true)
                        }
                        hold = work
                        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5, execute: work)
                    }
                    .onEnded { _ in
                        pressed = false
                        hold?.cancel()
                        hold = nil
                        if repeating {
                            repeating = false
                            model.backspaceRepeat(false)
                        }
                    }
            )
            .accessibilityElement()
            .accessibilityLabel(Text("Delete"))
            .accessibilityAddTraits(.isKeyboardKey)
            .accessibilityAction { model.inkBackspace() }
    }
}
