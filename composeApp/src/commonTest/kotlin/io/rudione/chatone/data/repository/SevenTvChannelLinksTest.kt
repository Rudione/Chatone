package io.rudione.chatone.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SevenTvChannelLinksTest {

    @Test
    fun reverseIndexesFollowEveryChange() {
        val links = SevenTvChannelLinks()
        val alpha = SevenTvChannelLink("1", "setA", "userA")
        links.put("alpha", alpha)
        links.put("gamma", SevenTvChannelLink("3", "setA", null))
        assertEquals(listOf("alpha", "gamma"), links.channelsUsingSet("setA"))
        assertEquals(listOf("alpha"), links.channelsForUser("userA"))

        links.put("alpha", SevenTvChannelLink("1", "setB", "userA"))
        assertEquals(listOf("gamma"), links.channelsUsingSet("setA"))
        assertEquals(listOf("alpha"), links.channelsUsingSet("setB"))

        links.remove("gamma")
        assertTrue(links.channelsUsingSet("setA").isEmpty())
        assertNull(links.get("gamma"))
    }

    @Test
    fun replaceOnlyWinsAgainstTheExpectedLink() {
        val links = SevenTvChannelLinks()
        val old = SevenTvChannelLink("1", "setA", "userA")
        links.put("alpha", old)
        val newer = old.copy(emoteSetId = "setB")
        assertTrue(links.replace("alpha", old, newer))
        assertFalse(links.replace("alpha", old, old.copy(emoteSetId = "setC")))
        assertEquals(newer, links.get("alpha"))
        assertEquals(listOf("alpha"), links.channelsUsingSet("setB"))
        assertTrue(links.channelsUsingSet("setC").isEmpty())
    }
}
