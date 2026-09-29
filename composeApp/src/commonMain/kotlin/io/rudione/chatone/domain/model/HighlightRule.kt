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
    val enabled: Boolean = true,
    val matchSubstring: Boolean = false
) {
    companion object {
        val USERNAME_RULE = HighlightRule(
            id = "username",
            pattern = "",
            playSound = true,
            showInMentions = true,
            color = 0xFFFF6B6B
        )

        const val LEGACY_WHISPER_COLOR = 0xFF9B59B6
        const val LEGACY_FIRST_MESSAGE_COLOR = 0xFFF39C12

        val WHISPER_RULE = HighlightRule(
            id = "whispers",
            pattern = "",
            playSound = true,
            showInMentions = true,
            color = LEGACY_FIRST_MESSAGE_COLOR
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
            color = LEGACY_WHISPER_COLOR
        )

        fun swapLegacyDefaultColors(rules: List<HighlightRule>): List<HighlightRule> {
            val firstMessage = rules.firstOrNull { it.id == "first_message" } ?: return rules
            val whisper = rules.firstOrNull { it.id == "whispers" } ?: return rules
            if (firstMessage.color != LEGACY_FIRST_MESSAGE_COLOR) return rules
            if (whisper.color != LEGACY_WHISPER_COLOR) return rules
            return rules.map {
                when (it.id) {
                    "first_message" -> it.copy(color = LEGACY_WHISPER_COLOR)
                    "whispers" -> it.copy(color = LEGACY_FIRST_MESSAGE_COLOR)
                    else -> it
                }
            }
        }

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
