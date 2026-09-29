package io.rudione.chatone.presentation.chat

import io.rudione.chatone.domain.model.DisplayMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ProfilePopupStackTest {

    private fun msg(userId: String, login: String = "user$userId") = DisplayMessage.PrivMsg(
        id = "m_$login",
        timestamp = 0L,
        channel = "chan",
        userId = userId,
        username = login,
        displayName = login,
        tokens = emptyList(),
        color = null,
        badges = emptyList(),
        isModerator = false,
        isSubscriber = false,
        isVip = false,
        isBroadcaster = false,
        isMention = false,
        isAction = false
    )

    private fun ProfilePopupStack.users() = slots.map { it.message.username }

    @Test
    fun unpinnedProfileIsReplacedByTheNextOne() {
        val stack = ProfilePopupStack()
        stack.open(msg("1"))
        stack.open(msg("2"))
        assertEquals(listOf("user2"), stack.users())
    }

    @Test
    fun pinnedProfilesStayWhileNewOnesOpen() {
        val stack = ProfilePopupStack()
        stack.open(msg("1"))
        stack.setPinned(stack.slots.single(), true)
        stack.open(msg("2"))
        stack.setPinned(stack.slots.last(), true)
        stack.open(msg("3"))
        stack.open(msg("4"))
        assertEquals(listOf("user1", "user2", "user4"), stack.users())
        assertEquals(3, stack.slots.map { it.key }.distinct().size)
    }

    @Test
    fun reopeningAnOpenUserReusesTheirCard() {
        val stack = ProfilePopupStack()
        stack.open(msg("1"))
        val first = stack.slots.single()
        stack.setPinned(first, true)
        stack.open(msg("1", login = "renamed"))
        assertSame(first, stack.slots.single())
        assertEquals("renamed", first.message.username)
    }

    @Test
    fun unpinningLeavesASingleTransientCardAndCloseRemovesIt() {
        val stack = ProfilePopupStack()
        stack.open(msg("1"))
        val first = stack.slots.single()
        stack.setPinned(first, true)
        stack.open(msg("2"))
        stack.setPinned(first, false)
        assertEquals(listOf("user1"), stack.users())
        stack.close(first)
        assertEquals(emptyList(), stack.users())
    }

    @Test
    fun openingByLoginReusesTheCardOfTheSameUser() {
        val stack = ProfilePopupStack()
        stack.open(msg("1", login = "streamer"))
        stack.setPinned(stack.slots.single(), true)
        stack.openLogin("@Streamer", channel = "chan", nowMs = 0L)
        assertEquals(listOf("streamer"), stack.users())
        assertEquals("1", stack.slots.single().message.userId)
        stack.openLogin("@other", channel = "chan", nowMs = 0L)
        assertEquals(listOf("streamer", "other"), stack.users())
        assertEquals("", stack.slots.last().message.userId)
        stack.openLogin("   ", channel = "chan", nowMs = 0L)
        assertEquals(2, stack.slots.size)
    }

    @Test
    fun cardsWithoutUserIdAreNeverMerged() {
        val stack = ProfilePopupStack()
        stack.open(msg("1"))
        stack.setPinned(stack.slots.single(), true)
        stack.open(msg("", login = "mentioned"))
        assertEquals(listOf("user1", "mentioned"), stack.users())
    }
}
