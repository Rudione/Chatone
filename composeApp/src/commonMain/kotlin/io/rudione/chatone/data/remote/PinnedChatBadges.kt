package io.rudione.chatone.data.remote

import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.util.chat.IrcMessageParser
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

internal fun pinnedChatBadges(user: JsonObject?): List<Badge> {
    if (user == null) return emptyList()
    val source = user["badges"] ?: user["displayBadges"]
    val badges = when (source) {
        is JsonPrimitive -> IrcMessageParser.parseBadges(source.contentOrNull.orEmpty())
        is JsonArray -> source.mapNotNull { value ->
            if (value is JsonPrimitive) return@mapNotNull IrcMessageParser.parseBadges(value.contentOrNull.orEmpty()).firstOrNull()
            val badge = value as? JsonObject ?: return@mapNotNull null
            val id = badge.text("setID", "set_id", "id").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            Badge(
                id = id,
                version = badge.text("version", "versionID").ifBlank { "1" },
                imageUrl = badge.text("imageURL", "imageUrl", "image_url"),
                tooltip = badge.text("title", "description")
            )
        }
        else -> emptyList()
    }
    val roles = user["roles"] as? JsonObject
    return buildList {
        addAll(badges)
        if ((roles?.get("isBroadcaster") as? JsonPrimitive)?.booleanOrNull == true) add(Badge("broadcaster", "1", ""))
        if ((roles?.get("isModerator") as? JsonPrimitive)?.booleanOrNull == true) add(Badge("moderator", "1", ""))
    }.distinctBy { it.id }
}

private fun JsonObject.text(vararg keys: String): String = keys.firstNotNullOfOrNull { key ->
    (get(key) as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
}.orEmpty()
