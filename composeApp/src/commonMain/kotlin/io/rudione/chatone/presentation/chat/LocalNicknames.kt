package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.compositionLocalOf
import io.rudione.chatone.data.repository.ThirdPartyBadge

val LocalNicknames = compositionLocalOf<Map<String, String>> { emptyMap() }

data class ThirdPartyBadgeMaps(
    val ffzByLogin: Map<String, List<ThirdPartyBadge>> = emptyMap(),
    val bttvByUserId: Map<String, ThirdPartyBadge> = emptyMap(),
    val chatoneByUserId: Map<String, List<ThirdPartyBadge>> = emptyMap()
) {
    fun hasAny(login: String, userId: String): Boolean =
        ffzByLogin.containsKey(login.lowercase()) ||
            bttvByUserId.containsKey(userId) ||
            chatoneByUserId.containsKey(userId)
}

val LocalThirdPartyBadges = compositionLocalOf { ThirdPartyBadgeMaps() }
