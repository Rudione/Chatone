package io.rudione.chatone.data.local

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = NativeSqliteDriver(ChatoneDatabase.Schema, "chatone.db")
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
                QueryResult.Unit
            }, 0)
        }

        fun addColIfMissing(col: String, def: String) {
            if (col.lowercase() !in existingColumns) {
                runCatching {
                    driver.execute(null, "ALTER TABLE AutomodRuleEntity ADD COLUMN $col $def;", 0)
                }
            }
        }

        addColIfMissing("linksClipsSameChannelOnly", "INTEGER NOT NULL DEFAULT 0")
        addColIfMissing("linksClipsAllowedChannels", "TEXT NOT NULL DEFAULT ''")
        addColIfMissing("eventMessage",              "TEXT NOT NULL DEFAULT ''")
        addColIfMissing("eventRepeat",               "INTEGER NOT NULL DEFAULT 1")
        addColIfMissing("eventDelaySeconds",         "INTEGER NOT NULL DEFAULT 0")
    }
}
