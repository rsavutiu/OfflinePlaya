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
- **Stream to PC (Play to PC)** — optionally stream live audio over your Wi-Fi to
  a companion **Windows / macOS / Linux** receiver that plays it through the
  computer's speakers. The phone stays the brain; **nothing is stored on the PC**.
  Off by default — the app stays strictly offline until you turn it on. See
  [Stream to PC](#stream-to-pc-play-to-pc) below.
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

The Android app is the main target; the `shared` module is Kotlin Multiplatform
so iOS/other targets can be added later. A separate **Compose Desktop** receiver
(`:desktopApp`, for the Stream-to-PC feature) runs on Windows/macOS/Linux — see
below.

## Stream to PC (Play to PC)

Play your phone's audio live through a computer's speakers over Wi-Fi — handy for
piping your library into a wired hi-fi. The phone does all the work (library,
decode, queue, transport); the PC is a dumb live sink. **Nothing is ever written
to the PC's disk**, and the feature is **off by default** — turning it on is the
only time OfflinePlaya touches the network.

Honest limits: audio is lossless over the wire but plays through the computer's
OS mixer (shared mode), so it is **not bit-perfect at the PC's DAC** — for the
audiophile path use the phone's USB-DAC output instead. The phone's EQ / preamp /
crossfade are bypassed in this mode (they run as Android output effects that the
captured stream never passes through).

### 1. Run the receiver on the computer

The receiver is a small cross-platform app. Build a portable copy (a folder with
a bundled Java runtime — no install, nothing else to download):

```bash
# Windows (PowerShell)
.\gradlew.bat :desktopApp:createDistributable
# macOS / Linux
./gradlew :desktopApp:createDistributable
```

The result lands in `desktopApp/build/compose/binaries/main/app/OfflinePlayaReceiver/`.
Run the `OfflinePlayaReceiver` launcher inside it. (Or just `./gradlew :desktopApp:run`
to launch it directly while testing.)

Pre-built downloads for every platform are produced by the
**Desktop Receiver distributables** GitHub Actions workflow (run it from the
Actions tab, or push a `desktop-v*` tag) — grab the artifact for your OS.

**First launch — the app is unsigned, so clear the one-time warning:**

- **Windows**: SmartScreen → **More info** → **Run anyway**.
- **macOS**: right-click the app → **Open** → **Open** (or
  `xattr -dr com.apple.quarantine OfflinePlayaReceiver.app`). It opens normally
  afterwards.
- **Linux**: nothing — it just runs.

Approve the OS **firewall prompt** on first launch so the phone can reach it
(it listens on TCP **45117** and announces itself via mDNS on UDP 5353).

### 2. Connect from the phone

1. Start playing something, then open **Settings → Audio → Play to PC**.
2. Pick your computer from the discovered list and tap **Connect** — audio moves
   to the PC's speakers, with play / pause / skip working as usual.
3. No computer in the list? Use **Connect by IP address** and type the
   `ADDRESS` shown on the receiver window (e.g. `192.168.1.20:45117`). If
   discovery never works, it's usually the PC firewall blocking mDNS.
4. Tap **Disconnect** (or close the receiver) to return audio to the phone.

### Troubleshooting

- **High frequencies sound rough / resampled** — your computer's output device
  is probably running at a different sample rate than the music, so the OS is
  resampling. Match them: on Windows, *Sound → your device → Properties →
  Advanced → Default Format* → pick the rate shown in the receiver's `FORMAT`
  tile (e.g. 44100 Hz). The receiver shows the live format precisely so you can
  match it.
- **It's choppy / drops out** — Wi-Fi signal; move closer to the router or use
  5 GHz. The receiver buffers ~1 s to ride out jitter.
- **The equalizer has no effect** — expected in this mode (see the limits above).

## Releasing

See [RELEASING.md](RELEASING.md) — pushing an annotated `v*` tag builds a signed
AAB and ships it to the Google Play production track.
