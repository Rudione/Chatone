package io.rudione.chatone.util.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object NotificationAccess {
    actual val granted: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    actual fun refresh() = Unit

    actual fun request() = Unit

    actual fun openSystemSettings() = Unit
}
