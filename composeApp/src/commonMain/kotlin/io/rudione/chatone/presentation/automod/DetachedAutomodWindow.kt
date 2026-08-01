package io.rudione.chatone.presentation.automod

import androidx.compose.runtime.Composable

@Composable
expect fun DetachedAutomodWindow(
    currentChannelLogin: String?,
    onClose: () -> Unit
)
