package io.rudione.chatone.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val driver = AndroidSqliteDriver(ChatoneDatabase.Schema, context, "chatone.db")
        ensureTablesExist(driver)
        migrateChatRuleColumns(driver)
        return driver
    }


    private fun ensureTablesExist(driver: SqlDriver) {
        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS AutomodRuleEntity (
                id TEXT NOT NULL PRIMARY KEY,
                scope TEXT NOT NULL,
                channelLogin TEXT,
                pattern TEXT NOT NULL,
                alternates TEXT NOT NULL DEFAULT '',
                isRegex INTEGER NOT NULL DEFAULT 0,
                caseSensitive INTEGER NOT NULL DEFAULT 0,
                wholeWord INTEGER NOT NULL DEFAULT 0,
                action TEXT NOT NULL,
                timeoutMs INTEGER NOT NULL DEFAULT 60000,
                frequencyThreshold INTEGER NOT NULL DEFAULT 0,
                frequencyWindowMs INTEGER NOT NULL DEFAULT 60000,
                exemptMods INTEGER NOT NULL DEFAULT 1,
                exemptSubs INTEGER NOT NULL DEFAULT 0,
                exemptVips INTEGER NOT NULL DEFAULT 1,
                enabled INTEGER NOT NULL DEFAULT 1,
                note TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """.trimIndent(), 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_automod_scope ON AutomodRuleEntity(scope)", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_automod_channel ON AutomodRuleEntity(channelLogin)", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_automod_enabled ON AutomodRuleEntity(enabled)", 0)

        driver.execute(null, """
            CREATE TABLE IF NOT EXISTS UserNicknameEntity (
                twitchUserId TEXT NOT NULL PRIMARY KEY,
                nickname TEXT NOT NULL DEFAULT '',
                updatedAt INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent(), 0)
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

        addColIfMissing("ruleKind",                  "TEXT NOT NULL DEFAULT 'WORD'")
        addColIfMissing("chatRuleType",              "TEXT")
        addColIfMissing("spamMaxMessages",           "INTEGER NOT NULL DEFAULT 5")
        addColIfMissing("spamWindowSeconds",         "INTEGER NOT NULL DEFAULT 10")
        addColIfMissing("capsThresholdPercent",      "INTEGER NOT NULL DEFAULT 70")
        addColIfMissing("capsMinLength",             "INTEGER NOT NULL DEFAULT 8")
        addColIfMissing("linksAllowClips",           "INTEGER NOT NULL DEFAULT 1")
        addColIfMissing("linksClipsSameChannelOnly", "INTEGER NOT NULL DEFAULT 0")
        addColIfMissing("linksClipsAllowedChannels", "TEXT NOT NULL DEFAULT ''")
        addColIfMissing("eventMessage",              "TEXT NOT NULL DEFAULT ''")
        addColIfMissing("eventRepeat",               "INTEGER NOT NULL DEFAULT 1")
        addColIfMissing("eventDelaySeconds",         "INTEGER NOT NULL DEFAULT 0")
        addColIfMissing("emoteMaxCount",             "INTEGER NOT NULL DEFAULT 8")
        addColIfMissing("newAccountAgeDays",         "INTEGER NOT NULL DEFAULT 7")
        addColIfMissing("duplicateMinLength",        "INTEGER NOT NULL DEFAULT 8")
        addColIfMissing("timeoutSeconds",            "INTEGER NOT NULL DEFAULT 60")

        runCatching {
            driver.execute(null,
                "CREATE INDEX IF NOT EXISTS idx_automod_kind ON AutomodRuleEntity(ruleKind);", 0)
        }
    }
}
