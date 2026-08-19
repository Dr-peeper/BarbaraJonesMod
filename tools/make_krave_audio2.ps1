# Second batch of synthesized original sound effects - same approach as
# make_krave_audio.ps1 (hand-rolled PCM -> WAV -> ffmpeg -> ogg), no external
# assets. Built because the mod's existing "krave mix" bank (krave_roar,
# krave_boom, krave_screech, etc.) was getting reused for everything and
# needed real variety for combat hits and transformations specifically.
# Idempotent - safe to re-run.

$repoRoot = Split-Path -Parent $PSScriptRoot
$soundDir = "$repoRoot\src\main\resources\assets\barbarajones\sounds"
$scratch = "$repoRoot\.tools\audio_scratch2"
New-Item -ItemType Directory -Force $soundDir,$scratch | Out-Null

$ffmpeg = (Get-ChildItem -Path "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter "ffmpeg.exe" -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
if (-not $ffmpeg) {
    $existing = "$repoRoot\.tools\ffmpeg\ffmpeg-master-latest-win64-gpl\bin\ffmpeg.exe"
    if (Test-Path $existing) { $ffmpeg = $existing }
}
if (-not $ffmpeg) {
    Write-Error "ffmpeg.exe not found (checked winget packages and .tools/ffmpeg)."
    exit 1
}

$sampleRate = 22050

function Write-Wav([float[]]$samples, [string]$path, [int]$rate) {
    $stream = [System.IO.File]::Open($path, [System.IO.FileMode]::Create)
    $w = New-Object System.IO.BinaryWriter($stream)
    $dataSize = $samples.Length * 2
    $byteRate = $rate * 2
    $w.Write([char[]]"RIFF")
    $w.Write([int32](36 + $dataSize))
    $w.Write([char[]]"WAVE")
    $w.Write([char[]]"fmt ")
    $w.Write([int32]16)
    $w.Write([int16]1)
    $w.Write([int16]1)
    $w.Write([int32]$rate)
    $w.Write([int32]$byteRate)
    $w.Write([int16]2)
    $w.Write([int16]16)
    $w.Write([char[]]"data")
    $w.Write([int32]$dataSize)
    foreach ($s in $samples) {
        $clamped = [Math]::Max(-1.0, [Math]::Min(1.0, $s))
        $w.Write([int16]([Math]::Round($clamped * 32000)))
    }
    $w.Flush(); $w.Close(); $stream.Close()
}

$script:sd = 4242
function Noise() {
    $script:sd = ($script:sd * 1103515245 + 12345) -band 0x7fffffff
    return (($script:sd % 2000) / 1000.0) - 1.0
}

# A slow rising charge-up hum - build-up before a transformation. Two
# detuned oscillators beating against each other for tension.
function ChargeSamples() {
    $dur = 1.1
    $n = [int]($sampleRate * $dur)
    $out = New-Object float[] $n
    $p1 = 0.0; $p2 = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $frac = $t / $dur
        $f1 = 60.0 + (340.0 * $frac * $frac)
        $f2 = $f1 * 1.015
        $p1 += 2.0 * [Math]::PI * $f1 / $sampleRate
        $p2 += 2.0 * [Math]::PI * $f2 / $sampleRate
        $env = [Math]::Min(1.0, $frac * 3.0) * (0.4 + 0.6 * $frac)
        $out[$i] = (([Math]::Sin($p1) + [Math]::Sin($p2)) * 0.4 + (Noise) * 0.08 * $frac) * $env
    }
    return $out
}

# The explosive release at the moment of transformation - a sharp noise
# burst with a falling tone underneath, longer and heavier than the boom
# already in the mix.
function ReleaseSamples() {
    $dur = 0.9
    $n = [int]($sampleRate * $dur)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $freq = 500.0 - (420.0 * [Math]::Min(1.0, $t / 0.6))
        $phase += 2.0 * [Math]::PI * [Math]::Max(30.0, $freq) / $sampleRate
        $env = [Math]::Exp(-2.6 * $t)
        $burst = if ($t -lt 0.05) { 1.0 } else { [Math]::Exp(-14.0 * ($t - 0.05)) }
        $out[$i] = (([Math]::Sin($phase) * 0.5) + ((Noise) * (0.7 * $burst + 0.15))) * $env
    }
    return $out
}

# A deep pulsating tone - Super Saiyan God / Blue's "pulsating energy."
# Amplitude breathes in a slow sine envelope over a low sustained tone.
function GodPulseSamples() {
    $dur = 1.6
    $n = [int]($sampleRate * $dur)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $phase += 2.0 * [Math]::PI * 90.0 / $sampleRate
        $pulse = 0.5 + 0.5 * [Math]::Sin(2.0 * [Math]::PI * 2.2 * $t)
        $fade = [Math]::Min(1.0, $t / 0.1) * [Math]::Exp(-1.1 * [Math]::Max(0.0, $t - 1.0))
        $out[$i] = ([Math]::Sin($phase) * 0.6 + [Math]::Sin($phase * 2.01) * 0.25) * $pulse * $fade
    }
    return $out
}

