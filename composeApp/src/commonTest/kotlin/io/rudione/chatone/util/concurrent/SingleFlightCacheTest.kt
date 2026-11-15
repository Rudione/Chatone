package io.rudione.chatone.util.concurrent

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SingleFlightCacheTest {

    private var now = 0L

    @Test
    fun concurrentLoadsOfOneKeyShareASingleProduction() = runTest {
        val cache = cache(capacity = 4)
        val gate = CompletableDeferred<String>()
        var productions = 0
        val waiters = List(12) {
            async {
                cache.load("paint") {
                    productions++
                    gate.await()
                }
            }
        }
        runCurrent()
        gate.complete("image")
        assertEquals(List(12) { "image" }, waiters.awaitAll())
        assertEquals(1, productions)
    }

    @Test
    fun laterLoadsAreServedFromTheCache() = runTest {
        val cache = cache(capacity = 4)
        var productions = 0
        repeat(3) {
            cache.load("paint") {
                productions++
                "image"
            }
        }
        assertEquals(1, productions)
        assertEquals("image", cache.peek("paint"))
    }

    @Test
    fun evictsTheLeastRecentlyUsedValue() = runTest {
        val cache = cache(capacity = 2)
        cache.load("a") { "A" }
        cache.load("b") { "B" }
        cache.peek("a")
        cache.load("c") { "C" }
        assertEquals("A", cache.peek("a"))
        assertNull(cache.peek("b"))
        assertEquals("C", cache.peek("c"))
    }

    @Test
    fun aFailedProductionIsNotRepeatedUntilTheRetryWindowPasses() = runTest {
        val cache = cache(capacity = 4, retryFailedAfterMs = 30_000)
        var productions = 0
        val failing: suspend () -> String? = {
            productions++
            error("offline")
        }
        assertNull(cache.load("paint", failing))
        assertNull(cache.load("paint", failing))
        assertEquals(1, productions)
        now += 30_000
        val recovered = cache.load("paint") {
            productions++
            "image"
        }
        assertEquals("image", recovered)
        assertEquals(2, productions)
    }

    @Test
    fun aWaiterThatGivesUpDoesNotCancelTheSharedProduction() = runTest {
        val cache = cache(capacity = 4)
        val gate = CompletableDeferred<String>()
        val impatient = async { cache.load("paint") { gate.await() } }
        val patient = async { cache.load("paint") { gate.await() } }
        runCurrent()
        impatient.cancel()
        gate.complete("image")
        assertEquals("image", patient.await())
        assertEquals("image", cache.peek("paint"))
    }

    private fun TestScope.cache(capacity: Int, retryFailedAfterMs: Long = 30_000) = SingleFlightCache<String>(
        capacity = capacity,
        retryFailedAfterMs = retryFailedAfterMs,
        scope = backgroundScope,
        nowMs = { now }
    )
}
