package io.rudione.chatone.util.automod

import io.rudione.chatone.domain.model.AutomodRule
import io.rudione.chatone.domain.model.ChatRule

expect suspend fun saveAutomodText(defaultName: String, content: String): String?

expect suspend fun readAutomodText(): String?

expect fun buildXlsxContent(wordRules: List<AutomodRule>, chatRules: List<ChatRule>): String
