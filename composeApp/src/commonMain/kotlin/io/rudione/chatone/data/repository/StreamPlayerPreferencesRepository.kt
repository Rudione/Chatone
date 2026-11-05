package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings
import io.rudione.chatone.domain.stream.LandscapeChatMode
import io.rudione.chatone.domain.stream.OverlayChatBounds
import io.rudione.chatone.domain.stream.StreamPlayerPreferences
import io.rudione.chatone.domain.stream.StreamVideoScale

class StreamPlayerPreferencesRepository(private val settings: Settings) {

    fun load(): StreamPlayerPreferences {
        val defaults = StreamPlayerPreferences()
        return StreamPlayerPreferences(
            preferredQuality = settings.getString(KEY_QUALITY, defaults.preferredQuality)
                .takeIf { it.length in 1..MAX_QUALITY_LENGTH && it.all(::isQualityChar) }
                ?: defaults.preferredQuality,
            lowLatency = settings.getBoolean(KEY_LOW_LATENCY, defaults.lowLatency),
            landscapeChatMode = enumOrDefault(
                settings.getString(KEY_CHAT_MODE, ""),
                defaults.landscapeChatMode
            ),
            videoScale = enumOrDefault(
                settings.getString(KEY_VIDEO_SCALE, ""),
                defaults.videoScale
            ),
            sideChatFraction = settings.getFloat(KEY_SIDE_FRACTION, defaults.sideChatFraction)
                .clampSideFraction(),
            overlayBounds = OverlayChatBounds(
                x = settings.getFloat(KEY_OVERLAY_X, OverlayChatBounds.DEFAULT.x),
                y = settings.getFloat(KEY_OVERLAY_Y, OverlayChatBounds.DEFAULT.y),
                width = settings.getFloat(KEY_OVERLAY_W, OverlayChatBounds.DEFAULT.width),
                height = settings.getFloat(KEY_OVERLAY_H, OverlayChatBounds.DEFAULT.height)
            ).sanitized(),
            overlayOpacity = settings.getFloat(KEY_OVERLAY_OPACITY, defaults.overlayOpacity)
                .clampOpacity(),
            overlayIdleOpacity = settings.getFloat(KEY_OVERLAY_IDLE_OPACITY, defaults.overlayIdleOpacity)
                .clampIdleOpacity(),
            overlayMessageOpacity = settings.getFloat(KEY_OVERLAY_MESSAGE_OPACITY, defaults.overlayMessageOpacity)
                .clampMessageOpacity(),
            overlayLocked = settings.getBoolean(KEY_OVERLAY_LOCKED, defaults.overlayLocked),
            overlayBackdrop = settings.getBoolean(KEY_OVERLAY_BACKDROP, defaults.overlayBackdrop),
            overlayBackdropOpacity = settings.getFloat(KEY_OVERLAY_BACKDROP_OPACITY, defaults.overlayBackdropOpacity)
                .clampIn(
                    StreamPlayerPreferences.MIN_OVERLAY_BACKDROP_OPACITY,
                    StreamPlayerPreferences.MAX_OVERLAY_BACKDROP_OPACITY,
                    StreamPlayerPreferences.DEFAULT_OVERLAY_BACKDROP_OPACITY
                ),
            overlayScale = settings.getFloat(KEY_OVERLAY_SCALE, defaults.overlayScale)
                .clampIn(
                    StreamPlayerPreferences.MIN_OVERLAY_SCALE,
                    StreamPlayerPreferences.MAX_OVERLAY_SCALE,
                    StreamPlayerPreferences.DEFAULT_OVERLAY_SCALE
                ),
            overlayHideDelaySeconds = settings.getInt(KEY_OVERLAY_HIDE_DELAY, defaults.overlayHideDelaySeconds)
                .coerceIn(
                    StreamPlayerPreferences.MIN_OVERLAY_HIDE_DELAY_SECONDS,
                    StreamPlayerPreferences.MAX_OVERLAY_HIDE_DELAY_SECONDS
                ),
            overlayAutoLockSeconds = settings.getInt(KEY_OVERLAY_AUTO_LOCK, defaults.overlayAutoLockSeconds)
                .coerceIn(
                    StreamPlayerPreferences.MIN_OVERLAY_AUTO_LOCK_SECONDS,
                    StreamPlayerPreferences.MAX_OVERLAY_AUTO_LOCK_SECONDS
                ),
            latencyOnVideo = settings.getBoolean(KEY_LATENCY_ON_VIDEO, defaults.latencyOnVideo),
            ambientLight = settings.getBoolean(KEY_AMBIENT_LIGHT, defaults.ambientLight),
            autoPictureInPicture = settings.getBoolean(KEY_AUTO_PIP, defaults.autoPictureInPicture),
            backgroundAudio = settings.getBoolean(KEY_BACKGROUND_AUDIO, defaults.backgroundAudio),
            autoOpen = settings.getBoolean(KEY_AUTO_OPEN, defaults.autoOpen)
        )
    }

