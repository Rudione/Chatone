package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.remote.dto.BadgeSetDto
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.domain.model.SubscriptionTier
import io.rudione.chatone.util.Result
import io.rudione.chatone.data.remote.BadgeImageSets
import io.rudione.chatone.data.remote.TwitchBadgeGqlClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.concurrent.Volatile

class BadgeRepository(
    private val apiClient: TwitchApiClient,
    private val gqlBadges: TwitchBadgeGqlClient
) {
    companion object {
        private const val TAG = "BadgeRepository"
        private const val MAX_ATTEMPTS = 3
        private const val RETRY_BASE_DELAY_MS = 2_000L
    }

    @Volatile
    private var globalBadges: BadgeImageSets = emptyMap()

    @Volatile
    private var channelBadges: Map<String, BadgeImageSets> = emptyMap()

    private val globalLock = Mutex()
    private val channelLock = Mutex()

    suspend fun loadGlobalBadges(accessToken: String) {
        if (globalBadges.isNotEmpty()) return
        globalLock.withLock {
            if (globalBadges.isNotEmpty()) return
            val loaded = withRetries { helixGlobal(accessToken) ?: gqlBadges.globalBadges()?.takeIf { it.isNotEmpty() } }
            if (loaded != null) {
                globalBadges = loaded
                Napier.d("Global badges loaded: ${loaded.size} sets", tag = TAG)
            }
        }
    }

    suspend fun loadChannelBadges(channelId: String, accessToken: String) {
        if (channelId.isEmpty() || channelBadges.containsKey(channelId)) return
        channelLock.withLock {
            if (channelBadges.containsKey(channelId)) return
            val loaded = withRetries { helixChannel(channelId, accessToken) ?: gqlBadges.channelBadges(channelId) }
            if (loaded != null) {
                channelBadges = channelBadges + (channelId to loaded)
                Napier.d("Channel $channelId badges loaded: ${loaded.size} sets", tag = TAG)
            }
        }
    }

    private suspend fun helixGlobal(accessToken: String): BadgeImageSets? {
        if (accessToken.isEmpty()) return null
        val result = apiClient.getGlobalBadges(accessToken) as? Result.Success ?: return null
        return result.data.data.toImageSets().takeIf { it.isNotEmpty() }
    }

    private suspend fun helixChannel(channelId: String, accessToken: String): BadgeImageSets? {
        if (accessToken.isEmpty()) return null
        val result = apiClient.getChannelBadges(channelId, accessToken) as? Result.Success ?: return null
        return result.data.data.toImageSets()
    }

    private fun List<BadgeSetDto>.toImageSets(): BadgeImageSets = associate { badgeSet ->
        badgeSet.setId.lowercase() to badgeSet.versions.associate { it.id.lowercase() to it.imageUrl2x }
    }

    private suspend fun <T : Any> withRetries(block: suspend () -> T?): T? {
        repeat(MAX_ATTEMPTS) { attempt ->
            block()?.let { return it }
            if (attempt < MAX_ATTEMPTS - 1) delay(RETRY_BASE_DELAY_MS shl attempt)
        }
        return null
    }

    fun resolveBadge(badgeId: String, version: String, channelId: String?): String {
        val id = badgeId.lowercase()
        val ver = version.lowercase()
        if (channelId != null) {
            val channelUrl = channelBadges[channelId]?.get(id)?.get(ver)
            if (channelUrl != null) return channelUrl
        }
        return globalBadges[id]?.get(ver) ?: ""
    }

    fun resolveBadges(rawBadges: List<Badge>, channelId: String?): List<Badge> {
        return rawBadges.map { raw ->
            val id = raw.id.lowercase()
            val ver = raw.version.lowercase()

            val channelUrl = if (channelId != null) {
                channelBadges[channelId]?.get(id)?.get(ver)
            } else null

            if (channelUrl != null) {
                raw.copy(
                    imageUrl = channelUrl,
                    tooltip = buildTooltip(id, ver, raw.months),
                    setId = id,
                    isGlobal = false
                )
            } else {
                val globalUrl = globalBadges[id]?.get(ver)
                if (globalUrl != null) {
                    raw.copy(
                        imageUrl = globalUrl,
                        tooltip = buildTooltip(id, ver, raw.months),
                        setId = id,
                        isGlobal = true
                    )
                } else {
                    val defaultIcon = getDefaultBadgeIcon(id)
                    raw.copy(
                        imageUrl = defaultIcon ?: "",
                        tooltip = raw.tooltip.ifEmpty { buildTooltip(id, ver, raw.months) },
                        isGlobal = defaultIcon != null
                    )
                }
            }
        }
    }

    private fun subscriptionTooltip(label: String, version: String, months: Int?): String {
        val tier = SubscriptionTier.fromBadgeVersion(version)
        val head = "$label · Tier ${tier.level}"
        return if (months != null && months > 0) "$head · $months mo" else head
    }

    private fun buildTooltip(badgeId: String, version: String, months: Int?): String {
        return when (badgeId.lowercase()) {
            "subscriber" -> subscriptionTooltip("Subscriber", version, months)
            "founder" -> subscriptionTooltip("Founder", version, months)
            "vip" -> "VIP"
            "moderator" -> "Moderator"
            "grand_moderator", "chat_manager", "super_moderator" -> "Grand Moderator"
            "broadcaster" -> "Broadcaster"
            "bits" -> "Bits: $version"
            "sub-gifter" -> "Sub Gifter: $version"
            "predictions-blue", "predictions-pink" -> "Predictions"
            "hype-train" -> "Hype Train"
            else -> badgeId.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }

    private fun getDefaultBadgeIcon(badgeId: String): String? {
        return when (badgeId.lowercase()) {
            "broadcaster" -> "https://static-cdn.jtvnw.net/badges/v1/5527c58c-fb7d-422d-b71b-f309dcb85cc1/3"
            "moderator" -> "https://static-cdn.jtvnw.net/badges/v1/3267646d-33f0-4b17-b3df-f923a41db1d0/3"
            "vip" -> "https://static-cdn.jtvnw.net/badges/v1/b817aba4-fad8-49e2-b88a-7cc744dfa6ec/3"
            else -> null
        }
    }
}
