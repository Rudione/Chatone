package io.rudione.chatone.data.remote

import io.rudione.chatone.domain.model.IrcEvent
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlin.time.Clock
import kotlin.time.Instant

internal object AutoModEventParser {

    const val HOLD = "automod.message.hold"
    const val UPDATE = "automod.message.update"
    const val VERSION = "2"
    const val REASON_BLOCKED_TERM = "blocked_term"
    const val REASON_BLOCKED_LINK = "blocked_link"

    fun hold(event: JsonObject): IrcEvent.AutoModHeld? {
        val msgId = event.text("message_id") ?: return null
        val channelId = event.text("broadcaster_user_id") ?: return null
        val login = event.text("user_login").orEmpty()
        val message = event["message"] as? JsonObject
        val text = message?.text("text")
            ?: (message?.get("fragments") as? JsonArray)?.joinToString("") { (it as? JsonObject)?.text("text").orEmpty() }
            ?: ""
        val automod = event["automod"] as? JsonObject
        val reason = event.text("reason")
        return IrcEvent.AutoModHeld(
            channel = channelId,
            msgId = msgId,
            userId = event.text("user_id").orEmpty(),
            username = login,
            displayName = event.text("user_name") ?: login,
            message = text,
            reasonCategory = automod?.text("category") ?: reason?.takeIf { it != "automod" },
            reasonLevel = (automod?.get("level") as? JsonPrimitive)?.intOrNull,
            flaggedFragments = flagged(text, automod, event["blocked_term"] as? JsonObject),
            emotes = AutoModMessageParts.twitchEmotes(AutoModMessageParts.eventSubFragments(message), text),
            channelLogin = event.text("broadcaster_user_login").orEmpty(),
            heldAtMs = event.text("held_at")?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
                ?: Clock.System.now().toEpochMilliseconds()
        )
    }

    fun update(event: JsonObject): IrcEvent.AutoModResolved? {
        val msgId = event.text("message_id") ?: return null
        val action = when (event.text("status")?.lowercase()) {
            "approved" -> IrcEvent.AutoModResolved.ALLOWED
            "denied" -> IrcEvent.AutoModResolved.DENIED
            "expired" -> IrcEvent.AutoModResolved.EXPIRED
            else -> return null
        }
        return IrcEvent.AutoModResolved(
            channel = event.text("broadcaster_user_id").orEmpty(),
            msgId = msgId,
            resolvedBy = event.text("moderator_user_login").orEmpty(),
            action = action,
            authorLogin = event.text("user_login"),
            channelLogin = event.text("broadcaster_user_login").orEmpty()
        )
    }

    private fun flagged(text: String, automod: JsonObject?, blockedTerm: JsonObject?): List<String> {
        val automodRanges = (automod?.get("boundaries") as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        val termRanges = (blockedTerm?.get("terms_found") as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonObject)?.get("boundary") as? JsonObject }
        return (automodRanges + termRanges).mapNotNull { range ->
            val start = (range["start_pos"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
            val end = (range["end_pos"] as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
            codePointSlice(text, start, end)?.trim()?.takeIf { it.isNotEmpty() }
        }.distinct()
    }

    internal fun codePointSlice(text: String, startCodePoint: Int, endCodePointInclusive: Int): String? {
        if (startCodePoint < 0 || endCodePointInclusive < startCodePoint) return null
        var codePoint = 0
        var index = 0
        var from = -1
        while (index < text.length) {
            if (codePoint == startCodePoint) from = index
            val width = if (text[index].isHighSurrogate() && index + 1 < text.length && text[index + 1].isLowSurrogate()) 2 else 1
            index += width
            if (codePoint == endCodePointInclusive) return if (from >= 0) text.substring(from, index) else null
            codePoint++
        }
        return if (from >= 0) text.substring(from) else null
    }

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
}
