package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val channelId: String,
    val channelName: String,
    val userId: String,
    val username: String,
    val displayName: String,
    val message: String,
    val timestamp: Long,
    val color: String? = null,
    val badges: List<Badge> = emptyList(),
    val emotes: List<Emote> = emptyList(),
    val isModerator: Boolean = false,
    val isSubscriber: Boolean = false,
    val isVip: Boolean = false,
    val isBroadcaster: Boolean = false,
    val isGrandMod: Boolean = false,
    val isMention: Boolean = false,
    val isAction: Boolean = false,
    val isFirstMessage: Boolean = false,
    val isHighlighted: Boolean = false,
    val customRewardId: String? = null,
    val rewardName: String? = null,
    val replyParentMsgId: String? = null,
    val replyParentUserLogin: String? = null,
    val replyParentDisplayName: String? = null,
    val replyParentMsgBody: String? = null,
    val bits: Int = 0,
    val gifs: List<ChatGif> = emptyList(),
)

@Serializable
data class Badge(
    val id: String,
    val version: String,
    val imageUrl: String,
    val months: Int? = null,
    val tooltip: String = "",
    val setId: String = "",
    val isGlobal: Boolean = false
)

fun List<Badge>.hasGrandModBadge(): Boolean = any { badge ->
    val id = badge.id.lowercase()
    when {
        id == "grand_moderator" -> true
        id == "chat_manager" -> true
        id == "broadcaster_mode" -> true
        id == "super_moderator" -> true
        id == "moderator" && (badge.version.toIntOrNull() ?: 1) >= 2 -> true
        else -> false
    }
}

@Serializable
data class Emote(
    val id: String,
    val name: String,
    val positions: List<EmotePosition>,
    val imageUrl: String
)

@Serializable
data class EmotePosition(
    val start: Int,
    val end: Int
)

@Serializable
data class Channel(
    val id: String,
    val login: String,
    val displayName: String,
    val profileImageUrl: String,
    val description: String = "",
    val viewerCount: Int = 0,
    val isLive: Boolean = false,
    val gameName: String = "",
    val title: String = ""
)

@Serializable
data class ChatGif(
    val id: String,
    val url: String,
    val title: String,
    val positions: List<EmotePosition>
)

enum class SubscriptionTier(val level: Int) {
    TIER_1(1), TIER_2(2), TIER_3(3);

    companion object {
        fun fromBadgeVersion(version: String): SubscriptionTier {
            val numeric = version.toIntOrNull() ?: return TIER_1
            return when (numeric / 1000) {
                3 -> TIER_3
                2 -> TIER_2
                else -> TIER_1
            }
        }
    }
}

fun List<Badge>.subscriptionTier(): SubscriptionTier? = firstOrNull {
    val id = it.id.lowercase()
    id == "subscriber" || id == "founder"
}?.let { SubscriptionTier.fromBadgeVersion(it.version) }

fun Badge.subscriptionTierOverlay(): Int? {
    val id = this.id.lowercase()
    if (id != "subscriber" && id != "founder") return null
    val tier = SubscriptionTier.fromBadgeVersion(version)
    return tier.level.takeIf { it > 1 }
}
