package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatHighlightColors(
    val mentionBg: Long = 0xFFFF6B6B,
    val mentionAccent: Long = 0xFFFF6B6B,
    val highlightBg: Long = 0xFFFFD700,
    val firstMessageColor: Long = 0xFF7B2FBE,
    val searchMatchBg: Long = 0xFF4FC3F7,
    val ownMessageBg: Long = 0x00000000,
)
