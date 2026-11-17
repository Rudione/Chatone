package io.rudione.chatone.presentation.media

import androidx.compose.runtime.Immutable
import io.rudione.chatone.base.UiState
import io.rudione.chatone.domain.media.MediaDetails
import io.rudione.chatone.domain.model.DisplayMessage

enum class ViewerLoad { LOADING, READY, MISSING, FAILED }

@Immutable
data class ReplayEntry(val revealAtMs: Long, val message: DisplayMessage.PrivMsg)

@Immutable
data class MediaViewerState(
    val load: ViewerLoad = ViewerLoad.LOADING,
    val details: MediaDetails? = null,
    val sourceIndex: Int = 0,
    val muted: Boolean = false,
    val volume: Float = 1f,
    val speed: Float = 1f,
    val looping: Boolean = true,
    val replayMessages: List<ReplayEntry> = emptyList(),
    val replayPrimed: Boolean = false,
    val replayHasClock: Boolean = false,
    val ambientLight: Boolean = true,
    val canModerate: Boolean = false,
    val busy: Boolean = false,
    val deleted: Boolean = false
) : UiState
