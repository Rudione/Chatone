package io.rudione.chatone.data.repository

import io.rudione.chatone.data.local.AutomodRuleEntity
import io.rudione.chatone.data.local.ChatoneDatabase
import io.rudione.chatone.domain.model.AutomodAction
import io.rudione.chatone.domain.model.AutomodRule
import io.rudione.chatone.domain.model.AutomodScope
import io.rudione.chatone.domain.model.ChatRule
import io.rudione.chatone.domain.model.ChatRuleAction
import io.rudione.chatone.domain.model.ChatRuleType
import io.rudione.chatone.domain.model.decodeAlternates
import io.rudione.chatone.domain.model.encodeAlternates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Clock

class AutomodRepository(private val database: ChatoneDatabase) {
    private val _rules = MutableStateFlow<List<AutomodRule>>(emptyList())
    val rules: StateFlow<List<AutomodRule>> = _rules

    private val _chatRules = MutableStateFlow<List<ChatRule>>(emptyList())
    val chatRules: StateFlow<List<ChatRule>> = _chatRules

    init { reload() }

    fun reload() {
        _rules.value = runCatching {
            database.automodRuleQueries.getAllWordRules().executeAsList().map { it.toWordDomain() }
        }.getOrElse { emptyList() }

        _chatRules.value = runCatching {
            database.automodRuleQueries.getAllChatRules().executeAsList().map { it.toChatDomain() }
        }.getOrElse { emptyList() }
    }

    fun rulesForChannel(channelLogin: String): List<AutomodRule> = runCatching {
        database.automodRuleQueries
            .getEnabledWordRulesForChannel(channelLogin.lowercase())
            .executeAsList()
            .map { it.toWordDomain() }
    }.getOrElse { emptyList() }

    fun upsert(rule: AutomodRule) {
        val now = Clock.System.now().toEpochMilliseconds()
        val r = rule.copy(
            createdAt = if (rule.createdAt == 0L) now else rule.createdAt,
            updatedAt = now,
            channelLogin = rule.channelLogin?.lowercase()?.takeIf { it.isNotBlank() }
        )
        database.automodRuleQueries.upsertWordRule(
            id = r.id,
            scope = r.scope.name,
            channelLogin = r.channelLogin,
            pattern = r.pattern,
            alternates = r.alternates.encodeAlternates(),
            isRegex = r.isRegex.toLong(),
            caseSensitive = r.caseSensitive.toLong(),
            wholeWord = r.wholeWord.toLong(),
            ignoreLinks = r.ignoreLinks.toLong(),
            action = r.action.name,
            timeoutMs = r.timeoutMs,
            frequencyThreshold = r.frequencyThreshold.toLong(),
            frequencyWindowMs = r.frequencyWindowMs,
            exemptMods = r.exemptMods.toLong(),
            exemptSubs = r.exemptSubs.toLong(),
            exemptVips = r.exemptVips.toLong(),
            enabled = r.enabled.toLong(),
            note = r.note,
            createdAt = r.createdAt,
            updatedAt = r.updatedAt
        )
        reload()
    }

    fun delete(id: String) {
        database.automodRuleQueries.deleteRule(id)
        reload()
    }

