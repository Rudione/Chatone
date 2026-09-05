package io.rudione.chatone.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.aakira.napier.Napier
import io.rudione.chatone.presentation.components.LiquidGlassSurface
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import org.koin.compose.viewmodel.koinViewModel

private enum class AuthPhase { CHECKING, CONTENT }

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current

    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary

    val fadeIn by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, easing = EaseOutCubic),
        label = "screenFadeIn"
    )

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AuthEffect.NavigateToHome -> {
                    Napier.d("Auth successful, navigating to home")
                    onAuthSuccess()
                }

                is AuthEffect.OpenAuthUrl -> uriHandler.openUri(effect.url)
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .alpha(fadeIn)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.15f),
                        backgroundColor.copy(alpha = 0.9f),
                        backgroundColor
                    ),
                    center = Offset(0f, -200f),
                    radius = 600f
                )
            ),
        color = Color.Transparent
    ) {
        DecorativeBackgroundOrbs()

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val phase = if (state.isCheckingToken) AuthPhase.CHECKING else AuthPhase.CONTENT
            AnimatedContent(targetState = phase, label = "authStateTransition") { current ->
                when (current) {
                    AuthPhase.CHECKING -> CheckingAuthState()
                    AuthPhase.CONTENT -> AuthContentState(
                        state = state,
                        onStartLogin = { viewModel.sendEvent(AuthEvent.OnStartLogin) },
                        onReopenBrowser = { viewModel.sendEvent(AuthEvent.OnReopenBrowser) },
                        onSubmitPayload = { payload, auto ->
                            viewModel.sendEvent(AuthEvent.OnPastePayload(payload, auto))
                        },
                        onRetryPayload = { viewModel.sendEvent(AuthEvent.OnRetryPayload) },
                        onGuestClick = { viewModel.sendEvent(AuthEvent.OnGuestClicked) },
                        onContinueWithoutRights = {
                            viewModel.sendEvent(AuthEvent.OnContinueWithoutRights)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthContentState(
    state: AuthState,
    onStartLogin: () -> Unit,
    onReopenBrowser: () -> Unit,
    onSubmitPayload: (String, Boolean) -> Unit,
    onRetryPayload: () -> Unit,
    onGuestClick: () -> Unit,
    onContinueWithoutRights: () -> Unit
) {
    val s = LocalStrings.current
    val uriHandler = LocalUriHandler.current

    LiquidGlassSurface(
        modifier = Modifier
            .padding(24.dp)
            .widthIn(max = 460.dp),
        backgroundAlphaHigh = 0.88f,
        backgroundAlphaLow = 0.75f,
        borderAlphaHigh = 0.25f,
        borderAlphaLow = 0.08f
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Chatone",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = s.authSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))

            WebLoginPanel(
                awaitingPaste = state.awaitingPaste,
                isPreparing = state.isPreparing,
                isVerifying = state.isVerifying,
                deviceState = state.deviceState,
                failure = state.failure,
                onStartLogin = onStartLogin,
                onReopenBrowser = onReopenBrowser,
                onSubmitPayload = onSubmitPayload,
                prepareAttempt = state.prepareAttempt,
                verifyAttempt = state.verifyAttempt,
                awaitingRights = state.awaitingRights,
                onRetryPayload = onRetryPayload,
                onContinueWithoutRights = onContinueWithoutRights
            )

            Spacer(Modifier.height(16.dp))

            TextButton(onClick = onGuestClick) {
                Text(s.chatGuestMode, style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(8.dp))

            LiquidGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                backgroundAlphaHigh = 0.7f,
                backgroundAlphaLow = 0.5f,
                borderAlphaHigh = 0.15f,
                borderAlphaLow = 0.03f,
                contentPadding = PaddingValues(14.dp)
            ) {
                Column {
                    Text(
                        text = s.loginSupport,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "t.me/rudionee",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { uriHandler.openUri("https://t.me/rudionee") }
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckingAuthState() {
    val s = LocalStrings.current
    LiquidGlassSurface(
        modifier = Modifier
            .padding(24.dp)
            .widthIn(max = 400.dp),
        backgroundAlphaHigh = 0.85f,
        backgroundAlphaLow = 0.70f
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = s.loading,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DecorativeBackgroundOrbs() {
    val infiniteTransition = rememberInfiniteTransition(label = "backgroundOrbs")

    val orb1Offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1Float"
    )

    val orb2Offset by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2Float"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(y = (orb1Offset * 100).dp)
                .size(200.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(y = (orb2Offset * -80).dp)
                .size(160.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(50)
                )
        )
    }
}
