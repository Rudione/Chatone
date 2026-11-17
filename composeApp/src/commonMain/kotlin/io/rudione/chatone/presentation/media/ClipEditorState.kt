package io.rudione.chatone.presentation.media

import androidx.compose.runtime.Immutable
import io.rudione.chatone.base.UiState
import io.rudione.chatone.domain.media.ClipDraft
import io.rudione.chatone.domain.media.ClipFailure
import io.rudione.chatone.domain.media.ClipTrim

enum class ClipEditorPhase { PREPARING, EDITING, PUBLISHING, DONE, FAILED }

@Immutable
data class ClipEditorState(
    val login: String,
    val channelName: String = "",
    val phase: ClipEditorPhase = ClipEditorPhase.PREPARING,
    val failure: ClipFailure? = null,
    val draft: ClipDraft? = null,
    val totalSeconds: Float = FALLBACK_WINDOW,
    val start: Float = FALLBACK_WINDOW - ClipTrim.DEFAULT_SECONDS,
    val end: Float = FALLBACK_WINDOW,
    val rangeEdited: Boolean = false,
    val trimming: Boolean = false,
    val title: String = "",
    val slug: String? = null,
    val publishedSlug: String? = null
) : UiState {
    val length: Float get() = end - start
    val range: ClosedFloatingPointRange<Float> get() = start..end
    val canPublish: Boolean get() = phase == ClipEditorPhase.EDITING && title.isNotBlank() && slug != null

    companion object {
        const val FALLBACK_WINDOW = 60f
    }
}
