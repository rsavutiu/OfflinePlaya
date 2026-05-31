# Android Auto — minimum-scope plan, separate app variant

**Goal.** Ship OfflinePlaya on Android Auto with a working browse tree, search, and playback —
distributed as a **separate APK / app ID** so the main phone app stays untouched and the
Auto-flavored build can be reviewed / shipped on its own track.

This is the *minimum* version — no Android Automotive OS (AAOS) integration (that's the in-car
infotainment OS, different surface, different review), no per-row dynamic browse, no
offline-data-saver tweaks.

---

## Build flavor structure

Two-flavor setup in `androidApp/build.gradle.kts`:

```kotlin
android {
    flavorDimensions += "distribution"
    productFlavors {
        create("standard") {
            dimension = "distribution"
            applicationIdSuffix = ""
            versionNameSuffix = ""
        }
        create("auto") {
            dimension = "distribution"
            applicationIdSuffix = ".auto"
            versionNameSuffix = "-auto"
        }
    }
}
```

Resulting application IDs:

- `com.offlineplaya.android` — the existing phone app, unchanged user-facing.
- `com.offlineplaya.android.auto` — the Auto-targeted build.

**Why a separate flavor and not a single app**: the Auto manifest needs `MediaLibraryService` (not
`MediaSessionService`), a `com.google.android.gms.car.application` metadata block, an
`automotive_app_desc.xml`, a different launcher icon path, and different content-provider
authorities. Keeping them as flavors means the phone APK has zero Auto-specific bloat and we can
ship them on independent release cycles.

**Shared code**: all `shared/` module code, all `androidApp/src/main/` non-service code. Auto-only
code lives under `androidApp/src/auto/` (Gradle source set, automatic on `auto` flavor). Phone-only
service code lives under `androidApp/src/standard/`.

Layout:

```
androidApp/src/
  main/                          shared between flavors (UI, MainActivity, Application, DI, etc.)
  standard/
    AndroidManifest.xml          (phone-only manifest entries — currently the only manifest)
    java/com/offlineplaya/android/service/PlaybackService.kt   ← MediaSessionService (today's code)
  auto/
    AndroidManifest.xml          adds MediaLibraryService + automotive metadata
    res/xml/automotive_app_desc.xml
    java/com/offlineplaya/android/service/PlaybackService.kt   ← MediaLibraryService
    java/com/offlineplaya/android/auto/                        (new package)
      AutoLibraryCallback.kt
      BrowseTreeBuilder.kt
      MediaIdRouter.kt
      ArtworkContentProvider.kt
```

The two `PlaybackService.kt` files share ~90% of their body — extract the common ExoPlayer +
audio-effects setup into a helper in `main/` (`PlaybackEngine` class), then both services delegate
to it.

---

## The browse tree

```
root
├── recents          → up to 20 recently-played albums (existing LibraryStateHolder.allAlbums.take())
├── albums           → all albums, first 100, sorted by name
├── artists          → all artists → on click: artist's albums → on click: tracks
├── playlists        → user playlists → tracks
└── all_tracks       → flat list, capped at 100 for v1
```

**Folders are deliberately omitted** for v1 — folder hierarchy doesn't map well to Auto's two-level
browse pattern and would need pagination work we're skipping.

**Pagination**: capped at 100 per node for v1, with the last row being a non-playable "Show more in
app" item if the underlying list exceeds 100. Real pagination via `LibraryParams.extras` is v2.

---

## Media ID scheme

Structured strings parseable by `MediaIdRouter`:

| ID              | Meaning                                                    |
|-----------------|------------------------------------------------------------|
| `root`          | Library root                                               |
| `recents`       | Browse: recently played                                    |
| `albums`        | Browse: all albums                                         |
| `artists`       | Browse: all artists                                        |
| `playlists`     | Browse: all playlists                                      |
| `all_tracks`    | Browse: all tracks                                         |
| `album/{id}`    | Browse: tracks of album / Playable: play whole album       |
| `artist/{id}`   | Browse: artist's albums                                    |
| `playlist/{id}` | Browse: tracks of playlist / Playable: play whole playlist |
| `track/{id}`    | Playable: play this track in album-context queue           |

Routing happens in pure-function `MediaIdRouter.parse(id): MediaRoute` returning a sealed type.
Unit-testable without any Auto dependencies.

---

## `MediaLibrarySession.Callback`

Implemented in `AutoLibraryCallback`:

- `onGetLibraryRoot` → returns a `MediaItem` with `mediaId = "root"`, `isBrowsable = true`,
  content-style hints set.
