package io.rudione.chatone.presentation.profile.editor

import io.rudione.chatone.base.UIEffect
import io.rudione.chatone.domain.profile.VanitySaveResult

sealed interface ProfileEditorEffect : UIEffect {
    data class Saved(val result: VanitySaveResult) : ProfileEditorEffect
}
