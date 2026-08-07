package io.rudione.chatone.util.settings

import com.russhwolf.settings.Settings
import io.github.aakira.napier.Napier
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

@Serializable
data class SettingsBackup(
    val version: Int = 1,
    val exportedAt: Long = 0L,
    val values: Map<String, JsonElement> = emptyMap()
)

object SettingsImportExport {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val PORTABLE_KEYS: List<String> = listOf(
        "dark_theme",
        "timestamp_format",
        "show_deleted",
        "scrollback_limit",
        "emote_size",
        "show_badges",
        "font_size",
        "default_timeout",
        "confirm_mod_actions",
        "channel_navigation",
        "highlight_rules",
        "mention_sound",
        "mention_volume",
        "custom_sound_path",
        "always_on_top",
        "ui_scale",
        "language",
        "pause_on_hover",
        "pause_hotkey",
        "pause_hotkey_mode",
        "show_inline_images",
        "inline_image_max_height",
        "custom_themes_json",
        "active_custom_theme_id",
        "wallpaper_path",
        "wallpaper_blur",
        "emote_picker_mouse_leave",
        "custom_mod_buttons",
        "all_mod_buttons_v2",
        "macros",
        "chat_commands",
        "show_chat_header",
        "smooth_chat_enabled",
        "alternate_row_bg",
        "show_default_delete",
        "show_default_timeout",
        "show_default_ban",
        "disable_scroll_on_alt",
        "link_open_mode",
        "accent_color_index",
        "image_uploader_config"
    )

    private const val KEY_IMAGE_UPLOADER = "image_uploader_config"

    private val SECRET_FIELDS = setOf("extraHeaders")

    private fun redactForExport(key: String, element: JsonElement): JsonElement {
        if (key != KEY_IMAGE_UPLOADER) return element
        val text = (element as? JsonPrimitive)?.contentOrNull ?: return element
        if (SECRET_FIELDS.none { text.contains("\"$it\"") }) return element
        return try {
            val obj = json.parseToJsonElement(text).jsonObject
            val cleaned = buildMap {
                putAll(obj)
                SECRET_FIELDS.forEach { field ->
                    if (field in obj) put(field, JsonPrimitive(""))
                }
            }
            JsonPrimitive(JsonObject(cleaned).toString())
        } catch (e: Exception) {
            Napier.w("Settings export: failed to redact $key: ${e.message}", tag = "SettingsIO")
            JsonPrimitive("")
        }
    }

    fun snapshot(settings: Settings): SettingsBackup {
        val map = mutableMapOf<String, JsonElement>()
        for (key in PORTABLE_KEYS) {
            try {
                if (!settings.hasKey(key)) continue
                val raw: JsonElement = run {
                    val asString = settings.getStringOrNull(key)
                    if (asString != null) return@run JsonPrimitive(asString)
                    val asInt = settings.getIntOrNull(key)
                    if (asInt != null) return@run JsonPrimitive(asInt)
                    val asLong = settings.getLongOrNull(key)
                    if (asLong != null) return@run JsonPrimitive(asLong)
                    val asFloat = settings.getFloatOrNull(key)
                    if (asFloat != null) return@run JsonPrimitive(asFloat)
                    val asBool = settings.getBooleanOrNull(key)
                    if (asBool != null) return@run JsonPrimitive(asBool)
                    JsonPrimitive("")
                }
                map[key] = redactForExport(key, raw)
            } catch (e: Exception) {
                Napier.w("Settings export: failed to read key=$key: ${e.message}", tag = "SettingsIO")
            }
        }
        return SettingsBackup(
            version = 1,
            exportedAt = Clock.System.now().toEpochMilliseconds(),
            values = map
        )
    }

    fun applyReplace(settings: Settings, backup: SettingsBackup) {
        for (key in PORTABLE_KEYS) {
            try {
                if (settings.hasKey(key)) settings.remove(key)
            } catch (_: Exception) {}
        }
        for ((key, element) in backup.values) {
            if (key !in PORTABLE_KEYS) continue
            writeValue(settings, key, element)
        }
    }

    private fun writeValue(settings: Settings, key: String, element: JsonElement) {
        if (element !is JsonPrimitive) return
        try {
            when {
                element.isString -> settings.putString(key, element.contentOrNull ?: "")
                element.booleanOrNull != null -> settings.putBoolean(key, element.boolean)
                element.intOrNull != null -> settings.putInt(key, element.intOrNull!!)
                element.longOrNull != null -> settings.putLong(key, element.longOrNull!!)
                element.floatOrNull != null -> settings.putFloat(key, element.floatOrNull!!)
                element.doubleOrNull != null -> settings.putFloat(key, element.doubleOrNull!!.toFloat())
                else -> settings.putString(key, element.contentOrNull ?: "")
            }
        } catch (e: Exception) {
            Napier.w("Settings import: failed to write key=$key: ${e.message}", tag = "SettingsIO")
        }
    }

    fun toJson(backup: SettingsBackup): String =
        json.encodeToString(SettingsBackup.serializer(), backup)

    fun fromJson(text: String): SettingsBackup? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        if (looksLikeCsv(trimmed)) return fromCsv(trimmed)
        return runCatching {
            json.decodeFromString(SettingsBackup.serializer(), trimmed)
        }.getOrElse {
            runCatching {
                val obj = json.parseToJsonElement(trimmed).jsonObject
                SettingsBackup(values = obj.toMap())
            }.getOrNull()
        }
    }

    private fun looksLikeCsv(text: String): Boolean {
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return false
        if (firstLine.startsWith("{") || firstLine.startsWith("[")) return false
        return firstLine.contains("\t") || firstLine.contains(",")
    }

    fun toCsv(backup: SettingsBackup): String = buildString {
        appendLine("# CHATONE SETTINGS BACKUP v${backup.version} @ ${backup.exportedAt}")
        appendLine("key\tvalue\ttype")
        backup.values.entries.sortedBy { it.key }.forEach { (k, v) ->
            val (value, type) = primitiveToPair(v)
            appendLine("$k\t${escapeCsv(value)}\t$type")
        }
    }

    fun fromCsv(text: String): SettingsBackup? {
        val map = mutableMapOf<String, JsonElement>()
        val sep = if (text.lineSequence().firstOrNull { it.contains("\t") } != null) "\t" else ","
        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("key$sep")) continue
            val parts = line.split(sep, limit = 3)
            if (parts.size < 2) continue
            val key = parts[0].trim()
            val value = unescapeCsv(parts[1])
            val type = parts.getOrNull(2)?.trim().orEmpty()
            map[key] = pairToPrimitive(value, type)
        }
        if (map.isEmpty()) return null
        return SettingsBackup(values = map)
    }

    fun toXlsx(backup: SettingsBackup): String =
        buildSettingsXlsx(backup)

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

    private fun pairToPrimitive(value: String, type: String): JsonElement = when (type.lowercase()) {
        "boolean" -> JsonPrimitive(value.toBooleanStrictOrNull() ?: false)
        "int" -> JsonPrimitive(value.toIntOrNull() ?: 0)
        "long" -> JsonPrimitive(value.toLongOrNull() ?: 0L)
        "float", "double" -> JsonPrimitive(value.toFloatOrNull() ?: 0f)
        else -> JsonPrimitive(value)
    }

    private fun escapeCsv(s: String): String =
        s.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")

    private fun unescapeCsv(s: String): String =
        s.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\\\", "\\")

    private fun JsonObject.toMap(): Map<String, JsonElement> =
        this.entries.associate { (k, v) -> k to v }
}
