package io.rudione.chatone.presentation.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.data.repository.DeviceAuthState
import io.rudione.chatone.data.repository.LoginFailure
import io.rudione.chatone.icons.lucide.Check
import io.rudione.chatone.icons.lucide.Lucide
import io.rudione.chatone.icons.lucide.Sparkles
import io.rudione.chatone.icons.lucide.User
import io.rudione.chatone.icons.material.Icons
import io.rudione.chatone.icons.material.automirrored.filled.Send
import io.rudione.chatone.presentation.components.expressive.GlassIconButton
import io.rudione.chatone.presentation.components.expressive.PillButton
import io.rudione.chatone.presentation.components.expressive.PillSegmented
import io.rudione.chatone.presentation.components.expressive.PillTone
import io.rudione.chatone.presentation.components.expressive.ScallopBadge
import io.rudione.chatone.presentation.components.expressive.ambientHaze
import io.rudione.chatone.presentation.components.expressive.expressiveGlass
import io.rudione.chatone.presentation.components.expressive.touchHaze
import io.rudione.chatone.presentation.theme.i18n.LocalStrings

@Immutable
data class AuthActions(
    val onStartLogin: () -> Unit = {},
    val onReopenBrowser: () -> Unit = {},
    val onOpenMirror: () -> Unit = {},
    val onSubmitPayload: (String, Boolean) -> Unit = { _, _ -> },
    val onRetryPayload: () -> Unit = {},
    val onGuest: () -> Unit = {},
    val onContinueWithoutRights: () -> Unit = {},
    val onRetryRights: () -> Unit = {},
    val onOpenActivation: () -> Unit = {},
    val onOpenSupport: () -> Unit = {},
    val onLanguageChange: ((String) -> Unit)? = null
)

private sealed interface AuthScene {
    data object Checking : AuthScene
    data class Step(val step: AuthStep) : AuthScene
}

@Composable
fun AuthLayout(
    state: AuthState,
    stepByStep: Boolean,
    language: String,
    actions: AuthActions,
    modifier: Modifier = Modifier
) {
    val step = state.currentStep()
    val scene = if (state.isCheckingToken) AuthScene.Checking else AuthScene.Step(step)
    val paste = rememberLoginPasteWatcher(
        awaitingPaste = state.awaitingPaste,
        busy = state.isPreparing || state.isVerifying,
        failure = state.failure,
        onSubmitPayload = actions.onSubmitPayload
    )
    val scheme = MaterialTheme.colorScheme
    val hazeTint = when {
        state.failure != null && state.failure != LoginFailure.Network -> scheme.error
        else -> scheme.primary
    }

    Box(modifier = modifier.fillMaxSize().ambientHaze(tint = hazeTint).touchHaze(tint = hazeTint)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = scene is AuthScene.Step,
                enter = fadeIn(tween(300)) + slideInVertically { -it / 2 },
                exit = fadeOut(tween(160))
            ) {
                AuthStepper(
                    slots = authStepSlots(stepByStep, step),
                    current = step,
                    automatic = state.automaticReturn
                )
            }

            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val compactHeight = maxHeight < COMPACT_HEIGHT
                Column(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ScallopBadge(
                        icon = heroIcon(scene, state, paste.codeAccepted),
                        size = if (compactHeight) 112.dp else 156.dp,
                        busy = isBusy(state, step),
                        pulseKey = scene
                    )
                    Spacer(Modifier.height(if (compactHeight) 20.dp else 32.dp))
                    AnimatedContent(
                        targetState = scene,
                        transitionSpec = {
                            val forward = order(targetState) >= order(initialState)
                            val enter =
                                slideInHorizontally(
                                    spring(stiffness = Spring.StiffnessMediumLow)
                                ) {
                                    if (forward) it / 3 else -it / 3
                                } + fadeIn(tween(260, 60))
                            val exit = slideOutHorizontally(
                                tween(200)
                            ) {
                                if (forward) -it / 3 else it / 3
                            } + fadeOut(tween(160))
                            (enter togetherWith exit).using(SizeTransform(clip = false))
                        },
                        label = "authScene"
                    ) { current ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            when (current) {
                                AuthScene.Checking -> CheckingPanel()
                                is AuthScene.Step -> when (current.step) {
                                    AuthStep.SIGN_IN -> SignInPanel(state, actions)
                                    AuthStep.PASTE -> PastePanel(state, paste, actions)
                                    AuthStep.RIGHTS -> RightsPanel(state, actions)
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = scene is AuthScene.Step,
                enter = fadeIn(tween(300)) + slideInVertically { it / 2 },
                exit = fadeOut(tween(160))
            ) {
                AuthToolbar(language = language, actions = actions)
            }
        }
    }
}

@Composable
private fun AuthToolbar(language: String, actions: AuthActions) {
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .expressiveGlass(CircleShape)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        actions.onLanguageChange?.let { change ->
            PillSegmented(
                options = listOf("ru" to "RU", "en" to "EN"),
                selected = if (language == "ru") "ru" else "en",
                onSelect = change
            )
        }
        PillButton(
            text = s.authGuest,
            onClick = actions.onGuest,
            icon = Lucide.User,
            tone = PillTone.Tonal,
            height = 44.dp
        )
        GlassIconButton(
            icon = Icons.AutoMirrored.Filled.Send,
            contentDescription = s.authSupport,
            onClick = actions.onOpenSupport
        )
    }
}

private fun heroIcon(scene: AuthScene, state: AuthState, codeAccepted: Boolean) = when (scene) {
    AuthScene.Checking -> Lucide.Sparkles
    is AuthScene.Step -> when {
        scene.step == AuthStep.PASTE && codeAccepted && state.failure == null -> Lucide.Check
        state.deviceState is DeviceAuthState.Success -> Lucide.Check
        else -> scene.step.icon(state.automaticReturn)
    }
}

private fun isBusy(state: AuthState, step: AuthStep): Boolean =
    state.isCheckingToken || state.isPreparing || state.isVerifying ||
            state.deviceState == DeviceAuthState.Validating ||
            (step == AuthStep.RIGHTS && state.failure == null && state.activationUrl == null &&
                    state.deviceState != DeviceAuthState.ConfirmingOnLoginPage)

private val COMPACT_HEIGHT = 560.dp

private fun order(scene: AuthScene): Int = when (scene) {
    AuthScene.Checking -> -1
    is AuthScene.Step -> scene.step.ordinal
}
