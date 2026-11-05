package io.rudione.chatone.presentation.chat.threads

import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.domain.model.DisplayMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReplyThreadsTest {

    private fun msg(id: String, login: String, parent: String? = null, threadRoot: String? = parent) = DisplayMessage.PrivMsg(
        id = id,
        timestamp = 0L,
        channel = "chan",
        userId = login,
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
        isAction = false,
        rawMessage = ChatMessage(
            id = id,
            channelId = "1",
            channelName = "chan",
            userId = login,
            username = login,
            displayName = login,
            message = "text $id",
            timestamp = 0L,
            replyParentMsgId = parent,
            replyParentUserLogin = parent?.let { "author_$it" },
            replyParentDisplayName = parent?.let { "Author_$it" },
            replyParentMsgBody = parent?.let { "body $it" },
            replyThreadParentMsgId = threadRoot
        )
    )

    @Test
    fun collectsRootAndEveryReplyOfTheThreadInOrder() {
        val messages = listOf(
            msg("root", "decoy"),
            msg("other", "someone"),
            msg("r1", "funer", parent = "root"),
            msg("r2", "decoy", parent = "r1", threadRoot = "root"),
            msg("elsewhere", "x", parent = "other")
        )
        val thread = replyThreadOf(messages, "root")
        assertEquals("root", thread.root?.id)
        assertEquals(listOf("r1", "r2"), thread.replies.map { it.id })
        assertEquals("decoy", thread.starterLogin)
        assertNull(thread.origin)
    }

    @Test
    fun scrolledAwayRootIsRebuiltFromTheReplyContext() {
        val thread = replyThreadOf(listOf(msg("r1", "funer", parent = "gone")), "gone")
        assertNull(thread.root)
        assertEquals(ThreadOrigin("author_gone", "Author_gone", "body gone"), thread.origin)
        assertEquals("r1", thread.replyTarget?.id)
    }

    @Test
    fun legacyRepliesWithoutThreadTagFallBackToTheParent() {
        val legacy = msg("r1", "funer", parent = "root", threadRoot = null)
        assertEquals("root", legacy.threadRootId())
    }
}
