package io.rudione.chatone.presentation.components

import androidx.compose.runtime.compositionLocalOf
import io.rudione.chatone.data.repository.StreamerModeController

val LocalStreamerPrivacy = compositionLocalOf {
    StreamerModeController.State(enabled = false, options = StreamerModeController.Options())
}
