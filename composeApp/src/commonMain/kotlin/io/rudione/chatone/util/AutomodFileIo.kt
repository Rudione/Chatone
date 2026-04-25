package io.rudione.chatone.util


expect suspend fun saveAutomodText(defaultName: String, content: String): String?

expect suspend fun readAutomodText(): String?
