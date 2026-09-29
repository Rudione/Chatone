package io.rudione.chatone.data.remote.stream

internal object TwitchAdBreakTestPlaylists {

    fun live(firstSequence: Long, firstSecond: Int, count: Int = 4, host: String = "edge-live"): String = buildString {
        appendLine("#EXTM3U")
        appendLine("#EXT-X-VERSION:3")
        appendLine("#EXT-X-TARGETDURATION:5")
        appendLine("#EXT-X-MEDIA-SEQUENCE:$firstSequence")
        repeat(count) { index ->
            appendLine("#EXT-X-PROGRAM-DATE-TIME:${timestamp(firstSecond + index * 2)}")
            appendLine("#EXTINF:2.000,live")
            appendLine("https://$host.hls.ttvnw.net/v1/segment/${firstSequence + index}.ts")
        }
    }

    fun withMidroll(firstSequence: Long, firstSecond: Int): String = buildString {
        appendLine("#EXTM3U")
        appendLine("#EXT-X-VERSION:3")
        appendLine("#EXT-X-TARGETDURATION:5")
        appendLine("#EXT-X-MEDIA-SEQUENCE:$firstSequence")
        appendLine("#EXT-X-PROGRAM-DATE-TIME:${timestamp(firstSecond)}")
        appendLine("#EXTINF:2.000,live")
        appendLine("https://edge-live.hls.ttvnw.net/v1/segment/$firstSequence.ts")
        appendLine(
            "#EXT-X-DATERANGE:ID=\"stitched-ad-1\",CLASS=\"twitch-stitched-ad\"," +
                "START-DATE=\"${timestamp(firstSecond + 2)}\",DURATION=30.235"
        )
        appendLine("#EXT-X-DISCONTINUITY")
        repeat(3) { index ->
            appendLine("#EXT-X-PROGRAM-DATE-TIME:${timestamp(firstSecond + 2 + index * 2)}")
            appendLine("#EXTINF:2.000,Amazon|2474283100494")
            appendLine("https://edge-ads.hls.ttvnw.net/v1/segment/ad-$index.ts")
        }
        appendLine("#EXT-X-TWITCH-PREFETCH:https://edge-ads.hls.ttvnw.net/v1/segment/ad-next.ts")
    }

    private fun timestamp(secondOffset: Int): String {
        val minute = 20 + secondOffset / 60
        val second = secondOffset % 60
        return "2026-09-19T09:${minute.pad()}:${second.pad()}.000Z"
    }

    private fun Int.pad(): String = toString().padStart(2, '0')
}
