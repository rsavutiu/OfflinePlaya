# OfflinePlaya

A privacy-respecting, **offline** music player for Android. It plays the audio
files you already own — no accounts, no streaming, no ads, no tracking.

## Features

- **Strictly offline** — plays local files (FLAC, MP3, M4A, OGG, WAV…); nothing
  is ever streamed or uploaded.
- **Automatic library scan** — your `Music` folder and device-indexed audio are
  picked up on launch; add more folders via SAF, exclude any you don't want.
- **Equalizer** — Off / Manual / Auto (genre-aware) modes, a 10-band curve, a
  manual preamp, and a growing set of presets: Rock, Pop, Electronic, Synth-Pop,
  EDM, Hip-Hop, Jazz, Classical, Bass Boost, Vocal Boost, Treble Boost, Party.
- **USB DAC support — bit-perfect *and* non-bit-perfect:**
  - **Bit-perfect** output on **Android 14+**, when both the phone and the DAC
    expose a bit-perfect mixer and the track is **16-bit or less**. The stream
    reaches the DAC untouched; EQ, preamp and crossfade are bypassed.
  - **Any other USB DAC** (older Android, no bit-perfect mixer, or >16-bit
    sources) plays cleanly through the system mixer. On some USB DACs the
    equalizer and preamp have no audible effect, since they run as Android audio
    effects the DAC's output path may skip; the app flags this in the EQ screen.
- **Playback** — gapless by default, optional crossfade, a loudness preamp, and
  hardware-volume integration.
- **Also** — tag editor, embedded/sidecar/remote album art, play history and
  smart playlists, Android Auto browsing, and 40+ languages.

## Build & run

```bash
# Compile the shared KMP module
./gradlew :shared:compileDebugKotlinAndroid
# Compile the Android app
./gradlew :androidApp:compileDebugKotlin
# Run the unit test suite
./gradlew :shared:testDebugUnitTest
```

Only the Android target ships today; the `shared` module is Kotlin Multiplatform
so iOS/desktop targets can be added later.

## Releasing

See [RELEASING.md](RELEASING.md) — pushing an annotated `v*` tag builds a signed
AAB and ships it to the Google Play production track.
