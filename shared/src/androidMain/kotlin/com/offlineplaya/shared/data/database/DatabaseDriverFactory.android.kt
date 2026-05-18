package com.offlineplaya.shared.data.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.offlineplaya.shared.database.OfflinePlayaDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver = AndroidSqliteDriver(
        schema = OfflinePlayaDatabase.Schema,
        context = context,
        name = "offlineplaya.db",
    )
}
