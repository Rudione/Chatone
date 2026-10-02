package io.rudione.chatone.util.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private val foreground = MutableStateFlow(false)

actual val appForeground: StateFlow<Boolean> = foreground.asStateFlow()

fun setAppForeground(value: Boolean) {
    foreground.value = value
}
