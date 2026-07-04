package io.rudione.chatone.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.rudione.chatone.domain.model.DisplayMessage
import io.rudione.chatone.presentation.theme.ChatoneTheme
import io.rudione.chatone.util.MessageToken
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
    _root_ide_package_.io.rudione.chatone.presentation.chat.PrivMsgItem(message = samplePrivMsg())
}

@Preview
@Composable
private fun PrivMsgMentionPreview() = PreviewSurface {
    _root_ide_package_.io.rudione.chatone.presentation.chat.PrivMsgItem(
        message = samplePrivMsg(
            username = "friend",
            text = "@you check this out",
            isMention = true
        )
    )
}

@Preview
@Composable
private fun PrivMsgActionPreview() = PreviewSurface {
    _root_ide_package_.io.rudione.chatone.presentation.chat.PrivMsgItem(
        message = samplePrivMsg(
            text = "waves hello",
            isAction = true
        )
    )
}

@Preview
@Composable
private fun PrivMsgFirstMessagePreview() = PreviewSurface {
    _root_ide_package_.io.rudione.chatone.presentation.chat.PrivMsgItem(
        message = samplePrivMsg(
            username = "newbie",
            isFirstMessage = true
        )
    )
}

@Preview
@Composable
private fun PrivMsgDeletedPreview() = PreviewSurface {
    _root_ide_package_.io.rudione.chatone.presentation.chat.PrivMsgItem(
        message = samplePrivMsg(
            text = "this got removed",
            isDeleted = true
        )
    )
}

@Preview
@Composable
private fun SystemMsgPreview() = PreviewSurface {
    _root_ide_package_.io.rudione.chatone.presentation.chat.SystemMsgItem(
        DisplayMessage.SystemMsg(
            id = "sys",
            timestamp = 1_700_000_000_000L,
            channel = "samplechannel",
            text = "Connected to chat"
        )
    )
}

@Preview
@Composable
private fun UserNoticeSubPreview() = PreviewSurface {
    _root_ide_package_.io.rudione.chatone.presentation.chat.UserNoticeMsgItem(
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
    _root_ide_package_.io.rudione.chatone.presentation.chat.UserNoticeMsgItem(
        DisplayMessage.UserNoticeMsg(
            id = "un2",
            timestamp = 1_700_000_000_000L,
            channel = "samplechannel",
            systemText = "Stream starting in 10 minutes!",
            innerMessage = samplePrivMsg(
                username = "streamer",
                text = "Stream starting in 10 minutes!"
            ),
            noticeType = "announcement",
            announceColor = "purple"
        )
    )
}

@Preview
@Composable
private fun ModerationBanPreview() = PreviewSurface {
    _root_ide_package_.io.rudione.chatone.presentation.chat.ModerationMsgItem(
        DisplayMessage.ModerationMsg(
            id = "mod1",
            timestamp = 1_700_000_000_000L,
            channel = "samplechannel",
            text = "baduser was banned by moderator",
            action = DisplayMessage.ModerationMsg.ModerationAction.BAN,
            targetUser = "baduser",
            moderatorLogin = "moderator"
        )
    )
}

@Preview
@Composable
private fun ModerationTimeoutPreview() = PreviewSurface {
    _root_ide_package_.io.rudione.chatone.presentation.chat.ModerationMsgItem(
        DisplayMessage.ModerationMsg(
            id = "mod2",
            timestamp = 1_700_000_000_000L,
            channel = "samplechannel",
            text = "spammer was timed out for 600s by moderator",
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
    _root_ide_package_.io.rudione.chatone.presentation.chat.ModerationMsgItem(
        DisplayMessage.ModerationMsg(
            id = "mod3",
            timestamp = 1_700_000_000_000L,
            channel = "samplechannel",
            text = "Message from viewer123 was deleted by moderator",
            action = DisplayMessage.ModerationMsg.ModerationAction.DELETE,
            targetUser = "viewer123",
            moderatorLogin = "moderator"
        )
    )
}

@Preview
@Composable
private fun AutoModPendingPreview() = PreviewSurface {
    _root_ide_package_.io.rudione.chatone.presentation.chat.AutoModMsgItem(
        message = DisplayMessage.AutoModMsg(
            id = "am1",
            timestamp = 1_700_000_000_000L,
            channel = "samplechannel",
            msgId = "am1",
            userId = "2",
            username = "flagged_user",
            displayName = "flagged_user",
            text = "this message was held for review",
            color = "#FF7F50",
            status = DisplayMessage.AutoModMsg.AutoModStatus.PENDING,
            reasonCategory = "swearing",
            reasonLevel = 2
        ),
        onAllow = {},
        onDeny = {}
    )
}
