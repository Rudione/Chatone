package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import io.rudione.chatone.data.repository.ThirdPartyBadge
import io.rudione.chatone.domain.model.SevenTvCosmetics
import io.rudione.chatone.domain.model.SevenTvUserCosmetic
import io.rudione.chatone.presentation.settings.InlineImageMode
import io.rudione.chatone.presentation.settings.SettingsState

val LocalNicknames = compositionLocalOf<Map<String, String>> { emptyMap() }

data class ThirdPartyBadgeMaps(
    val ffzByLogin: Map<String, List<ThirdPartyBadge>> = emptyMap(),
    val bttvByUserId: Map<String, ThirdPartyBadge> = emptyMap()
) {
    fun hasAny(login: String, userId: String): Boolean =
        ffzByLogin.containsKey(login.lowercase()) ||
            bttvByUserId.containsKey(userId)
}

val LocalThirdPartyBadges = compositionLocalOf { ThirdPartyBadgeMaps() }

val LocalChatGifsEnabled = compositionLocalOf { true }

val LocalReadableNickColors = compositionLocalOf { true }

@Immutable
data class ChatMediaSettings(
    val linkOpenMode: SettingsState.LinkOpenMode = SettingsState.LinkOpenMode.DEFAULT,
    val showInlineImages: InlineImageMode = InlineImageMode.ON,
    val inlineImageMaxHeight: Int = 200,
    val clipPreviewWidth: Int = 140
)

val LocalChatMediaSettings = compositionLocalOf { ChatMediaSettings() }

val LocalSevenTvCosmetics =
    compositionLocalOf<Map<String, SevenTvUserCosmetic>> {
        emptyMap()
    }

val LocalSevenTvPaints =
    compositionLocalOf<Map<String, SevenTvCosmetics.Paint>> {
        emptyMap()
    }
