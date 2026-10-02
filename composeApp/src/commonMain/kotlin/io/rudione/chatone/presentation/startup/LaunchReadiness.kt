package io.rudione.chatone.presentation.startup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LaunchStage { Starting, Connecting, Emotes, History, Rendering, Ready }

class LaunchReadiness {

    private val _stage = MutableStateFlow(LaunchStage.Starting)
    val stage: StateFlow<LaunchStage> = _stage.asStateFlow()

    val isReady: Boolean
        get() = _stage.value == LaunchStage.Ready

    fun advance(next: LaunchStage) {
        _stage.update { current -> if (next > current) next else current }
    }

    fun finish() = advance(LaunchStage.Ready)
}
