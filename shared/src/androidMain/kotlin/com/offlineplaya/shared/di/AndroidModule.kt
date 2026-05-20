package com.offlineplaya.shared.di

import com.offlineplaya.shared.data.database.DatabaseDriverFactory
import com.offlineplaya.shared.data.image.createMusicBrainzArtSource
import com.offlineplaya.shared.data.metadata.AndroidMetadataReader
import com.offlineplaya.shared.data.scanner.SafFolderScanner
import com.offlineplaya.shared.domain.image.RemoteArtSource
import com.offlineplaya.shared.domain.scanner.FolderScanner
import com.offlineplaya.shared.domain.scanner.MetadataReader
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

val androidModule: Module = module {
    single { DatabaseDriverFactory(androidContext()) }

    // FolderScanner + MetadataReader contracts are defined in commonMain;
    // these are the Android-specific implementations.
    single { SafFolderScanner(androidContext()) } bind FolderScanner::class
    single { AndroidMetadataReader(androidContext()) } bind MetadataReader::class

    // MusicBrainz + Cover Art Archive lookup. Singleton — holds an in-memory
    // session cache and an OkHttpClient.
    single<RemoteArtSource> { createMusicBrainzArtSource() }
}
