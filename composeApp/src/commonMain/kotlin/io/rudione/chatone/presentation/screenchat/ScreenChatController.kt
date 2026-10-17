package io.rudione.chatone.presentation.screenchat

import com.russhwolf.settings.Settings
import io.rudione.chatone.util.system.isDesktopPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ScreenChatController(
    private val settings: Settings,
    val isSupported: Boolean = isDesktopPlatform
) {
    private val _open = MutableStateFlow(if (isSupported) load() else emptyList())
    val open: StateFlow<List<String>> = _open.asStateFlow()

    fun isOpen(channelLogin: String): Boolean = normalize(channelLogin) in _open.value

    fun toggle(channelLogin: String) {
        if (isOpen(channelLogin)) close(channelLogin) else open(channelLogin)
    }

    fun open(channelLogin: String) {
        val key = normalize(channelLogin) ?: return
        if (!isSupported) return
        change { if (key in it) it else (it + key).takeLast(MAX_WINDOWS) }
    }

    fun close(channelLogin: String) {
        val key = normalize(channelLogin) ?: return
        change { it - key }
    }

    fun closeAll(channelLogins: Collection<String>) {
        val keys = channelLogins.mapNotNull(::normalize).toSet()
        if (keys.isEmpty()) return
        change { current -> current.filterNot { it in keys } }
    }

    private fun change(transform: (List<String>) -> List<String>) {
        _open.update(transform)
        settings.putString(KEY_OPEN, _open.value.joinToString(SEPARATOR))
    }

    private fun load(): List<String> = settings.getStringOrNull(KEY_OPEN)
        ?.split(SEPARATOR)
        ?.mapNotNull(::normalize)
        ?.distinct()
        ?.takeLast(MAX_WINDOWS)
        .orEmpty()

    private fun normalize(channelLogin: String): String? =
        channelLogin.trim().removePrefix("#").lowercase().takeIf { LOGIN.matches(it) }

    private companion object {
        const val KEY_OPEN = "screen_chat_open"
        const val SEPARATOR = ","
        const val MAX_WINDOWS = 6
        val LOGIN = Regex("^[a-z0-9_]{1,25}$")
    }
}
