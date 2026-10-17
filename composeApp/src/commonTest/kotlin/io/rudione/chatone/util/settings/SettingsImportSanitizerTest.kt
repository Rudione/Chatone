package io.rudione.chatone.util.settings

import io.rudione.chatone.data.remote.ImageUploaderClient
import io.rudione.chatone.domain.model.ImageUploaderConfig
import io.rudione.chatone.domain.model.UploaderFormField
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsImportSanitizerTest {

    private fun backup(vararg values: Pair<String, String>) =
        SettingsBackup(values = values.associate { (key, value) -> key to JsonPrimitive(value) })

    @Test
    fun importedUploaderArrivesDisabledButKeepsItsFields() {
        val uploader = """{"enabled":true,"askOnUpload":false,"requestUrl":"https://api.telegram.org/bot1:x/sendPhoto","formField":"photo"}"""
        val sanitized = SettingsImportExport.sanitizeUntrusted(backup("image_uploader_config" to uploader))
        val config = Json.parseToJsonElement(sanitized.values.getValue("image_uploader_config").jsonPrimitive.content).jsonObject
        assertFalse(config.getValue("enabled").jsonPrimitive.boolean)
        assertEquals("photo", config.getValue("formField").jsonPrimitive.content)
    }

    @Test
    fun brokenUploaderPayloadIsDropped() {
        val sanitized = SettingsImportExport.sanitizeUntrusted(backup("image_uploader_config" to "not json"))
        assertNull(sanitized.values["image_uploader_config"])
    }

    @Test
    fun networkAndRemotePathsAreNeverImported() {
        val sanitized = SettingsImportExport.sanitizeUntrusted(
            backup(
                "wallpaper_path" to "\\\\203.0.113.7\\share\\bg.png",
                "custom_sound_path" to "https://example.com/ping.wav",
                "font_size" to "14"
            )
        )
        assertNull(sanitized.values["wallpaper_path"])
        assertNull(sanitized.values["custom_sound_path"])
        assertEquals("14", sanitized.values.getValue("font_size").jsonPrimitive.content)
    }

    @Test
    fun unknownKeysAreDropped() {
        val sanitized = SettingsImportExport.sanitizeUntrusted(backup("vault_dpapi_pepper_v1" to "x"))
        assertTrue(sanitized.values.isEmpty())
    }

    @Test
    fun onlyPlainAbsoluteLocalPathsCount() {
        assertTrue(SettingsImportExport.isPlainLocalPath("C:\\Users\\me\\Pictures\\bg.png"))
        assertTrue(SettingsImportExport.isPlainLocalPath("D:/sounds/ping.wav"))
        assertTrue(SettingsImportExport.isPlainLocalPath("/home/me/bg.png"))
        assertFalse(SettingsImportExport.isPlainLocalPath("\\\\server\\share\\bg.png"))
        assertFalse(SettingsImportExport.isPlainLocalPath("//server/share/bg.png"))
        assertFalse(SettingsImportExport.isPlainLocalPath("/\\server\\share\\bg.png"))
        assertFalse(SettingsImportExport.isPlainLocalPath("\\\\?\\UNC\\server\\share\\bg.png"))
        assertFalse(SettingsImportExport.isPlainLocalPath("\\\\.\\pipe\\x"))
        assertFalse(SettingsImportExport.isPlainLocalPath("file://server/share/bg.png"))
        assertFalse(SettingsImportExport.isPlainLocalPath("C:bg.png"))
        assertFalse(SettingsImportExport.isPlainLocalPath("bg.png"))
        assertFalse(SettingsImportExport.isPlainLocalPath("C:\\bg.png\u0000"))
        assertFalse(SettingsImportExport.isPlainLocalPath(""))
    }

    @Test
    fun exportRedactsUploaderSecretsIncludingCustomFields() {
        val uploader = """{"requestUrl":"https://catbox.moe/user/api.php","extraHeaders":"Authorization: x","formFields":[{"name":"key","value":"secret"}]}"""
        val redacted = SettingsImportExport.withSecretsRedacted("image_uploader_config", JsonPrimitive(uploader))
        val config = Json.parseToJsonElement(redacted.jsonPrimitive.content).jsonObject
        assertEquals("", config.getValue("extraHeaders").jsonPrimitive.content)
        assertTrue(config.getValue("formFields").jsonArray.isEmpty())
        assertEquals("https://catbox.moe/user/api.php", config.getValue("requestUrl").jsonPrimitive.content)
    }

    @Test
    fun customFormFieldsSkipBlankNamesAndTheFileField() {
        val config = ImageUploaderConfig(
            formField = "fileToUpload",
            formFields = listOf(
                UploaderFormField(" reqtype ", "fileupload"),
                UploaderFormField("", "orphan"),
                UploaderFormField("FileToUpload", "clash"),
                UploaderFormField("reqtype", "duplicate")
            )
        )
        assertEquals(listOf(UploaderFormField("reqtype", "fileupload")), config.extraFormFields)
    }

    @Test
    fun uploaderStartsWithTheStandardService() {
        val config = ImageUploaderConfig()
        assertEquals("https://p.ftk.li/upload", config.requestUrl)
        assertEquals("image", config.formField)
        assertEquals("POST", config.extraHeaders)
        assertEquals("{url}", config.linkFormat)
        assertEquals("", config.deletionLinkFormat)
    }

    @Test
    fun unconfiguredUploaderGetsTheStandardServiceButOwnSetupStays() {
        val blank = ImageUploaderConfig(requestUrl = "", formField = "", extraHeaders = "", linkFormat = "", enabled = true)
        assertEquals(ImageUploaderConfig(enabled = true), blank.withStandardService())
        val own = ImageUploaderConfig(requestUrl = "https://i.nuuls.com/upload", formField = "attachment", extraHeaders = "")
        assertEquals(own, own.withStandardService())
    }

    @Test
    fun uploaderSecretsAreSealedAndOpenedTogether() {
        val config = ImageUploaderConfig(extraHeaders = "h", formFields = listOf(UploaderFormField("key", "v")))
        val sealed = config.sealed { "<$it>" }
        assertEquals("<h>", sealed.extraHeaders)
        assertEquals("<v>", sealed.formFields.single().value)
        assertEquals(config, sealed.opened { it.removeSurrounding("<", ">") })
    }

    @Test
    fun uploaderDestinationIsTheLowercasedHost() {
        assertEquals("i.nuuls.com", ImageUploaderClient.destinationHost(" https://I.Nuuls.com/upload "))
        assertEquals("api.telegram.org", ImageUploaderClient.destinationHost("https://api.telegram.org/bot1:x/sendPhoto?chat_id=1"))
        assertNull(ImageUploaderClient.destinationHost("ftp://example.com/upload"))
        assertNull(ImageUploaderClient.destinationHost("not a url"))
        assertNull(ImageUploaderClient.destinationHost(""))
    }
}
