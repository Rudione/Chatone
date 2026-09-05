package io.rudione.chatone.presentation.account

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.repository.WebLoginController
import io.rudione.chatone.data.repository.WebLoginStage
import io.rudione.chatone.domain.model.TwitchAccount
import io.rudione.chatone.presentation.auth.WebLoginPanel
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import org.koin.compose.koinInject

@Composable
fun AccountAddDialog(
    onDismiss: () -> Unit,
    onAccountAdded: (TwitchAccount) -> Unit,
    controller: WebLoginController = koinInject()
) {
    val strings = LocalStrings.current
    val uriHandler = LocalUriHandler.current
    val stage by controller.stage.collectAsState()
    val deviceState by controller.deviceAuthState.collectAsState()

    LaunchedEffect(Unit) { controller.reset() }

    LaunchedEffect(stage) {
        val current = stage
        if (current is WebLoginStage.Success) {
            onAccountAdded(current.account)
            controller.reset()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            controller.reset()
            onDismiss()
        },
        title = { Text(strings.accountsAdd) },
        text = {
            Column(modifier = Modifier.widthIn(min = 340.dp, max = 460.dp)) {
                Text(
                    text = strings.loginOpenSiteHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                WebLoginPanel(
                    awaitingPaste = stage is WebLoginStage.AwaitingPaste ||
                            stage is WebLoginStage.Verifying ||
                            stage is WebLoginStage.Failure,
                    isPreparing = stage is WebLoginStage.Preparing,
                    isVerifying = stage is WebLoginStage.Verifying,
                    deviceState = deviceState,
                    failure = (stage as? WebLoginStage.Failure)?.reason,
                    onStartLogin = { controller.begin { url -> uriHandler.openUri(url) } },
                    onReopenBrowser = { controller.loginUrl.value?.let(uriHandler::openUri) },
                    onSubmitPayload = { payload, auto -> controller.submit(payload, auto) },
                    prepareAttempt = (stage as? WebLoginStage.Preparing)?.attempt ?: 1,
                    verifyAttempt = (stage as? WebLoginStage.Verifying)?.attempt ?: 1,
                    onRetryPayload = { controller.retryPendingPayload() },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = {
                    controller.reset()
                    onDismiss()
                }
            ) { Text(strings.cancel) }
        }
    )
}
