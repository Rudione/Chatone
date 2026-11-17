package io.rudione.chatone.data.remote

import io.rudione.chatone.data.remote.emote.SevenTvCommands
import io.rudione.chatone.data.remote.emote.SevenTvSubscriptionBook
import io.rudione.chatone.data.remote.emote.SevenTvSubscriptionBook.Kind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SevenTvSubscriptionBookTest {

    @Test
    fun commandsAreValidSubscribeFrames() {
        assertEquals(
            """{"op":35,"d":{"type":"emote_set.update","condition":{"object_id":"01FE9DRF000009TR6M9N941CYW"}}}""",
            SevenTvCommands.emoteSet("01FE9DRF000009TR6M9N941CYW")
        )
        assertEquals(
            """{"op":35,"d":{"type":"user.update","condition":{"object_id":"u1"}}}""",
            SevenTvCommands.user("u1")
        )
        assertEquals(
            listOf("entitlement.create", "entitlement.delete", "cosmetic.create").map {
                """{"op":35,"d":{"type":"$it","condition":{"ctx":"channel","platform":"TWITCH","id":"71092938"}}}"""
            },
            SevenTvCommands.twitchChannel("71092938")
        )
    }

    @Test
    fun nothingIsSentBeforeHelloAndEverythingAfterIt() {
        val book = SevenTvSubscriptionBook()
        assertTrue(book.add(Kind.CHANNEL_SET, "setA").isEmpty())
        assertTrue(book.add(Kind.TWITCH_CHANNEL, "123").isEmpty())
        val first = book.startSession(500)
        assertEquals(1 + 3, first.size)
        assertEquals(listOf(SevenTvCommands.emoteSet("setB")), book.add(Kind.CHANNEL_SET, "setB"))
        assertTrue(book.add(Kind.CHANNEL_SET, "setB").isEmpty())
        book.endSession()
        assertTrue(book.add(Kind.USER, "u1").isEmpty())
        assertEquals(1 + 1 + 3 + 1, book.startSession(500).size)
    }

    @Test
    fun invalidIdsAreRejected() {
        val book = SevenTvSubscriptionBook()
        book.startSession(500)
        listOf("", "a\"b", "set id", "x".repeat(65), "../x").forEach { id ->
            assertTrue(book.add(Kind.CHANNEL_SET, id).isEmpty())
        }
        assertEquals(0, book.count(Kind.CHANNEL_SET))
    }

    @Test
    fun personalSetsAreCapped() {
        val book = SevenTvSubscriptionBook(personalSetCap = 2)
        book.startSession(500)
        assertEquals(1, book.add(Kind.PERSONAL_SET, "p1").size)
        assertEquals(1, book.add(Kind.PERSONAL_SET, "p2").size)
        assertTrue(book.add(Kind.PERSONAL_SET, "p3").isEmpty())
        assertEquals(2, book.count(Kind.PERSONAL_SET))
    }

    @Test
    fun serverLimitIsNeverExceededAndChannelsGoFirst() {
        val book = SevenTvSubscriptionBook(personalSetCap = 100)
        repeat(50) { book.add(Kind.PERSONAL_SET, "p$it") }
        repeat(3) { book.add(Kind.CHANNEL_SET, "set$it") }
        repeat(2) { book.add(Kind.TWITCH_CHANNEL, "${it + 1}") }
        val sent = book.startSession(10)
        assertEquals(10, sent.size)
        assertEquals((0 until 3).map { SevenTvCommands.emoteSet("set$it") }, sent.take(3))
        assertEquals(SevenTvCommands.twitchChannel("1") + SevenTvCommands.twitchChannel("2"), sent.subList(3, 9))
        assertEquals(SevenTvCommands.emoteSet("p0"), sent[9])
        assertTrue(book.add(Kind.CHANNEL_SET, "late").isEmpty())
        assertEquals(1, book.startSession(11).count { it == SevenTvCommands.emoteSet("late") })
    }

    @Test
    fun concurrentAddsAndSessionsStayConsistent() = runTest {
        val book = SevenTvSubscriptionBook(personalSetCap = 10_000)
        coroutineScope {
            repeat(4) { worker ->
                launch(Dispatchers.Default) {
                    repeat(1_000) { index -> book.add(Kind.PERSONAL_SET, "w${worker}i$index") }
                }
            }
            launch(Dispatchers.Default) {
                repeat(200) {
                    val sent = book.startSession(100_000)
                    assertEquals(sent.size, sent.toSet().size)
                    book.endSession()
                }
            }
        }
        assertEquals(4_000, book.count(Kind.PERSONAL_SET))
        assertEquals(4_000, book.startSession(100_000).size)
    }
}
