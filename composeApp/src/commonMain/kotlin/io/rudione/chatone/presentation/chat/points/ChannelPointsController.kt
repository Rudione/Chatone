package io.rudione.chatone.presentation.chat.points

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.actions_auto_claim_failed
import chatone.composeapp.generated.resources.chat_vm_failed_to_load_points
import chatone.composeapp.generated.resources.chat_vm_failed_to_redeem
import chatone.composeapp.generated.resources.chat_vm_gql_token_required
import chatone.composeapp.generated.resources.chat_vm_redeemed
import chatone.composeapp.generated.resources.points_claim_failed
import io.rudione.chatone.data.remote.GqlChannelPointReward
import io.rudione.chatone.data.remote.GqlChannelPointRewardsInfo
import io.rudione.chatone.data.remote.TwitchAutomaticRewardType
import io.rudione.chatone.data.repository.ChannelPointsGateway
import io.rudione.chatone.domain.model.IrcEvent
import io.rudione.chatone.presentation.chat.ChatState
import io.rudione.chatone.presentation.text.UiText
import io.rudione.chatone.presentation.text.uiText
import io.rudione.chatone.util.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock

internal class ChannelPointsController(
    private val scope: CoroutineScope,
    private val gateway: ChannelPointsGateway,
    private val getState: () -> ChatState,
    private val update: ((ChatState) -> ChatState) -> Unit,
    private val autoClaimEnabled: () -> Boolean,
    private val onError: (UiText) -> Unit,
    private val onNotice: (channelLogin: String, text: UiText) -> Unit,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val creditWaitMs: Long = CLAIM_CREDIT_WAIT_MS
) {
    private var balanceJob: Job? = null
    private var claimJob: Job? = null
    private var awaitingCredit: String? = null
    private var autoClaimErrorShown = false
    private var autoClaimWasOn = autoClaimEnabled()
    private var gainSeq = 0L

    fun reset() {
        balanceJob?.cancel()
        claimJob?.cancel()
        awaitingCredit = null
    }

    fun handle(event: IrcEvent): Boolean {
        when (event) {
            is IrcEvent.PointsClaimAvailable -> onClaimAvailable(event)
            is IrcEvent.PointsClaimed -> onClaimed(event)
            is IrcEvent.PointsEarned -> onEarned(event)
            is IrcEvent.PointsBalanceChanged -> onBalance(event)
            else -> return false
        }
        return true
    }

    fun loadContext(channelLogin: String, channelId: String) {
        if (!gateway.hasRights) return
        val token = getState().currentAccessToken
        scope.launch {
            val info = gateway.context(channelLogin, token) ?: return@launch
            applyContext(channelId, info)
        }
    }

    fun refreshIfStale() {
        val s = getState()
        if (s.channelLogin.isEmpty() || balanceJob?.isActive == true) return
        if (now() - s.pointsBalanceAtMs < BALANCE_FRESH_MS) return
        if (!gateway.hasRights) return
        val channelLogin = s.channelLogin
        balanceJob = scope.launch {
            val info = gateway.context(channelLogin, s.currentAccessToken) ?: return@launch
            if (!getState().channelLogin.equals(channelLogin, ignoreCase = true)) return@launch
            applyContext(getState().channelId, info, keepRewards = true)
        }
    }

    fun openPanel() {
        val s = getState()
        update { it.copy(showPointsBitsPanel = true, pointsBitsLoading = true, pointsBitsError = null) }
        if (!gateway.hasRights) {
            update { it.copy(pointsBitsLoading = false, pointsBitsError = uiText(Res.string.chat_vm_gql_token_required)) }
            return
        }
        scope.launch {
            val info = gateway.context(s.channelLogin, s.currentAccessToken)
            if (info == null) {
                update { it.copy(pointsBitsLoading = false, pointsBitsError = uiText(Res.string.chat_vm_failed_to_load_points)) }
            } else {
                update { it.copy(pointsBitsLoading = false) }
                applyContext(s.channelId, info)
            }
        }
    }

    fun closePanel() {
        update { it.copy(showPointsBitsPanel = false) }
    }

    fun redeem(reward: GqlChannelPointReward, textInput: String) {
        val s = getState()
        if (!reward.isRedeemableInApp) return
        if (!gateway.hasRights) return onError(uiText(Res.string.chat_vm_gql_token_required))
        scope.launch {
            val automatic = reward.automaticType
            if (automatic != null) {
                redeemAutomatic(s, reward, automatic)
                return@launch
            }
            when (val r = gateway.redeem(s.channelId, reward, textInput, s.currentAccessToken)) {
                is Result.Success -> {
                    update { it.copy(pointsBalance = r.data.balance, pointsBalanceAtMs = now()) }
                    onNotice(s.channelLogin, uiText(Res.string.chat_vm_redeemed, reward.title))
                }
                is Result.Error -> onError(uiText(Res.string.chat_vm_failed_to_redeem, r.exception.message))
                else -> {}
            }
        }
    }

    fun onAutoClaimSetting(enabled: Boolean) {
        val turnedOn = enabled && !autoClaimWasOn
        autoClaimWasOn = enabled
        if (turnedOn) getState().pointsClaim?.let { performClaim(it, auto = true) }
    }

    fun claim() {
        val claim = getState().pointsClaim ?: return
        performClaim(claim, auto = false)
    }

    private suspend fun redeemAutomatic(
        s: ChatState,
        reward: GqlChannelPointReward,
        type: TwitchAutomaticRewardType
    ) {
        val result = when (type) {
            TwitchAutomaticRewardType.RANDOM_SUB_EMOTE_UNLOCK ->
                gateway.unlockRandomSubEmote(s.channelId, reward.cost, s.currentAccessToken)
            else -> return
        }
        when (result) {
            is Result.Success -> {
                update { it.copy(pointsBalance = (it.pointsBalance - reward.cost).coerceAtLeast(0L)) }
                if (getState().showPointsBitsPanel) {
                    gateway.context(s.channelLogin, s.currentAccessToken)?.let { applyContext(s.channelId, it) }
                }
            }
            is Result.Error -> onError(uiText(Res.string.chat_vm_failed_to_redeem, result.exception.message))
            else -> {}
        }
    }

    private fun applyContext(channelId: String, info: GqlChannelPointRewardsInfo, keepRewards: Boolean = false) {
        val claimId = info.availableClaimId?.takeIf { it != awaitingCredit }
        val silent = claimId != null && autoClaimEnabled()
        update {
            if (it.channelId != channelId) return@update it
            it.copy(
                pointsBalance = info.balance,
                pointsBalanceAtMs = now(),
                pointsIconUrl = info.currencyIconUrl ?: it.pointsIconUrl,
                channelRewards = if (keepRewards && info.rewards.isEmpty()) it.channelRewards else info.rewards,
                pointsClaim = when {
                    claimId == null || silent -> null
                    it.pointsClaim?.id == claimId -> it.pointsClaim
                    else -> PointsClaim(claimId, channelId)
                }
            )
        }
        if (silent && claimId != null) performClaim(PointsClaim(claimId, channelId), auto = true)
    }

    private fun onClaimAvailable(event: IrcEvent.PointsClaimAvailable) {
        if (event.channelId != getState().channelId) return
        val claim = PointsClaim(event.claimId, event.channelId, event.points)
        if (autoClaimEnabled()) {
            performClaim(claim, auto = true)
        } else {
            update { it.copy(pointsClaim = claim) }
        }
    }

    private fun onClaimed(event: IrcEvent.PointsClaimed) {
        update {
            if (it.pointsClaim?.id == event.claimId) it.copy(pointsClaim = null, pointsClaiming = false) else it
        }
    }

    private fun onEarned(event: IrcEvent.PointsEarned) {
        if (event.channelId != getState().channelId || event.amount <= 0) return
        val reason = PointsGainReason.fromTwitch(event.reason)
        if (reason == PointsGainReason.CLAIM) awaitingCredit = null
        val gain = PointsGain(++gainSeq, event.amount, reason)
        update {
            it.copy(
                pointsBalance = event.balance ?: it.pointsBalance,
                pointsBalanceAtMs = if (event.balance != null) now() else it.pointsBalanceAtMs,
                pointsGain = gain,
                pointsClaim = if (reason == PointsGainReason.CLAIM) null else it.pointsClaim
            )
        }
    }

    private fun onBalance(event: IrcEvent.PointsBalanceChanged) {
        update {
            if (it.channelId != event.channelId) it
            else it.copy(pointsBalance = event.balance, pointsBalanceAtMs = now())
        }
    }

    private fun performClaim(claim: PointsClaim, auto: Boolean) {
        val s = getState()
        if (claimJob?.isActive == true || s.currentAccessToken.isEmpty()) {
            if (auto) showChest(claim)
            return
        }
        if (!auto) update { it.copy(pointsClaiming = true) }
        claimJob = scope.launch {
            when (val r = gateway.claim(claim.channelId, claim.id, s.currentAccessToken)) {
                is Result.Success -> {
                    awaitingCredit = claim.id
                    update {
                        if (it.pointsClaim?.id == claim.id) it.copy(pointsClaim = null, pointsClaiming = false)
                        else it.copy(pointsClaiming = false)
                    }
                    creditFallback(claim, s.channelLogin, s.pointsBalance, s.pointsBalanceAtMs > 0L)
                }
                is Result.Error -> {
                    update { it.copy(pointsClaiming = false) }
                    if (!auto) {
                        onError(uiText(Res.string.points_claim_failed))
                    } else {
                        showChest(claim)
                        if (!autoClaimErrorShown) {
                            autoClaimErrorShown = true
                            onError(uiText(Res.string.actions_auto_claim_failed))
                        }
                    }
                }
                else -> update { it.copy(pointsClaiming = false) }
            }
        }
    }

    private fun showChest(claim: PointsClaim) {
        update { if (it.channelId == claim.channelId) it.copy(pointsClaim = claim) else it }
    }

    private suspend fun creditFallback(claim: PointsClaim, channelLogin: String, before: Long, knewBalance: Boolean) {
        delay(creditWaitMs)
        if (awaitingCredit != claim.id) return
        awaitingCredit = null
        if (getState().channelId != claim.channelId) return
        val info = if (gateway.hasRights) gateway.context(channelLogin, getState().currentAccessToken) else null
        val amount = when {
            info != null && knewBalance && info.balance > before -> (info.balance - before).toInt()
            claim.points > 0 -> claim.points
            else -> 0
        }
        val gain = PointsGain(++gainSeq, amount, PointsGainReason.CLAIM).takeIf { amount > 0 }
        update {
            if (it.channelId != claim.channelId) return@update it
            val balance = when {
                info != null -> info.balance
                knewBalance && gain != null -> it.pointsBalance + gain.amount
                else -> it.pointsBalance
            }
            it.copy(
                pointsBalance = balance,
                pointsBalanceAtMs = if (info != null) now() else it.pointsBalanceAtMs,
                pointsGain = gain ?: it.pointsGain
            )
        }
    }

    companion object {
        const val BALANCE_FRESH_MS = 30_000L
        const val CLAIM_CREDIT_WAIT_MS = 2_500L
    }
}
