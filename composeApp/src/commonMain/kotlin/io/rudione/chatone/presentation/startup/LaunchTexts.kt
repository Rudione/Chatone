package io.rudione.chatone.presentation.startup

import androidx.compose.runtime.Composable
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.launch_stage_almost_ready
import chatone.composeapp.generated.resources.launch_stage_connecting
import chatone.composeapp.generated.resources.launch_stage_emotes
import chatone.composeapp.generated.resources.launch_stage_history
import chatone.composeapp.generated.resources.launch_stage_starting
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun launchStageLabel(stage: LaunchStage): String = stringResource(
    when (stage) {
        LaunchStage.Starting -> Res.string.launch_stage_starting
        LaunchStage.Connecting -> Res.string.launch_stage_connecting
        LaunchStage.Emotes -> Res.string.launch_stage_emotes
        LaunchStage.History -> Res.string.launch_stage_history
        LaunchStage.Rendering, LaunchStage.Ready -> Res.string.launch_stage_almost_ready
    }
)
