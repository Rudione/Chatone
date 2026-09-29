package io.rudione.chatone.presentation.stream

import androidx.compose.runtime.Immutable
import io.rudione.chatone.domain.stream.LandscapeChatMode
import io.rudione.chatone.domain.stream.OverlayChatBounds
import io.rudione.chatone.domain.stream.StreamErrorKind
import io.rudione.chatone.domain.stream.StreamPhase
import io.rudione.chatone.domain.stream.StreamPlayerPreferences
import io.rudione.chatone.domain.stream.StreamQualitySelection
import io.rudione.chatone.domain.stream.StreamVariant
import io.rudione.chatone.domain.stream.StreamVideoScale

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

    companion object {
        const val DEFAULT_ASPECT_RATIO = 16f / 9f
    }
}

sealed interface StreamPlayerEvent {
    data class Open(val channelLogin: String, val quality: String? = null) : StreamPlayerEvent
    data object Close : StreamPlayerEvent
    data object Dismiss : StreamPlayerEvent
    data class FollowChannel(val channelLogin: String?) : StreamPlayerEvent
    data object TogglePlayback : StreamPlayerEvent
    data object ToggleMute : StreamPlayerEvent
    data class SelectQuality(val selection: StreamQualitySelection) : StreamPlayerEvent
    data class SetLowLatency(val enabled: Boolean) : StreamPlayerEvent
    data class SetVideoScale(val scale: StreamVideoScale) : StreamPlayerEvent
    data object ToggleVideoScale : StreamPlayerEvent
    data class SetLandscapeChatMode(val mode: LandscapeChatMode) : StreamPlayerEvent
    data object CycleLandscapeChatMode : StreamPlayerEvent
    data class SetSideChatFraction(val fraction: Float) : StreamPlayerEvent
    data class SetOverlayBounds(val bounds: OverlayChatBounds) : StreamPlayerEvent
    data class SetOverlayOpacity(val opacity: Float) : StreamPlayerEvent
    data class SetOverlayMessageOpacity(val opacity: Float) : StreamPlayerEvent
    data class SetOverlayLocked(val locked: Boolean) : StreamPlayerEvent
    data class SetAutoPictureInPicture(val enabled: Boolean) : StreamPlayerEvent
    data class SetStatsVisible(val visible: Boolean) : StreamPlayerEvent
    data class SetSettingsVisible(val visible: Boolean) : StreamPlayerEvent
    data object Retry : StreamPlayerEvent
    data object Reload : StreamPlayerEvent
    data object DismissChannelNotice : StreamPlayerEvent
    data object StreamWentLive : StreamPlayerEvent
    data class ViewportChanged(val width: Int, val height: Int) : StreamPlayerEvent
    data class AppVisibilityChanged(val visible: Boolean) : StreamPlayerEvent
}
