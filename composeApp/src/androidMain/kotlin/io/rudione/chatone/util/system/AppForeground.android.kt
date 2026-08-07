package io.rudione.chatone.util.system

import kotlinx.atomicfu.atomic

private val foreground = atomic(false)

fun setAppForeground(value: Boolean) {
    foreground.value = value
}

actual fun isAppInForeground(): Boolean = foreground.value