    fun setEnabled(id: String, enabled: Boolean) {
        database.automodRuleQueries.updateEnabled(
            enabled = enabled.toLong(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            id = id
        )
        reload()
    }

    fun replaceAll(rules: List<AutomodRule>) {
        database.automodRuleQueries.deleteAllWordRules()
        rules.forEach { upsert(it.copy(createdAt = 0L)) }
        reload()
    }

    fun importMerge(rules: List<AutomodRule>) {
        rules.forEach { upsert(it.copy(createdAt = 0L)) }
        reload()
    }

    fun chatRulesForChannel(channelLogin: String): List<ChatRule> = runCatching {
        database.automodRuleQueries
            .getEnabledChatRulesForChannel(channelLogin.lowercase())
            .executeAsList()
            .map { it.toChatDomain() }
    }.getOrElse { emptyList() }

    fun upsertChatRule(rule: ChatRule) {
        val now = Clock.System.now().toEpochMilliseconds()
        val r = rule.copy(
            createdAt = if (rule.createdAt == 0L) now else rule.createdAt,
            channelLogin = rule.channelLogin?.lowercase()?.takeIf { it.isNotBlank() }
        )
        database.automodRuleQueries.upsertChatRule(
            id = r.id,
            scope = r.scope.name,
            channelLogin = r.channelLogin,
            action = r.action.name,
            timeoutMs = r.timeoutSeconds * 1000L,
            exemptMods = r.exemptMods.toLong(),
            exemptSubs = r.exemptSubs.toLong(),
            exemptVips = r.exemptVips.toLong(),
            enabled = r.enabled.toLong(),
            createdAt = r.createdAt,
            updatedAt = now,
            chatRuleType = r.type.name,
            spamMaxMessages = r.spamMaxMessages.toLong(),
            spamWindowSeconds = r.spamWindowSeconds.toLong(),
            capsThresholdPercent = r.capsThresholdPercent.toLong(),
            capsMinLength = r.capsMinLength.toLong(),
            linksAllowClips = r.linksAllowClips.toLong(),
            linksClipsSameChannelOnly = r.linksClipsSameChannelOnly.toLong(),
            linksClipsAllowedChannels = r.linksClipsAllowedChannels.joinToString("\n"),
            emoteMaxCount = r.emoteMaxCount.toLong(),
            newAccountAgeDays = r.newAccountAgeDays.toLong(),
            duplicateMinLength = r.duplicateMinLength.toLong(),
            timeoutSeconds = r.timeoutSeconds.toLong(),
            eventMessage = r.eventMessage,
            eventRepeat = r.eventRepeat.toLong().coerceIn(1, 10),
            eventDelaySeconds = r.eventDelaySeconds.toLong().coerceIn(0, 600),
            linksRequireHttps = r.linksRequireHttps.toLong(),
            linksAllowedSites = r.linksAllowedSites.joinToString("\n")
        )
        reload()
    }

    fun deleteChatRule(id: String) {
        database.automodRuleQueries.deleteRule(id)
        reload()
    }

    fun setChatRuleEnabled(id: String, enabled: Boolean) {
        database.automodRuleQueries.updateEnabled(
            enabled = enabled.toLong(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            id = id
        )
        reload()
    }

    fun replaceAllChatRules(rules: List<ChatRule>) {
        database.automodRuleQueries.deleteAllChatRules()
        rules.forEach { upsertChatRule(it.copy(createdAt = 0L)) }
        reload()
    }

    fun importMergeChatRules(rules: List<ChatRule>) {
        rules.forEach { upsertChatRule(it.copy(createdAt = 0L)) }
        reload()
    }

    private fun Boolean.toLong(): Long = if (this) 1L else 0L

    private fun AutomodRuleEntity.toWordDomain(): AutomodRule = AutomodRule(
        id = id,
        scope = runCatching { AutomodScope.valueOf(scope) }.getOrDefault(AutomodScope.GLOBAL),
        channelLogin = channelLogin,
        pattern = pattern,
        alternates = alternates.decodeAlternates(),
        isRegex = isRegex == 1L,
        caseSensitive = caseSensitive == 1L,
        wholeWord = wholeWord == 1L,
        ignoreLinks = ignoreLinks == 1L,
        action = runCatching { AutomodAction.valueOf(action) }.getOrDefault(AutomodAction.DELETE),
        timeoutMs = timeoutMs,
        frequencyThreshold = frequencyThreshold.toInt(),
        frequencyWindowMs = frequencyWindowMs,
        exemptMods = exemptMods == 1L,
        exemptSubs = exemptSubs == 1L,
        exemptVips = exemptVips == 1L,
        enabled = enabled == 1L,
        note = note,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun AutomodRuleEntity.toChatDomain(): ChatRule = ChatRule(
        id = id,
        type = runCatching { ChatRuleType.valueOf(chatRuleType ?: "") }.getOrDefault(ChatRuleType.SPAM_RATE),
        scope = runCatching { AutomodScope.valueOf(scope) }.getOrDefault(AutomodScope.GLOBAL),
        channelLogin = channelLogin,
        action = runCatching { ChatRuleAction.valueOf(action) }.getOrDefault(ChatRuleAction.DELETE),
        timeoutSeconds = timeoutSeconds.toInt(),
        enabled = enabled == 1L,
        spamMaxMessages = spamMaxMessages.toInt(),
        spamWindowSeconds = spamWindowSeconds.toInt(),
        capsThresholdPercent = capsThresholdPercent.toInt(),
        capsMinLength = capsMinLength.toInt(),
        linksAllowClips = linksAllowClips == 1L,
        linksClipsSameChannelOnly = linksClipsSameChannelOnly == 1L,
        linksClipsAllowedChannels = linksClipsAllowedChannels.lines()
            .map { it.trim().lowercase().removePrefix("#") }
            .filter { it.isNotBlank() },
        linksRequireHttps = linksRequireHttps == 1L,
        linksAllowedSites = linksAllowedSites.lines()
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() },
        emoteMaxCount = emoteMaxCount.toInt(),
        newAccountAgeDays = newAccountAgeDays.toInt(),
        duplicateMinLength = duplicateMinLength.toInt(),
        exemptMods = exemptMods == 1L,
        exemptVips = exemptVips == 1L,
        exemptSubs = exemptSubs == 1L,
        eventMessage = eventMessage,
        eventRepeat = eventRepeat.toInt().coerceIn(1, 10),
        eventDelaySeconds = eventDelaySeconds.toInt().coerceIn(0, 600),
        createdAt = createdAt
    )
}
