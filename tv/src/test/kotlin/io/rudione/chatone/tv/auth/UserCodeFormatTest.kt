package io.rudione.chatone.tv.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class UserCodeFormatTest {

    @Test
    fun longCodesAreSplitForReading() {
        assertEquals("MFXF PBPT", formatUserCode("MFXFPBPT"))
        assertEquals("ABCD", formatUserCode("ABCD"))
    }
}
