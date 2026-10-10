package io.rudione.chatone.presentation.browse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rudione.chatone.icons.lucide.ChevronUp
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.presentation.stream.LocalFloatingStreamPlayer
import io.rudione.chatone.presentation.theme.i18n.BrowseStrings
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

private const val SHOW_AFTER_INDEX = 4
private const val JUMP_BEFORE_ANIMATION_INDEX = 24
private const val LANDING_INDEX = 6
private val ButtonMargin = 16.dp

@Composable
internal fun BoxScope.ScrollToTopButton(gridState: LazyGridState, strings: BrowseStrings, bottomInset: Dp) {
    val visible by remember(gridState) { derivedStateOf { gridState.firstVisibleItemIndex > SHOW_AFTER_INDEX } }
    val floating = LocalFloatingStreamPlayer.current
    val clearance by animateDpAsState(floating?.bottomEndClearance ?: 0.dp, tween(260), label = "scrollTopClearance")
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.6f) + fadeIn(),
        exit = scaleOut(targetScale = 0.6f) + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = ButtonMargin, bottom = ButtonMargin + bottomInset + clearance)
    ) {
        SmallFloatingActionButton(
            onClick = {
                scope.launch {
                    if (gridState.firstVisibleItemIndex > JUMP_BEFORE_ANIMATION_INDEX) gridState.scrollToItem(LANDING_INDEX)
                    gridState.animateScrollToItem(0)
                }
            },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(Lucide.ChevronUp, contentDescription = strings.scrollToTop)
        }
    }
}

@Composable
internal fun ResetScrollOnChange(gridState: LazyGridState, key: Any?) {
    val latestKey by rememberUpdatedState(key)
    LaunchedEffect(gridState) {
        snapshotFlow { latestKey }
            .drop(1)
            .collect { if (gridState.firstVisibleItemIndex > 0) gridState.scrollToItem(0) }
    }
}
