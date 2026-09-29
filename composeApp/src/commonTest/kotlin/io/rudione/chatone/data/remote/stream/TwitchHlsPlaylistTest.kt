package io.rudione.chatone.data.remote.stream

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TwitchHlsPlaylistTest {

    private val master = """
        #EXTM3U
        #EXT-X-TWITCH-INFO:NODE="video-edge-1.abc.hls.ttvnw.net",SERVER-TIME="1789592391.68",USER-IP="203.0.113.7",SERVING-ID="29bb4a2fc20248ecaa5f60de5374e2cb",CLUSTER="cloudfront_prod_euw32",BROADCAST-ID="317774767587",TRANSCODESTACK="2025-Transcode-ELT-V1"
        #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="chunked",NAME="1080p60 (source)",AUTOSELECT=YES,DEFAULT=YES
        #EXT-X-STREAM-INF:BANDWIDTH=6797481,RESOLUTION=1920x1080,CODECS="avc1.64002A,mp4a.40.2",VIDEO="chunked",FRAME-RATE=60.000
        https://eun11.playlist.ttvnw.net/v1/playlist/source.m3u8
        #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="720p60",NAME="720p60",AUTOSELECT=YES,DEFAULT=YES
        #EXT-X-STREAM-INF:BANDWIDTH=3422999,RESOLUTION=1280x720,CODECS="avc1.4D401F,mp4a.40.2",VIDEO="720p60",FRAME-RATE=60.000
        https://eun11.playlist.ttvnw.net/v1/playlist/720.m3u8
        #EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID="audio_only",NAME="audio_only",AUTOSELECT=NO,DEFAULT=NO
        #EXT-X-STREAM-INF:BANDWIDTH=160000,CODECS="mp4a.40.2",VIDEO="audio_only"
        https://eun11.playlist.ttvnw.net/v1/playlist/audio.m3u8
    """.trimIndent()

    @Test
    fun parsesEveryVariantWithItsTwitchName() {
        val parsed = assertNotNull(TwitchHlsPlaylist.parseMaster(master))
        assertEquals(listOf("chunked", "720p60", "audio_only"), parsed.variants.map { it.groupId })
        val source = parsed.variants.first()
        assertEquals("1080p60 (source)", source.name)
        assertEquals("1080p60", source.displayName)
        assertTrue(source.isSource)
        assertEquals(1920, source.width)
        assertEquals(1080, source.height)
        assertEquals(60f, source.frameRate)
        assertEquals(6_797_481L, source.bandwidth)
        assertEquals("avc1.64002A,mp4a.40.2", source.codecs)
        assertTrue(parsed.variants.last().isAudioOnly)
        assertFalse(parsed.variants[1].isAudioOnly)
    }

    @Test
    fun readsServingInfoAndDropsTheLineWithTheViewerIp() {
        val parsed = assertNotNull(TwitchHlsPlaylist.parseMaster(master))
        assertEquals("29bb4a2fc20248ecaa5f60de5374e2cb", parsed.serving.servingId)
        assertEquals(1_789_592_391_680L, parsed.serving.serverTimeMs)
        assertFalse(parsed.sanitizedPlaylist.contains("203.0.113.7"))
        assertFalse(parsed.sanitizedPlaylist.contains("EXT-X-TWITCH-INFO"))
        assertTrue(parsed.sanitizedPlaylist.contains("#EXT-X-STREAM-INF"))
    }

    @Test
    fun quotedValuesKeepTheirCommas() {
        val attrs = TwitchHlsPlaylist.attributes("""CODECS="avc1.4D401F,mp4a.40.2",BANDWIDTH=1,NAME="a, b"""")
        assertEquals("avc1.4D401F,mp4a.40.2", attrs["CODECS"])
        assertEquals("1", attrs["BANDWIDTH"])
        assertEquals("a, b", attrs["NAME"])
    }

    @Test
    fun malformedAttributeListsDoNotLoop() {
        val attrs = TwitchHlsPlaylist.attributes(""",,,NOVALUE,KEY="unterminated""")
        assertEquals("unterminated", attrs["KEY"])
        assertNull(attrs["NOVALUE"])
    }

    @Test
    fun rejectsDocumentsThatAreNotPlaylists() {
        assertNull(TwitchHlsPlaylist.parseMaster("<html>nope</html>"))
        assertNull(TwitchHlsPlaylist.parseMaster("#EXTM3U\n#EXT-X-VERSION:3\n"))
        assertNull(TwitchHlsPlaylist.parseMaster("#EXTM3U\n" + "#".repeat(TwitchHlsPlaylist.MAX_PLAYLIST_CHARS)))
    }

    @Test
    fun serverInfoWithUnexpectedCharactersIsNotShown() {
        val parsed = assertNotNull(
            TwitchHlsPlaylist.parseMaster(
                master.replace("SERVING-ID=\"29bb4a2fc20248ecaa5f60de5374e2cb\"", "SERVING-ID=\"<script>\"")
            )
        )
        assertNull(parsed.serving.servingId)
    }

    @Test
    fun prefetchSegmentsBecomeRegularSegments() {
        val media = """
            #EXTM3U
            #EXT-X-TARGETDURATION:5
            #EXT-X-PROGRAM-DATE-TIME:2026-09-16T21:00:24.663Z
            #EXTINF:2.000,live
            https://edge.hls.ttvnw.net/v1/segment/1.ts
            #EXT-X-TWITCH-PREFETCH:https://edge.hls.ttvnw.net/v1/segment/2.ts
            #EXT-X-TWITCH-PREFETCH:https://edge.hls.ttvnw.net/v1/segment/3.ts
        """.trimIndent()
        val rewritten = assertNotNull(TwitchHlsPlaylist.rewriteMediaPlaylist(media, expandPrefetch = true))
        assertFalse(rewritten.contains("EXT-X-TWITCH-PREFETCH"))
        assertEquals(3, Regex("#EXTINF:2.000,").findAll(rewritten).count())
        assertTrue(rewritten.contains("#EXTINF:2.000,live\nhttps://edge.hls.ttvnw.net/v1/segment/3.ts"))
        assertTrue(rewritten.contains("#EXT-X-TARGETDURATION:2\n"))
    }

    @Test
    fun shortAdSegmentsDoNotShrinkPrefetchDuration() {
        val media = "#EXTM3U\n#EXTINF:2.000,ad\nhttps://a/1.ts\n#EXTINF:0.235,ad\nhttps://a/2.ts\n#EXT-X-TWITCH-PREFETCH:https://a/3.ts\n"
        val rewritten = assertNotNull(TwitchHlsPlaylist.rewriteMediaPlaylist(media, expandPrefetch = true))
        assertTrue(rewritten.contains("#EXTINF:2.000,live\nhttps://a/3.ts"))
    }

    @Test
    fun playlistsThatAreAlreadyTightAreLeftAlone() {
        assertNull(TwitchHlsPlaylist.rewriteMediaPlaylist("#EXTM3U\n#EXT-X-TARGETDURATION:2\n#EXTINF:2.000,\nhttps://a/1.ts\n", expandPrefetch = true))
    }

    @Test
    fun oversizedTargetDurationIsTightenedWithoutTouchingPrefetchInNormalMode() {
        val media = "#EXTM3U\n#EXT-X-TARGETDURATION:6\n#EXTINF:2.000,live\nhttps://a/1.ts\n#EXT-X-TWITCH-PREFETCH:https://a/2.ts\n"
        val rewritten = assertNotNull(TwitchHlsPlaylist.rewriteMediaPlaylist(media, expandPrefetch = false))
        assertTrue(rewritten.contains("#EXT-X-TARGETDURATION:2\n"))
        assertTrue(rewritten.contains("#EXT-X-TWITCH-PREFETCH:https://a/2.ts"))
    }

    @Test
    fun targetDurationNeverDropsBelowTheLongestSegment() {
        val media = "#EXTM3U\n#EXT-X-TARGETDURATION:6\n#EXTINF:2.000,a\nhttps://a/1.ts\n#EXTINF:4.600,b\nhttps://a/2.ts\n"
        val rewritten = assertNotNull(TwitchHlsPlaylist.rewriteMediaPlaylist(media, expandPrefetch = true))
        assertTrue(rewritten.contains("#EXT-X-TARGETDURATION:5\n"))
    }
}
