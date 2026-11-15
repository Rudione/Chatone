package io.rudione.chatone.presentation.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import io.rudione.chatone.presentation.chat.rendering.ChatSelectionContainer
import io.rudione.chatone.util.platform.setText
import io.rudione.chatone.util.system.isDesktopPlatform
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun ProfileHistorySelection(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    if (isDesktopPlatform) {
        ChatSelectionContainer(modifier, content)
    } else {
        DisableSelection { Box(modifier) { content() } }
    }
}

@Composable
internal fun ProfileMessageCopy(text: String, content: @Composable (() -> Unit) -> Unit) {
    val clipboard = LocalClipboard.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var copyCount by remember(text) { mutableIntStateOf(0) }
    var copied by remember(text) { mutableStateOf(false) }
    val feedback by animateColorAsState(
        if (copied) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
    )
    LaunchedEffect(copyCount) {
        if (copyCount > 0) {
            copied = true
            delay(800)
            copied = false
        }
    }
    Box(Modifier.background(feedback)) {
        content {
            scope.launch {
                clipboard.setText(text)
                if (!isDesktopPlatform) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                copyCount++
            }
        }
    }
}
