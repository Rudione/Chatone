package io.rudione.chatone.presentation.browse

import androidx.compose.runtime.Composable

@Composable
actual fun BrowseOverlay(
    visible: Boolean,
    onClose: () -> Unit,
    onOpen: (BrowseOpenRequest) -> Unit
) = Unit
