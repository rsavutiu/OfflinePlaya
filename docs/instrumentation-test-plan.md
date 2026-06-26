# Instrumentation / UI Test Plan — OfflinePlaya

Status: **active**. Owner: TBD. Last updated: 2026-06-26.

## Goal

Add UI-level test coverage on top of the existing 242 unit tests, with two
hard constraints:

1. **Real-device capable** — the end-to-end and platform-stack tests must run
   on physical hardware (SAF, MediaStore, ExoPlayer, Jaudiotagger).
2. **iOS-portability preserved** — the project is Compose Multiplatform with the
   structure to add iOS/desktop later (`No iOS / desktop targets implemented`
   today). The screen-level tests must not have to be rewritten if that day
   comes.

## Decision record

**Screen-level UI tests live in `shared/src/commonTest`, written against the
Compose Multiplatform `runComposeUiTest` API — but gated behind a Phase-0
spike, with `androidApp/src/androidTest` as a zero-cost fallback.**

Why this resolves the "between Android-instrumented and commonTest" tension:

- Every page and `App()` is **param-wired** (state holders + callbacks as
  parameters; no `koinInject` inside any composable; composables have no Android
  imports). A screen test is therefore just "construct the page with fake state,
  tap a tagged node, assert a tagged node" — nothing in that body is Android.
- The shared module currently has **only `androidTarget()`**, so `commonTest`
  executes on Android today regardless. Putting the tests in `commonTest` costs
  ~the same as `androidTest` now, and they automatically gain iOS/desktop run
  targets *if* those are ever added — which is a separate, larger project
  (every `expect` needs an iOS `actual`).
- Because the test **bodies are identical** between the two homes (only the
  harness entry point differs: `runComposeUiTest` vs `createAndroidComposeRule`),
  if the experimental Compose-MP test tooling fights on our Compose version, the
  same robots/assertions drop into `androidApp/androidTest` with no logic lost.

Caveats accepted:

- `runComposeUiTest` is `@ExperimentalTestApi`. The Phase-0 spike exists to flush
  out tooling pain before we write the bulk.
- "Real device" for the *screen* layer = whatever the Android target runs it as
  (Robolectric/JVM unit test, or instrumented on device). The spike confirms
  which; instrumented can be forced if hardware execution is wanted.

## Portability matrix

| Layer | Portable to iOS? | Home |
|---|---|---|
| Unit tests (repos, use cases, state holders) | ✅ logic is platform-agnostic | `commonTest` (long-term) / `androidUnitTest` (today) |
| **Screen-behavior tests** (param-wired pages + fakes + testTags) | ✅ if framework-neutral | `shared/src/commonTest` (spike-gated) |
| Full E2E (launch host, real service, SAF, MediaStore, ExoPlayer) | ❌ host is per-platform | `androidApp/src/androidTest` |
| Platform-stack (Jaudiotagger writers, scanners) | ❌ `androidMain` actuals | `androidApp/src/androidTest` |

The bottom two rows are irreducibly Android — `expect`/`actual` guarantees iOS
needs its own host + platform tests written fresh. No test strategy changes that.

## Conventions

- **`testTag`, never text matchers.** 42 locales *and* strings are Compose-MP
  `Res.string.*` resources (awkward to resolve in-test). Tag every node we
  assert on. Give **each page root a stable identity tag** (`PageTags.<page>`),
  which makes `AppNavigator` push/pop assertions trivial (tap → assert new root;
  back → assert prior root). Pin a fixed test locale.
- **Robots / page objects** against the shared matcher surface (`onNodeWithTag`,
  `assertIsDisplayed`, `performClick`, `waitUntil`). The harness is a thin
  swappable shell so a commonTest→androidTest move (or the reverse) is mechanical.
- **Idle on semantics, never sleep.** Compose auto-sync does NOT idle ExoPlayer,
  Coil, or `Dispatchers.IO`. Use `waitUntil { onAllNodesWithTag(...).fetch... }`.
  `Thread.sleep` is banned — it is the #1 flakiness source here.
- **Fakes are the determinism layer.** Screen tests construct pages with fake
  state holders / fake `MusicPlayer` / fake repositories. No real DB, SAF, or
  network in the screen layer.
