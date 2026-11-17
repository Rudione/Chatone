package io.rudione.chatone.presentation.chat.points

import io.rudione.chatone.data.remote.GqlChannelPointRedeemResult
import io.rudione.chatone.data.remote.GqlChannelPointReward
import io.rudione.chatone.data.remote.GqlChannelPointRewardsInfo
import io.rudione.chatone.data.repository.ChannelPointsGateway
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.presentation.chat.ChatState
import io.rudione.chatone.presentation.text.UiText
import io.rudione.chatone.util.Result
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChannelPointsControllerTest {

    private class FakeGateway : ChannelPointsGateway {
        override var hasRights: Boolean = true
        var contextReply: GqlChannelPointRewardsInfo? = null
        var claimReply: Result<Unit> = Result.Success(Unit)
        val claims = mutableListOf<Pair<String, String>>()

        override suspend fun context(channelLogin: String, accountToken: String) = contextReply

        override suspend fun claim(channelId: String, claimId: String, accountToken: String): Result<Unit> {
            claims += channelId to claimId
            return claimReply
        }

        override suspend fun redeem(
            channelId: String,
            reward: GqlChannelPointReward,
            textInput: String,
            accountToken: String
        ): Result<GqlChannelPointRedeemResult> = Result.Error(IllegalStateException())

        override suspend fun unlockRandomSubEmote(channelId: String, cost: Long, accountToken: String): Result<Unit> =
            Result.Error(IllegalStateException())
    }

    private val gateway = FakeGateway()
    private val errors = mutableListOf<UiText>()
    private var autoClaim = false
    private var state = ChatState(
        channelLogin = "streamer",
        channelId = "42",
        currentAccessToken = "token",
        currentUserId = "7"
    )

    private val chestShown = mutableListOf<PointsClaim>()

    private fun TestScope.controller() = ChannelPointsController(
        scope = backgroundScope,
        gateway = gateway,
        getState = { state },
        update = { state = it(state); state.pointsClaim?.let { claim -> chestShown += claim } },
        autoClaimEnabled = { autoClaim },
        onError = { errors += it },
        onNotice = { _, _ -> },
        now = { 1_000L }
    )

    private fun info(balance: Long, claimId: String? = null) = GqlChannelPointRewardsInfo(
        channelId = "42",
        balance = balance,
        rewards = emptyList(),
        availableClaimId = claimId
    )

    @Test
    fun claimForThisChannelShowsTheChest() = runTest {
        val points = controller()
        points.handle(IrcEvent.PointsClaimAvailable("42", "c1", 50))
        assertEquals(PointsClaim("c1", "42", 50), state.pointsClaim)
        assertTrue(gateway.claims.isEmpty())
    }

    @Test
    fun claimForAnotherChannelIsIgnored() = runTest {
        val points = controller()
        points.handle(IrcEvent.PointsClaimAvailable("99", "c1", 50))
        assertNull(state.pointsClaim)
    }

    @Test
    fun autoClaimTakesTheBonusRightAway() = runTest {
        autoClaim = true
        val points = controller()
        points.handle(IrcEvent.PointsClaimAvailable("42", "c1", 50))
        runCurrent()
        assertEquals(listOf("42" to "c1"), gateway.claims)
        assertNull(state.pointsClaim)
        assertEquals(false, state.pointsClaiming)
        assertTrue(chestShown.isEmpty())
    }

    @Test
    fun failedManualClaimKeepsTheChestAndReports() = runTest {
        gateway.claimReply = Result.Error(IllegalStateException("nope"))
        val points = controller()
        points.handle(IrcEvent.PointsClaimAvailable("42", "c1", 50))
        points.claim()
        runCurrent()
        assertEquals(PointsClaim("c1", "42", 50), state.pointsClaim)
        assertEquals(1, errors.size)
    }

    @Test
    fun autoClaimErrorIsShownOnlyOnce() = runTest {
        autoClaim = true
        gateway.claimReply = Result.Error(IllegalStateException("nope"))
        val points = controller()
        points.handle(IrcEvent.PointsClaimAvailable("42", "c1", 50))
        runCurrent()
        points.handle(IrcEvent.PointsClaimAvailable("42", "c2", 50))
        runCurrent()
        assertEquals(2, gateway.claims.size)
        assertEquals(1, errors.size)
        assertEquals(PointsClaim("c2", "42", 50), state.pointsClaim)
    }

    @Test
    fun earnedPointsUpdateBalanceAndPlayGain() = runTest {
        val points = controller()
        points.handle(IrcEvent.PointsEarned("42", 10, "WATCH", 1_290L))
        assertEquals(1_290L, state.pointsBalance)
        assertEquals(PointsGain(1, 10, PointsGainReason.WATCH), state.pointsGain)
        points.handle(IrcEvent.PointsEarned("42", 10, "WATCH", 1_300L))
        assertEquals(2L, state.pointsGain?.seq)
    }

    @Test
    fun earnedPointsOfAnotherChannelAreIgnored() = runTest {
        val points = controller()
        points.handle(IrcEvent.PointsEarned("99", 10, "WATCH", 5L))
        assertNull(state.pointsGain)
        assertEquals(0L, state.pointsBalance)
    }

    @Test
    fun claimCreditFromPubSubClearsTheChest() = runTest {
        val points = controller()
        points.handle(IrcEvent.PointsClaimAvailable("42", "c1", 50))
        points.handle(IrcEvent.PointsEarned("42", 50, "CLAIM", 1_340L))
        assertNull(state.pointsClaim)
        assertEquals(PointsGainReason.CLAIM, state.pointsGain?.reason)
    }

    @Test
    fun claimWithoutPubSubCreditFallsBackToBalanceDelta() = runTest {
        state = state.copy(pointsBalance = 1_000L, pointsBalanceAtMs = 500L)
        val points = controller()
        points.handle(IrcEvent.PointsClaimAvailable("42", "c1", 50))
        gateway.contextReply = info(balance = 1_050L)
        points.claim()
        runCurrent()
        assertNull(state.pointsGain)
        advanceTimeBy(ChannelPointsController.CLAIM_CREDIT_WAIT_MS + 1)
        runCurrent()
        assertEquals(1_050L, state.pointsBalance)
        assertEquals(PointsGain(1, 50, PointsGainReason.CLAIM), state.pointsGain)
    }

    @Test
    fun pubSubCreditCancelsTheFallback() = runTest {
        val points = controller()
        points.handle(IrcEvent.PointsClaimAvailable("42", "c1", 50))
        points.claim()
        runCurrent()
        points.handle(IrcEvent.PointsEarned("42", 50, "CLAIM", 1_340L))
        advanceTimeBy(ChannelPointsController.CLAIM_CREDIT_WAIT_MS + 1)
        runCurrent()
        assertEquals(1L, state.pointsGain?.seq)
    }

    @Test
    fun contextWithAvailableClaimAutoClaimsWithoutChest() = runTest {
        autoClaim = true
        gateway.contextReply = info(balance = 700L, claimId = "c9")
        val points = controller()
        points.loadContext("streamer", "42")
        runCurrent()
        assertEquals(700L, state.pointsBalance)
        assertEquals(listOf("42" to "c9"), gateway.claims)
        assertNull(state.pointsClaim)
        assertTrue(chestShown.isEmpty())
    }

    @Test
    fun claimedElsewhereHidesTheChest() = runTest {
        val points = controller()
        points.handle(IrcEvent.PointsClaimAvailable("42", "c1"))
        points.handle(IrcEvent.PointsClaimed("42", "c1"))
        assertNull(state.pointsClaim)
    }

    @Test
    fun turningAutoClaimOnTakesTheVisibleChest() = runTest {
        val points = controller()
        points.handle(IrcEvent.PointsClaimAvailable("42", "c1", 50))
        runCurrent()
        assertTrue(gateway.claims.isEmpty())
        autoClaim = true
        points.onAutoClaimSetting(true)
        runCurrent()
        assertEquals(listOf("42" to "c1"), gateway.claims)
        assertNull(state.pointsClaim)
        points.onAutoClaimSetting(true)
        runCurrent()
        assertEquals(1, gateway.claims.size)
    }
}
