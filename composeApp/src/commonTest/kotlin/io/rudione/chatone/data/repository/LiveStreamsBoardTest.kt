package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.dto.ChannelData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class LiveStreamsBoardTest {

    private var now = 1_000_000L
    private val board = LiveStreamsBoard(nowMs = { now })

    @Test
    fun aBatchAnswersForEveryCheckedChannelIncludingOfflineOnes() {
        board.publish(listOf("Alpha", "beta", "gamma"), listOf(stream("alpha", viewers = 10)))
        assertEquals(10, board.fresh("alpha", MAX_AGE_MS)?.stream?.viewerCount)
        val offline = assertNotNull(board.fresh("BETA", MAX_AGE_MS))
        assertNull(offline.stream)
        assertNull(board.fresh("unknown", MAX_AGE_MS))
    }

    @Test
    fun aReadingStopsBeingFreshWhenItAges() {
        board.publish(listOf("alpha"), emptyList())
        now += MAX_AGE_MS
        assertNotNull(board.fresh("alpha", MAX_AGE_MS))
        now += 1
        assertNull(board.fresh("alpha", MAX_AGE_MS))
    }

    @Test
    fun aLaterBatchKeepsAnswersForChannelsItDidNotCheck() {
        board.publish(listOf("alpha"), listOf(stream("alpha", viewers = 10)))
        board.publish(listOf("beta"), emptyList())
        assertEquals(10, board.fresh("alpha", MAX_AGE_MS)?.stream?.viewerCount)
    }

    @Test
    fun subscribersSeeEveryNewCheckOfTheirChannelOnly() = runTest {
        val seen = ArrayList<Int?>()
        val job = launch { board.freshReadings("alpha", MAX_AGE_MS).collect { seen += it.stream?.viewerCount } }
        runCurrent()
        board.publish(listOf("alpha"), listOf(stream("alpha", viewers = 10)))
        runCurrent()
        board.publish(listOf("beta"), listOf(stream("beta", viewers = 99)))
        runCurrent()
        board.publish(listOf("alpha", "beta"), emptyList())
        runCurrent()
        job.cancel()
        assertEquals(listOf<Int?>(10, null), seen)
    }

    @Test
    fun aStaleAnswerIsNotReplayedToANewSubscriber() = runTest {
        board.publish(listOf("alpha"), listOf(stream("alpha", viewers = 10)))
        now += MAX_AGE_MS + 1
        val seen = ArrayList<Int?>()
        val job = launch { board.freshReadings("alpha", MAX_AGE_MS).collect { seen += it.stream?.viewerCount } }
        runCurrent()
        assertEquals(emptyList(), seen)
        board.publish(listOf("alpha"), listOf(stream("alpha", viewers = 25)))
        runCurrent()
        job.cancel()
        assertEquals(listOf<Int?>(25), seen)
    }

    @Test
    fun waitingForAFreshAnswerEndsWhenSomeoneElsePublishesIt() = runTest {
        val waiter = launch {
            val reading = withTimeoutOrNull(5_000) { board.awaitFresh("alpha", MAX_AGE_MS) }
            assertEquals(7, reading?.stream?.viewerCount)
        }
        runCurrent()
        board.publish(listOf("alpha"), listOf(stream("alpha", viewers = 7)))
        waiter.join()
    }

    private fun stream(login: String, viewers: Int) = ChannelData(
        id = "stream-$login",
        userId = "id-$login",
        userLogin = login,
        userName = login,
        type = "live",
        title = "title",
        viewerCount = viewers
    )

    private companion object {
        const val MAX_AGE_MS = 90_000L
    }
}
