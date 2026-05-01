package io.rudione.chatone.util

import io.rudione.chatone.domain.model.AutomodAction
import io.rudione.chatone.domain.model.AutomodRule
import io.rudione.chatone.domain.model.AutomodScope
import io.rudione.chatone.domain.model.ChatRule
import io.rudione.chatone.domain.model.ChatRuleAction
import io.rudione.chatone.domain.model.ChatRuleType
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Serializable
private data class AutomodExportFile(
    val version: Int = 2,
    val exportedAt: Long = 0L,
    val wordRules: List<AutomodRule> = emptyList(),
    val chatRules: List<ChatRule> = emptyList(),
    val rules: List<AutomodRule> = emptyList()
)

data class ImportResult(
    val wordRules: List<AutomodRule>,
    val chatRules: List<ChatRule>
)

object AutomodImportExport {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }


    fun toJson(wordRules: List<AutomodRule>, chatRules: List<ChatRule> = emptyList()): String =
        json.encodeToString(
            AutomodExportFile.serializer(),
            AutomodExportFile(
                version = 2,
                exportedAt = Clock.System.now().toEpochMilliseconds(),
                wordRules = wordRules,
                chatRules = chatRules
            )
        )

    fun toMarkdown(wordRules: List<AutomodRule>, chatRules: List<ChatRule> = emptyList()): String = buildString {
        appendLine("# Chatone Automod Export")
        appendLine()
        appendLine("Exported at: ${Clock.System.now()}")
        appendLine("Word rules: ${wordRules.size}  |  Chat rules: ${chatRules.size}")
        appendLine()

        if (wordRules.isNotEmpty()) {
            appendLine("## Word Filters")
            appendLine()
            appendLine("| TYPE | CHANNEL | PATTERN | ALTERNATES | ACTION | TIMEOUT(ms) | REGEX | WHOLE WORD | CASE | FREQ | WINDOW(ms) | EXEMPT MOD/SUB/VIP | ENABLED | NOTE |")
            appendLine("|------|---------|---------|-----------|--------|-------------|-------|-----------|------|------|------------|-------------------|---------|------|")
            wordRules.forEach { r ->
                appendLine("| ${r.scope.name} | ${r.channelLogin ?: "-"} | ${esc(r.pattern)} | ${esc(r.alternates.joinToString("; "))} " +
                        "| ${r.action.name} | ${r.timeoutMs} | ${r.isRegex} | ${r.wholeWord} | ${r.caseSensitive} " +
                        "| ${r.frequencyThreshold} | ${r.frequencyWindowMs} | ${r.exemptMods}/${r.exemptSubs}/${r.exemptVips} " +
                        "| ${r.enabled} | ${esc(r.note)} |")
            }
            appendLine()
        }

        if (chatRules.isNotEmpty()) {
            appendLine("## Chat Rules")
            appendLine()
            appendLine("| TYPE | SCOPE | CHANNEL | ACTION | TIMEOUT(s) | CONFIG | EXEMPT MOD/VIP/SUB | ENABLED |")
            appendLine("|------|-------|---------|--------|------------|--------|-------------------|---------|")
            chatRules.forEach { r ->
                val cfg = when (r.type) {
                    ChatRuleType.SPAM_RATE -> "${r.spamMaxMessages}msg/${r.spamWindowSeconds}s"
                    ChatRuleType.ALL_CAPS -> "${r.capsThresholdPercent}% caps, min ${r.capsMinLength}"
                    ChatRuleType.LINKS -> "allowClips=${r.linksAllowClips}"
                    ChatRuleType.EMOTE_SPAM -> "max ${r.emoteMaxCount} emotes"
                    ChatRuleType.NEW_ACCOUNT -> "<${r.newAccountAgeDays}d"
                    ChatRuleType.DUPLICATE_MESSAGE -> "min ${r.duplicateMinLength} chars"
                }
                appendLine("| ${r.type.name} | ${r.scope.name} | ${r.channelLogin ?: "-"} | ${r.action.name} | ${r.timeoutSeconds} | $cfg | ${r.exemptMods}/${r.exemptVips}/${r.exemptSubs} | ${r.enabled} |")
            }
        }
    }


    fun fromJson(text: String): ImportResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ImportResult(emptyList(), emptyList())

        return runCatching {
            val file = json.decodeFromString(AutomodExportFile.serializer(), trimmed)
            val words = (file.wordRules + file.rules)
                .distinctBy { it.id }
                .map { it.copy(id = it.id.ifBlank { newId() }, createdAt = 0L, updatedAt = 0L) }
            val chats = file.chatRules
                .map { it.copy(id = it.id.ifBlank { newId() }, createdAt = 0L) }
            ImportResult(words, chats)
        }.getOrElse {
            val words = runCatching {
                json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(AutomodRule.serializer()), trimmed)
                    .map { it.copy(id = if (it.id.isBlank()) newId() else it.id, createdAt = 0L, updatedAt = 0L) }
            }.getOrDefault(emptyList())
            ImportResult(words, emptyList())
        }
    }


    fun newId(): String =
        "rule_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(100_000, 999_999)}"

    private fun esc(s: String): String =
        s.replace("|", "\\|").replace("\n", " ⏎ ").take(200)


    fun defaultWordStarterPack(): List<AutomodRule> {
        val now = Clock.System.now().toEpochMilliseconds()
        return listOf(
            AutomodRule(
                id = newId(), scope = AutomodScope.GLOBAL, pattern = "n-word",
                alternates = listOf("n!gger", "nigga"), action = AutomodAction.BAN, wholeWord = true,
                note = "Hard-line slur. Ships disabled — review before enabling.",
                enabled = false, createdAt = now, updatedAt = now
            ),
            AutomodRule(
                id = newId(), scope = AutomodScope.GLOBAL, pattern = "discord.gg/",
                action = AutomodAction.TIMEOUT, timeoutMs = 600_000L,
                note = "10-min timeout for Discord invites.", enabled = false, createdAt = now, updatedAt = now
            ),
            AutomodRule(
                id = newId(), scope = AutomodScope.GLOBAL, pattern = "(?i)f[u\\*]+ck",
                isRegex = true, action = AutomodAction.DELETE,
                note = "Delete f-bomb variants.", enabled = false, createdAt = now, updatedAt = now
            )
        )
    }

    fun defaultStarterPack() = defaultWordStarterPack()

    fun defaultChatStarterPack(): List<ChatRule> {
        val now = Clock.System.now().toEpochMilliseconds()
        return listOf(
            ChatRule(
                id = newId(), type = ChatRuleType.SPAM_RATE,
                scope = AutomodScope.GLOBAL,
                spamMaxMessages = 6, spamWindowSeconds = 8,
                action = ChatRuleAction.TIMEOUT, timeoutSeconds = 60,
                enabled = false, createdAt = now
            ),
            ChatRule(
                id = newId(), type = ChatRuleType.ALL_CAPS,
                scope = AutomodScope.GLOBAL,
                capsThresholdPercent = 80, capsMinLength = 10,
                action = ChatRuleAction.DELETE,
                enabled = false, createdAt = now
            ),
            ChatRule(
                id = newId(), type = ChatRuleType.LINKS,
                scope = AutomodScope.GLOBAL,
                linksAllowClips = true,
                action = ChatRuleAction.DELETE,
                enabled = false, createdAt = now
            )
        )
    }
}
