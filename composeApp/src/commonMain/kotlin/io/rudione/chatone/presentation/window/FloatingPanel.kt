package io.rudione.chatone.presentation.window

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.russhwolf.settings.Settings
import org.koin.compose.koinInject

@Composable
internal fun rememberFloatingPlacement(key: String): FloatingPopupPlacement {
    val settings: Settings = koinInject()
    return remember(settings, key) { FloatingPopupPlacement(SettingsFloatingPositionStore(settings, key)) }
}

@Composable
internal fun rememberFloatingPopupPositionProvider(placement: FloatingPopupPlacement): PopupPositionProvider {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing
    val safe = SafeInsets(
        left = insets.getLeft(density, layoutDirection),
        top = insets.getTop(density),
        right = insets.getRight(density, layoutDirection),
        bottom = insets.getBottom(density)
    )
    val container = LocalWindowInfo.current.containerSize
    return remember(placement, safe, container) { FloatingPopupPositionProvider(placement, safe, container) }
}

@Composable
internal fun FloatingPanel(
    placement: FloatingPopupPlacement,
    modifier: Modifier = Modifier,
    content: @Composable (dragHandle: Modifier) -> Unit
) {
    val dragHandle = remember(placement) {
        Modifier.pointerInput(placement) {
            detectDragGestures(
                onDragEnd = placement::onDragEnd,
                onDragCancel = placement::onDragEnd
            ) { change, delta ->
                change.consume()
                placement.onDrag(delta)
            }
        }
    }
    Layout(
        content = { Box { content(dragHandle) } },
        modifier = modifier.fillMaxSize()
    ) { measurables, constraints ->
        val panel = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(constraints.maxWidth, constraints.maxHeight) {
            panel.place(
                placement.resolve(
                    anchor = IntOffset.Zero,
                    window = IntSize(constraints.maxWidth, constraints.maxHeight),
                    popup = IntSize(panel.width, panel.height),
                    safe = SafeInsets.Zero
                )
            )
        }
    }
}

@Composable
internal fun FloatingPanelGrip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(top = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
    }
}
