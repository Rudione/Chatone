package io.rudione.chatone.tv.player

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.player_chat
import chatone.composeapp.generated.resources.tv_chat_hint
import chatone.composeapp.generated.resources.tv_chat_write
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.MessageSquare
import io.rudione.chatone.presentation.chat.ChatEvent
import io.rudione.chatone.presentation.chat.ChatRemoteScroll
import io.rudione.chatone.presentation.chat.ChatScreen
import io.rudione.chatone.presentation.chat.ChatScreenCallbacks
import io.rudione.chatone.presentation.chat.ChatScreenOptions
import io.rudione.chatone.presentation.chat.ChatSessionIdentity
import io.rudione.chatone.presentation.chat.ChatSurfaceStyle
import io.rudione.chatone.presentation.chat.ChatViewModel
import io.rudione.chatone.presentation.chat.LocalChatRemoteScroll
import io.rudione.chatone.presentation.chat.LocalChatSurfaceStyle
import io.rudione.chatone.presentation.components.ChatoneWindowSize
import io.rudione.chatone.presentation.components.LocalWindowSize
import io.rudione.chatone.presentation.stream.streamLatencyText
import io.rudione.chatone.presentation.theme.LocalWallpaperController
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val CHAT_SCROLL_STEP_DP = 96
private val TvChatStyle = ChatSurfaceStyle(
    transparentBackground = true,
    showHeader = false,
    showInput = false,
    showScrollbar = false,
    showPinnedMessage = false,
    showEventBanners = false,
    claimsFocus = false
)

@Composable
internal fun TvChatPanel(
    channelLogin: String,
    identity: ChatSessionIdentity,
    callbacks: ChatScreenCallbacks,
    scale: Float,
    backgroundAlpha: () -> Float,
    latency: StateFlow<Float?>,
    showLatency: Boolean,
    returnToLiveMs: Long,
    focusRequester: FocusRequester,
    onLeave: () -> Unit,
    modifier: Modifier = Modifier,
    chromeAlpha: () -> Float = { 1f }
) {
    val remote = remember { ChatRemoteScroll() }
    val density = LocalDensity.current
    val scaledDensity =
        remember(density, scale) { Density(density.density * scale, density.fontScale) }
    val scrollStep = with(density) { CHAT_SCROLL_STEP_DP.dp.toPx() }
    val wallpaper = LocalWallpaperController.current.state
    val composerFocus = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var scrolls by remember { mutableIntStateOf(0) }
    val scheme = MaterialTheme.colorScheme

    LaunchedEffect(scrolls, returnToLiveMs) {
        if (scrolls > 0 && returnToLiveMs > 0) {
            delay(returnToLiveMs)
            remote.toLatest()
        }
    }

    Column(modifier.drawBehind { drawRect(scheme.surface.copy(alpha = backgroundAlpha())) }) {
        TvChatHeader(
            latency = latency,
            showLatency = showLatency,
            modifier = Modifier.graphicsLayer { alpha = chromeAlpha() }
        )
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (focused && !it.isFocused) remote.toLatest()
                    focused = it.isFocused
                }
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> {
                            remote.scrollBy(-scrollStep)
                            scrolls++
                            true
                        }

                        Key.DirectionDown -> {
                            remote.scrollBy(scrollStep)
                            scrolls++
                            true
                        }

                        Key.DirectionLeft -> {
                            onLeave()
                            true
                        }

                        Key.DirectionRight -> true

                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            if (identity.userId.isNotBlank()) composerFocus.requestFocus()
                            true
                        }

                        else -> false
                    }
                }
                .focusable()
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .focusProperties { onEnter = { cancelFocusChange() } }
                    .focusGroup()
            ) {
                CompositionLocalProvider(
                    LocalWindowSize provides ChatoneWindowSize.Compact,
                    LocalChatSurfaceStyle provides TvChatStyle,
                    LocalChatRemoteScroll provides remote,
                    LocalDensity provides scaledDensity
                ) {
                    ChatScreen(
                        channelLogin = channelLogin,
                        identity = identity,
                        callbacks = callbacks,
                        wallpaper = wallpaper,
                        modifier = Modifier.fillMaxSize(),
                        options = ChatScreenOptions(renderBackground = false)
                    )
                }
            }
        }
        if (focused) {
            Text(
                stringResource(Res.string.tv_chat_hint),
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }
        if (identity.userId.isNotBlank()) {
            TvChatComposer(
                focusRequester = composerFocus,
                onDone = { focusRequester.requestFocus() },
                modifier = Modifier.graphicsLayer { alpha = chromeAlpha() }
            )
        }
    }
}

@Composable
private fun TvChatHeader(latency: StateFlow<Float?>, showLatency: Boolean, modifier: Modifier = Modifier) {
    val seconds by latency.collectAsState()
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(Res.string.player_chat), style = MaterialTheme.typography.titleSmall)
        val current = seconds
        if (showLatency && current != null) {
            Text(
                streamLatencyText(current, compact = false),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TvChatComposer(focusRequester: FocusRequester, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val chatViewModel: ChatViewModel = koinViewModel()
    var text by rememberSaveable { mutableStateOf("") }
    var fieldFocused by remember { mutableStateOf(false) }
    var keyboardSeen by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val keyboardOpen by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
    val leave by rememberUpdatedState(onDone)
    val scheme = MaterialTheme.colorScheme
    LaunchedEffect(keyboardOpen, fieldFocused) {
        when {
            keyboardOpen && fieldFocused -> keyboardSeen = true
            !keyboardOpen && keyboardSeen -> {
                keyboardSeen = false
                if (fieldFocused) leave()
            }
        }
    }
    val send = {
        val message = text.trim()
        if (message.isNotEmpty()) chatViewModel.sendEvent(ChatEvent.OnSendMessageText(message))
        text = ""
        onDone()
    }
    Surface(
        onClick = { focusRequester.requestFocus() },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        shape = ClickableSurfaceDefaults.shape(CircleShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(2.dp, scheme.onSurface), shape = CircleShape)
        ),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = scheme.surfaceVariant,
            focusedContainerColor = scheme.surfaceVariant
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Lucide.MessageSquare,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(MAX_MESSAGE) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { fieldFocused = it.isFocused },
                decorationBox = { field ->
                    if (text.isEmpty()) {
                        Text(
                            stringResource(Res.string.tv_chat_write),
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant
                        )
                    }
                    field()
                }
            )
        }
    }
}

private const val MAX_MESSAGE = 500
