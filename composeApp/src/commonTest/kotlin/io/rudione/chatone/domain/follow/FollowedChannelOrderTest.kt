package io.rudione.chatone.domain.follow

import kotlin.test.Test
import kotlin.test.assertEquals

class FollowedChannelOrderTest {

    private fun channel(login: String, viewers: Int? = null, followedAt: Long? = null, category: String? = null) =
        FollowedChannel(login, login, login.uppercase(), "", followedAt, viewers, category)

    @Test
    fun liveChannelsComeFirstByViewersThenRecentFollows() {
        val sorted = FollowedChannelOrder.sort(
            listOf(
                channel("old", followedAt = 1),
                channel("small", viewers = 3),
                channel("fresh", followedAt = 9),
                channel("big", viewers = 7000)
            )
        )
        assertEquals(listOf("big", "small", "fresh", "old"), sorted.map { it.login })
    }

    @Test
    fun filterMatchesLoginNameAndCategoryIgnoringHash() {
        val channels = listOf(channel("buster", 10, category = "Dota 2"), channel("lapshad"), channel("emiru", 5, category = "Just Chatting"))
        assertEquals(listOf("buster"), FollowedChannelOrder.filter(channels, "#BUS").map { it.login })
        assertEquals(listOf("emiru"), FollowedChannelOrder.filter(channels, "chatting").map { it.login })
        assertEquals(3, FollowedChannelOrder.filter(channels, "  ").size)
    }
}
