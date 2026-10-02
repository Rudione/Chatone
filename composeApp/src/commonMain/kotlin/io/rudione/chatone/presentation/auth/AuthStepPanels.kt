package io.rudione.chatone.presentation.auth

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.ic_twitch
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.repository.DeviceAuthState
import io.rudione.chatone.data.repository.LoginFailure
import io.rudione.chatone.data.repository.RightsHandoff
import io.rudione.chatone.data.repository.WebLoginController
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.ClipboardPaste
import io.rudione.chatone.icons.lucide.ExternalLink
import io.rudione.chatone.icons.lucide.Globe
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.RefreshCw
import io.rudione.chatone.icons.lucide.TriangleAlert
import io.rudione.chatone.presentation.components.ChatoneSecretField
import io.rudione.chatone.presentation.components.expressive.GlassIconButton
import io.rudione.chatone.presentation.components.expressive.HeroPillButton
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.PillTone
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import org.jetbrains.compose.resources.vectorResource
import io.rudione.chatone.presentation.theme.i18n.format

@Composable
internal fun StepHeading(title: String, body: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )
    Text(
        body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)
    )
}

@Composable
internal fun ColumnScope.SignInPanel(state: AuthState, actions: AuthActions) {
    val s = LocalStrings.current
    StepHeading(s.authSignInTitle, if (state.automaticReturn) s.authSignInBody else s.authSignInBodyManual)
    PanelGap()
    HeroPillButton(
        text = s.authSignInAction,
        onClick = actions.onStartLogin,
        icon = vectorResource(Res.drawable.ic_twitch),
        loading = state.isPreparing,
        modifier = Modifier.fillMaxWidth()
    )
    when {
        state.isPreparing && state.prepareAttempt > 1 -> StatusChip(
            s.format(s.loginRetrying, state.prepareAttempt, WebLoginController.DEVICE_ATTEMPTS),
            progress = true
        )
        state.failure != null -> ErrorChip(s.loginFailureText(state.failure))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ColumnScope.PastePanel(state: AuthState, paste: LoginPasteWatcher, actions: AuthActions) {
    val s = LocalStrings.current
    val busy = state.isPreparing || state.isVerifying
    var manualOpen by remember { mutableStateOf(false) }
    val automatic = state.automaticReturn
    if (automatic) {
        StepHeading(s.authBrowserTitle, s.authBrowserBody)
        PanelGap()
        PillButton(
            text = s.authReopen,
            onClick = actions.onReopenBrowser,
            icon = Lucide.RefreshCw,
            tone = PillTone.Tonal,
            loading = state.isVerifying,
            enabled = !state.isPreparing,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        StepHeading(s.authPasteTitle, s.authPasteBody)
        PanelGap()
        PillButton(
            text = s.authPasteAction,
            onClick = paste::pasteFromClipboard,
            icon = Lucide.ClipboardPaste,
            loading = state.isVerifying,
            enabled = !state.isPreparing,
            modifier = Modifier.fillMaxWidth()
        )
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)
    ) {
        if (automatic) {
            PillButton(s.authPasteManual, paste::pasteFromClipboard, icon = Lucide.ClipboardPaste, tone = PillTone.Ghost, enabled = !busy, height = 44.dp)
        } else {
            PillButton(s.authReopen, actions.onReopenBrowser, icon = Lucide.RefreshCw, tone = PillTone.Ghost, enabled = !busy, height = 44.dp)
        }
        PillButton(s.authMirror, actions.onOpenMirror, icon = Lucide.Globe, tone = PillTone.Ghost, enabled = !busy, height = 44.dp)
        PillButton(s.authManualEntry, { manualOpen = !manualOpen }, tone = PillTone.Ghost, enabled = !busy, height = 44.dp)
    }
    AnimatedVisibility(
        visible = manualOpen,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        ChatoneSecretField(
            value = paste.manualPayload,
            onValueChange = paste::type,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            label = s.loginPasteManualLabel,
            enabled = !busy,
            trailing = {
                if (paste.manualPayload.isNotBlank() && !busy) {
                    GlassIconButton(
                        icon = Lucide.Check,
                        contentDescription = s.loginPasteManualLabel,
                        onClick = paste::submitManual,
                        size = 40.dp
                    )
                }
            }
        )
    }
    val failure = state.failure
    when {
        state.isVerifying && state.verifyAttempt > 1 -> StatusChip(
            s.format(s.loginRetrying, state.verifyAttempt, WebLoginController.VERIFY_ATTEMPTS),
            progress = true
        )
        state.isVerifying -> StatusChip(s.loginVerifying, progress = true)
        failure != null -> ErrorChip(s.loginFailureText(failure))
        paste.codeAccepted -> StatusChip(s.loginCodeAccepted, icon = Lucide.Check)
    }
    if (failure == LoginFailure.Network) {
        PillButton(
            text = s.loginRetryNow,
            onClick = actions.onRetryPayload,
            icon = Lucide.RefreshCw,
            tone = PillTone.Tonal,
            enabled = !busy,
            height = 48.dp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
internal fun ColumnScope.RightsPanel(state: AuthState, actions: AuthActions) {
    val s = LocalStrings.current
    val device = state.deviceState
    val failed = state.failure == LoginFailure.RightsNotGranted
    val onSite = device == DeviceAuthState.ConfirmingOnLoginPage
    val inBrowser = state.rightsHandoff == RightsHandoff.InBrowser
    StepHeading(
        s.authRightsTitle,
        when {
            onSite -> s.authRightsOnSite
            inBrowser -> s.authRightsInBrowser
            else -> s.authRightsBody
        }
    )
    PanelGap()
    when {
        failed -> PillButton(
            text = s.loginRetryNow,
            onClick = actions.onRetryRights,
            icon = Lucide.RefreshCw,
            modifier = Modifier.fillMaxWidth()
        )
        !onSite -> PillButton(
            text = if (inBrowser) s.authReopen else s.loginOpenActivation,
            onClick = actions.onOpenActivation,
            icon = if (inBrowser) Lucide.RefreshCw else Lucide.ExternalLink,
            tone = if (inBrowser) PillTone.Tonal else PillTone.Primary,
            loading = state.activationUrl == null,
            modifier = Modifier.fillMaxWidth()
        )
    }
    PillButton(
        text = s.authSkip,
        onClick = actions.onContinueWithoutRights,
        tone = PillTone.Ghost,
        height = 44.dp,
        modifier = Modifier.padding(top = 6.dp)
    )
    when {
        failed -> ErrorChip(s.authRightsFailed)
        device == DeviceAuthState.Validating -> StatusChip(s.loginVerifying, progress = true)
        state.activationUrl == null && !onSite -> StatusChip(s.authPreparingLink, progress = true)
        else -> StatusChip(s.authWaiting, progress = true)
    }
}

@Composable
private fun PanelGap() {
    Spacer(Modifier.height(28.dp))
}

@Composable
internal fun StatusChip(text: String, progress: Boolean = false, icon: ImageVector? = null) {
    Chip(
        text = text,
        tint = MaterialTheme.colorScheme.primary,
        leading = when {
            progress -> {
                { CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary) }
            }
            icon != null -> {
                { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp)) }
            }
            else -> null
        }
    )
}

@Composable
internal fun ErrorChip(text: String) {
    Chip(
        text = text,
        tint = MaterialTheme.colorScheme.error,
        leading = { Icon(Lucide.TriangleAlert, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp)) },
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun Chip(
    text: String,
    tint: Color,
    leading: (@Composable () -> Unit)?,
    shape: Shape = CircleShape
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(140)) },
        label = "authChip",
        modifier = Modifier.padding(top = 16.dp)
    ) { message ->
        Row(
            modifier = Modifier
                .clip(shape)
                .background(tint.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leading?.invoke()
            Text(
                message,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
internal fun CheckingPanel() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            LocalStrings.current.loading,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
