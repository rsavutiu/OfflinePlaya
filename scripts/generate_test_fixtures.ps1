# Generate small silent audio fixtures used by instrumentation tests.
# Run once after cloning, or whenever you want to refresh the fixtures.
# Requires ffmpeg in PATH (e.g. `choco install ffmpeg`).
#
# Why silence: zero copyright risk, byte-deterministic, tiny on disk.
# Why pre-generated: Android's platform doesn't ship an MP3 encoder, so
# instrumentation tests can't synthesize MP3s at runtime — and shipping
# native FFmpeg in the test APK would add ~5–10 MB for negligible benefit.
#
# Drops the fixtures into
#   androidApp/src/androidTest/assets/fixtures/
# from where the test harness copies them into the test cache dir before
# pointing scanners and tag writers at them.

$ErrorActionPreference = 'Stop'

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$out = Join-Path $repoRoot 'androidApp\src\androidTest\assets\fixtures'
New-Item -ItemType Directory -Force -Path $out | Out-Null

$ffmpeg = (Get-Command ffmpeg -ErrorAction Stop).Source

function Encode {
    param([string]$Name, [string[]]$Args)
    $dest = Join-Path $out $Name
    if (Test-Path $dest) { Remove-Item $dest -Force }
    $common = @('-hide_banner', '-loglevel', 'error',
                '-f', 'lavfi', '-i', 'anullsrc=r=44100:cl=stereo', '-t', '1')
    & $ffmpeg @common @Args $dest
    if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed for $Name" }
    $sz = (Get-Item $dest).Length
    "  {0,-22} {1,8} bytes" -f $Name, $sz
}

"Generating fixtures into $out"
Encode 'silence-1s.mp3'  @('-codec:a', 'libmp3lame', '-b:a', '128k')
Encode 'silence-1s.flac' @('-codec:a', 'flac', '-compression_level', '5')
Encode 'silence-1s.m4a'  @('-codec:a', 'aac', '-b:a', '64k')
Encode 'silence-1s.ogg'  @('-codec:a', 'libvorbis', '-q:a', '3')
Encode 'silence-1s.opus' @('-codec:a', 'libopus', '-b:a', '32k')
Encode 'silence-1s.wav'  @('-codec:a', 'pcm_s16le')
"Done."
