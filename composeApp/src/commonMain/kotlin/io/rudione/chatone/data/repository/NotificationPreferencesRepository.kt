package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationPreferencesRepository(private val settings: Settings) {

    private val _mentionAlerts = MutableStateFlow(settings.getBoolean(KEY_MENTION_ALERTS, false))
    val mentionAlerts: StateFlow<Boolean> = _mentionAlerts.asStateFlow()

    fun setMentionAlerts(enabled: Boolean) {
        settings.putBoolean(KEY_MENTION_ALERTS, enabled)
        _mentionAlerts.value = enabled
    }

    private companion object {
        const val KEY_MENTION_ALERTS = "mention_push_notifications"
    }
}
