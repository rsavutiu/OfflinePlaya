# Second sweep: replace `<name> = emptyList()` parameter assignments with
# `<name> = persistentListOf()` across UI composables. The first sweep
# handled named `= listOf(...)` literals but missed the no-args
# `emptyList()` cases used in @Preview's "empty state" examples and
# default parameter values.

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..\shared\src\commonMain\kotlin\com\offlineplaya\shared\presentation\ui')).Path

# Match: <whitespace> name = emptyList()   — only when emptyList is bare-no-args.
$pattern = '(\W\w+\s*=\s*)emptyList\(\s*\)'

$files = Get-ChildItem $root -Recurse -Filter '*.kt'
foreach ($f in $files) {
    $src = Get-Content $f.FullName -Raw -Encoding UTF8
    $new = [regex]::Replace($src, $pattern, '${1}persistentListOf()')
    if ($new -ne $src) {
        if ($new -match 'persistentListOf\(' -and $new -notmatch 'import kotlinx\.collections\.immutable\.persistentListOf\b') {
            $lines = $new -split "`r?`n"
            $lastImport = -1
            for ($i = 0; $i -lt $lines.Length; $i++) {
                if ($lines[$i] -like 'import *') { $lastImport = $i }
            }
            if ($lastImport -ge 0) {
                $before = $lines[0..$lastImport] -join "`n"
                $after = if ($lastImport + 1 -le $lines.Length - 1) {
                    $lines[($lastImport + 1)..($lines.Length - 1)] -join "`n"
                } else { '' }
                $new = $before + "`nimport kotlinx.collections.immutable.persistentListOf`n" + $after
            }
        }
        [System.IO.File]::WriteAllText($f.FullName, $new, [System.Text.UTF8Encoding]::new($false))
        Write-Host ("updated " + $f.FullName.Substring($root.Length + 1))
    }
}
