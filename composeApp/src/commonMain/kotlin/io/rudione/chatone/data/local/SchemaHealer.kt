package io.rudione.chatone.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import io.github.aakira.napier.Napier

internal object SchemaHealer {

    private const val TAG = "SchemaHealer"

    private val TABLES: List<String> = listOf(
        """
        CREATE TABLE IF NOT EXISTS ChannelFolderEntity (
            id TEXT NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            color TEXT NOT NULL DEFAULT '#9146FF',
            sortOrder INTEGER NOT NULL DEFAULT 0,
            isExpanded INTEGER NOT NULL DEFAULT 1,
            createdAt INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS ChannelFolderMapping (
            channelId TEXT NOT NULL,
            folderId TEXT NOT NULL,
            sortOrder INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY (channelId, folderId),
            FOREIGN KEY (folderId) REFERENCES ChannelFolderEntity(id) ON DELETE CASCADE
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS UserNoteEntity (
            twitchUserId TEXT NOT NULL PRIMARY KEY,
            note TEXT NOT NULL DEFAULT '',
            updatedAt INTEGER NOT NULL DEFAULT 0
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS UserNicknameEntity (
            twitchUserId TEXT NOT NULL PRIMARY KEY,
            nickname TEXT NOT NULL DEFAULT '',
            updatedAt INTEGER NOT NULL DEFAULT 0
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS MentionEntity (
            id TEXT NOT NULL PRIMARY KEY,
            channelLogin TEXT NOT NULL,
            fromUsername TEXT NOT NULL,
            fromDisplayName TEXT NOT NULL,
            fromColor TEXT,
            text TEXT NOT NULL,
            timestamp INTEGER NOT NULL,
            isRead INTEGER NOT NULL DEFAULT 0
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS ModerationEventEntity (
            id TEXT NOT NULL PRIMARY KEY,
            channelId TEXT NOT NULL,
            targetUserId TEXT NOT NULL,
            targetLogin TEXT NOT NULL,
            action TEXT NOT NULL,
            durationSeconds INTEGER,
            reason TEXT,
            moderatorLogin TEXT,
            timestamp INTEGER NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS AutomodRuleEntity (
            id TEXT NOT NULL PRIMARY KEY,
            scope TEXT NOT NULL,
            channelLogin TEXT,
            pattern TEXT NOT NULL DEFAULT '',
            alternates TEXT NOT NULL DEFAULT '',
            isRegex INTEGER NOT NULL DEFAULT 0,
            caseSensitive INTEGER NOT NULL DEFAULT 0,
            wholeWord INTEGER NOT NULL DEFAULT 0,
            action TEXT NOT NULL DEFAULT 'DELETE',
            timeoutMs INTEGER NOT NULL DEFAULT 60000,
            frequencyThreshold INTEGER NOT NULL DEFAULT 0,
            frequencyWindowMs INTEGER NOT NULL DEFAULT 60000,
            exemptMods INTEGER NOT NULL DEFAULT 1,
            exemptSubs INTEGER NOT NULL DEFAULT 0,
            exemptVips INTEGER NOT NULL DEFAULT 1,
            enabled INTEGER NOT NULL DEFAULT 1,
            note TEXT NOT NULL DEFAULT '',
            createdAt INTEGER NOT NULL DEFAULT 0,
            updatedAt INTEGER NOT NULL DEFAULT 0
        )
        """
    )

    private val COLUMNS: Map<String, List<Pair<String, String>>> = mapOf(
        "AutomodRuleEntity" to listOf(
            "ruleKind" to "TEXT NOT NULL DEFAULT 'WORD'",
            "chatRuleType" to "TEXT",
            "spamMaxMessages" to "INTEGER NOT NULL DEFAULT 5",
            "spamWindowSeconds" to "INTEGER NOT NULL DEFAULT 10",
            "capsThresholdPercent" to "INTEGER NOT NULL DEFAULT 70",
            "capsMinLength" to "INTEGER NOT NULL DEFAULT 8",
            "linksAllowClips" to "INTEGER NOT NULL DEFAULT 1",
            "linksClipsSameChannelOnly" to "INTEGER NOT NULL DEFAULT 0",
            "linksClipsAllowedChannels" to "TEXT NOT NULL DEFAULT ''",
            "linksRequireHttps" to "INTEGER NOT NULL DEFAULT 1",
            "linksAllowedSites" to "TEXT NOT NULL DEFAULT ''",
            "ignoreLinks" to "INTEGER NOT NULL DEFAULT 0",
            "emoteMaxCount" to "INTEGER NOT NULL DEFAULT 8",
            "newAccountAgeDays" to "INTEGER NOT NULL DEFAULT 7",
            "duplicateMinLength" to "INTEGER NOT NULL DEFAULT 8",
            "consecutiveNumbersThreshold" to "INTEGER NOT NULL DEFAULT 0",
            "timeoutSeconds" to "INTEGER NOT NULL DEFAULT 60",
            "eventMessage" to "TEXT NOT NULL DEFAULT ''",
            "eventRepeat" to "INTEGER NOT NULL DEFAULT 1",
            "eventDelaySeconds" to "INTEGER NOT NULL DEFAULT 0"
        )
    )

    private val INDEXES: List<String> = listOf(
        "CREATE INDEX IF NOT EXISTS idx_automod_scope ON AutomodRuleEntity(scope)",
        "CREATE INDEX IF NOT EXISTS idx_automod_channel ON AutomodRuleEntity(channelLogin)",
        "CREATE INDEX IF NOT EXISTS idx_automod_enabled ON AutomodRuleEntity(enabled)",
        "CREATE INDEX IF NOT EXISTS idx_automod_kind ON AutomodRuleEntity(ruleKind)",
        "CREATE INDEX IF NOT EXISTS idx_moderation_event_target ON ModerationEventEntity(channelId, targetUserId)",
        "CREATE INDEX IF NOT EXISTS idx_message_channel_user ON MessageEntity(channelId, userId)",
        "CREATE INDEX IF NOT EXISTS idx_message_channel_time ON MessageEntity(channelId, timestamp)"
    )

    fun heal(driver: SqlDriver) {
        TABLES.forEach { ddl ->
            runCatching { driver.execute(null, ddl.trimIndent(), 0) }
                .onFailure { Napier.w("Table DDL failed: ${it.message}", tag = TAG) }
        }

        COLUMNS.forEach { (table, columns) ->
            val existing = readColumns(driver, table)
            if (existing.isEmpty()) return@forEach
            columns.forEach { (name, definition) ->
                if (name.lowercase() !in existing) {
                    runCatching {
                        driver.execute(null, "ALTER TABLE $table ADD COLUMN $name $definition", 0)
                    }.onFailure { Napier.w("Add column $table.$name failed: ${it.message}", tag = TAG) }
                }
            }
        }

        INDEXES.forEach { ddl ->
            runCatching { driver.execute(null, ddl, 0) }
                .onFailure { Napier.w("Index DDL failed: ${it.message}", tag = TAG) }
        }
    }

    private fun readColumns(driver: SqlDriver, table: String): Set<String> {
        val columns = mutableSetOf<String>()
        runCatching {
            driver.executeQuery(null, "PRAGMA table_info($table);", { cursor ->
                while (cursor.next().value) {
                    cursor.getString(1)?.let { columns += it.lowercase() }
                }
                QueryResult.Unit
            }, 0)
        }
        return columns
    }
}
