# Generates cayden_alarm.ogg - a piercing, deliberately irritating two-tone
# emergency siren for when Cayden's health drops into the danger zone.
# Loopable: exactly 2.0s, starts and ends at zero crossing.
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$candidates = @(
    "$repoRoot\.tools\ffmpeg\ffmpeg.exe",
    'C:\Users\ADMIN\BarbaraJonesMod\.tools\ffmpeg\ffmpeg.exe'
)
$ffmpeg = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
if (-not $ffmpeg) {
    $onPath = Get-Command ffmpeg.exe -ErrorAction SilentlyContinue
    if ($onPath) { $ffmpeg = $onPath.Source }
}
if (-not $ffmpeg) { throw "ffmpeg.exe not found. Checked: $($candidates -join ', '), and PATH. Put a copy at $repoRoot\.tools\ffmpeg\ffmpeg.exe or install it on PATH." }

$outDir = "$repoRoot\src\main\resources\assets\barbarajones\sounds"
$wav    = Join-Path $env:TEMP 'cayden_alarm.wav'
$ogg    = Join-Path $outDir  'cayden_alarm.ogg'

$rate     = 44100
$duration = 2.0
$total    = [int]($rate * $duration)
$samples  = New-Object 'double[]' $total

# Two-tone alarm: alternates a high and a low tone 5x/second. Square-ish so it
# carries harmonics and cuts straight through everything else in the mix.
$hiFreq   = 1046.5    # C6
$loFreq   = 784.0     # G5
$swap     = 0.2       # seconds per tone

function SoftClip([double]$v) { return [Math]::Tanh($v * 2.4) }

for ($i = 0; $i -lt $total; $i++) {
    $t   = $i / $rate
    $seg = [Math]::Floor($t / $swap)
    $f   = if ($seg % 2 -eq 0) { $hiFreq } else { $loFreq }

    # main tone + a detuned twin: the beating between them is what grates
    $a = [Math]::Sin(2 * [Math]::PI * $f * $t)
    $b = [Math]::Sin(2 * [Math]::PI * ($f * 1.006) * $t) * 0.7
    # odd harmonic for the square-wave bite
    $c = [Math]::Sin(2 * [Math]::PI * $f * 3 * $t) * 0.22

    $v = SoftClip ($a + $b + $c)

    # short attack/release on each tone segment so it pulses instead of sliding
    $inSeg = $t - ($seg * $swap)
    $env = 1.0
    $ramp = 0.012
    if ($inSeg -lt $ramp)            { $env = $inSeg / $ramp }
    elseif ($inSeg -gt ($swap - $ramp)) { $env = ($swap - $inSeg) / $ramp }
    if ($env -lt 0) { $env = 0 }

    $samples[$i] = $v * $env * 0.82
}

# ---- write 16-bit mono PCM WAV -------------------------------------------
$stream = [System.IO.File]::Create($wav)
$writer = New-Object System.IO.BinaryWriter($stream)
$dataBytes = $total * 2
$writer.Write([char[]]'RIFF'); $writer.Write([int](36 + $dataBytes))
$writer.Write([char[]]'WAVE'); $writer.Write([char[]]'fmt ')
$writer.Write([int]16); $writer.Write([int16]1); $writer.Write([int16]1)
$writer.Write([int]$rate); $writer.Write([int]($rate * 2))
$writer.Write([int16]2); $writer.Write([int16]16)
$writer.Write([char[]]'data'); $writer.Write([int]$dataBytes)
foreach ($s in $samples) {
    $clamped = [Math]::Max(-1.0, [Math]::Min(1.0, $s))
    $writer.Write([int16]([Math]::Round($clamped * 32000)))
}
$writer.Close(); $stream.Close()

# ---- encode to OGG (Minecraft only reads Vorbis) --------------------------
# A touch of highpass keeps it thin and nasty rather than round and pleasant.
& $ffmpeg -y -loglevel error -i $wav -af "highpass=f=400,alimiter=limit=0.95" `
    -c:a libvorbis -q:a 5 -ar 44100 -ac 1 $ogg
if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed ($LASTEXITCODE)" }

Remove-Item $wav -Force -ErrorAction SilentlyContinue

if (Test-Path $ogg) {
    "OK  $ogg  ($([int]((Get-Item $ogg).Length/1KB)) KB)"
} else {
    throw "cayden_alarm.ogg was not written"
}
