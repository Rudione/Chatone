package io.rudione.chatone.presentation.chat.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.chat_emote_only
import chatone.composeapp.generated.resources.chat_follow_wait
import chatone.composeapp.generated.resources.chat_requires_follow
import chatone.composeapp.generated.resources.chat_requires_subscription
import chatone.composeapp.generated.resources.chat_slow_mode
import io.rudione.chatone.presentation.chat.ChatState
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock

@Composable
internal fun inputRoomModeLabels(state: ChatState): List<String> {
    val room = state.roomState
    val relationship = state.viewerRelationship
    val followedAt = relationship?.followedAtEpochMs
    val followDeadline = followedAt?.let { it + room.followersOnly.coerceAtLeast(0) * 60_000L }
    var now by remember(followDeadline) { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(followDeadline) {
        val remaining = (followDeadline ?: return@LaunchedEffect) - Clock.System.now().toEpochMilliseconds()
        if (remaining > 0) delay(remaining)
        now = Clock.System.now().toEpochMilliseconds()
    }
    if (state.isMod || state.isBroadcaster || state.isGrandMod || state.viewerBadges == null || !state.roomStateReceived) return emptyList()
    val privileged = state.hasPrivilegedChatAccess
    val subscriber = state.ownSubTier > 0 || (relationship?.subscriptionTier ?: 0) > 0
    return buildList {
        if (!privileged && room.subsOnly && !subscriber) add(stringResource(Res.string.chat_requires_subscription))
        if (!privileged && room.followersOnly >= 0 && relationship != null) {
            if (followedAt == null) add(stringResource(Res.string.chat_requires_follow))
            else if (followDeadline != null && now < followDeadline) {
                add(stringResource(Res.string.chat_follow_wait, ((followDeadline - now + 59_999L) / 60_000L).toInt()))
            }
        }
        if (!privileged && room.slowMode > 0) add(stringResource(Res.string.chat_slow_mode, room.slowMode))
        if (room.emoteOnly) add(stringResource(Res.string.chat_emote_only))
        if (room.r9k) add("R9K")
    }
}
