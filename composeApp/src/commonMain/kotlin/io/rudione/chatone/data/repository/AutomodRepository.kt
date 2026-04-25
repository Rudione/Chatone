package io.rudione.chatone.data.repository

import io.rudione.chatone.data.local.AutomodRuleEntity
import io.rudione.chatone.data.local.ChatoneDatabase
import io.rudione.chatone.domain.model.AutomodAction
import io.rudione.chatone.domain.model.AutomodRule
import io.rudione.chatone.domain.model.AutomodScope
import io.rudione.chatone.domain.model.decodeAlternates
import io.rudione.chatone.domain.model.encodeAlternates
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Clock

class AutomodRepository(private val database: ChatoneDatabase) {

    private val _rules = MutableStateFlow<List<AutomodRule>>(emptyList())
    val rules: StateFlow<List<AutomodRule>> = _rules

    init { reload() }

    fun reload() {
        _rules.value = runCatching {
            database.automodRuleQueries.getAllRules().executeAsList().map { it.toDomain() }
        }.getOrElse { emptyList() }
    }

    fun rulesForChannel(channelLogin: String): List<AutomodRule> = runCatching {
        database.automodRuleQueries
            .getEnabledRulesForChannel(channelLogin.lowercase())
            .executeAsList()
            .map { it.toDomain() }
    }.getOrElse { emptyList() }

    fun upsert(rule: AutomodRule) {
        val now = Clock.System.now().toEpochMilliseconds()
        val finalRule = rule.copy(
            createdAt = if (rule.createdAt == 0L) now else rule.createdAt,
            updatedAt = now,
            channelLogin = rule.channelLogin?.lowercase()?.takeIf { it.isNotBlank() }
        )
        database.automodRuleQueries.upsertRule(
            id = finalRule.id,
            scope = finalRule.scope.name,
            channelLogin = finalRule.channelLogin,
            pattern = finalRule.pattern,
            alternates = finalRule.alternates.encodeAlternates(),
            isRegex = finalRule.isRegex.toLong(),
            caseSensitive = finalRule.caseSensitive.toLong(),
            wholeWord = finalRule.wholeWord.toLong(),
            action = finalRule.action.name,
            timeoutMs = finalRule.timeoutMs,
            frequencyThreshold = finalRule.frequencyThreshold.toLong(),
            frequencyWindowMs = finalRule.frequencyWindowMs,
            exemptMods = finalRule.exemptMods.toLong(),
            exemptSubs = finalRule.exemptSubs.toLong(),
            exemptVips = finalRule.exemptVips.toLong(),
            enabled = finalRule.enabled.toLong(),
            note = finalRule.note,
            createdAt = finalRule.createdAt,
            updatedAt = finalRule.updatedAt
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
        database.automodRuleQueries.deleteAll()
        rules.forEach { upsert(it.copy(createdAt = 0L)) }
        reload()
    }

    fun importMerge(rules: List<AutomodRule>) {
        rules.forEach { upsert(it.copy(createdAt = 0L)) }
        reload()
    }

    private fun Boolean.toLong(): Long = if (this) 1L else 0L

    private fun AutomodRuleEntity.toDomain(): AutomodRule = AutomodRule(
        id = id,
        scope = runCatching { AutomodScope.valueOf(scope) }.getOrDefault(AutomodScope.GLOBAL),
        channelLogin = channelLogin,
        pattern = pattern,
        alternates = alternates.decodeAlternates(),
        isRegex = isRegex == 1L,
        caseSensitive = caseSensitive == 1L,
        wholeWord = wholeWord == 1L,
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
}
