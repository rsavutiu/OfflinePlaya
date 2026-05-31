---
name: run-offlineplaya
description: Build, install, launch, screenshot, and tail logs of the OfflinePlaya Android app on a connected device or running emulator. Use when asked to run / start / deploy / screenshot / test / verify the app, or to look at it on the phone after a change.
---

OfflinePlaya is an Android-only offline music player ([CLAUDE.md](../../../CLAUDE.md)). Driven from
this Windows host with `gradlew.bat` + `adb`. There is no emulator workflow committed — drive
whatever physical device is attached. All paths below are **relative to the repo root**.

## The driver

`.claude/skills/run-offlineplaya/driver.ps1` wraps install, launch, screenshot, logcat, and
force-stop. Invoke it through `powershell.exe` (Windows PowerShell 5.1 — not `pwsh`).

```powershell
powershell -File .claude\skills\run-offlineplaya\driver.ps1 <command> [<arg>] [-Serial <id>] [-Seconds <n>] [-WaitMs <n>]
```

Commands: `devices`, `install`, `launch`, `screenshot [name]`, `logcat`, `stop`, `uninstall`, `run`.

`run` is the one-shot "deploy + look at it" loop — `installDebug` → wake screen → `am start` → wait
4s → pull a PNG into `.claude/skills/run-offlineplaya/screenshots/`. That's what you want after
editing UI code.

## Prerequisites

- A physical Android device in USB-debugging mode, **or** a running emulator. Verify with:
  ```powershell
  powershell -File .claude\skills\run-offlineplaya\driver.ps1 devices
  ```
  Last run showed `ZY22KMNN65 device product:cancunf_g_sysenq model:moto_g54_5G` — i.e. a moto g54
  5G on Android 15.
- Android SDK platform-tools. Driver auto-detects
  `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`; override with `$env:ANDROID_SDK_ROOT` if
  installed elsewhere.
- JDK that `gradlew.bat` can find — on this machine it's
  `C:\Program Files\Android\Android Studio\jbr` (printed by gradle's startup).

No SDK install steps documented here: if Android Studio runs the project, the driver runs.

## Run (agent path) — deploy + screenshot

```powershell
powershell -File .claude\skills\run-offlineplaya\driver.ps1 run home
```

That writes `.claude\skills\run-offlineplaya\screenshots\home.png`. Read it back with the Read tool
to verify the change visually.

If you only need to re-screenshot a state the app is already in:

```powershell
powershell -File .claude\skills\run-offlineplaya\driver.ps1 screenshot <name>
```

To restart the app (force-stop + relaunch, without rebuilding):

```powershell
powershell -File .claude\skills\run-offlineplaya\driver.ps1 stop
powershell -File .claude\skills\run-offlineplaya\driver.ps1 launch
powershell -File .claude\skills\run-offlineplaya\driver.ps1 screenshot fresh
```

## Watching logs

```powershell
powershell -File .claude\skills\run-offlineplaya\driver.ps1 logcat -Seconds 10
```

Tails for 10s then exits. Filtered to the tags this codebase actually emits (`LibrarySyncUseCase`,
`LibrarySyncCoordinator`, `PlaybackService`, `OfflinePlaya`) plus `AndroidRuntime:E` for crashes;
everything else is silenced. Bump `-Seconds` for slower flows like a full library re-scan.

## Run (human path)

Open the project in Android Studio and hit Run. The UX is the same as the driver's `install` +
`launch`; only useful when you want the IDE's debugger or the Logcat panel's interactive filters.

## Tests

Unit tests are JVM, no device needed:

```powershell
.\gradlew.bat :shared:testDebugUnitTest
```

Compile-only sanity check (used by CLAUDE.md as the "full check"):

```powershell
.\gradlew.bat :shared:testDebugUnitTest :androidApp:compileDebugKotlin
```

No instrumented (`connectedAndroidTest`) suite exists in this repo.

## Gotchas

- **PowerShell `>` corrupts PNGs.** `adb exec-out screencap -p > file.png` produces a
  UTF-16-encoded "PNG" with header `FF FE FD FF ...` that no image viewer will open. The driver
  always goes through `adb shell screencap -p /sdcard/...` + `adb pull` for that reason. If you
  screencap by hand, do the same.
- **Screen must be awake before `screencap`.** A locked or dozed phone returns a fully-black PNG —
  looks like the app crashed but didn't. `launch` and `run` send `KEYCODE_WAKEUP` first; if you call
  `screenshot` standalone on a sleeping device you'll get black. Wake it (
  `adb shell input keyevent KEYCODE_WAKEUP`) or unlock first.
- **`pwsh` is not on this machine.** Only Windows PowerShell 5.1 (`powershell.exe`). The driver uses
  `param()` + `switch` so it's 5.1-compatible; if you add features, don't reach for ternary (`?:`)
  or null-coalescing (`??`).
- **First-run permission gate.** On a fresh install the
  app's [MainActivity.kt:118](../../../androidApp/src/main/java/com/offlineplaya/android/MainActivity.kt:118)
  blocks the UI behind a `PermissionRequiredScreen` until `MANAGE_EXTERNAL_STORAGE` is granted. The
  driver can't grant that — tap through the system settings screen on the device once, then re-
  `launch`. Subsequent runs skip it.
- **First frame can take longer than 4s on a cold start.** The default `-WaitMs 4000` is fine after
  the app's been launched once today; on a real cold boot, pass `-WaitMs 8000` or screenshot will
  catch the splash.

## Troubleshooting

| Symptom                                                        | Fix                                                                                                          |
|----------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| `adb not found`                                                | Set `$env:ANDROID_SDK_ROOT` to your SDK root (the one containing `platform-tools\`).                         |
| `devices` lists nothing                                        | USB-debugging off, cable in a charge-only port, or RSA prompt unanswered on the phone. Reconnect and unlock. |
| Screenshot is solid black                                      | Screen was off when capture fired. Re-run `launch` (it now wakes first) or pass `-WaitMs 8000`.              |
| Screenshot is a system dialog, not the app                     | Permission gate (see Gotchas). Tap through it on the device once.                                            |
| `installDebug` fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Signing key mismatch with a previously sideloaded build. `driver.ps1 uninstall` then re-`install`.           |
