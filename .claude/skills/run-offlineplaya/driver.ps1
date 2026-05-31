# OfflinePlaya driver.
# Build / install / launch / screenshot / logcat / uninstall against a
# connected Android device or running emulator. All commands operate on
# the first device returned by `adb devices`; pass -Serial to target a
# specific one when multiple are attached.
#
# Usage (from repo root):
#   pwsh -File .claude/skills/run-offlineplaya/driver.ps1 install
#   pwsh -File .claude/skills/run-offlineplaya/driver.ps1 launch
#   pwsh -File .claude/skills/run-offlineplaya/driver.ps1 screenshot home
#   pwsh -File .claude/skills/run-offlineplaya/driver.ps1 logcat -Seconds 10
#   pwsh -File .claude/skills/run-offlineplaya/driver.ps1 stop
#
# `run` is install + launch + screenshot in one shot — the common
# "deploy a change and look at it" loop.

param(
    [Parameter(Position = 0, Mandatory = $true)]
    [ValidateSet('devices', 'install', 'launch', 'screenshot', 'logcat',
                 'stop', 'uninstall', 'run')]
    [string]$Command,

    [Parameter(Position = 1)]
    [string]$Arg,

    [string]$Serial,
    [int]$Seconds = 5,
    [int]$WaitMs = 4000
)

$ErrorActionPreference = 'Stop'

$Pkg = 'com.offlineplaya.android'
$Activity = "$Pkg/.MainActivity"
$RepoRoot = (Resolve-Path "$PSScriptRoot\..\..\..").Path
$ShotDir = Join-Path $PSScriptRoot 'screenshots'

# adb lives at a predictable path on this machine. If you run on a fresh
# install where it isn't there, point at it via $env:ANDROID_SDK_ROOT.
$Adb = if ($env:ANDROID_SDK_ROOT) {
    Join-Path $env:ANDROID_SDK_ROOT 'platform-tools\adb.exe'
} else {
    Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
}
if (-not (Test-Path $Adb)) {
    $found = (Get-Command adb -ErrorAction SilentlyContinue).Source
    if ($found) { $Adb = $found } else { throw "adb not found. Install Android SDK platform-tools or set ANDROID_SDK_ROOT." }
}

function Invoke-Adb {
    if ($Serial) { & $Adb -s $Serial @args } else { & $Adb @args }
}

function Invoke-Gradle {
    Push-Location $RepoRoot
    try { & "$RepoRoot\gradlew.bat" @args } finally { Pop-Location }
    if ($LASTEXITCODE -ne 0) { throw "gradle failed: $args" }
}

switch ($Command) {
    'devices' {
        Invoke-Adb devices -l
    }
    'install' {
        # `installDebug` builds + installs in one step; faster than
        # `assembleDebug` + manual `adb install` because Gradle reuses
        # the dex cache.
        Invoke-Gradle ':androidApp:installDebug'
    }
    'launch' {
        # Wake the screen first — on a locked/dozed phone `am start`
        # fires but `screencap` returns a black frame. KEYCODE_WAKEUP
        # is a no-op if already awake; safe to send unconditionally.
        Invoke-Adb shell input keyevent KEYCODE_WAKEUP | Out-Null
        Invoke-Adb shell am start -n $Activity | Out-Null
        Start-Sleep -Milliseconds $WaitMs
        Write-Host "Launched $Activity (waited ${WaitMs}ms for first frame)"
    }
    'screenshot' {
        # Capture on-device then pull. Going through `adb exec-out
        # screencap -p > file` corrupts the PNG in PowerShell because
        # `>` uses UTF-16 LE encoding — `adb pull` is binary-safe.
        $name = if ($Arg) { $Arg } else { 'shot' }
        if (-not $name.EndsWith('.png')) { $name = "$name.png" }
        $local = Join-Path $ShotDir $name
        New-Item -ItemType Directory -Force -Path $ShotDir | Out-Null
        $remote = '/sdcard/op_driver_shot.png'
        Invoke-Adb shell screencap -p $remote | Out-Null
        Invoke-Adb pull $remote $local | Out-Null
        Invoke-Adb shell rm $remote | Out-Null
        Write-Host "Saved $local"
    }
    'logcat' {
        # Tail only this app's logs (Kermit-tagged) plus AndroidRuntime
        # crashes. -d would dump-and-exit; we want a bounded live tail
        # so the driver doesn't hang in CI.
        Invoke-Adb logcat -c
        $job = Start-Job -ScriptBlock {
            param($adb, $serial)
            $args = @('logcat', '-v', 'brief', 'AndroidRuntime:E',
                      'LibrarySyncUseCase:V', 'LibrarySyncCoordinator:V',
                      'PlaybackService:V', 'OfflinePlaya:V', '*:S')
            if ($serial) { & $adb -s $serial @args } else { & $adb @args }
        } -ArgumentList $Adb, $Serial
        Start-Sleep -Seconds $Seconds
        Stop-Job $job | Out-Null
        Receive-Job $job
        Remove-Job $job | Out-Null
    }
    'stop' {
        Invoke-Adb shell am force-stop $Pkg | Out-Null
        Write-Host "Force-stopped $Pkg"
    }
    'uninstall' {
        Invoke-Adb uninstall $Pkg
    }
    'run' {
        Invoke-Gradle ':androidApp:installDebug'
        Invoke-Adb shell input keyevent KEYCODE_WAKEUP | Out-Null
        Invoke-Adb shell am start -n $Activity | Out-Null
        Start-Sleep -Milliseconds $WaitMs
        $name = if ($Arg) { $Arg } else { 'run' }
        if (-not $name.EndsWith('.png')) { $name = "$name.png" }
        $local = Join-Path $ShotDir $name
        New-Item -ItemType Directory -Force -Path $ShotDir | Out-Null
        $remote = '/sdcard/op_driver_shot.png'
        Invoke-Adb shell screencap -p $remote | Out-Null
        Invoke-Adb pull $remote $local | Out-Null
        Invoke-Adb shell rm $remote | Out-Null
        Write-Host "Installed, launched, captured $local"
    }
}
