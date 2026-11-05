package io.rudione.chatone.data.remote

import io.rudione.chatone.domain.model.Badge
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.time.Instant

data class AutoModProfile(
    val chatColor: String?,
    val badges: List<Badge>,
    val createdAtMs: Long?,
    val followedAtMs: Long?,
    val followKnown: Boolean,
    val subscriptionTier: String?
)

internal object AutoModProfileParser {

    const val QUERY =
        "query ChatoneAutoModProfile(\$login: String!, \$channelLogin: String!, \$channelID: ID!) { " +
            "user(login: \$login) { chatColor createdAt " +
            "displayBadges(channelLogin: \$channelLogin) { setID version imageURL title } " +
            "relationship(targetUserID: \$channelID) { followedAt subscriptionBenefit { tier } } } }"

    fun parse(user: JsonObject): AutoModProfile {
        val relationship = user["relationship"] as? JsonObject
        return AutoModProfile(
            chatColor = user.text("chatColor"),
            badges = (user["displayBadges"] as? JsonArray).orEmpty().mapNotNull { element ->
                val badge = element as? JsonObject ?: return@mapNotNull null
                val setId = badge.text("setID") ?: return@mapNotNull null
                Badge(
                    id = setId,
                    version = badge.text("version") ?: "1",
                    imageUrl = badge.text("imageURL")?.let(::sharperBadge).orEmpty(),
                    tooltip = badge.text("title").orEmpty(),
                    setId = setId
                )
            },
            createdAtMs = user.instant("createdAt"),
            followedAtMs = relationship?.instant("followedAt"),
            followKnown = relationship != null,
            subscriptionTier = (relationship?.get("subscriptionBenefit") as? JsonObject)?.text("tier")
        )
    }

    private fun sharperBadge(url: String): String = if (url.endsWith("/1")) url.dropLast(1) + "2" else url

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

    private fun JsonObject.instant(key: String): Long? =
        text(key)?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrNull() }
}
