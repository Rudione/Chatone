package io.rudione.chatone.presentation.automod

import chatone.composeapp.generated.resources.chat_rule_reply_caps
import chatone.composeapp.generated.resources.chat_rule_reply_duplicate
import chatone.composeapp.generated.resources.chat_rule_reply_emotes
import chatone.composeapp.generated.resources.chat_rule_reply_length
import chatone.composeapp.generated.resources.chat_rule_reply_links
import chatone.composeapp.generated.resources.chat_rule_reply_new_account
import chatone.composeapp.generated.resources.chat_rule_reply_numbers
import chatone.composeapp.generated.resources.chat_rule_reply_spam
import io.rudione.chatone.domain.model.ChatRuleType
import chatone.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.StringResource

fun defaultChatRuleReplyRes(type: ChatRuleType): StringResource? = when (type) {
    ChatRuleType.SPAM_RATE -> Res.string.chat_rule_reply_spam
    ChatRuleType.ALL_CAPS -> Res.string.chat_rule_reply_caps
    ChatRuleType.LINKS -> Res.string.chat_rule_reply_links
    ChatRuleType.EMOTE_SPAM -> Res.string.chat_rule_reply_emotes
    ChatRuleType.NEW_ACCOUNT -> Res.string.chat_rule_reply_new_account
    ChatRuleType.DUPLICATE_MESSAGE -> Res.string.chat_rule_reply_duplicate
    ChatRuleType.CONSECUTIVE_NUMBERS -> Res.string.chat_rule_reply_numbers
    ChatRuleType.MESSAGE_LENGTH -> Res.string.chat_rule_reply_length
    ChatRuleType.STREAM_ONLINE,
    ChatRuleType.STREAM_OFFLINE,
    ChatRuleType.FIRST_MESSAGE_GREETING,
    ChatRuleType.RAID_WELCOME -> null
}
