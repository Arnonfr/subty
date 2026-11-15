import Foundation
import CoreGraphics
import CoreText
import ImageIO
import UniformTypeIdentifiers

// Only composes real app captures with editable typography; never fabricates UI.
let root = FileManager.default.currentDirectoryPath
let captureDir = root + "/artifacts/release-20261019"
let brandDir = root + "/store-assets/brand-20261019"
func color(_ hex: UInt32) -> CGColor {
    CGColor(red: CGFloat((hex >> 16) & 255)/255, green: CGFloat((hex >> 8) & 255)/255, blue: CGFloat(hex & 255)/255, alpha:1)
}
let cream = color(0xFFFCFA), purple = color(0x9C6BFF), plum = color(0x201727), muted = color(0x7F5A99)
func load(_ path: String) -> CGImage {
    guard let source = CGImageSourceCreateWithURL(URL(fileURLWithPath:path) as CFURL,nil), let image = CGImageSourceCreateImageAtIndex(source,0,nil) else { fatalError("Missing image: \(path)") }
    return image
}
func canvas(_ width: Int, _ height: Int) -> CGContext {
    let ctx = CGContext(data:nil,width:width,height:height,bitsPerComponent:8,bytesPerRow:0,space:CGColorSpaceCreateDeviceRGB(),bitmapInfo:CGImageAlphaInfo.noneSkipLast.rawValue)!
    ctx.setFillColor(cream); ctx.fill(CGRect(x:0,y:0,width:width,height:height)); ctx.interpolationQuality = .high
    return ctx
}
func text(_ value: String, in rect: CGRect, size: CGFloat, color: CGColor, bold: Bool = false, rtl: Bool = false, ctx: CGContext) {
    let font = CTFontCreateWithName((bold ? "HelveticaNeue-Bold" : "HelveticaNeue") as CFString,size,nil)
    var alignment: CTTextAlignment = rtl ? .right : .left
    var direction: CTWritingDirection = rtl ? .rightToLeft : .leftToRight
    let style = withUnsafePointer(to:&alignment) { a in
        withUnsafePointer(to:&direction) { d in
            var settings = [CTParagraphStyleSetting(spec:.alignment,valueSize:MemoryLayout<CTTextAlignment>.size,value:a),CTParagraphStyleSetting(spec:.baseWritingDirection,valueSize:MemoryLayout<CTWritingDirection>.size,value:d)]
            return CTParagraphStyleCreate(&settings,settings.count)
        }
    }
    let attributes:[NSAttributedString.Key:Any] = [NSAttributedString.Key(kCTFontAttributeName as String):font,NSAttributedString.Key(kCTForegroundColorAttributeName as String):color,NSAttributedString.Key(kCTParagraphStyleAttributeName as String):style]
    let framesetter = CTFramesetterCreateWithAttributedString(NSAttributedString(string:value,attributes:attributes))
    let frame = CTFramesetterCreateFrame(framesetter,CFRange(location:0,length:0),CGPath(rect:rect,transform:nil),nil)
    CTFrameDraw(frame,ctx)
}
func save(_ ctx: CGContext, _ path: String) throws {
    let url=URL(fileURLWithPath:path)
    try FileManager.default.createDirectory(at:url.deletingLastPathComponent(),withIntermediateDirectories:true)
    let destination=CGImageDestinationCreateWithURL(url as CFURL,UTType.png.identifier as CFString,1,nil)!
    CGImageDestinationAddImage(destination,ctx.makeImage()!,nil)
    guard CGImageDestinationFinalize(destination) else { fatalError("Cannot export \(path)") }
}
let icon = load(brandDir + "/subty-icon-512.png")
let shots:[(String,String,String)] = [
    ("01_search.png","Your next subtitle,\nfound.","Search movies and TV shows."),
    ("02_series.png","Right show.\nRight episode.","Choose a season. Pick an episode."),
    ("03_results.png","Find a file.\nKeep watching.","Compare sources and download."),
    ("04_history.png","Keep your subtitles.\nFind them again.","Recent searches and saved translations.")
]
for locale in ["en-US","he-IL"] {
    let rtl=false // User requires English content even for the Hebrew/Israel audience.
    let dir=root + "/fastlane/metadata/android/\(locale)/images"
    for (index,shot) in shots.enumerated() {
        let ctx=canvas(1080,1920)
        ctx.setFillColor(color(0xF0E7FF)); ctx.fillEllipse(in:CGRect(x:730,y:1100,width:800,height:800))
        ctx.setFillColor(color(0xFFF0E8)); ctx.fill(CGRect(x:0,y:0,width:1080,height:620))
        ctx.draw(icon,in:CGRect(x:72,y:1800,width:72,height:72))
        text("Subty",in:CGRect(x:155,y:1802,width:500,height:60),size:36,color:plum,bold:true,ctx:ctx)
        text(String(format:"%02d / 04",index+1),in:CGRect(x:842,y:1812,width:180,height:40),size:24,color:muted,ctx:ctx)
        text(shot.1,in:CGRect(x:78,y:1610,width:924,height:175),size:72,color:plum,bold:true,rtl:rtl,ctx:ctx)
        text(shot.2,in:CGRect(x:78,y:1530,width:924,height:60),size:32,color:muted,rtl:rtl,ctx:ctx)
        let image=load(captureDir + "/" + shot.0)
        let w:CGFloat=784, h=CGFloat(image.height)*784/CGFloat(image.width)
        let rect=CGRect(x:148,y:76,width:w,height:h)
        let frame=rect.insetBy(dx:-13,dy:-13)
        ctx.setFillColor(plum); ctx.addPath(CGPath(roundedRect:frame,cornerWidth:39,cornerHeight:39,transform:nil));ctx.fillPath()
        ctx.saveGState();ctx.addPath(CGPath(roundedRect:rect,cornerWidth:26,cornerHeight:26,transform:nil));ctx.clip();ctx.draw(image,in:rect);ctx.restoreGState()
        try save(ctx,dir + "/phoneScreenshots/" + shot.0)
    }
    let ctx=canvas(1024,500)
    ctx.setFillColor(color(0xF0E7FF));ctx.fillEllipse(in:CGRect(x:770,y:-310,width:700,height:700))
    ctx.draw(icon,in:CGRect(x:50,y:104,width:260,height:260))
    text("Subty",in:CGRect(x:340,y:285,width:620,height:115),size:82,color:plum,bold:true,ctx:ctx)
    text("Subtitles, in your language.",in:CGRect(x:340,y:200,width:620,height:80),size:38,color:plum,rtl:rtl,ctx:ctx)
    text("Search · Download · Translate on-device",in:CGRect(x:340,y:118,width:620,height:60),size:25,color:muted,rtl:rtl,ctx:ctx)
    ctx.setFillColor(purple);ctx.fill(CGRect(x:340,y:100,width:70,height:6))
    try save(ctx,dir + "/featureGraphic.png")
}
print("Exported English-only store content for both locale slots")
