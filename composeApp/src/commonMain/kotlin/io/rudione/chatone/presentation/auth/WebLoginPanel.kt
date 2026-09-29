package io.rudione.chatone.presentation.auth

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.auth.LoginPayloadCodec
import io.rudione.chatone.data.repository.DeviceAuthState
import io.rudione.chatone.data.repository.LoginFailure
import io.rudione.chatone.data.repository.WebLoginController
import io.rudione.chatone.presentation.components.ChatoneButtonText
import io.rudione.chatone.presentation.theme.i18n.AppStrings
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.presentation.theme.i18n.format
import io.rudione.chatone.util.platform.DeviceFormFactor
import io.rudione.chatone.util.platform.currentFormFactor
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun AppStrings.loginFailureText(failure: LoginFailure): String = when (failure) {
    LoginFailure.ClipboardEmpty -> loginErrClipboardEmpty
    LoginFailure.Malformed -> loginErrMalformed
    LoginFailure.DecryptionFailed -> loginErrDecrypt
    LoginFailure.Incomplete -> loginErrIncomplete
    LoginFailure.ForeignClientId -> loginErrForeignClient
    LoginFailure.IdentityMismatch -> loginErrIdentity
    LoginFailure.TokenRejected -> loginErrTokenRejected
    LoginFailure.EncryptionUnsupported -> loginErrEncryptionUnsupported
    LoginFailure.UnsafeLoginUrl -> loginErrUnsafeUrl
    LoginFailure.RightsNotGranted -> loginErrRightsNotGranted
    LoginFailure.Network -> loginErrNetwork
}

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
    val s = LocalStrings.current
    val clipboard = LocalClipboardManager.current
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val pollsClipboard = remember { currentFormFactor() == DeviceFormFactor.DESKTOP }
    var manualPayload by remember { mutableStateOf("") }
    var acceptedPayload by remember { mutableStateOf("") }
    var codeAccepted by remember { mutableStateOf(false) }

    val busy = isPreparing || isVerifying

    val accept: (String) -> Boolean = accept@{ raw ->
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed == acceptedPayload) return@accept false
        if (!LoginPayloadCodec.canAutoSubmit(trimmed)) return@accept false
        acceptedPayload = trimmed
        codeAccepted = true
        manualPayload = ""
        onSubmitPayload(trimmed, true)
        runCatching { clipboard.setText(AnnotatedString("")) }
        true
    }

    LaunchedEffect(awaitingPaste, windowFocused, busy) {
        if (!awaitingPaste || !windowFocused || busy) return@LaunchedEffect
        var reads = 0
        while (true) {
            val fromClipboard = runCatching { clipboard.getText()?.text }.getOrNull().orEmpty()
            if (accept(fromClipboard)) break
            reads++
            val limit = if (pollsClipboard) DESKTOP_CLIPBOARD_READS else FOCUS_CLIPBOARD_READS
            if (reads >= limit) break
            delay((if (pollsClipboard) DESKTOP_POLL_MS else FOCUS_POLL_MS).milliseconds)
        }
    }

    LaunchedEffect(failure) {
        if (failure != null && failure != LoginFailure.Network) codeAccepted = false
    }

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
                    text = if (awaitingPaste) s.loginReopenSite else s.loginOpenSite,
                    maxLines = 2
                )
            }

            OutlinedButton(
                onClick = {
                    val text = clipboard.getText()?.text.orEmpty()
                    if (!accept(text)) {
                        onSubmitPayload(text, false)
                        if (text.isNotBlank()) clipboard.setText(AnnotatedString(""))
                    }
                },
                enabled = !busy,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(s.loginPasteFromClipboard, maxLines = 2)
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = when {
                isPreparing && prepareAttempt > 1 ->
                    s.format(s.loginRetrying, prepareAttempt, WebLoginController.DEVICE_ATTEMPTS)

                isPreparing -> s.loginPreparing
                isVerifying && verifyAttempt > 1 ->
                    s.format(s.loginRetrying, verifyAttempt, WebLoginController.VERIFY_ATTEMPTS)

                isVerifying -> s.loginVerifying
                codeAccepted -> s.loginCodeAccepted
                awaitingPaste -> s.loginAutoWatch
                else -> s.loginOpenSiteHint
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (awaitingPaste && onOpenMirror != null) {
            TextButton(onClick = onOpenMirror, enabled = !busy) {
                Text(
                    text = s.loginOpenMirror,
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
                    value = manualPayload,
                    onValueChange = { typed -> if (!accept(typed)) manualPayload = typed },
                    modifier = Modifier.fillMaxWidth(),
                    label = s.loginPasteManualLabel,
                    hint = s.loginPasteManualHint,
                    enabled = !busy,
                    trailing = {
                        if (manualPayload.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    onSubmitPayload(manualPayload, false)
                                    manualPayload = ""
                                },
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
                    if (deviceState == DeviceAuthState.ConfirmingOnLoginPage) s.loginRightsOnSite else s.loginAwaitingRights,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (onOpenActivation != null) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = onOpenActivation, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    ChatoneButtonText(s.loginOpenActivation)
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
                    s.loginCodeAccepted,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (failure != null) {
            Spacer(Modifier.height(12.dp))
            LoginErrorBanner(message = s.loginFailureText(failure))
        }

        if (failure == LoginFailure.Network && onRetryPayload != null) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRetryPayload,
                enabled = !busy,
                shape = RoundedCornerShape(12.dp)
            ) {
                ChatoneButtonText(s.loginRetryNow)
            }
        }

        if (failure == LoginFailure.RightsNotGranted && onRetryRights != null) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetryRights, shape = RoundedCornerShape(12.dp)) {
                ChatoneButtonText(s.loginRetryRights)
            }
        }

        if (onContinueWithoutRights != null && (awaitingRights || failure == LoginFailure.RightsNotGranted)) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onContinueWithoutRights, shape = RoundedCornerShape(12.dp)) {
                ChatoneButtonText(s.loginContinueWithoutRights)
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
    val s = LocalStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        listOf(s.loginStepOpen, s.loginStepPaste, s.loginStepConfirm).forEachIndexed { index, label ->
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
    val s = LocalStrings.current
    var codeRevealed by remember(deviceState) { mutableStateOf(false) }
    val (text, tint) = when (deviceState) {
        is DeviceAuthState.WaitingForApproval -> {
            val code = if (codeRevealed) {
                deviceState.userCode
            } else {
                "•".repeat(deviceState.userCode.length.coerceIn(4, 10))
            }
            s.format(s.loginModPending, code) to MaterialTheme.colorScheme.tertiary
        }

        DeviceAuthState.Validating ->
            s.loginVerifying to MaterialTheme.colorScheme.tertiary

        is DeviceAuthState.Success ->
            s.format(s.loginModLinked, deviceState.displayName) to MaterialTheme.colorScheme.primary

        is DeviceAuthState.Error ->
            s.loginModFailed to MaterialTheme.colorScheme.onSurfaceVariant

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

private const val DESKTOP_POLL_MS = 700L
private const val FOCUS_POLL_MS = 350L
private const val FOCUS_CLIPBOARD_READS = 4
private const val DESKTOP_CLIPBOARD_READS = 430
