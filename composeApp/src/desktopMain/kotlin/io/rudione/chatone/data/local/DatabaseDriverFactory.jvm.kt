package io.rudione.chatone.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val databasePath = File(System.getProperty("user.home"), ".chatone/chatone.db")
        databasePath.parentFile?.mkdirs()

        val dbExists = databasePath.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${databasePath.absolutePath}")

        if (!dbExists) {
            ChatoneDatabase.Schema.create(driver)
        } else {
            val currentVersion = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA user_version;",
                mapper = { cursor ->
                    cursor.next()
                    app.cash.sqldelight.db.QueryResult.Value(cursor.getLong(0) ?: 0L)
                },
                parameters = 0
            ).value

            val schemaVersion = ChatoneDatabase.Schema.version
            if (currentVersion < schemaVersion) {
                ChatoneDatabase.Schema.migrate(driver, currentVersion, schemaVersion)
            }
        }

        ensureTablesExist(driver)
        migrateChatRuleColumns(driver)

        return driver
    }

    private fun migrateChatRuleColumns(driver: SqlDriver) {
        val existingColumns = mutableSetOf<String>()
        runCatching {
            driver.executeQuery(null, "PRAGMA table_info(AutomodRuleEntity);", { cursor ->
                while (cursor.next().value) {
                    existingColumns += (cursor.getString(1) ?: "").lowercase()
                }
                app.cash.sqldelight.db.QueryResult.Unit
            }, 0)
        }

        fun addColIfMissing(col: String, def: String) {
            if (col.lowercase() !in existingColumns) {
                runCatching {
                    driver.execute(null,
                        "ALTER TABLE AutomodRuleEntity ADD COLUMN $col $def;", 0)
                }
            }
        }

        addColIfMissing("ruleKind",              "TEXT NOT NULL DEFAULT 'WORD'")
        addColIfMissing("chatRuleType",           "TEXT")
        addColIfMissing("spamMaxMessages",        "INTEGER NOT NULL DEFAULT 5")
        addColIfMissing("spamWindowSeconds",      "INTEGER NOT NULL DEFAULT 10")
        addColIfMissing("capsThresholdPercent",   "INTEGER NOT NULL DEFAULT 70")
        addColIfMissing("capsMinLength",          "INTEGER NOT NULL DEFAULT 8")
        addColIfMissing("linksAllowClips",        "INTEGER NOT NULL DEFAULT 1")
        addColIfMissing("emoteMaxCount",          "INTEGER NOT NULL DEFAULT 8")
        addColIfMissing("newAccountAgeDays",      "INTEGER NOT NULL DEFAULT 7")
        addColIfMissing("duplicateMinLength",     "INTEGER NOT NULL DEFAULT 8")
        addColIfMissing("timeoutSeconds",         "INTEGER NOT NULL DEFAULT 60")

        runCatching {
            driver.execute(null,
                "CREATE INDEX IF NOT EXISTS idx_automod_kind ON AutomodRuleEntity(ruleKind);", 0)
        }
    }

    private fun ensureTablesExist(driver: SqlDriver) {
        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS ChannelFolderEntity (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                color TEXT NOT NULL DEFAULT '#9146FF',
                sortOrder INTEGER NOT NULL DEFAULT 0,
                isExpanded INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
            )
        """.trimIndent(), 0)

        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS ChannelFolderMapping (
                channelId TEXT NOT NULL,
                folderId TEXT NOT NULL,
                sortOrder INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (channelId, folderId),
                FOREIGN KEY (folderId) REFERENCES ChannelFolderEntity(id) ON DELETE CASCADE
            )
        """.trimIndent(), 0)

        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS UserNoteEntity (
                twitchUserId TEXT NOT NULL PRIMARY KEY,
                note TEXT NOT NULL DEFAULT '',
                updatedAt INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent(), 0)

        driver.execute(null, """
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
        """.trimIndent(), 0)

        driver.execute(null, """
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
        """.trimIndent(), 0)

        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_automod_scope   ON AutomodRuleEntity(scope)", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_automod_channel ON AutomodRuleEntity(channelLogin)", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_automod_enabled ON AutomodRuleEntity(enabled)", 0)
    }
}