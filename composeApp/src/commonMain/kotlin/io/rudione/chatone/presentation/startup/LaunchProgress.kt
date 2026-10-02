package io.rudione.chatone.presentation.startup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import io.rudione.chatone.presentation.chat.ChatWarmup
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

fun ChatWarmup.launchStage(): LaunchStage = when {
    !assetsReady -> LaunchStage.Emotes
    !history -> LaunchStage.History
    else -> LaunchStage.Ready
}

@Composable
fun ReportLaunchStage(stage: LaunchStage) {
    val readiness: LaunchReadiness = koinInject()
    LaunchedEffect(readiness, stage) {
        if (readiness.isReady) return@LaunchedEffect
        if (stage != LaunchStage.Ready) {
            readiness.advance(stage)
            return@LaunchedEffect
        }
        readiness.advance(LaunchStage.Rendering)
        repeat(SETTLE_FRAMES) { withFrameNanos { } }
        delay(SETTLE_MILLIS)
        readiness.finish()
    }
}

private const val SETTLE_FRAMES = 3
private const val SETTLE_MILLIS = 180L
