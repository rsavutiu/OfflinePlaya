package com.offlineplaya.shared.data.database

import app.cash.sqldelight.db.SqlDriver
import com.offlineplaya.shared.database.OfflinePlayaDatabase

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

fun createDatabase(driverFactory: DatabaseDriverFactory): OfflinePlayaDatabase {
    val driver = driverFactory.createDriver()
    enableForeignKeys(driver)
    return OfflinePlayaDatabase(driver)
}

internal fun enableForeignKeys(driver: SqlDriver) {
    driver.execute(identifier = null, sql = "PRAGMA foreign_keys = ON", parameters = 0)
}
