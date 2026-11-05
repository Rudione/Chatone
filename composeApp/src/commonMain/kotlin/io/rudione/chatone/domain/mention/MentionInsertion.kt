package io.rudione.chatone.domain.mention

object MentionInsertion {

    fun append(input: String, name: String): String {
        val clean = name.trim().removePrefix("@").trim()
        if (clean.isEmpty() || clean.any(Char::isWhitespace)) return input
        val tag = "@$clean"
        if (input.isBlank()) return "$tag "
        if (input.trimEnd().substringAfterLast(' ').equals(tag, ignoreCase = true)) {
            return if (input.last().isWhitespace()) input else "$input "
        }
        val separator = if (input.last().isWhitespace()) "" else " "
        return "$input$separator$tag "
    }

    fun preferredName(displayName: String, login: String): String =
        if (login.isNotBlank() && !displayName.equals(login, ignoreCase = true)) login else displayName.ifBlank { login }
}
