package io.rudione.chatone.presentation.auth

import chatone.composeapp.generated.resources.login_err_clipboard_empty
import chatone.composeapp.generated.resources.login_err_decrypt
import chatone.composeapp.generated.resources.login_err_encryption_unsupported
import chatone.composeapp.generated.resources.login_err_foreign_client
import chatone.composeapp.generated.resources.login_err_identity
import chatone.composeapp.generated.resources.login_err_incomplete
import chatone.composeapp.generated.resources.login_err_malformed
import chatone.composeapp.generated.resources.login_err_network
import chatone.composeapp.generated.resources.login_err_rights_not_granted
import chatone.composeapp.generated.resources.login_err_token_rejected
import chatone.composeapp.generated.resources.login_err_unsafe_url
import org.jetbrains.compose.resources.StringResource
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.login_auto_watch
import chatone.composeapp.generated.resources.login_awaiting_rights
import chatone.composeapp.generated.resources.login_code_accepted
import chatone.composeapp.generated.resources.login_continue_without_rights
import chatone.composeapp.generated.resources.login_mod_failed
import chatone.composeapp.generated.resources.login_mod_linked
import chatone.composeapp.generated.resources.login_mod_pending
import chatone.composeapp.generated.resources.login_open_activation
import chatone.composeapp.generated.resources.login_open_mirror
import chatone.composeapp.generated.resources.login_open_site
import chatone.composeapp.generated.resources.login_open_site_hint
import chatone.composeapp.generated.resources.login_paste_from_clipboard
import chatone.composeapp.generated.resources.login_paste_manual_hint
import chatone.composeapp.generated.resources.login_paste_manual_label
import chatone.composeapp.generated.resources.login_preparing
import chatone.composeapp.generated.resources.login_reopen_site
import chatone.composeapp.generated.resources.login_retry_now
import chatone.composeapp.generated.resources.login_retry_rights
import chatone.composeapp.generated.resources.login_retrying
import chatone.composeapp.generated.resources.login_rights_on_site
import chatone.composeapp.generated.resources.login_step_confirm
import chatone.composeapp.generated.resources.login_step_open
import chatone.composeapp.generated.resources.login_step_paste
import chatone.composeapp.generated.resources.login_verifying
import org.jetbrains.compose.resources.stringResource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.filled.Check
import io.rudione.chatone.icons.material.filled.ContentPaste
import io.rudione.chatone.icons.material.filled.OpenInNew
import io.rudione.chatone.icons.material.filled.Visibility
import io.rudione.chatone.icons.material.filled.VisibilityOff
import io.rudione.chatone.icons.material.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.repository.DeviceAuthState
import io.rudione.chatone.data.repository.LoginFailure
import io.rudione.chatone.data.repository.WebLoginController
import io.rudione.chatone.presentation.components.ChatoneButtonText

fun LoginFailure.messageRes(): StringResource = when (this) {
    LoginFailure.ClipboardEmpty -> Res.string.login_err_clipboard_empty
    LoginFailure.Malformed -> Res.string.login_err_malformed
    LoginFailure.DecryptionFailed -> Res.string.login_err_decrypt
    LoginFailure.Incomplete -> Res.string.login_err_incomplete
    LoginFailure.ForeignClientId -> Res.string.login_err_foreign_client
    LoginFailure.IdentityMismatch -> Res.string.login_err_identity
    LoginFailure.TokenRejected -> Res.string.login_err_token_rejected
    LoginFailure.EncryptionUnsupported -> Res.string.login_err_encryption_unsupported
    LoginFailure.UnsafeLoginUrl -> Res.string.login_err_unsafe_url
    LoginFailure.RightsNotGranted -> Res.string.login_err_rights_not_granted
    LoginFailure.Network -> Res.string.login_err_network
}

