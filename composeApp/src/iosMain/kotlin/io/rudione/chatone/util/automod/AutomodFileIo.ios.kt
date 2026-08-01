package io.rudione.chatone.util.automod

import io.rudione.chatone.domain.model.AutomodRule
import io.rudione.chatone.domain.model.ChatRule

actual suspend fun saveAutomodText(defaultName: String, content: String): String? = null

actual suspend fun readAutomodText(): String? = null

actual fun buildXlsxContent(wordRules: List<AutomodRule>, chatRules: List<ChatRule>): String = ""
