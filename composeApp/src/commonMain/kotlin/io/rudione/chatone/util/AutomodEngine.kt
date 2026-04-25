package io.rudione.chatone.util

import io.rudione.chatone.domain.model.AutomodAction
import io.rudione.chatone.domain.model.AutomodRule
import io.rudione.chatone.domain.model.AutomodScope
import kotlinx.datetime.Clock

data class AutomodTarget(
    val userId: String,
    val username: String,
    val isMod: Boolean = false,
    val isSubscriber: Boolean = false,
    val isVip: Boolean = false,
    val isBroadcaster: Boolean = false
)

data class AutomodVerdict(
    val rule: AutomodRule,
    val matchedPattern: String,
    val action: AutomodAction,
    val timeoutSeconds: Int
)


object AutomodEngine {

    private val regexCache = mutableMapOf<Pair<String, Int>, Regex>()

    
    private val frequency = mutableMapOf<String, ArrayDeque<Long>>()

    fun evaluate(
        text: String,
        target: AutomodTarget,
        currentChannelLogin: String,
        rules: List<AutomodRule>
    ): AutomodVerdict? {
        if (text.isBlank() || rules.isEmpty()) return null
       
        if (target.isBroadcaster) return null

        for (rule in rules) {
            if (!rule.enabled) continue
            if (rule.scope == AutomodScope.LOCAL &&
                !rule.channelLogin.equals(currentChannelLogin, ignoreCase = true)
            ) continue
            if (rule.exemptMods && target.isMod) continue
            if (rule.exemptVips && target.isVip) continue
            if (rule.exemptSubs && target.isSubscriber) continue

            val matched = matchRule(rule, text) ?: continue

            if (rule.frequencyThreshold > 0) {
                val key = "${currentChannelLogin.lowercase()}#${rule.id}"
                if (!recordAndCheckFrequency(key, rule.frequencyThreshold, rule.frequencyWindowMs)) {
                    continue
                }
            }

            return AutomodVerdict(
                rule = rule,
                matchedPattern = matched,
                action = rule.action,
                timeoutSeconds = (rule.timeoutMs / 1000L).toInt().coerceAtLeast(1)
            )
        }
        return null
    }

    private fun matchRule(rule: AutomodRule, text: String): String? {
        val haystack = if (rule.caseSensitive) text else text.lowercase()
        for (pattern in rule.allPatterns) {
            val needle = if (rule.caseSensitive) pattern else pattern.lowercase()
            val hit = if (rule.isRegex) {
                regexFor(pattern, rule.caseSensitive)?.containsMatchIn(text) == true
            } else if (rule.wholeWord) {
                wholeWordContains(haystack, needle)
            } else {
                haystack.contains(needle)
            }
            if (hit) return pattern
        }
        return null
    }

    private fun regexFor(pattern: String, caseSensitive: Boolean): Regex? {
        val key = pattern to (if (caseSensitive) 1 else 0)
        regexCache[key]?.let { return it }
        return runCatching {
            val opts = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
            Regex(pattern, opts).also { regexCache[key] = it }
        }.getOrNull()
    }

    private fun wholeWordContains(haystack: String, needle: String): Boolean {
        if (needle.isEmpty()) return false
        var idx = haystack.indexOf(needle)
        while (idx >= 0) {
            val before = if (idx == 0) ' ' else haystack[idx - 1]
            val afterIdx = idx + needle.length
            val after = if (afterIdx >= haystack.length) ' ' else haystack[afterIdx]
            if (!before.isLetterOrDigit() && !after.isLetterOrDigit()) return true
            idx = haystack.indexOf(needle, idx + 1)
        }
        return false
    }

    private fun recordAndCheckFrequency(key: String, threshold: Int, windowMs: Long): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        val bucket = frequency.getOrPut(key) { ArrayDeque() }
        bucket.addLast(now)
        while (bucket.isNotEmpty() && now - bucket.first() > windowMs) {
            bucket.removeFirst()
        }
        return bucket.size >= threshold
    }

    
    fun invalidate() {
        regexCache.clear()
        frequency.clear()
    }
}
