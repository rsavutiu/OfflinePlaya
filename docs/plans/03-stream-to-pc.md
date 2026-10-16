# Stream to PC — live LAN audio receiver (cross-platform)

**Goal.** Let OfflinePlaya on the phone **stream its audio live, over the local WiFi, to a
companion app on a PC** that plays it out the PC's wired audio system. The phone stays the whole
brain — library, decode, queue, transport controls. The PC is a **stateless live sink**: audio
comes in over a socket, goes straight to the speakers, and **nothing is ever written to disk**. No
Bluetooth (lossy), no file transfer, no library mirroring, no internet.

Hard requirement: the PC receiver **must run on Windows, macOS *and* Linux** from one codebase.
That dictates a **Compose Multiplatform / Kotlin-JVM desktop target** — the JVM is the one runtime
that gives us identical code and a universal audio API on all three, and the repo's `shared` module
is already structured for a desktop target that was never built.

This is the *minimum* version: one phone → one PC, lossless PCM over the LAN, shared-mode output.
Multi-room, hi-res renegotiation polish, and bit-perfect exclusive output are explicitly later.

---

## Scope summary

| Area              | In (v1)                                                            | Out (v2 / later)                                  |
|-------------------|-------------------------------------------------------------------|---------------------------------------------------|
| Topology          | One phone → one PC receiver on the same LAN                        | Multi-room, multiple sinks, sync groups           |
| Transport         | Uncompressed **PCM over TCP** + a small control channel           | Opus/FLAC-on-wire, UDP/RTP, adaptive bitrate      |
| PC storage        | **None** — live stream only, never touches disk                   | (never — this is the whole point)                 |
| PC audio out      | `javax.sound.sampled` (OS mixer / shared mode)                    | Bit-perfect exclusive (WASAPI/CoreAudio/ALSA hw)  |
| Discovery         | mDNS / DNS-SD (`_offlineplaya._tcp`)                              | Manual IP entry as a fallback                     |
| Pairing           | PIN shown on PC, entered on phone                                 | Accounts, persistent trust store, TLS             |
| Platforms         | Win/macOS/Linux desktop receiver; Android controller              | iOS controller, a phone acting as a sink          |
| Phone-side DSP    | Bypassed in this mode (see "Play to PC" mode)                     | Streaming the EQ'd/preamp'd signal as an option   |

---

## Why JVM desktop (the "runs on every PC" decision)

- **One audio API everywhere.** `javax.sound.sampled.SourceDataLine` is the JVM's output sink and
  maps to **WASAPI/DirectSound on Windows, CoreAudio on macOS, ALSA/PulseAudio on Linux** with no
  per-OS code. Feed it a PCM `AudioFormat` and bytes; it plays. That universality is the reason to
  accept the JVM.
- **Shared Kotlin.** A Compose Desktop target under `shared` reuses the domain + queue + protocol
  models already written for Android. The receiver UI is tiny Compose.
- **The honest cost — not bit-perfect on the PC.** Java Sound plays through the OS mixer (**shared
  mode**): the OS may resample and will mix system sounds in. So the PC output is **lossless over
  the wire but not bit-perfect at the PC's DAC**. True exclusive-mode output (WASAPI exclusive,
  CoreAudio hog, ALSA `hw:`) needs per-OS native code via JNI — which breaks "one codebase runs
  everywhere." We take shared-mode for v1; bit-perfect-to-PC is a later, per-OS add-on. (Note the
  contrast with the phone's USB-DAC path, which *is* bit-perfect — that stays the audiophile route;
  this is the "no cable, PC speakers" route.)
- **ARM reality.** "Every PC" still means shipping **per-OS, per-arch installers** (Win x64, macOS
  arm64 + x64, Linux x64 + arm64) — all built from the single codebase, just packaged N times.

---

## Architecture

### Shared (`shared/commonMain`)

```
domain/stream/
  StreamFormat.kt       data class (sampleRate, channels, bitsPerSample, encoding=PCM_SIGNED, bigEndian=false)
  StreamControl.kt      sealed: Hello(name, protoVer) / Format(StreamFormat) / Volume(0..100)
                        / Flush / Heartbeat / Bye
  ReceiverInfo.kt       data class (name, host, port)  // from discovery
  StreamProtocol.kt     frame constants, magic, version; encode/decode of control frames
```

