// Draws the input menu icons (template images: black on transparent, macOS tints them).
// Run from macosApp/: `swift scripts/make-icons.swift`, then commit InputMethod/*.tiff.
import AppKit

func render(_ text: String, size: CGFloat, font: NSFont) -> Data {
    let scale: CGFloat = 1
    let px = Int(size * scale)
    let rep = NSBitmapImageRep(
        bitmapDataPlanes: nil, pixelsWide: px, pixelsHigh: px, bitsPerSample: 8, samplesPerPixel: 4,
        hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0
    )!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
    let attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: NSColor.black]
    let string = NSAttributedString(string: text, attributes: attrs)
    let bounds = string.boundingRect(with: NSSize(width: size * 2, height: size * 2), options: [.usesLineFragmentOrigin])
    string.draw(at: NSPoint(x: (size - bounds.width) / 2, y: (size - bounds.height) / 2))
    NSGraphicsContext.restoreGraphicsState()
    return rep.representation(using: .png, properties: [:])!
}

let tmp = FileManager.default.temporaryDirectory
for (name, text, weight) in [("Romanized", "Ne", NSFont.Weight.semibold), ("Traditional", "ने", NSFont.Weight.semibold)] {
    var files: [String] = []
    for size in [16.0, 32.0] {
        let font = NSFont.systemFont(ofSize: size * (text == "Ne" ? 0.62 : 0.7), weight: weight)
        let url = tmp.appendingPathComponent("\(name)-\(Int(size)).png")
        try! render(text, size: size, font: font).write(to: url)
        files.append(url.path)
    }
    // A 16 pt image with its 2x version in one TIFF
    let task = Process()
    task.executableURL = URL(fileURLWithPath: "/usr/bin/tiffutil")
    task.arguments = ["-cathidpicheck"] + files + ["-out", "InputMethod/\(name).tiff"]
    try! task.run()
    task.waitUntilExit()
}