# An airy, shimmering high hum - Ultra Instinct's "stopped trying" calm.
# Bright, thin, almost no attack - the opposite of everything else here.
function UltraHumSamples() {
    $dur = 1.3
    $n = [int]($sampleRate * $dur)
    $out = New-Object float[] $n
    $p1 = 0.0; $p2 = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $p1 += 2.0 * [Math]::PI * 1200.0 / $sampleRate
        $p2 += 2.0 * [Math]::PI * 1803.0 / $sampleRate
        $env = [Math]::Min(1.0, $t / 0.4) * [Math]::Exp(-0.9 * $t)
        $shimmer = 0.7 + 0.3 * [Math]::Sin(2.0 * [Math]::PI * 5.0 * $t)
        $out[$i] = (([Math]::Sin($p1) * 0.25 + [Math]::Sin($p2) * 0.18) * $shimmer + (Noise) * 0.02) * $env
    }
    return $out
}

# A punchy melee impact - short, dry, mostly noise with a low thump.
function PunchSamples() {
    $dur = 0.22
    $n = [int]($sampleRate * $dur)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $phase += 2.0 * [Math]::PI * 95.0 / $sampleRate
        $env = [Math]::Exp(-24.0 * $t)
        $out[$i] = ([Math]::Sin($phase) * 0.55 + (Noise) * 0.6) * $env
    }
    return $out
}

# A heavier crunchy hit - the Krave Monster landing a blow. Lower, longer,
# with a gravelly noise tail.
function HeavyHitSamples() {
    $dur = 0.45
    $n = [int]($sampleRate * $dur)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $freq = 70.0 - (30.0 * [Math]::Min(1.0, $t / 0.3))
        $phase += 2.0 * [Math]::PI * $freq / $sampleRate
        $env = [Math]::Exp(-7.0 * $t)
        $crunch = (Noise) * (0.5 + 0.5 * [Math]::Exp(-20.0 * $t))
        $out[$i] = ([Math]::Sin($phase) * 0.6 + $crunch * 0.5) * $env
    }
    return $out
}

# A sharp electric crack - the lightning-wall ring during transformations.
function LightningCrackSamples() {
    $dur = 0.4
    $n = [int]($sampleRate * $dur)
    $out = New-Object float[] $n
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $env = if ($t -lt 0.01) { $t / 0.01 } else { [Math]::Exp(-11.0 * ($t - 0.01)) }
        $crackle = (Noise)
        $ring = [Math]::Sin(2.0 * [Math]::PI * 2200.0 * $t) * 0.3
        $out[$i] = ($crackle * 0.75 + $ring) * $env
    }
    return $out
}

# New Krave Monster roar variant - deeper and more guttural than the
# existing krave_roar, for the new higher forms.
function MonsterRoar2Samples() {
    $dur = 1.0
    $n = [int]($sampleRate * $dur)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $wobble = 1.0 + 0.15 * [Math]::Sin(2.0 * [Math]::PI * 7.0 * $t)
        $freq = (55.0 + 25.0 * [Math]::Sin(2.0 * [Math]::PI * 1.4 * $t)) * $wobble
        $phase += 2.0 * [Math]::PI * $freq / $sampleRate
        $env = [Math]::Min(1.0, $t / 0.12) * [Math]::Exp(-1.6 * $t)
        $out[$i] = ([Math]::Sin($phase) * 0.55 + [Math]::Sin($phase * 1.5) * 0.2 + (Noise) * 0.18) * $env
    }
    return $out
}

# New Krave Monster screech variant - higher, harsher, for the top forms.
function MonsterScreech2Samples() {
    $dur = 0.75
    $n = [int]($sampleRate * $dur)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $freq = 1400.0 + 900.0 * [Math]::Sin(2.0 * [Math]::PI * 11.0 * $t)
        $phase += 2.0 * [Math]::PI * $freq / $sampleRate
        $env = [Math]::Min(1.0, $t / 0.03) * [Math]::Exp(-3.2 * $t)
        $out[$i] = ([Math]::Sin($phase) * 0.4 + (Noise) * 0.45) * $env
    }
    return $out
}

function Build([string]$name, [scriptblock]$gen) {
    $wav = "$scratch\$name.wav"
    $ogg = "$soundDir\$name.ogg"
    Write-Wav (& $gen) $wav $sampleRate
    & $ffmpeg -y -loglevel error -i $wav -c:a libvorbis -qscale:a 4 $ogg
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $ogg)) {
        Write-Error "ffmpeg failed to encode $name"
        exit 1
    }
    "  $name.ogg: synthesized + encoded"
}

Build "transform_charge" { ChargeSamples }
Build "transform_release" { ReleaseSamples }
Build "transform_godpulse" { GodPulseSamples }
Build "transform_ultra_hum" { UltraHumSamples }
Build "combat_punch" { PunchSamples }
Build "combat_heavy_hit" { HeavyHitSamples }
Build "lightning_crack" { LightningCrackSamples }
Build "monster_roar2" { MonsterRoar2Samples }
Build "monster_screech2" { MonsterScreech2Samples }

Remove-Item -Recurse -Force $scratch
"done"
