package io.rudione.chatone.presentation.profile.editor

import io.rudione.chatone.domain.profile.StreamCategory
import io.rudione.chatone.domain.profile.VanityChange
import androidx.compose.runtime.Immutable
import io.rudione.chatone.base.UiState
import io.rudione.chatone.domain.profile.SevenTvLink
import io.rudione.chatone.domain.profile.SevenTvVanity
import io.rudione.chatone.domain.profile.TwitchVanity
import io.rudione.chatone.domain.profile.VanityDraft
import io.rudione.chatone.domain.profile.VanitySaveResult

enum class EditorLoad { LOADING, READY, SIGNED_OUT, FAILED }

@Immutable
data class ProfileEditorState(
    val load: EditorLoad = EditorLoad.LOADING,
    val twitch: TwitchVanity? = null,
    val sevenTv: SevenTvLink = SevenTvLink.NotConnected,
    val sevenTvBusy: Boolean = false,
    val draft: VanityDraft? = null,
    val saving: Boolean = false,
    val result: VanitySaveResult? = null,
    val pendingChanges: List<VanityChange>? = null,
    val categoryResults: List<StreamCategory> = emptyList()
) : UiState {
    val sevenTvVanity: SevenTvVanity? get() = (sevenTv as? SevenTvLink.Connected)?.vanity

    val dirty: Boolean
        get() {
            val base = twitch ?: return false
            val current = draft ?: return false
            return current != draftOf(base, sevenTvVanity)
        }
}
