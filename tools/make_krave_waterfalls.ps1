# Synthesizes the one ambient sound the chocolate waterfalls feature needs
# (chocolate_flow: a soft, viscous pour-and-gulp loop-friendly clip) as raw
# PCM written to a hand-rolled WAV file, then shells out to ffmpeg to encode
# it as real Ogg Vorbis - same "no external assets" approach as
# tools/make_krave_audio.ps1 and tools/make_krave_audio2.ps1, whose
# Write-Wav/ffmpeg-lookup this file is deliberately a close copy of rather
# than a new invention. Idempotent - safe to re-run.

$repoRoot = Split-Path -Parent $PSScriptRoot
$soundDir = "$repoRoot\src\main\resources\assets\barbarajones\sounds"
$scratch = "$repoRoot\.tools\audio_scratch_waterfalls"
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

# xorshift-ish seeded PRNG, same convention as the other audio/texture generators' $script:sd
$script:sd = 8814
function Noise() {
    $script:sd = ($script:sd * 1103515245 + 12345) -band 0x7fffffff
    return (($script:sd % 2000) / 1000.0) - 1.0
}

# Soft, viscous pour: low-pass-filtered noise (a one-pole IIR smoothing raw
# white noise into a duller "whoosh" instead of a hiss) under a slow
# turbulence undulation, plus a warm low sine for body, plus a handful of
# discrete descending "gloop" blips scattered through it - thick liquid
# glugging down a face, not a clean waterfall hiss.
function ChocolateFlowSamples() {
    $dur = 2.4
    $n = [int]($sampleRate * $dur)
    $out = New-Object float[] $n

    # envelope: quick fade in, long sustain, gentle fade out
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $env = [Math]::Min(1.0, $t / 0.12) * [Math]::Min(1.0, ($dur - $t) / 0.35)
        $out[$i] = 0.0
        $out[$i] += $env
    }

    # low-pass-filtered pour noise
    $lp = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $lp = $lp * 0.90 + (Noise) * 0.10
        $turbulence = 0.7 + 0.3 * [Math]::Sin(2.0 * [Math]::PI * 1.7 * $t + (Noise) * 0.4)
        $out[$i] = $out[$i] * ($lp * 0.5 * $turbulence)
    }

    # warm low body tone with slow vibrato
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $freq = 78.0 + 6.0 * [Math]::Sin(2.0 * [Math]::PI * 0.6 * $t)
        $phase += 2.0 * [Math]::PI * $freq / $sampleRate
        $bodyEnv = [Math]::Min(1.0, $t / 0.2) * [Math]::Min(1.0, ($dur - $t) / 0.4)
        $out[$i] += [Math]::Sin($phase) * 0.16 * $bodyEnv
    }

    # discrete "gloop" blips at fixed, deliberately non-uniform offsets
    $gloopAt = @(0.18, 0.55, 0.95, 1.3, 1.7, 2.05)
    foreach ($start in $gloopAt) {
        $gloopDur = 0.09
        $gn = [int]($sampleRate * $gloopDur)
        $gp = 0.0
        for ($j = 0; $j -lt $gn; $j++) {
            $gt = $j / $sampleRate
            $gfreq = 170.0 - 110.0 * ($gt / $gloopDur)
            $gp += 2.0 * [Math]::PI * [Math]::Max(40.0, $gfreq) / $sampleRate
            $genv = [Math]::Exp(-14.0 * $gt)
            $idx = [int](($start / $dur) * $n) + $j
            if ($idx -ge 0 -and $idx -lt $n) {
                $out[$idx] += [Math]::Sin($gp) * 0.42 * $genv
            }
        }
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
    $info = Get-Item $ogg
    if ($info.Length -lt 500) {
        Write-Error "$name.ogg looks too small ($($info.Length) bytes) - something went wrong"
        exit 1
    }
    "  $name.ogg: synthesized + encoded ($($info.Length) bytes)"
}

Build "chocolate_flow" { ChocolateFlowSamples }

Remove-Item -Recurse -Force $scratch
"done"
