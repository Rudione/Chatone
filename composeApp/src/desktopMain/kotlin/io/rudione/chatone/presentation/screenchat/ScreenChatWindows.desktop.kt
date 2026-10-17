package io.rudione.chatone.presentation.screenchat

import io.rudione.chatone.presentation.components.PopupPortalHost
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.icon
import com.russhwolf.settings.Settings
import io.rudione.chatone.domain.model.ScreenChatLayer
import io.rudione.chatone.domain.model.ScreenChatSettings
import io.rudione.chatone.presentation.chat.ChatEvent
import io.rudione.chatone.presentation.chat.ChatSessionIdentity
import io.rudione.chatone.presentation.chat.ChatSessions
import io.rudione.chatone.presentation.chat.ChatViewModel
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.settings.components.ScreenChatSettingsContent
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.window.ChatoneDetachedWindow
import io.rudione.chatone.presentation.window.LocalDraggableWindow
import io.rudione.chatone.presentation.window.MIN_TOOL_WINDOW_HEIGHT
import io.rudione.chatone.presentation.window.MIN_TOOL_WINDOW_WIDTH
import io.rudione.chatone.presentation.window.isMacOs
import io.rudione.chatone.util.automod.DisplayedChatChannels
import io.rudione.chatone.util.system.DesktopMainWindow
import io.rudione.chatone.util.system.MacSpaces
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import java.awt.Dimension
import java.awt.KeyboardFocusManager
import java.beans.PropertyChangeListener
import kotlin.math.roundToInt

actual val screenChatFloatsOverFullScreen: Boolean = isMacOs

@Composable
actual fun ScreenChatWindows(
    identity: ChatSessionIdentity,
    chat: SettingsState,
    onSettingsChange: (ScreenChatSettings) -> Unit
) {
    val controller: ScreenChatController = koinInject()
    val open by controller.open.collectAsState()
    if (open.isEmpty()) return
    val appActive by rememberAppActive()
    var settingsOpen by remember { mutableStateOf(false) }
    val hideDockIcon = isMacOs && chat.screenChat.layer == ScreenChatLayer.ALL_WINDOWS &&
        chat.screenChat.overFullScreen && !chat.screenChat.keepDockIcon
    DisposableEffect(hideDockIcon) {
        if (hideDockIcon) MacSpaces.setDockIconHidden(true)
        onDispose { if (hideDockIcon) MacSpaces.setDockIconHidden(false) }
    }
    open.forEach { channel ->
        key(channel) {
            ScreenChatWindow(
                channel = channel,
                identity = identity,
                chat = chat,
                appActive = appActive,
                onSettingsChange = onSettingsChange,
                onOpenSettings = { settingsOpen = true },
                onClose = { controller.close(channel) }
            )
        }
    }
    if (settingsOpen) {
        ScreenChatSettingsWindow(
            settings = chat.screenChat,
            onSettingsChange = onSettingsChange,
            onClose = { settingsOpen = false }
        )
    }
}

@Composable
private fun ScreenChatSettingsWindow(
    settings: ScreenChatSettings,
    onSettingsChange: (ScreenChatSettings) -> Unit,
    onClose: () -> Unit
) {
    ChatoneDetachedWindow(
        windowId = "screen_chat_settings",
        title = LocalStrings.current.screenChat.settingsWindowTitle,
        defaultWidth = SETTINGS_WIDTH,
        defaultHeight = SETTINGS_HEIGHT,
        minWidth = MIN_TOOL_WINDOW_WIDTH,
        minHeight = MIN_TOOL_WINDOW_HEIGHT,
        alwaysOnTop = true,
        onCloseRequest = onClose
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ScreenChatSettingsContent(settings, floatsOverFullScreen = screenChatFloatsOverFullScreen, onChange = onSettingsChange)
            }
        }
    }
}

@Composable
private fun ScreenChatWindow(
    channel: String,
    identity: ChatSessionIdentity,
    chat: SettingsState,
    appActive: Boolean,
    onSettingsChange: (ScreenChatSettings) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    val settings = chat.screenChat
    val windowState = rememberScreenChatWindowState(channel)
    val source = rememberScreenChatSource(channel, identity)
    val state by source.state.collectAsState()
    val messages = if (state.channelLogin.equals(channel, ignoreCase = true)) state.messages else emptyList()
    val onTop = when (settings.layer) {
        ScreenChatLayer.ALL_WINDOWS -> true
        ScreenChatLayer.APP_WINDOWS -> appActive
        ScreenChatLayer.NORMAL -> false
    }
    DisposableEffect(channel) {
        DisplayedChatChannels.acquire(channel)
        onDispose { DisplayedChatChannels.release(channel) }
    }
    val title = LocalStrings.current.screenChat.windowTitle.replace("{0}", channel)
    Window(
        onCloseRequest = onClose,
        state = windowState,
        title = title,
        icon = painterResource(Res.drawable.icon),
        undecorated = true,
        transparent = true,
        resizable = false,
        alwaysOnTop = onTop
    ) {
        SideEffect { window.applyOverlayChrome() }
        SideEffect(settings.layer, appActive) {
            if (settings.layer == ScreenChatLayer.APP_WINDOWS && !appActive) window.toBack()
        }
        if (isMacOs) {
            LaunchedEffect(title, settings.layer, settings.overFullScreen, settings.keepDockIcon) {
                val pinned = settings.layer == ScreenChatLayer.ALL_WINDOWS
                val aboveFullScreen = pinned && settings.overFullScreen
                val nonActivating = aboveFullScreen && settings.keepDockIcon
                delay(SPACES_SETTLE_MS)
                MacSpaces.pinToAllSpaces(window.windowHandle, title, pinned, aboveFullScreen, nonActivating)
                if (aboveFullScreen) {
                    delay(POLICY_SETTLE_MS)
                    MacSpaces.pinToAllSpaces(window.windowHandle, title, pinned, aboveFullScreen, nonActivating)
                }
            }
        }
        PopupPortalHost {
            CompositionLocalProvider(LocalDraggableWindow provides window) {
                ScreenChatSurface(
                    channelLogin = channel,
                    messages = messages,
                    chat = chat,
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                    onOpenSettings = onOpenSettings,
                    onClose = onClose,
                    onOpenApp = DesktopMainWindow::bringToFront
                )
            }
        }
    }
}

