# Demo video — metadata

The demo carrying the voiceover is live at <https://youtu.be/iyMItL6WTzU>
(uploaded 2026-11-01). It replaces `aA88EhbNlVw`, which had no audio for its
first 85 seconds.

Kept here as the source of truth for the title, description and tags, so a
future re-upload or a translation doesn't start from scratch.

## Title

> OfflinePlaya — offline music player with bit-perfect USB DAC output

Leads with the thing competitors can't claim rather than "demo". YouTube
truncates around 60–70 characters in search results, so the important half is
first.

Shorter alternative, if you'd rather keep it plain:

> OfflinePlaya — the offline music player for files you already own

## Description

> OfflinePlaya is an offline music player for Android, built for people who
> still keep their own music — ripped CDs, Bandcamp downloads, DJ sets,
> bootlegs, audiobooks, field recordings.
>
> No streaming. No account. No ads. No cloud. It plays the audio files already
> on your phone: MP3, FLAC, M4A, OGG, WAV and Opus.
>
> What's in this demo:
> 00:00 Pointing it at your music
> 00:30 Reading the library — tags, embedded art, folder covers
> 00:55 Browsing by artist, album and folder, and fixing tags in place
> 01:25 Ten-band equalizer with hand-tuned presets and an Auto mode
> 01:55 USB DAC output — bit-perfect on Android 14+
> 02:20 Play to PC over your own WiFi
> 02:45 Privacy
>
> Why a local player: if you have a collection, a streaming app is the wrong
> tool. OfflinePlaya is built for files — fast library scanning, real folder
> browsing, and a tag editor that writes changes back into your files so other
> apps see them too.
>
> Play to PC streams your phone's audio live, over your own network, to a
> companion app on Windows, Mac or Linux — out through your real speakers.
> Nothing is saved on the PC and the stream never leaves your home network.
>
> Open source, GPL-3.0. A Google-Play-free build is published on GitHub for
> anyone who wants to sideload it.
>
> Google Play: https://play.google.com/store/apps/details?id=com.offlineplaya.android
> Source and FOSS builds: https://github.com/rsavutiu/OfflinePlaya
>
> Available in 40+ languages.

The timestamps double as chapter markers — YouTube generates chapters
automatically when the first one is `00:00` and there are at least three, each
at least ten seconds apart. Chapters measurably help retention, and they make
the DAC section findable by people who only care about that.

## Tags

    offline music player, android music player, flac player android,
    mp3 player android, usb dac android, bit perfect android, local music
    player, music player no ads, equalizer android, offline mp3 player,
    android auto music, tag editor android

## Where the video is wired in

Play's listing has more than one video slot, and the old demo was only in the
Android XR one — Console calls that change "non-spatial XR video" — so it never
appeared on the store page anyone actually sees. Both now point at the new
video:

- **Video** (the main field, shown on the store listing) — was the Shorts
  `TZEk8RL2cIc`, now the demo. This is the one that matters.
- **Android XR assets → video** — was the old demo, now the new one.
- **Phone assets → video** is deliberately empty: an empty per-form-factor slot
  inherits the main Video field, and filling it would be a second copy to keep
  in step.

The video fields are **shared across all 13 listing locales**, not per-locale,
so one edit covers every language.

## Follow-ups

1. **Submitted for review on 2026-11-01**, together with 1.1.0 and the 13
   localized listings.
2. **The old video (`aA88EhbNlVw`) is still public.** Two public copies compete
   in search — make it unlisted (keeps its views and any inbound link) or
   delete it. Not done here: it is a public post, so it is yours to decide.
3. **The audio claims will reappear on the new upload**, since the music is
   unchanged — blocked in Russia and Belarus, no strike. If you want it clean
   everywhere, swap the tracks for YouTube Audio Library ones and re-render:
   the voiceover is a separate track and would not need redoing.
   `scripts/build_demo_voiceover.py` regenerates it in about ninety seconds.
