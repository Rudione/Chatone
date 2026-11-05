package io.rudione.chatone.util.link

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TwitchLinksTest {

    @Test
    fun clipLinksInEveryShape() {
        val clip = TwitchLink.Clip("ExuberantDrab-pQvpycm")
        assertEquals(clip, TwitchLinks.parse("https://clips.twitch.tv/ExuberantDrab-pQvpycm"))
        assertEquals(clip, TwitchLinks.parse("https://www.twitch.tv/lapshad/clip/ExuberantDrab-pQvpycm?filter=clips"))
        assertEquals(clip, TwitchLinks.parse("https://m.twitch.tv/lapshad/clip/ExuberantDrab-pQvpycm"))
        assertEquals(clip, TwitchLinks.parse("https://clips.twitch.tv/embed?clip=ExuberantDrab-pQvpycm&parent=x"))
    }

    @Test
    fun videosKeepTheTimestamp() {
        assertEquals(TwitchLink.Video("2911576040", 3723), TwitchLinks.parse("https://www.twitch.tv/videos/2911576040?t=1h2m3s"))
        assertEquals(TwitchLink.Video("2911576040", 90), TwitchLinks.parse("https://twitch.tv/videos/2911576040?t=90"))
        assertEquals(TwitchLink.Video("1", 0), TwitchLinks.parse("https://www.twitch.tv/lapshad/v/1"))
    }

    @Test
    fun channelsAndReservedPaths() {
        assertEquals(TwitchLink.Channel("lapshad"), TwitchLinks.parse("https://www.twitch.tv/LapShad"))
        assertEquals(TwitchLink.Channel("lapshad"), TwitchLinks.parse("https://www.twitch.tv/lapshad/about"))
        assertEquals(TwitchLink.Channel("lapshad"), TwitchLinks.parse("https://www.twitch.tv/popout/lapshad/chat?popout="))
        assertNull(TwitchLinks.parse("https://www.twitch.tv/directory/category/dota-2"))
        assertNull(TwitchLinks.parse("https://www.twitch.tv/settings/profile"))
        assertNull(TwitchLinks.parse("https://evil.tv/lapshad"))
        assertNull(TwitchLinks.parse("https://twitch.tv.evil.com/lapshad"))
        assertNull(TwitchLinks.parse("javascript:alert(1)"))
    }

    @Test
    fun typedChannelNamesAndLinksBecomeALogin() {
        assertEquals("lapshad", TwitchLinks.channelLogin("  Lapshad "))
        assertEquals("lapshad", TwitchLinks.channelLogin("#lapshad"))
        assertEquals("lapshad", TwitchLinks.channelLogin("@lapshad"))
        assertEquals("lapshad", TwitchLinks.channelLogin("twitch.tv/lapshad"))
        assertEquals("lapshad", TwitchLinks.channelLogin("https://www.twitch.tv/lapshad/videos"))
    }

    @Test
    fun invalidChannelInputIsRejected() {
        assertNull(TwitchLinks.channelLogin(""))
        assertNull(TwitchLinks.channelLogin("два слова"))
        assertNull(TwitchLinks.channelLogin("name-with-dash"))
        assertNull(TwitchLinks.channelLogin("a".repeat(26)))
        assertNull(TwitchLinks.channelLogin("directory"))
        assertNull(TwitchLinks.channelLogin("https://evil.tv/lapshad"))
    }
}
