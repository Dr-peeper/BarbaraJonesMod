# Refuse to build an entity that nothing can draw. See tools/check_renderers.ps1.
& (Join-Path $PSScriptRoot "tools/check_renderers.ps1")
& (Join-Path $PSScriptRoot "tools/check_module_init.ps1")
& (Join-Path $PSScriptRoot "tools/check_creative_tabs.ps1")
# Builds the 1.20.1 mod. Uses the JDK 17 in .tools and a memory-capped Gradle,
# because this machine is tight on RAM.
#
#   .\build.ps1
#
# Output: build\libs\BarbaraJonesMod-<version>.jar
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

$jdk = Join-Path $root '.tools\jdk17\jdk-17.0.20+8'
if (-not (Test-Path $jdk)) {
    $found = Get-ChildItem -Directory (Join-Path $root '.tools\jdk17') | Select-Object -First 1
    if ($found) { $jdk = $found.FullName } else { throw "JDK 17 missing under .tools\jdk17" }
}

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
$env:GRADLE_OPTS = '-Xmx1200m -Dorg.gradle.daemon=false'
# reobfJar shells out to the Forge renamer as a SEPARATE java process, which does
# not inherit GRADLE_OPTS - it takes the JVM default heap of a quarter of RAM, and
# on this machine Windows cannot commit that much ("The paging file is too small
# for this operation to complete"). JAVA_TOOL_OPTIONS is read by every JVM that
# starts, so it caps the child too. Gradle keeps its bigger heap because its own
# explicit -Xmx is applied after this one.
$env:JAVA_TOOL_OPTIONS = '-Xmx768m'

Set-Location $root
$gradle = Join-Path $root '.tools\gradle\gradle-8.1.1\bin\gradle.bat'
if (-not (Test-Path $gradle)) { throw "Gradle 8.1.1 missing under .tools\gradle" }

# Wipe the jar outputs before building.
#
# This is the root cause of the partially-reobfuscated jar that shipped: a
# build interrupted during reobfJar leaves a half-rewritten jar in build/libs,
# and the next build happily treats that as its starting point and rewrites
# only part of it again. Reobfuscation is not idempotent over its own output.
# Deleting first means every jar is produced whole, in one run, or not at all.
#
# The paths below lost their backslashes when this block was first written -
# "buildlibs*.jar", and a literal carriage return inside "buildeobfJar" - so
# for several builds it deleted nothing whatsoever. The protection read as
# present while doing none of the work, which is exactly how the
# half-reobfuscated jar reached the mods folder and crashed the game.
# Single-quoted now, so nothing can eat them again.
Remove-Item (Join-Path $root 'build\libs\*.jar') -Force -ErrorAction SilentlyContinue
Remove-Item (Join-Path $root 'build\reobfJar') -Recurse -Force -ErrorAction SilentlyContinue

& $gradle build --no-daemon --stacktrace
if ($LASTEXITCODE -ne 0) { throw "Gradle build FAILED (exit $LASTEXITCODE) - do not ship this jar." }

# ---------------------------------------------------------------------------
# Verify the jar is actually REOBFUSCATED before anyone installs it.
#
# We compile against Mojang names ('nutrition'); Forge runs on SRG names
# ('m_38760_'). The reobfJar task rewrites them. If that task fails or is
# skipped, build\libs still holds a Mojang-named jar that LOOKS fine and dies
# at mod construction with NoSuchMethodError. This has happened - never again.
# ---------------------------------------------------------------------------
Add-Type -AssemblyName System.IO.Compression.FileSystem

$jarPath = Get-ChildItem (Join-Path $root 'build\libs\*.jar') | Sort-Object LastWriteTime -Desc | Select-Object -First 1
if (-not $jarPath) { throw "No jar produced in build\libs." }

function Get-SrgCount([string]$path) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($path)
    $srg = 0
    foreach ($entry in $zip.Entries) {
        if ($entry.FullName -like 'com/barbarajones/*.class') {
            $mem = New-Object System.IO.MemoryStream
            $stream = $entry.Open(); $stream.CopyTo($mem); $stream.Close()
            $txt = [System.Text.Encoding]::ASCII.GetString($mem.ToArray())
            $srg += ([regex]::Matches($txt, 'm_\d+_')).Count
            $mem.Close()
        }
    }
    $zip.Dispose()
    return $srg
}

$srgCount = Get-SrgCount $jarPath.FullName

if ($srgCount -lt 100) {
    # The reobf output is the real artifact - recover it rather than shipping junk.
    $reobf = Join-Path $root 'build\reobfJar\output.jar'
    if ((Test-Path $reobf) -and ((Get-SrgCount $reobf) -ge 100)) {
        Copy-Item $reobf $jarPath.FullName -Force
        $srgCount = Get-SrgCount $jarPath.FullName
        Write-Host "NOTE: build\libs jar was unmapped; restored from reobfJar output." -ForegroundColor Yellow
    }
}

Write-Host ""
if ($srgCount -lt 100) {
    Write-Host "JAR IS NOT REOBFUSCATED ($srgCount SRG refs) - IT WILL NOT LOAD." -ForegroundColor Red
    throw "Refusing to report success: $($jarPath.Name) is unmapped."
}
Write-Host "Reobfuscation verified: $srgCount SRG references." -ForegroundColor Green
Get-ChildItem (Join-Path $root 'build\libs') -ErrorAction SilentlyContinue

# ---------------------------------------------------------------------------
# Install into the game's mods folder.
#
# Deliberately AFTER the reobfuscation gate above, so a jar that would not load
# never reaches the folder - the whole point of that gate is that an unmapped
# jar looks perfectly fine sitting on disk and dies on startup.
#
# The copy itself lives in tools/install_mod.ps1 so it can be re-run without a
# rebuild: it refuses while the game holds the file handle, and charging a full
# rebuild for a file copy is a poor trade.
# ---------------------------------------------------------------------------
& (Join-Path $PSScriptRoot 'tools/install_mod.ps1')
