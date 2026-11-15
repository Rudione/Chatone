package io.rudione.chatone.presentation.liveactivity

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.live_activity_follow
import chatone.composeapp.generated.resources.live_activity_stop
import chatone.composeapp.generated.resources.live_activity_unavailable
import org.jetbrains.compose.resources.stringResource

@Composable
fun FollowLiveEventButton(event: LiveChatEvent) {
    val available by LiveEventActivities.available.collectAsState()
    if (!available) return
    val followed by LiveEventActivities.followed.collectAsState()
    val failed by LiveEventActivities.error.collectAsState()
    val selected = followed?.key == event.key
    if ((event.deadlineMs != null && !event.terminal) || selected) {
        TextButton(onClick = { if (selected) LiveEventActivities.stop() else LiveEventActivities.follow(event) }) {
            Text(stringResource(if (selected) Res.string.live_activity_stop else Res.string.live_activity_follow))
        }
    }
    if (failed) Text(stringResource(Res.string.live_activity_unavailable))
}
