package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings
import io.rudione.chatone.domain.live.LiveAlertEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class LiveAlertsRepository(private val settings: Settings) {

    private val json = Json { ignoreUnknownKeys = true }
    private val ledgerSerializer = MapSerializer(String.serializer(), LiveAlertEntry.serializer())

    private val _channels = MutableStateFlow(loadChannels())
    val channels: StateFlow<Set<String>> = _channels.asStateFlow()

    fun toggle(channelLogin: String) {
        val login = channelLogin.trim().lowercase()
        if (login.isEmpty()) return
        _channels.update { current ->
            val next = if (login in current) current - login else current + login
            settings.putString(KEY_CHANNELS, next.joinToString(SEPARATOR))
            next
        }
    }

    fun ledger(): Map<String, LiveAlertEntry> =
        settings.getStringOrNull(KEY_LEDGER)
            ?.let { runCatching { json.decodeFromString(ledgerSerializer, it) }.getOrNull() }
            .orEmpty()

    fun saveLedger(entries: Map<String, LiveAlertEntry>) {
        settings.putString(KEY_LEDGER, json.encodeToString(ledgerSerializer, entries))
    }

    private fun loadChannels(): Set<String> =
        settings.getStringOrNull(KEY_CHANNELS)
            ?.split(SEPARATOR)
            ?.map { it.trim().lowercase() }
            ?.filter { it.isNotEmpty() }
            ?.toSet()
            .orEmpty()

    private companion object {
        const val KEY_CHANNELS = "live_notify_channels"
        const val KEY_LEDGER = "live_notify_ledger"
        const val SEPARATOR = ","
    }
}
