package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_click_to_reveal
import coil3.compose.AsyncImage
import io.rudione.chatone.presentation.components.ImagePreviewExpandButton
import io.rudione.chatone.presentation.components.LocalImagePreviewController
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ExpandableImagePreview(
    imageUrl: String,
    description: String,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    obscured: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val controller = LocalImagePreviewController.current
    var painter by remember(imageUrl) { mutableStateOf<Painter?>(null) }
    val coordinates = remember(imageUrl) { PreviewCoordinates() }
    val hidden = controller?.isSource(coordinates.value) == true
    val open = {
        painter?.let { controller?.open(imageUrl, description, it, coordinates.value) }
        Unit
    }
    Box(
        modifier.graphicsLayer { alpha = if (hidden) 0f else 1f }.clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = description,
            modifier = imageModifier.onGloballyPositioned { coordinates.value = it }
                .then(if (obscured) Modifier.blur(20.dp) else Modifier),
            contentScale = contentScale,
            onSuccess = { painter = it.painter },
            onError = { painter = null }
        )
        if (obscured) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(Res.string.chat_click_to_reveal),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White
                )
            }
        } else if (controller != null && painter != null) {
            ImagePreviewExpandButton(open, Modifier.align(Alignment.TopEnd).padding(4.dp))
        }
    }
}

private class PreviewCoordinates {
    var value: LayoutCoordinates? = null
}
