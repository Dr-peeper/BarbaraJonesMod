# make_horror.ps1 - re-horrify every sound in the mod.
#
# The other tools/make_*.ps1 scripts generate CLEAN source audio. This one is the
# POST-PROCESS pass: it pitches every clip down, slows it, distorts it and drowns
# it in reverb so the whole mod sounds wrong. Run it LAST, after any regeneration,
# or the clean sounds come back.
#
# Two intensities, chosen by filename:
#   * gentle  - idle barks and the va_* dialogue: still intelligible, just sinister
#               and comical (pitch/slow 0.85, light crush + short verb).
#   * heavy   - everything else (krave_*, deaths, hurts, the alarm): deep, slow,
#               bit-crushed, cavernous, wobbling (pitch/slow 0.70).
#
# Positional audio must stay MONO, so every output is forced to one channel.
#
# Usage:
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\make_horror.ps1
#   (optionally: -Ffmpeg C:\path\to\ffmpeg.exe)

param(
    [string]$Ffmpeg = ""
)

$ErrorActionPreference = "Stop"

function Resolve-Ffmpeg {
    param([string]$Hint)
    if ($Hint -and (Test-Path $Hint)) { return $Hint }
    $onPath = Get-Command ffmpeg -ErrorAction SilentlyContinue
    if ($onPath) { return $onPath.Source }
    $local = Join-Path $PSScriptRoot "..\.tools\ffmpeg\ffmpeg.exe"
    if (Test-Path $local) { return (Resolve-Path $local).Path }
    throw "ffmpeg not found. Pass -Ffmpeg <path>, put it on PATH, or drop it in .tools\ffmpeg\."
}

$ff = Resolve-Ffmpeg $Ffmpeg
$soundsDir = Join-Path $PSScriptRoot "..\src\main\resources\assets\barbarajones\sounds"
if (-not (Test-Path $soundsDir)) { throw "sounds dir not found: $soundsDir" }

# The exact filtergraphs (source clips are 22050 Hz mono).
$HEAVY  = "aresample=22050,asetrate=15435,aresample=44100,acrusher=bits=6:mode=log," +
          "aecho=0.8:0.9:60|110|180:0.5|0.35|0.22,aphaser=type=t:speed=0.3:decay=0.35," +
          "lowpass=f=3200,tremolo=f=5.5:d=0.35,volume=4dB,alimiter=limit=0.95"
$GENTLE = "aresample=22050,asetrate=18742,aresample=44100,acrusher=bits=8:mode=log," +
          "aecho=0.8:0.85:24|46:0.35|0.22,lowpass=f=6000,volume=2dB,alimiter=limit=0.95"

$files = Get-ChildItem -Path $soundsDir -Filter *.ogg
$done = 0
foreach ($f in $files) {
    $name = $f.Name
    if ($name -like "va_*" -or $name -like "*idle*") { $chain = $GENTLE; $tag = "gentle" }
    else                                             { $chain = $HEAVY;  $tag = "heavy"  }

    $tmp = Join-Path $env:TEMP ("horror_" + $name)
    & $ff -hide_banner -loglevel error -y -i $f.FullName -af $chain -ac 1 -c:a libvorbis -qscale:a 5 $tmp
    if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed on $name" }
    Move-Item -Force $tmp $f.FullName
    "{0,-24} {1}" -f $name, $tag | Write-Host
    $done++
}
Write-Host "Re-horrified $done sound(s)."
