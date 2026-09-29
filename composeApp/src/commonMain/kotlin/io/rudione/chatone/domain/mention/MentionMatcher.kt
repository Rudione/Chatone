package io.rudione.chatone.domain.mention

import io.rudione.chatone.domain.model.HighlightRule
import io.rudione.chatone.util.automod.RegexCache

object MentionMatcher {

    sealed interface Match {
        data object OwnName : Match
        data class Rule(val rule: HighlightRule) : Match
    }

    private val NON_TEXT_RULES = setOf("whispers", "subscriptions", "first_message")

    fun match(
        text: String,
        login: String,
        displayName: String,
        replyParentLogin: String?,
        replyParentDisplayName: String?,
        rules: List<HighlightRule>
    ): Match? {
        val selfLogin = login.lowercase()
        val selfDisplay = displayName.lowercase()
        if (selfLogin.isEmpty() && selfDisplay.isEmpty()) return null

        val repliedToMe = listOfNotNull(replyParentLogin, replyParentDisplayName).any { parent ->
            val normalized = parent.lowercase()
            (selfLogin.isNotEmpty() && normalized == selfLogin) ||
                (selfDisplay.isNotEmpty() && normalized == selfDisplay)
        }
        val loginMatch = selfLogin.isNotEmpty() && containsUserMention(text, selfLogin)
        val displayMatch = selfDisplay.isNotEmpty() && selfDisplay != selfLogin &&
            containsUserMention(text, selfDisplay)
        if (repliedToMe || loginMatch || displayMatch) return Match.OwnName

        for (rule in rules) {
            if (!rule.enabled || rule.id in NON_TEXT_RULES) continue
            val pattern = if (rule.id == HighlightRule.USERNAME_RULE.id) selfLogin else rule.pattern
            if (pattern.isNotEmpty() && matches(rule, pattern, text)) return Match.Rule(rule)
        }
        return null
    }

    fun containsUserMention(text: String, name: String): Boolean {
        if (name.isEmpty()) return false
        var index = text.indexOf(name, ignoreCase = true)
        while (index >= 0) {
            val before = text.getOrNull(index - 1)
            val after = text.getOrNull(index + name.length)
            val startOk = before == null || before == '@' || !before.isLetterOrDigit() && before != '_'
            val endOk = after == null || !after.isLetterOrDigit() && after != '_'
            if (startOk && endOk) return true
            index = text.indexOf(name, startIndex = index + 1, ignoreCase = true)
        }
        return false
    }

    private fun matches(rule: HighlightRule, pattern: String, text: String): Boolean = when {
        rule.isRegex -> RegexCache.regex(pattern, ignoreCase = !rule.caseSensitive)?.containsMatchIn(text) ?: false
        rule.matchSubstring -> text.contains(pattern, ignoreCase = !rule.caseSensitive)
        else -> RegexCache.wholeWord(pattern, ignoreCase = !rule.caseSensitive)?.containsMatchIn(text)
            ?: text.equals(pattern, ignoreCase = !rule.caseSensitive)
    }
}
