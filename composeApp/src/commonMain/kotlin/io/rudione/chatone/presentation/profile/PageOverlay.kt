package io.rudione.chatone.presentation.profile

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import io.rudione.chatone.domain.media.MediaKind
import io.rudione.chatone.presentation.media.ClipEditorNavigation
import io.rudione.chatone.presentation.media.ClipEditorScreen
import io.rudione.chatone.presentation.media.MediaViewerNavigation
import io.rudione.chatone.presentation.media.MediaViewerScreen
import io.rudione.chatone.presentation.profile.editor.ProfileEditorScreen
import io.rudione.chatone.presentation.settings.SettingsState
import io.rudione.chatone.presentation.stream.FloatingStreamPlayer
import io.rudione.chatone.util.link.InAppLinkRouter
import io.rudione.chatone.util.link.TwitchLink
import io.rudione.chatone.util.link.openUrl
import org.koin.compose.koinInject

private val EnterEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val ExitEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private const val PAGE_Z_INDEX = 65f

@Composable
fun PageOverlay(
    onOpenChat: (login: String, displayName: String, avatarUrl: String) -> Unit,
    onWatch: (login: String) -> Unit,
    onPageOpened: () -> Unit = {}
) {
    val navigator: PageNavigator = koinInject()
    if (!navigator.isSupported) return
    val stack by navigator.stack.collectAsState()
    val saved = rememberSaveableStateHolder()
    val openChat by rememberUpdatedState(onOpenChat)
    val watch by rememberUpdatedState(onWatch)
    val pageOpened by rememberUpdatedState(onPageOpened)
    val hasPage = stack.isNotEmpty()
    LaunchedEffect(hasPage) { if (hasPage) pageOpened() }
    val navigation = remember(navigator) {
        ChannelProfileNavigation(
            onClose = navigator::back,
            onOpenChat = { login, name, avatar ->
                navigator.close()
                openChat(login, name, avatar)
            },
            onWatch = { login ->
                navigator.close()
                watch(login)
            },
            onEditProfile = { navigator.openEditor(null) },
            onOpenProfile = navigator::openChannel,
            onOpenClip = navigator::openClip,
            onOpenVideo = { id -> navigator.openVideo(id) }
        )
    }
    val mediaNavigation = remember(navigator) {
        MediaViewerNavigation(
            onClose = navigator::back,
            onOpenProfile = navigator::openChannel,
            onOpenUrl = { url -> openUrl(url, SettingsState.LinkOpenMode.DEFAULT) },
            onClipVideo = { details, offsetSeconds ->
                navigator.createVideoClip(details.broadcaster.login, details.id, details.broadcaster.id, offsetSeconds)
            }
        )
    }
    val clipNavigation = remember(navigator) {
        ClipEditorNavigation(
            onClose = navigator::back,
            onWatch = { slug ->
                navigator.back()
                navigator.openClip(slug)
            }
        )
    }
    DisposableEffect(navigator) {
        val handler: (TwitchLink) -> Boolean = { link ->
            when (link) {
                is TwitchLink.Channel -> navigator.openChannel(link.login)
                is TwitchLink.Clip -> navigator.openClip(link.slug)
                is TwitchLink.Video -> navigator.openVideo(link.id, link.startSeconds)
            }
            true
        }
        InAppLinkRouter.register(handler)
        onDispose { InAppLinkRouter.unregister(handler) }
    }
    AnimatedContent(
        targetState = stack,
        contentKey = { it.lastOrNull()?.key },
        transitionSpec = {
            if (targetState.lastOrNull() is PageDestination.ClipCreate || initialState.lastOrNull() is PageDestination.ClipCreate) {
                return@AnimatedContent fadeIn(tween(160)) togetherWith fadeOut(tween(160))
            }
            val forward = targetState.size >= initialState.size
            val direction = if (forward) 1 else -1
            (slideInHorizontally(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) { w -> direction * w / 4 } +
                    fadeIn(tween(220, delayMillis = 40, easing = EnterEasing))) togetherWith
                    (slideOutHorizontally(tween(200, easing = ExitEasing)) { w -> -direction * w / 6 } +
                            fadeOut(tween(140, easing = ExitEasing)))
        },
        label = "pageOverlay",
        modifier = Modifier.fillMaxSize().zIndex(PAGE_Z_INDEX)
    ) { current ->
        val top = current.lastOrNull()
        Box(Modifier.fillMaxSize()) {
            when (top) {
                null -> Unit
                is PageDestination.Channel -> saved.SaveableStateProvider(top.key) {
                    ChannelProfileScreen(login = top.login, navigation = navigation)
                }
                is PageDestination.Editor -> ProfileEditorScreen(channelLogin = top.channelLogin, onClose = navigator::back)
                is PageDestination.Clip -> MediaViewerScreen(MediaKind.CLIP, top.slug, 0, mediaNavigation)
                is PageDestination.Video -> MediaViewerScreen(MediaKind.VIDEO, top.id, top.startSeconds, mediaNavigation)
                is PageDestination.ClipCreate -> ClipEditorScreen(top.login, top.origin, clipNavigation)
            }
            if (top is PageDestination.Channel || top is PageDestination.Editor) {
                FloatingStreamPlayer(onExpand = navigator::close)
            }
        }
    }
}
