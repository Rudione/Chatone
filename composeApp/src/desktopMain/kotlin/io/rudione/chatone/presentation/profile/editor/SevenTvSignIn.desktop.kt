package io.rudione.chatone.presentation.profile.editor

import androidx.compose.runtime.Composable

actual val sevenTvSignInSupported: Boolean = false

@Composable
actual fun SevenTvSignInDialog(onToken: (String) -> Unit, onDismiss: () -> Unit) = Unit
