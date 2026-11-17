package io.rudione.chatone.presentation.media

import io.rudione.chatone.base.UiEvent

sealed interface MediaViewerEvent : UiEvent {
    data object Retry : MediaViewerEvent
    data class SelectSource(val index: Int) : MediaViewerEvent
    data object ToggleMute : MediaViewerEvent
    data class SetVolume(val volume: Float) : MediaViewerEvent
    data class SetSpeed(val speed: Float) : MediaViewerEvent
    data object ToggleLooping : MediaViewerEvent
    data class Progress(val positionSeconds: Int) : MediaViewerEvent
    data object DeleteClip : MediaViewerEvent
    data object BanCurator : MediaViewerEvent
}
