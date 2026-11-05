package io.rudione.chatone.data.remote

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Instant

enum class ModLogActionType { BAN, TIMEOUT, UNBAN, UNTIMEOUT, WARN }

data class GqlModLogAction(
    val id: String,
    val type: ModLogActionType,
    val timestampMs: Long,
    val label: String?,
    val moderatorLogin: String?
)

data class GqlModLogSummary(
    val counts: Map<ModLogActionType, Int>,
    val messageCount: Int?,
    val actions: List<GqlModLogAction>
)

internal object ModLogSummaryParser {

    val aliases: Map<String, ModLogActionType> = mapOf(
        "bans" to ModLogActionType.BAN,
        "timeouts" to ModLogActionType.TIMEOUT,
        "unbans" to ModLogActionType.UNBAN,
        "untimeouts" to ModLogActionType.UNTIMEOUT,
        "warnings" to ModLogActionType.WARN
    )

    fun parse(modLogs: JsonObject, targetId: String): GqlModLogSummary? {
        val counts = mutableMapOf<ModLogActionType, Int>()
        val actions = mutableListOf<GqlModLogAction>()
        var granted = false
        for ((alias, type) in aliases) {
            val result = modLogs[alias] as? JsonObject ?: continue
            val edges = result["edges"] as? JsonArray ?: continue
            granted = true
            counts[type] = result.int("count") ?: edges.size
            edges.forEach { edge -> parseAction(edge, type, targetId)?.let(actions::add) }
        }
        if (!granted) return null
        val messageCount = (modLogs["messages"] as? JsonObject)?.int("count")
        return GqlModLogSummary(counts, messageCount, actions.sortedByDescending { it.timestampMs })
    }

    private fun parseAction(edge: JsonElement, type: ModLogActionType, targetId: String): GqlModLogAction? {
        val node = (edge as? JsonObject)?.get("node") as? JsonObject ?: return null
        val id = node.string("id") ?: return null
        val timestamp = node.string("timestamp")?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
        val label = node["localizedLabel"] as? JsonObject
        return GqlModLogAction(
            id = id,
            type = type,
            timestampMs = timestamp.toEpochMilliseconds(),
            label = label?.string("fallbackString")?.let { detailOf(it, type) },
            moderatorLogin = label?.let { moderatorOf(it, targetId) }
        )
    }

    fun detailOf(fallback: String, type: ModLogActionType): String? {
        val text = fallback.trim()
        val bare = text.trimEnd('.').lowercase()
        return text.takeIf { it.isNotEmpty() && bare !in genericLabels.getValue(type) }
    }

    private val genericLabels: Map<ModLogActionType, Set<String>> = mapOf(
        ModLogActionType.BAN to setOf("banned", "ban"),
        ModLogActionType.TIMEOUT to setOf("timed out", "timeout"),
        ModLogActionType.UNBAN to setOf("unbanned", "unban"),
        ModLogActionType.UNTIMEOUT to setOf("untimed out", "timeout removed", "untimeout"),
        ModLogActionType.WARN to setOf("warned", "warning")
    )

    private fun moderatorOf(label: JsonObject, targetId: String): String? {
        val fragments = label["localizedStringFragments"] as? JsonArray ?: return null
        return fragments.asSequence()
            .mapNotNull { (it as? JsonObject)?.get("token") as? JsonObject }
            .filter { it.string("login") != null && it.string("id") != targetId }
            .firstNotNullOfOrNull { it.string("login") }
    }

    private fun JsonObject.string(key: String): String? = this[key]?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }

    private fun JsonObject.int(key: String): Int? = this[key]?.let { runCatching { it.jsonPrimitive.intOrNull }.getOrNull() }
}
