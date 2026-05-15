package io.rudione.chatone.presentation.chat.multichat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier


val LocalSharedBackgroundActive = staticCompositionLocalOf { false }


@Composable
fun SharedChatBackgroundProvider(
    background: @Composable () -> Unit,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        background()
        CompositionLocalProvider(LocalSharedBackgroundActive provides true) {
            content()
        }
    }
}
