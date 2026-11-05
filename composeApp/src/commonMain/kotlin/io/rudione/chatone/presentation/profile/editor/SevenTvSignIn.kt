package io.rudione.chatone.presentation.profile.editor

import androidx.compose.runtime.Composable

expect val sevenTvSignInSupported: Boolean

@Composable
expect fun SevenTvSignInDialog(onToken: (String) -> Unit, onDismiss: () -> Unit)
