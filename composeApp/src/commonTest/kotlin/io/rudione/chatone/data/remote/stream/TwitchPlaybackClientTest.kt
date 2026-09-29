package io.rudione.chatone.data.remote.stream

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.rudione.chatone.domain.stream.StreamErrorKind
import io.rudione.chatone.domain.stream.StreamManifestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TwitchPlaybackClientTest {

    private val tokenResponse =
        """{"data":{"streamPlaybackAccessToken":{"value":"{\"authorization\":{\"forbidden\":false,\"reason\":\"\"},\"geoblock_reason\":\"\"}","signature":"abc123","authorization":{"isForbidden":false,"forbiddenReasonCode":"NONE"}}}}"""

    private val master = """
        #EXTM3U
        #EXT-X-TWITCH-INFO:SERVER-TIME="1000.5",USER-IP="198.51.100.1",SERVING-ID="serving1"
        #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="chunked",NAME="1080p60 (source)",AUTOSELECT=YES,DEFAULT=YES
        #EXT-X-STREAM-INF:BANDWIDTH=6000000,RESOLUTION=1920x1080,CODECS="avc1.64002A,mp4a.40.2",VIDEO="chunked",FRAME-RATE=60.000
        https://eun11.playlist.ttvnw.net/v1/playlist/source.m3u8
    """.trimIndent()

    private val requests = mutableListOf<HttpRequestData>()

    private fun client(usher: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): TwitchPlaybackClient {
        val engine = MockEngine { request ->
            requests += request
            when (request.url.host) {
                "gql.twitch.tv" -> respond(tokenResponse, HttpStatusCode.OK, jsonHeaders)
                else -> usher(request)
            }
        }
        val http = HttpClient(engine)
        return TwitchPlaybackClient(httpClient = { http }, random = Random(7), nowMs = { 1_000_000L })
    }

    @Test
    fun liveChannelProducesManifestWithoutTheViewerIp() = runTest {
        val result = fetchIn(client { respond(master, HttpStatusCode.OK) }, "SomeStreamer", "h264")
        val manifest = assertIs<StreamManifestResult.Ready>(result).manifest
        assertEquals("somestreamer", manifest.channelLogin)
        assertEquals(1, manifest.variants.size)
        assertEquals("serving1", manifest.serving.servingId)
        assertEquals(500L, manifest.clockOffsetMs)
        assertEquals(32, manifest.playSessionId.length)
        assertFalse(manifest.playlist.contains("198.51.100.1"))
        val usherRequest = requests.last()
        assertEquals("/api/channel/hls/somestreamer.m3u8", usherRequest.url.encodedPath)
        assertEquals("abc123", usherRequest.url.parameters["sig"])
        assertEquals("h264", usherRequest.url.parameters["supported_codecs"])
        assertTrue(requests.none { it.headers[HttpHeaders.Authorization] != null })
    }

    @Test
    fun notFoundMeansOffline() = runTest {
        val result = fetchIn(client { respond("""[{"error":"Can not find channel"}]""", HttpStatusCode.NotFound) }, "streamer", "h264")
        assertIs<StreamManifestResult.Offline>(result)
    }

    @Test
    fun forbiddenBodyIsMappedToAReason() = runTest {
        val geoblocked = fetchIn(client {
            respond("""[{"error":"blocked","error_code":"content_geoblocked"}]""", HttpStatusCode.Forbidden)
        }, "streamer", "h264")
        assertEquals(StreamErrorKind.GEOBLOCKED, assertIs<StreamManifestResult.Failed>(geoblocked).kind)

        val subsOnly = fetchIn(client {
            respond("""[{"error":"x","error_code":"unauthorized_entitlements"}]""", HttpStatusCode.Forbidden)
        }, "streamer", "h264")
        assertEquals(StreamErrorKind.SUBSCRIBERS_ONLY, assertIs<StreamManifestResult.Failed>(subsOnly).kind)
    }

    @Test
    fun invalidLoginNeverTouchesTheNetwork() = runTest {
        val playback = client { respond(master, HttpStatusCode.OK) }
        listOf("../admin", "a b", "", "x".repeat(40), "evil.m3u8?x=1").forEach { login ->
            val result = fetchIn(playback, login, "h264")
            assertEquals(StreamErrorKind.NOT_FOUND, assertIs<StreamManifestResult.Failed>(result).kind)
        }
        assertTrue(requests.isEmpty())
    }

    @Test
    fun variantsPointingOutsideHttpsAreRejected() = runTest {
        val tampered = master.replace("https://eun11.playlist.ttvnw.net/v1/playlist/source.m3u8", "http://127.0.0.1/steal.m3u8")
        val result = fetchIn(client { respond(tampered, HttpStatusCode.OK) }, "streamer", "h264")
        assertEquals(StreamErrorKind.UNKNOWN, assertIs<StreamManifestResult.Failed>(result).kind)
    }

    @Test
    fun unknownCodecsAreNotForwarded() = runTest {
        fetchIn(client { respond(master, HttpStatusCode.OK) }, "streamer", "h265,evil&x=1,h264")
        assertEquals("h265,h264", requests.last().url.parameters["supported_codecs"])
    }

    @Test
    fun missingPersistedQueryFallsBackToInlineQuery() = runTest {
        var gqlCalls = 0
        val engine = MockEngine { request ->
            requests += request
            if (request.url.host == "gql.twitch.tv") {
                gqlCalls++
                val body = (request.body as TextContent).text
                if (gqlCalls == 1) {
                    assertTrue(body.contains("persistedQuery"))
                    respond("""{"errors":[{"message":"PersistedQueryNotFound"}]}""", HttpStatusCode.OK, jsonHeaders)
                } else {
                    assertTrue(body.contains("streamPlaybackAccessToken"))
                    respond(tokenResponse, HttpStatusCode.OK, jsonHeaders)
                }
            } else {
                respond(master, HttpStatusCode.OK)
            }
        }
        val http = HttpClient(engine)
        val result = fetchIn(TwitchPlaybackClient(httpClient = { http }, nowMs = { 0L }), "streamer", "h264")
        assertIs<StreamManifestResult.Ready>(result)
        assertEquals(2, gqlCalls)
    }

    @Test
    fun forbiddenTokenStopsBeforeUsher() = runTest {
        val forbidden = """{"data":{"streamPlaybackAccessToken":{"value":"{\"authorization\":{\"forbidden\":true},\"geoblock_reason\":\"RU\"}","signature":"s","authorization":{"isForbidden":true}}}}"""
        val engine = MockEngine { request ->
            requests += request
            respond(forbidden, HttpStatusCode.OK, jsonHeaders)
        }
        val http = HttpClient(engine)
        val result = fetchIn(TwitchPlaybackClient(httpClient = { http }), "streamer", "h264")
        assertEquals(StreamErrorKind.GEOBLOCKED, assertIs<StreamManifestResult.Failed>(result).kind)
        assertNull(requests.firstOrNull { it.url.host == "usher.ttvnw.net" })
    }

    @Test
    fun manifestIsRequestedForTheEmbedPlayerThatGetsNoPrerolls() = runTest {
        fetchIn(client { respond(master, HttpStatusCode.OK) }, "streamer", "h264")
        val tokenRequests = requests.filter { it.url.host == "gql.twitch.tv" }
        assertEquals(1, tokenRequests.size)
        assertTrue((tokenRequests.single().body as TextContent).text.contains("\"playerType\":\"embed\""))
    }

    @Test
    fun rejectedEmbedPlaybackFallsBackToTheSitePlayer() = runTest {
        var usherCalls = 0
        val result = fetchIn(client {
            usherCalls++
            if (usherCalls == 1) {
                respond("""[{"error":"denied","error_code":"unknown"}]""", HttpStatusCode.Forbidden)
            } else {
                respond(master, HttpStatusCode.OK)
            }
        }, "streamer", "h264")
        assertIs<StreamManifestResult.Ready>(result)
        val tokenBodies = requests.filter { it.url.host == "gql.twitch.tv" }.map { (it.body as TextContent).text }
        assertTrue(tokenBodies.first().contains("\"playerType\":\"embed\""))
        assertTrue(tokenBodies.last().contains("\"playerType\":\"site\""))
    }

    @Test
    fun offlineChannelIsNotRetriedWithAnotherPlayer() = runTest {
        fetchIn(client { respond("[]", HttpStatusCode.NotFound) }, "streamer", "h264")
        assertEquals(1, requests.count { it.url.host == "usher.ttvnw.net" })
    }

    private suspend fun fetchIn(client: TwitchPlaybackClient, login: String, codecs: String): StreamManifestResult =
        withContext(Dispatchers.Default) { client.fetchManifest(login, codecs) }

    private companion object {
        val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    }
}
