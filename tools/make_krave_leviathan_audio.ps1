# Three low, long whale-style calls for the Krave Leviathan - synthesized
# from scratch as raw PCM, same pipeline as make_krave_audio.ps1 (hand-rolled
# WAV, encoded to Ogg Vorbis via the portable ffmpeg in .tools/ffmpeg).
# Deliberately three DIFFERENT lengths and frequency shapes rather than one
# clip repeated, so the creature doesn't read as a single looping sample:
# a long steady low drone, a shorter one with a slow downward glide (the
# classic whale-song shape), and a shortest one that pulses instead of
# holding a steady tone.
# Idempotent - safe to re-run.

$repoRoot = Split-Path -Parent $PSScriptRoot
$soundDir = "$repoRoot\src\main\resources\assets\barbarajones\sounds"
$scratch = "$repoRoot\.tools\audio_scratch"
$ffmpeg = "$repoRoot\.tools\ffmpeg\ffmpeg-master-latest-win64-gpl\bin\ffmpeg.exe"
New-Item -ItemType Directory -Force $soundDir,$scratch | Out-Null

if (-not (Test-Path $ffmpeg)) {
    Write-Error "ffmpeg not found at $ffmpeg - run the .tools/ffmpeg fetch step first."
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
    $w.Write([int16]1)          # PCM
    $w.Write([int16]1)          # mono
    $w.Write([int32]$rate)
    $w.Write([int32]$byteRate)
    $w.Write([int16]2)          # block align
    $w.Write([int16]16)         # bits per sample
    $w.Write([char[]]"data")
    $w.Write([int32]$dataSize)
    foreach ($s in $samples) {
        $clamped = [Math]::Max(-1.0, [Math]::Min(1.0, $s))
        $w.Write([int16]([Math]::Round($clamped * 32000)))
    }
    $w.Flush(); $w.Close(); $stream.Close()
}

$script:sd = 9001
function Noise() {
    $script:sd = ($script:sd * 1103515245 + 12345) -band 0x7fffffff
    return (($script:sd % 2000) / 1000.0) - 1.0
}

# Long, steady, very low drone with slow vibrato and a soft breathy noise
# floor - the "something enormous is out there" baseline call.
function Call1Samples() {
    $dur = 6.5
    $n = [int]($sampleRate * $dur)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $vibrato = [Math]::Sin($t * 2.0 * [Math]::PI * 0.3) * 1.5
        $freq = 38.0 + $vibrato
        $phase += 2.0 * [Math]::PI * $freq / $sampleRate
        $harmonicPhase = $phase * 2.01
        $attack = [Math]::Min(1.0, $t / 1.2)
        $release = [Math]::Min(1.0, ($dur - $t) / 1.5)
        $env = [Math]::Min($attack, $release)
        $tone = [Math]::Sin($phase) * 0.8 + [Math]::Sin($harmonicPhase) * 0.15
        $out[$i] = ($tone + (Noise) * 0.05) * $env * 0.9
    }
    return $out
}

# Medium length with a slow downward glide, the classic whale-song shape -
# starts higher, sinks lower, fades out.
function Call2Samples() {
    $dur = 4.2
    $n = [int]($sampleRate * $dur)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $freq = 70.0 - (35.0 * ($t / $dur))
        $phase += 2.0 * [Math]::PI * $freq / $sampleRate
        $harmonicPhase = $phase * 1.5
        $attack = [Math]::Min(1.0, $t / 0.8)
        $release = [Math]::Min(1.0, ($dur - $t) / 1.8)
        $env = [Math]::Min($attack, $release)
        $tone = [Math]::Sin($phase) * 0.85 + [Math]::Sin($harmonicPhase) * 0.1
        $out[$i] = ($tone + (Noise) * 0.06) * $env * 0.9
    }
    return $out
}

# Shortest call - pulses instead of holding steady, like a series of low
# moans rather than one sustained tone.
function Call3Samples() {
    $dur = 3.0
    $n = [int]($sampleRate * $dur)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $freq = 52.0 + [Math]::Sin($t * 2.0 * [Math]::PI * 0.5) * 4.0
        $phase += 2.0 * [Math]::PI * $freq / $sampleRate
        $pulse = 0.55 + 0.45 * [Math]::Sin($t * 2.0 * [Math]::PI * 1.3)
        $attack = [Math]::Min(1.0, $t / 0.5)
        $release = [Math]::Min(1.0, ($dur - $t) / 1.0)
        $env = [Math]::Min($attack, $release)
        $tone = [Math]::Sin($phase)
        $out[$i] = ($tone * 0.85 + (Noise) * 0.08) * $env * $pulse * 0.9
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

Build "krave_leviathan_call_1" { Call1Samples }
Build "krave_leviathan_call_2" { Call2Samples }
Build "krave_leviathan_call_3" { Call3Samples }

Remove-Item -Recurse -Force $scratch
"done"
