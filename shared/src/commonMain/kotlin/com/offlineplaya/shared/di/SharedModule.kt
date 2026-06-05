package com.offlineplaya.shared.di

import com.offlineplaya.shared.data.database.createDatabase
import com.offlineplaya.shared.data.repository.SqlAlbumRepository
import com.offlineplaya.shared.data.repository.SqlArtistRepository
import com.offlineplaya.shared.data.repository.SqlFolderRepository
import com.offlineplaya.shared.data.repository.SqlManagedTreeRootRepository
import com.offlineplaya.shared.data.repository.SqlPlaylistRepository
import com.offlineplaya.shared.data.repository.SqlQueueRepository
import com.offlineplaya.shared.data.repository.SqlRecentAlbumRepository
import com.offlineplaya.shared.data.repository.SqlSettingsRepository
import com.offlineplaya.shared.data.repository.SqlTrackRepository
import com.offlineplaya.shared.domain.repository.AlbumRepository
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.domain.repository.FolderRepository
import com.offlineplaya.shared.domain.repository.ManagedTreeRootRepository
import com.offlineplaya.shared.domain.repository.PlaylistRepository
import com.offlineplaya.shared.domain.repository.QueueRepository
import com.offlineplaya.shared.domain.repository.RecentAlbumRepository
import com.offlineplaya.shared.domain.repository.SettingsRepository
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.domain.usecase.BurnMetadataUseCase
import com.offlineplaya.shared.domain.usecase.LibrarySyncUseCase
import com.offlineplaya.shared.presentation.eq.EqualizerStateHolder
import com.offlineplaya.shared.presentation.library.LibraryStateHolder
import com.offlineplaya.shared.presentation.lyrics.LyricsStateHolder
import com.offlineplaya.shared.presentation.metadata.BurnMetadataCoordinator
import com.offlineplaya.shared.presentation.navigation.AppNavigator
import com.offlineplaya.shared.presentation.playlist.PlaylistStateHolder
import com.offlineplaya.shared.presentation.settings.ArtworkStateHolder
import com.offlineplaya.shared.presentation.settings.PlaybackTuningStateHolder
import com.offlineplaya.shared.presentation.settings.ThemeStateHolder
import com.offlineplaya.shared.presentation.sync.LibrarySyncCoordinator
import com.offlineplaya.shared.presentation.theme.AlbumColorStateHolder
import com.offlineplaya.shared.util.createLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module

val sharedModule: Module = module {
    single { createLogger() }
    single { createDatabase(get()) }

    single { SqlTrackRepository(get(), get()) } bind TrackRepository::class
    single { SqlArtistRepository(get(), get()) } bind ArtistRepository::class
    single { SqlAlbumRepository(get(), get()) } bind AlbumRepository::class
    single { SqlRecentAlbumRepository(get(), get()) } bind RecentAlbumRepository::class
    single { SqlFolderRepository(get(), get()) } bind FolderRepository::class
    single { SqlPlaylistRepository(get()) } bind PlaylistRepository::class
    single { SqlQueueRepository(get()) } bind QueueRepository::class
    single { SqlManagedTreeRootRepository(get()) } bind ManagedTreeRootRepository::class
    single { SqlSettingsRepository(get()) } bind SettingsRepository::class

    // Use cases. Scanner / MetadataReader bindings live in the platform module
    // (androidModule for Android; tests inject fakes directly).
    factory {
        LibrarySyncUseCase(
            managedRoots = get(),
            folders = get(),
            artists = get(),
            albums = get(),
            tracks = get(),
            scanner = get(),
            metadataReader = get(),
            deviceAudio = get(),
            logger = get(),
        )
    }

    // Burn-metadata pass. Combines art and genre lookups/writes.
    factory {
        BurnMetadataUseCase(
            tracks = get(),
            artSource = get(),
            folderArtSource = get(),
            artWriter = get(),
            genreSource = get(),
            genreWriter = get(),
            logger = get(),
        )
    }

    // Application-scoped CoroutineScope for fire-and-forget background work
    // (sync, future re-scan). SupervisorJob so a single failure doesn't kill it.
    single<CoroutineScope> {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    // Presentation-layer coordinator: bridges SAF picker results into the use
    // case and exposes a SyncStatus flow for the UI.
    single {
        LibrarySyncCoordinator(
            syncUseCase = get(),
            managedRoots = get(),
            tracks = get(),
            folders = get(),
            artists = get(),
            albums = get(),
            scope = get(),
        )
    }

    // Theme state holder reads/writes ThemePreferences and exposes a hot flow.
    single {
        ThemeStateHolder(
            settings = get(),
            scope = get(),
        )
    }

    // Artwork preferences (download from MusicBrainz / embed back into files).
    single {
        ArtworkStateHolder(
            settings = get(),
            scope = get(),
        )
    }

    // Playback-engine preferences (crossfade toggle + duration). Observed by
    // the UI and by the Android CrossfadeController in the playback service.
    single {
        PlaybackTuningStateHolder(
            settings = get(),
            scope = get(),
        )
    }

    // Album-art reactive theme: watches the player, extracts a seed color from
    // the current cover, and persists it so the app opens tinted to the last
    // song. `extractor` is platform-provided (androidModule binds the Palette
    // impl). Single so exactly one observer runs for the app's lifetime.
    single {
        AlbumColorStateHolder(
            player = get(),
            extractor = get(),
            settings = get(),
            scope = get(),
        )
    }

    // Burn-metadata coordinator: runs the unified pass and exposes hot state.
    single {
        BurnMetadataCoordinator(
            useCase = get(),
            scope = get(),
        )
    }

    // App-wide navigation stack. Single instance so back-stack survives recompositions.
    single { AppNavigator() }

    // Library facade — hot artist list + drill-down lookups for the library pages.
    single {
        LibraryStateHolder(
            artists = get(),
            albums = get(),
            folders = get(),
            tracks = get(),
            recentAlbumsRepo = get(),
            scope = get(),
        )
    }

    // Playlist CRUD facade.
    single {
        PlaylistStateHolder(
            playlists = get(),
            scope = get(),
        )
    }

    // Lyrics state — resolves lyrics for the current track (embedded → sidecar
    // → remote) and exposes the active line for the Lyrics page and the
    // tap-to-flip view on Now Playing. Single so one resolver runs app-wide.
    single {
        LyricsStateHolder(
            musicPlayer = get(),
            repository = get(),
            scope = get(),
        )
    }

    // Equalizer state — observed by the UI and by the platform audio-effect
    // driver. Lives in shared so future iOS/Desktop targets can reuse it.
    // Takes the genre use case so EQ Auto can opportunistically tag tracks
    // whose files lack a genre tag, lighting up the right preset on the
    // next playback frame without the user having to run the burn pass.
    single {
        EqualizerStateHolder(
            settings = get(),
            musicPlayer = get(),
            tracks = get(),
            burnMetadata = get(),
            scope = get(),
        )
    }
}

fun initKoin(
    appDeclaration: KoinAppDeclaration = {},
    platformModules: List<Module> = emptyList(),
) {
    startKoin {
        appDeclaration()
        modules(sharedModule)
        modules(platformModules)
    }
}
