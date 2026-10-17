package io.rudione.chatone.presentation.chat

import io.rudione.chatone.data.repository.ChannelRoleGateway
import io.rudione.chatone.data.repository.RoleActor
import io.rudione.chatone.domain.moderation.ChannelRole
import io.rudione.chatone.domain.moderation.RoleChange
import io.rudione.chatone.domain.moderation.RoleTarget
import io.rudione.chatone.domain.moderation.UserRoles
import io.rudione.chatone.util.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChannelRoleControllerTest {

    private class FakeGateway : ChannelRoleGateway {
        val calls = mutableListOf<Pair<RoleChange, RoleActor>>()
        val replies = ArrayDeque<CompletableDeferred<Result<Unit>>>()
        var autoSucceed = true

        override suspend fun apply(change: RoleChange, target: RoleTarget, actor: RoleActor): Result<Unit> {
            calls += change to actor
            if (autoSucceed) return Result.Success(Unit)
            val reply = CompletableDeferred<Result<Unit>>()
            replies.addLast(reply)
            return reply.await()
        }
    }

    private val target = RoleTarget(userId = "77", login = "viewer")
    private val gateway = FakeGateway()
    private val errors = mutableListOf<String>()
    private var state = ChatState(
        channelLogin = "streamer",
        channelId = "42",
        currentAccessToken = "token",
        currentUserId = "42",
        currentUserLogin = "streamer",
        isBroadcaster = true
    )
    private var clock = 0L

    private fun TestScope.controller() = ChannelRoleController(
        scope = backgroundScope,
        gateway = gateway,
        getState = { state },
        update = { state = it(state) },
        onError = { errors += it },
        now = { clock }
    )

    @Test
    fun grantingVipUpdatesTheCardRightAway() = runTest {
        val roles = controller()
        gateway.autoSucceed = false
        roles.set(target, UserRoles(), ChannelRole.VIP, granted = true)
        assertEquals(UserRoles(vip = true), state.roleOverrides[target.userId])
        runCurrent()
        gateway.replies.removeFirst().complete(Result.Success(Unit))
        runCurrent()
        assertEquals(UserRoles(vip = true), state.roleOverrides[target.userId])
        assertEquals(listOf(RoleChange(ChannelRole.VIP, true)), gateway.calls.map { it.first })
        assertTrue(gateway.calls.single().second.isBroadcaster)
    }

    @Test
    fun aFailedRequestRestoresTheCardAndReports() = runTest {
        val roles = controller()
        gateway.autoSucceed = false
        roles.set(target, UserRoles(), ChannelRole.MODERATOR, granted = true)
        runCurrent()
        gateway.replies.removeFirst().complete(Result.Error(Exception("user is banned")))
        runCurrent()
        assertNull(state.roleOverrides[target.userId])
        assertEquals(listOf("Failed to add moderator: user is banned"), errors)
    }

    @Test
    fun makingAVipAModeratorRemovesVipFirst() = runTest {
        val roles = controller()
        roles.set(target, UserRoles(vip = true), ChannelRole.MODERATOR, granted = true)
        runCurrent()
        assertEquals(
            listOf(RoleChange(ChannelRole.VIP, false), RoleChange(ChannelRole.MODERATOR, true)),
            gateway.calls.map { it.first }
        )
        assertEquals(UserRoles(moderator = true), state.roleOverrides[target.userId])
    }

    @Test
    fun aHalfDoneSwitchKeepsWhatTwitchAccepted() = runTest {
        val roles = controller()
        gateway.autoSucceed = false
        roles.set(target, UserRoles(vip = true), ChannelRole.MODERATOR, granted = true)
        runCurrent()
        gateway.replies.removeFirst().complete(Result.Success(Unit))
        runCurrent()
        gateway.replies.removeFirst().complete(Result.Error(Exception("rate limited")))
        runCurrent()
        assertEquals(UserRoles(), state.roleOverrides[target.userId])
        assertEquals(1, errors.size)
    }

    @Test
    fun leadModeratorsGoThroughTheSameFlow() = runTest {
        state = state.copy(isBroadcaster = false, isGrandMod = true, isMod = true, currentUserId = "5")
        val roles = controller()
        roles.set(target, UserRoles(), ChannelRole.VIP, granted = true)
        runCurrent()
        assertEquals(false, gateway.calls.single().second.isBroadcaster)
    }

    @Test
    fun regularModeratorsCannotManageRoles() = runTest {
        state = state.copy(isBroadcaster = false, isGrandMod = false, isMod = true, currentUserId = "5")
        val roles = controller()
        roles.set(target, UserRoles(), ChannelRole.VIP, granted = true)
        runCurrent()
        assertTrue(gateway.calls.isEmpty())
        assertTrue(state.roleOverrides.isEmpty())
    }

    @Test
    fun theBroadcasterAndYourselfAreNeverTargeted() = runTest {
        val roles = controller()
        roles.set(RoleTarget("42", "streamer"), UserRoles(), ChannelRole.VIP, granted = true)
        runCurrent()
        assertTrue(gateway.calls.isEmpty())
    }

    @Test
    fun aConfirmingMessageDropsTheOverride() = runTest {
        val roles = controller()
        roles.set(target, UserRoles(), ChannelRole.VIP, granted = true)
        runCurrent()
        roles.onMessage(target.userId, UserRoles())
        assertEquals(UserRoles(vip = true), state.roleOverrides[target.userId])
        roles.onMessage(target.userId, UserRoles(vip = true))
        assertNull(state.roleOverrides[target.userId])
    }

    @Test
    fun anOldOverrideGivesWayToFreshBadges() = runTest {
        val roles = controller()
        roles.set(target, UserRoles(), ChannelRole.VIP, granted = true)
        runCurrent()
        clock += 10 * 60_000L
        roles.onMessage(target.userId, UserRoles())
        assertNull(state.roleOverrides[target.userId])
    }
}
