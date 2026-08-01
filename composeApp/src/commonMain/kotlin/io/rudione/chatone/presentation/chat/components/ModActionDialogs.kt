package io.rudione.chatone.presentation.chat.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

internal sealed class PendingModAction {
    data class Timeout(val userId: String, val displayName: String, val duration: Int) :
        PendingModAction()

    data class Ban(val userId: String, val displayName: String) : PendingModAction()
}

@Composable
internal fun ModActionConfirmDialog(
    action: PendingModAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val (title, text) = when (action) {
        is PendingModAction.Timeout -> {
            val d = when {
                action.duration < 60 -> "${action.duration}s"; action.duration < 3600 -> "${action.duration / 60}m"; action.duration < 86400 -> "${action.duration / 3600}h"; else -> "${action.duration / 86400}d"
            }
            "${s.chatTimeoutUser} ${action.displayName}?" to "${s.chatTimeoutUser}: $d"
        }

        is PendingModAction.Ban -> "${s.chatBanUser} ${action.displayName}?" to s.chatBanConfirmText
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(text) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = if (action is PendingModAction.Ban) ChatoneTheme.extraColors.modBan else ChatoneTheme.extraColors.modTimeout)
            ) { Text(s.confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(s.cancel) } }
    )
}
