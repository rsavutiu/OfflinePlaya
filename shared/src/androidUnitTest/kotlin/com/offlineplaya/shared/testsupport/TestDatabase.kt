package com.offlineplaya.shared.testsupport

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.offlineplaya.shared.database.OfflinePlayaDatabase

/**
 * In-memory SQLite database for JVM unit tests. Each call returns a fresh database
 * with the SQLDelight schema applied and foreign-key enforcement enabled, isolated
 * from other tests.
 */
fun createInMemoryDatabase(): OfflinePlayaDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
    OfflinePlayaDatabase.Schema.create(driver)
    driver.execute(identifier = null, sql = "PRAGMA foreign_keys = ON", parameters = 0)
    return OfflinePlayaDatabase(driver)
}
