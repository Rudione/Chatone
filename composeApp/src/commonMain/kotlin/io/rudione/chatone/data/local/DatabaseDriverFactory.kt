package io.rudione.chatone.data.local

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DatabaseDriverFactory): ChatoneDatabase {
    val driver = driverFactory.createDriver()
    runCatching {
        driver.execute(null, "ALTER TABLE AutomodRuleEntity ADD COLUMN linksRequireHttps INTEGER NOT NULL DEFAULT 1", 0)
    }
    runCatching {
        driver.execute(null, "ALTER TABLE AutomodRuleEntity ADD COLUMN linksAllowedSites TEXT NOT NULL DEFAULT ''", 0)
    }
    return ChatoneDatabase(driver)
}
