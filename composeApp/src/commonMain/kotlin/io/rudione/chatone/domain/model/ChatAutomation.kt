package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AutomationKind {
    TIMED_MESSAGE,

    AUTO_REPLY,

    KEYWORD_SOUND
}

@Serializable
data class ChatAutomation(
    val id: String,
    val enabled: Boolean = true,
    val kind: AutomationKind = AutomationKind.TIMED_MESSAGE,
    val channelLogin: String = "",
    val message: String = "",
    val keyword: String = "",
    val intervalMinutes: Int = 15,
    val cooldownSeconds: Int = 60,
    val onlyWhenMentioned: Boolean = false
)
