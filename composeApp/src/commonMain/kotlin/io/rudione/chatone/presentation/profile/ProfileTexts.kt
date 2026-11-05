package io.rudione.chatone.presentation.profile

import androidx.compose.runtime.Composable
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.channel_page_live_line
import chatone.composeapp.generated.resources.channel_page_live_line_viewers
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun channelLiveLine(category: String, viewers: String): String =
    if (category.isBlank()) {
        stringResource(Res.string.channel_page_live_line_viewers, viewers)
    } else {
        stringResource(Res.string.channel_page_live_line, category, viewers)
    }
