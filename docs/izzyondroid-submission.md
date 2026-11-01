# IzzyOnDroid submission notes

Draft only — **nothing has been submitted**. Opening the request is a post made
in your name, so it's left for you to send.

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
