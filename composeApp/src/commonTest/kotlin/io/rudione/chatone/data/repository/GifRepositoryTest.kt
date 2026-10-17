package io.rudione.chatone.data.repository

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.rudione.chatone.data.remote.gif.GiphyApiClient
import io.rudione.chatone.domain.model.ChannelGifConfig
import io.rudione.chatone.domain.model.ChannelGifConfigSource
import io.rudione.chatone.domain.model.GifAccess
import io.rudione.chatone.domain.model.GifSearchConfig
import io.rudione.chatone.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class GifRepositoryTest {

    private var nowMs = 1_000_000L
    private val clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(nowMs)
    }
    private val requests = mutableListOf<Pair<String, String>>()
    private var nextConfig: Result<ChannelGifConfig> = Result.Error(IllegalStateException("unset"))

    private fun repository(personalKey: String = ""): GifRepository {
        val source = ChannelGifConfigSource { channelId, token ->
            requests += channelId to token
            nextConfig
        }
        return GifRepository(
            giphyApi = GiphyApiClient(HttpClient(MockEngine { error("Giphy must not be called") })),
            settings = InMemorySettings(),
            channelConfigs = source,
            clock = clock,
            personalKey = { personalKey }
        )
    }

    @Test
    fun channelKeyFromTwitchIsUsedFirst() = runTest {
        nextConfig = Result.Success(ChannelGifConfig(enabled = true, apiKey = "twitch-key", rating = "pg"))

        val access = repository(personalKey = "own-key").access("123", "token")

        assertEquals(GifAccess.Ready(GifSearchConfig("twitch-key", "pg"), fromChannel = true), access)
        assertEquals(listOf("123" to "token"), requests)
    }

    @Test
    fun disabledChannelIsReportedEvenWithAPersonalKey() = runTest {
        nextConfig = Result.Success(ChannelGifConfig(enabled = false, apiKey = "twitch-key", rating = "g"))

        assertEquals(GifAccess.Disabled, repository(personalKey = "own-key").access("123", "token"))
    }

    @Test
    fun failedTwitchConfigFallsBackToThePersonalKey() = runTest {
        nextConfig = Result.Error(IllegalStateException("integrity"))

        val access = repository(personalKey = "own-key").access("123", "token")

        assertEquals(
            GifAccess.Ready(GifSearchConfig("own-key", GifRepository.PERSONAL_KEY_RATING), fromChannel = false),
            access
        )
    }

    @Test
    fun withoutTokenOrKeyThePickerAsksForAKey() = runTest {
        assertEquals(GifAccess.NeedsKey, repository().access("123", ""))
        assertEquals(emptyList(), requests)
    }

    @Test
    fun channelConfigIsCachedUntilItExpires() = runTest {
        nextConfig = Result.Success(ChannelGifConfig(enabled = true, apiKey = "twitch-key", rating = "g"))
        val repository = repository()

        repeat(3) { repository.access("123", "token") }
        assertEquals(1, requests.size)

        nowMs += 11 * 60_000L
        repository.access("123", "token")
        assertEquals(2, requests.size)
    }

    @Test
    fun failuresAreRetriedOnTheNextOpen() = runTest {
        val repository = repository()
        nextConfig = Result.Error(IllegalStateException("offline"))
        assertEquals(GifAccess.NeedsKey, repository.access("123", "token"))

        nextConfig = Result.Success(ChannelGifConfig(enabled = true, apiKey = "twitch-key", rating = "g"))

        assertEquals(
            GifAccess.Ready(GifSearchConfig("twitch-key", "g"), fromChannel = true),
            repository.access("123", "token")
        )
        assertEquals(2, requests.size)
    }

    private class InMemorySettings : Settings {
        private val values = HashMap<String, Any>()
        override val keys: Set<String> get() = values.keys
        override val size: Int get() = values.size
        override fun clear() = values.clear()
        override fun remove(key: String) {
            values.remove(key)
        }
        override fun hasKey(key: String): Boolean = key in values
        override fun putInt(key: String, value: Int) = put(key, value)
        override fun getInt(key: String, defaultValue: Int): Int = values[key] as? Int ?: defaultValue
        override fun getIntOrNull(key: String): Int? = values[key] as? Int
        override fun putLong(key: String, value: Long) = put(key, value)
        override fun getLong(key: String, defaultValue: Long): Long = values[key] as? Long ?: defaultValue
        override fun getLongOrNull(key: String): Long? = values[key] as? Long
        override fun putString(key: String, value: String) = put(key, value)
        override fun getString(key: String, defaultValue: String): String = values[key] as? String ?: defaultValue
        override fun getStringOrNull(key: String): String? = values[key] as? String
        override fun putFloat(key: String, value: Float) = put(key, value)
        override fun getFloat(key: String, defaultValue: Float): Float = values[key] as? Float ?: defaultValue
        override fun getFloatOrNull(key: String): Float? = values[key] as? Float
        override fun putDouble(key: String, value: Double) = put(key, value)
        override fun getDouble(key: String, defaultValue: Double): Double = values[key] as? Double ?: defaultValue
        override fun getDoubleOrNull(key: String): Double? = values[key] as? Double
        override fun putBoolean(key: String, value: Boolean) = put(key, value)
        override fun getBoolean(key: String, defaultValue: Boolean): Boolean = values[key] as? Boolean ?: defaultValue
        override fun getBooleanOrNull(key: String): Boolean? = values[key] as? Boolean

        private fun put(key: String, value: Any) {
            values[key] = value
        }
    }
}
