package io.rudione.chatone.presentation.settings

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SettingsDestination {
    data class EditMacro(val macroId: String) : SettingsDestination
}

class SettingsNavigator {

    private val _pending = MutableStateFlow<SettingsDestination?>(null)
    val pending: StateFlow<SettingsDestination?> = _pending.asStateFlow()

    private val _openRequests = MutableSharedFlow<SettingsDestination>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val openRequests: SharedFlow<SettingsDestination> = _openRequests.asSharedFlow()

    fun open(destination: SettingsDestination) {
        _pending.value = destination
        _openRequests.tryEmit(destination)
    }

    fun consume(destination: SettingsDestination) {
        _pending.compareAndSet(destination, null)
    }
}
