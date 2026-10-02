package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
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

private class EmptyMapState<V> : State<Map<String, V>> {
    override val value: Map<String, V> = emptyMap()
}

val LocalSevenTvCosmetics =
    staticCompositionLocalOf<State<Map<String, SevenTvUserCosmetic>>> { EmptyMapState() }

val LocalSevenTvPaints =
    staticCompositionLocalOf<State<Map<String, SevenTvCosmetics.Paint>>> { EmptyMapState() }
