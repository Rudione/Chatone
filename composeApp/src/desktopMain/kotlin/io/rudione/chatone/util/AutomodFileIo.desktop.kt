package io.rudione.chatone.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.util.Base64

actual suspend fun saveAutomodText(defaultName: String, content: String): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dialog = FileDialog(null as Frame?, "Export automod rules", FileDialog.SAVE).apply {
                file = defaultName
                isVisible = true
            }
            val dir = dialog.directory ?: return@withContext null
            val name = dialog.file ?: return@withContext null
            val target = File(dir, name)
            if (content.startsWith("XLSX_BASE64:")) {
                val bytes = Base64.getDecoder().decode(content.removePrefix("XLSX_BASE64:"))
                target.writeBytes(bytes)
            } else {
                target.writeText(content, Charsets.UTF_8)
            }
            target.absolutePath
        }.getOrNull()
    }

actual suspend fun readAutomodText(): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val dialog = FileDialog(null as Frame?, "Import automod rules", FileDialog.LOAD).apply {
                setFilenameFilter { _, n ->
                    n.endsWith(".json", true) || n.endsWith(".csv", true) ||
                            n.endsWith(".xlsx", true) || n.endsWith(".txt", true)
                }
                isVisible = true
            }
            val dir = dialog.directory ?: return@withContext null
            val name = dialog.file ?: return@withContext null
            val file = File(dir, name)
            when {
                name.endsWith(".xlsx", true) -> readXlsxAsCsv(file)
                else -> file.readText(Charsets.UTF_8)
            }
        }.getOrNull()
    }

private fun readXlsxAsCsv(file: File): String {
    val zip = java.util.zip.ZipFile(file)
    fun readEntry(name: String): String {
        val entry = zip.getEntry(name) ?: return ""
        return zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).readText()
    }

    val ssXml = readEntry("xl/sharedStrings.xml")
    val sharedStrings = Regex("<t[^>]*>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
        .findAll(ssXml).map { it.groupValues[1]
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">") }.toList()

    fun parseSheet(sheetPath: String): List<List<String>> {
        val xml = readEntry(sheetPath)
        val rows = mutableListOf<List<String>>()
        Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL).findAll(xml).forEach { rowMatch ->
            val cells = mutableListOf<String>()
            var lastCol = -1
            Regex("<c r=\"([A-Z]+)(\\d+)\"[^>]*(?:t=\"([^\"]*)\")[^>]*>.*?<v>(.*?)</v>", RegexOption.DOT_MATCHES_ALL)
                .findAll(rowMatch.groupValues[1]).forEach { cellMatch ->
                    val colStr = cellMatch.groupValues[1]
                    val colIdx = colStr.fold(0) { acc, c -> acc * 26 + (c - 'A' + 1) } - 1
                    repeat(colIdx - lastCol - 1) { cells.add("") }
                    lastCol = colIdx
                    val t = cellMatch.groupValues[3]
                    val v = cellMatch.groupValues[4]
                    cells.add(if (t == "s") sharedStrings.getOrElse(v.toIntOrNull() ?: -1) { v } else v)
                }
            if (cells.isNotEmpty()) rows.add(cells)
        }
        return rows
    }

    val sheet1 = parseSheet("xl/worksheets/sheet1.xml")
    val sheet2 = parseSheet("xl/worksheets/sheet2.xml")
    zip.close()

    return buildString {
        appendLine("# WORD FILTERS")
        sheet1.forEach { row -> appendLine(row.joinToString("\t")) }
        appendLine()
        appendLine("# CHAT RULES")
        sheet2.forEach { row -> appendLine(row.joinToString("\t")) }
    }
}

actual fun buildXlsxContent(
    wordRules: List<io.rudione.chatone.domain.model.AutomodRule>,
    chatRules: List<io.rudione.chatone.domain.model.ChatRule>
): String {
    val baos = java.io.ByteArrayOutputStream()
    java.util.zip.ZipOutputStream(baos).use { zip ->
        fun entry(name: String, content: String) {
            zip.putNextEntry(java.util.zip.ZipEntry(name))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        entry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/></Types>""")
        entry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
        entry("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/></Relationships>""")
        entry("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Word Filters" sheetId="1" r:id="rId1"/><sheet name="Chat Rules" sheetId="2" r:id="rId2"/></sheets></workbook>""")

        val strings = mutableListOf<String>()
        fun si(s: String): Int { val i = strings.size; strings.add(s); return i }

        fun buildSheet(headers: List<String>, rows: List<List<String>>): String {
            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            (listOf(headers) + rows).forEachIndexed { ri, row ->
                sb.append("<row r=\"${ri+1}\">")
                row.forEachIndexed { ci, cell ->
                    val col = ('A' + ci).toString()
                    sb.append("""<c r="$col${ri+1}" t="s"><v>${si(cell)}</v></c>""")
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
    return "XLSX_BASE64:" + Base64.getEncoder().encodeToString(baos.toByteArray())
}
