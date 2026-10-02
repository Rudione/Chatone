package io.rudione.chatone.presentation.startup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.rudione.chatone.presentation.components.expressive.ChatoneMark
import io.rudione.chatone.presentation.components.expressive.ScallopBadge
import io.rudione.chatone.presentation.components.expressive.ambientHaze
import io.rudione.chatone.presentation.components.expressive.touchHaze
import io.rudione.chatone.presentation.theme.i18n.LocalStrings
import io.rudione.chatone.util.system.LaunchGate
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
fun LaunchSplashOverlay(maxHold: Duration = MAX_HOLD) {
    val readiness: LaunchReadiness = koinInject()
    val systemSplash: LaunchGate = koinInject()
    val stage by readiness.stage.collectAsState()
    LaunchedEffect(readiness, systemSplash) {
        withFrameNanos { }
        systemSplash.open()
        delay(maxHold)
        readiness.finish()
    }
    AnimatedVisibility(
        visible = stage != LaunchStage.Ready,
        enter = EnterTransition.None,
        exit = fadeOut(tween(EXIT_MILLIS)) + scaleOut(tween(EXIT_MILLIS), targetScale = EXIT_SCALE)
    ) {
        LaunchSplash(stage)
    }
}

@Composable
fun LaunchSplash(stage: LaunchStage, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .ambientHaze()
            .touchHaze(tint = scheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScallopBadge(
                icon = ChatoneMark,
                size = 168.dp,
                busy = stage != LaunchStage.Ready,
                iconFraction = MARK_FRACTION,
                content = Color.Unspecified
            )
            Spacer(Modifier.height(32.dp))
            Text(
                s.appName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
            AnimatedContent(
                targetState = stage,
                transitionSpec = { fadeIn(tween(220, 60)) togetherWith fadeOut(tween(140)) },
                label = "launchStage"
            ) { current ->
                Text(
                    s.launch.stage(current),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

private val MAX_HOLD = 8.seconds
private const val EXIT_MILLIS = 320
private const val EXIT_SCALE = 1.06f
private const val MARK_FRACTION = 0.46f
