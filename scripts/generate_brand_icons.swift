import Foundation
import CoreGraphics
import ImageIO
import UniformTypeIdentifiers
let root = FileManager.default.currentDirectoryPath
let image = CGImageSourceCreateImageAtIndex(CGImageSourceCreateWithURL(URL(fileURLWithPath:root + "/store-assets/brand-20261019/subty-logo-purple-master.png") as CFURL,nil)!,0,nil)!
let outputs = [("store-assets/brand-20261019/subty-icon-512.png",512),("app/src/main/res/drawable/ic_launcher_foreground.png",432)] + ["en-US","he-IL"].map { ("fastlane/metadata/android/\($0)/images/icon.png",512) } + [("mdpi",48),("hdpi",72),("xhdpi",96),("xxhdpi",144),("xxxhdpi",192)].flatMap { d,s in ["ic_launcher","ic_launcher_round"].map { ("app/src/main/res/mipmap-\(d)/\($0).png",s) } }
for (output,size) in outputs {
 let ctx = CGContext(data:nil,width:size,height:size,bitsPerComponent:8,bytesPerRow:0,space:CGColorSpaceCreateDeviceRGB(),bitmapInfo:CGImageAlphaInfo.noneSkipLast.rawValue)!
 ctx.interpolationQuality = .high
 ctx.draw(image,in:CGRect(x:0,y:0,width:size,height:size))
 let url=URL(fileURLWithPath:root + "/" + output)
 try FileManager.default.createDirectory(at:url.deletingLastPathComponent(),withIntermediateDirectories:true)
 let destination=CGImageDestinationCreateWithURL(url as CFURL, UTType.png.identifier as CFString,1,nil)!
 CGImageDestinationAddImage(destination,ctx.makeImage()!,nil)
 guard CGImageDestinationFinalize(destination) else { fatalError(output) }
}
print("Generated launcher and store icons")
