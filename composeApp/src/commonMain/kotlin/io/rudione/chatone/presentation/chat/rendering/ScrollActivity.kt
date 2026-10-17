package io.rudione.chatone.presentation.chat.rendering

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
class ScrollActivity internal constructor(private val source: ScrollableState?) {
    val isScrolling: Boolean get() = source?.isScrollInProgress == true
}

val LocalScrollActivity = staticCompositionLocalOf { ScrollActivity(null) }

@Composable
fun rememberScrollActivity(state: ScrollableState): ScrollActivity = remember(state) { ScrollActivity(state) }
