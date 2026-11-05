package io.rudione.chatone.tv.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

internal class QrMatrix(val size: Int, private val cells: BooleanArray) {
    fun isDark(x: Int, y: Int): Boolean = cells[y * size + x]
}

internal fun encodeQr(content: String): QrMatrix {
    val hints = mapOf(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        EncodeHintType.MARGIN to 0
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
    val size = matrix.width
    return QrMatrix(
        size,
        BooleanArray(size * size) { index -> matrix.get(index % size, index / size) })
}

@Composable
fun TvQrCode(content: String, modifier: Modifier = Modifier, color: Color = Color.Black) {
    val matrix = remember(content) { encodeQr(content) }
    Canvas(modifier) {
        val cell = size.minDimension / matrix.size
        val module = Size(cell * 0.92f, cell * 0.92f)
        val radius = CornerRadius(cell * 0.28f)
        for (y in 0 until matrix.size) {
            for (x in 0 until matrix.size) {
                if (matrix.isDark(x, y)) {
                    drawRoundRect(color, Offset(x * cell, y * cell), module, radius)
                }
            }
        }
    }
}
