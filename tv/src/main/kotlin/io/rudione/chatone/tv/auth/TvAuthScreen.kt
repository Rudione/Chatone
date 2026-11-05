package io.rudione.chatone.tv.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.tv_auth_body
import chatone.composeapp.generated.resources.tv_auth_failed
import chatone.composeapp.generated.resources.tv_auth_guest
import chatone.composeapp.generated.resources.tv_auth_open_hint
import chatone.composeapp.generated.resources.tv_auth_rights_body
import chatone.composeapp.generated.resources.tv_auth_rights_skip
import chatone.composeapp.generated.resources.tv_auth_rights_title
import chatone.composeapp.generated.resources.tv_auth_scan
import chatone.composeapp.generated.resources.tv_auth_title
import io.rudione.chatone.data.repository.DeviceActivation
import io.rudione.chatone.presentation.auth.DeviceLoginEffect
import io.rudione.chatone.presentation.auth.DeviceLoginEvent
import io.rudione.chatone.presentation.auth.DeviceLoginState
import io.rudione.chatone.presentation.auth.DeviceLoginStep
import io.rudione.chatone.presentation.auth.DeviceLoginViewModel
import io.rudione.chatone.presentation.components.SystemBackHandler
import io.rudione.chatone.presentation.components.expressive.ChatoneMark
import io.rudione.chatone.tv.components.TvButtonTone
import io.rudione.chatone.tv.components.TvLoading
import io.rudione.chatone.tv.components.TvMessage
import io.rudione.chatone.tv.components.TvPillButton
import io.rudione.chatone.tv.components.TvQrCode
import io.rudione.chatone.tv.theme.TvDimens
import io.rudione.chatone.tv.theme.TvTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private val QrTileShape = RoundedCornerShape(32.dp)

@Composable
fun TvAuthScreen(onAuthenticated: () -> Unit) {
    TvTheme {
        val viewModel: DeviceLoginViewModel = koinViewModel()
        val state by viewModel.state.collectAsState()
        LaunchedEffect(viewModel) {
            viewModel.sendEvent(DeviceLoginEvent.Start)
            viewModel.effect.collect { effect ->
                when (effect) {
                    DeviceLoginEffect.SignedIn -> onAuthenticated()
                }
            }
        }
        TvAuthContent(
            state = state,
            onRetry = { viewModel.sendEvent(DeviceLoginEvent.Retry) },
            onSkipRights = { viewModel.sendEvent(DeviceLoginEvent.SkipRights) },
            onGuest = {
                viewModel.sendEvent(DeviceLoginEvent.Cancel)
                onAuthenticated()
            }
        )
    }
}

@Composable
private fun TvAuthContent(
    state: DeviceLoginState,
    onRetry: () -> Unit,
    onSkipRights: () -> Unit,
    onGuest: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        scheme.primaryContainer,
                        scheme.background,
                        scheme.background
                    )
                )
            )
            .padding(horizontal = TvDimens.ScreenHorizontal, vertical = TvDimens.ScreenVertical),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(
                ChatoneMark,
                contentDescription = null,
                tint = scheme.primary,
                modifier = Modifier.size(72.dp)
            )
            val rights = state.step == DeviceLoginStep.RIGHTS
            Text(
                stringResource(if (rights) Res.string.tv_auth_rights_title else Res.string.tv_auth_title),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(if (rights) Res.string.tv_auth_rights_body else Res.string.tv_auth_body),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurfaceVariant
            )
            val focus = remember { FocusRequester() }
            TvPillButton(
                stringResource(if (rights) Res.string.tv_auth_rights_skip else Res.string.tv_auth_guest),
                onClick = if (rights) onSkipRights else onGuest,
                tone = TvButtonTone.Tonal,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .focusRequester(focus)
            )
            LaunchedEffect(rights) { focus.requestFocus() }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight(), contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = state.activation,
                contentKey = { it?.userCode },
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(160)) },
                label = "tvAuthCode"
            ) { activation ->
                when {
                    state.failure != null -> TvMessage(
                        title = stringResource(Res.string.tv_auth_failed),
                        onRetry = onRetry
                    )

                    activation != null -> TvActivationCard(activation)
                    else -> TvLoading()
                }
            }
        }
    }
}

@Composable
private fun TvActivationCard(activation: DeviceActivation) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            Modifier
                .size(248.dp)
                .background(Color.White, QrTileShape)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            TvQrCode(activation.activationUrl, Modifier.fillMaxSize())
        }
        Text(
            stringResource(Res.string.tv_auth_scan),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            formatUserCode(activation.userCode),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            letterSpacing = 4.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            stringResource(Res.string.tv_auth_open_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TvRightsDialog(onDone: () -> Unit) {
    val viewModel: DeviceLoginViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(viewModel) {
        viewModel.sendEvent(DeviceLoginEvent.StartRights)
        viewModel.effect.collect { effect ->
            when (effect) {
                DeviceLoginEffect.SignedIn -> done()
            }
        }
    }
    SystemBackHandler {
        viewModel.sendEvent(DeviceLoginEvent.SkipRights)
        onDone()
    }
    TvAuthContent(
        state = state,
        onRetry = { viewModel.sendEvent(DeviceLoginEvent.StartRights) },
        onSkipRights = {
            viewModel.sendEvent(DeviceLoginEvent.SkipRights)
            onDone()
        },
        onGuest = onDone
    )
}

internal fun formatUserCode(code: String): String {
    val middle = code.length / 2
    return if (code.length >= 6) "${code.take(middle)} ${code.drop(middle)}" else code
}
