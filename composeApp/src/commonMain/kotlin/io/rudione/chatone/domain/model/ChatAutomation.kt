package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

/** What an automation does. */
@Serializable
enum class AutomationKind {
    /** Send [ChatAutomation.message] every [ChatAutomation.intervalMinutes] while the channel is open. */
    TIMED_MESSAGE,

    /** When an incoming message contains [ChatAutomation.keyword], reply with [ChatAutomation.message]. */
    AUTO_REPLY,

    /** When an incoming message contains [ChatAutomation.keyword], play the mention sound. */
    KEYWORD_SOUND
}

/**
 * User-defined chat automation ("Действия") — useful for regular viewers, not just mods.
 * All automations are per-channel ([channelLogin], empty = active channel only is NOT
 * supported deliberately: an automation always names its channel so nothing fires by surprise).
 */
@Serializable
data class ChatAutomation(
    val id: String,
    val enabled: Boolean = true,
    val kind: AutomationKind = AutomationKind.TIMED_MESSAGE,
    val channelLogin: String = "",
    /** Message to send (TIMED_MESSAGE) or reply text (AUTO_REPLY). */
    val message: String = "",
    /** Trigger keyword for AUTO_REPLY / KEYWORD_SOUND (case-insensitive substring). */
    val keyword: String = "",
    val intervalMinutes: Int = 15,
    /** Min seconds between two AUTO_REPLY/KEYWORD_SOUND firings of this automation. */
    val cooldownSeconds: Int = 60
)
