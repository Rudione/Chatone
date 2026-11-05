package io.rudione.chatone.tv.components

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties

private const val CLAIM_FRAMES = 8

internal fun Modifier.tvFocusTrap(): Modifier = focusProperties {
    onExit = { if (requestedFocusDirection != FocusDirection.Enter) cancelFocusChange() }
}

internal suspend fun FocusRequester.claim() {
    repeat(CLAIM_FRAMES) {
        withFrameNanos { }
        if (runCatching { requestFocus() }.getOrDefault(false)) return
    }
}
