package io.rudione.chatone.presentation.chat.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_jump_to_mention
import chatone.composeapp.generated.resources.chat_jump_to_mention_hint
import chatone.composeapp.generated.resources.chat_scroll_to_bottom
import io.rudione.chatone.icons.lucide.AtSign
import io.rudione.chatone.icons.lucide.ChevronDown
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.chat.mentions.MentionJumpState
import io.rudione.chatone.presentation.components.animatedBorder
import io.rudione.chatone.presentation.components.crumble
import io.rudione.chatone.presentation.components.interactiveIcon
import io.rudione.chatone.presentation.components.onSecondaryClick
import io.rudione.chatone.presentation.components.rememberCrumbleState
import io.rudione.chatone.presentation.components.tactilePress
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.contentOnAccent
import io.rudione.chatone.util.system.isDesktopPlatform
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@Immutable
private data class JumpMetrics(
    val button: Dp,
    val icon: Dp,
    val gap: Dp,
    val badgeHeight: Dp,
    val badgeText: TextUnit,
    val timer: Dp,
    val timerText: TextUnit
) {
    companion object {
        val Desktop = JumpMetrics(
            38.dp,
            18.dp,
            16.dp,
            16.dp,
            10.sp,
            14.dp,
            8.sp
        )
        val Touch = JumpMetrics(
            46.dp,
            24.dp,
            18.dp,
            20.dp,
            10.sp,
            16.dp,
            8.sp
        )
    }
}

private const val MAX_BADGE = 999
private const val REST_ALPHA = 0.8f
private const val HOVER_ALPHA = 0.88f
private const val PRESSED_ALPHA = 0.94f
private val ButtonShadow = 4.dp

@Composable
internal fun ChatJumpButtons(
    showScrollDown: Boolean,
    unreadCount: Int,
    mentions: MentionJumpState,
    onScrollDown: () -> Unit,
    onJumpToMention: () -> Unit,
    onMentionAuthors: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    glass: ChatInputGlass? = null
) {
    val metrics = remember { if (isDesktopPlatform) JumpMetrics.Desktop else JumpMetrics.Touch }
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val crumble = rememberCrumbleState()
    var dissolving by remember { mutableStateOf(false) }
    val mentionCount = mentions.count
    LaunchedEffect(mentionCount > 0, dissolving) {
        if (mentionCount > 0 && !dissolving && crumble.isCrumbling) crumble.reset()
    }
    val palette = listOf(
        MaterialTheme.colorScheme.surfaceContainerHighest,
        MaterialTheme.colorScheme.onSurfaceVariant,
        MaterialTheme.colorScheme.primary
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(metrics.gap)
    ) {
        AnimatedVisibility(
            visible = mentionCount > 0 || dissolving,
            enter = fadeIn(tween(160)) + scaleIn(
                spring(
                    Spring.DampingRatioMediumBouncy,
                    Spring.StiffnessMediumLow
                ), 0.4f
            ) +
                    expandVertically(expandFrom = Alignment.Bottom, clip = false),
            exit = fadeOut(tween(140)) + scaleOut(tween(180), 0.6f) +
                    shrinkVertically(shrinkTowards = Alignment.Bottom, clip = false)
        ) {
            val jumpLabel = stringResource(Res.string.chat_jump_to_mention)
            val button: @Composable () -> Unit = {
                JumpCircle(
                    glass = glass,
                    icon = Lucide.AtSign,
                    contentDescription = jumpLabel,
                    metrics = metrics,
                    modifier = Modifier.crumble(crumble, palette),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                        onJumpToMention()
                    },
                    onSecondary = {
                        if (!dissolving) {
                            dissolving = true
                            onMentionAuthors(mentions.authors())
                            mentions.consumeAll()
                            scope.launch {
                                crumble.crumble()
                                dissolving = false
                            }
                        }
                    }
                ) {
                    JumpBadge(
                        count = mentionCount,
                        metrics = metrics,
                        modifier = Modifier.align(Alignment.TopCenter)
                    )
                    LingerTimer(
                        endsAtMs = mentions.lingerEndsAtMs,
                        metrics = metrics,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }
            if (isDesktopPlatform) {
                LiquidGlassTooltipBox(
                    tooltip = stringResource(Res.string.chat_jump_to_mention_hint),
                    content = button
                )
            } else {
                button()
            }
        }
        AnimatedVisibility(
            visible = showScrollDown,
            enter = fadeIn(tween(160)) + scaleIn(
                spring(
                    Spring.DampingRatioMediumBouncy,
                    Spring.StiffnessMediumLow
                ), 0.4f
            ),
            exit = fadeOut(tween(140)) + scaleOut(tween(180), 0.6f)
        ) {
            JumpCircle(
                glass = glass,
                icon = Lucide.ChevronDown,
                contentDescription = stringResource(Res.string.chat_scroll_to_bottom),
                metrics = metrics,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onScrollDown()
                }
            ) {
                JumpBadge(
                    count = unreadCount,
                    metrics = metrics,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun JumpCircle(
    glass: ChatInputGlass?,
    icon: ImageVector,
    contentDescription: String,
    metrics: JumpMetrics,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSecondary: (() -> Unit)? = null,
    decorations: @Composable BoxScope.() -> Unit = {}
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        when {
            pressed -> scheme.surfaceBright.copy(alpha = PRESSED_ALPHA)
            hovered -> scheme.surfaceContainerHighest.copy(alpha = HOVER_ALPHA)
            else -> scheme.surfaceContainerHigh.copy(alpha = REST_ALPHA)
        },
        tween(140),
        label = "jumpContainer"
    )
    val shadow = ChatoneTheme.extraColors.shadowColor
    val tint by animateColorAsState(
        if (hovered || pressed) scheme.onSurface else scheme.onSurfaceVariant,
        tween(140),
        label = "jumpTint"
    )
    val edge = scheme.outlineVariant
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(metrics.button)
                .tactilePress(interaction, pressedScale = 0.86f)
                .then(
                    if (glass != null) {
                        Modifier.chatGlass(
                            glass = glass,
                            shape = CircleShape,
                            resting = { container },
                            border = { edge },
                            borderWidth = { 1.dp },
                            elevation = ButtonShadow,
                            glow = shadow
                        )
                    } else {
                        Modifier
                            .shadow(ButtonShadow, CircleShape, clip = false)
                            .clip(CircleShape)
                            .drawBehind { drawRect(container) }
                            .animatedBorder(1.dp, CircleShape) { edge }
                    }
                )
                .then(if (onSecondary != null) Modifier.onSecondaryClick(onSecondary) else Modifier)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    onLongClick = onSecondary,
                    onClick = onClick
                )
                .pointerHoverIcon(PointerIcon.Hand),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(metrics.icon).interactiveIcon(interaction)
            )
        }
        decorations()
    }
}

