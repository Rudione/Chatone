package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.compositionLocalOf
import io.rudione.chatone.data.repository.ThirdPartyBadge

/**
 * userId → local nickname override (moltorino-style Nicknames). Provided at App root from
 * [io.rudione.chatone.data.repository.NicknameRepository]; empty when no overrides exist.
 * Renderers resolve names as `LocalNicknames.current[userId] ?: displayName`.
 */
val LocalNicknames = compositionLocalOf<Map<String, String>> { emptyMap() }

/** Global FFZ (keyed by login) / BTTV (keyed by Twitch user id) badges, provided at App root. */
data class ThirdPartyBadgeMaps(
    val ffzByLogin: Map<String, List<ThirdPartyBadge>> = emptyMap(),
    val bttvByUserId: Map<String, ThirdPartyBadge> = emptyMap()
) {
    fun hasAny(login: String, userId: String): Boolean =
        ffzByLogin.containsKey(login.lowercase()) || bttvByUserId.containsKey(userId)
}

val LocalThirdPartyBadges = compositionLocalOf { ThirdPartyBadgeMaps() }
