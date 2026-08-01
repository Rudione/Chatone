package io.rudione.chatone.util.automod

import io.rudione.chatone.domain.model.AutomodAction
import io.rudione.chatone.domain.model.AutomodRule
import io.rudione.chatone.domain.model.AutomodScope
import io.rudione.chatone.domain.model.ChatRule
import io.rudione.chatone.domain.model.ChatRuleAction
import io.rudione.chatone.domain.model.ChatRuleType
import kotlin.time.Clock
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

    fun toCsv(wordRules: List<AutomodRule>, chatRules: List<ChatRule> = emptyList()): String = buildString {
        appendLine("# WORD FILTERS")
        appendLine("id\tscope\tchannel\tpattern\talternates\taction\ttimeoutMs\tisRegex\twholeWord\tcaseSensitive\tignoreLinks\tfrequencyThreshold\tfrequencyWindowMs\texemptMods\texemptSubs\texemptVips\tenabled\tnote")
        wordRules.forEach { r ->
            appendLine(listOf(
                r.id, r.scope.name, r.channelLogin ?: "", esc(r.pattern),
                r.alternates.joinToString(";"), r.action.name, r.timeoutMs,
                r.isRegex, r.wholeWord, r.caseSensitive, r.ignoreLinks,
                r.frequencyThreshold, r.frequencyWindowMs,
                r.exemptMods, r.exemptSubs, r.exemptVips, r.enabled, esc(r.note)
            ).joinToString("\t"))
        }
        appendLine()
        appendLine("# CHAT RULES")
        appendLine("id\ttype\tscope\tchannel\taction\ttimeoutSeconds\tspamMaxMessages\tspamWindowSeconds\tcapsThresholdPercent\tcapsMinLength\tlinksAllowClips\tlinksAllowedSites\tlinksRequireHttps\temoteMaxCount\tnewAccountAgeDays\tduplicateMinLength\tconsecutiveNumbersThreshold\texemptMods\texemptVips\texemptSubs\tenabled")
        chatRules.forEach { r ->
            appendLine(listOf(
                r.id, r.type.name, r.scope.name, r.channelLogin ?: "",
                r.action.name, r.timeoutSeconds,
                r.spamMaxMessages, r.spamWindowSeconds,
                r.capsThresholdPercent, r.capsMinLength,
                r.linksAllowClips, r.linksAllowedSites.joinToString(";"), r.linksRequireHttps,
                r.emoteMaxCount, r.newAccountAgeDays, r.duplicateMinLength, r.consecutiveNumbersThreshold,
                r.exemptMods, r.exemptVips, r.exemptSubs, r.enabled
            ).joinToString("\t"))
        }
    }

    fun toXlsx(wordRules: List<AutomodRule>, chatRules: List<ChatRule> = emptyList()): String =
        buildXlsxContent(wordRules, chatRules)

    fun fromCsv(text: String): ImportResult {
        val lines = text.lines()
        val wordRules = mutableListOf<AutomodRule>()
        val chatRules = mutableListOf<ChatRule>()
        var section = ""
        var headerIndices = mapOf<String, Int>()
        for (line in lines) {
            when {
                line.startsWith("# WORD FILTERS") -> { section = "word"; headerIndices = mapOf() }
                line.startsWith("# CHAT RULES") -> { section = "chat"; headerIndices = mapOf() }
                line.startsWith("id\t") -> {
                    headerIndices = line.split("\t").mapIndexed { i, h -> h to i }.toMap()
                }
                line.isBlank() || line.startsWith("#") -> {}
                else -> {
                    val cols = line.split("\t")
                    fun col(name: String) = headerIndices[name]?.let { cols.getOrNull(it) }?.trim() ?: ""
                    if (section == "word" && headerIndices.isNotEmpty()) {
                        runCatching {
                            wordRules.add(AutomodRule(
                                id = col("id").ifBlank { newId() },
                                scope = runCatching { AutomodScope.valueOf(col("scope")) }.getOrDefault(AutomodScope.GLOBAL),
                                channelLogin = col("channel").ifBlank { null },
                                pattern = col("pattern"),
                                alternates = col("alternates").split(";").map { it.trim() }.filter { it.isNotBlank() },
                                action = runCatching { AutomodAction.valueOf(col("action")) }.getOrDefault(AutomodAction.DELETE),
                                timeoutMs = col("timeoutMs").toLongOrNull() ?: 600_000L,
                                isRegex = col("isRegex").toBoolean(),
                                wholeWord = col("wholeWord").toBoolean(),
                                caseSensitive = col("caseSensitive").toBoolean(),
                                ignoreLinks = col("ignoreLinks").toBooleanStrictOrNull() ?: false,
                                frequencyThreshold = col("frequencyThreshold").toIntOrNull() ?: 0,
                                frequencyWindowMs = col("frequencyWindowMs").toLongOrNull() ?: 30_000L,
                                exemptMods = col("exemptMods").toBooleanStrictOrNull() ?: true,
                                exemptSubs = col("exemptSubs").toBooleanStrictOrNull() ?: false,
                                exemptVips = col("exemptVips").toBooleanStrictOrNull() ?: true,
                                enabled = col("enabled").toBooleanStrictOrNull() ?: true,
                                note = col("note"),
                                createdAt = 0L, updatedAt = 0L
                            ))
                        }
                    } else if (section == "chat" && headerIndices.isNotEmpty()) {
                        runCatching {
                            chatRules.add(ChatRule(
                                id = col("id").ifBlank { newId() },
                                type = runCatching { ChatRuleType.valueOf(col("type")) }.getOrDefault(ChatRuleType.SPAM_RATE),
                                scope = runCatching { AutomodScope.valueOf(col("scope")) }.getOrDefault(AutomodScope.GLOBAL),
                                channelLogin = col("channel").ifBlank { null },
                                action = runCatching { ChatRuleAction.valueOf(col("action")) }.getOrDefault(ChatRuleAction.DELETE),
                                timeoutSeconds = col("timeoutSeconds").toIntOrNull() ?: 60,
                                spamMaxMessages = col("spamMaxMessages").toIntOrNull() ?: 5,
                                spamWindowSeconds = col("spamWindowSeconds").toIntOrNull() ?: 10,
                                capsThresholdPercent = col("capsThresholdPercent").toIntOrNull() ?: 70,
                                capsMinLength = col("capsMinLength").toIntOrNull() ?: 8,
                                linksAllowClips = col("linksAllowClips").toBooleanStrictOrNull() ?: true,
                                linksAllowedSites = col("linksAllowedSites").split(";").map { it.trim() }.filter { it.isNotBlank() },
                                linksRequireHttps = col("linksRequireHttps").toBooleanStrictOrNull() ?: true,
                                emoteMaxCount = col("emoteMaxCount").toIntOrNull() ?: 8,
                                newAccountAgeDays = col("newAccountAgeDays").toIntOrNull() ?: 7,
                                duplicateMinLength = col("duplicateMinLength").toIntOrNull() ?: 8,
                                consecutiveNumbersThreshold = col("consecutiveNumbersThreshold").toIntOrNull() ?: 8,
                                exemptMods = col("exemptMods").toBooleanStrictOrNull() ?: true,
                                exemptVips = col("exemptVips").toBooleanStrictOrNull() ?: true,
                                exemptSubs = col("exemptSubs").toBooleanStrictOrNull() ?: false,
                                enabled = col("enabled").toBooleanStrictOrNull() ?: true,
                                createdAt = 0L
                            ))
                        }
                    }
                }
            }
        }
        return ImportResult(wordRules, chatRules)
    }

    fun fromJson(text: String): ImportResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return ImportResult(emptyList(), emptyList())
        if (trimmed.startsWith("# WORD FILTERS") || trimmed.startsWith("# CHAT RULES") || trimmed.startsWith("id\t")) {
            return fromCsv(trimmed)
        }
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
                linksRequireHttps = true,
                action = ChatRuleAction.DELETE,
                enabled = false, createdAt = now
            ),
            ChatRule(
                id = newId(), type = ChatRuleType.CONSECUTIVE_NUMBERS,
                scope = AutomodScope.GLOBAL,
                consecutiveNumbersThreshold = 8,
                action = ChatRuleAction.DELETE,
                enabled = false, createdAt = now
            )
        )
    }
}
