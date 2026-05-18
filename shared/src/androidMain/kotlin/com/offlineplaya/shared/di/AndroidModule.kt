package com.offlineplaya.shared.di

import com.offlineplaya.shared.data.database.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

val androidModule: Module = module {
    single { DatabaseDriverFactory(androidContext()) }
}
