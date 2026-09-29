package io.rudione.chatone.util.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class NotificationTopic { GENERAL, LIVE, MENTION }

@ConsistentCopyVisibility
data class NotificationTarget private constructor(val channelLogin: String, val messageId: String?) {

    companion object {
        private val LOGIN_PATTERN = Regex("^[a-z0-9_]{1,25}$")
        private val MESSAGE_ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,64}$")

        fun of(channelLogin: String?, messageId: String? = null): NotificationTarget? {
            val login = channelLogin?.trim()?.lowercase()?.removePrefix("#") ?: return null
            if (!LOGIN_PATTERN.matches(login)) return null
            return NotificationTarget(login, messageId?.takeIf { MESSAGE_ID_PATTERN.matches(it) })
        }
    }
}

object NotificationLaunches {
    private val _pending = MutableStateFlow<NotificationTarget?>(null)
    val pending: StateFlow<NotificationTarget?> = _pending.asStateFlow()

    fun publish(target: NotificationTarget) {
        _pending.value = target
    }

    fun consume(target: NotificationTarget) {
        _pending.compareAndSet(target, null)
    }
}

expect fun showSystemNotification(
    title: String,
    body: String,
    topic: NotificationTopic,
    target: NotificationTarget?
)

expect fun dismissSystemNotifications(channelLogin: String)

fun notifySystem(
    title: String,
    body: String,
    topic: NotificationTopic = NotificationTopic.GENERAL,
    target: NotificationTarget? = null
) {
    val streamerMode =
        io.rudione.chatone.di.GlobalDi.tryGet<io.rudione.chatone.data.repository.StreamerModeController>()
    if (streamerMode != null && streamerMode.enabled && streamerMode.options.suppressNotifications) return
    showSystemNotification(title, body, topic, target)
}
