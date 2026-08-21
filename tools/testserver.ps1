# Starts the mod on a headless Forge server so changes can be tested without
# launching the game.
#
#   .\tools\testserver.ps1            # start it (returns once it is accepting rcon)
#   .\tools\testserver.ps1 -Stop      # shut it down
#   .\tools\testserver.ps1 -Status
#
# Once it is up, drive it with:
#   node tools\rcon.js "/summon barbarajones:krave_monster ~ ~ ~"
#
# This runs the mod straight from sourceSets.main - no jar, no reobfuscation, no
# copying into a mods folder - so what it tests is exactly what was just
# compiled. It is a real dedicated server, so it answers everything that is
# decided server-side: crashes, registry failures, whether an attack actually
# removed the blocks it claimed to, and what it cost in tick time. It cannot
# answer anything about how something LOOKS. Nothing renders here.
param(
    [switch]$Stop,
    [switch]$Status,
    [int]$TimeoutSeconds = 300
)

$ErrorActionPreference = 'Stop'
$root    = Split-Path $PSScriptRoot -Parent
$runDir  = Join-Path $root 'run'
$logFile = Join-Path $runDir 'testserver.out'
$pidFile = Join-Path $runDir 'testserver.pid'

function Get-ServerProcess {
    if (-not (Test-Path $pidFile)) { return $null }
    $serverPid = Get-Content $pidFile | Select-Object -First 1
    try { return Get-Process -Id $serverPid -ErrorAction Stop } catch { return $null }
}

if ($Status) {
    $p = Get-ServerProcess
    if ($p) { Write-Host "running (pid $($p.Id))" -ForegroundColor Green }
    else    { Write-Host "not running" -ForegroundColor Yellow }
    return
}

if ($Stop) {
    $p = Get-ServerProcess
    if (-not $p) { Write-Host "not running"; return }
    # Ask through rcon first so the world saves; only kill it if that fails.
    try {
        & node (Join-Path $root 'tools\rcon.js') '/stop' | Out-Null
        $p.WaitForExit(20000) | Out-Null
    } catch {
        Write-Host "rcon stop failed, terminating" -ForegroundColor Yellow
    }
    if (-not $p.HasExited) { $p.Kill() }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
    Write-Host "stopped" -ForegroundColor Green
    return
}

if (Get-ServerProcess) { Write-Host "already running"; return }

New-Item -ItemType Directory -Force -Path $runDir | Out-Null

# ---------------------------------------------------------------------------
# server.properties
#
# Rewritten every start rather than edited, so a setting cannot drift and
# silently change what a test means. rcon is the whole point: without it there
# is no way to send this server a command, because a background process has no
# console to type into.
# ---------------------------------------------------------------------------
$rconPassword = 'barbarajones-test'
@"
# Written by tools/testserver.ps1. Edits here are overwritten on next start.
enable-rcon=true
rcon.port=25575
rcon.password=$rconPassword
broadcast-rcon-to-ops=false
online-mode=false
server-port=25599
# Nothing connects to this and nobody watches it, so everything that exists to
# be pleasant for players is turned off. view-distance in particular: the
# default keeps hundreds of chunks loaded and ticking, which on this machine is
# most of the memory budget spent on terrain no test looks at.
view-distance=6
simulation-distance=6
max-players=4
spawn-protection=0
sync-chunk-writes=false
level-name=testworld
motd=BarbaraJones test server
"@ | Set-Content -Path (Join-Path $runDir 'server.properties') -Encoding utf8

