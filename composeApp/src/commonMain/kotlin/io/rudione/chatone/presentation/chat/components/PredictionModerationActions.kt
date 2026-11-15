package io.rudione.chatone.presentation.chat.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.mod_panel_cancel_prediction
import chatone.composeapp.generated.resources.prediction_cancel_confirmation
import chatone.composeapp.generated.resources.prediction_resolve_complete
import chatone.composeapp.generated.resources.prediction_resolve_lock
import io.rudione.chatone.presentation.components.ChatoneActionRow
import io.rudione.chatone.presentation.components.ChatoneButton
import io.rudione.chatone.presentation.components.ChatoneButtonTone
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PredictionModerationActions(
    status: String,
    enabled: Boolean,
    onLock: () -> Unit,
    onCancel: () -> Unit,
    onResolve: () -> Unit
) {
    var confirmCancel by remember { mutableStateOf(false) }
    ChatoneActionRow(horizontalSpacing = 5.dp, verticalSpacing = 5.dp) {
        if (status == "ACTIVE") ChatoneButton(
            text = stringResource(Res.string.prediction_resolve_lock),
            tone = ChatoneButtonTone.Neutral,
            enabled = enabled,
            onClick = onLock
        )
        ChatoneButton(
            text = stringResource(Res.string.mod_panel_cancel_prediction),
            tone = ChatoneButtonTone.Danger,
            enabled = enabled,
            onClick = { confirmCancel = true }
        )
        ChatoneButton(
            text = stringResource(Res.string.prediction_resolve_complete),
            tone = ChatoneButtonTone.Neutral,
            enabled = enabled,
            onClick = onResolve
        )
    }
    if (confirmCancel) AlertDialog(
        onDismissRequest = { confirmCancel = false },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomEnd = 24.dp, bottomStart = 8.dp),
        title = { Text(stringResource(Res.string.mod_panel_cancel_prediction), color = MaterialTheme.colorScheme.onSurface) },
        text = { Text(stringResource(Res.string.prediction_cancel_confirmation), color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            TextButton(enabled = enabled, onClick = { confirmCancel = false; onCancel() }) {
                Text(stringResource(Res.string.mod_panel_cancel_prediction), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text(stringResource(Res.string.cancel)) } }
    )
}
