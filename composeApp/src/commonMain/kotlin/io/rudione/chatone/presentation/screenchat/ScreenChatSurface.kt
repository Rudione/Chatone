package io.rudione.chatone.presentation.screenchat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.domain.model.ScreenChatSettings
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.window.WindowGrip
import io.rudione.chatone.presentation.window.windowGrip
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Stable
internal class ScreenChatChrome {
    var revealed by mutableStateOf(false)
        private set
    var held by mutableStateOf(false)
    var activity by mutableIntStateOf(0)
        private set

    fun poke() {
        revealed = true
        activity++
    }

    fun hide() {
        if (!held) revealed = false
    }
}

@Composable
fun ScreenChatSurface(
    channelLogin: String,
    messages: List<DisplayMessage>,
    chat: SettingsState,
    settings: ScreenChatSettings,
    onSettingsChange: (ScreenChatSettings) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenApp: (() -> Unit)? = null
) {
    val chrome = remember { ScreenChatChrome() }
    val latestSettings by rememberUpdatedState(settings)
    val change by rememberUpdatedState(onSettingsChange)
    LaunchedEffect(chrome, settings.hideDelayMs) {
        snapshotFlow { Triple(chrome.revealed, chrome.held, chrome.activity) }.collectLatest { (revealed, held, _) ->
            if (revealed && !held) {
                delay(settings.hideDelayMs)
                chrome.hide()
            }
        }
    }
    val revealed = chrome.revealed
    val background = animateFloatAsState(
        targetValue = if (revealed) settings.backgroundAlpha else 0f,
        animationSpec = if (revealed) tween(REVEAL_MS, easing = Decelerate) else tween(FADE_MS, easing = Accelerate),
        label = "screenChatBackground"
    )
    val outline = animateFloatAsState(
        targetValue = if (revealed) OUTLINE_ALPHA else 0f,
        animationSpec = tween(REVEAL_MS),
        label = "screenChatOutline"
    )
    val baseDensity = LocalDensity.current
    val scaled = remember(baseDensity, settings.scale) {
        Density(baseDensity.density * settings.scale, baseDensity.fontScale)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(chrome) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        when (event.type) {
                            PointerEventType.Scroll -> {
                                val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                if ((event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed) && delta != 0f) {
                                    event.changes.forEach { it.consume() }
                                    change(latestSettings.scaledBy(if (delta < 0f) 1 else -1))
                                }
                                chrome.poke()
                            }
                            PointerEventType.Exit -> Unit
                            else -> chrome.poke()
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(WindowShape)
                .drawWithContent {
                    drawRect(Color.Black.copy(alpha = background.value))
                    drawContent()
                    val stroke = 1.dp.toPx()
                    drawRoundRect(
                        color = Color.White.copy(alpha = outline.value),
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(CORNER.toPx()),
                        style = Stroke(stroke)
                    )
                }
        ) {
            AnimatedVisibility(
                visible = revealed,
                enter = expandVertically(tween(REVEAL_MS, easing = Decelerate), expandFrom = Alignment.Top) +
                        fadeIn(tween(REVEAL_MS, easing = Decelerate)),
                exit = shrinkVertically(tween(FADE_MS, easing = Accelerate), shrinkTowards = Alignment.Top) +
                        fadeOut(tween(FADE_MS, easing = Accelerate))
            ) {
                ScreenChatBar(
                    channelLogin = channelLogin,
                    locked = settings.locked,
                    onHeldChange = { chrome.held = it },
                    onToggleLock = { change(latestSettings.copy(locked = !latestSettings.locked)) },
                    onOpenSettings = onOpenSettings,
                    onOpenApp = onOpenApp,
                    onClose = onClose
                )
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                CompositionLocalProvider(LocalDensity provides scaled) {
                    if (messages.isEmpty()) {
                        Text(
                            LocalStrings.current.screenChat.waiting,
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                        )
                    } else {
                        ScreenChatMessages(messages = messages, chat = chat, settings = settings, live = !revealed)
                    }
                }
            }
        }
        if (revealed && !settings.locked) ResizeGrips(onHeldChange = { chrome.held = it })
    }
}

@Composable
private fun BoxScope.ResizeGrips(onHeldChange: (Boolean) -> Unit) {
    Box(Modifier.align(Alignment.CenterStart).width(EDGE).fillMaxHeight().windowGrip(WindowGrip.LEFT, onHeldChange))
    Box(Modifier.align(Alignment.CenterEnd).width(EDGE).fillMaxHeight().windowGrip(WindowGrip.RIGHT, onHeldChange))
    Box(Modifier.align(Alignment.BottomCenter).height(EDGE).fillMaxWidth().windowGrip(WindowGrip.BOTTOM, onHeldChange))
    Box(Modifier.align(Alignment.BottomStart).size(CORNER_GRIP).windowGrip(WindowGrip.BOTTOM_LEFT, onHeldChange))
    ScreenChatCornerGrip(
        modifier = Modifier.align(Alignment.BottomEnd).size(CORNER_GRIP).windowGrip(WindowGrip.BOTTOM_RIGHT, onHeldChange)
    )
}

private val WindowShape = RoundedCornerShape(12.dp)
private val CORNER = 12.dp
private val EDGE = 6.dp
private val CORNER_GRIP = 18.dp
private const val OUTLINE_ALPHA = 0.14f
private const val REVEAL_MS = 180
private const val FADE_MS = 320
private val Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val Accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