# ---------------------------------------------------------------------------
# The EULA is Mojang's agreement with the person running the server, so it is
# not mine to accept. The server writes eula.txt with eula=false on its first
# run and refuses to start until a human changes it.
# ---------------------------------------------------------------------------
$eula = Join-Path $runDir 'eula.txt'
if ((Test-Path $eula) -and -not ((Get-Content $eula -Raw) -match 'eula\s*=\s*true')) {
    Write-Host ""
    Write-Host "The Minecraft EULA has not been accepted for this test server." -ForegroundColor Yellow
    Write-Host "Read https://aka.ms/MinecraftEULA and, if you agree, set eula=true in:" -ForegroundColor Yellow
    Write-Host "  $eula" -ForegroundColor Yellow
    Write-Host ""
    throw "EULA not accepted - not starting."
}

$jdk = Join-Path $root '.tools\jdk17\jdk-17.0.20+8'
if (-not (Test-Path $jdk)) {
    $found = Get-ChildItem -Directory (Join-Path $root '.tools\jdk17') | Select-Object -First 1
    if ($found) { $jdk = $found.FullName } else { throw "JDK 17 missing under .tools\jdk17" }
}
$gradle = Join-Path $root '.tools\gradle\gradle-8.1.1\bin\gradle.bat'
if (-not (Test-Path $gradle)) { throw "Gradle 8.1.1 missing under .tools\gradle" }

$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
$env:GRADLE_OPTS = '-Xmx1000m -Dorg.gradle.daemon=false'
# Deliberately cleared. build.ps1 sets this to cap the reobfuscation child, and
# every JVM launched afterwards in the same shell inherits it - including the
# server, which cannot generate a world in 768m and dies with an error that
# blames anything but the heap.
Remove-Item Env:\JAVA_TOOL_OPTIONS -ErrorAction SilentlyContinue

Remove-Item $logFile -Force -ErrorAction SilentlyContinue

$proc = Start-Process -FilePath $gradle `
        -ArgumentList 'runServer', '--no-daemon' `
        -WorkingDirectory $root `
        -RedirectStandardOutput $logFile `
        -RedirectStandardError (Join-Path $runDir 'testserver.err') `
        -WindowStyle Hidden -PassThru
$proc.Id | Set-Content $pidFile

Write-Host "starting (pid $($proc.Id))... first run generates a world, give it a few minutes" -ForegroundColor DarkGray

# Wait for the line the server prints when it is actually accepting commands.
# Polling the log beats a fixed sleep: a first run generating a world takes
# minutes and a restart takes seconds, and a sleep long enough for the first is
# wasted on every one after it.
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
while ((Get-Date) -lt $deadline) {
    $text = if (Test-Path $logFile) { Get-Content $logFile -Raw -ErrorAction SilentlyContinue } else { '' }

    if ($text -match 'RCON running on') {
        Write-Host "up and accepting rcon on port 25575" -ForegroundColor Green
        Write-Host "  node tools\rcon.js `"/list`"" -ForegroundColor DarkGray
        return
    }

    # Checked BEFORE the generic exit branch. The server stops on its own when
    # the EULA is unaccepted, so ordering these the other way round buries a
    # one-line instruction under twenty-five lines of mixin debug output and
    # makes a perfectly normal first run look like a crash.
    if ($text -match 'You need to agree to the EULA') {
        Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
        Write-Host ""
        Write-Host "The server wrote run\eula.txt and stopped, which is the expected first run." -ForegroundColor Yellow
        Write-Host "The EULA is Mojang's agreement with you, so it is yours to accept." -ForegroundColor Yellow
        Write-Host "Read https://aka.ms/MinecraftEULA and, if you agree, set eula=true in:" -ForegroundColor Yellow
        Write-Host "  $eula" -ForegroundColor Yellow
        Write-Host "then run this script again." -ForegroundColor Yellow
        throw "EULA not accepted"
    }

    if ($proc.HasExited) {
        Write-Host ""
        Write-Host "Server exited during startup. Last lines:" -ForegroundColor Red
        if (Test-Path $logFile) { Get-Content $logFile -Tail 25 }
        Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
        throw "test server failed to start"
    }
    Start-Sleep -Seconds 3
}
throw "test server did not come up within $TimeoutSeconds seconds - see $logFile"
