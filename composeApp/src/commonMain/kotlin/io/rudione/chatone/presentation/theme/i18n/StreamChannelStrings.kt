package io.rudione.chatone.presentation.theme.i18n

interface StreamChannelStrings {
    val follow: String
    val unfollow: String
    val liveAlertsOn: String
    val liveAlertsOff: String
    val pin: String
    val assistant: String
    val followFailed: String
    val followBlocked: String
    val openOnTwitch: String
    val backToStreamChat: String
    val followConfirmTitle: String
    val followConfirmHint: String
    val unfollowConfirmTitle: String
    val unfollowConfirmHint: String
    val unfollowHold: String
}

object StreamChannelStringsEn : StreamChannelStrings {
    override val follow = "Follow"
    override val unfollow = "Unfollow"
    override val liveAlertsOn = "Notify when live"
    override val liveAlertsOff = "Don't notify when live"
    override val pin = "Pin to sidebar"
    override val assistant = "AI assistant"
    override val followFailed = "Couldn't change the follow. Try again."
    override val followBlocked = "Twitch only allows this on its site."
    override val openOnTwitch = "Open Twitch"
    override val backToStreamChat = "Back to the stream chat"
    override val followConfirmTitle = "Follow {0}?"
    override val followConfirmHint = "Their streams will show up in the Following tab."
    override val unfollowConfirmTitle = "Unfollow {0}?"
    override val unfollowConfirmHint = "Hold the heart for 2 seconds."
    override val unfollowHold = "Hold to unfollow"
}

object StreamChannelStringsRu : StreamChannelStrings {
    override val follow = "Отслеживать"
    override val unfollow = "Отписаться"
    override val liveAlertsOn = "Оповещать о начале стрима"
    override val liveAlertsOff = "Не оповещать о начале стрима"
    override val pin = "Закрепить в боковой панели"
    override val assistant = "ИИ-ассистент"
    override val followFailed = "Не получилось. Попробуйте ещё раз."
    override val followBlocked = "Twitch разрешает это только на своём сайте."
    override val openOnTwitch = "Открыть Twitch"
    override val backToStreamChat = "Вернуться к чату стрима"
    override val followConfirmTitle = "Отслеживать {0}?"
    override val followConfirmHint = "Стримы канала появятся во вкладке «Подписки»."
    override val unfollowConfirmTitle = "Отписаться от {0}?"
    override val unfollowConfirmHint = "Удерживайте сердце 2 секунды."
    override val unfollowHold = "Удерживайте, чтобы отписаться"
}
