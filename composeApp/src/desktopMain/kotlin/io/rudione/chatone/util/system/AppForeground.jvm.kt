package io.rudione.chatone.util.system

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual val appForeground: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
