package io.rudione.chatone.data.local

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DatabaseDriverFactory): ChatoneDatabase {
    val driver = driverFactory.createDriver()
    SchemaHealer.heal(driver)
    return ChatoneDatabase(driver)
}
