package io.rudione.chatone.presentation.media

import io.rudione.chatone.base.UIEffect

sealed interface MediaViewerEffect : UIEffect {
    data object Deleted : MediaViewerEffect
    data object Banned : MediaViewerEffect
    data object ActionFailed : MediaViewerEffect
}
