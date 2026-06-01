# One-shot sweep: rewrite `name: List<X>` parameter declarations to
# `name: PersistentList<X>` across the UI module, and ensure the
# kotlinx.collections.immutable imports are present in every modified
# file. The regex deliberately matches the *parameter* shape (newline or
# comma, then identifier, then colon) so it leaves return types, local
# `val` lines, and lambda return-types alone.
#
# Run once, then verify with `gradlew :shared:compileDebugKotlinAndroid`.

$ErrorActionPreference = 'Stop'
$root = Join-Path $PSScriptRoot '..\shared\src\commonMain\kotlin\com\offlineplaya\shared\presentation\ui'
$root = (Resolve-Path $root).Path

# Identifier + ':' + List< ... > followed by '=', ',', or ')' — i.e. param shape.
$paramPattern = '((?:^|,|\r?\n)\s+\w+\s*:\s*)List<([A-Za-z][A-Za-z0-9_<>?, ]*)>(\s*(?:=|,|\)))'

# Inline `listOf(` literals inside @Preview blocks become `persistentListOf(`
# only when they appear as a parameter value (assigned with `=`). Bare
# `listOf` inside function bodies stays unchanged.
$listOfPattern = '(\w+\s*=\s*)listOf\('

$files = Get-ChildItem $root -Recurse -Filter '*.kt'
foreach ($f in $files) {
    $src = Get-Content $f.FullName -Raw -Encoding UTF8
    $new = [regex]::Replace($src, $paramPattern, '${1}PersistentList<${2}>${3}')
    $new = [regex]::Replace($new, $listOfPattern, '${1}persistentListOf(')

    if ($new -ne $src) {
        # Ensure imports are present. Insert after the last existing import line.
        $needed = @()
        if ($new -match 'PersistentList<' -and $new -notmatch 'import kotlinx\.collections\.immutable\.PersistentList\b') {
            $needed += 'import kotlinx.collections.immutable.PersistentList'
        }
        if ($new -match 'persistentListOf\(' -and $new -notmatch 'import kotlinx\.collections\.immutable\.persistentListOf\b') {
            $needed += 'import kotlinx.collections.immutable.persistentListOf'
        }
        if ($new -match 'toPersistentList\(' -and $new -notmatch 'import kotlinx\.collections\.immutable\.toPersistentList\b') {
            $needed += 'import kotlinx.collections.immutable.toPersistentList'
        }
        if ($needed.Count -gt 0) {
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
                $new = $before + "`n" + ($needed -join "`n") + "`n" + $after
            }
        }
        # Set-Content with UTF8 would write a BOM; use .NET to write without.
        [System.IO.File]::WriteAllText($f.FullName, $new, [System.Text.UTF8Encoding]::new($false))
        Write-Host ("updated " + $f.FullName.Substring($root.Length + 1))
    }
}
