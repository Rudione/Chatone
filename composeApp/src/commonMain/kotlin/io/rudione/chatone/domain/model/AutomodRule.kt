package io.rudione.chatone.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AutomodScope { GLOBAL, LOCAL }

@Serializable
enum class AutomodAction { DELETE, TIMEOUT, BAN }


@Serializable
data class AutomodRule(
    val id: String,
    val scope: AutomodScope,
    val channelLogin: String? = null,
    val pattern: String,
    val alternates: List<String> = emptyList(),
    val isRegex: Boolean = false,
    val caseSensitive: Boolean = false,
    val wholeWord: Boolean = false,
    val action: AutomodAction = AutomodAction.DELETE,
    val timeoutMs: Long = 60_000L,
    val frequencyThreshold: Int = 0,
    val frequencyWindowMs: Long = 60_000L,
    val exemptMods: Boolean = true,
    val exemptSubs: Boolean = false,
    val exemptVips: Boolean = true,
    val enabled: Boolean = true,
    val note: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    
    val allPatterns: List<String>
        get() = (listOf(pattern) + alternates).map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    val displayLabel: String
        get() = pattern.ifBlank { "(empty)" }

    val scopeLabel: String
        get() = when (scope) {
            AutomodScope.GLOBAL -> "GLOBAL"
            AutomodScope.LOCAL -> "#${channelLogin.orEmpty()}"
        }
}


internal const val AUTOMOD_ALT_DELIM = "\n"

internal fun List<String>.encodeAlternates(): String =
    filter { it.isNotBlank() }.joinToString(AUTOMOD_ALT_DELIM) { it.trim() }

internal fun String.decodeAlternates(): List<String> =
    if (isBlank()) emptyList() else split(AUTOMOD_ALT_DELIM).map { it.trim() }.filter { it.isNotEmpty() }
