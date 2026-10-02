package io.rudione.chatone.util.system

import kotlinx.coroutines.flow.StateFlow

expect val appForeground: StateFlow<Boolean>

fun isAppInForeground(): Boolean = appForeground.value
