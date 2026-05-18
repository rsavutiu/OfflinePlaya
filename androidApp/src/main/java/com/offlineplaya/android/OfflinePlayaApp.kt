package com.offlineplaya.android

import android.app.Application
import com.offlineplaya.shared.di.androidModule
import com.offlineplaya.shared.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level

class OfflinePlayaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(
            appDeclaration = {
                androidLogger(Level.INFO)
                androidContext(this@OfflinePlayaApp)
            },
            platformModules = listOf(androidModule),
        )
    }
}
