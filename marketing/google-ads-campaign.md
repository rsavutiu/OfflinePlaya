# Google Ads — App campaign setup

Everything needed to launch a paid install campaign for OfflinePlaya. Assets are
in `marketing/ads/`.

## Read this first

For an Android app the campaign type is an **App campaign** (Google Ads →
New campaign → App promotion → App installs). It does **not** take finished
ads. You supply *assets* — headlines, descriptions, images, a video — and Google
assembles and tests combinations across Search, Play, YouTube, Discover and the
Display network. There is **no keyword targeting** and no manual ad design, so
the asset quality and the store listing do all the work.

**Prerequisite:** the Play account must be linked to the Google Ads account
(Tools → Data manager → Google Play). Without the link the campaign cannot
optimise for installs.

## ⚠️ Two things to fix before spending money

1. **The Play listing is stale.** As of 2026-11-04 the store page still served
   the October copy and the old trailer — the 11-01 localized listings and demo
   video were not live. An App campaign sends every click to that page. Paying
   for traffic into an outdated listing is the most expensive way to find out it
   didn't go live. Confirm it in Console first.
2. **Never upload the existing store screenshots as ad images.** Every one of
   them shows real copyrighted album art (Charli XCX, Gorillaz, Phil Collins —
   one of them with an Apple Music badge, one with a suggestive cover). That is
   third-party IP in a paid ad, plus a Google Ads content-policy risk. The
   images in `marketing/ads/` are built from the Equalizer screen, which is the
   only one with no cover art in frame.

## Text assets

Google allows up to 5 headlines (30 characters) and 5 descriptions (90
characters). Counts verified; each is within limit.

### Headlines (pick 5)

| Chars | Text |
|---|---|
| 20 | `Offline Music Player` |
| 22 | `Your Music, No Account` |
| 23 | `MP3 & FLAC Music Player` |
| 22 | `Play the Music You Own` |
| 26 | `No Ads. No Cloud. Offline.` |
| 25 | `Equalizer + Gapless Audio` |
| 29 | `Offline Player for Your Files` |

### Descriptions (use all 5)

| Chars | Text |
|---|---|
| 85 | `Plays the MP3 and FLAC files already on your phone. No streaming, no account, no ads.` |
| 84 | `10-band equalizer, gapless playback and synced lyrics for your own music collection.` |
| 82 | `Browse by artist, album or folder. Fix tags. Works offline, even in airplane mode.` |
| 85 | `Stream to your PC over WiFi and play through your real speakers. Nothing leaves home.` |
| 79 | `Built for ripped CDs, Bandcamp downloads and DJ sets, not another subscription.` |

Avoid superlatives ("best", "#1") — unverifiable claims get assets disapproved.
Nothing above makes a performance or ranking claim.

## Image assets — in `marketing/ads/`

| File | Size | Ratio | Slot |
|---|---|---|---|
| `ad_landscape_1200x628.png` | 1200×628 | 1.91:1 | landscape |
| `ad_square_1200x1200.png` | 1200×1200 | 1:1 | square |
| `ad_portrait_1200x1500.png` | 1200×1500 | 4:5 | portrait |

Upload all three — App campaigns need each ratio to fill every placement, and
missing a ratio silently cuts off inventory.

Built from `store_screenshots/08_equalizer.png` cropped above the mini-player,
on the app's dark violet with the Walkman-orange accent. Regenerate with
ImageMagick; the commands are in the commit that added them.

## Video asset

Use the demo: <https://youtu.be/iyMItL6WTzU> (3:09). Paste the YouTube URL into
the campaign's video assets. App campaigns strongly favour having a video — if
none is supplied Google auto-generates one from the store listing, which is
worse than the real demo.

Worth adding later: a **portrait (9:16) cut under 30 seconds** for YouTube
Shorts inventory, which is where most cheap app-install volume sits. The 3:09
landscape demo will not serve there.

## Targeting and budget

- **Locations.** Don't run all 178 countries. Start with the markets whose
  listing language is hand-written and live, so the click lands on real copy —
  de-DE, en-US/en-GB, fr-FR, it-IT, es-ES, nl-NL, pl-PL. Tier-1 countries cost
  more per install but convert; broad low-cost geography buys installs that
  never open the app twice.
- **Languages.** Match the locations.
- **Bidding.** "Target cost per install" is the simplest. Set a target you'd
  actually accept — for a free utility, somewhere around €0.30–€1.00 is a
  realistic range, but treat the first week's actual CPI as the real number.
- **Budget.** Google wants daily budget ≳ 10× target CPI or the campaign starves
  and learns nothing. At €0.50 target CPI that's ~€5/day. **Run one campaign for
  at least 2 weeks without touching it** — editing assets or bids resets the
  learning phase.

## Honest expectations

The app is free with no IAP and no ads, so there is no revenue to recover: every
euro spent is spent, not invested. €100 at a €0.50 CPI is roughly 200 installs.
That is worth doing to break out of a cold start — ranking signal, review volume,
a base of real users to get feedback from — but it is not a growth engine, and
it stops the moment the budget stops.

The free channels in `launch-posts.md` cost nothing and reach the exact people
who want a local-files player. Worth running those first or alongside, so the
paid installs are not the only thing happening.

## Measuring it

In-app conversions need the Google Analytics for Firebase SDK, which this app
does not have and arguably should not (it has no tracking, and that is a selling
point). Without it, optimise on the Play install conversion, which the Play
link provides on its own. Judge the campaign on installs and cost per install,
plus Play Console's retention figures — not on anything in-app.
