package io.rudione.chatone.presentation.ai

import io.rudione.chatone.domain.model.AutomodScope
import io.rudione.chatone.domain.model.ChatRule
import io.rudione.chatone.domain.model.ChatRuleAction
import io.rudione.chatone.domain.model.ChatRuleType
import kotlin.time.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AiRuleProposal(
    val tool: String = "",
    val type: String = "",
    val action: String = "DELETE",
    val timeoutSeconds: Int = 60,
    val scope: String = "GLOBAL",
    val channelLogin: String? = null,
    val reason: String = "",
    val capsThresholdPercent: Int? = null,
    val spamMaxMessages: Int? = null,
    val spamWindowSeconds: Int? = null,
    val emoteMaxCount: Int? = null,
    val newAccountAgeDays: Int? = null,
    val consecutiveNumbersThreshold: Int? = null
)

object AiActions {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val BLOCK_RE =
        Regex("```(?:chatone-action|json)?\\s*(\\{.*?\\})\\s*```", RegexOption.DOT_MATCHES_ALL)

    private val ALLOWED_TYPES = setOf(
        ChatRuleType.SPAM_RATE, ChatRuleType.ALL_CAPS, ChatRuleType.LINKS,
        ChatRuleType.EMOTE_SPAM, ChatRuleType.NEW_ACCOUNT,
        ChatRuleType.DUPLICATE_MESSAGE, ChatRuleType.CONSECUTIVE_NUMBERS
    )
    private val ALLOWED_ACTIONS =
        setOf(ChatRuleAction.DELETE, ChatRuleAction.TIMEOUT, ChatRuleAction.BAN)

    fun parse(text: String): AiRuleProposal? {
        val raw = BLOCK_RE.find(text)?.groupValues?.getOrNull(1) ?: return null
        val proposal =
            runCatching { json.decodeFromString<AiRuleProposal>(raw) }.getOrNull() ?: return null
        if (proposal.tool != "add_automod_rule") return null
        if (runCatching { ChatRuleType.valueOf(proposal.type) }.getOrNull() !in ALLOWED_TYPES) return null
        return proposal
    }

    fun strip(text: String): String = BLOCK_RE.replace(text, "").trim()

    fun toChatRule(proposal: AiRuleProposal): ChatRule? {
        val type = runCatching { ChatRuleType.valueOf(proposal.type) }.getOrNull()
            ?.takeIf { it in ALLOWED_TYPES }
            ?: return null
        val action = runCatching { ChatRuleAction.valueOf(proposal.action) }.getOrNull()
            ?.takeIf { it in ALLOWED_ACTIONS } ?: ChatRuleAction.DELETE
        val channel = proposal.channelLogin?.takeIf { it.isNotBlank() }
        val requestedScope =
            runCatching { AutomodScope.valueOf(proposal.scope) }.getOrNull() ?: AutomodScope.GLOBAL
        val scope =
            if (requestedScope == AutomodScope.LOCAL && channel == null) AutomodScope.GLOBAL else requestedScope
        val now = Clock.System.now().toEpochMilliseconds()

        var rule = ChatRule(
            id = "ai_$now",
            type = type,
            action = action,
            scope = scope,
            channelLogin = channel,
            timeoutSeconds = proposal.timeoutSeconds.coerceIn(1, 1_209_600),
            createdAt = now
        )
        proposal.capsThresholdPercent?.let {
            rule = rule.copy(capsThresholdPercent = it.coerceIn(1, 100))
        }
        proposal.spamMaxMessages?.let { rule = rule.copy(spamMaxMessages = it.coerceAtLeast(1)) }
        proposal.spamWindowSeconds?.let {
            rule = rule.copy(spamWindowSeconds = it.coerceAtLeast(1))
        }
        proposal.emoteMaxCount?.let { rule = rule.copy(emoteMaxCount = it.coerceAtLeast(1)) }
        proposal.newAccountAgeDays?.let {
            rule = rule.copy(newAccountAgeDays = it.coerceAtLeast(1))
        }
        proposal.consecutiveNumbersThreshold?.let {
            rule = rule.copy(consecutiveNumbersThreshold = it.coerceAtLeast(2))
        }
        return rule
    }
}
