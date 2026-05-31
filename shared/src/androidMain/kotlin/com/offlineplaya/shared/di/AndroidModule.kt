package com.offlineplaya.shared.di

import com.offlineplaya.shared.data.database.DatabaseDriverFactory
import com.offlineplaya.shared.data.image.JaudiotaggerArtWriter
import com.offlineplaya.shared.data.image.SafFolderArtSource
import com.offlineplaya.shared.data.image.createMusicBrainzArtSource
import com.offlineplaya.shared.data.metadata.AndroidMetadataReader
import com.offlineplaya.shared.data.scanner.MediaStoreDeviceAudioScanner
import com.offlineplaya.shared.data.scanner.SafFolderScanner
import com.offlineplaya.shared.data.scheduling.WorkManagerTaskRunner
import com.offlineplaya.shared.domain.image.AlbumArtWriter
import com.offlineplaya.shared.domain.image.FolderArtSource
import com.offlineplaya.shared.domain.image.RemoteArtSource
import com.offlineplaya.shared.domain.scanner.DeviceAudioScanner
import com.offlineplaya.shared.domain.scanner.FolderScanner
import com.offlineplaya.shared.domain.scanner.MetadataReader
import com.offlineplaya.shared.domain.scheduling.BackgroundTaskRunner
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

val androidModule: Module = module {
    single { DatabaseDriverFactory(androidContext()) }

    // FolderScanner + MetadataReader contracts are defined in commonMain;
    // these are the Android-specific implementations.
    single { SafFolderScanner(androidContext(), get()) } bind FolderScanner::class
    single { AndroidMetadataReader(androidContext()) } bind MetadataReader::class

    // MediaStore-backed audio index. Lets the app see music in folders SAF
    // can't tree-pick (Download/, internal-storage root) without making the
    // user grant MANAGE_EXTERNAL_STORAGE. Returns an empty list when the
    // READ_MEDIA_AUDIO permission isn't held — sync treats that as a no-op.
    single<DeviceAudioScanner> { MediaStoreDeviceAudioScanner(androidContext()) }

    // Deezer + MusicBrainz/CAA album art + artist image lookup. Singleton with
    // persistent miss cache so failed lookups don't waste data on every restart.
    single<RemoteArtSource> { createMusicBrainzArtSource(get()) }

    // Sidecar cover.jpg / folder.jpg / album.jpg lookup inside the track's
    // SAF folder. Checked before remote so ripped/torrented folders with
    // bundled artwork never hit the network.
    single<FolderArtSource> { SafFolderArtSource(androidContext(), get()) }

    // Jaudiotagger-backed art writer. Reads via MediaMetadataRetriever (so
    // hasEmbeddedArt is cheap) and writes via the temp-file FD dance.
    single<AlbumArtWriter> { JaudiotaggerArtWriter(androidContext(), get()) }

    // WorkManager-backed dispatch for long-running tasks declared in commonMain
    // (BackgroundTaskKind). Other targets will bind their own implementation —
    // a plain coroutine on Desktop, BGTaskScheduler on iOS.
    single<BackgroundTaskRunner> { WorkManagerTaskRunner(androidContext()) }
}
