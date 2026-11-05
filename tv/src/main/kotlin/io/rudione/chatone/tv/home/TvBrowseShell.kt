package io.rudione.chatone.tv.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.tv.material3.MaterialTheme
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.presentation.components.SystemBackHandler
import io.rudione.chatone.tv.theme.TvDimens

private const val CONTENT_SETTLE_FRAMES = 2
private const val SCRIM_ALPHA = 0.88f

@Composable
fun TvBrowseShell(
    account: TwitchAccount?,
    contentFocus: FocusRequester,
    active: Boolean,
    channels: TvDrawerChannels,
    drawerActions: TvDrawerActions,
    drawerFocus: FocusRequester,
    content: @Composable (TvSection) -> Unit
) {
    var section by rememberSaveable { mutableStateOf(TvSection.HOME) }
    var drawerFocused by remember { mutableStateOf(false) }
    val selectionFocus = remember { FocusRequester() }
    var pendingContentFocus by remember { mutableStateOf(false) }
    LaunchedEffect(pendingContentFocus, section) {
        if (pendingContentFocus) {
            repeat(CONTENT_SETTLE_FRAMES) { withFrameNanos { } }
            runCatching { contentFocus.requestFocus() }
            pendingContentFocus = false
        }
    }
    SystemBackHandler(enabled = active && !drawerFocused) {
        runCatching { selectionFocus.requestFocus() }
    }
    val background = MaterialTheme.colorScheme.background
    val scrim by animateFloatAsState(if (drawerFocused) 1f else 0f, tween(220), label = "tvDrawerScrim")
    Box(
        Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = TvDimens.RailWidth)
                .focusRequester(contentFocus)
                .focusRestorer()
                .focusGroup()
        ) {
            key(section) { content(section) }
        }
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = scrim }
                .background(
                    Brush.horizontalGradient(
                        0f to background.copy(alpha = SCRIM_ALPHA),
                        0.45f to background.copy(alpha = SCRIM_ALPHA * 0.6f),
                        1f to Color.Transparent
                    )
                )
        )
        TvSideDrawer(
            account = account,
            selected = section,
            expanded = drawerFocused,
            channels = channels,
            actions = drawerActions,
            onSelect = {
                section = it
                pendingContentFocus = true
            },
            onFocusChanged = { drawerFocused = it },
            selectionFocus = selectionFocus,
            drawerFocus = drawerFocus,
            modifier = Modifier.fillMaxHeight()
        )
    }
}
