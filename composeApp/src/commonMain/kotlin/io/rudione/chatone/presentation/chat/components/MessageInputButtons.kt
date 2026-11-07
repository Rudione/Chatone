package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_send
import chatone.composeapp.generated.resources.ui_emotes
import chatone.composeapp.generated.resources.uploader_copy_link
import chatone.composeapp.generated.resources.uploader_link_copied
import chatone.composeapp.generated.resources.uploader_uploading
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.Link
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.SendHorizontal
import io.rudione.chatone.icons.lucide.Smile
import io.rudione.chatone.presentation.components.TactileIconButton
import io.rudione.chatone.presentation.components.TactileIconButtonDefaults
import io.rudione.chatone.presentation.components.layoutHeight
import io.rudione.chatone.presentation.components.layoutWidth
import io.rudione.chatone.presentation.components.interactiveIcon
import io.rudione.chatone.presentation.components.tactilePress
import io.rudione.chatone.presentation.theme.contentOnAccent
import io.rudione.chatone.util.system.isDesktopPlatform
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Immutable
internal data class MessageInputMetrics(
    val touch: Boolean,
    val emojiTarget: Dp,
    val emojiIcon: Dp,
    val send: Dp,
    val sendIcon: Dp,
    val rowMinHeight: Dp
) {
    @Composable
    fun textStyle(): TextStyle =
        if (touch) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium

    companion object {
        val Desktop = MessageInputMetrics(false, 32.dp, 20.dp, 28.dp, 15.dp, 34.dp)
        val Touch = MessageInputMetrics(true, 40.dp, 24.dp, 44.dp, 20.dp, 44.dp)

        fun current(): MessageInputMetrics = if (isDesktopPlatform) Desktop else Touch
    }
}

@Composable
internal fun MessageInputEmojiButton(
    enabled: Boolean,
    metrics: MessageInputMetrics,
    onClick: () -> Unit
) {
    TactileIconButton(
        onClick = onClick,
        size = metrics.emojiTarget,
        enabled = enabled,
        pressedScale = 0.82f
    ) { tint ->
        Icon(
            imageVector = Lucide.Smile,
            contentDescription = stringResource(Res.string.ui_emotes),
            modifier = Modifier.size(metrics.emojiIcon),
            tint = tint
        )
    }
}

@Composable
internal fun MessageInputSendButton(
    canSend: Boolean,
    metrics: MessageInputMetrics,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: () -> Dp = { metrics.send }
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val scheme = MaterialTheme.colorScheme
    val fill by animateFloatAsState(
        if (canSend) 1f else 0f,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "sendFill"
    )
    val container by animateColorAsState(
        if (canSend && hovered) lerp(scheme.primary, scheme.onPrimary, 0.12f) else scheme.primary,
        tween(140),
        label = "sendContainer"
    )
    val tint by animateColorAsState(
        if (canSend) contentOnAccent(scheme.primary, scheme.onPrimary) else scheme.onSurfaceVariant.copy(alpha = 0.55f),
        tween(160),
        label = "sendTint"
    )
    Box(
        modifier = modifier
            .layoutWidth(diameter)
            .layoutHeight(diameter)
            .tactilePress(interaction, pressedScale = if (canSend) 0.84f else 1f)
            .clip(CircleShape)
            .drawBehind {
                if (fill <= 0f) return@drawBehind
                drawCircle(container, radius = size.minDimension / 2f * fill.coerceAtMost(1f))
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = canSend,
                role = Role.Button,
                onClick = onClick
            )
            .pointerHoverIcon(if (canSend) PointerIcon.Hand else PointerIcon.Default),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Lucide.SendHorizontal,
            contentDescription = stringResource(Res.string.chat_send),
            tint = tint,
            modifier = Modifier
                .size(metrics.sendIcon)
                .graphicsLayer { translationX = metrics.sendIcon.toPx() * 0.04f }
                .interactiveIcon(interaction, enabled = canSend)
        )
    }
}

