# IzzyOnDroid submission notes

Draft only — **nothing has been submitted**. Opening the request is a post made
in your name, so it's left for you to send.

> ## ⚠️ Read this before sending: we probably do not qualify
>
> Checked against the live
> [App Inclusion Policy](https://izzyondroid.org/docs/general/AppInclusionPolicy/)
> on 2026-11-01. Under *"What criteria would prevent an app to be included?"* it
> opens:
>
> > "We are strongly opposed to apps which are fully or in part created by
> > generative AI tools."
>
> and continues: *"Vibe-coded apps will be rejected."* … *"Readme, Changelogs and
> similar documentation files are allowed to include LLM-generated texts, but the
> code itself should be free of it."* … *"Any lack of transparency discovered by
> us, can lead to the project being degraded to rejected state."*
>
> **169 of this repo's 180 commits carry a `Co-Authored-By: Claude` trailer.**
> That is public, greppable history. So:
>
> - Submitting **without** disclosing is a transparency failure they can verify
>   in one command — and the stated penalty is being marked rejected, which is
>   worse than never having asked.
> - Submitting **with** disclosure is honest, and under the policy as written
>   will almost certainly be declined.
>
> This is a policy judgement, not a technical gap: the GPL-3.0 licence, the
> Play-free `foss` flavour and the signed release attachment all meet their
> other requirements. See "Where to go instead" at the bottom.

## Why IzzyOnDroid first (not F-Droid proper)

F-Droid's main repo builds from source on their infrastructure and has a long
review queue. IzzyOnDroid ingests the **APK you attach to a GitHub Release**,
which this repo already produces on every `v*` tag. Much lower bar, same
audience, and being in IzzyOnDroid is the usual stepping stone to the main repo
later.

## What's already done

- `LICENSE` — GPL-3.0 text, with the notice and SPDX id in the README.
- A `foss` product flavour that links against **no** Google Play libraries.
  Verified with:
  ```bash
  ./gradlew :androidApp:dependencies --configuration fossReleaseRuntimeClasspath
  ```
  (zero hits for `com.google.android.play`, `gms`, `firebase`, including
  transitives).
- `dependenciesInfo { includeInApk = false }` — drops the encrypted dependency
  blob that F-Droid/IzzyOnDroid flag as an opaque binary. The AAB that goes to
  Play keeps its own copy.
- `.github/workflows/release.yml` now attaches
  `androidApp-foss-release.apk` to the GitHub Release (Play still gets
  `bundlePlayRelease`).

## Before you submit

1. **Cut one release with the new workflow** so there is a `foss` APK on a
   GitHub Release for them to pick up. IzzyOnDroid tracks GitHub releases, so
   submitting before such a release exists means there is nothing for them to
   ingest.
2. **Keep signing the FOSS APK with the existing upload key.** Same
   `applicationId`, no suffix — anyone who sideloaded an earlier GitHub APK must
   be able to upgrade in place.
3. **Mirror the store listing into the fastlane layout.** Both F-Droid and
   IzzyOnDroid read metadata from:
   ```
   fastlane/metadata/android/en-US/
     title.txt
     short_description.txt
     full_description.txt
     changelogs/<versionCode>.txt
     images/phoneScreenshots/*.png
   ```
   Today the equivalents live in `store-listing/` (and `release.yml` points
   `whatsNewDirectory` at `store-listing/whatsnew`). Mirroring rather than moving
   avoids breaking the Play upload step. This layout is also what you'd extend
   when you localize the listing.

## Anti-features to disclose up front

Declare these yourself rather than letting a reviewer find them — IzzyOnDroid is
fine with them when they're documented, and less fine with surprises.

**NonFreeNet** applies, but only to some of the hosts. The full list of outbound
endpoints in the code, and where each stands:

| Host | Used for | Free service? |
| --- | --- | --- |
| `musicbrainz.org` | album/artist metadata lookup | yes — open data |
| `coverartarchive.org` | album covers | yes — open data |
| `lrclib.net` | synced lyrics (.lrc) | yes — open database |
| `openlibrary.org` / `covers.openlibrary.org` | cover fallback | yes — open data |
| `en.wikipedia.org` | artist images | yes |
| `api.deezer.com` | album-art fallback | **no — proprietary service** |
| `www.googleapis.com/books/v1` | cover fallback (`MusicBrainzArtSource`) | **no — proprietary service** |

So the honest declaration is **NonFreeNet, on account of the Deezer and Google
Books fallbacks**. Both are last-resort art lookups, both are reachable only
when earlier (free) sources miss, and no library data is uploaded to either —
only an album/artist name query.

Worth stating explicitly in the request: **Play-to-PC is off by default** and is
LAN-only, and there is no analytics, no tracking, no accounts, no ads.

## Note on upgrade paths

The "upgrades in place" guarantee covers **sideloaded** installs only. Google
Play re-signs uploads with its own app-signing key under Play App Signing, so an
APK from Play and the FOSS APK from GitHub almost certainly carry different
signatures — a Play user cannot install over the top and must uninstall first.
Confirm before you advertise otherwise:

```bash
apksigner verify --print-certs androidApp/build/outputs/apk/foss/release/androidApp-foss-release.apk
```

and compare the SHA-256 with the app-signing certificate shown in Play Console →
Test and release → App integrity.

## The request

Inclusion requests go through their GitLab repo,
<https://gitlab.com/IzzyOnDroid/repo>. **Check their current inclusion policy and
issue template before writing anything** — the exact fields change, and the
details below are what you'll want ready rather than a transcription of the
form:

- repo URL and license (GPL-3.0-or-later)
- package id: `com.offlineplaya.android`
- a link to the GitHub release carrying `androidApp-foss-release.apk`
- the anti-feature declaration above

Keep the description to a couple of sentences — the README does the rest.

---

## Updated process facts (checked 2026-11-01)

- **The GitLab repo is archived and read-only.** Inclusion requests now go to
  <https://codeberg.org/IzzyOnDroid/repodata/issues>. Do not file on GitLab.
- **Fastlane metadata is mandatory, and we do not have it yet.** Required at the
  repo root:
  ```
  fastlane/metadata/android/en-US/
    short_description.txt    (max 80 chars)
    full_description.txt     (max 4000 chars, basic HTML)
    title.txt
    images/icon.png
    images/phoneScreenshots/
    changelogs/<versionCode>.txt
  ```
  `en-US` is the fallback for every other locale. We currently keep the
  equivalents in `store-listing/`; they would need mirroring, not moving
  (`release.yml` points `whatsNewDirectory` at `store-listing/whatsnew`).
- **Size:** ~30 MB per APK rule of thumb. Ours is 5.9 MB — fine.
- **Signing:** release-signed by the developer, no `debuggable` / `testOnly`.
  Ours is signed with the upload key. Fine.
- **APK source:** GitHub `releases/` attachments are a preferred source. We now
  have one: `foss-v1.1.0`.

## The request text (only if you decide to send it)

> **App:** OfflinePlaya — offline music player
> **Package:** `com.offlineplaya.android`
> **Source:** https://github.com/rsavutiu/OfflinePlaya
> **Licence:** GPL-3.0-or-later
> **APK:** attached to GitHub releases, e.g.
> https://github.com/rsavutiu/OfflinePlaya/releases/tag/foss-v1.1.0
> (`OfflinePlaya-1.1.0-foss.apk`, 5.9 MB, release-signed)
>
> OfflinePlaya plays the audio files you already own — no accounts, no
> streaming, no ads, no analytics, no tracking. Library scanning, equalizer,
> USB-DAC output, tag editing, Android Auto, 41 languages.
>
> The build attached to GitHub releases is a dedicated `foss` product flavour
> that links against **no Google Play libraries** — verifiable with
> `./gradlew :androidApp:dependencies --configuration fossReleaseRuntimeClasspath`.
> The Play Store build is a separate flavour and is not what I'm submitting.
>
> **Anti-feature declaration — NonFreeNet.** The app is offline by default. Two
> optional, user-triggered album-art fallbacks query proprietary services when
> the free sources (MusicBrainz, Cover Art Archive, LRCLIB, OpenLibrary,
> Wikipedia) miss: `api.deezer.com` and `www.googleapis.com/books/v1`. Only an
> album/artist name is sent; no library data leaves the device. The optional
> "Play to PC" feature is off by default and LAN-only.
>
> **Disclosure:** this project has been developed with substantial assistance
> from an AI coding assistant, recorded honestly in the commit history as
> `Co-Authored-By:` trailers. I am flagging this up front against your policy on
> generative-AI-authored code rather than letting you discover it.

## Where to go instead

If IzzyOnDroid declines (or you'd rather not spend the attempt):

1. **Obtainium** — needs no permission from anyone. Users point it at
   `github.com/rsavutiu/OfflinePlaya` and get tracked updates straight from the
   releases you already publish. Worth a line in the README today.
2. **Accrescent** — curated, growing, and has its own criteria; check theirs
   before assuming.
3. **F-Droid proper** — builds from source, slower queue, and **check whether
   their current policy carries an equivalent AI clause before investing the
   effort**; do not assume it matches IzzyOnDroid's.
4. **The community posts still work, but carry the same disclosure duty.**
   r/fossdroid and r/degoogle are substantially the *same* audience as
   IzzyOnDroid and hold similar views on AI-written code — and anyone can read
   the `Co-Authored-By: Claude` trailers in the public history. Check each
   community's rules on AI content and self-promotion first, and disclose AI
   assistance in the post itself. Being caught not disclosing there does more
   damage than a lukewarm reception. r/headphones and Head-Fi care mainly about
   the USB-DAC story and are a softer audience for this.
