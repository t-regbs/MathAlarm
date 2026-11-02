// macOS asset generator: swift -module-cache-path /tmp/mathalarm-guide-cache scripts/generate_ios_alarm_permission_guide.swift
// Generates a silent, localized illustration of Settings → Apps → Math Alarm → Alarms.
// Requires ffmpeg. No screenshots, account information or recordings are embedded.
import AppKit
import Foundation

let root = URL(fileURLWithPath: FileManager.default.currentDirectoryPath)
let catalogURL = root.appendingPathComponent("iosApp/iosApp/Localizable.xcstrings")
let catalog = try JSONSerialization.jsonObject(with: Data(contentsOf: catalogURL)) as! [String: Any]
let strings = catalog["strings"] as! [String: Any]
let destination = root.appendingPathComponent("iosApp/iosApp/PermissionGuide", isDirectory: true)
let frames = root.appendingPathComponent("build/ios-design/permission-guide-frames", isDirectory: true)
try FileManager.default.createDirectory(at: destination, withIntermediateDirectories: true)
try FileManager.default.createDirectory(at: frames, withIntermediateDirectories: true)

func translation(_ key: String, _ locale: String) -> String {
    let entry = strings[key] as! [String: Any]
    let localizations = entry["localizations"] as! [String: Any]
    let localized = localizations[locale] as! [String: Any]
    return (localized["stringUnit"] as! [String: Any])["value"] as! String
}

let violet = NSColor(srgbRed: 0.33, green: 0.19, blue: 1, alpha: 1)
func rounded(_ rect: CGRect, _ radius: CGFloat, _ color: NSColor) {
    color.setFill(); NSBezierPath(roundedRect: rect, xRadius: radius, yRadius: radius).fill()
}
func text(_ value: String, x: CGFloat, y: CGFloat, width: CGFloat, size: CGFloat, bold: Bool = false,
          color: NSColor = .labelColor, center: Bool = false) {
    let style = NSMutableParagraphStyle()
    style.alignment = center ? .center : .left
    style.lineBreakMode = .byWordWrapping
    (value as NSString).draw(in: CGRect(x: x, y: y, width: width, height: 100), withAttributes: [
        .font: NSFont.systemFont(ofSize: size, weight: bold ? .semibold : .regular),
        .foregroundColor: color, .paragraphStyle: style
    ])
}

for locale in ["en", "es", "de", "ru", "pt", "hi", "pa", "bn", "zh"] {
    let settings = translation("Settings", locale), apps = translation("Apps", locale), alarms = translation("Alarms", locale)
    for frame in 0..<144 {
        let time = Double(frame) / 24
        let page = min(2, Int(time / 2))
        let image = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: 480, pixelsHigh: 640,
            bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB,
            bytesPerRow: 0, bitsPerPixel: 0)!
        NSGraphicsContext.saveGraphicsState()
        NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: image)
        let cg = NSGraphicsContext.current!.cgContext
        cg.translateBy(x: 0, y: 640); cg.scaleBy(x: 1, y: -1)
        // NSString drawing uses AppKit's flipped context for top-to-bottom text.
        NSGraphicsContext.current = NSGraphicsContext(cgContext: cg, flipped: true)
        NSColor(srgbRed: 0.95, green: 0.95, blue: 0.97, alpha: 1).setFill()
        NSBezierPath(rect: CGRect(x: 0, y: 0, width: 480, height: 640)).fill()
        text(page == 0 ? settings : page == 1 ? apps : "Math Alarm", x: 28, y: 48, width: 424, size: 36, bold: true)
        if page > 0 { text("‹ " + (page == 1 ? settings : apps), x: 28, y: 14, width: 424, size: 18, color: .systemBlue) }
        let rowY: CGFloat = 178
        rounded(CGRect(x: 24, y: rowY, width: 432, height: 78), 18, .white)
        if page < 2 {
            rounded(CGRect(x: 42, y: rowY + 19, width: 40, height: 40), 9, page == 0 ? .systemBlue : violet)
            let symbol = NSImage(systemSymbolName: page == 0 ? "square.grid.2x2.fill" : "alarm.fill", accessibilityDescription: nil)!
            let icon = symbol.withSymbolConfiguration(.init(pointSize: 24, weight: .medium))!
                .withSymbolConfiguration(.init(paletteColors: [.white]))!
            icon.draw(in: CGRect(x: 50, y: rowY + 27, width: 24, height: 24))
            text(page == 0 ? apps : "Math Alarm", x: 98, y: rowY + 23, width: 298, size: 25)
            text("›", x: 420, y: rowY + 19, width: 24, size: 30, color: .secondaryLabelColor)
        } else {
            text(alarms, x: 44, y: rowY + 23, width: 295, size: 25)
            let progress = CGFloat(min(1, max(0, (time - 4.8) / 0.3)))
            let track = NSColor(srgbRed: 0.78 * (1 - progress) + 0.2 * progress,
                green: 0.78 * (1 - progress) + 0.78 * progress,
                blue: 0.8 * (1 - progress) + 0.35 * progress, alpha: 1)
            rounded(CGRect(x: 360, y: rowY + 21, width: 68, height: 40), 20, track)
            rounded(CGRect(x: 363 + 28 * progress, y: rowY + 24, width: 34, height: 34), 17, .white)
        }
        // A pulsing touch cue makes the target legible at PiP size.
        let local = time - Double(page) * 2
        if local > 0.6 && local < 1.6 {
            let point = CGPoint(x: page == 2 ? 394 : 257, y: rowY + 40)
            let radius = CGFloat(23 + 7 * sin((local - 0.6) * .pi))
            violet.withAlphaComponent(0.2).setFill()
            NSBezierPath(ovalIn: CGRect(x: point.x - radius, y: point.y - radius, width: radius * 2, height: radius * 2)).fill()
            let outline = NSBezierPath(ovalIn: CGRect(x: point.x - radius, y: point.y - radius, width: radius * 2, height: radius * 2))
            outline.lineWidth = 3; violet.setStroke(); outline.stroke()
        }
        // Only the relevant Settings rows are illustrated; the route is always visible.
        text(settings + "  ›  " + apps + "  ›  Math Alarm", x: 28, y: 500, width: 424, size: 20,
             color: .secondaryLabelColor, center: true)
        for dot in 0..<3 {
            rounded(CGRect(x: 214 + CGFloat(dot) * 22, y: 586, width: 9, height: 9), 4.5,
                    dot == page ? violet : .tertiaryLabelColor)
        }
        NSGraphicsContext.restoreGraphicsState()
        let name = String(format: "frame-%03d.png", frame)
        try image.representation(using: .png, properties: [:])!.write(to: frames.appendingPathComponent(name))
        if frame == 0 {
            try image.representation(using: .png, properties: [:])!.write(to:
                destination.appendingPathComponent("alarm-settings-guide-\(locale).png"))
        }
    }
    let process = Process()
    process.executableURL = URL(fileURLWithPath: "/opt/homebrew/bin/ffmpeg")
    process.arguments = ["-hide_banner", "-loglevel", "error", "-y", "-framerate", "24", "-i",
        frames.appendingPathComponent("frame-%03d.png").path, "-an", "-c:v", "libx264", "-crf", "24",
        "-pix_fmt", "yuv420p", "-movflags", "+faststart", destination.appendingPathComponent("alarm-settings-guide-\(locale).mp4").path]
    try process.run(); process.waitUntilExit()
    precondition(process.terminationStatus == 0, "Video encoding failed")
    print("Generated silent Settings guide: \(locale)")
}