- `onGetChildren(parentId, ...)` → `BrowseTreeBuilder.childrenOf(parentId)` calling existing
  `LibraryStateHolder` / `PlaylistStateHolder` flows synchronously (`.first()` on flows is OK here —
  callback is already on a background thread).
- `onGetItem(mediaId)` → `BrowseTreeBuilder.itemOf(mediaId)`.
- `onSearch(query)` / `onGetSearchResult(query)` → call existing `LibraryStateHolder.search(query)`,
  map results to `MediaItem`s grouped by type (tracks first, then albums, then artists).
- `onSetMediaItems(controller, mediaItems, startIndex, startPositionMs)` → translate first item's
  mediaId via `MediaIdRouter` → resolve to a queue via `LibraryStateHolder` →
  `musicPlayer.setQueue(tracks, index)`.

Pseudocode for `onGetChildren("album/123")`:

```kotlin
val tracks = library.tracksByAlbum(123L).first()
return tracks.map { track ->
    MediaItem.Builder()
        .setMediaId("track/${track.id}")
        .setMediaMetadata(track.toAutoMetadata())  // title, artist, artwork URI
        .build()
}
```

---

## Artwork delivery

Auto reads artwork via `MediaMetadata.artworkUri` — a `content://` URI it can fetch in its own
process.

**Approach**: a new `ArtworkContentProvider` under the `auto` source set, exposed in the auto
manifest with authority `com.offlineplaya.android.auto.artwork`. URIs of the form
`content://com.offlineplaya.android.auto.artwork/track/{id}` resolve to the cached file under
`cacheDir/track_art/<album-key>` (existing on-disk cache from `TrackArtFetcher`).

If the album-key file doesn't exist yet, the provider triggers a synchronous `TrackArtFetcher`
fetch (which has its 4-step chain: disk → embedded → sidecar → remote). First load is slow;
subsequent loads are instant. Worth it because Auto needs *something* — without artwork the browse
rows show generic icons.

`FileProvider` would be simpler than a custom provider, but `FileProvider` requires the file to
exist at query time — our chain might need to fetch first. Custom provider is the right call.

`grantUriPermissions=true` so Auto's process can read the URIs.

---

## Manifest changes (auto source set only)

`androidApp/src/auto/AndroidManifest.xml`:

```xml

<manifest ...><application>
<meta-data android:name="com.google.android.gms.car.application"
    android:resource="@xml/automotive_app_desc" />

<service android:name=".service.PlaybackService" android:exported="true"
    android:foregroundServiceType="mediaPlayback">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaLibraryService" />
        <action android:name="android.media.browse.MediaBrowserService" />
        <action android:name="android.intent.action.MEDIA_BUTTON" />
    </intent-filter>
</service>

<provider android:name=".auto.ArtworkContentProvider"
    android:authorities="com.offlineplaya.android.auto.artwork" android:exported="true"
    android:grantUriPermissions="true" />
</application></manifest>
```

`res/xml/automotive_app_desc.xml`:

```xml

<automotiveApp>
    <uses name="media" />
</automotiveApp>
```

Manifest merger picks up these in the auto flavor; the standard flavor's manifest stays exactly as
it is today (`MediaSessionService`, no automotive metadata).

---

## Voice search

Auto sends search via `onSearch` when the user says "Play X in OfflinePlaya". The query string is
whatever the assistant heard.

`AutoLibraryCallback.onSearch` → `LibraryStateHolder.search(query).first()` → return as a flat list
of `MediaItem`s with tracks first.

Auto then either:

- shows a results screen (user picks a row), or
- immediately calls `onSetMediaItems` with the first result if the assistant is confident.

Both flow through our existing playback path. Nothing special needed.

---

## Content-style hints

In `LibraryParams` returned from `onGetLibraryRoot`:

```kotlin
val extras = Bundle().apply {
    putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 2)  // GRID
    putInt("android.media.browse.CONTENT_STYLE_PLAYABLE_HINT", 1)   // LIST
}
```

Browsable nodes (Albums, Artists) render as a grid with cover art. Playable nodes (Tracks) render as
a list. Without these, everything renders as an unstyled list and looks broken.

---

## Files touched / created

**Created** (auto flavor only):

- `androidApp/src/auto/AndroidManifest.xml`
- `androidApp/src/auto/res/xml/automotive_app_desc.xml`
- `androidApp/src/auto/java/com/offlineplaya/android/service/PlaybackService.kt` (
  MediaLibraryService variant)
