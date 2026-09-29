package io.rudione.chatone.data.repository

import com.russhwolf.settings.PreferencesSettings
import io.rudione.chatone.domain.stream.LandscapeChatMode
import io.rudione.chatone.domain.stream.OverlayChatBounds
import io.rudione.chatone.domain.stream.StreamPlayerPreferences
import io.rudione.chatone.domain.stream.StreamVideoScale
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class StreamPlayerPreferencesRepositoryTest {

    private val node = Preferences.userRoot().node("chatone-test-stream-prefs-${System.nanoTime()}")
    private val settings = PreferencesSettings(node)
    private val repository = StreamPlayerPreferencesRepository(settings)

    @AfterTest
    fun removeNode() {
        node.removeNode()
    }

    @Test
    fun defaultsWhenNothingWasSaved() {
        assertEquals(StreamPlayerPreferences(), repository.load())
    }

    @Test
    fun restoresEverythingThatWasSaved() {
        val saved = StreamPlayerPreferences(
            preferredQuality = "480p30",
            lowLatency = false,
            landscapeChatMode = LandscapeChatMode.OVERLAY,
            videoScale = StreamVideoScale.FILL,
            sideChatFraction = 0.4f,
            overlayBounds = OverlayChatBounds(0.1f, 0.2f, 0.3f, 0.5f),
            overlayOpacity = 0.3f,
            autoPictureInPicture = false
        )
        repository.save(saved)
        assertEquals(saved, repository.load())
    }

    @Test
    fun corruptedValuesFallBackToSafeOnes() {
        settings.putString("stream_player_quality", "../../etc?x=1")
        settings.putString("stream_player_landscape_chat", "FLOATING")
        settings.putFloat("stream_player_side_chat_fraction", Float.NaN)
        settings.putFloat("stream_player_overlay_opacity", 7f)
        settings.putFloat("stream_player_overlay_x", 5f)
        settings.putFloat("stream_player_overlay_w", -1f)
        val loaded = repository.load()
        assertEquals(StreamPlayerPreferences.QUALITY_AUTO, loaded.preferredQuality)
        assertEquals(LandscapeChatMode.SIDE, loaded.landscapeChatMode)
        assertEquals(StreamPlayerPreferences.DEFAULT_SIDE_CHAT_FRACTION, loaded.sideChatFraction)
        assertEquals(StreamPlayerPreferences.MAX_OVERLAY_OPACITY, loaded.overlayOpacity)
        assertEquals(OverlayChatBounds.MIN_WIDTH, loaded.overlayBounds.width)
        assertEquals(1f - OverlayChatBounds.MIN_WIDTH, loaded.overlayBounds.x)
    }
}