@Composable
internal fun MessageInputUploadButton(upload: MessageInputUploadState, size: Dp) {
    val state = when {
        upload.progress != null -> UploadView.Progress
        upload.link != null -> UploadView.Link
        else -> UploadView.Idle
    }
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            (scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), 0.6f) + fadeIn(tween(140)))
                .togetherWith(scaleOut(tween(120), 0.6f) + fadeOut(tween(100)))
        },
        contentAlignment = Alignment.Center,
        label = "uploadState"
    ) { view ->
        when (view) {
            UploadView.Progress -> LiquidGlassTooltipBox(tooltip = stringResource(Res.string.uploader_uploading)) {
                Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
                    val primary = MaterialTheme.colorScheme.primary
                    CircularProgressIndicator(
                        progress = { (upload.progress ?: 1f).coerceIn(0f, 1f) },
                        modifier = Modifier.size(size * 0.62f),
                        strokeWidth = 2.5.dp,
                        color = primary,
                        trackColor = primary.copy(alpha = 0.15f)
                    )
                }
            }
            UploadView.Link -> UploadedLinkButton(link = upload.link.orEmpty(), size = size, onCopy = upload.onCopyLink)
            UploadView.Idle -> Box(modifier = Modifier.size(size))
        }
    }
}

private enum class UploadView { Progress, Link, Idle }

@Composable
private fun UploadedLinkButton(link: String, size: Dp, onCopy: () -> Unit) {
    var justCopied by remember(link) { mutableStateOf(false) }
    LaunchedEffect(justCopied) {
        if (justCopied) {
            delay(COPIED_FEEDBACK_MS)
            justCopied = false
        }
    }
    val label = stringResource(if (justCopied) Res.string.uploader_link_copied else Res.string.uploader_copy_link)
    LiquidGlassTooltipBox(tooltip = label) {
        TactileIconButton(
            onClick = {
                justCopied = true
                onCopy()
            },
            size = size,
            colors = TactileIconButtonDefaults.colors(
                content = MaterialTheme.colorScheme.primary,
                hoveredContent = MaterialTheme.colorScheme.primary
            )
        ) { tint ->
            AnimatedContent(
                targetState = justCopied,
                transitionSpec = {
                    (scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), 0.5f) + fadeIn(tween(120)))
                        .togetherWith(scaleOut(tween(100), 0.5f) + fadeOut(tween(90)))
                },
                label = "uploadCopied"
            ) { copied ->
                Icon(
                    if (copied) Lucide.Check else Lucide.Link,
                    contentDescription = label,
                    modifier = Modifier.size(size * 0.56f),
                    tint = tint
                )
            }
        }
    }
}

@Composable
internal fun MessageInputLimits(
    charCount: Int,
    maxChars: Int,
    isNearLimit: Boolean,
    isOverLimit: Boolean,
    slowmodeRemainingSeconds: Int,
    slowmodeColor: Color,
    modifier: Modifier = Modifier
) {
    if (!isNearLimit && !isOverLimit && slowmodeRemainingSeconds <= 0) return
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (isNearLimit || isOverLimit) {
            Text(
                text = "$charCount/$maxChars",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = if (isOverLimit) LimitErrorColor else LimitWarnColor,
                fontWeight = if (isOverLimit) FontWeight.SemiBold else FontWeight.Medium
            )
        }
        if (slowmodeRemainingSeconds > 0) {
            Text(
                text = formatSlowmodeRemaining(slowmodeRemainingSeconds),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = slowmodeColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

internal fun formatSlowmodeRemaining(totalSeconds: Int): String =
    if (totalSeconds >= 60) "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
    else "${totalSeconds}s"

internal val LimitErrorColor = Color(0xFFCF6679)
internal val LimitWarnColor = Color(0xFFD4A017)
private const val COPIED_FEEDBACK_MS = 1500L
