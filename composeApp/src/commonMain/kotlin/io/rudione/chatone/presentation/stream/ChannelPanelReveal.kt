package io.rudione.chatone.presentation.stream

import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import io.rudione.chatone.presentation.stream.ambient.ambientGlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private const val GLOW_HANDOFF_MS = 320

@Stable
internal class ChannelPanelRevealState(
    val login: String?,
    val visible: Boolean,
    private val progressState: State<Float>
) {
    val progress: Float get() = progressState.value
}

@Composable
internal fun rememberChannelPanelReveal(ui: MobileStreamUi, enabled: Boolean): ChannelPanelRevealState {
    val host = ui.host
    val forced by remember(host) {
        host.state.map { it.forcesControls }.distinctUntilChanged()
    }.collectAsState(host.state.value.forcesControls)
    val login by remember(host) {
        host.state.map { it.channelLogin }.distinctUntilChanged()
    }.collectAsState(host.state.value.channelLogin)
    val playerOnScreen = !ui.inPictureInPicture && !ui.floating.isShowing
    val visible = enabled && login != null && playerOnScreen && (ui.chrome.controlsVisible || forced)
    val progress = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(GLOW_HANDOFF_MS),
        label = "channelPanelGlow"
    )
    return remember(login, visible, progress) { ChannelPanelRevealState(login, visible, progress) }
}

@Composable
internal fun ChannelPanelReveal(
    ui: MobileStreamUi,
    reveal: ChannelPanelRevealState,
    glowing: Boolean,
    panel: @Composable (login: String, modifier: Modifier) -> Unit
) {
    val login = reveal.login ?: return
    val chrome = ui.chrome
    val darkBackground = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val surface = MaterialTheme.colorScheme.surfaceContainerLow
    AnimatedVisibility(
        visible = reveal.visible,
        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow), expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(spring(stiffness = Spring.StiffnessMedium), shrinkTowards = Alignment.Top) + fadeOut()
    ) {
        val background = Modifier.background(surface)
        val glow = if (glowing) {
            Modifier.ambientGlow(ui.ambience, darkBackground, alpha = { reveal.progress })
        } else {
            Modifier
        }
        CompositionLocalProvider(LocalStreamChrome provides chrome) {
            panel(
                login,
                background
                    .then(glow)
                    .pointerInput(chrome) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            chrome.touch()
                        }
                    }
            )
        }
    }
}
