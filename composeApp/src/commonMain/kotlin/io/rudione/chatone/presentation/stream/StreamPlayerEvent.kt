package io.rudione.chatone.presentation.stream

import io.rudione.chatone.domain.stream.LandscapeChatMode
import io.rudione.chatone.domain.stream.OverlayChatBounds
import io.rudione.chatone.domain.stream.StreamMediaInfo
import io.rudione.chatone.domain.stream.StreamQualitySelection
import io.rudione.chatone.domain.stream.StreamVideoScale

sealed interface StreamPlayerEvent {
    data class Open(val channelLogin: String, val quality: String? = null) : StreamPlayerEvent
    data object Close : StreamPlayerEvent
    data object Dismiss : StreamPlayerEvent
    data class FollowChannel(val channelLogin: String?) : StreamPlayerEvent
    data object TogglePlayback : StreamPlayerEvent
    data object ToggleMute : StreamPlayerEvent
    data class SelectQuality(val selection: StreamQualitySelection) : StreamPlayerEvent
    data class SetPreferredQuality(val quality: String) : StreamPlayerEvent
    data class SetLowLatency(val enabled: Boolean) : StreamPlayerEvent
    data class SetVideoScale(val scale: StreamVideoScale) : StreamPlayerEvent
    data object ToggleVideoScale : StreamPlayerEvent
    data class SetLandscapeChatMode(val mode: LandscapeChatMode) : StreamPlayerEvent
    data object CycleLandscapeChatMode : StreamPlayerEvent
    data class SetSideChatFraction(val fraction: Float) : StreamPlayerEvent
    data class SetOverlayBounds(val bounds: OverlayChatBounds) : StreamPlayerEvent
    data class SetOverlayOpacity(val opacity: Float) : StreamPlayerEvent
    data class SetOverlayIdleOpacity(val opacity: Float) : StreamPlayerEvent
    data class SetOverlayMessageOpacity(val opacity: Float) : StreamPlayerEvent
    data class SetOverlayLocked(val locked: Boolean) : StreamPlayerEvent
    data class SetOverlayBackdrop(val enabled: Boolean) : StreamPlayerEvent
    data class SetOverlayBackdropOpacity(val opacity: Float) : StreamPlayerEvent
    data class SetOverlayScale(val scale: Float) : StreamPlayerEvent
    data class SetOverlayHideDelay(val seconds: Int) : StreamPlayerEvent
    data class SetOverlayAutoLock(val seconds: Int) : StreamPlayerEvent
    data class SetLatencyOnVideo(val visible: Boolean) : StreamPlayerEvent
    data class SetAmbientLight(val enabled: Boolean) : StreamPlayerEvent
    data class SetAutoPictureInPicture(val enabled: Boolean) : StreamPlayerEvent
    data class SetBackgroundAudio(val enabled: Boolean) : StreamPlayerEvent
    data class SetMediaInfo(val info: StreamMediaInfo) : StreamPlayerEvent
    data class SetStatsVisible(val visible: Boolean) : StreamPlayerEvent
    data class SetSettingsVisible(val visible: Boolean) : StreamPlayerEvent
    data object Retry : StreamPlayerEvent
    data object Reload : StreamPlayerEvent
    data object DismissChannelNotice : StreamPlayerEvent
    data object StreamWentLive : StreamPlayerEvent
    data class ViewportChanged(val width: Int, val height: Int) : StreamPlayerEvent
    data class AppVisibilityChanged(val visible: Boolean) : StreamPlayerEvent
    data object PictureInPictureDismissed : StreamPlayerEvent
}
