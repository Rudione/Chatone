package io.rudione.chatone.presentation.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

internal object NativeVideoPresence {
    private var surfaces = 0

    val isOnScreen: Boolean get() = surfaces > 0

    fun acquire() {
        surfaces++
    }

    fun release() {
        if (surfaces > 0) surfaces--
    }
}

@Composable
internal fun NativeVideoPresenceEffect() {
    DisposableEffect(Unit) {
        NativeVideoPresence.acquire()
        onDispose { NativeVideoPresence.release() }
    }
}
