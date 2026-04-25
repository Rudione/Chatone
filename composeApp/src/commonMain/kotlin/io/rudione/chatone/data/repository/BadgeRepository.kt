package io.rudione.chatone.data.repository

import io.github.aakira.napier.Napier
import io.rudione.chatone.data.remote.TwitchApiClient
import io.rudione.chatone.data.remote.dto.BadgeSetDto
import io.rudione.chatone.domain.model.Badge
import io.rudione.chatone.util.Result
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class BadgeRepository(
    private val apiClient: TwitchApiClient
) {
    companion object {
        private const val TAG = "BadgeRepository"
    }


    private val globalBadges = mutableMapOf<String, Map<String, String>>()
    private val channelBadges = mutableMapOf<String, Map<String, Map<String, String>>>()

    private var globalLoaded = false

    suspend fun loadGlobalBadges(accessToken: String) {
        if (globalLoaded) return
        globalLoaded = true

        val result = apiClient.getGlobalBadges(accessToken)
        if (result is Result.Success) {
            result.data.data.forEach { badgeSet ->
               
                globalBadges[badgeSet.setId.lowercase()] = badgeSet.versions.associate {
                    it.id.lowercase() to it.imageUrl2x
                }
            }
            Napier.d("Global badges loaded: ${globalBadges.size} sets", tag = TAG)
        }
    }

    suspend fun loadChannelBadges(channelId: String, accessToken: String) {
        val result = apiClient.getChannelBadges(channelId, accessToken)
        if (result is Result.Success) {
            val badgeMap = mutableMapOf<String, Map<String, String>>()
            result.data.data.forEach { badgeSet ->
               
                badgeMap[badgeSet.setId.lowercase()] = badgeSet.versions.associate {
                    it.id.lowercase() to it.imageUrl2x
                }
            }
            channelBadges[channelId] = badgeMap
            Napier.d("Channel $channelId badges loaded: ${badgeMap.size} sets", tag = TAG)
        }
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

   
    private fun buildTooltip(badgeId: String, version: String, months: Int?): String {
        return when (badgeId.lowercase()) {
            "subscriber" -> if (months != null) "Subscriber for $months months" else "Subscriber"
            "founder" -> if (months != null) "Founder for $months months" else "Founder"
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
            "subscriber" -> "https://static-cdn.jtvnw.net/badges/v1/subscriber/3"
            "founder" -> "https://static-cdn.jtvnw.net/badges/v1/founder/3"
            "bits" -> "https://static-cdn.jtvnw.net/badges/v1/bits/3"
            "sub-gifter" -> "https://static-cdn.jtvnw.net/badges/v1/sub-gifter/3"
            "predictions-blue" -> "https://static-cdn.jtvnw.net/badges/v1/predictions-blue/3"
            "predictions-pink" -> "https://static-cdn.jtvnw.net/badges/v1/predictions-pink/3"
            "hype-train" -> "https://static-cdn.jtvnw.net/badges/v1/hype-train/3"
            "prime" -> "https://static-cdn.jtvnw.net/badges/v1/prime/1"
            "turbo" -> "https://static-cdn.jtvnw.net/badges/v1/turbo/1"
            "premium" -> "https://static-cdn.jtvnw.net/badges/v1/premium/1"
            else -> null
        }
    }
}