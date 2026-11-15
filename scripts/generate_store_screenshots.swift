import AppKit

struct Screenshot {
    let input: String
    let output: String
    let title: String
    let subtitle: String
}

let screenshots = [
    Screenshot(
        input: "/tmp/subty-lab-import-dive.png",
        output: "fastlane/metadata/android/en-US/images/phoneScreenshots/01_search_home.png",
        title: "Find the right subtitles",
        subtitle: "Search by movie, series or episode."
    ),
    Screenshot(
        input: "/tmp/subty-search-current.png",
        output: "fastlane/metadata/android/en-US/images/phoneScreenshots/02_series_episode_picker.png",
        title: "Pick your language",
        subtitle: "Keep the subtitle search focused."
    ),
    Screenshot(
        input: "/tmp/subty-results-current.png",
        output: "fastlane/metadata/android/en-US/images/phoneScreenshots/03_subtitle_results_sources.png",
        title: "Download, then watch",
        subtitle: "Sources and details in one place."
    )
]

let canvasSize = NSSize(width: 1080, height: 1920)
let screenFrame = NSRect(x: 130, y: 350, width: 820, height: 1050)
let titleFont = NSFont.systemFont(ofSize: 58, weight: .bold)
let subtitleFont = NSFont.systemFont(ofSize: 28, weight: .regular)
let titleStyle = NSMutableParagraphStyle()
titleStyle.alignment = .center
let subtitleStyle = NSMutableParagraphStyle()
subtitleStyle.alignment = .center

for shot in screenshots {
    guard let source = NSImage(contentsOfFile: shot.input) else {
        fatalError("Cannot read \(shot.input)")
    }
    let image = NSImage(size: canvasSize)
    image.lockFocus()

    NSColor(calibratedRed: 0.11, green: 0.075, blue: 0.19, alpha: 1).setFill()
    NSBezierPath(rect: NSRect(origin: .zero, size: canvasSize)).fill()

    NSColor(calibratedRed: 0.61, green: 0.39, blue: 0.96, alpha: 1).setFill()
    NSBezierPath(ovalIn: NSRect(x: -240, y: 1510, width: 650, height: 650)).fill()
    NSColor(calibratedRed: 0.36, green: 0.23, blue: 0.68, alpha: 1).setFill()
    NSBezierPath(ovalIn: NSRect(x: 780, y: 1510, width: 430, height: 430)).fill()

    let titleAttributes: [NSAttributedString.Key: Any] = [
        .font: titleFont, .foregroundColor: NSColor.white, .paragraphStyle: titleStyle
    ]
    let subtitleAttributes: [NSAttributedString.Key: Any] = [
        .font: subtitleFont, .foregroundColor: NSColor(calibratedWhite: 0.88, alpha: 1), .paragraphStyle: subtitleStyle
    ]
    (shot.title as NSString).draw(in: NSRect(x: 60, y: 1770, width: 960, height: 75), withAttributes: titleAttributes)
    (shot.subtitle as NSString).draw(in: NSRect(x: 60, y: 1715, width: 960, height: 45), withAttributes: subtitleAttributes)

    NSColor(calibratedWhite: 0, alpha: 0.26).setFill()
    NSBezierPath(roundedRect: screenFrame.offsetBy(dx: 0, dy: -12), xRadius: 42, yRadius: 42).fill()
    NSGraphicsContext.saveGraphicsState()
    NSBezierPath(roundedRect: screenFrame, xRadius: 38, yRadius: 38).addClip()
    let cropHeight = source.size.width / screenFrame.width * screenFrame.height
    let sourceRect = NSRect(x: 0, y: source.size.height - cropHeight, width: source.size.width, height: cropHeight)
    source.draw(in: screenFrame, from: sourceRect, operation: .sourceOver, fraction: 1)
    NSGraphicsContext.restoreGraphicsState()

    NSColor.white.setStroke()
    let border = NSBezierPath(roundedRect: screenFrame, xRadius: 38, yRadius: 38)
    border.lineWidth = 3
    border.stroke()
    image.unlockFocus()

    guard let data = image.tiffRepresentation,
          let bitmap = NSBitmapImageRep(data: data),
          let png = bitmap.representation(using: .png, properties: [:]) else {
        fatalError("Cannot encode \(shot.output)")
    }
    try png.write(to: URL(fileURLWithPath: shot.output))
}
