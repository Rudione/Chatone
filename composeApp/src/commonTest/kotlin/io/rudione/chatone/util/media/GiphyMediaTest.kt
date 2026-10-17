package io.rudione.chatone.util.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GiphyMediaTest {

    @Test
    fun giphyOriginalsAreSwappedForTheLightRendition() {
        val url = GiphyMedia.chatUrl(
            "3o7TKSjRrfIPjeiVyM",
            "https://media4.giphy.com/media/v1.Y2lk/3o7TKSjRrfIPjeiVyM/giphy.gif?cid=abc&rid=giphy.gif"
        )

        val expected = "https://media.giphy.com/media/3o7TKSjRrfIPjeiVyM/giphy."
        assertTrue(url == expected + "webp" || url == expected + "gif", url)
        assertEquals(animatedWebpSupported, url.endsWith(".webp"))
    }

    @Test
    fun foreignHostsAndOddIdsStayUntouched() {
        val twitch = "https://static-cdn.jtvnw.net/gifs/abc/giphy.gif"
        val giphy = "https://media.giphy.com/media/abc/giphy.gif"

        assertEquals(twitch, GiphyMedia.chatUrl("abc", twitch))
        assertEquals(giphy, GiphyMedia.chatUrl("../evil", giphy))
    }

    @Test
    fun webpPreviewIsUsedOnlyWhenPresent() {
        assertEquals("a.gif", GiphyMedia.preferWebp("a.gif", null))
        assertEquals("a.gif", GiphyMedia.preferWebp("a.gif", ""))
        assertEquals(if (animatedWebpSupported) "a.webp" else "a.gif", GiphyMedia.preferWebp("a.gif", "a.webp"))
    }
}
