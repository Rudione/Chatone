package io.rudione.chatone.presentation.chat

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

@Composable
expect fun UserProfileContainer(
    isPinned: Boolean,
    width: Dp,
    height: Dp,
    onResize: (Dp, Dp) -> Unit,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
)
