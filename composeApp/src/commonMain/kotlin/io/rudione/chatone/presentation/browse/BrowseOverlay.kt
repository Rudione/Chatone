package io.rudione.chatone.presentation.browse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

@Immutable
data class BrowseOpenRequest(
    val login: String,
    val displayName: String,
    val avatarUrl: String,
    val live: Boolean
)

@Composable
expect fun BrowseOverlay(
    visible: Boolean,
    onClose: () -> Unit,
    onOpen: (BrowseOpenRequest) -> Unit
)
