package io.rudione.chatone.presentation.stream

import androidx.compose.runtime.Immutable
import io.rudione.chatone.domain.stream.StreamErrorKind
import io.rudione.chatone.domain.stream.StreamPhase
import io.rudione.chatone.domain.stream.StreamPlayerPreferences
import io.rudione.chatone.domain.stream.StreamQualitySelection
import io.rudione.chatone.domain.stream.StreamVariant

@Immutable
data class StreamPlayerUiState(
    val isSupported: Boolean = false,
    val channelLogin: String? = null,
    val phase: StreamPhase = StreamPhase.IDLE,
    val errorKind: StreamErrorKind? = null,
    val variants: List<StreamVariant> = emptyList(),
    val selection: StreamQualitySelection = StreamQualitySelection.Auto,
    val activeVariant: StreamVariant? = null,
    val isMuted: Boolean = false,
    val isAudioOnly: Boolean = false,
    val videoAspectRatio: Float = DEFAULT_ASPECT_RATIO,
    val streamStartedAtMs: Long? = null,
    val preferences: StreamPlayerPreferences = StreamPlayerPreferences(),
    val statsVisible: Boolean = false,
    val settingsVisible: Boolean = false,
    val chatChannelLogin: String? = null,
    val channelNoticeDismissed: Boolean = false
) {
    val showsOtherChannelNotice: Boolean get() = chatChannelLogin != null && !channelNoticeDismissed

    val isOpen: Boolean get() = channelLogin != null

    val isBusy: Boolean get() = phase == StreamPhase.LOADING || phase == StreamPhase.BUFFERING

    val isPlaybackActive: Boolean get() = phase == StreamPhase.PLAYING || phase == StreamPhase.BUFFERING

    val needsAttention: Boolean
        get() = phase == StreamPhase.OFFLINE || phase == StreamPhase.ERROR || phase == StreamPhase.ENDED

    val forcesControls: Boolean get() = phase == StreamPhase.PAUSED || needsAttention

    companion object {
        const val DEFAULT_ASPECT_RATIO = 16f / 9f
    }
}
