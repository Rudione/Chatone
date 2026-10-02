package io.rudione.chatone.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import io.github.aakira.napier.Napier
import io.rudione.chatone.util.link.openAuthTab
import io.rudione.chatone.util.system.isDesktopPlatform
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    language: String = "en",
    onLanguageChange: ((String) -> Unit)? = null,
    viewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AuthEffect.NavigateToHome -> {
                    Napier.d("Auth successful, navigating to home")
                    onAuthSuccess()
                }

                is AuthEffect.OpenAuthUrl -> uriHandler.openUri(effect.url)
                is AuthEffect.OpenAuthTab -> openAuthTab(effect.url)
            }
        }
    }

    val actions = remember(viewModel, uriHandler, onLanguageChange) {
        AuthActions(
            onStartLogin = { viewModel.sendEvent(AuthEvent.OnStartLogin) },
            onReopenBrowser = { viewModel.sendEvent(AuthEvent.OnReopenBrowser) },
            onOpenMirror = { viewModel.sendEvent(AuthEvent.OnOpenMirror) },
            onSubmitPayload = { payload, auto ->
                viewModel.sendEvent(
                    AuthEvent.OnPastePayload(
                        payload,
                        auto
                    )
                )
            },
            onRetryPayload = { viewModel.sendEvent(AuthEvent.OnRetryPayload) },
            onGuest = { viewModel.sendEvent(AuthEvent.OnGuestClicked) },
            onContinueWithoutRights = { viewModel.sendEvent(AuthEvent.OnContinueWithoutRights) },
            onRetryRights = { viewModel.sendEvent(AuthEvent.OnRetryRights) },
            onOpenActivation = { viewModel.sendEvent(AuthEvent.OnOpenActivation) },
            onOpenSupport = { uriHandler.openUri(SUPPORT_URL) },
            onLanguageChange = onLanguageChange
        )
    }

    AuthLayout(
        state = state,
        stepByStep = !isDesktopPlatform,
        language = language,
        actions = actions,
        modifier = modifier
    )
}

private const val SUPPORT_URL = "https://t.me/rudionee"
