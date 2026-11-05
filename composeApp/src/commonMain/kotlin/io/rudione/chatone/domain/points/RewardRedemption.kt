package io.rudione.chatone.domain.points

import androidx.compose.runtime.Immutable

@Immutable
data class RewardRedemption(
    val id: String,
    val channelId: String,
    val rewardId: String,
    val rewardTitle: String,
    val cost: Long,
    val userId: String,
    val login: String,
    val displayName: String,
    val userInput: String?,
    val redeemedAtMs: Long,
    val imageUrl: String? = null,
    val backgroundColor: Long? = null
) {
    val carriesChatMessage: Boolean get() = !userInput.isNullOrBlank()
}

sealed interface RedemptionRoute {
    data class Show(val redemption: RewardRedemption) : RedemptionRoute
    data class TitleOnly(val rewardId: String, val title: String) : RedemptionRoute
    data object Skip : RedemptionRoute
}

class RewardRedemptionRouter(private val memory: Int = 256) {
    private val seen = LinkedHashSet<String>()

    fun route(redemption: RewardRedemption): RedemptionRoute {
        if (redemption.id.isBlank() || !remember(redemption.id)) return RedemptionRoute.Skip
        return if (redemption.carriesChatMessage) {
            if (redemption.rewardTitle.isBlank()) RedemptionRoute.Skip
            else RedemptionRoute.TitleOnly(redemption.rewardId, redemption.rewardTitle)
        } else {
            RedemptionRoute.Show(redemption)
        }
    }

    fun reset() = seen.clear()

    private fun remember(id: String): Boolean {
        if (!seen.add(id)) return false
        if (seen.size > memory) seen.remove(seen.first())
        return true
    }
}
