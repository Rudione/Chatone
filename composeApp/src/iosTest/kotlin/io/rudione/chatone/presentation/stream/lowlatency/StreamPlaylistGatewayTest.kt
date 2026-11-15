package io.rudione.chatone.presentation.stream.lowlatency

import io.rudione.chatone.presentation.stream.MediaPlaylistSource
import io.rudione.chatone.presentation.stream.PlaylistFetch
import kotlin.concurrent.Volatile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StreamPlaylistGatewayTest {

    private class RecordingSource(private val replies: Map<String, PlaylistFetch>) : MediaPlaylistSource {
        @Volatile
        var requested: List<String> = emptyList()

        override suspend fun fetch(url: String): PlaylistFetch {
            requested = requested + url
            return replies[url] ?: PlaylistFetch(404, null)
        }
    }

    private val source = RecordingSource(
        mapOf(
            SOURCE_URL to PlaylistFetch(200, MEDIA_PLAYLIST),
            LOW_URL to PlaylistFetch(200, MEDIA_PLAYLIST)
        )
    )
    private val gateway = StreamPlaylistGateway(source = source, newToken = { TOKEN })
    private val opened = ArrayList<PlaylistSession>()

    @Volatile
    private var upstreamStatus = 0

    @AfterTest
    fun closeSessions() {
        opened.forEach { it.close() }
    }

    @Test
    fun servesMasterWithLocalVariantAddressesAndServerClock() {
        val session = open()
        val reply = get(session, "master")
        assertEquals(200, reply.status)
        assertEquals("application/vnd.apple.mpegurl", reply.headers["content-type"])
        assertEquals("close", reply.headers["connection"])
        val lines = reply.body.lines()
        assertEquals("#EXTM3U", lines[0])
        assertEquals("#EXT-X-TWITCH-INFO:SERVER-TIME=\"1794261898.71\"", lines[1])
        assertTrue(lines.contains("${base(session)}0.m3u8"), reply.body)
        assertTrue(lines.contains("${base(session)}1.m3u8"), reply.body)
        assertFalse(reply.body.contains("ttvnw.net"), "remote addresses must stay inside the app")
        assertFalse(reply.body.contains("USER-IP"), "the original server info line must be dropped")
    }

    @Test
    fun servesMediaPlaylistFromTheMappedVariantThroughTheTransform() {
        val session = open(transformSuffix = "#EXT-X-FILTERED")
        val reply = get(session, "1")
        assertEquals(200, reply.status)
        assertEquals(listOf(LOW_URL), source.requested)
        assertEquals(200, upstreamStatus)
        assertTrue(reply.body.contains("#EXT-X-FILTERED"))
        assertTrue(reply.body.contains("https://video.ttvnw.net/v1/segment/low/1.ts"), "relative segments must be made absolute")
        assertTrue(reply.body.contains("#EXT-X-TWITCH-PREFETCH:https://video.ttvnw.net/v1/segment/low/3.ts"))
    }

    @Test
    fun mirrorsUpstreamFailureSoThePlayerSeesTheStreamEnd() {
        val offline = StreamPlaylistGateway(source = RecordingSource(emptyMap()), newToken = { TOKEN })
        val session = assertNotNull(offline.open(spec()))
        opened += session
        val reply = LoopbackClient.get(portOf(session), "/$TOKEN/0.m3u8")
        assertEquals(404, reply?.status)
        assertEquals(404, upstreamStatus)
    }

    @Test
    fun rejectsRequestsThatDoNotComeFromTheLocalPlayer() {
        val session = open()
        val port = portOf(session)
        val host = "127.0.0.1:$port"
        val wrongToken = "/${"0".repeat(32)}/master.m3u8"
        val attacks = mapOf(
            "unknown session" to LoopbackClient.get(port, wrongToken),
            "foreign host header" to LoopbackClient.get(port, "/$TOKEN/master.m3u8", mapOf("Host" to "evil.example")),
            "dns rebinding host" to LoopbackClient.get(port, "/$TOKEN/master.m3u8", mapOf("Host" to "localhost:$port")),
            "browser origin" to LoopbackClient.get(port, "/$TOKEN/master.m3u8", mapOf("Host" to host, "Origin" to "https://evil.example")),
            "unknown variant" to LoopbackClient.get(port, "/$TOKEN/7.m3u8"),
            "remote address in path" to LoopbackClient.get(port, "/$TOKEN/https://evil.example/a.m3u8"),
            "path traversal" to LoopbackClient.get(port, "/$TOKEN/../$TOKEN/master.m3u8"),
            "query string" to LoopbackClient.get(port, "/$TOKEN/master.m3u8?u=https://evil.example"),
            "write method" to LoopbackClient.exchange(port, LoopbackClient.request("POST", "/$TOKEN/master.m3u8", mapOf("Host" to host))),
            "missing host" to LoopbackClient.get(port, "/$TOKEN/master.m3u8", emptyMap())
        )
        attacks.forEach { (name, reply) ->
            assertEquals(404, reply?.status, name)
            assertEquals("", reply?.body, "$name must not leak anything")
        }
        assertEquals(emptyList(), source.requested, "rejected requests must never reach the network")
    }

    @Test
    fun dropsOversizedAndMalformedRequests() {
        val session = open()
        val port = portOf(session)
        val oversized = LoopbackClient.request(
            "GET",
            "/$TOKEN/master.m3u8",
            mapOf("Host" to "127.0.0.1:$port", "X-Padding" to "a".repeat(LoopbackHttpServer.MAX_REQUEST_BYTES))
        )
        val oversizedReply = LoopbackClient.exchange(port, oversized)
        assertTrue(oversizedReply == null || oversizedReply.status == 400, "oversized request got ${oversizedReply?.status}")
        assertEquals(400, LoopbackClient.exchange(port, "garbage\r\n\r\n")?.status)
        assertEquals(200, get(session, "master").status, "the gateway keeps serving after bad requests")
    }

    @Test
    fun stopsListeningWhenTheLastSessionCloses() {
        var counter = 0
        val distinct = StreamPlaylistGateway(source = source, newToken = { (++counter).toString().padStart(32, '0') })
        val first = assertNotNull(distinct.open(spec()))
        val second = assertNotNull(distinct.open(spec()))
        val port = portOf(second)
        first.close()
        assertEquals(200, LoopbackClient.get(port, second.masterUrl.substringAfter(":$port"))?.status)
        second.close()
        assertTrue(LoopbackClient.awaitRefused(port), "nothing may listen without an active stream")
        assertFalse(second.isServing)
    }

    @Test
    fun closedSessionIsNoLongerServedWhileAnotherStaysOpen() {
        var counter = 0
        val distinct = StreamPlaylistGateway(source = source, newToken = { (++counter).toString().padStart(32, '0') })
        val first = assertNotNull(distinct.open(spec()))
        val second = assertNotNull(distinct.open(spec()))
        opened += second
        assertNotEquals(first.masterUrl, second.masterUrl)
        first.close()
        val port = portOf(second)
        assertEquals(404, LoopbackClient.get(port, first.masterUrl.substringAfter(":$port"))?.status)
        assertEquals(200, LoopbackClient.get(port, second.masterUrl.substringAfter(":$port"))?.status)
    }

    private fun open(transformSuffix: String? = null): PlaylistSession {
        val session = assertNotNull(gateway.open(spec(transformSuffix)))
        opened += session
        return session
    }

    private fun spec(transformSuffix: String? = null) = PlaylistSessionSpec(
        masterPlaylist = MASTER_PLAYLIST,
        serverTimeMs = { SERVER_TIME_MS },
        transform = { _, playlist -> if (transformSuffix == null) playlist else "$playlist\n$transformSuffix" },
        onMediaStatus = { upstreamStatus = it }
    )

    private fun get(session: PlaylistSession, resource: String): LoopbackReply =
        assertNotNull(LoopbackClient.get(portOf(session), "/$TOKEN/$resource.m3u8"))

    private fun base(session: PlaylistSession): String = session.masterUrl.removeSuffix("master.m3u8")

    private fun portOf(session: PlaylistSession): Int =
        session.masterUrl.removePrefix("http://127.0.0.1:").substringBefore('/').toInt()

    private companion object {
        const val TOKEN = "0123456789abcdef0123456789abcdef"
        const val SERVER_TIME_MS = 1_794_261_898_710L
        const val SOURCE_URL = "https://video.ttvnw.net/v1/playlist/source.m3u8"
        const val LOW_URL = "https://video.ttvnw.net/v1/playlist/low.m3u8"
        val MASTER_PLAYLIST = """
            #EXTM3U
            #EXT-X-TWITCH-INFO:NODE="edge",USER-IP="203.0.113.7",SERVER-TIME="1.00"
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="chunked",NAME="1080p60 (source)",AUTOSELECT=YES,DEFAULT=YES
            #EXT-X-STREAM-INF:BANDWIDTH=8000000,RESOLUTION=1920x1080,CODECS="avc1.64002A,mp4a.40.2",VIDEO="chunked",FRAME-RATE=60.000
            $SOURCE_URL
            #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="360p30",NAME="360p",AUTOSELECT=YES,DEFAULT=YES
            #EXT-X-STREAM-INF:BANDWIDTH=630000,RESOLUTION=640x360,CODECS="avc1.4D401E,mp4a.40.2",VIDEO="360p30",FRAME-RATE=30.000
            $LOW_URL
        """.trimIndent()
        val MEDIA_PLAYLIST = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-TARGETDURATION:6
            #EXT-X-MEDIA-SEQUENCE:1
            #EXT-X-PROGRAM-DATE-TIME:2026-11-09T21:44:58.000Z
            #EXTINF:2.000,live
            /v1/segment/low/1.ts
            #EXTINF:2.000,live
            https://video.ttvnw.net/v1/segment/low/2.ts
            #EXT-X-TWITCH-PREFETCH:/v1/segment/low/3.ts
        """.trimIndent()
    }
}
