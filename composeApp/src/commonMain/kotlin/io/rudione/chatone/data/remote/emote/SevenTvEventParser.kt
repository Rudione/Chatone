package io.rudione.chatone.data.remote.emote

import io.rudione.chatone.data.remote.dto.SevenTvChangeField
import io.rudione.chatone.data.remote.dto.SevenTvEmote
import io.rudione.chatone.data.remote.dto.SevenTvEventBody
import io.rudione.chatone.data.remote.dto.SevenTvEventData
import io.rudione.chatone.data.remote.dto.SevenTvEventEmoteValue
import io.rudione.chatone.data.remote.dto.SevenTvEventMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal sealed interface SevenTvFrame {
    data class Hello(val heartbeatIntervalMs: Long, val subscriptionLimit: Int) : SevenTvFrame
    data class Dispatch(val events: List<SevenTvLiveEvent>) : SevenTvFrame
    data object Heartbeat : SevenTvFrame
    data object Reconnect : SevenTvFrame
    data class EndOfStream(val code: Int?, val reason: String?) : SevenTvFrame
    data class Failure(val reason: String?) : SevenTvFrame
    data object Ignored : SevenTvFrame
}

internal class SevenTvEventParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) {
    fun parse(text: String): SevenTvFrame? {
        val message = try {
            json.decodeFromString<SevenTvEventMessage>(text)
        } catch (e: IllegalArgumentException) {
            return null
        }
        val data = message.d
        return when (message.op) {
            OP_DISPATCH -> SevenTvFrame.Dispatch(data?.let(::dispatchEvents).orEmpty())
            OP_HELLO -> SevenTvFrame.Hello(
                heartbeatIntervalMs = (data?.heartbeatInterval ?: DEFAULT_HEARTBEAT_MS).coerceIn(MIN_HEARTBEAT_MS, MAX_HEARTBEAT_MS),
                subscriptionLimit = data?.subscriptionLimit?.takeIf { it > 0 } ?: DEFAULT_SUBSCRIPTION_LIMIT
            )
            OP_HEARTBEAT -> SevenTvFrame.Heartbeat
            OP_RECONNECT -> SevenTvFrame.Reconnect
            OP_ERROR -> SevenTvFrame.Failure(data?.message)
            OP_END_OF_STREAM -> SevenTvFrame.EndOfStream(data?.code, data?.message)
            else -> SevenTvFrame.Ignored
        }
    }

    private fun dispatchEvents(data: SevenTvEventData): List<SevenTvLiveEvent> {
        val body = data.body ?: return emptyList()
        val actor = body.actor?.let { it.displayName.ifBlank { it.username } }.orEmpty()
        return when (data.type) {
            "emote_set.update" -> emoteSetEvents(body.id.ifBlank { data.condition["object_id"].orEmpty() }, body, actor)
            "user.update" -> listOfNotNull(userEvent(body, actor))
            "entitlement.create" -> entitlementEvents(body, granted = true)
            "entitlement.delete" -> entitlementEvents(body, granted = false)
            else -> emptyList()
        }
    }

    private fun emoteSetEvents(setId: String, body: SevenTvEventBody, actor: String): List<SevenTvLiveEvent> {
        if (setId.isBlank()) return emptyList()
        val removed = body.pulled.emoteFields().mapNotNull { field ->
            val old = field.oldValue.emoteValue() ?: return@mapNotNull null
            SevenTvLiveEvent.EmoteRemoved(setId, old.id, old.name, actor)
        }
        val renamed = body.updated.emoteFields().mapNotNull { field ->
            val old = field.oldValue.emoteValue() ?: return@mapNotNull null
            val new = field.value.emoteValue() ?: return@mapNotNull null
            if (old.name == new.name) return@mapNotNull null
            SevenTvLiveEvent.EmoteRenamed(setId, new.id, old.name, new.name, actor)
        }
        val added = body.pushed.emoteFields().mapNotNull { field ->
            val entry = field.value.emoteValue() ?: return@mapNotNull null
            val emote = SevenTvEmoteMapper.fromSetEntry(SevenTvEmote(entry.id, entry.name, entry.flags, entry.data))
                ?: SevenTvEmoteMapper.fromCdn(entry.id, entry.name)
            SevenTvLiveEvent.EmoteAdded(setId, emote, actor)
        }
        return removed + renamed + added
    }

    private fun userEvent(body: SevenTvEventBody, actor: String): SevenTvLiveEvent? {
        if (body.id.isBlank()) return null
        val setChanged = body.updated.any { field ->
            field.key == CONNECTIONS && (field.value as? JsonArray).orEmpty().any { nested ->
                ((nested as? JsonObject)?.get("key") as? JsonPrimitive)?.contentOrNull in EMOTE_SET_KEYS
            }
        }
        return if (setChanged) SevenTvLiveEvent.UserEmoteSetChanged(body.id, actor) else null
    }

    private fun entitlementEvents(body: SevenTvEventBody, granted: Boolean): List<SevenTvLiveEvent> {
        val obj = body.obj ?: return emptyList()
        if (!obj.kind.equals("EMOTE_SET", ignoreCase = true)) return emptyList()
        val emoteSetId = obj.refId.ifBlank { return emptyList() }
        return obj.user?.connections.orEmpty()
            .filter { it.platform.equals("TWITCH", ignoreCase = true) && it.id.isNotBlank() }
            .map { connection ->
                if (granted) SevenTvLiveEvent.PersonalEmoteSetGranted(connection.id, emoteSetId)
                else SevenTvLiveEvent.PersonalEmoteSetRevoked(connection.id, emoteSetId)
            }
    }

    private fun List<SevenTvChangeField>.emoteFields() = filter { it.key == EMOTES }

    private fun JsonElement?.emoteValue(): SevenTvEventEmoteValue? {
        val obj = this as? JsonObject ?: return null
        val value = try {
            json.decodeFromJsonElement(SevenTvEventEmoteValue.serializer(), obj)
        } catch (e: IllegalArgumentException) {
            return null
        }
        return value.takeIf { it.id.isNotBlank() && it.name.isNotBlank() }
    }

    private companion object {
        const val OP_DISPATCH = 0
        const val OP_HELLO = 1
        const val OP_HEARTBEAT = 2
        const val OP_RECONNECT = 4
        const val OP_ERROR = 6
        const val OP_END_OF_STREAM = 7
        const val DEFAULT_HEARTBEAT_MS = 30_000L
        const val MIN_HEARTBEAT_MS = 1_000L
        const val MAX_HEARTBEAT_MS = 300_000L
        const val DEFAULT_SUBSCRIPTION_LIMIT = 500
        const val EMOTES = "emotes"
        const val CONNECTIONS = "connections"
        val EMOTE_SET_KEYS = setOf("emote_set", "emote_set_id")
    }
}
