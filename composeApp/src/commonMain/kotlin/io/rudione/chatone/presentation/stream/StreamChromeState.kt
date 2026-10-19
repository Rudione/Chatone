package io.rudione.chatone.presentation.stream

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
class StreamChromeState {
    var controlsVisible by mutableStateOf(true)
        private set
    var interactions by mutableIntStateOf(0)
        private set
    private var holds by mutableIntStateOf(0)

    val held: Boolean get() = holds > 0

    fun toggle() {
        if (held) return
        controlsVisible = !controlsVisible
        interactions++
    }

    fun touch() {
        interactions++
    }

    fun hide() {
        if (!held) controlsVisible = false
    }

    fun hold() {
        holds++
        controlsVisible = true
    }

    fun release() {
        if (holds == 0) return
        holds--
        interactions++
    }
}

val LocalStreamChrome = staticCompositionLocalOf<StreamChromeState?> { null }

@Composable
fun KeepStreamControlsShown() {
    val chrome = LocalStreamChrome.current ?: return
    DisposableEffect(chrome) {
        chrome.hold()
        onDispose { chrome.release() }
    }
}
