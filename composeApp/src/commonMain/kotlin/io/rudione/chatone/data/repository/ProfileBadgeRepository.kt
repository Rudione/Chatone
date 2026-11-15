package io.rudione.chatone.data.repository

import io.rudione.chatone.data.remote.AccountBadge
import io.rudione.chatone.data.remote.BadgeHqClient
import io.rudione.chatone.data.remote.CommunityBadgeClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart

class ProfileBadgeRepository(private val badgeHq: BadgeHqClient, private val community: CommunityBadgeClient) {
    fun badgesFor(userId: String, login: String): Flow<List<AccountBadge>> = combine(
        flow { emit(community.badgesFor(userId)) }.onStart { emit(emptyList()) },
        flow { emit(badgeHq.accountBadges(login).orEmpty()) }.onStart { emit(emptyList()) }
    ) { direct, aggregate ->
        (direct + aggregate).distinctBy {
            if (it.key.startsWith("7tv_") || it.key.startsWith("tw_")) it.key else it.imageUrl
        }
    }.distinctUntilChanged()
}
