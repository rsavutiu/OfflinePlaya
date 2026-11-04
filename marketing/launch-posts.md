# Launch posts — drafts

**Nothing here has been posted.** Every item below is a post made in your name,
so sending it is yours to do. Check each community's self-promotion rules
before posting — several subreddits require flair, an account-age/karma
minimum, or ban app posts outside a weekly thread.

### What is actually live on Play, read 2026-11-04

Read off the public Play page in the browser pane, not inferred from the repo
(WebFetch can't read Play — the page is JS-rendered and comes back truncated):

| | Live on the store page | In the repo |
|---|---|---|
| Last updated | **Oct 17, 2026** | 1.1.0 submitted Nov 1 |
| en-US short description | "Offline player for your own music. Stream to your PC, EQ, no ads, no cloud." | "Offline MP3 & FLAC music player with equalizer for the music you already own." |
| de-DE short description | "Offline-Player für deine eigene Musik. Streame auf deinen PC, Equalizer, keine Werbung, keine Cloud." (~105 chars, over Play's 80) | "Offline-Musikplayer mit Equalizer für MP3 und FLAC auf deinem Telefon." |
| de-DE full description | formal *Sie*, "(NEU)", "41 Sprachen" | informal *du*, no "NEU" |
| Trailer video ID | `TZEk8RL2cIc` | `iyMItL6WTzU` (the demo) |
| Downloads | **50+** | — |

**None of the 2026-11-01 work is live.** Not the 13 hand-written localized
listings, not the rewritten short description, not the new demo video. The store
page still serves the pre-11-01 listing — the German text is a machine
translation of the old English copy, which is why it breaks the 80-character
limit and still carries the "keine Werbung, keine Cloud" phrasing that
`check_listing.py` bans and that got the app promo-flagged in the first place.

So either the 11-01 change set is still in review after three days, or it did
not apply. **Checking that in Console is the first thing to do** — every post
below points people at a listing that is three weeks stale, and the demo they
were drafted around is not the video the store shows.

The one piece of good news is unambiguous: **50+ downloads**, against the ~5
that framed this as a failure. That is the baseline to measure the posts
against.

Shared links:

- Play: <https://play.google.com/store/apps/details?id=com.offlineplaya.android>
- GitHub: <https://github.com/rsavutiu/OfflinePlaya>
- Demo video (3:09): <https://youtu.be/iyMItL6WTzU>
- FOSS APK (no Play libraries): GitHub Releases

### Claim discipline

Only claims the app actually ships. Two to keep straight, because the people
most likely to care are the ones who will test them:

- **Bit-perfect is narrower than "bit-perfect USB DAC output" sounds.** It needs
  Android 14+, and because Media3's sink writes 16-bit PCM, only tracks at
  **16-bit or less** are bit-perfect. 24-bit files fall back to the system
  mixer. Say so in the post — an audiophile audience will find it in an hour
  and the correction costs more than the caveat.
- **No testimonials, no "hundreds of users."** The live figure is 50+ downloads
  and no rating yet. There's no need to mention numbers at all — lead with what
  the app does.

---

## 1. r/androidapps — the offline / no-account angle

> **[Dev] OfflinePlaya — an offline music player for the files you already own**
>
> I kept running into the same thing: every "music player" on Play now wants an
> account, shows ads, or is a front-end for a subscription. I have a folder of
> ripped CDs, Bandcamp downloads and DJ sets, and I wanted an app that just
> plays them.
>
> So OfflinePlaya does exactly that and nothing else: point it at a folder, it
> scans your files (MP3, FLAC, M4A, OGG, WAV, Opus), and you browse by artist,
> album, folder or track. No account, no ads, no tracking, no cloud. Works in
> airplane mode because there is nothing to connect to.
>
> Some things that took the longest:
>
> - **10-band EQ** with hand-tuned presets, a manual graph, and an Auto mode
>   that picks a preset from the track's genre tag. It also steps out of the way
>   of your OEM's own EQ (Dolby etc.) instead of fighting it.
> - **A tag editor that writes back into the file**, so your fixes show up in
>   every other app too — not just in a private database.
> - **Play to PC** — stream your phone's audio over your own WiFi to a free
>   desktop receiver (Windows/Mac/Linux) and out through your real speakers.
>   Nothing is saved on the PC and the stream never leaves your network.
> - **Smart playlists** built from actual listening history: Recently played,
>   Most played, Forgotten favorites, Never played.
> - Gapless, optional crossfade, synced lyrics (.lrc + embedded), Android Auto,
>   Material You theming, and theming pulled from the album art.
> - **USB DAC output** — on Android 14+ it requests a bit-perfect mixer, which
>   works for 16-bit-and-under tracks; deeper files go through the system mixer.
>
> Free, no ads, no IAP. There's also a FOSS build on GitHub with no Play
> libraries at all if you'd rather sideload.
>
> Play: https://play.google.com/store/apps/details?id=com.offlineplaya.android
> Source: https://github.com/rsavutiu/OfflinePlaya
> Demo (3 min): https://youtu.be/iyMItL6WTzU
>
> Happy to hear what's missing — especially from anyone with a big local
> library, because that's where the scanner gets interesting.

Rules note: read the sidebar before posting — app-promotion subs commonly want
a dev flag or flair, and restrict how often the same app may be posted.

---

## 2. Audiophile / DAP angle

Candidate communities (check each one's rules; some ban app promotion
outright): r/headphones, r/audiophile, r/DigitalAudioPlayer and other
portable-audio subs, Head-Fi's Android forum, and the Audio Science Review
forum.

> **Android player with a bit-perfect USB DAC path on Android 14+ — would like
> it torn apart**
>
> I've been building a local-files Android player and the part I'd most like
> scrutinised is the USB output path, because the Android stack makes this
> annoying.
>
> On Android 14+ the app asks for a bit-perfect `AudioMixerAttributes` mixer and
> pins the player to the DAC. While that's active, the EQ, the loudness preamp
> and the crossfade engine all remove themselves from the signal path — the
> point being that nothing of ours touches the samples.
>
> **The honest limitation:** Media3's `DefaultAudioSink` writes 16-bit PCM with
> float output off, so only tracks at 16-bit or below actually come out
> bit-perfect. 24-bit FLAC falls back to the system mixer, and the on-screen DAC
> badge says "System mixer" rather than pretending otherwise. Fixing the deeper case means going
> around the default sink, which I haven't done yet.
>
> The rest of it is a plain offline player: FLAC/MP3/M4A/OGG/WAV/Opus, gapless,
> a 10-band EQ with a manual graph, folder browsing, a tag editor that writes
> into the file. No streaming, no account.
>
> If you have a DAC and five minutes, I'd like to know what it reports on your
> hardware — chip, Android version, and whether the DAC badge reads
> "Bit-perfect" or "System mixer".
>
> Play: https://play.google.com/store/apps/details?id=com.offlineplaya.android
> Source: https://github.com/rsavutiu/OfflinePlaya
> Demo (3 min): https://youtu.be/iyMItL6WTzU

That ask — "tell me what your DAC reports" — is the part that makes this a
discussion rather than an ad. Keep it.

---

## 3. Show HN

Title (HN wants plain, under ~80 chars, no exclamation):

> Show HN: OfflinePlaya – an offline Android music player for your own files

Body:

> I wrote this because local music playback on Android has quietly rotted. The
> good players are abandoned, and the maintained ones are streaming front-ends.
>
> It's Kotlin Multiplatform — all the domain, data and Compose UI live in a
> shared module, with only the Activity, the playback service and the platform
> scanners in the Android layer. Android-only at runtime today, but the seams
> are real rather than aspirational.
>
> A few things that were more work than expected:
>
> - **Library scanning.** `DocumentFile.listFiles()` under SAF is pathologically
>   slow; bulk `DocumentsContract` cursors plus parallelised metadata reads turned a
>   scan you could watch tick up one track at a time into one that finishes
>   while you are still looking at it.
> - **Audio effects arbitration.** OEM system EQs (Dolby and friends) attach via
>   a broadcast intent. You have to explicitly close that session to run your own
>   `Equalizer`, and reopen it when the user turns yours off, or the two stack.
> - **Hardware volume past max.** Claiming `PLAYBACK_TYPE_REMOTE` with an
>   extended range lets the volume keys ramp into a software preamp. Side effect
>   I accepted: the system volume dialog renders cast-style, because remote
>   playback type *is* the Cast API.
> - **Bit-perfect USB output** on Android 14+, which works for ≤16-bit tracks
>   because Media3's sink writes 16-bit PCM.
> - **Play to PC** — a small protocol streaming PCM over the LAN to a Compose
>   Desktop receiver.
>
> Source is GPL-3.0-or-later, there's a FOSS build with no Play libraries, and the
> commit history is honest about being heavily AI-assisted.
>
> Play: https://play.google.com/store/apps/details?id=com.offlineplaya.android
> Source: https://github.com/rsavutiu/OfflinePlaya
> Demo (3 min): https://youtu.be/iyMItL6WTzU

HN responds to the engineering specifics, not the feature list — the
`DocumentFile.listFiles` and `PLAYBACK_TYPE_REMOTE` bits are the hook. Post on
a weekday morning US time and answer comments for the first couple of hours.

---

## 4. Directory listings (not posts — low effort, lasting)

These are forms rather than social posts, so no community rules to trip over:

- **AlternativeTo** — list it as an alternative to Poweramp, Musicolet, Retro
  Music Player, BlackPlayer, Phonograph. Suggested blurb:
  > Offline Android music player for local files (MP3, FLAC, M4A, OGG, WAV,
  > Opus). 10-band EQ, gapless, synced lyrics, tag editor, folder browsing,
  > smart playlists, Android Auto, USB DAC output, and streaming to a desktop
  > receiver over your own WiFi. No account, no ads, no tracking.
- **Obtainium** — already matched by the release asset name; worth confirming a
  user-facing mention in the README's install section.
- **F-Droid main repo** — IzzyOnDroid's inclusion policy rejects
  generative-AI-assisted code (see `docs/izzyondroid-submission.md`). F-Droid's
  own policy is a **separate document and has not been checked** — read it
  before assuming the same outcome either way.
- **"Best offline music player" roundup blogs** — reaching those is outreach
  email, a different kind of ask, and not drafted here. (Adding your own app to
  Wikipedia's comparison lists is a conflict of interest — don't.)

---

## 5. The video you already have

The demo is the strongest asset in the repo, and on the Play side it is already
wired in: `bf6c0b1` records that both the main Video field and the XR slot point
at it, saved and submitted alongside 1.1.0 on 2026-11-01 (the video fields are
shared across all 13 locales, not per-locale). The per-form-factor phone slot is
deliberately empty so it inherits the main field.

`fastlane/metadata/android/*/video.txt` now carries the URL too, so a future
`fastlane supply` run picks it up instead of depending on a Console visit.

Where it is still underused is off-Play:

1. ~~The README links it as plain text~~ — now a clickable thumbnail at the top
   of the README.
2. **Every post above** — three minutes of footage answers "what does this look
   like" faster than any screenshot set.

---

## 6. The Play listing is done — don't re-do it

An earlier draft of this file claimed the 13 localized listings had never been
uploaded. That was wrong, and it is worth recording why, because the repo
invites the mistake:

- `release.yml` really does ship only the AAB and the English changelog, and its
  comment really does say to add localized listings in Console. But Console
  entry is a **manual step that was already taken** — all 13 were added under
  Manage languages and submitted for review on 2026-11-01, together with the
  video and the 1.1.0 release.
- **Console state is not visible from the repo.** The absence of automation is
  not evidence that the manual step was skipped. Check the commit history
  (`git log -- store-listing fastlane`) and the project notes before concluding
  anything about what is live.
- The limits are already enforced by **`python scripts/check_listing.py`**,
  which checks title ≤30, short ≤80, full ≤4000, glossary-term survival,
  promo-keyword bans and script mismatches. All 13 locales pass.
- **Do not hand-roll the character counts.** `wc -m` in Git Bash counts *bytes*
  for non-Latin text, which reported ja-JP's title as 36 characters when it is
  21, and ru-RU's as 40 when it is 27 — and produced a table of limit breaches
  that did not exist.
- Likewise, do not test for the Play-to-PC section by grepping for `PC`: it
  won't appear in Japanese, Russian or Turkish copy. `check_listing.py` already
  covers this, because its glossary includes Windows / Mac / Linux and those
  terms appear only in that paragraph.

**So the Play surface is not the open work.** It changed on 2026-11-01 — too
recently to read an install effect. The genuinely untouched channels are the
off-Play ones in sections 1-4.
