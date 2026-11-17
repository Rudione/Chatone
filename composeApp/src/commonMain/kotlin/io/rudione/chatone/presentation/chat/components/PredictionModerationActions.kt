package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.mod_panel_cancel_prediction
import chatone.composeapp.generated.resources.prediction_cancel_confirmation
import chatone.composeapp.generated.resources.prediction_action_complete_short
import chatone.composeapp.generated.resources.prediction_action_lock_short
import io.rudione.chatone.presentation.components.ChatoneButton
import io.rudione.chatone.presentation.components.ChatoneButtonTone
import io.rudione.chatone.presentation.components.ConfirmActionDialog
import org.jetbrains.compose.resources.stringResource

private val ActionHeight = 28.dp

@Composable
internal fun PredictionModerationActions(
    status: String,
    enabled: Boolean,
    onLock: () -> Unit,
    onCancel: () -> Unit,
    onResolve: () -> Unit
) {
    var confirmCancel by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        if (status == "ACTIVE") ChatoneButton(
            text = stringResource(Res.string.prediction_action_lock_short),
            tone = ChatoneButtonTone.Neutral,
            enabled = enabled,
            height = ActionHeight,
            modifier = Modifier.weight(1f),
            onClick = onLock
        )
        ChatoneButton(
            text = stringResource(Res.string.mod_panel_cancel_prediction),
            tone = ChatoneButtonTone.Danger,
            enabled = enabled,
            height = ActionHeight,
            modifier = Modifier.weight(1f),
            onClick = { confirmCancel = true }
        )
        ChatoneButton(
            text = stringResource(Res.string.prediction_action_complete_short),
            tone = ChatoneButtonTone.Neutral,
            enabled = enabled,
            height = ActionHeight,
            modifier = Modifier.weight(1f),
            onClick = onResolve
        )
    }
    if (confirmCancel) {
        ConfirmActionDialog(
            title = stringResource(Res.string.mod_panel_cancel_prediction),
            text = stringResource(Res.string.prediction_cancel_confirmation),
            confirmLabel = stringResource(Res.string.mod_panel_cancel_prediction),
            enabled = enabled,
            onConfirm = onCancel,
            onDismiss = { confirmCancel = false }
        )
    }
}
