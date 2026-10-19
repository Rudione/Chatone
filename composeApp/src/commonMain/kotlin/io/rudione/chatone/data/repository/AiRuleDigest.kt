package io.rudione.chatone.data.repository

import io.rudione.chatone.domain.model.AutomodAction
import io.rudione.chatone.domain.model.AutomodRule
import io.rudione.chatone.domain.model.ChatRule
import io.rudione.chatone.domain.model.ChatRuleAction

object AiRuleDigest {
    private const val MAX_RULES = 60

    fun lines(chatRules: List<ChatRule>, wordRules: List<AutomodRule>): List<String> =
        (chatRules.map(::describe) + wordRules.map(::describe)).take(MAX_RULES)

    private fun describe(rule: ChatRule): String {
        val timeout = if (rule.action == ChatRuleAction.TIMEOUT) " ${rule.timeoutSeconds}s" else ""
        return "${rule.displayLabel} → ${rule.action}$timeout (${rule.scopeLabel})"
    }

    private fun describe(rule: AutomodRule): String {
        val kind = if (rule.isRegex) "regex" else "phrase"
        val timeout = if (rule.action == AutomodAction.TIMEOUT) " ${rule.timeoutMs / 1_000}s" else ""
        return "Blocked $kind \"${rule.pattern}\" → ${rule.action}$timeout"
    }
}
