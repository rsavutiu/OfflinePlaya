# Releasing OfflinePlaya to Google Play

Releases are automated by [`.github/workflows/release.yml`](.github/workflows/release.yml).
Push an annotated tag and CI builds a signed AAB and ships it to the
**production** track.

```bash
git tag -a v0.1.2 -m "v0.1.2"
git push origin v0.1.2
```

- **versionName** comes from the tag (`v0.1.2` → `0.1.2`).
- **versionCode** is the commit count (`git rev-list --count HEAD`), which is
  monotonic and already past the published `2`. Nothing to bump by hand.
- Local/dev builds ignore all this and use the committed defaults in
  `androidApp/build.gradle.kts`.

## One-time setup

The workflow needs five **repository secrets**
(Settings → Secrets and variables → Actions → New repository secret).

### 1. Signing keystore (4 secrets)

The upload keystore is the same `.jks` referenced by your local
`androidApp/keystore.properties` (gitignored). Base64-encode it:

```bash
# from the repo root; path is the storeFile= value in keystore.properties
base64 -w0 /path/to/upload.jks > upload.jks.b64    # Linux
# Windows PowerShell:
# [Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\upload.jks")) | Set-Content upload.jks.b64
```

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | contents of `upload.jks.b64` |
| `KEYSTORE_PASSWORD` | `storePassword` from keystore.properties |
| `KEY_ALIAS` | `keyAlias` |
| `KEY_PASSWORD` | `keyPassword` (same as store password — PKCS12) |

Delete `upload.jks.b64` afterwards. **Never commit the keystore or the base64.**

### 2. Play Developer API service account (1 secret)

1. **Google Cloud Console** → create (or reuse) a project → *IAM & Admin →
   Service Accounts* → create one → *Keys* → *Add key → JSON*. Download it.
2. **Play Console** → *Users and permissions* → *Invite new users* → paste the
   service account email → grant **Release** permissions (at minimum: *Release
   to production, exclude devices, and use app signing* + *Manage testing
   tracks*) for this app → send.
3. (If not already on) enable the **Google Play Android Developer API** for the
   Cloud project.

Add the **entire JSON file contents** as secret `PLAY_SERVICE_ACCOUNT_JSON`.

## Notes & gotchas

- **First automated upload:** the app must already have had at least one
  *manual* upload in Play Console. OfflinePlaya is already published
  (versionCode 2), so this is satisfied.
- **Play app signing:** the keystore here is the *upload* key. Google re-signs
  with the app signing key on their side — correct and expected.
- **Staged rollout instead of full:** in `release.yml`, replace
  `status: completed` with `status: inProgress` and `userFraction: 0.2` to ship
  to 20 % first, then complete the rollout from Play Console.
- **Translations are machine-generated (Google MT).** 41 locales, validated for
  key/placeholder parity by `tools/validate_translations.py` (the workflow runs
  it as a gate). Plurals are `one`/`other` only — languages with richer CLDR
  categories fall back to `other`. Flag for native review before relying on
  them in marketing.
- **Roll back a bad release:** Play Console → Production → halt rollout /
  promote a previous release. CI can't un-publish.
- **Watch a run:** Actions tab → "Release to Play Store". The built `.aab` is
  also saved as a workflow artifact regardless of upload success.
