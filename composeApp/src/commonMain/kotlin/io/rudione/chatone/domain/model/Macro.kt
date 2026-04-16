package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable


@Serializable
data class ModActionButton(
    val id: String,
    val durationSeconds: Int,
    val label: String = "",
    val enabled: Boolean = true
) {
    val displayLabel: String get() = label.ifEmpty { formatDuration(durationSeconds) }

    companion object {
        fun formatDuration(seconds: Int): String = when {
            seconds <= 0 -> ""
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m"
            seconds < 86400 -> "${seconds / 3600}h"
            else -> "${seconds / 86400}d"
        }

        val DEFAULT_DELETE  = ModActionButton("default_delete",  0)
        val DEFAULT_TIMEOUT = ModActionButton("default_timeout", 600)
        val DEFAULT_BAN     = ModActionButton("default_ban",    -1)
    }
}


@Serializable
data class Macro(
    val id: String,
    val name: String,
    val icon: String = "⚡",
    val steps: List<MacroStep> = emptyList(),
    val pinnedIndex: Int = -1
)

@Serializable
sealed class MacroStep {
    @Serializable data class SendMessage(val text: String)         : MacroStep()
    @Serializable data class SubMode(val enable: Boolean)         : MacroStep()
    @Serializable data class EmoteMode(val enable: Boolean)       : MacroStep()
    @Serializable data class SlowMode(val enable: Boolean, val seconds: Int = 30)      : MacroStep()
    @Serializable data class FollowerMode(val enable: Boolean, val minutes: Int = 10)  : MacroStep()
    @Serializable data class R9KMode(val enable: Boolean)         : MacroStep()
    @Serializable data class StartRaid(val targetLogin: String)   : MacroStep()
    @Serializable data class PinMessage(val message: String)      : MacroStep()
    @Serializable data class Delay(val seconds: Int)              : MacroStep()
    @Serializable data class ClearChat(val dummy: Boolean = false): MacroStep()
    @Serializable data class InsertText(val text: String)         : MacroStep()
}