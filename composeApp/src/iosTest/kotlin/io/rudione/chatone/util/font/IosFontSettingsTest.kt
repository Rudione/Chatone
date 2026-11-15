@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class, ExperimentalTextApi::class)

package io.rudione.chatone.util.font

import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.platform.SystemFont
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.writeToFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import androidx.compose.ui.text.ExperimentalTextApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi

class IosFontSettingsTest {
    @Test fun installedFontSelectionUsesTheRequestedFamily() {
        val available = listAvailableFontNames(emptyList())
        val selected = available.first { it == "Helvetica Neue" || it == "Arial" }
        val family = assertIs<FontListFontFamily>(resolveFontFamily(selected, emptyList()))
        assertEquals(selected, assertIs<SystemFont>(family.fonts.first()).identity)
    }

    @Test fun importedFontNamesUseSafeKotlinPathHandling() {
        val path = NSTemporaryDirectory() + "chatone-font-settings.TEST.TTF"
        NSData.create(base64EncodedString = "AAECAw==", options = 0u)?.writeToFile(path, atomically = true)
        try {
            assertTrue("chatone-font-settings.TEST" in listAvailableFontNames(listOf(path)))
        } finally {
            NSFileManager.defaultManager.removeItemAtPath(path, error = null)
        }
    }
}
