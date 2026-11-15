package io.rudione.chatone.presentation.chat

import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.IrcEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ChatGiftBundlesTest {
    private fun notice(
        id: String,
        sender: String = "alice",
        recipient: String? = null,
        total: Int = 1,
        community: String? = null,
        timestamp: Long = 1_000,
        anonymous: Boolean = false
    ): DisplayMessage.UserNoticeMsg {
        val type = (if (anonymous) "anon" else "") + if (recipient == null) "submysterygift" else "subgift"
        val tags = buildMap {
            put("id", id)
            put("user-id", sender)
            put("login", sender)
            put("display-name", sender.replaceFirstChar(Char::uppercase))
            put("color", "#12AB34")
            put("msg-param-sub-plan", "1000")
            if (recipient != null) {
                put("msg-param-recipient-id", recipient)
                put("msg-param-recipient-user-name", recipient)
                put("msg-param-recipient-display-name", recipient.uppercase())
            } else put("msg-param-mass-gift-count", total.toString())
            if (community != null) put("msg-param-community-gift-id", community)
        }
        val event = IrcEvent.UserNotice("channel", "gift", null, type, tags)
        return DisplayMessage.UserNoticeMsg(id, timestamp, "channel", "gift", noticeType = type, details = ChatNoticeParser.parse(event, timestamp))
    }

    private fun bundle(messages: List<DisplayMessage>) = assertIs<DisplayMessage.UserNoticeMsg>(messages.single()).details!!.gifts!!

    @Test
    fun summaryAndRecipientsBecomeOneRowWithoutDoubleCounting() {
        val result = ChatGiftBundles.fold(listOf(notice("summary", total = 2), notice("first", recipient = "bob"), notice("second", recipient = "carol")))
        assertEquals(2, bundle(result).total)
        assertEquals(listOf("bob", "carol"), bundle(result).recipients.map { it.login })
    }

    @Test
    fun arrivingRecipientsDoNotChangeTheClosedRowsRenderingData() {
        val summary = notice("summary", total = 10)
        val updated = ChatGiftBundles.append(listOf(summary), notice("recipient", recipient = "bob")).single() as DisplayMessage.UserNoticeMsg
        assertEquals(summary.compactNoticeData(), updated.compactNoticeData())
    }

    @Test
    fun recipientsCanArriveBeforeTheSummary() {
        val result = ChatGiftBundles.fold(listOf(notice("first", recipient = "bob"), notice("second", recipient = "carol"), notice("summary", total = 2)))
        assertEquals(2, bundle(result).total)
        assertEquals(2, bundle(result).recipients.size)
    }

    @Test
    fun differentSendersAndCommunitiesRemainSeparate() {
        assertEquals(2, ChatGiftBundles.fold(listOf(notice("a"), notice("b", sender = "bob", recipient = "carol"))).size)
        assertEquals(2, ChatGiftBundles.fold(listOf(notice("a", community = "one"), notice("b", community = "two", recipient = "carol"))).size)
    }

    @Test
    fun summariesWithTheSameBatchIdKeepOneTotal() {
        val result = ChatGiftBundles.fold(listOf(
            notice("summary-one", total = 2, community = "batch"),
            notice("summary-two", total = 3, community = "batch")
        ))
        assertEquals(3, bundle(result).total)
    }

    @Test
    fun explicitCommunityIdsHandleOverlappingBatches() {
        val result = ChatGiftBundles.fold(listOf(
            notice("a", total = 2, community = "one"), notice("b", total = 2, community = "two"),
            notice("c", community = "one", recipient = "bob"), notice("d", community = "two", recipient = "carol")
        )).filterIsInstance<DisplayMessage.UserNoticeMsg>()
        assertEquals(listOf("bob"), result[0].details!!.gifts!!.recipients.map { it.login })
        assertEquals(listOf("carol"), result[1].details!!.gifts!!.recipients.map { it.login })
    }

    @Test
    fun overlappingUncorrelatedSummariesKeepAllGiftsInOneSenderBundle() {
        val result = ChatGiftBundles.fold(listOf(
            notice("summary-one", total = 2), notice("summary-two", total = 3),
            notice("recipient-one", recipient = "bob"), notice("recipient-two", recipient = "carol")
        ))
        assertEquals(5, bundle(result).total)
        assertEquals(2, bundle(result).recipients.size)
    }

    @Test
    fun placeholderCommunityIdsDoNotJoinUnrelatedHistoricalGifts() {
        val result = ChatGiftBundles.fold(listOf(
            notice("old", total = 2, community = "0"),
            notice("new", recipient = "bob", community = "0", timestamp = 60_000)
        ))
        assertEquals(2, result.size)
    }

    @Test
    fun completedAndOldBatchesAreNotReused() {
        assertEquals(2, ChatGiftBundles.fold(listOf(notice("a", total = 1), notice("b", recipient = "bob"), notice("c", recipient = "carol"))).size)
        assertEquals(2, ChatGiftBundles.fold(listOf(notice("a", total = 2), notice("b", recipient = "bob", timestamp = 30_000))).size)
    }

    @Test
    fun historyReplayAndDuplicateRecipientsDoNotInflateCounts() {
        val summary = notice("a", total = 2)
        val recipient = notice("b", recipient = "bob")
        val result = ChatGiftBundles.fold(listOf(summary, recipient, summary, recipient, recipient.copy(id = "history-copy")))
        assertEquals(2, bundle(result).total)
        assertEquals(1, bundle(result).recipients.size)
        assertEquals(emptyList(), ChatHistoryEvents.newMessages(listOf(summary), result))
        assertEquals(1, ChatHistoryEvents.newMessages(listOf(notice("other-summary", total = 2)), result).size)
    }

    @Test
    fun anonymousAndNamedGiftBatchesNeverMix() {
        val result = ChatGiftBundles.fold(listOf(notice("anon", total = 2, anonymous = true), notice("anon-recipient", recipient = "bob", anonymous = true)))
        assertEquals(1, bundle(result).recipients.size)
        assertEquals(2, ChatGiftBundles.append(result, notice("named", recipient = "carol")).size)
    }

    @Test
    fun raidAndStreakMetadataKeepIdentityAndColorWithoutAnInnerMessage() {
        val tags = mapOf("user-id" to "42", "login" to "alice", "display-name" to "Alice", "color" to "#123456", "msg-param-viewerCount" to "15000")
        val raid = ChatNoticeParser.parse(IrcEvent.UserNotice("channel", "raid", null, "raid", tags), 1_000)
        assertEquals("42", assertNotNull(raid.author).userId)
        assertEquals("#123456", raid.author.color)
        assertEquals(15_000, raid.raidViewers)
        assertNull(raid.gifts)
        val streak = ChatNoticeParser.parse(IrcEvent.UserNotice("channel", "streak", null, "viewermilestone", tags + mapOf("msg-param-category" to "watch-streak", "msg-param-value" to "7")), 1_000)
        assertEquals(7, streak.watchStreak)
        assertNull(streak.raidViewers)
    }
}
