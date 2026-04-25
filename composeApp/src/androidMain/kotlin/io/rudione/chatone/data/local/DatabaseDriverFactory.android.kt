package io.rudione.chatone.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val driver = AndroidSqliteDriver(ChatoneDatabase.Schema, context, "chatone.db")
        ensureTablesExist(driver)
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
    }
}
