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
    val isHighlighted: Boolean = false
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