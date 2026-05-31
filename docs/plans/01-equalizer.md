# Equalizer — minimum-scope plan

**Goal.** Ship an app-owned 5-band equalizer with three modes: **Off**, **Manual** (user picks a
preset / tweaks sliders), **Auto** (preset picked from the current track's normalized genre).
One-tap access from NowPlaying.

This is the *minimum* version — no on-device audio ML classifier, no per-output-device profiles, no
custom user preset library. Just: tag → canonical genre → preset, with sliders for override.

---

## Scope summary

| Area             | In                                                            | Out (v2 / later)                         |
|------------------|---------------------------------------------------------------|------------------------------------------|
| Effect chain     | `android.media.audiofx.Equalizer` only                        | BassBoost, Virtualizer, LoudnessEnhancer |
| Bands            | Whatever `Equalizer.getNumberOfBands()` reports (typically 5) | Forcing N bands across devices           |
| Genre source     | Tag string → normalize                                        | Audio-content ML classifier              |
| Presets          | 7 built-in (one per canonical genre + Default)                | User-created presets                     |
| Output awareness | Same EQ regardless of headphones / speaker / BT               | Per-output profiles                      |
| System EQ        | Mutually exclusive — toggle in Settings                       | Stacking app + OEM EQ                    |

---

## Architecture

### Domain layer (`shared/commonMain`)

```
domain/
  model/
    EqMode.kt             enum { OFF, MANUAL, AUTO }
    EqBand.kt             data class (centerFrequencyHz: Int, minMillibels: Int, maxMillibels: Int, gainMillibels: Int)
    EqPreset.kt           data class (name: String, gains: List<Int>)  // gains in millibels, one per band
    CanonicalGenre.kt     enum { ROCK, POP, ELECTRONIC, JAZZ, CLASSICAL, HIPHOP, DEFAULT }
  repository/
    EqPreferencesRepository.kt
      observeState(): Flow<EqState>
      setMode(mode: EqMode)
      setManualPreset(name: String)
      setBandGain(bandIndex: Int, millibels: Int)
      setEnabled(enabled: Boolean)  // false = OFF, true = restore prior MANUAL/AUTO
  usecase/
    GenreClassifier.kt    fun classify(rawGenre: String?): CanonicalGenre  // pure
    PickPresetForTrack.kt fun pick(track: Track, mode: EqMode, manualName: String, builtins: List<EqPreset>): EqPreset
```

**`EqState`** = the projected view a UI / effect-driver consumes:

```kotlin
data class EqState(
    val mode: EqMode,
    val activePreset: EqPreset,  // resolved preset (Default if OFF)
    val manualPresetName: String,  // remembered when mode flips back to MANUAL
)
```

### Data layer

- **`shared/commonMain/sqldelight/.../Track.sq`**: new column `canonical_genre TEXT`. Migration
  `3.sqm`.
- **Scanner change**
  in [LibrarySyncUseCase.applyMetadataAndGrouping](shared/src/commonMain/kotlin/com/offlineplaya/shared/domain/usecase/LibrarySyncUseCase.kt):
  after metadata is read, call `GenreClassifier.classify(metadata.genre).name` and persist alongside
  the metadata update. Same for `syncDeviceAudio`.
- **Backfill**: lazy — on first sync after upgrade, any row with `canonical_genre IS NULL` gets its
  tag re-classified (no audio re-read required, just SQL update). One-shot WorkManager job at
  startup.
- **`EqPreferencesRepository`** backed by DataStore (whatever store is used today for
  `ThemePreferences` / `ArtworkPreferences` — keep symmetry).

### Effect driver (`androidApp`)

```
androidApp/src/main/java/com/offlineplaya/android/audio/
  AppEqualizerController.kt
    - holds android.media.audiofx.Equalizer bound to ExoPlayer.audioSessionId
    - collects EqState + currently-playing Track
    - applies preset via setBandLevel(band, millibels)
    - enables/disables broadcast handoff to system EQ
```

The controller is constructed in `PlaybackService.onCreate` after the ExoPlayer exists. It owns the
lifecycle: created with the session, released in `onDestroy`.

**Critical**: the existing `openAudioEffectSession` broadcast
in [PlaybackService.kt:55](androidApp/src/main/java/com/offlineplaya/android/service/PlaybackService.kt#L55)
is gated on `EqState.mode != OFF`. When our EQ is on, we do NOT broadcast; when it's off, we do. The
controller manages both sides so they stay mutually exclusive.

### Presentation

```
presentation/eq/
  EqualizerStateHolder.kt  observes prefs + current track → emits EqState

presentation/ui/pages/
  EqualizerPage.kt         full page: Off/Manual/Auto radio + preset chooser + 5 sliders
                           Auto mode shows "Auto: Rock (from tag)" or "Auto: Default (no genre tag)"

presentation/ui/molecules/
  EqQuickButton.kt         icon button shown in NowPlaying top bar — opens EqualizerPage
```

No bottom sheet for v1. Tapping the icon pushes `AppDestination.Equalizer`. Settings → Audio also
links to the same page.

---

## Built-in presets (millibels, 5-band)

Bands assumed at ~60 / 230 / 910 / 3600 / 14000 Hz. Actual centers come from
`Equalizer.getBandCenterFrequency(band)` — the table is just a template that gets remapped if a
device reports different centers.

| Genre          | 60Hz | 230Hz | 910Hz | 3.6kHz | 14kHz |
|----------------|------|-------|-------|--------|-------|
| Default (Flat) | 0    | 0     | 0     | 0      | 0     |
| Rock           | +300 | +200  | -100  | +200   | +400  |
| Pop            | +200 | +400  | +200  | +300   | +200  |
| Electronic     | +500 | +300  | 0     | +200   | +400  |
| Jazz           | +300 | +200  | -100  | +200   | +300  |
| Classical      | +400 | +300  | -200  | +300   | +400  |
| Hip-Hop        | +500 | +400  | +100  | -100   | +200  |

Clamped to per-band `min/max millibels` reported by the platform.

---

## Genre normalization rules (`GenreClassifier`)

Pure function. Lowercase input, strip whitespace and punctuation, then match against a keyword
table:

- `rock`, `alt`, `indie`, `punk`, `grunge`, `metal` (everything except "death/black/doom" which we
  still bucket as ROCK for v1) → **ROCK**
- `pop`, `dance pop`, `synth pop`, `k-pop`, `j-pop` → **POP**
- `electronic`, `edm`, `house`, `techno`, `trance`, `drum and bass`, `dubstep`, `idm`, `ambient`,
  `synthwave` → **ELECTRONIC**
- `jazz`, `bebop`, `swing`, `fusion`, `blues` (blues bucketed here, not as separate v1 category) → *
  *JAZZ**
- `classical`, `baroque`, `romantic`, `orchestra`, `opera`, `chamber` → **CLASSICAL**
- `hip hop`, `hip-hop`, `rap`, `trap`, `r&b`, `rnb`, `soul`, `funk` → **HIPHOP**
- Empty / `unknown` / `other` / numeric ID3 codes / no match → **DEFAULT**

ID3v1 numeric genre codes (`(17)` = Rock) get expanded via a small lookup table before matching.

Tested with a fixture of ~50 real-world tag strings.

---

## Migration `3.sqm`

```sql
ALTER TABLE Track ADD COLUMN canonical_genre TEXT;
CREATE INDEX idx_track_canonical_genre ON Track(canonical_genre);
```

No data backfill in the migration itself (would require running Kotlin classifier from SQL).
Backfill happens via the lazy sync pass described above.

---

## Files touched / created

**Created** (~10 files):

- `domain/model/EqMode.kt`, `EqBand.kt`, `EqPreset.kt`, `CanonicalGenre.kt`
- `domain/repository/EqPreferencesRepository.kt`
- `domain/usecase/GenreClassifier.kt`, `PickPresetForTrack.kt`
- `data/preferences/EqPreferencesStore.kt` (DataStore impl, in androidMain or commonMain depending
  on existing preference setup)
- `androidApp/audio/AppEqualizerController.kt`
- `presentation/eq/EqualizerStateHolder.kt`
- `presentation/ui/pages/EqualizerPage.kt`
- `presentation/ui/molecules/EqQuickButton.kt`
- `sqldelight/.../3.sqm`

**Modified**:

- `domain/usecase/LibrarySyncUseCase.kt` — classify + persist `canonical_genre` during scan
- `data/repository/SqlTrackRepository.kt` + `Track.sq` — column + setter
- `domain/repository/TrackRepository.kt` — new `setCanonicalGenre(id, value)` or extend
  `updateMetadata`
- `domain/scanner/AudioMetadata.kt` — no change (already has `genre`)
- `androidApp/service/PlaybackService.kt` — instantiate `AppEqualizerController`, gate system-EQ
  broadcast on EqState.mode
- `presentation/navigation/AppDestination.kt` — `Equalizer` destination
- `presentation/ui/App.kt` — route the new destination
- `presentation/ui/pages/NowPlayingPage.kt` — drop in `EqQuickButton`
- `presentation/ui/pages/SettingsPage.kt` — "Audio" section linking to EqualizerPage
- `di/AndroidModule.kt` + `SharedModule.kt` — wire the new components
- `MEMORY.md` (project memory) once shipped — note the system-EQ-vs-app-EQ exclusion invariant

**No new permissions.** No manifest changes.

---

## Tests

- `GenreClassifierTest` — table-driven, ~30 cases covering common tag strings, numeric ID3 codes,
  garbage input.
- `PickPresetForTrackTest` — OFF returns Default, MANUAL returns named preset, AUTO maps canonical
  genre → preset, AUTO with no canonical_genre → Default.
- `LibrarySyncUseCaseTest` — extend: scanned tracks have `canonical_genre` populated after sync.
- Backfill — one test that a track whose canonical_genre starts NULL gets populated on the next sync
  pass.

No instrumented tests for `AppEqualizerController` — it touches `android.media.audiofx` which
doesn't work in unit tests. Verified manually via DHU / phone.

---

## Manual validation checklist

- [ ] EQ off → audio identical to pre-feature build (system EQ still works).
- [ ] EQ on → system EQ broadcast does NOT fire (check via `adb shell dumpsys media.audio_flinger`).
- [ ] Switching tracks in AUTO mode → preset visibly changes in the EQ page in real time.
- [ ] Untagged tracks → preset shows "Default".
- [ ] Force-quit + relaunch → EQ state restored.
- [ ] BT headset connect/disconnect mid-track → EQ still applies (audio session stays the same).

---

## Open questions before starting

1. **Where do existing preferences live** (`ThemePreferences`, `ArtworkPreferences`)? Need to mirror
   that pattern for `EqPreferences`. Worth a 10-min look before structuring `EqPreferencesStore`.
2. **Are there any tracks in the wild with multi-genre tags** (`Rock; Pop`)? The classifier should
   handle this — first-wins is the simplest rule.
3. **DataStore vs the existing settings table** (there's a `Setting.sq` already). Probably reuse
   `Setting.sq` for symmetry with theme/artwork prefs; confirm before implementing.

---

## Rollout

Single PR. No staged rollout — feature defaults to **OFF** so existing users see no behavioral
change until they opt in via Settings → Audio.
