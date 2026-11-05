package io.rudione.chatone.presentation.chat.components

import io.rudione.chatone.presentation.components.durationText
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.cancel
import chatone.composeapp.generated.resources.chat_ban_confirm_text
import chatone.composeapp.generated.resources.chat_ban_user
import chatone.composeapp.generated.resources.chat_timeout_user
import chatone.composeapp.generated.resources.confirm
import org.jetbrains.compose.resources.stringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import io.rudione.chatone.presentation.theme.ChatoneTheme

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
    val (title, text) = when (action) {
        is PendingModAction.Timeout -> {
            val d = durationText(action.duration)
            "${stringResource(Res.string.chat_timeout_user)} ${action.displayName}?" to "${stringResource(Res.string.chat_timeout_user)}: $d"
        }

        is PendingModAction.Ban -> "${stringResource(Res.string.chat_ban_user)} ${action.displayName}?" to stringResource(Res.string.chat_ban_confirm_text)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = { Text(text) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = if (action is PendingModAction.Ban) ChatoneTheme.extraColors.modBan else ChatoneTheme.extraColors.modTimeout)
            ) { Text(stringResource(Res.string.confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } }
    )
}
