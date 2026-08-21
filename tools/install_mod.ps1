# Copies the freshly built jar into the game's mods folder.
#
# This lives apart from build.ps1 so it can be re-run on its own:
#
#   .\tools\install_mod.ps1
#
# The install refuses while the game is open, and a build takes half a minute,
# so telling someone to "build again" after closing Minecraft charges them a
# full rebuild for what is a file copy. build.ps1 calls this at the end; run it
# directly to install a jar that is already built.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root    = Split-Path $PSScriptRoot -Parent
$modsDir = 'C:\Users\ADMIN\AppData\Roaming\.tlauncher\legacy\Minecraft\game\mods'

$jarPath = Get-ChildItem (Join-Path $root 'build\libs\*.jar') -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Desc | Select-Object -First 1
if (-not $jarPath) { throw "No jar in build\libs - build first." }

# A jar that skipped reobfuscation loads and then fails at the first vanilla
# call, which reads as a mod bug rather than a build one. Check before copying.
function Get-SrgCount([string]$path) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($path)
    $srg = 0
    foreach ($entry in $zip.Entries) {
        if ($entry.FullName -like 'com/barbarajones/*.class') {
            $mem = New-Object System.IO.MemoryStream
            $stream = $entry.Open(); $stream.CopyTo($mem); $stream.Close()
            $srg += ([regex]::Matches([System.Text.Encoding]::ASCII.GetString($mem.ToArray()), 'm_\d+_')).Count
            $mem.Close()
        }
    }
    $zip.Dispose()
    return $srg
}

if (-not (Test-Path $modsDir)) {
    Write-Host "Mods folder not found, skipping install: $modsDir" -ForegroundColor Yellow
    return
}

# Do not fight a running game for the file handle - Windows will either fail the
# copy or, worse, leave a half-written jar. The launcher itself also shows up as
# javaw, so match on the game's own java runtime rather than on memory: the
# launcher sits at a 256m heap but can still cross a memory threshold.
$running = Get-CimInstance Win32_Process -Filter "Name='javaw.exe'" -ErrorAction SilentlyContinue |
        Where-Object { $_.CommandLine -match 'java-runtime|net\.minecraft\.client' }
if ($running) {
    Write-Host ""
    Write-Host "Minecraft is running - NOT installing." -ForegroundColor Yellow
    Write-Host "  Close the game, then run: .\tools\install_mod.ps1" -ForegroundColor Yellow
    return
}

# Two versions in the folder at once is a duplicate-mod-id crash before the
# title screen, and a version bump renames the file, so a plain copy leaves both
# behind. That failure looks nothing like its cause.
$stale = Get-ChildItem $modsDir -Filter 'BarbaraJonesMod-*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -ne $jarPath.Name }
foreach ($old in $stale) {
    Remove-Item $old.FullName -Force
    Write-Host "  removed old $($old.Name)" -ForegroundColor DarkGray
}

Copy-Item $jarPath.FullName (Join-Path $modsDir $jarPath.Name) -Force

# Verify the INSTALLED copy, not the one just built: a truncated or locked write
# is exactly the case this exists to catch.
$installed = Join-Path $modsDir $jarPath.Name
$sameSize  = (Get-Item $installed).Length -eq $jarPath.Length
$srg       = Get-SrgCount $installed
if (-not $sameSize -or $srg -lt 100) {
    throw "Install verification FAILED for $installed (size match: $sameSize, SRG refs: $srg)."
}
Write-Host ""
Write-Host "Installed to mods folder: $($jarPath.Name)" -ForegroundColor Green
Write-Host "  $modsDir" -ForegroundColor DarkGray
