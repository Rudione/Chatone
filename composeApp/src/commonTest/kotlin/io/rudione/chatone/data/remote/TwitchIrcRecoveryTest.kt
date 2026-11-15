package io.rudione.chatone.data.remote

import io.rudione.chatone.domain.model.ChatMessage
import io.rudione.chatone.util.network.NetworkMonitor
import io.rudione.chatone.util.network.NetworkStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TwitchIrcRecoveryTest {
    private class Socket : IrcSocket {
        val incoming = Channel<String>(Channel.UNLIMITED)
        val sent = mutableListOf<String>()
        var cancelled = false
        override suspend fun receive(): String? = incoming.receiveCatching().getOrNull()
        override suspend fun send(line: String) { sent.add(line) }
        override fun cancel() { cancelled = true; incoming.cancel() }
    }

    private class Fixture(val scope: TestScope) {
        val path = MutableStateFlow(NetworkStatus(true, "wifi"))
        val sockets = mutableListOf<Socket>()
        var opens = 0
        var failures = 0
        var clockOffset = 0L
        val client = TwitchIrcClient(
            scope.backgroundScope,
            NetworkMonitor(scope.backgroundScope, path),
            openSocket = {
                opens++
                if (failures-- > 0) throw IllegalStateException("offline")
                Socket().also(sockets::add)
            },
            now = { scope.testScheduler.currentTime + clockOffset },
            jitter = { 0L }
        )
        val latest get() = sockets.last()
        suspend fun start() {
            client.joinChannel("example")
            client.connect("viewer", "token")
            scope.runCurrent()
        }
        suspend fun welcome() {
            latest.incoming.send(":tmi.twitch.tv 001 viewer :Welcome")
            scope.runCurrent()
        }
    }

    @Test fun waitsForAuthenticationBeforeReportingConnected() = runTest {
        val f = Fixture(this)
        f.start()
        assertIs<TwitchIrcClient.ConnectionState.Connecting>(f.client.connectionState.value)
        assertFalse(f.latest.sent.any { it.startsWith("JOIN") })
        f.welcome()
        assertIs<TwitchIrcClient.ConnectionState.Connected>(f.client.connectionState.value)
        assertEquals(1, f.latest.sent.count { it == "JOIN #example" })
    }

    @Test fun retriesBeyondFiveFailuresAndRejoinsWithoutRestartingApp() = runTest {
        val f = Fixture(this)
        f.failures = 8
        f.start()
        repeat(200) {
            if (f.sockets.isEmpty()) { advanceTimeBy(1_000); runCurrent() }
        }
        assertTrue(f.opens >= 9)
        f.welcome()
        assertIs<TwitchIrcClient.ConnectionState.Connected>(f.client.connectionState.value)
        assertTrue("JOIN #example" in f.latest.sent)
    }

    @Test fun reconnectsAfterNormalSocketClosure() = runTest {
        val f = Fixture(this)
        f.start(); f.welcome()
        val old = f.latest
        old.incoming.close()
        runCurrent()
        assertIs<TwitchIrcClient.ConnectionState.Reconnecting>(f.client.connectionState.value)
        advanceTimeBy(1_001); runCurrent()
        assertEquals(2, f.opens)
        f.welcome()
        assertTrue(old.cancelled)
        assertTrue("JOIN #example" in f.latest.sent)
    }

    @Test fun unansweredPingInvalidatesGreenStateAndReopensSocket() = runTest {
        val f = Fixture(this)
        f.start(); f.welcome()
        val old = f.latest
        advanceTimeBy(30_001); runCurrent()
        assertTrue(old.sent.any { it.startsWith("PING :chatone-") })
        advanceTimeBy(10_000); runCurrent()
        val state = assertIs<TwitchIrcClient.ConnectionState.Reconnecting>(f.client.connectionState.value)
        assertEquals(TwitchIrcClient.FailureReason.TIMEOUT, state.reason)
        assertTrue(old.cancelled)
        advanceTimeBy(1_001); runCurrent()
        assertEquals(2, f.opens)
    }

    @Test fun matchedPongKeepsConnectionAliveButUnrelatedPongDoesNot() = runTest {
        val f = Fixture(this)
        f.start(); f.welcome()
        advanceTimeBy(30_001); runCurrent()
        val token = f.latest.sent.last { it.startsWith("PING :") }.substringAfter("PING :")
        f.latest.incoming.send(":tmi.twitch.tv PONG tmi.twitch.tv :$token")
        runCurrent()
        advanceTimeBy(30_001); runCurrent()
        assertEquals(1, f.opens)
        f.latest.incoming.send(":tmi.twitch.tv PONG tmi.twitch.tv :wrong")
        runCurrent()
        advanceTimeBy(10_001); runCurrent()
        assertIs<TwitchIrcClient.ConnectionState.Reconnecting>(f.client.connectionState.value)
    }

    @Test fun pausesOfflineAndReconnectsImmediatelyWhenNetworkReturns() = runTest {
        val f = Fixture(this)
        f.start(); f.welcome()
        f.path.value = NetworkStatus(false)
        runCurrent()
        assertIs<TwitchIrcClient.ConnectionState.WaitingForNetwork>(f.client.connectionState.value)
        val offlineOpens = f.opens
        advanceTimeBy(300_000); runCurrent()
        assertEquals(offlineOpens, f.opens)
        f.path.value = NetworkStatus(true, "mobile")
        runCurrent()
        assertEquals(offlineOpens + 1, f.opens)
        f.welcome()
        assertTrue("JOIN #example" in f.latest.sent)
    }

    @Test fun pathChangeAndWakeReplaceStaleSocketEvenWhenItLooksConnected() = runTest {
        val f = Fixture(this)
        f.start(); f.welcome()
        val first = f.latest
        f.path.value = NetworkStatus(true, "ethernet")
        runCurrent(); f.welcome()
        assertTrue(first.cancelled)
        val second = f.latest
        f.path.value = NetworkStatus(true, "ethernet", wakeVersion = 1)
        runCurrent(); f.welcome()
        assertTrue(second.cancelled)
        assertEquals(3, f.opens)
    }

    @Test fun clockJumpAfterSleepInvalidatesSocketWithoutWaitingForPongTimeout() = runTest {
        val f = Fixture(this)
        f.start(); f.welcome()
        f.clockOffset = 3_600_000
        advanceTimeBy(30_001); runCurrent()
        assertTrue(f.latest.cancelled)
        assertIs<TwitchIrcClient.ConnectionState.Reconnecting>(f.client.connectionState.value)
    }

    @Test fun manualReconnectWorksWhileStateIsStillGreenAndNewMessagesArrive() = runTest {
        val f = Fixture(this)
        val messages = mutableListOf<ChatMessage>()
        backgroundScope.launch { f.client.messages.collect(messages::add) }
        f.start(); f.welcome()
        val first = f.latest
        f.client.reconnect()
        runCurrent(); f.welcome()
        f.latest.incoming.send("@id=after;user-id=123;display-name=Other;room-id=1;tmi-sent-ts=1000 :other!other@other PRIVMSG #example :fresh message")
        runCurrent()
        assertTrue(first.cancelled)
        assertEquals(listOf("fresh message"), messages.map { it.message })
    }

    @Test fun explicitDisconnectNeverSchedulesRetryAndClearsChannelIntent() = runTest {
        val f = Fixture(this)
        f.start(); f.welcome()
        f.client.disconnect()
        advanceTimeBy(300_000); runCurrent()
        assertEquals(1, f.opens)
        assertIs<TwitchIrcClient.ConnectionState.Disconnected>(f.client.connectionState.value)
        f.client.connect("viewer", "token")
        runCurrent(); f.welcome()
        assertFalse(f.latest.sent.any { it.startsWith("JOIN") })
    }

    @Test fun serverReconnectAndPartPreserveOnlyDesiredChannels() = runTest {
        val f = Fixture(this)
        f.start(); f.welcome()
        f.client.joinChannel("second")
        f.client.partChannel("example")
        f.latest.incoming.send(":tmi.twitch.tv RECONNECT")
        runCurrent(); advanceTimeBy(1_001); runCurrent(); f.welcome()
        assertTrue("JOIN #second" in f.latest.sent)
        assertFalse("JOIN #example" in f.latest.sent)
    }

    @Test fun invalidAuthenticationWaitsForRecoveryAndFreshCredentialsReplaceOldSession() = runTest {
        val f = Fixture(this)
        f.start()
        f.latest.incoming.send(":tmi.twitch.tv NOTICE * :Login authentication failed")
        runCurrent()
        val state = assertIs<TwitchIrcClient.ConnectionState.Error>(f.client.connectionState.value)
        assertEquals(TwitchIrcClient.FailureReason.AUTHENTICATION, state.reason)
        advanceTimeBy(120_000); runCurrent()
        assertEquals(1, f.opens)
        f.client.connect("viewer", "new-token")
        runCurrent(); f.welcome()
        assertTrue("PASS oauth:new-token" in f.latest.sent)
        assertTrue("JOIN #example" in f.latest.sent)
    }

    @Test fun disconnectWinsOverAPendingConnectRequest() = runTest {
        val f = Fixture(this)
        f.client.connect("viewer", "token")
        f.client.disconnect()
        runCurrent()
        assertEquals(0, f.opens)
        assertIs<TwitchIrcClient.ConnectionState.Disconnected>(f.client.connectionState.value)
    }

    @Test fun latestCredentialsWinWhenConnectRequestsOverlap() = runTest {
        val f = Fixture(this)
        f.client.connect("viewer", "old-token")
        f.client.connect("viewer", "fresh-token")
        runCurrent()
        assertEquals(1, f.opens)
        assertTrue("PASS oauth:fresh-token" in f.latest.sent)
        assertFalse("PASS oauth:old-token" in f.latest.sent)
    }

    @Test fun connectionTimeoutIsRetriedAndRepeatedConnectDoesNotCreateParallelSockets() = runTest {
        var opens = 0
        val gate = CompletableDeferred<IrcSocket>()
        val client = TwitchIrcClient(backgroundScope, NetworkMonitor(backgroundScope, MutableStateFlow(NetworkStatus(true, "wifi"))), {
            opens++
            gate.await()
        }, now = { testScheduler.currentTime }, jitter = { 0 })
        client.connect("viewer", "token")
        client.connect("viewer", "token")
        runCurrent()
        assertEquals(1, opens)
        advanceTimeBy(15_001); runCurrent()
        assertIs<TwitchIrcClient.ConnectionState.Reconnecting>(client.connectionState.value)
        advanceTimeBy(1_001); runCurrent()
        assertEquals(2, opens)
        client.disconnect()
        advanceTimeBy(60_000); runCurrent()
        assertEquals(2, opens)
    }
}
