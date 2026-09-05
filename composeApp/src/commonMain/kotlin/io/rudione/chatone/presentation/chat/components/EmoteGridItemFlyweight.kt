package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.domain.model.GenericEmote
import io.rudione.chatone.presentation.chat.AnimatedEmoteImage
import io.rudione.chatone.presentation.chat.models.EmoteUiData

private const val LOCKED_ALPHA = 0.35f

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmoteGridItemFlyweight(
    uiData: EmoteUiData,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val interactionSource = remember(uiData.listKey) { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                enabled = !uiData.isLocked,
                onClick = onClick,
                onLongClick = onToggleFavorite,
                indication = if (LocalInspectionMode.current) null else LocalIndication.current,
                interactionSource = interactionSource
            )
            .pointerInput(onToggleFavorite, uiData.isLocked) {
                if (uiData.isLocked) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.buttons.isSecondaryPressed) {
                            onToggleFavorite()
                        }
                    }
                }
            }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            AnimatedEmoteImage(
                url = uiData.imageUrl,
                contentDescription = uiData.displayCode,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .alpha(if (uiData.isLocked) LOCKED_ALPHA else 1f),
            )

            if (uiData.isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(9.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiData.isFavorite) {
                Text(
                    text = "★",
                    fontSize = 10.sp,
                    color = Color(0xFFFFD700),
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)
                )
            }
        }
        Text(
            text = uiData.displayCode,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
                .copy(alpha = if (uiData.isLocked) 0.4f else 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
        )
    }
}
