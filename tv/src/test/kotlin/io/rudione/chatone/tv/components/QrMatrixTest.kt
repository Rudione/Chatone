package io.rudione.chatone.tv.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QrMatrixTest {

    @Test
    fun activationLinkFitsAScannableSquare() {
        val matrix = encodeQr("https://www.twitch.tv/activate?device-code=MFXFPBPT")
        assertTrue(matrix.size >= 21)
        assertTrue(matrix.isDark(0, 0))
        assertTrue(matrix.isDark(matrix.size - 1, 0))
        assertTrue(matrix.isDark(0, matrix.size - 1))
        assertEquals((matrix.size - 17) % 4, 0)
    }
}
