package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.gestures.ScrollableState

@Stable
class ScrollActivity {
    var isScrolling by mutableStateOf(false)
        internal set
}

val LocalScrollActivity = staticCompositionLocalOf { ScrollActivity() }

@Composable
fun rememberScrollActivity(state: ScrollableState): ScrollActivity {
    val activity = remember { ScrollActivity() }
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.collect { activity.isScrolling = it }
    }
    return activity
}