Pure Kotlin, no platform imports — same rules as the rest of `domain/`. The wire framing lives here
so phone and desktop can never disagree on it.

### Android (source + controller)

```
androidMain / androidApp:
  audio/PcmStreamSink.kt         a Media3 AudioSink (or TeeAudioProcessor) that captures decoded
                                 PCM from ExoPlayer and writes it to the network sender instead of
                                 (or in addition to) the local AudioTrack
  net/StreamSender.kt            opens TCP to the receiver, sends Hello+Format, then streams PCM;
                                 paces by the player clock; sends Flush on seek/skip, Format on
                                 track-format change, Bye on stop
  net/ReceiverDiscovery.kt       NsdManager browse for _offlineplaya._tcp -> List<ReceiverInfo>
  presentation/stream/PlayToPcStateHolder.kt   connection state, selected receiver, volume
```

- **Routing.** In Play-to-PC mode the decoded PCM goes to the network, not the phone speaker. A
  custom `AudioSink` is the clean hook (Media3 lets you replace the sink); `TeeAudioProcessor` is
  the quick-and-dirty alternative if we want local + remote simultaneously.
- **Reuse the bypass pattern.** This mode must make `AppEqualizerController`, `PreampVolumePlayer`,
  `CrossfadeController` and the USB-DAC path **step out** — exactly like `bitPerfectActive` already
  does. Simplest: fold "Play to PC active" into the same `softwareDspAllowed == false` gate so the
  signal-path plumbing is shared. The stream carries clean, decoded PCM; per-track volume is scaled
  in the PCM on the phone so the PC stays a dumb sink.

### Desktop receiver (`shared` desktop target + `desktopApp`, Compose/JVM)

```
desktopApp/
  Main.kt                        Compose Desktop window: receiver name, status, selected output
                                 device, volume, pairing PIN
  audio/PcmPlayer.kt             javax.sound.sampled SourceDataLine; (re)opens on Format frames;
                                 ring buffer ~1 s; flush() drains on Flush frames
  net/StreamServer.kt            TCP accept; reads frames; routes control vs PCM
  net/ReceiverAdvertiser.kt      jmdns advertises _offlineplaya._tcp with the receiver name + port
```

jmdns is pure Java → works on all three OSes. The window is deliberately minimal: it exists to be
discoverable, show "Playing from <phone>", pick the output device, and surface the pairing PIN.

---

## Wire protocol (PCM over TCP)

One TCP connection, length-prefixed frames:

```
[ 1 byte type ][ 4 bytes big-endian length ][ payload ]
  type 0x01 CONTROL  payload = UTF-8 JSON (StreamControl)
  type 0x02 AUDIO    payload = raw interleaved PCM for the current Format
```

Sequence:

1. Phone connects → `CONTROL Hello{name, protoVer}`.
2. PC replies `CONTROL Hello{name, protoVer}` (and, first time, requires a PIN — see Pairing).
3. Phone sends `CONTROL Format{44100, 2, 16, PCM_SIGNED, LE}`.
4. PC opens a `SourceDataLine` for that `AudioFormat`, starts its ring buffer.
5. Phone streams `AUDIO` frames, paced to the player's audio clock.
6. On **seek/skip**: phone sends `CONTROL Flush`; PC drains its buffer so the new audio is prompt.
7. On a **track whose format differs** (e.g. 44.1→48 kHz, 16→24-bit): phone sends a new `Format`;
   PC reopens the line. A brief gap on format switch is acceptable in v1 (gapless holds *within* a
   format).
8. `CONTROL Heartbeat` both ways (~2 s) to detect WiFi death; `Bye` on clean stop.

**Bandwidth is a non-issue:** 16/44.1 stereo PCM ≈ 1.4 Mbps, 24/96 ≈ 4.6 Mbps — trivial on WiFi.

---

## Buffering & responsiveness (the actual hard part)

- **Ring buffer on the PC, ~0.5–2 s.** Rides out WiFi jitter and keeps playback gapless. This
  buffer *is* latency, so:
