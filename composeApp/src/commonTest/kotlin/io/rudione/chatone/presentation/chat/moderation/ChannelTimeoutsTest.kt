package io.rudione.chatone.presentation.chat.moderation

import io.rudione.chatone.data.remote.HelixModerationException
import io.rudione.chatone.data.repository.ChannelBanGateway
import io.rudione.chatone.data.repository.ModerationActor
import io.rudione.chatone.util.Result
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ChannelTimeoutsTest {

    private class FakeGateway : ChannelBanGateway {
        val calls = mutableListOf<String>()
        val banReplies = ArrayDeque<Result<Unit>>()
        var unbanReply: Result<Unit> = Result.Success(Unit)

        override suspend fun ban(actor: ModerationActor, userId: String, durationSeconds: Int?, reason: String?): Result<Unit> {
            calls += "ban:$userId:${durationSeconds ?: "perm"}"
            return banReplies.removeFirstOrNull() ?: Result.Success(Unit)
        }

        override suspend fun unban(actor: ModerationActor, userId: String): Result<Unit> {
            calls += "unban:$userId"
            return unbanReply
        }
    }

    private val actor = ModerationActor(accessToken = "token", channelId = "42", moderatorId = "7")
    private val gateway = FakeGateway()
    private val timeouts = ChannelTimeouts(gateway)

    @Test
    fun aRegularTimeoutIsASingleCall() = runTest {
        val result = timeouts.timeout(actor, "77", 600, null, knownBanned = false)
        assertEquals(Result.Success(TimeoutOutcome.APPLIED), result)
        assertEquals(listOf("ban:77:600"), gateway.calls)
    }

    @Test
    fun aKnownPermabanIsLiftedBeforeTheTimeout() = runTest {
        val result = timeouts.timeout(actor, "77", 600, null, knownBanned = true)
        assertEquals(Result.Success(TimeoutOutcome.REPLACED_BAN), result)
        assertEquals(listOf("unban:77", "ban:77:600"), gateway.calls)
    }

    @Test
    fun anUnknownPermabanIsReplacedWhenTwitchRejectsTheTimeout() = runTest {
        gateway.banReplies += Result.Error(HelixModerationException(400, "The user specified in the user_id field is already banned."))
        val result = timeouts.timeout(actor, "77", 60, "spam", knownBanned = false)
        assertEquals(Result.Success(TimeoutOutcome.REPLACED_BAN), result)
        assertEquals(listOf("ban:77:60", "unban:77", "ban:77:60"), gateway.calls)
    }

    @Test
    fun otherFailuresAreReportedWithoutUnbanning() = runTest {
        val failure = HelixModerationException(403, "Missing scope")
        gateway.banReplies += Result.Error(failure)
        val result = timeouts.timeout(actor, "77", 60, null, knownBanned = false)
        assertSame(failure, (result as Result.Error).exception)
        assertEquals(listOf("ban:77:60"), gateway.calls)
    }

    @Test
    fun aFailedUnbanStopsBeforeTheTimeout() = runTest {
        gateway.unbanReply = Result.Error(HelixModerationException(400, "The user is not banned."))
        val result = timeouts.timeout(actor, "77", 60, null, knownBanned = true)
        assertTrue(result is Result.Error)
        assertEquals(listOf("unban:77"), gateway.calls)
    }

    @Test
    fun banLedgerTracksOnlyRealChanges() {
        val empty = emptySet<String>()
        val banned = empty.withBanState("77", banned = true)
        assertEquals(setOf("77"), banned)
        assertSame(banned, banned.withBanState("77", banned = true))
        assertSame(banned, banned.withBanState(null, banned = false))
        assertEquals(empty, banned.withBanState("77", banned = false))
    }
}
