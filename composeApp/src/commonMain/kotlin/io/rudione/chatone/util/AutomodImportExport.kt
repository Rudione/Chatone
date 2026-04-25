package io.rudione.chatone.util

import io.rudione.chatone.domain.model.AutomodAction
import io.rudione.chatone.domain.model.AutomodRule
import io.rudione.chatone.domain.model.AutomodScope
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.random.Random


@Serializable
private data class AutomodExportFile(
    val version: Int = 1,
    val exportedAt: Long = 0L,
    val rules: List<AutomodRule> = emptyList()
)

object AutomodImportExport {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toJson(rules: List<AutomodRule>): String =
        json.encodeToString(
            AutomodExportFile.serializer(),
            AutomodExportFile(
                version = 1,
                exportedAt = Clock.System.now().toEpochMilliseconds(),
                rules = rules
            )
        )

    fun fromJson(text: String): List<AutomodRule> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        return runCatching {
            json.decodeFromString(AutomodExportFile.serializer(), trimmed).rules
        }.getOrElse {
           
            runCatching {
                json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(AutomodRule.serializer()), trimmed)
            }.getOrDefault(emptyList())
        }.map { rule ->
           
            rule.copy(
                id = if (rule.id.isBlank()) newId() else rule.id,
                createdAt = 0L,
                updatedAt = 0L
            )
        }
    }

    
    fun toMarkdown(rules: List<AutomodRule>): String {
        val header = "| TYPE | CHANNEL | PATTERN | ALTERNATES | ACTION | TIMEOUT(ms) | REGEX | WHOLE WORD | CASE | FREQ | WINDOW(ms) | EXEMPT MOD/SUB/VIP | ENABLED | NOTE |"
        val sep = "|------|---------|---------|-----------|--------|-------------|-------|-----------|------|------|------------|-------------------|---------|------|"
        val rows = rules.joinToString("\n") { r ->
            "| ${r.scope.name} | ${r.channelLogin ?: "-"} | ${escapeCell(r.pattern)} | ${escapeCell(r.alternates.joinToString("; "))} " +
                "| ${r.action.name} | ${r.timeoutMs} | ${r.isRegex} | ${r.wholeWord} | ${r.caseSensitive} " +
                "| ${r.frequencyThreshold} | ${r.frequencyWindowMs} | ${r.exemptMods}/${r.exemptSubs}/${r.exemptVips} " +
                "| ${r.enabled} | ${escapeCell(r.note)} |"
        }
        return buildString {
            appendLine("# Chatone Local Automod export")
            appendLine()
            appendLine("Exported at: ${Clock.System.now()}")
            appendLine("Total rules: ${rules.size}")
            appendLine()
            appendLine(header)
            appendLine(sep)
            append(rows)
            append("\n")
        }
    }

    
    fun newId(): String =
        "amrule_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(100_000, 999_999)}"

    private fun escapeCell(s: String): String =
        s.replace("|", "\\|").replace("\n", " ⏎ ").take(200)

    
    fun defaultStarterPack(): List<AutomodRule> {
        val now = Clock.System.now().toEpochMilliseconds()
        return listOf(
            AutomodRule(
                id = newId(),
                scope = AutomodScope.GLOBAL,
                pattern = "n-word",
                alternates = listOf("n!gger", "nigga"),
                action = AutomodAction.BAN,
                wholeWord = true,
                note = "Hard-line slur. Ships disabled — review before enabling.",
                enabled = false,
                createdAt = now,
                updatedAt = now
            ),
            AutomodRule(
                id = newId(),
                scope = AutomodScope.GLOBAL,
                pattern = "discord.gg/",
                action = AutomodAction.TIMEOUT,
                timeoutMs = 600_000L,
                note = "10-min timeout for Discord invites.",
                enabled = false,
                createdAt = now,
                updatedAt = now
            ),
            AutomodRule(
                id = newId(),
                scope = AutomodScope.GLOBAL,
                pattern = "(?i)f[u\\*]+ck",
                isRegex = true,
                action = AutomodAction.DELETE,
                note = "Delete f-bomb variants.",
                enabled = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}
