# OfflinePlaya — Project Notes for Claude Sessions

An Android offline music player. Kotlin Multiplatform skeleton today, Android-only at runtime —
`shared/commonMain` is structured so iOS/desktop targets can slot in, but only `androidApp` ships.

## Build & run

- **Compile shared**: `./gradlew :shared:compileDebugKotlinAndroid`
- **Compile app**: `./gradlew :androidApp:compileDebugKotlin`
- **Run unit tests**: `./gradlew :shared:testDebugUnitTest` (70 tests as of last edit)
- **Full check**: `./gradlew :shared:testDebugUnitTest :androidApp:compileDebugKotlin`

PowerShell is the default shell on this machine — use PowerShell syntax when invoking gradle from
the harness, not bash chaining (`&&` doesn't work in 5.1).

## Project layout

```
androidApp/                 → thin Android host (Activity, Application, PlaybackService)
shared/                     → all real code, KMP module
├── commonMain/             → domain + presentation, platform-agnostic
│   ├── data/repository     → SqlXxxRepository (SQLDelight)
│   ├── data/mapper         → row ↔ domain converters
│   ├── domain/             → models, repository interfaces, use cases, scanner contracts
│   ├── presentation/       → state holders, navigation, sync coordinator, UI (Compose)
│   └── sqldelight/         → .sq schema files + .sqm migrations
├── androidMain/            → SAF scanner, MediaStore scanner, Coil setup, Media3, Jaudiotagger
├── commonTest/             → TestLogger (no expect/actual — just a class)
└── androidUnitTest/        → repo + use case + coordinator tests against in-memory SQLite
```

## Architecture

**Clean Architecture + Atomic Design** — these are explicit conventions, not aspirations:

- `domain/` has no Android imports. Models, repository *interfaces*, use cases.
- `data/` implements the repository interfaces with SQLDelight; mapping happens in `data/mapper/`.
- `presentation/` has state holders (e.g. `LibraryStateHolder`, `PlaylistStateHolder`,
  `LibrarySyncCoordinator`) that expose `StateFlow`s; Compose pages observe them.
- UI in `presentation/ui/` is split atoms → molecules → organisms → templates → pages.
- **Every composable ships a working `@PreviewScreenSizes`.** This is enforced by convention, not
  tooling — when you add or edit a composable, add/update its preview. `Preview` is an `expect`
  typealias mapped to Compose's preview annotation on Android.

## Navigation

**No Jetpack Navigation. No Navigation 3. No Scenes / adaptive layouts.** Navigation is a homegrown
`AppNavigator` (`presentation/navigation/AppNavigator.kt`) that holds
`StateFlow<List<AppDestination>>` with `push`/`pop`/`replace`. `MainActivity` wires `BackHandler`
into `navigator.pop()`. The app is phone-only single-pane today; if multi-pane lands later, that's
where adaptive layouts would slot in.

**Scope of the "no two-pane" rule:** it bans *navigation-level* two-pane — list-detail where two
**destinations** co-display, coordinated by a nav framework (Nav3 / Scenes). That's the complexity
we're avoiding. It does **not** ban a single page laying its own content into multiple columns in
landscape. A page is free to branch on `LocalOrientation` and render a `Row { leftColumn;
rightColumn }` for better use of horizontal space (see `HomePage.LandscapeHome` — left: header /
stats / recently-played; right: the Browse grid at full height). That's responsive content within
one destination, not a navigation Scene.

## Tech stack

- **Compose Multiplatform** for UI (commonMain)
- **Media3 / ExoPlayer** for playback — bound via `MediaController` ↔ `MediaSessionService` (
  `androidApp/service/PlaybackService.kt`)
- **Coil 3** for image loading
- **SQLDelight** for persistence; driver factory is `expect`/`actual`
- **Koin** for DI (`shared/di/SharedModule.kt`, `shared/di/AndroidModule.kt`,
  `androidApp/di/AppPlayerModule.kt`)
- **OkHttp** for HTTP (MusicBrainz, Cover Art Archive, artist images)
- **Jaudiotagger** for writing embedded album art into audio tags
- **WorkManager** for background tasks (`BackgroundTaskRunner` is the domain interface)
- **Kermit + AppLogger** — domain code takes `AppLogger`, Android impl is Kermit-backed, tests use
  `TestLogger`

## Critical flows

### Library sync — `LibrarySyncUseCase`

1. SAF folder walk via `SafFolderScanner` (uses bulk `DocumentsContract` cursors, NOT
   `DocumentFile.listFiles` — much faster).
2. Folders upserted parent-before-child.
3. Pending track rows inserted.
4. **Metadata reads are parallelized** with `Semaphore(8)` — IO-bound and the original bottleneck.
   DB writes after that stay sequential because artist/album upserts have read-then-insert race
   semantics.
5. Aggregate counts refreshed on Artist / Album / Folder.
6. `syncDeviceAudio()` then folds in MediaStore-indexed tracks under a synthetic `device://audio`
   tree — picks up music in `Downloads/` etc. without forcing the user to grant tree access.

### Managed-root dedup

`LibrarySyncCoordinator.findOverlappingRoot` rejects exact dupes AND subfolder/parent overlaps
within the same SAF authority. It parses tree URIs into `(authority, decoded-doc-id-path)` and does
prefix checks on the decoded path with `/` as the boundary. Cross-authority overlap is undecidable
from URI alone — flagged in comments, not implemented.

### Album-art chain (`TrackArtFetcher`)

Order:

1. **On-disk cache** — `cacheDir/track_art/<album-key>` (where key matches `TrackKeyer`). Survives
   process death.
2. **Embedded** — `MediaMetadataRetriever.embeddedPicture`.
3. **Sidecar** — `SafFolderArtSource` looks for `cover.jpg|png|webp` / `folder.*` / `album.*` /
   `front.*` / `albumart.*` (case-insensitive, priority order) via a single `DocumentsContract`
   child cursor against the track's parent folder.
4. **Remote** — `RemoteArtSource` (MusicBrainz + Cover Art Archive + Deezer fallback).

Bytes from any successful step get persisted to the on-disk cache so subsequent loads (and other
tracks on the same album) skip all of the above.

Coil's own `diskCache` is ALSO configured (50 MB at `cacheDir/coil_cache`) but custom fetchers
returning `ImageFetchResult` (decoded `Image`) bypass it — that's why we maintain our own file
cache. If you ever migrate to `SourceFetchResult` Coil's cache would take over, but the current
setup is fine.

OkHttp client used by `ArtistArtFetcher` and `MusicBrainzArtSource` has a 50 MB HTTP cache at
`cacheDir/http_cache` — covers artist images and CAA covers across restarts.

### Playback — `PlaybackService`

- `MediaSessionService` hosting one `ExoPlayer` + one `MediaSession`.
- **System audio-effects broadcast** is wired:
  `AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION` in `onCreate`, matching close in
  `onDestroy`. Without this OEM EQ / Dolby / Spatial Audio services skip our stream and the audio
  comes out flat.
- `AudioAttributes(CONTENT_TYPE_MUSIC, USAGE_MEDIA)` + `setHandleAudioBecomingNoisy(true)` + auto
  audio-focus.
- `stopSelf()` on task-removed when nothing is playing — otherwise the foreground notification can
  linger.

### Startup

`OfflinePlayaApp.onCreate` does:

1. `initKoin(...)` — registers modules. Singletons are lazy.
2. Sends everything else (`installTrackArtImageLoader`, `LibrarySyncCoordinator.resyncAll()`) onto
   the Koin-provided `CoroutineScope` (`Dispatchers.Default`). DB instantiation thus happens off the
   main thread.

If `MainActivity` wins the race for the DB singleton it'll fall back to opening on the main thread —
not currently an observed issue because the background coroutine starts before the first Compose
frame.

## Database & migrations

- Schema: `shared/src/commonMain/sqldelight/com/offlineplaya/shared/database/*.sq`
- Migrations: `*.sqm` files in the same dir. `2.sqm` exists (untracked at last check). When you
  change schema, **add a numbered migration** rather than editing existing `.sq` definitions or
  production DBs need wipes.
- Indexes worth knowing: `Folder(tree_uri, relative_path)` UNIQUE, `Track.document_uri` UNIQUE,
  `Album(name COLLATE NOCASE, artist_id)` UNIQUE, `Artist.name` UNIQUE COLLATE NOCASE.

## Track-count perf

`LibraryStateHolder.totalTrackCount` uses `tracks.observeCount()` → `SELECT COUNT(*)`, NOT
`tracks.observeAll().map { it.size }`. Cold-start cost is constant; if you ever rewrite this be
aware materializing every track row just to count them was a real bottleneck.

## Theme / design tokens

`presentation/ui/theme/` has the full Material 3 token set — Colors, Typography, Shapes, Spacing,
Elevation, Motion. Use `AppSpacing.sm/md/lg/xl` etc. rather than hard-coded `dp` values where you
have a choice. There's a `DesignSystemGalleryPage` you can navigate to from Settings.

Single accent (violet `#7C5CBF`) throughout — if you find another accent color in code, that's a
regression from the redesign pass.

## Time / clock

`commonMain` has no datetime library bundled. `currentHourOfDay()` is an `expect`/`actual` with the
Android impl using `java.time.LocalTime`. If you need more datetime APIs, decide whether to add
`kotlinx-datetime` (clean but a real dep) or extend the expect surface.

## Testing conventions

- All unit tests live in `shared/src/androidUnitTest/` (the JVM-backed test set), even ones that
  *could* be in commonTest. There's a `commonTest/util/Logger.test.kt` exposing an
  `internal class TestLogger : AppLogger` — pass it to any repo or use case that wants a logger.
- In-memory database via `createInMemoryDatabase()` from `testsupport/`.
- `FakeFolderScanner`, `FakeMetadataReader`, `FakeDeviceAudioScanner`, `FakeFolderScanner` are the
  fakes in `testsupport/`.
- Constructors that take a logger: `SqlArtistRepository`, `SqlAlbumRepository`,
  `SqlFolderRepository`, `SqlTrackRepository`, `SafFolderScanner`, `LibrarySyncUseCase`,
  `EmbedMissingArtUseCase`. If you add another, the test fixtures need updating.

## Things that have bitten us before

- **Sequential metadata reads** during scan were the user-visible "I can watch it sync one track at
  a time" bug. Now parallelized — keep it that way.
- **No on-disk image cache** because `TrackArtFetcher` returned `ImageFetchResult` (Coil disk cache
  only persists `SourceFetchResult`). We route around that with our own file cache.
- **Same folder picked twice via SAF** silently creating duplicate managed roots — solved by
  `findOverlappingRoot` in the coordinator. The check is on the **decoded path** scoped to the *
  *authority**, not raw string comparison.
- **`tracks.observeAll().map { it.size }`** for the home page count loaded every row on every
  change. Replaced with `observeCount()`. Don't reintroduce.
- **`Application.onCreate` doing `koin.get<...>` synchronously** forces DB init on the main thread.
  Keep all DB-touching `koin.get` calls inside `appScope.launch { ... }`.
- **Tests that don't pass a logger** fail to compile after a repo gets an `AppLogger` constructor
  param. There's no expect/actual for the test logger anymore — just instantiate `TestLogger()`.

## What this project does NOT do

- No Nav3 / Scenes / list-detail / two-pane *navigation* (a single page may still split into
  multiple columns in landscape — see the Navigation section).
- No remote streaming — strictly offline, files the user already has.
- No accounts, no cloud sync.
- No ReplayGain / LUFS normalization.
- No equalizer UI — the user gets whatever the OEM system audio-effects service provides (now that
  the broadcast is wired).
- No iOS / desktop targets implemented even though the structure supports them.

## When the user asks for "design feedback"

There's a redesign HTML mockup that lives on the user's desktop (`offlineplaya_redesign.html`). It's
the source of truth for visual direction — single violet accent, atomic design, large browse cards,
recently-played shelf on home, compact gradient album header, sidebar/playing-state highlighting on
the track list.
