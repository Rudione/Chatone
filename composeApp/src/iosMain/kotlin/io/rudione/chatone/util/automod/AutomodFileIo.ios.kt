package io.rudione.chatone.util.automod

import io.rudione.chatone.domain.model.AutomodRule
import io.rudione.chatone.domain.model.ChatRule
import io.rudione.chatone.util.media.IosFilePicker
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.stringWithContentsOfFile
import platform.UniformTypeIdentifiers.UTTypeCommaSeparatedText
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.UniformTypeIdentifiers.UTTypePlainText
import platform.UniformTypeIdentifiers.UTTypeTabSeparatedText

private const val XLSX_PREFIX = "XLSX_BASE64:"
private const val IMPORT_FOLDER = "imports"

actual suspend fun saveAutomodText(defaultName: String, content: String): String? {
    if (content.isBlank()) return null
    val data = exportData(content) ?: return null
    return IosFilePicker.export(exportName(defaultName, content), data)
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun readAutomodText(): String? {
    val types = listOf(UTTypeJSON, UTTypeCommaSeparatedText, UTTypeTabSeparatedText, UTTypePlainText)
    val path = IosFilePicker.pickDocument(types, IMPORT_FOLDER) ?: return null
    return NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null)
}

actual fun buildXlsxContent(wordRules: List<AutomodRule>, chatRules: List<ChatRule>): String =
    AutomodImportExport.toCsv(wordRules, chatRules)

internal fun exportName(defaultName: String, content: String): String =
    if (defaultName.endsWith(".xlsx", ignoreCase = true) && !content.startsWith(XLSX_PREFIX)) {
        defaultName.dropLast(".xlsx".length) + ".csv"
    } else {
        defaultName
    }

@OptIn(BetaInteropApi::class)
private fun exportData(content: String): NSData? =
    if (content.startsWith(XLSX_PREFIX)) {
        NSData.create(base64EncodedString = content.removePrefix(XLSX_PREFIX), options = 0u)
    } else {
        NSString.create(string = content).dataUsingEncoding(NSUTF8StringEncoding)
    }
