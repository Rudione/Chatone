package io.rudione.chatone.presentation.automod

import io.rudione.chatone.domain.model.ChatRuleType
import io.rudione.chatone.presentation.theme.i18n.AppStrings

fun AppStrings.defaultChatRuleReply(type: ChatRuleType): String = when (type) {
    ChatRuleType.SPAM_RATE -> chatRuleReplySpam
    ChatRuleType.ALL_CAPS -> chatRuleReplyCaps
    ChatRuleType.LINKS -> chatRuleReplyLinks
    ChatRuleType.EMOTE_SPAM -> chatRuleReplyEmotes
    ChatRuleType.NEW_ACCOUNT -> chatRuleReplyNewAccount
    ChatRuleType.DUPLICATE_MESSAGE -> chatRuleReplyDuplicate
    ChatRuleType.CONSECUTIVE_NUMBERS -> chatRuleReplyNumbers
    ChatRuleType.MESSAGE_LENGTH -> chatRuleReplyLength
    ChatRuleType.STREAM_ONLINE,
    ChatRuleType.STREAM_OFFLINE,
    ChatRuleType.FIRST_MESSAGE_GREETING,
    ChatRuleType.RAID_WELCOME -> ""
}