- **Flush on every transport command.** Pause/skip/seek send `Flush`; the PC drops buffered audio
  so controls feel immediate instead of lagging a bufferful behind.
- **Underrun handling.** If the buffer starves (WiFi hiccup), output brief silence and re-fill
  rather than crackle; never block the audio line's callback.
- **Pacing.** The phone sends at playback rate (driven by the ExoPlayer clock), not as fast as the
  socket allows, so the PC buffer neither starves nor grows unbounded.
- This is the Snapcast/AirPlay problem space — well-trodden, but it's where the iterations go.

---

## Discovery, pairing, security

- **Discovery:** DNS-SD service `_offlineplaya._tcp` — jmdns advertises on desktop, Android
  `NsdManager` browses. Manual IP entry as a fallback for hostile networks.
- **Pairing:** the PC shows a 6-digit PIN; the phone enters it on first connect. Prevents any phone
  on the WiFi from hijacking the receiver. Store the paired key per-receiver on the phone.
- **Security posture:** LAN-only, no internet, no accounts. v1 is cleartext on a trusted home LAN;
  TLS is a later hardening. Bind the receiver to the local subnet; reject off-subnet peers.
- **Firewall:** first launch triggers the OS inbound-connection prompt (Windows Defender / macOS /
  ufw). Document it; there's no way around a user approving an inbound listener.

---

## Packaging — "runs on every PC"

Compose Desktop's `nativeDistributions` (jpackage) with a **bundled JRE** so the user installs
*nothing else*:

| OS      | Artifacts                     | Friction to flag                                  |
|---------|-------------------------------|---------------------------------------------------|
| Windows | `.msi` / `.exe`               | SmartScreen warning unless the exe is signed      |
| macOS   | `.dmg` (arm64 + x64)          | Gatekeeper — needs signing + notarization or users right-click-open |
| Linux   | `.deb` / `.rpm` (+ portable tar) | per-distro audio backend (PulseAudio/PipeWire/ALSA) via Java Sound |

Also emit a portable runnable fat-JAR (needs a system JRE) for the technical crowd. One Gradle
module, built per OS/arch on CI (GitHub Actions matrix) — the same way one codebase becomes three
installers.

---

## v1 scope cut vs later

**In v1:** one phone → one PC; mDNS discovery + PIN pairing; PCM/TCP; shared-mode Java Sound
output; ring-buffer + flush; "Play to PC" mode that bypasses phone DSP; native installers for
Win/macOS/Linux.

**Later:** multi-room sync; on-wire compression (Opus/FLAC) for weak WiFi; bit-perfect exclusive
output per OS (JNI); TLS; output-device hot-switching; streaming the EQ'd signal as an option; an
iOS controller.

---

## Risks

| Risk                                             | Mitigation                                               |
|--------------------------------------------------|----------------------------------------------------------|
| Control latency behind the buffer                | Flush-on-command; keep the default buffer small          |
| WiFi dropouts → stutter                          | Ring buffer + underrun silence + heartbeat reconnect     |
| Java Sound device/format quirks across OSes      | Query `AudioSystem` for supported lines; fall back to 16/44.1 |
| macOS notarization / Windows signing            | Budget for certs; document right-click-open for unsigned |
| Not bit-perfect on PC (OS mixer)                 | Set expectations in UI; offer the USB-DAC path for audiophile use; exclusive mode as a later per-OS add-on |
| Scope: a whole second app to maintain            | Keep it thin; share everything possible via `shared`     |
| Identity drift (an "offline" app doing LAN audio)| Gate strictly behind "Play to PC" mode; no internet ever |

---

## Open questions

1. **Volume ownership** — scale PCM on the phone (dumb PC, chosen above) vs control the PC's
   `SourceDataLine` gain (lets the PC's own volume keys work). Lean phone-side for v1.
2. **Does the phone keep its screen/CPU awake** streaming for a whole album? Likely a foreground
   service + wakelock while Play-to-PC is active — mirror the playback service's behaviour.
3. **One desktop module or a separate repo?** Prefer a `desktopApp` module here so `shared` reuse is
   free and versions move together.
