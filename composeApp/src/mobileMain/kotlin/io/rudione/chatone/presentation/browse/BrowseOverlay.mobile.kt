package io.rudione.chatone.presentation.browse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import io.rudione.chatone.data.repository.AiAssistantController
import org.koin.compose.koinInject

private val OverlayExitEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private const val OVERLAY_Z_INDEX = 60f

@Composable
actual fun BrowseOverlay(
    visible: Boolean,
    onClose: () -> Unit,
    onOpen: (BrowseOpenRequest) -> Unit
) {
    val assistant: AiAssistantController = koinInject()
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) { it / 6 } + fadeIn(tween(220)),
        exit = slideOutVertically(tween(200, easing = OverlayExitEasing)) { it / 8 } +
                fadeOut(tween(160, easing = OverlayExitEasing)),
        modifier = Modifier.fillMaxSize().zIndex(OVERLAY_Z_INDEX)
    ) {
        BrowseScreen(
            onClose = onClose,
            onOpenStream = { stream ->
                onOpen(
                    BrowseOpenRequest(
                        stream.login,
                        stream.displayName,
                        stream.avatarUrl,
                        live = true
                    )
                )
            },
            onOpenChannel = { channel ->
                onOpen(
                    BrowseOpenRequest(
                        channel.login,
                        channel.displayName,
                        channel.avatarUrl,
                        channel.isLive
                    )
                )
            },
            onAskAi = assistant::openWithPrompt
        )
    }
}
