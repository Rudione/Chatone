package io.rudione.chatone.presentation.components

import androidx.compose.runtime.Composable

@Composable
actual fun SystemPredictiveBackHandler(
    enabled: Boolean,
    onProgress: (Float) -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit
) = Unit