- `androidApp/src/auto/java/com/offlineplaya/android/auto/AutoLibraryCallback.kt`
- `androidApp/src/auto/java/com/offlineplaya/android/auto/BrowseTreeBuilder.kt`
- `androidApp/src/auto/java/com/offlineplaya/android/auto/MediaIdRouter.kt`
- `androidApp/src/auto/java/com/offlineplaya/android/auto/ArtworkContentProvider.kt`

**Created** (shared between flavors):

- `androidApp/src/main/java/com/offlineplaya/android/service/PlaybackEngine.kt` — extracted common
  ExoPlayer / audio-effects setup
- `androidApp/src/auto/res/values/strings.xml`, `mipmap/` — Auto-specific app name + icon if you
  want them distinct

**Created** (standard flavor only):

- `androidApp/src/standard/AndroidManifest.xml` — move the current phone-only manifest entries here
- `androidApp/src/standard/java/com/offlineplaya/android/service/PlaybackService.kt` — current
  MediaSessionService, refactored to use `PlaybackEngine`

**Modified**:

- `androidApp/build.gradle.kts` — flavors block
- `androidApp/src/main/AndroidManifest.xml` — strip the `<service>` tag (now per-flavor), keep
  everything else shared

---

## Tests

- `MediaIdRouterTest` — table-driven, every media ID shape parses round-trip.
- `BrowseTreeBuilderTest` — given a fake `LibraryStateHolder`, returns expected `MediaItem` lists
  per parent ID; respects the 100-item cap; emits "Show more in app" sentinel when truncated.
- No tests for `AutoLibraryCallback` — it's a thin glue layer over `Builder` and `LibraryParams`,
  hard to test, low value.
- No tests for `ArtworkContentProvider` — needs `Context`, validated via DHU.

---

## Manual validation — DHU required

1. Install Desktop Head Unit from Android Studio SDK Manager → Extras → Android Auto Desktop Head
   Unit.
2. Enable developer mode on Auto: in the Android Auto app on phone, tap version 10x.
3. Run DHU on desktop: `$ANDROID_HOME/extras/google/auto/desktop-head-unit`.
4. Pair phone via USB.

Validation checklist:

- [ ] App appears in Auto's app drawer with the launcher icon.
- [ ] Tapping the app shows the root browse with Recents / Albums / Artists / Playlists / All
  Tracks.
- [ ] Tapping Albums shows a grid with cover art (NOT generic icons — if generic,
  ArtworkContentProvider isn't wiring correctly).
- [ ] Tapping an album → tracks list → tapping a track starts playback.
- [ ] Playback controls (play/pause/skip) work from the head unit.
- [ ] Voice query "play [artist name]" returns results and starts playback.
- [ ] Phone app still works normally during Auto playback.
- [ ] Disconnecting USB → playback continues on phone.
- [ ] Reconnecting USB → Auto re-attaches to existing session.

---

## Distribution

**Play Store**: the auto-flavor APK can be uploaded as a separate app listing (`.auto` package ID
makes it a different "app" to the Store) OR as an alternate APK in the same listing via APK splits.
Either way, it goes through **Android Auto app review** which Google does manually. Plan ~1–2 week
review window first time.

For sideload / dev-only distribution: just install the `auto`-flavored APK, no review needed.

**Recommended**: ship `.auto` as a separate Play Store listing called "OfflinePlaya for Auto". Users
searching for Auto-compatible apps find it via the Auto-compatibility filter. The phone app stays
clean.

---

## Open questions before starting

1. **Confirm the package ID structure** — `com.offlineplaya.android.auto`, or
   `com.offlineplaya.auto`? Affects branding and Play Store listings.
2. **Distinct launcher icon for the Auto build?** Or reuse the phone icon? Reuse is fine for v1.
3. **Should the Auto flavor still include the full phone UI** (in case the user opens the app on
   phone) or be Auto-only with a minimal "open the main app" screen on phone? Recommend keeping the
   full UI — simpler, and users will inevitably open the icon to check.
4. **Is there budget for the Google review window?** Don't start a 1-week Auto review the day before
   a trip.

---

## Rollout

1. Land the flavor restructure as PR 1 — zero behavioral change, just code reorganization. Validates
   the split is clean.
2. Land the auto-flavor implementation as PR 2 — strictly additive to the `auto` source set.
3. Internal testing track on Play Store for the auto APK before public release.
4. Public release with screenshots from DHU.

Depends on the Equalizer feature only loosely: if shipping EQ first, the auto flavor inherits EQ for
free since both share `androidApp/src/main/` for the `PlaybackEngine`. No coordination needed beyond
not landing both in the same week.
