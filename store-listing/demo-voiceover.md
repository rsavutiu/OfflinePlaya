# OfflinePlaya demo — voiceover script

For the 3:09 demo video (`aA88EhbNlVw`). Written to be read at a normal,
unhurried pace — roughly 140 words per minute. Total ≈ 360 words, which leaves
real pauses rather than filling every second.

## Why this exists

The video has **no audio at all from 0:00 to about 1:25** — not because of a
copyright action, but because nothing is playing yet in that part of the demo
(permissions, folder picking, the library scan). That silent 85 seconds is the
exact stretch where a viewer decides whether to keep watching.

The music from 1:25 onward is claimed by three rights holders (Romeo Void,
Sabrina Carpenter, The Cure). The only effect is that the video is blocked in
Russia and Belarus. No strike, no channel impact.

So the voiceover carries the first half, and thins out once the music starts —
from 1:25 the track should be audible and the narration should get out of its
way.

## Script

### 0:00 – 0:14 · Hook
> If you still keep your own music — ripped CDs, Bandcamp downloads, DJ sets,
> bootlegs — streaming apps are the wrong tool for it. This one is built for
> files.

### 0:14 – 0:30 · Permission and setup
> OfflinePlaya asks for access to the audio already on your phone. That's the
> only thing it ever asks for. No account, no sign-up, no email.

### 0:30 – 0:55 · Point it at a folder
> You point it at a folder, and it reads your music into a proper library.
> It handles MP3, FLAC, M4A, OGG, WAV and Opus — and it reads the tags, the
> embedded artwork, and the folder covers sitting next to your files.

### 0:55 – 1:25 · Browse and organise
> From there you browse the way your collection is actually organised — by
> artist, by album, or straight down the folder tree, which most players won't
> let you do.
>
> Tags wrong? Fix them in the app. It writes the change back into the file, so
> every other app sees your correction too.

*(music starts around here — from this point let it breathe)*

### 1:25 – 1:55 · Now playing and EQ
> Ten-band equalizer, with presets tuned by hand. Or let Auto mode pick one
> from each track's genre as it plays.

### 1:55 – 2:20 · USB DAC
> Plug in a USB DAC and, on Android 14 and up, it hands your DAC an untouched
> signal — bit-perfect, with the system mixer out of the way.

### 2:20 – 2:45 · Play to PC
> And it plays to your computer. Over your own WiFi, to a companion app on
> Windows, Mac or Linux — straight out of your real speakers. Nothing is saved
> on the PC, and it never leaves your home network.

### 2:45 – 3:09 · Close
> No ads. No tracking. No account. No subscription. Just the music you already
> own, on the phone you already have.
>
> OfflinePlaya. On Google Play, and on GitHub.

## Notes for recording

- Read it flat and unexcited. The features are interesting on their own; a
  hyped delivery makes a one-person app sound like an ad.
- The two lines worth slowing down for are the USB DAC one and "writes the
  change back into the file" — those are the claims competitors can't make.
- Sections are sized to the current cut. If your on-screen timing differs,
  slide the blocks; don't speed up the read to hit a mark.

## Producing the audio

Any of these will do a convincing read. All have usable free tiers at this
length:

- **ElevenLabs** — best quality; a 3-minute read fits in the free monthly
  characters.
- **Microsoft Edge's built-in Read Aloud** (or `edge-tts` on the command line)
  — free, unlimited, and the neural voices are good. `en-GB-RyanNeural` or
  `en-US-GuyNeural` suit this.
- **Google Cloud TTS** Studio voices, if you already have a project.

Export a single WAV or MP3 at the full length, with the silences between
sections preserved, so it lines up without manual nudging.

## Attaching it

YouTube's editor cannot add an audio track — it can only trim, blur, mute and
replace claimed songs. So this needs a local edit and a re-upload.

Download the original from Studio (⋮ → Download), then mix the voiceover over
the existing audio, ducking the music under it:

```bash
ffmpeg -i demo.mp4 -i voiceover.wav -filter_complex "[0:a]volume=0.30[bed];[bed][1:a]amix=inputs=2:duration=first:dropout_transition=0:normalize=0[mix]" -map 0:v -map "[mix]" -c:v copy -c:a aac -b:a 192k demo-vo.mp4
```

`volume=0.30` drops the music to about a third under the narration. Because
the first 85 seconds have no music at all, the voiceover sits there on its own
at full level — no ducking needed.

**One consequence to plan for:** a re-upload gets a new video ID, and the Play
Console listing points at the current one. The listing would need the new URL,
which means another listing change and another review cycle. Worth batching
with any other listing edit rather than doing it alone.

**Optional, while you're re-uploading:** replace the three claimed tracks with
something from YouTube's Audio Library. The claims cost you Russia and Belarus
today, which is minor — but a demo of a music player works just as well with
music nobody owns, and then the video is clean everywhere.
