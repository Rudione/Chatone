package io.rudione.chatone.util.automod

import io.rudione.chatone.domain.model.AutomodAction
import io.rudione.chatone.domain.model.AutomodRule
import io.rudione.chatone.domain.model.ChatRule
import io.rudione.chatone.domain.model.ChatRuleAction
import io.rudione.chatone.util.chat.MessageToken

enum class LocalModAction { DELETE, TIMEOUT, BAN, NONE }

sealed interface LocalAutomodDecision {
    val modAction: LocalModAction
    val timeoutSeconds: Int

    data class Word(val verdict: AutomodVerdict) : LocalAutomodDecision {
        override val modAction: LocalModAction
            get() = when (verdict.action) {
                AutomodAction.DELETE -> LocalModAction.DELETE
                AutomodAction.TIMEOUT -> LocalModAction.TIMEOUT
                AutomodAction.BAN -> LocalModAction.BAN
            }
        override val timeoutSeconds: Int get() = verdict.timeoutSeconds
    }

    data class Chat(val verdict: ChatRuleEngine.Verdict) : LocalAutomodDecision {
        override val modAction: LocalModAction
            get() = when (verdict.action) {
                ChatRuleAction.DELETE -> LocalModAction.DELETE
                ChatRuleAction.TIMEOUT -> LocalModAction.TIMEOUT
                ChatRuleAction.BAN -> LocalModAction.BAN
                ChatRuleAction.SEND_MESSAGE -> LocalModAction.NONE
            }
        override val timeoutSeconds: Int get() = verdict.timeoutSeconds
    }
}

object LocalAutomodDecider {

    fun decide(
        text: String,
        tokens: List<MessageToken>,
        target: AutomodTarget,
        channelLogin: String,
        wordRules: List<AutomodRule>,
        chatRules: List<ChatRule>,
        accountCreatedAtMs: Long? = null
    ): LocalAutomodDecision? {
        if (wordRules.isNotEmpty()) {
            AutomodEngine.evaluate(text, target, channelLogin, wordRules)
                ?.let { return LocalAutomodDecision.Word(it) }
        }
        if (chatRules.isEmpty()) return null
        return ChatRuleEngine.evaluate(text, tokens, target, channelLogin, chatRules, accountCreatedAtMs)
            ?.let { LocalAutomodDecision.Chat(it) }
    }

    fun notice(decision: LocalAutomodDecision, chatter: String): String = when (decision) {
        is LocalAutomodDecision.Word -> buildString {
            append("Automod [${decision.verdict.rule.scopeLabel}] ")
            append(actionLabel(decision))
            append(" @$chatter: matched \"${decision.verdict.matchedPattern}\"")
        }

        is LocalAutomodDecision.Chat -> buildString {
            append("ChatRule [${decision.verdict.rule.scopeLabel}] ")
            append(actionLabel(decision))
            append(" @$chatter: ${decision.verdict.reason}")
        }
    }

    private fun actionLabel(decision: LocalAutomodDecision): String = when (decision.modAction) {
        LocalModAction.DELETE -> "deleted"
        LocalModAction.TIMEOUT -> "timed out (${decision.timeoutSeconds}s)"
        LocalModAction.BAN -> "banned"
        LocalModAction.NONE -> "responded"
    }
}