- **Never drive SAF.** The picker is the system Documents UI in another process;
  Espresso can't touch it and UIAutomator is device-flaky. Seed the library
  directly; the overlap/dedup logic is already unit-tested.
- **Permissions:** `GrantPermissionRule.grant(READ_MEDIA_AUDIO)` (+ the ≤API32
  `READ_EXTERNAL_STORAGE` split) for anything that touches the real app.

## Phases

### Phase 0 — de-risk the infrastructure (the gate)

Nothing else is written until a smoke test is green.

1. Add the Compose-MP UI-test dependency to `commonTest` (version catalog +
   `shared/build.gradle.kts`).
2. **Spike:** one trivial `runComposeUiTest` test in `commonTest` rendering a
   tagged composable and asserting it. Compile, then run.
   - Clean → screen layer stays in `commonTest`.
   - Fights tooling → move the same robots to `androidApp/androidTest`.
3. **Relocate** the reusable fakes (`FakeFolderScanner`, `FakeMetadataReader`,
   `FakeDeviceAudioScanner`) and the in-memory DB helper so both unit and
   screen tests reach them. The DB helper may need an `expect`/`actual` if its
   driver is Android-specific.
4. Add `GrantPermissionRule` + a fixed-locale rule for the Android-host tests.
5. **Success criterion:** one screen smoke test green (on device or Robolectric,
   per spike outcome).

### Phase 1 — isolated-screen tests (the stable bulk)

Construct each page with fakes; add `testTag`s as you go. High-value pages only:

- **HomePage** — stat strip, recently-played shelf, browse grid; row tap routes.
- **LibraryFlatPage / LibraryArtistsPage** — seeded list renders; row tap.
- **SearchPage** — type → results; empty-state for short / no-match queries.
- **NowPlayingPage** — title/artist/seek/transport reflect fake player state.
- **PlaylistsPage** — five smart playlists + user playlists render.
- **SettingsPage** — key toggles flip (EQ mode, album-art color, crossfade).
- **LyricsPage** — Loading / None / Plain / Synced states.

### Phase 2 — full E2E happy paths (Android-only, 2–3 max)

`createAndroidComposeRule<MainActivity>()` + a Koin **override module** (in-memory
SQLDelight driver + fake scanners + `silence-1s.*` fixtures). The override is
installed by a custom `AndroidJUnitRunner` whose test `Application` calls
`initKoin(platformModules = … + overrides)`. Assert **UI state transitions**,
not audio:

1. Launch → seeded library visible → tap track → Now Playing appears →
   pause/skip reflected.
2. Library → add track to queue → QueuePage shows it.
3. Search → result → play → Now Playing.

### Phase 3 — regression guards for the 2026-06-26 review fixes (UI-observable)

- **Search resilience** (`8028a29`): throwing `TrackRepository` fake → search
  degrades to empty, UI stays usable (no crash/hang).
- **Lyrics resilience** (`047387d`): throwing `LyricsRepository` → `None`, next
  track still resolves.
- *Splash:* hard to assert on-device → **manual verification, not automated.**

## Out of scope

- Automating SAF or the splash window.
- Targeting all 19 pages — high-value screens + the 2–3 E2E paths only. A
  19-page matrix is the failure mode.
- Adding a JVM/desktop or iOS target to the shared module (separate project;
  every `expect` needs new actuals).

## Run commands

```bash
# Screen layer (per spike outcome — one of these):
./gradlew :shared:testDebugUnitTest                       # if Robolectric/JVM
./gradlew :shared:connectedDebugAndroidTest               # if instrumented

# Android-host E2E + platform stack (needs a device):
./gradlew :androidApp:connectedDebugAndroidTest
./gradlew :androidApp:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.offlineplaya.android.<Class>
```

## Prerequisites / risks

- **No device attached today** (`adb devices` empty). Instrumented execution
  blocks until one is connected. Compilation and plan work do not.
- **Koin override** must be confirmed reachable for Phase 2 (`initKoin` takes
  `platformModules`; verify override semantics — may need `loadKoinModules` or
  `allowOverride`).
- **Compose-MP test tooling** maturity is the Phase-0 unknown.

## Rough effort

Phase 0 ≈ 0.5–1 day (real risk: spike + fixture relocation). Phase 1 ≈ 1–2 days.
Phase 2 ≈ 0.5 day. Phase 3 ≈ 0.5 day.