    fun save(preferences: StreamPlayerPreferences) {
        settings.putString(KEY_QUALITY, preferences.preferredQuality)
        settings.putBoolean(KEY_LOW_LATENCY, preferences.lowLatency)
        settings.putString(KEY_CHAT_MODE, preferences.landscapeChatMode.name)
        settings.putString(KEY_VIDEO_SCALE, preferences.videoScale.name)
        settings.putFloat(KEY_SIDE_FRACTION, preferences.sideChatFraction.clampSideFraction())
        val bounds = preferences.overlayBounds.sanitized()
        settings.putFloat(KEY_OVERLAY_X, bounds.x)
        settings.putFloat(KEY_OVERLAY_Y, bounds.y)
        settings.putFloat(KEY_OVERLAY_W, bounds.width)
        settings.putFloat(KEY_OVERLAY_H, bounds.height)
        settings.putFloat(KEY_OVERLAY_OPACITY, preferences.overlayOpacity.clampOpacity())
        settings.putFloat(KEY_OVERLAY_IDLE_OPACITY, preferences.overlayIdleOpacity.clampIdleOpacity())
        settings.putFloat(KEY_OVERLAY_MESSAGE_OPACITY, preferences.overlayMessageOpacity.clampMessageOpacity())
        settings.putBoolean(KEY_OVERLAY_LOCKED, preferences.overlayLocked)
        settings.putBoolean(KEY_OVERLAY_BACKDROP, preferences.overlayBackdrop)
        settings.putFloat(KEY_OVERLAY_BACKDROP_OPACITY, preferences.overlayBackdropOpacity)
        settings.putFloat(KEY_OVERLAY_SCALE, preferences.overlayScale)
        settings.putInt(KEY_OVERLAY_HIDE_DELAY, preferences.overlayHideDelaySeconds)
        settings.putInt(KEY_OVERLAY_AUTO_LOCK, preferences.overlayAutoLockSeconds)
        settings.putBoolean(KEY_LATENCY_ON_VIDEO, preferences.latencyOnVideo)
        settings.putBoolean(KEY_AMBIENT_LIGHT, preferences.ambientLight)
        settings.putBoolean(KEY_AUTO_PIP, preferences.autoPictureInPicture)
        settings.putBoolean(KEY_BACKGROUND_AUDIO, preferences.backgroundAudio)
        settings.putBoolean(KEY_AUTO_OPEN, preferences.autoOpen)
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == raw } ?: default

    private fun Float.clampSideFraction(): Float =
        if (isFinite()) coerceIn(
            StreamPlayerPreferences.MIN_SIDE_CHAT_FRACTION,
            StreamPlayerPreferences.MAX_SIDE_CHAT_FRACTION
        )
        else StreamPlayerPreferences.DEFAULT_SIDE_CHAT_FRACTION

    private fun Float.clampOpacity(): Float =
        if (isFinite()) coerceIn(
            StreamPlayerPreferences.MIN_OVERLAY_OPACITY,
            StreamPlayerPreferences.MAX_OVERLAY_OPACITY
        )
        else StreamPlayerPreferences.DEFAULT_OVERLAY_OPACITY

    private fun Float.clampIdleOpacity(): Float =
        clampIn(0f, StreamPlayerPreferences.MAX_OVERLAY_IDLE_OPACITY, StreamPlayerPreferences.DEFAULT_OVERLAY_IDLE_OPACITY)

    private fun Float.clampMessageOpacity(): Float =
        if (isFinite()) coerceIn(
            StreamPlayerPreferences.MIN_OVERLAY_MESSAGE_OPACITY,
            StreamPlayerPreferences.MAX_OVERLAY_MESSAGE_OPACITY
        )
        else StreamPlayerPreferences.DEFAULT_OVERLAY_MESSAGE_OPACITY

    private fun Float.clampIn(min: Float, max: Float, fallback: Float): Float =
        if (isFinite()) coerceIn(min, max) else fallback

    private fun isQualityChar(c: Char): Boolean =
        c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '_' || c == '.'

    private companion object {
        const val KEY_QUALITY = "stream_player_quality"
        const val KEY_LOW_LATENCY = "stream_player_low_latency"
        const val KEY_CHAT_MODE = "stream_player_landscape_chat"
        const val KEY_VIDEO_SCALE = "stream_player_video_scale"
        const val KEY_SIDE_FRACTION = "stream_player_side_chat_fraction"
        const val KEY_OVERLAY_X = "stream_player_overlay_x"
        const val KEY_OVERLAY_Y = "stream_player_overlay_y"
        const val KEY_OVERLAY_W = "stream_player_overlay_w"
        const val KEY_OVERLAY_H = "stream_player_overlay_h"
        const val KEY_OVERLAY_OPACITY = "stream_player_overlay_opacity"
        const val KEY_OVERLAY_IDLE_OPACITY = "stream_player_overlay_idle_opacity"
        const val KEY_OVERLAY_MESSAGE_OPACITY = "stream_player_overlay_message_opacity"
        const val KEY_OVERLAY_LOCKED = "stream_player_overlay_locked"
        const val KEY_OVERLAY_BACKDROP = "stream_player_overlay_backdrop"
        const val KEY_OVERLAY_BACKDROP_OPACITY = "stream_player_overlay_backdrop_opacity"
        const val KEY_OVERLAY_SCALE = "stream_player_overlay_scale"
        const val KEY_OVERLAY_HIDE_DELAY = "stream_player_overlay_hide_delay"
        const val KEY_OVERLAY_AUTO_LOCK = "stream_player_overlay_auto_lock"
        const val KEY_LATENCY_ON_VIDEO = "stream_player_latency_badge"
        const val KEY_AMBIENT_LIGHT = "stream_player_ambient_light"
        const val KEY_AUTO_PIP = "stream_player_auto_pip"
        const val KEY_BACKGROUND_AUDIO = "stream_player_background_audio"
        const val KEY_AUTO_OPEN = "stream_player_auto_open"
        const val MAX_QUALITY_LENGTH = 32
    }
}
