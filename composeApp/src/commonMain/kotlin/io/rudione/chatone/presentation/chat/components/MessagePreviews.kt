package io.rudione.chatone.presentation.chat.components

import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.ui_baduser_was_banned_by_moderator
import chatone.composeapp.generated.resources.ui_connected_to_chat
import chatone.composeapp.generated.resources.ui_message_from_viewer123_was_deleted_by
import chatone.composeapp.generated.resources.ui_spammer_was_timed_out_for_600s
import chatone.composeapp.generated.resources.ui_stream_starting_in_10_minutes
import chatone.composeapp.generated.resources.ui_this_got_removed
import chatone.composeapp.generated.resources.ui_this_message_was_held_for_review
import chatone.composeapp.generated.resources.ui_waves_hello
import chatone.composeapp.generated.resources.ui_you_check_this_out
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.presentation.chat.AutoModMsgItem
import io.rudione.chatone.presentation.chat.ModerationMsgItem
import io.rudione.chatone.presentation.chat.PrivMsgItem
import io.rudione.chatone.presentation.chat.SystemMsgItem
import io.rudione.chatone.presentation.chat.UserNoticeMsgItem
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.util.chat.MessageToken
import org.jetbrains.compose.ui.tooling.preview.Preview

private fun samplePrivMsg(
    username: String = "viewer123",
    text: String = "Hello chat! This is a sample message",
    color: String? = "#9146FF",
    isMention: Boolean = false,
    isAction: Boolean = false,
    isFirstMessage: Boolean = false,
    isDeleted: Boolean = false,
    isModerator: Boolean = false,
    isSubscriber: Boolean = false,
    isVip: Boolean = false,
    isBroadcaster: Boolean = false,
): DisplayMessage.PrivMsg = DisplayMessage.PrivMsg(
    id = "preview-${username}",
    timestamp = 1_700_000_000_000L,
    channel = "samplechannel",
    userId = "1",
    username = username,
    displayName = username,
    tokens = listOf(MessageToken.Text(text)),
    color = color,
    badges = emptyList(),
    isModerator = isModerator,
    isSubscriber = isSubscriber,
    isVip = isVip,
    isBroadcaster = isBroadcaster,
    isMention = isMention,
    isAction = isAction,
    isFirstMessage = isFirstMessage,
    isDeleted = isDeleted,
)

@Composable
private fun PreviewSurface(content: @Composable () -> Unit) {
    ChatoneTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(8.dp)
        ) { content() }
    }
}

@Preview
@Composable
private fun PrivMsgPreview() = PreviewSurface {
    PrivMsgItem(message = samplePrivMsg())
}

@Preview
@Composable
private fun PrivMsgMentionPreview() = PreviewSurface {
    PrivMsgItem(
        message = samplePrivMsg(
            username = "friend",
            text = stringResource(Res.string.ui_you_check_this_out),
            isMention = true
        )
    )
}

@Preview
@Composable
private fun PrivMsgActionPreview() = PreviewSurface {
    PrivMsgItem(
        message = samplePrivMsg(
            text = stringResource(Res.string.ui_waves_hello),
            isAction = true
        )
    )
}

@Preview
@Composable
private fun PrivMsgFirstMessagePreview() = PreviewSurface {
    PrivMsgItem(
        message = samplePrivMsg(
            username = "newbie",
            isFirstMessage = true
        )
    )
}

@Preview
@Composable
private fun PrivMsgDeletedPreview() = PreviewSurface {
    PrivMsgItem(
        message = samplePrivMsg(
            text = stringResource(Res.string.ui_this_got_removed),
            isDeleted = true
        )
    )
}

@Preview
@Composable
private fun SystemMsgPreview() = PreviewSurface {
    SystemMsgItem(
        DisplayMessage.SystemMsg(
            id = "sys",
            timestamp = 1_700_000_000_000L,
            channel = "samplechannel",
            text = stringResource(Res.string.ui_connected_to_chat)
        )
    )
}

@Preview
@Composable
private fun UserNoticeSubPreview() = PreviewSurface {
    UserNoticeMsgItem(
        DisplayMessage.UserNoticeMsg(
            id = "un1",
            timestamp = 1_700_000_000_000L,
            channel = "samplechannel",
            systemText = "viewer123 subscribed at Tier 1. They've subscribed for 6 months!",
            noticeType = "resub"
        )
    )
}

@Preview
@Composable
private fun UserNoticeAnnouncementPreview() = PreviewSurface {
    UserNoticeMsgItem(
        DisplayMessage.UserNoticeMsg(
            id = "un2",
            timestamp = 1_700_000_000_000L,
            channel = "samplechannel",
            systemText = "Stream starting in 10 minutes!",
            innerMessage = samplePrivMsg(
                username = "streamer",
                text = stringResource(Res.string.ui_stream_starting_in_10_minutes)
            ),
            noticeType = "announcement",
            announceColor = "purple"
        )
    )
}

@Preview
@Composable
private fun ModerationBanPreview() = PreviewSurface {
    ModerationMsgItem(
        DisplayMessage.ModerationMsg(
            id = "mod1",
            timestamp = 1_700_000_000_000L,
            channel = "samplechannel",
            text = stringResource(Res.string.ui_baduser_was_banned_by_moderator),
            action = DisplayMessage.ModerationMsg.ModerationAction.BAN,
            targetUser = "baduser",
            moderatorLogin = "moderator"
        )
    )
}

@Preview
@Composable
private fun ModerationTimeoutPreview() = PreviewSurface {
    ModerationMsgItem(
        DisplayMessage.ModerationMsg(
            id = "mod2",
            timestamp = 1_700_000_000_000L,
            channel = "samplechannel",
            text = stringResource(Res.string.ui_spammer_was_timed_out_for_600s),
            action = DisplayMessage.ModerationMsg.ModerationAction.TIMEOUT,
            targetUser = "spammer",
            duration = 600,
            moderatorLogin = "moderator"
        )
    )
}

@Preview
@Composable
private fun ModerationDeletePreview() = PreviewSurface {
    ModerationMsgItem(
        DisplayMessage.ModerationMsg(
            id = "mod3",
            timestamp = 1_700_000_000_000L,
            channel = "samplechannel",
            text = stringResource(Res.string.ui_message_from_viewer123_was_deleted_by),
            action = DisplayMessage.ModerationMsg.ModerationAction.DELETE,
            targetUser = "viewer123",
            moderatorLogin = "moderator"
        )
    )
}

@Preview
@Composable
private fun AutoModPendingPreview() = PreviewSurface {
    AutoModMsgItem(
        message = DisplayMessage.AutoModMsg(
            id = "am1",
            timestamp = 1_700_000_000_000L,
            channel = "samplechannel",
            msgId = "am1",
            userId = "2",
            username = "flagged_user",
            displayName = "flagged_user",
            text = stringResource(Res.string.ui_this_message_was_held_for_review),
            color = "#FF7F50",
            status = DisplayMessage.AutoModMsg.AutoModStatus.PENDING,
            reasonCategory = "swearing",
            reasonLevel = 2
        ),
        onAllow = {},
        onDeny = {}
    )
}
