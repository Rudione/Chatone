package io.rudione.chatone.data.repository

import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecentChannelsRepositoryTest {

    private val node = Preferences.userRoot().node("chatone-test-recent-${System.nanoTime()}")
    private val settings = PreferencesSettings(node)
    private val repository = RecentChannelsRepository(settings)

    @AfterTest
    fun removeNode() {
        node.removeNode()
    }

    @Test
    fun latestOpenedChannelComesFirst() {
        repository.remember("alpha", "Alpha", "")
        repository.remember("beta", "Beta", "")
        repository.remember("alpha", "Alpha", "")

        assertEquals(listOf("alpha", "beta"), RecentChannelsRepository(settings).load().map { it.login })
    }

    @Test
    fun listIsCappedToTheMostRecentChannels() {
        (1..15).forEach { repository.remember("channel_$it", "Channel $it", "") }

        val logins = repository.load().map { it.login }

        assertEquals(RecentChannelsRepository.MAX_RECENT, logins.size)
        assertEquals("channel_15", logins.first())
        assertTrue("channel_1" !in logins)
    }

    @Test
    fun forgetRemovesTheChannel() {
        repository.remember("alpha", "Alpha", "")
        repository.remember("beta", "Beta", "")

        assertEquals(listOf("alpha"), repository.forget("#BETA").map { it.login })
    }

    @Test
    fun invalidLoginsAndInsecureAvatarsAreRejected() {
        repository.remember("../../etc", "Evil", "")
        repository.remember("Gamma", "Gamma", "http://insecure.example/a.png")

        val stored = repository.load().single()
        assertEquals("gamma", stored.login)
        assertEquals("", stored.avatarUrl)
    }

    @Test
    fun knownNameAndAvatarSurviveBareReopen() {
        repository.remember("delta", "Delta", "https://static-cdn.jtvnw.net/a.png")
        repository.remember("delta", "delta", "")

        val stored = repository.load().single()
        assertEquals("Delta", stored.displayName)
        assertEquals("https://static-cdn.jtvnw.net/a.png", stored.avatarUrl)
    }

    @Test
    fun corruptedStorageFallsBackToEmpty() {
        settings.putString("recent_channels_v1", "{not json")

        assertEquals(emptyList(), repository.load())
    }
}
