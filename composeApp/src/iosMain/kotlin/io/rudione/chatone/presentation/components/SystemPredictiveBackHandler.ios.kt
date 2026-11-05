package io.rudione.chatone.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

@Composable
actual fun SystemPredictiveBackHandler(
    enabled: Boolean,
    onProgress: (Float) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit
) {
    val progress by rememberUpdatedState(onProgress)
    val state = rememberNavigationEventState(NavigationEventInfo.None)
    val transition = state.transitionState
    LaunchedEffect(transition) {
        if (transition is NavigationEventTransitionState.InProgress) progress(transition.latestEvent.progress)
    }
    NavigationBackHandler(
        state = state,
        isBackEnabled = enabled,
        onBackCancelled = onCancel,
        onBackCompleted = onBack
    )
}