@Composable
private fun JumpBadge(count: Int, metrics: JumpMetrics, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = count > 0,
        modifier = modifier.offset(y = -(metrics.badgeHeight / 2)),
        enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) + fadeIn(
            tween(120)
        ),
        exit = scaleOut(tween(140)) + fadeOut(tween(120))
    ) {
        val shown = count.coerceAtLeast(1)
        val badgeColor = MaterialTheme.colorScheme.primary
        val badgeContent = contentOnAccent(badgeColor, MaterialTheme.colorScheme.onPrimary)
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = metrics.badgeHeight, minHeight = metrics.badgeHeight)
                .clip(CircleShape)
                .drawBehind { drawRect(badgeColor) }
                .padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = shown,
                transitionSpec = { countTransition(initialState, targetState) },
                label = "jumpBadgeCount"
            ) { value ->
                Text(
                    text = if (value > MAX_BADGE) "$MAX_BADGE+" else value.toString(),
                    color = badgeContent,
                    fontSize = metrics.badgeText,
                    lineHeight = metrics.badgeText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LingerTimer(endsAtMs: Long?, metrics: JumpMetrics, modifier: Modifier = Modifier) {
    val secondsLeft by produceState(initialValue = 0, endsAtMs) {
        if (endsAtMs == null) {
            value = 0
            return@produceState
        }
        while (true) {
            val left = endsAtMs - Clock.System.now().toEpochMilliseconds()
            value = ((left + 999) / 1000).toInt().coerceAtLeast(0)
            if (left <= 0L) break
            delay(left % 1000 + 1)
        }
    }
    AnimatedVisibility(
        visible = endsAtMs != null && secondsLeft > 0,
        modifier = modifier.offset(x = 3.dp, y = 3.dp),
        enter = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)) + fadeIn(
            tween(120)
        ),
        exit = scaleOut(tween(140)) + fadeOut(tween(120))
    ) {
        val scheme = MaterialTheme.colorScheme
        Box(
            modifier = Modifier
                .size(metrics.timer)
                .clip(CircleShape)
                .drawBehind { drawRect(scheme.surfaceContainerHighest) }
                .animatedBorder(1.dp, CircleShape) { scheme.surfaceContainer },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = secondsLeft.coerceAtLeast(1),
                transitionSpec = { countTransition(initialState, targetState) },
                label = "jumpTimer"
            ) { value ->
                Text(
                    text = value.toString(),
                    color = scheme.onSurfaceVariant,
                    fontSize = metrics.timerText,
                    lineHeight = metrics.timerText,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

private fun countTransition(from: Int, to: Int): ContentTransform {
    val direction = if (to > from) 1 else -1
    return (slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { height -> height * direction } + fadeIn(
        tween(120)
    ))
        .togetherWith(slideOutVertically(tween(140)) { height -> -height * direction } + fadeOut(
            tween(100)
        ))
}