@Composable
private fun rememberScreenChatSource(channel: String, identity: ChatSessionIdentity): ChatViewModel {
    val owner = remember(channel) { ScreenChatStoreOwner() }
    DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
    val shared = ChatSessions.find(channel)
    if (shared != null) {
        SideEffect(owner, shared) { owner.viewModelStore.clear() }
        return shared
    }
    val own: ChatViewModel = koinViewModel(viewModelStoreOwner = owner, key = "screen-chat-$channel")
    SideEffect(own, channel, identity.accessToken) {
        own.sendEvent(
            ChatEvent.OnInit(
                channelLogin = channel,
                accessToken = identity.accessToken,
                userId = identity.userId,
                userLogin = identity.userLogin,
                userDisplayName = identity.displayName
            )
        )
    }
    return own
}

private class ScreenChatStoreOwner : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
}

@OptIn(FlowPreview::class)
@Composable
private fun rememberScreenChatWindowState(channel: String): WindowState {
    val settings = remember { Settings() }
    val prefix = "screen_chat_${channel}_"
    val saved = remember(channel) {
        val x = settings.getFloatOrNull(prefix + "x")
        val y = settings.getFloatOrNull(prefix + "y")
        SavedBounds(
            position = if (x != null && y != null) WindowPosition(x.dp, y.dp) else WindowPosition(Alignment.BottomEnd),
            size = DpSize(
                settings.getFloat(prefix + "w", DEFAULT_WIDTH.value).dp.coerceAtLeast(MIN_WIDTH),
                settings.getFloat(prefix + "h", DEFAULT_HEIGHT.value).dp.coerceAtLeast(MIN_HEIGHT)
            )
        )
    }
    val state = rememberWindowState(position = saved.position, size = saved.size)
    LaunchedEffect(state, channel) {
        snapshotFlow { state.position to state.size }
            .distinctUntilChanged()
            .debounce(SAVE_DEBOUNCE_MS)
            .collect { (position, size) ->
                (position as? WindowPosition.Absolute)?.let {
                    settings.putFloat(prefix + "x", it.x.value)
                    settings.putFloat(prefix + "y", it.y.value)
                }
                settings.putFloat(prefix + "w", size.width.value)
                settings.putFloat(prefix + "h", size.height.value)
            }
    }
    return state
}

private class SavedBounds(val position: WindowPosition, val size: DpSize)

private fun ComposeWindow.applyOverlayChrome() {
    val minimum = Dimension(MIN_WIDTH.value.roundToInt(), MIN_HEIGHT.value.roundToInt())
    if (minimumSize != minimum) minimumSize = minimum
    if (isMacOs && rootPane.getClientProperty(MAC_SHADOW) != false) rootPane.putClientProperty(MAC_SHADOW, false)
}

@Composable
private fun rememberAppActive(): State<Boolean> {
    val focus = remember { KeyboardFocusManager.getCurrentKeyboardFocusManager() }
    val raw = remember { mutableStateOf(focus.activeWindow != null) }
    val settled = remember { mutableStateOf(raw.value) }
    DisposableEffect(focus) {
        val listener = PropertyChangeListener { raw.value = focus.activeWindow != null }
        focus.addPropertyChangeListener(ACTIVE_WINDOW, listener)
        onDispose { focus.removePropertyChangeListener(ACTIVE_WINDOW, listener) }
    }
    LaunchedEffect(raw.value) {
        if (!raw.value) delay(FOCUS_SETTLE_MS)
        settled.value = raw.value
    }
    return settled
}

private val DEFAULT_WIDTH = 340.dp
private val DEFAULT_HEIGHT = 480.dp
private val MIN_WIDTH = 160.dp
private val MIN_HEIGHT = 120.dp
private val SETTINGS_WIDTH = 460.dp
private val SETTINGS_HEIGHT = 680.dp
private const val SAVE_DEBOUNCE_MS = 400L
private const val FOCUS_SETTLE_MS = 250L
private const val SPACES_SETTLE_MS = 300L
private const val POLICY_SETTLE_MS = 1_600L
private const val ACTIVE_WINDOW = "activeWindow"
private const val MAC_SHADOW = "Window.shadow"
