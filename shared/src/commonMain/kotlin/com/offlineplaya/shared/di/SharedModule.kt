package com.offlineplaya.shared.di

import com.offlineplaya.shared.data.database.createDatabase
import com.offlineplaya.shared.data.repository.SqlAlbumRepository
import com.offlineplaya.shared.data.repository.SqlArtistRepository
import com.offlineplaya.shared.data.repository.SqlFolderRepository
import com.offlineplaya.shared.data.repository.SqlManagedTreeRootRepository
import com.offlineplaya.shared.data.repository.SqlPlaylistRepository
import com.offlineplaya.shared.data.repository.SqlQueueRepository
import com.offlineplaya.shared.data.repository.SqlSettingsRepository
import com.offlineplaya.shared.data.repository.SqlTrackRepository
import com.offlineplaya.shared.domain.repository.AlbumRepository
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.domain.repository.FolderRepository
import com.offlineplaya.shared.domain.repository.ManagedTreeRootRepository
import com.offlineplaya.shared.domain.repository.PlaylistRepository
import com.offlineplaya.shared.domain.repository.QueueRepository
import com.offlineplaya.shared.domain.repository.SettingsRepository
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.domain.usecase.LibrarySyncUseCase
import com.offlineplaya.shared.presentation.library.LibraryStateHolder
import com.offlineplaya.shared.presentation.navigation.AppNavigator
import com.offlineplaya.shared.presentation.settings.ThemeStateHolder
import com.offlineplaya.shared.presentation.sync.LibrarySyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module

val sharedModule: Module = module {
    single { createDatabase(get()) }

    single { SqlTrackRepository(get()) } bind TrackRepository::class
    single { SqlArtistRepository(get()) } bind ArtistRepository::class
    single { SqlAlbumRepository(get()) } bind AlbumRepository::class
    single { SqlFolderRepository(get()) } bind FolderRepository::class
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

    // App-wide navigation stack. Single instance so back-stack survives recompositions.
    single { AppNavigator() }

    // Library facade — hot artist list + drill-down lookups for the library pages.
    single {
        LibraryStateHolder(
            artists = get(),
            albums = get(),
            tracks = get(),
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
