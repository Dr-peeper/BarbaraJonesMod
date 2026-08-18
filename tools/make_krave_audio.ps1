# Synthesizes original sound effects for the Krave Kosmos expansion (the
# mouth-beam fire/impact cues) as raw PCM written to hand-rolled WAV files,
# then shells out to the portable ffmpeg fetched into .tools/ffmpeg to encode
# them as Ogg Vorbis (Minecraft requires real .ogg - a renamed .wav will not
# load). Deliberately NOT reusing any of the mod's existing "krave mix" sound
# bank - these are new waveforms built from scratch, same "no external
# assets" spirit as the tools/*.ps1 texture generators.
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

# xorshift-ish seeded PRNG, same convention as the texture generators' $script:sd
$script:sd = 1337
function Noise() {
    $script:sd = ($script:sd * 1103515245 + 12345) -band 0x7fffffff
    return (($script:sd % 2000) / 1000.0) - 1.0
}

# A rising sci-fi charge/zap sweep with a short noisy tail - the mouth beam's
# fire cue. Frequency ramps up (a "charging" feel), amplitude has a quick
# attack and a longer decay so it reads as a single confident blast.
function BeamFireSamples() {
    $dur = 0.55
    $n = [int]($sampleRate * $dur)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $freq = 180.0 + (820.0 * ($t / $dur))
        $phase += 2.0 * [Math]::PI * $freq / $sampleRate
        $env = [Math]::Min(1.0, $t / 0.03) * [Math]::Exp(-3.0 * $t)
        $tone = [Math]::Sin($phase)
        $out[$i] = ($tone * 0.85 + (Noise) * 0.15) * $env
    }
    return $out
}

# A lower, short impact thud - the beam's hit cue. Falling pitch + noise burst.
function BeamHitSamples() {
    $dur = 0.35
    $n = [int]($sampleRate * $dur)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $freq = 260.0 - (200.0 * ($t / $dur))
        $phase += 2.0 * [Math]::PI * [Math]::Max(40.0, $freq) / $sampleRate
        $env = [Math]::Exp(-9.0 * $t)
        $tone = [Math]::Sin($phase)
        $out[$i] = ($tone * 0.6 + (Noise) * 0.5) * $env
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

Build "krave_beam_fire" { BeamFireSamples }
Build "krave_beam_hit" { BeamHitSamples }

Remove-Item -Recurse -Force $scratch
"done"
