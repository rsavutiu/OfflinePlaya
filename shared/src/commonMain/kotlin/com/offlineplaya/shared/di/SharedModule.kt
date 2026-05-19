package com.offlineplaya.shared.di

import com.offlineplaya.shared.data.database.createDatabase
import com.offlineplaya.shared.data.repository.SqlAlbumRepository
import com.offlineplaya.shared.data.repository.SqlArtistRepository
import com.offlineplaya.shared.data.repository.SqlFolderRepository
import com.offlineplaya.shared.data.repository.SqlManagedTreeRootRepository
import com.offlineplaya.shared.data.repository.SqlPlaylistRepository
import com.offlineplaya.shared.data.repository.SqlQueueRepository
import com.offlineplaya.shared.data.repository.SqlTrackRepository
import com.offlineplaya.shared.domain.repository.AlbumRepository
import com.offlineplaya.shared.domain.repository.ArtistRepository
import com.offlineplaya.shared.domain.repository.FolderRepository
import com.offlineplaya.shared.domain.repository.ManagedTreeRootRepository
import com.offlineplaya.shared.domain.repository.PlaylistRepository
import com.offlineplaya.shared.domain.repository.QueueRepository
import com.offlineplaya.shared.domain.repository.TrackRepository
import com.offlineplaya.shared.domain.usecase.LibrarySyncUseCase
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
