package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class HighlightRule(
    val id: String,
    val pattern: String,
    val isRegex: Boolean = false,
    val caseSensitive: Boolean = false,
    val playSound: Boolean = true,
    val showInMentions: Boolean = true,
    val color: Long = 0xFFFF6B6B,
    val enabled: Boolean = true
) {
    companion object {
        val USERNAME_RULE = HighlightRule(
            id = "username",
            pattern = "",
            playSound = true,
            showInMentions = true,
            color = 0xFFFF6B6B
        )

        val WHISPER_RULE = HighlightRule(
            id = "whispers",
            pattern = "",
            playSound = true,
            showInMentions = true,
            color = 0xFF9B59B6
        )

        val SUBSCRIPTION_RULE = HighlightRule(
            id = "subscriptions",
            pattern = "",
            playSound = false,
            showInMentions = true,
            color = 0xFF2ECC71
        )

        val FIRST_MESSAGE_RULE = HighlightRule(
            id = "first_message",
            pattern = "",
            playSound = false,
            showInMentions = false,
            color = 0xFFF39C12
        )

        val SEARCH_MATCH_RULE = HighlightRule(
            id = "search_match",
            pattern = "",
            playSound = false,
            showInMentions = false,
            color = 0xFF4FC3F7
        )

        val CHANNEL_POINTS_RULE = HighlightRule(
            id = "channel_points",
            pattern = "",
            playSound = false,
            showInMentions = false,
            color = 0xFF9146FF
        )

        val MENTION_ACCENT_RULE = HighlightRule(
            id = "mention_accent",
            pattern = "",
            playSound = false,
            showInMentions = false,
            color = 0xFFFF6B6B
        )
    }
}
