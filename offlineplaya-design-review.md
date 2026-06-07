# OfflinePlaya — Design Review & Fix List

A prioritized, implementation-ready review of the current UI (Android/Compose, KMP).
Work top-down: **P0 first**. Each item says *what*, *why*, and *where to look*.

> **How to use with Claude Code:** Drop this file in your repo root. Then run something like:
> `claude` → "Read offlineplaya-design-review.md. Start with the P0 section. Do ONE task at a time, show me the diff, and wait for my OK before the next." Tackling them in small, reviewable chunks beats asking it to do all 20 at once.

---

## P0 — Brand & legibility (do these first)

- [ ] **Restore the brand accent (Walkman orange).** Album-art Palette has eaten the brand — there is no fixed identity and zero orange. Decouple the two roles:
  - **Ambient tint** (from Palette / album art): backgrounds, Now Playing world, card washes.
  - **Brand accent** (FIXED): primary buttons, active tab underline, FAB, toggle "on" state, seek-fill. Use Walkman **orange** here so it pops against the cool tints.
  - *Where:* your theme/color layer — likely a `Color.kt` / `Theme.kt` and wherever the Palette result is applied. Add a `brandAccent` token that is NOT overwritten by the dynamic color pipeline.

- [ ] **Fix low-contrast secondary text app-wide.** Many sub-labels fail WCAG AA on near-black. Raise secondary text to ~70% opacity, tertiary to ~55%.
  - *Worst offenders:* Home card subtitles ("50 albums", "0 playlists"), All-tracks artist sub-lines, Settings descriptions.
  - *Where:* central text-color tokens (`onSurfaceVariant` / custom `textSecondary`). Fix the token, not each call site.

- [ ] **Remove the repeated album thumbnail on the Browse cards (Home).** The same "Phil Collins Essentials" cover floats in the top-right of all four tiles (All Tracks, Albums, Artists, Playlists) — reads as a bug. Either remove it or show each card's own relevant recent item.
  - *Where:* the Browse grid Composable / its card component.

- [ ] **String-escaping bug (Settings).** The empty-library hint renders literal backslashes: `Tap \"Add music folder\" above`. Fix the escaping in the string resource.
  - *Where:* `strings.xml` / KMP string resource for that empty state.

## P1 — Component & consistency fixes

- [ ] **Fix the Now Playing seek handle.** It currently looks like two overlapping elements (a pill + a separate vertical bar). Make it a single clear thumb.
  - *Where:* the Now Playing slider/seekbar Composable.

- [ ] **Normalize artist-name casing.** "Charli Xcx" (All tracks / Now Playing) vs "Charli XCX" (Artists). Pick the canonical form and apply everywhere — ideally at the data/format layer, not per screen.

- [ ] **Reconsider the leading number on All-tracks rows.** "184 · Fiction Factory" — an in-album track number on a global alphabetical list reads as noise. Hide it here, or replace with year/album.

- [ ] **Two competing primary buttons (Settings).** "Start metadata burn" and "Add music folder" are both full-width filled. One primary per section: make "Add music folder" the filled one; demote "Start metadata burn" to tonal/outlined (it's the heavier/destructive action).

- [ ] **Pluralization.** Home stat reads "1 Folders" → "1 Folder". Use a plurals resource.

- [ ] **Tab label casing.** "All tracks" (tab) vs "All Tracks" (Home card). Standardize.

## P2 — Polish & layout

- [ ] **Artist detail empty space.** Large black void below a single album. Surface top tracks / a tracklist, or fill the space meaningfully.
- [ ] **Lyrics top clip.** Top line clips mid-sentence at the header edge — add a top fade/scrim to match the bottom.
- [ ] **Now Playing vertical balance.** Tighten the gap between album art and title; the art feels stranded.
- [ ] **Artist hero crop.** Header image can decapitate faces — add a gradient scrim and a safer crop anchor.
- [ ] **Remove redundant counts.** Artist detail repeats "1 album · 22 tracks".

## Platform note (KMP / iOS)
The UI is heavily **Material** (FAB, switch styling, radio rows, segmented control). Correct for Android-first, but when you build the iOS surface, swap the FAB, toggle visuals, and nav bar to **HIG** equivalents or it'll feel like an Android app on iPhone.

---

### Suggested commit slicing
1. `theme: introduce fixed brandAccent (orange) separate from dynamic Palette tint`
2. `a11y: raise secondary/tertiary text contrast tokens`
3. `home: fix Browse card thumbnail + folder pluralization`
4. `fix: settings string escaping + button hierarchy`
5. `nowplaying: rebuild seek handle`
6. `data: normalize artist name casing`