@Composable
fun loginFailureText(failure: LoginFailure): String = stringResource(failure.messageRes())

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WebLoginPanel(
    awaitingPaste: Boolean,
    isPreparing: Boolean,
    isVerifying: Boolean,
    deviceState: DeviceAuthState,
    failure: LoginFailure?,
    onStartLogin: () -> Unit,
    onReopenBrowser: () -> Unit,
    onSubmitPayload: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    prepareAttempt: Int = 1,
    verifyAttempt: Int = 1,
    awaitingRights: Boolean = false,
    onRetryPayload: (() -> Unit)? = null,
    onContinueWithoutRights: (() -> Unit)? = null,
    onOpenMirror: (() -> Unit)? = null,
    onRetryRights: (() -> Unit)? = null,
    onOpenActivation: (() -> Unit)? = null
) {
    val busy = isPreparing || isVerifying
    val paste = rememberLoginPasteWatcher(awaitingPaste, busy, failure, onSubmitPayload)
    val codeAccepted = paste.codeAccepted

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoginStepRow(
            currentStep = when {
                awaitingRights || failure == LoginFailure.RightsNotGranted -> 3
                awaitingPaste -> 2
                else -> 1
            }
        )

        Spacer(Modifier.height(14.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = if (awaitingPaste) onReopenBrowser else onStartLogin,
                enabled = !busy,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (awaitingPaste) stringResource(Res.string.login_reopen_site) else stringResource(Res.string.login_open_site),
                    maxLines = 2
                )
            }

            OutlinedButton(
                onClick = paste::pasteFromClipboard,
                enabled = !busy,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.login_paste_from_clipboard), maxLines = 2)
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = when {
                isPreparing && prepareAttempt > 1 ->
                    stringResource(Res.string.login_retrying, prepareAttempt, WebLoginController.DEVICE_ATTEMPTS)

                isPreparing -> stringResource(Res.string.login_preparing)
                isVerifying && verifyAttempt > 1 ->
                    stringResource(Res.string.login_retrying, verifyAttempt, WebLoginController.VERIFY_ATTEMPTS)

                isVerifying -> stringResource(Res.string.login_verifying)
                codeAccepted -> stringResource(Res.string.login_code_accepted)
                awaitingPaste -> stringResource(Res.string.login_auto_watch)
                else -> stringResource(Res.string.login_open_site_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (awaitingPaste && onOpenMirror != null) {
            TextButton(onClick = onOpenMirror, enabled = !busy) {
                Text(
                    text = stringResource(Res.string.login_open_mirror),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
        }

        AnimatedVisibility(visible = awaitingPaste) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                io.rudione.chatone.presentation.components.ChatoneSecretField(
                    value = paste.manualPayload,
                    onValueChange = paste::type,
                    modifier = Modifier.fillMaxWidth(),
                    label = stringResource(Res.string.login_paste_manual_label),
                    hint = stringResource(Res.string.login_paste_manual_hint),
                    enabled = !busy,
                    trailing = {
                        if (paste.manualPayload.isNotBlank()) {
                            OutlinedButton(
                                onClick = paste::submitManual,
                                enabled = !busy,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Check, null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                )
            }
        }

        if (failure != LoginFailure.RightsNotGranted) {
            if (awaitingPaste || awaitingRights) DeviceAuthStatus(deviceState = deviceState)
        }

        if (awaitingRights) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                Text(
                    if (deviceState == DeviceAuthState.ConfirmingOnLoginPage) stringResource(Res.string.login_rights_on_site) else stringResource(Res.string.login_awaiting_rights),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (onOpenActivation != null) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = onOpenActivation, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    ChatoneButtonText(stringResource(Res.string.login_open_activation))
                }
            }
        }

        if (codeAccepted && failure == null && !busy) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    Icons.Filled.Check,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    stringResource(Res.string.login_code_accepted),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (failure != null) {
            Spacer(Modifier.height(12.dp))
            LoginErrorBanner(message = loginFailureText(failure))
        }

        if (failure == LoginFailure.Network && onRetryPayload != null) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRetryPayload,
                enabled = !busy,
                shape = RoundedCornerShape(12.dp)
            ) {
                ChatoneButtonText(stringResource(Res.string.login_retry_now))
            }
        }

        if (failure == LoginFailure.RightsNotGranted && onRetryRights != null) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetryRights, shape = RoundedCornerShape(12.dp)) {
                ChatoneButtonText(stringResource(Res.string.login_retry_rights))
            }
        }

        if (onContinueWithoutRights != null && (awaitingRights || failure == LoginFailure.RightsNotGranted)) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onContinueWithoutRights, shape = RoundedCornerShape(12.dp)) {
                ChatoneButtonText(stringResource(Res.string.login_continue_without_rights))
            }
        }

        if (busy) {
            Spacer(Modifier.height(12.dp))
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun LoginStepRow(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        listOf(stringResource(Res.string.login_step_open), stringResource(Res.string.login_step_paste), stringResource(Res.string.login_step_confirm)).forEachIndexed { index, label ->
            val step = index + 1
            LoginStepChip(
                index = step,
                label = label,
                active = step == currentStep,
                done = step < currentStep,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LoginStepChip(index: Int, label: String, active: Boolean, done: Boolean, modifier: Modifier = Modifier) {
    val container by animateColorAsState(
        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        label = "loginStepContainer"
    )
    val content by animateColorAsState(
        when {
            active -> MaterialTheme.colorScheme.primary
            done -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "loginStepContent"
    )
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(content.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Filled.Check, null, tint = content, modifier = Modifier.size(12.dp))
            } else {
                Text(
                    index.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = content
                )
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DeviceAuthStatus(deviceState: DeviceAuthState) {
    var codeRevealed by remember(deviceState) { mutableStateOf(false) }
    val (text, tint) = when (deviceState) {
        is DeviceAuthState.WaitingForApproval -> {
            val code = if (codeRevealed) {
                deviceState.userCode
            } else {
                "•".repeat(deviceState.userCode.length.coerceIn(4, 10))
            }
            stringResource(Res.string.login_mod_pending, code) to MaterialTheme.colorScheme.tertiary
        }

        DeviceAuthState.Validating ->
            stringResource(Res.string.login_verifying) to MaterialTheme.colorScheme.tertiary

        is DeviceAuthState.Success ->
            stringResource(Res.string.login_mod_linked, deviceState.displayName) to MaterialTheme.colorScheme.primary

        is DeviceAuthState.Error ->
            stringResource(Res.string.login_mod_failed) to MaterialTheme.colorScheme.onSurfaceVariant

        DeviceAuthState.Idle, DeviceAuthState.ConfirmingOnLoginPage -> return
    }

    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        if (deviceState is DeviceAuthState.Success) {
            Icon(Icons.Filled.Check, null, tint = tint, modifier = Modifier.size(14.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint)
        if (deviceState is DeviceAuthState.WaitingForApproval) {
            Icon(
                imageVector = if (codeRevealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { codeRevealed = !codeRevealed }
            )
        }
    }
}

@Composable
private fun LoginErrorBanner(message: String) {
    val error = MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(error.copy(alpha = 0.10f))
            .border(1.dp, error.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Filled.Warning, null, tint = error, modifier = Modifier.size(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
