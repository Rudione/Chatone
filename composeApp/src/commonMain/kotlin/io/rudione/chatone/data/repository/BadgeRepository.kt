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
                globalBadges[badgeSet.setId] = badgeSet.versions.associate {
                    it.id to it.imageUrl2x
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
                badgeMap[badgeSet.setId] = badgeSet.versions.associate {
                    it.id to it.imageUrl2x
                }
            }
            channelBadges[channelId] = badgeMap
            Napier.d("Channel $channelId badges loaded: ${badgeMap.size} sets", tag = TAG)
        }
    }

    fun resolveBadge(badgeId: String, version: String, channelId: String?): String {
       
        if (channelId != null) {
            val channelUrl = channelBadges[channelId]?.get(badgeId)?.get(version)
            if (channelUrl != null) return channelUrl
        }
        return globalBadges[badgeId]?.get(version) ?: ""
    }

    fun resolveBadges(badges: List<Badge>, channelId: String?): List<Badge> {
        return badges.map { badge ->
            val resolvedUrl = resolveBadge(badge.id, badge.version, channelId)
            badge.copy(imageUrl = resolvedUrl)
        }
    }
}
