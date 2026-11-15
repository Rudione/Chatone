package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class StreamerModeController(private val settings: Settings) {

    data class Options(
        val hideModActions: Boolean = true,
        val hideTokens: Boolean = true,
        val hideThumbnails: Boolean = true,
        val suppressNotifications: Boolean = true,
        val hidePrivateMessages: Boolean = true
    )

    data class State(val enabled: Boolean, val options: Options) {
        val tokensHidden: Boolean get() = enabled && options.hideTokens
        val thumbnailsHidden: Boolean get() = enabled && options.hideThumbnails
        val modActionsHidden: Boolean get() = enabled && options.hideModActions
        val notificationsSuppressed: Boolean get() = enabled && options.suppressNotifications
        val privateMessagesHidden: Boolean get() = enabled && options.hidePrivateMessages
    }

    private val _state = MutableStateFlow(load())
    val state: StateFlow<State> = _state.asStateFlow()

    val enabled: Boolean get() = _state.value.enabled
    val options: Options get() = _state.value.options

    fun setEnabled(value: Boolean) {
        settings.putBoolean(KEY_ENABLED, value)
        _state.value = _state.value.copy(enabled = value)
    }

    fun setOptions(update: (Options) -> Options) {
        val next = update(_state.value.options)
        settings.putBoolean(KEY_HIDE_MOD, next.hideModActions)
        settings.putBoolean(KEY_HIDE_TOKENS, next.hideTokens)
        settings.putBoolean(KEY_HIDE_THUMBS, next.hideThumbnails)
        settings.putBoolean(KEY_SUPPRESS_NOTIF, next.suppressNotifications)
        settings.putBoolean(KEY_HIDE_PRIVATE_MESSAGES, next.hidePrivateMessages)
        _state.value = _state.value.copy(options = next)
    }

    fun maskToken(token: String): String =
        if (enabled && options.hideTokens && token.isNotEmpty()) "•".repeat(
            minOf(
                token.length,
                12
            )
        ) else token

    private fun load(): State = State(
        enabled = settings.getBoolean(KEY_ENABLED, false),
        options = Options(
            hideModActions = settings.getBoolean(KEY_HIDE_MOD, true),
            hideTokens = settings.getBoolean(KEY_HIDE_TOKENS, true),
            hideThumbnails = settings.getBoolean(KEY_HIDE_THUMBS, true),
            suppressNotifications = settings.getBoolean(KEY_SUPPRESS_NOTIF, true),
            hidePrivateMessages = settings.getBoolean(KEY_HIDE_PRIVATE_MESSAGES, true)
        )
    )

    companion object {
        private const val KEY_ENABLED = "streamer_mode_enabled"
        private const val KEY_HIDE_MOD = "streamer_mode_hide_mod"
        private const val KEY_HIDE_TOKENS = "streamer_mode_hide_tokens"
        private const val KEY_HIDE_THUMBS = "streamer_mode_hide_thumbs"
        private const val KEY_SUPPRESS_NOTIF = "streamer_mode_suppress_notif"
        private const val KEY_HIDE_PRIVATE_MESSAGES = "streamer_mode_hide_private_messages"
    }
}
