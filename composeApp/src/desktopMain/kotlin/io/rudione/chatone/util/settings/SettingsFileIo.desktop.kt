package io.rudione.chatone.util.settings

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import java.util.Base64

actual fun buildSettingsXlsx(backup: SettingsBackup): String {
    val baos = java.io.ByteArrayOutputStream()
    java.util.zip.ZipOutputStream(baos).use { zip ->
        fun entry(name: String, content: String) {
            zip.putNextEntry(java.util.zip.ZipEntry(name))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }

        entry(
            "[Content_Types].xml",
            """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/></Types>"""
        )
        entry(
            "_rels/.rels",
            """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>"""
        )
        entry(
            "xl/_rels/workbook.xml.rels",
            """<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/></Relationships>"""
        )
        entry(
            "xl/workbook.xml",
            """<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Settings" sheetId="1" r:id="rId1"/></sheets></workbook>"""
        )

        val strings = mutableListOf<String>()
        fun si(s: String): Int { val i = strings.size; strings.add(s); return i }

        val rows = mutableListOf<List<String>>()
        rows.add(listOf("key", "value", "type"))
        backup.values.toSortedMap().forEach { (k, v) ->
            val (value, type) = primitiveToPair(v)
            rows.add(listOf(k, value, type))
        }

        val sheet = buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
            rows.forEachIndexed { ri, row ->
                append("<row r=\"${ri + 1}\">")
                row.forEachIndexed { ci, cell ->
                    val col = ('A' + ci).toString()
                    append("""<c r="$col${ri + 1}" t="s"><v>${si(cell)}</v></c>""")
                }
                append("</row>")
            }
            append("</sheetData></worksheet>")
        }
        entry("xl/worksheets/sheet1.xml", sheet)

        val sst = buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?><sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${strings.size}" uniqueCount="${strings.size}">""")
            strings.forEach {
                append(
                    "<si><t>" + it
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;") + "</t></si>"
                )
            }
            append("</sst>")
        }
        entry("xl/sharedStrings.xml", sst)
    }
    return "XLSX_BASE64:" + Base64.getEncoder().encodeToString(baos.toByteArray())
}

private fun primitiveToPair(element: JsonElement): Pair<String, String> {
    if (element !is JsonPrimitive) return element.toString() to "json"
    return when {
        element.isString -> (element.contentOrNull ?: "") to "string"
        element.booleanOrNull != null -> element.boolean.toString() to "boolean"
        element.intOrNull != null -> element.intOrNull.toString() to "int"
        element.longOrNull != null -> element.longOrNull.toString() to "long"
        element.floatOrNull != null -> element.floatOrNull.toString() to "float"
        element.doubleOrNull != null -> element.doubleOrNull.toString() to "float"
        else -> element.contentOrNull.orEmpty() to "string"
    }
}
