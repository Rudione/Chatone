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

        // Ensure tables added after initial schema exist (handles stale DBs without migration files)
        ensureTablesExist(driver)

        return driver
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
    }
}
