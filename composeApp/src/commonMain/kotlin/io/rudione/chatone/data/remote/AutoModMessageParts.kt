package io.rudione.chatone.data.remote

import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.Emote
import io.rudione.chatone.domain.model.EmotePosition
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

internal data class AutoModFragment(val text: String, val emoteId: String? = null)

internal object AutoModMessageParts {

    fun pubSubFragments(content: JsonObject?): List<AutoModFragment> =
        (content?.get("fragments") as? JsonArray).orEmpty().mapNotNull { element ->
            val fragment = element as? JsonObject ?: return@mapNotNull null
            val emoticon = fragment["emoticon"] as? JsonObject
            AutoModFragment(
                text = fragment.text("text") ?: return@mapNotNull null,
                emoteId = emoticon?.text("emoticonID") ?: emoticon?.text("emoticon_id")
            )
        }

    fun eventSubFragments(message: JsonObject?): List<AutoModFragment> =
        (message?.get("fragments") as? JsonArray).orEmpty().mapNotNull { element ->
            val fragment = element as? JsonObject ?: return@mapNotNull null
            val emote = fragment["emote"] as? JsonObject
            AutoModFragment(
                text = fragment.text("text") ?: return@mapNotNull null,
                emoteId = emote?.text("id")?.takeIf { fragment.text("type") == "emote" || fragment.text("type") == null }
            )
        }

    fun pubSubBadges(sender: JsonObject?): List<Badge> =
        (sender?.get("badges") as? JsonArray).orEmpty().mapNotNull { element ->
            val badge = element as? JsonObject ?: return@mapNotNull null
            val id = badge.text("id") ?: badge.text("set_id") ?: return@mapNotNull null
            Badge(id = id, version = badge.text("version") ?: "1", imageUrl = "")
        }

    fun twitchEmotes(fragments: List<AutoModFragment>, text: String): List<Emote> {
        var offset = 0
        val byId = LinkedHashMap<String, MutableList<EmotePosition>>()
        val names = HashMap<String, String>()
        for (fragment in fragments) {
            val start = text.indexOf(fragment.text, offset).takeIf { it >= 0 } ?: offset
            val end = start + fragment.text.length - 1
            val id = fragment.emoteId
            if (id != null && fragment.text.isNotEmpty() && end < text.length) {
                byId.getOrPut(id) { mutableListOf() } += EmotePosition(start, end)
                names[id] = fragment.text
            }
            offset = start + fragment.text.length
        }
        return byId.map { (id, positions) ->
            Emote(
                id = id,
                name = names[id].orEmpty(),
                positions = positions,
                imageUrl = "https://static-cdn.jtvnw.net/emoticons/v2/$id/default/dark/2.0"
            )
        }
    }

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
}
