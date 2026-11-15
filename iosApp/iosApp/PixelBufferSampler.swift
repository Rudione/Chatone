import ComposeApp
import CoreVideo

enum PixelBufferSampler {
    static func sample(_ buffer: CVPixelBuffer, width: Int, height: Int) -> KotlinIntArray? {
        CVPixelBufferLockBaseAddress(buffer, .readOnly)
        defer { CVPixelBufferUnlockBaseAddress(buffer, .readOnly) }
        let sourceWidth = CVPixelBufferGetWidth(buffer)
        let sourceHeight = CVPixelBufferGetHeight(buffer)
        guard sourceWidth > 0, sourceHeight > 0 else { return nil }
        let reader: ((Int, Int) -> Int32)?
        switch CVPixelBufferGetPixelFormatType(buffer) {
        case kCVPixelFormatType_32BGRA:
            reader = bgraReader(buffer)
        case kCVPixelFormatType_420YpCbCr8BiPlanarVideoRange:
            reader = biPlanarReader(buffer, fullRange: false)
        case kCVPixelFormatType_420YpCbCr8BiPlanarFullRange:
            reader = biPlanarReader(buffer, fullRange: true)
        default:
            reader = nil
        }
        guard let reader else { return nil }
        let pixels = KotlinIntArray(size: Int32(width * height))
        for row in 0..<height {
            let y = row * sourceHeight / height
            for column in 0..<width {
                pixels.set(index: Int32(row * width + column), value: reader(column * sourceWidth / width, y))
            }
        }
        return pixels
    }

    private static func bgraReader(_ buffer: CVPixelBuffer) -> ((Int, Int) -> Int32)? {
        guard let base = CVPixelBufferGetBaseAddress(buffer) else { return nil }
        let stride = CVPixelBufferGetBytesPerRow(buffer)
        let bytes = base.assumingMemoryBound(to: UInt8.self)
        return { x, y in
            let offset = y * stride + x * 4
            return argb(red: Int(bytes[offset + 2]), green: Int(bytes[offset + 1]), blue: Int(bytes[offset]))
        }
    }

    private static func biPlanarReader(_ buffer: CVPixelBuffer, fullRange: Bool) -> ((Int, Int) -> Int32)? {
        guard CVPixelBufferGetPlaneCount(buffer) >= 2,
              let lumaBase = CVPixelBufferGetBaseAddressOfPlane(buffer, 0),
              let chromaBase = CVPixelBufferGetBaseAddressOfPlane(buffer, 1) else { return nil }
        let lumaStride = CVPixelBufferGetBytesPerRowOfPlane(buffer, 0)
        let chromaStride = CVPixelBufferGetBytesPerRowOfPlane(buffer, 1)
        let luma = lumaBase.assumingMemoryBound(to: UInt8.self)
        let chroma = chromaBase.assumingMemoryBound(to: UInt8.self)
        let lumaOffset = fullRange ? 0.0 : 16.0
        let lumaScale = fullRange ? 1.0 : 255.0 / 219.0
        let chromaScale = fullRange ? 1.0 : 255.0 / 224.0
        return { x, y in
            let chromaIndex = (y / 2) * chromaStride + (x / 2) * 2
            let brightness = (Double(luma[y * lumaStride + x]) - lumaOffset) * lumaScale
            let blueDifference = (Double(chroma[chromaIndex]) - 128.0) * chromaScale
            let redDifference = (Double(chroma[chromaIndex + 1]) - 128.0) * chromaScale
            return argb(
                red: Int(brightness + 1.5748 * redDifference),
                green: Int(brightness - 0.1873 * blueDifference - 0.4681 * redDifference),
                blue: Int(brightness + 1.8556 * blueDifference)
            )
        }
    }

    private static func argb(red: Int, green: Int, blue: Int) -> Int32 {
        let packed = 0xFF00_0000 | UInt32(clamp(red)) << 16 | UInt32(clamp(green)) << 8 | UInt32(clamp(blue))
        return Int32(bitPattern: packed)
    }

    private static func clamp(_ value: Int) -> Int {
        min(255, max(0, value))
    }
}
