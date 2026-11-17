package io.rudione.chatone.presentation.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import org.jetbrains.compose.resources.stringResource

private val ConfirmShape =
    RoundedCornerShape(topStart = 24.dp, topEnd = 8.dp, bottomEnd = 24.dp, bottomStart = 8.dp)

@Composable
fun ConfirmActionDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
    enabled: Boolean = true
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = ConfirmShape,
        title = { Text(title, color = MaterialTheme.colorScheme.onSurface) },
        text = { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        confirmButton = {
            TextButton(enabled = enabled, onClick = { onDismiss(); onConfirm() }) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } }
    )
}

@Immutable
class ConfirmRequest(
    val title: String,
    val text: String,
    val confirmLabel: String,
    val destructive: Boolean = true,
    val action: () -> Unit
)

@Composable
fun rememberConfirmGate(): ConfirmGate = remember { ConfirmGate() }

class ConfirmGate {
    var pending by mutableStateOf<ConfirmRequest?>(null)
        private set

    fun ask(request: ConfirmRequest) {
        pending = request
    }

    fun dismiss() {
        pending = null
    }
}

@Composable
fun ConfirmGateHost(gate: ConfirmGate) {
    val request = gate.pending ?: return
    ConfirmActionDialog(
        title = request.title,
        text = request.text,
        confirmLabel = request.confirmLabel,
        destructive = request.destructive,
        onConfirm = request.action,
        onDismiss = gate::dismiss
    )
}
