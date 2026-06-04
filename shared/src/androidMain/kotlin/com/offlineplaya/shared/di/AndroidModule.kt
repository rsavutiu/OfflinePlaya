package com.offlineplaya.shared.di

import com.offlineplaya.shared.data.database.DatabaseDriverFactory
import com.offlineplaya.shared.data.genre.JaudiotaggerGenreWriter
import com.offlineplaya.shared.data.image.CoilPaletteColorExtractor
import com.offlineplaya.shared.data.image.JaudiotaggerArtWriter
import com.offlineplaya.shared.data.image.MusicBrainzArtSource
import com.offlineplaya.shared.data.image.SafFolderArtSource
import com.offlineplaya.shared.data.image.createMusicBrainzSource
import com.offlineplaya.shared.data.metadata.AndroidMetadataReader
import com.offlineplaya.shared.data.scanner.MediaStoreDeviceAudioScanner
import com.offlineplaya.shared.data.scanner.SafFolderScanner
import com.offlineplaya.shared.data.scheduling.WorkManagerTaskRunner
import com.offlineplaya.shared.domain.genre.GenreTagWriter
import com.offlineplaya.shared.domain.genre.RemoteGenreSource
import com.offlineplaya.shared.domain.image.AlbumArtColorExtractor
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
    single<DeviceAudioScanner> {
        MediaStoreDeviceAudioScanner(
            context = androidContext(),
            logger = get()
        )
    }

    // Deezer + MusicBrainz/CAA album art + artist image lookup, plus genre
    // resolution. The concrete class implements both interfaces; binding the
    // same singleton to both means art-lookup and genre-lookup share the
    // same rate-limit mutex and miss-cache instead of each running their own.
    single { createMusicBrainzSource(get()) }
    single<RemoteArtSource> { get<MusicBrainzArtSource>() }
    single<RemoteGenreSource> { get<MusicBrainzArtSource>() }

    // Extracts a seed color from album art (Palette over the Coil chain) to
    // drive the album-art reactive theme. Reuses the singleton ImageLoader so
    // extraction is usually a cache hit on art the user has already seen.
    single { CoilPaletteColorExtractor(androidContext(), get()) } bind AlbumArtColorExtractor::class

    // Sidecar cover.jpg / folder.jpg / album.jpg lookup inside the track's
    // SAF folder. Checked before remote so ripped/torrented folders with
    // bundled artwork never hit the network.
    single<FolderArtSource> { SafFolderArtSource(androidContext(), get()) }

    // Jaudiotagger-backed art writer. Reads via MediaMetadataRetriever (so
    // hasEmbeddedArt is cheap) and writes via the temp-file FD dance.
    single<AlbumArtWriter> { JaudiotaggerArtWriter(androidContext(), get()) }

    // Jaudiotagger-backed genre tag writer. Same temp-file dance as the art
    // writer; reads via Jaudiotagger too since MediaMetadataRetriever doesn't
    // round-trip the genre string reliably across container formats.
    single<GenreTagWriter> { JaudiotaggerGenreWriter(androidContext(), get()) }

    // WorkManager-backed dispatch for long-running tasks declared in commonMain
    // (BackgroundTaskKind). Other targets will bind their own implementation —
    // a plain coroutine on Desktop, BGTaskScheduler on iOS.
    single<BackgroundTaskRunner> { WorkManagerTaskRunner(androidContext()) }
}
