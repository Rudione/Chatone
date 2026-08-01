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

        return driver
    }
}
