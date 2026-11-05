package io.rudione.chatone.presentation.stream

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemVideoOutput
import platform.AVFoundation.addOutput
import platform.CoreVideo.CVPixelBufferGetBaseAddress
import platform.CoreVideo.CVPixelBufferGetBytesPerRow
import platform.CoreVideo.CVPixelBufferGetHeight
import platform.CoreVideo.CVPixelBufferGetWidth
import platform.CoreVideo.CVPixelBufferLockBaseAddress
import platform.CoreVideo.CVPixelBufferRelease
import platform.CoreVideo.CVPixelBufferUnlockBaseAddress
import platform.CoreVideo.kCVPixelBufferLock_ReadOnly
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.Foundation.NSNumber
import platform.QuartzCore.CACurrentMediaTime

@OptIn(ExperimentalForeignApi::class)
internal class VideoFrameSampler {
    private var output: AVPlayerItemVideoOutput? = null

    fun attach(item: AVPlayerItem) {
        val created = AVPlayerItemVideoOutput(
            pixelBufferAttributes = mapOf<Any?, Any?>(PIXEL_FORMAT_KEY to NSNumber(unsignedInt = kCVPixelFormatType_32BGRA))
        )
        item.addOutput(created)
        output = created
    }

    fun sample(width: Int, height: Int): IntArray? {
        val current = output ?: return null
        val time = current.itemTimeForHostTime(CACurrentMediaTime())
        val buffer = current.copyPixelBufferForItemTime(time, itemTimeForDisplay = null) ?: return null
        CVPixelBufferLockBaseAddress(buffer, kCVPixelBufferLock_ReadOnly)
        try {
            val base = CVPixelBufferGetBaseAddress(buffer)?.reinterpret<ByteVar>() ?: return null
            val sourceWidth = CVPixelBufferGetWidth(buffer).toInt()
            val sourceHeight = CVPixelBufferGetHeight(buffer).toInt()
            val stride = CVPixelBufferGetBytesPerRow(buffer).toInt()
            if (sourceWidth <= 0 || sourceHeight <= 0) return null
            return IntArray(width * height) { index ->
                val x = (index % width) * sourceWidth / width
                val y = (index / width) * sourceHeight / height
                val offset = y * stride + x * BYTES_PER_PIXEL
                val blue = base[offset].toInt() and 0xFF
                val green = base[offset + 1].toInt() and 0xFF
                val red = base[offset + 2].toInt() and 0xFF
                (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
            }
        } finally {
            CVPixelBufferUnlockBaseAddress(buffer, kCVPixelBufferLock_ReadOnly)
            CVPixelBufferRelease(buffer)
        }
    }

    private companion object {
        const val PIXEL_FORMAT_KEY = "PixelFormatType"
        const val BYTES_PER_PIXEL = 4
    }
}
