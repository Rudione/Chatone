package io.rudione.chatone.util

import android.app.Application
import android.util.Base64
import io.github.aakira.napier.Napier
import io.rudione.chatone.domain.model.AutomodRule
import io.rudione.chatone.domain.model.ChatRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


actual suspend fun saveAutomodText(defaultName: String, content: String): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val app = KoinPlatform.getKoin().get<Application>()
            val dir = app.getExternalFilesDir("automod") ?: app.filesDir.resolve("automod").also { it.mkdirs() }
            dir.mkdirs()
            val target = File(dir, defaultName)
            target.writeText(content, Charsets.UTF_8)
            target.absolutePath
        }.onFailure { Napier.e("Automod save failed", it, tag = "Automod") }.getOrNull()
    }

actual suspend fun readAutomodText(): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val app = KoinPlatform.getKoin().get<Application>()
            val dir = app.getExternalFilesDir("automod") ?: app.filesDir.resolve("automod")
            val candidates = dir.listFiles { f ->
                f.isFile && (f.extension.equals("json", true) || f.extension.equals("md", true) || f.extension.equals("txt", true))
            }.orEmpty().sortedByDescending { it.lastModified() }
            candidates.firstOrNull()?.readText(Charsets.UTF_8)
        }.onFailure { Napier.e("Automod read failed", it, tag = "Automod") }.getOrNull()
    }

actual fun buildXlsxContent(wordRules: List<AutomodRule>, chatRules: List<ChatRule>): String {
    val baos = ByteArrayOutputStream()
    ZipOutputStream(baos).use { zip ->
        fun entry(name: String, content: String) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        entry(
            "[Content_Types].xml",
            """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/></Types>"""
        )
        entry(
            "_rels/.rels",
            """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""
        )
        entry(
            "xl/_rels/workbook.xml.rels",
            """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/></Relationships>"""
        )
        entry(
            "xl/workbook.xml",
            """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Word Filters" sheetId="1" r:id="rId1"/><sheet name="Chat Rules" sheetId="2" r:id="rId2"/></sheets></workbook>"""
        )

        val strings = mutableListOf<String>()
        fun si(s: String): Int { val i = strings.size; strings.add(s); return i }

        fun buildSheet(headers: List<String>, rows: List<List<String>>): String {
            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            (listOf(headers) + rows).forEachIndexed { ri, row ->
                sb.append("<row r=\"${ri + 1}\">")
                row.forEachIndexed { ci, cell ->
                    val col = ('A' + ci).toString()
                    sb.append("""<c r="$col${ri + 1}" t="s"><v>${si(cell)}</v></c>""")
                }
                sb.append("</row>")
            }
            sb.append("</sheetData></worksheet>")
            return sb.toString()
        }

        val wh = listOf("ID","Scope","Channel","Pattern","Alternates","Action","TimeoutMs","Regex","WholeWord","CaseSensitive","FreqThreshold","FreqWindowMs","ExemptMods","ExemptSubs","ExemptVips","Enabled","Note")
        val ch = listOf("ID","Type","Scope","Channel","Action","TimeoutSec","SpamMaxMsg","SpamWindowSec","CapsPercent","CapsMinLen","AllowClips","AllowedSites","RequireHttps","EmoteMax","NewAcctDays","DuplicateMin","ConsecutiveNums","ExemptMods","ExemptVips","ExemptSubs","Enabled")

        val wr = wordRules.map { r -> listOf(r.id, r.scope.name, r.channelLogin ?: "", r.pattern, r.alternates.joinToString("; "), r.action.name, r.timeoutMs.toString(), r.isRegex.toString(), r.wholeWord.toString(), r.caseSensitive.toString(), r.frequencyThreshold.toString(), r.frequencyWindowMs.toString(), r.exemptMods.toString(), r.exemptSubs.toString(), r.exemptVips.toString(), r.enabled.toString(), r.note) }
        val cr = chatRules.map { r -> listOf(r.id, r.type.name, r.scope.name, r.channelLogin ?: "", r.action.name, r.timeoutSeconds.toString(), r.spamMaxMessages.toString(), r.spamWindowSeconds.toString(), r.capsThresholdPercent.toString(), r.capsMinLength.toString(), r.linksAllowClips.toString(), r.linksAllowedSites.joinToString("; "), r.linksRequireHttps.toString(), r.emoteMaxCount.toString(), r.newAccountAgeDays.toString(), r.duplicateMinLength.toString(), r.consecutiveNumbersThreshold.toString(), r.exemptMods.toString(), r.exemptVips.toString(), r.exemptSubs.toString(), r.enabled.toString()) }

        entry("xl/worksheets/sheet1.xml", buildSheet(wh, wr))
        entry("xl/worksheets/sheet2.xml", buildSheet(ch, cr))

        val sst = buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?><sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${strings.size}" uniqueCount="${strings.size}">""")
            strings.forEach { append("<si><t>${it.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")}</t></si>") }
            append("</sst>")
        }
        entry("xl/sharedStrings.xml", sst)
    }
    return "XLSX_BASE64:" + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
}
