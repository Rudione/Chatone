package io.rudione.chatone.data.remote.stream

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.rudione.chatone.data.remote.stream.TwitchAdBreakTestPlaylists.live
import io.rudione.chatone.data.remote.stream.TwitchAdBreakTestPlaylists.withMidroll
import io.rudione.chatone.domain.stream.StreamVariant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TwitchAdBreakResolverTest {

    private val tokenResponse =
        """{"data":{"streamPlaybackAccessToken":{"value":"{\"authorization\":{\"forbidden\":false}}","signature":"sig","authorization":{"isForbidden":false}}}}"""

    private val target = StreamVariant(
        "720p60", "720p60", "https://primary.playlist.ttvnw.net/v1/playlist/720.m3u8",
        3_400_000, 1280, 720, 60f, "avc1,mp4a.40.2"
    )

    private val tokenPlayerTypes = mutableListOf<String>()
    private val mediaRequests = mutableListOf<String>()

    private fun master(playerType: String, fullQuality: Boolean): String = buildString {
        appendLine("#EXTM3U")
        if (fullQuality) {
            appendLine("#EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID=\"720p60\",NAME=\"720p60\"")
            appendLine("#EXT-X-STREAM-INF:BANDWIDTH=3400000,RESOLUTION=1280x720,CODECS=\"avc1,mp4a.40.2\",VIDEO=\"720p60\",FRAME-RATE=60.000")
            appendLine("https://backup.playlist.ttvnw.net/v1/playlist/$playerType-720p60.m3u8")
        }
        appendLine("#EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID=\"360p30\",NAME=\"360p\"")
        appendLine("#EXT-X-STREAM-INF:BANDWIDTH=700000,RESOLUTION=640x360,CODECS=\"avc1,mp4a.40.2\",VIDEO=\"360p30\",FRAME-RATE=30.000")
        appendLine("https://backup.playlist.ttvnw.net/v1/playlist/$playerType-360p30.m3u8")
    }

    private fun resolver(mediaFor: (String) -> String): TwitchAdBreakResolver {
        val engine = MockEngine { request ->
            when (request.url.host) {
                "gql.twitch.tv" -> {
                    tokenPlayerTypes += playerTypeOf(request)
                    respond(tokenResponse, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
                "usher.ttvnw.net" -> {
                    val type = tokenPlayerTypes.last()
                    respond(master(type, fullQuality = type != TwitchPlayerType.AUTOPLAY), HttpStatusCode.OK)
                }
                else -> {
                    val name = request.url.encodedPath.substringAfterLast('/').removeSuffix(".m3u8")
                    mediaRequests += name
                    respond(mediaFor(name), HttpStatusCode.OK)
                }
            }
        }
        val http = HttpClient(engine)
        return TwitchAdBreakResolver(TwitchPlaybackClient(httpClient = { http }), nowMs = { 0L })
    }

    private fun playerTypeOf(request: HttpRequestData): String {
        val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
        return body["variables"]!!.jsonObject["playerType"]!!.jsonPrimitive.content
    }

    @Test
    fun skipsBackupsThatAlsoCarryAdsAndRemembersTheCleanOne() = runTest {
        val clean = live(6697, 0, host = "edge-backup")
        val resolver = resolver { name -> if (name.startsWith(TwitchPlayerType.POPOUT)) withMidroll(6697, 0) else clean }

        assertEquals(clean, withContext(Dispatchers.Default) { resolver.adFreeMediaPlaylist("Streamer", target, "h264") })
        assertEquals(listOf(TwitchPlayerType.POPOUT, TwitchPlayerType.EMBED), tokenPlayerTypes)
        assertEquals(listOf("popout-720p60", "embed-720p60"), mediaRequests)

        withContext(Dispatchers.Default) { resolver.adFreeMediaPlaylist("streamer", target, "h264") }
        assertEquals(2, tokenPlayerTypes.size)
        assertEquals("embed-720p60", mediaRequests.last())
    }

    @Test
    fun lowQualityBackupIsUsedWhenFullQualityOnesHaveAds() = runTest {
        val clean = live(6697, 0, host = "edge-backup")
        val resolver = resolver { name -> if (name.startsWith(TwitchPlayerType.AUTOPLAY)) clean else withMidroll(6697, 0) }

        assertEquals(clean, withContext(Dispatchers.Default) { resolver.adFreeMediaPlaylist("streamer", target, "h264") })
        assertEquals("autoplay-360p30", mediaRequests.last())
    }

    @Test
    fun givesUpWhenEveryBackupHasAds() = runTest {
        val resolver = resolver { withMidroll(6697, 0) }
        assertNull(withContext(Dispatchers.Default) { resolver.adFreeMediaPlaylist("streamer", target, "h264") })
        assertEquals(TwitchAdBreakResolver.BACKUP_PLAYER_TYPES, tokenPlayerTypes)
    }

    @Test
    fun variantMatchingFallsBackToTheClosestLowerQuality() {
        val candidates = listOf(
            StreamVariant("360p30", "360p", "https://x/360.m3u8", 700_000, 640, 360, 30f, "avc1"),
            StreamVariant("160p30", "160p", "https://x/160.m3u8", 230_000, 284, 160, 30f, "avc1"),
            StreamVariant("audio_only", "audio_only", "https://x/a.m3u8", 160_000, 0, 0, 0f, "mp4a.40.2")
        )
        assertEquals("360p30", TwitchAdBreakResolver.matchVariant(target, candidates)?.groupId)
        val audio = StreamVariant("audio_only", "audio_only", "https://y/a.m3u8", 160_000, 0, 0, 0f, "mp4a.40.2")
        assertEquals("audio_only", TwitchAdBreakResolver.matchVariant(audio, candidates)?.groupId)
    }
}
