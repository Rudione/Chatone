package io.rudione.chatone.util.system

import kotlinx.coroutines.flow.StateFlow

expect object NotificationAccess {
    val granted: StateFlow<Boolean>

    fun refresh()

    fun request()

    fun openSystemSettings()
}
