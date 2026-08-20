# Textures + sounds for the Craveling mob family (Craveling, Krispbone,
# Loomweaver, Soggy, The Mascot) plus their flavor items/block.
#
# Entity skins use the exact UV math the game itself uses for a cuboid at
# texOffs (u,v) with size (w,h,d):
#   top    = (u+d,       v,       w, d)
#   bottom = (u+d+w,     v,       w, d)
#   right  = (u,         v+d,     d, h)
#   front  = (u+d,       v+d,     w, h)
#   left   = (u+d+w,     v+d,     d, h)
#   back   = (u+2d+w,    v+d,     w, h)
# Paint-Box below implements exactly that, so every texOffs/size pair here
# matches the addBox() calls in the corresponding client/*Model.java 1:1.
#
# Idempotent - safe to re-run. Verifies every file it writes by reloading it
# and checking actual pixel dimensions, not just Test-Path.

Add-Type -AssemblyName System.Drawing

$repoRoot   = Split-Path -Parent $PSScriptRoot
$entityDir  = "$repoRoot\src\main\resources\assets\barbarajones\textures\entity"
$itemDir    = "$repoRoot\src\main\resources\assets\barbarajones\textures\item"
$blockDir   = "$repoRoot\src\main\resources\assets\barbarajones\textures\block"
$soundDir   = "$repoRoot\src\main\resources\assets\barbarajones\sounds"
$scratch    = "$repoRoot\.tools\craveling_audio_scratch"
New-Item -ItemType Directory -Force $entityDir,$itemDir,$blockDir,$soundDir,$scratch | Out-Null

function C([string]$h,[int]$a=255){
    [System.Drawing.Color]::FromArgb($a,
        [Convert]::ToInt32($h.Substring(0,2),16),
        [Convert]::ToInt32($h.Substring(2,2),16),
        [Convert]::ToInt32($h.Substring(4,2),16))
}

function Rct($bmp,[int]$x,[int]$y,[int]$w,[int]$h,$col){
    for ($i = 0; $i -lt $w; $i++) {
        for ($j = 0; $j -lt $h; $j++) {
            $px = $x + $i; $py = $y + $j
            if ($px -ge 0 -and $py -ge 0 -and $px -lt $bmp.Width -and $py -lt $bmp.Height) {
                $bmp.SetPixel($px, $py, $col)
            }
        }
    }
}

# One cuboid's worth of the six standard skin faces. $mid is used for both
# side faces (right/left) - real vanilla skins vary those slightly, but a
# single mid-tone reads perfectly well at 16x scale and halves the work.
function Paint-Box($bmp, [int]$u, [int]$v, [int]$w, [int]$h, [int]$d, $base, $dark, $light, $mid) {
    Rct $bmp ($u + $d) $v $w $d $light                      # top
    Rct $bmp ($u + $d + $w) $v $w $d $dark                   # bottom
    Rct $bmp $u ($v + $d) $d $h $mid                         # right
    Rct $bmp ($u + $d) ($v + $d) $w $h $base                 # front
    Rct $bmp ($u + $d + $w) ($v + $d) $d $h $mid             # left
    Rct $bmp ($u + 2 * $d + $w) ($v + $d) $w $h $dark         # back
}

function New-Canvas([int]$w,[int]$h){
    $b = New-Object System.Drawing.Bitmap $w,$h,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    Rct $b 0 0 $w $h (C '000000' 0)
    return $b
}

function Save-Verify($bmp, [string]$path, [int]$expectW, [int]$expectH) {
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    if (-not (Test-Path $path)) { Write-Error "MISSING after save: $path"; exit 1 }
    $check = New-Object System.Drawing.Bitmap $path
    $ok = ($check.Width -eq $expectW -and $check.Height -eq $expectH)
    $check.Dispose()
    if (-not $ok) { Write-Error "BAD DIMENSIONS: $path"; exit 1 }
    "  OK  $path ($expectW x $expectH)"
}

# ==========================================================================
# CRAVELING - 64x80. Chunky cereal-square palette: warm browns/tans, a
# lighter khaki top, dark crevice back/bottom - reads as blocky cereal
# pieces stacked into a person, not skin.
# ==========================================================================
$b = New-Canvas 64 80
$base  = C '9A6B34'; $dark = C '6B441C'; $light = C 'C99857'; $mid = C '82552A'
Paint-Box $b 0 0 8 8 8 $base $dark $light $mid         # head
Paint-Box $b 16 16 8 12 4 $base $dark $light $mid       # body
Paint-Box $b 40 16 4 12 4 $base $dark $light $mid       # right arm
Paint-Box $b 32 48 4 12 4 $base $dark $light $mid       # left arm
Paint-Box $b 0 16 4 12 4 $base $dark $light $mid        # right leg
Paint-Box $b 16 48 4 12 4 $base $dark $light $mid       # left leg
$crumb = C 'B98A4A'; $crumbD = C '7A5426'; $crumbL = C 'D9B378'; $crumbM = C '9A6E3A'
Paint-Box $b 0 64 2 2 2 $crumb $crumbD $crumbL $crumbM   # chunk_shoulder_l
Paint-Box $b 8 64 2 2 2 $crumb $crumbD $crumbL $crumbM   # chunk_shoulder_r
Paint-Box $b 16 64 3 2 1 $crumb $crumbD $crumbL $crumbM  # chunk_chest
Paint-Box $b 24 64 2 2 2 $crumb $crumbD $crumbL $crumbM  # floating_crumb
# face: two dark eye pips + a flat mouth line on the head FRONT (8,8,8,8)
Rct $b 10 11 2 1 (C '241408'); Rct $b 13 11 2 1 (C '241408')
Rct $b 10 14 5 1 (C '3A2510')
# a scatter of loose crumb specks across the torso front, selling "made of
# pieces" better than a flat fill ever could
$specks = @(@(21,20), @(22,23), @(25,21), @(27,25), @(24,28), @(19,26), @(30,24))
foreach ($sp in $specks) { Rct $b $sp[0] $sp[1] 1 1 (C '4E3418') }
Save-Verify $b "$entityDir\craveling.png" 64 80

# ==========================================================================
# KRISPBONE - 64x64. Dry, pale, cracked-cereal skeleton: bone-tan base with
# dark hairline cracks, same standard skin layout as Craveling but every box
# is thinner (see KrispboneModel.java).
# ==========================================================================
$b = New-Canvas 64 64
$kbase = C 'D8C79A'; $kdark = C '9C8A5E'; $klight = C 'EDE0BE'; $kmid = C 'C0AD7E'
Paint-Box $b 0 0 8 8 8 $kbase $kdark $klight $kmid       # head (skull)
Paint-Box $b 16 16 6 12 3 $kbase $kdark $klight $kmid    # thin body
Paint-Box $b 40 16 2 12 2 $kbase $kdark $klight $kmid    # right arm
Paint-Box $b 32 48 2 12 2 $kbase $kdark $klight $kmid    # left arm
Paint-Box $b 0 16 2 12 2 $kbase $kdark $klight $kmid     # right leg
Paint-Box $b 16 48 2 12 2 $kbase $kdark $klight $kmid    # left leg
# eye sockets: dark hollows on the head front (8,8,8,8)
Rct $b 9 10 3 3 (C '241E14'); Rct $b 14 10 3 3 (C '241E14')
Rct $b 10 15 4 1 (C '5A4E30')
# crack lines - thin dark hairlines across skull + ribs
Rct $b 12 8 1 3 (C '7A6C46'); Rct $b 18 20 1 6 (C '7A6C46'); Rct $b 22 24 1 4 (C '7A6C46')
Save-Verify $b "$entityDir\krispbone.png" 64 64

# ==========================================================================
# LOOMWEAVER - 64x32. Clumped cereal body + hardened milk-strand legs: dark
# muddy-brown body, pale bone-white legs (the "milk strand" material).
# ==========================================================================
$b = New-Canvas 64 32
$lbase = C '4A3220'; $ldark = C '2E1E12'; $llight = C '6B4A2E'; $lmid = C '3E2A19'
Paint-Box $b 0 0 8 6 8 $lbase $ldark $llight $lmid       # abdomen
Paint-Box $b 32 0 6 5 4 $lbase $ldark $llight $lmid      # thorax
Paint-Box $b 0 14 4 4 3 $lbase $ldark $llight $lmid      # head
$strand = C 'EDE6D6'; $strandD = C 'B8AD94'; $strandL = C 'FFFDF6'; $strandM = C 'D6CBB2'
Paint-Box $b 16 14 1 8 1 $strand $strandD $strandL $strandM  # leg (reused UV for all 8)
# two small pale eye dots on the head front
Rct $b 1 15 1 1 (C 'D8CFBA'); Rct $b 3 15 1 1 (C 'D8CFBA')
Save-Verify $b "$entityDir\loomweaver.png" 64 32

# ==========================================================================
# SOGGY - 64x96. Waterlogged, pale and blue-green, glossy highlights (wet
# sheen) instead of a dry crumb texture.
# ==========================================================================
$b = New-Canvas 64 96
$sbase = C '6E7A4A'; $sdark = C '445230'; $slight = C '9AAE6E'; $smid = C '576639'
Paint-Box $b 0 0 8 8 8 $sbase $sdark $slight $smid        # head
Paint-Box $b 0 64 10 12 6 $sbase $sdark $slight $smid     # bloated body
Paint-Box $b 40 16 5 12 5 $sbase $sdark $slight $smid     # right arm (stubby)
Paint-Box $b 32 48 5 12 5 $sbase $sdark $slight $smid     # left arm (stubby)
Paint-Box $b 0 16 4 12 4 $sbase $sdark $slight $smid      # right leg
Paint-Box $b 16 48 4 12 4 $sbase $sdark $slight $smid     # left leg
$belly = C '8CA060'; $bellyD = C '576639'; $bellyL = C 'B4C888'; $bellyM = C '6E8248'
Paint-Box $b 32 64 4 4 2 $belly $bellyD $bellyL $bellyM   # belly bulge
# droopy dark eyes + a wet highlight streak down the belly
Rct $b 10 11 2 2 (C '2A331C'); Rct $b 13 11 2 2 (C '2A331C')
Rct $b 34 66 1 3 (C 'D2E2AC')
Save-Verify $b "$entityDir\soggy.png" 64 96

# ==========================================================================
# THE MASCOT - 64x96. A cheerful painted cereal-box face on the oversized
# box head: bright red box, big painted eyes and a wide painted grin, gold
# trim - reads as costume packaging, not a body part.
# ==========================================================================
$b = New-Canvas 64 96
$mbody = C '3A2C1E'; $mbodyD = C '241A11'; $mbodyL = C '5A4530'; $mbodyM = C '2E2216'
Paint-Box $b 40 16 4 12 4 $mbody $mbodyD $mbodyL $mbodyM   # right arm
Paint-Box $b 32 48 4 12 4 $mbody $mbodyD $mbodyL $mbodyM   # left arm
Paint-Box $b 0 16 4 12 4 $mbody $mbodyD $mbodyL $mbodyM    # right leg
Paint-Box $b 16 48 4 12 4 $mbody $mbodyD $mbodyL $mbodyM   # left leg
$box = C 'D9384A'; $boxD = C '8F1E2A'; $boxL = C 'F26A78'; $boxM = C 'B82A38'
Paint-Box $b 0 64 10 12 6 $box $boxD $boxL $boxM           # box head
$gold = C 'F2D33A'; $goldD = C 'B89620'
Rct $b 6 64 22 2 $gold                                     # top trim band (on the "top" UV row)
Rct $b 6 76 22 2 $goldD                                    # bottom trim band ("bottom" UV row)
# big painted grin + eyes on the head FRONT face, which is (u+d, v+d, w, h) = (6,70,10,12)
Rct $b 8 73 2 2 (C 'FFFFFF'); Rct $b 8 73 1 1 (C '1A1008')
Rct $b 13 73 2 2 (C 'FFFFFF'); Rct $b 14 73 1 1 (C '1A1008')
Rct $b 8 78 7 1 (C 'FFF3C4')
Rct $b 8 79 1 1 (C 'FFF3C4'); Rct $b 14 79 1 1 (C 'FFF3C4')
Save-Verify $b "$entityDir\the_mascot.png" 64 96

# ==========================================================================
# ITEMS + BLOCK (flat 16x16 icons)
# ==========================================================================
function Save-VerifySame($bmp,$path){ Save-Verify $bmp $path 16 16 }

# krave_shard: a jagged off-white/tan sliver
$b = New-Canvas 16 16
Rct $b 6 2 4 3 (C 'EDE0BE'); Rct $b 5 5 5 3 (C 'D8C79A'); Rct $b 6 8 4 3 (C 'C0AD7E'); Rct $b 7 11 3 3 (C '9C8A5E')
Rct $b 7 3 1 1 (C 'FFFDF6'); Rct $b 8 12 1 1 (C '6B5C3A')
Save-VerifySame $b "$itemDir\krave_shard.png"

# cereal_mascot_head: a tiny box-head trophy icon
$b = New-Canvas 16 16
Rct $b 2 3 12 10 (C 'D9384A'); Rct $b 2 3 12 2 (C 'F2D33A'); Rct $b 2 11 12 2 (C 'B89620')
Rct $b 4 7 2 2 (C 'FFFFFF'); Rct $b 4 7 1 1 (C '1A1008')
Rct $b 10 7 2 2 (C 'FFFFFF'); Rct $b 11 7 1 1 (C '1A1008')
Rct $b 4 10 8 1 (C 'FFF3C4')
Save-VerifySame $b "$itemDir\cereal_mascot_head.png"

# milk_webbing: pale cross-hatched strand texture (cobweb-style cross model)
$b = New-Canvas 16 16
$w = C 'F2EEE2'; $wd = C 'C9C2AC'
for ($i = 0; $i -lt 16; $i++) {
    Rct $b $i $i 1 1 $w
    Rct $b (15 - $i) $i 1 1 $w
}
Rct $b 0 7 16 1 $wd; Rct $b 7 0 1 16 $wd
Save-VerifySame $b "$blockDir\milk_webbing.png"

"textures done"

# ==========================================================================
# SOUNDS - hand-rolled PCM -> WAV -> ffmpeg -> ogg, same approach as
# tools/make_krave_audio2.ps1. sounds.json entries were added by hand
# alongside ModMobSounds (see src/main/resources/assets/barbarajones/sounds.json).
# ==========================================================================
$ffmpeg = (Get-ChildItem -Path "$env:LOCALAPPDATA\Microsoft\WinGet\Packages" -Recurse -Filter "ffmpeg.exe" -ErrorAction SilentlyContinue | Select-Object -First 1).FullName
if (-not $ffmpeg) {
    $existing = "$repoRoot\.tools\ffmpeg\ffmpeg-master-latest-win64-gpl\bin\ffmpeg.exe"
    if (Test-Path $existing) { $ffmpeg = $existing }
}
if (-not $ffmpeg) {
    Write-Error "ffmpeg.exe not found (checked winget packages and .tools/ffmpeg). Textures were still written above."
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

$script:sd = 9001
function Noise() {
    $script:sd = ($script:sd * 1103515245 + 12345) -band 0x7fffffff
    return (($script:sd % 2000) / 1000.0) - 1.0
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
    $reread = New-Object System.IO.FileInfo($ogg)
    if ($reread.Length -le 0) { Write-Error "$name.ogg wrote empty"; exit 1 }
    "  OK  $name.ogg ($($reread.Length) bytes)"
}

# ---- Craveling: dry crunchy shuffle/bite ---------------------------------
function CravelingAmbient() {
    $n = [int]($sampleRate * 0.5)
    $out = New-Object float[] $n
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $env = [Math]::Exp(-4.0 * $t)
        $out[$i] = (Noise) * 0.5 * $env
    }
    return $out
}
function CravelingHurt() {
    $n = [int]($sampleRate * 0.3)
    $out = New-Object float[] $n
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $env = [Math]::Exp(-9.0 * $t)
        $crunch = if (($i % 40) -lt 4) { 0.8 } else { 0.0 }
        $out[$i] = ((Noise) * 0.6 + $crunch) * $env
    }
    return $out
}
function CravelingDeath() {
    $n = [int]($sampleRate * 0.6)
    $out = New-Object float[] $n
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $env = [Math]::Exp(-3.0 * $t)
        $out[$i] = (Noise) * 0.55 * $env
    }
    return $out
}
function CravelingStep() {
    $n = [int]($sampleRate * 0.12)
    $out = New-Object float[] $n
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $env = [Math]::Exp(-25.0 * $t)
        $out[$i] = (Noise) * 0.4 * $env
    }
    return $out
}

# ---- Krispbone: dry rattling clicks + a sharp flick -----------------------
function KrispboneAmbient() {
    $n = [int]($sampleRate * 0.4)
    $out = New-Object float[] $n
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $click = if (($i % 900) -lt 8) { 0.7 } else { 0.0 }
        $out[$i] = $click * [Math]::Exp(-40.0 * (($i % 900) / $sampleRate))
    }
    return $out
}
function KrispboneHurt() {
    $n = [int]($sampleRate * 0.25)
    $out = New-Object float[] $n
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $env = [Math]::Exp(-14.0 * $t)
        $out[$i] = (Noise) * 0.5 * $env
    }
    return $out
}
function KrispboneDeath() {
    $n = [int]($sampleRate * 0.5)
    $out = New-Object float[] $n
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $rattle = if (($i % 700) -lt 6) { 0.6 } else { 0.0 }
        $env = [Math]::Exp(-3.0 * $t)
        $out[$i] = ($rattle + (Noise) * 0.2) * $env
    }
    return $out
}
function KrispboneShoot() {
    $n = [int]($sampleRate * 0.15)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $freq = 2200.0 - 1400.0 * $t / 0.15
        $phase += 2.0 * [Math]::PI * $freq / $sampleRate
        $env = [Math]::Exp(-18.0 * $t)
        $out[$i] = ([Math]::Sin($phase) * 0.3 + (Noise) * 0.3) * $env
    }
    return $out
}

# ---- Loomweaver: wet clicking skitter + a sticky stretch -----------------
function LoomweaverAmbient() {
    $n = [int]($sampleRate * 0.4)
    $out = New-Object float[] $n
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $click = if (($i % 600) -lt 10) { 0.5 } else { 0.0 }
        $out[$i] = $click * [Math]::Exp(-30.0 * (($i % 600) / $sampleRate))
    }
    return $out
}
function LoomweaverHurt() {
    $n = [int]($sampleRate * 0.22)
    $out = New-Object float[] $n
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $env = [Math]::Exp(-16.0 * $t)
        $out[$i] = (Noise) * 0.45 * $env
    }
    return $out
}
function LoomweaverDeath() {
    $n = [int]($sampleRate * 0.5)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $freq = 500.0 - 350.0 * $t / 0.5
        $phase += 2.0 * [Math]::PI * $freq / $sampleRate
        $env = [Math]::Exp(-4.0 * $t)
        $out[$i] = ([Math]::Sin($phase) * 0.3 + (Noise) * 0.25) * $env
    }
    return $out
}
function LoomweaverWeb() {
    $n = [int]($sampleRate * 0.3)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $freq = 300.0 + 200.0 * $t / 0.3
        $phase += 2.0 * [Math]::PI * $freq / $sampleRate
        $env = [Math]::Sin([Math]::Min(1.0, $t / 0.05) * [Math]::PI / 2.0) * [Math]::Exp(-5.0 * $t)
        $out[$i] = ([Math]::Sin($phase) * 0.25 + (Noise) * 0.2) * $env
    }
    return $out
}

# ---- Soggy: wet squelching splashes ---------------------------------------
function SoggyAmbient() {
    $n = [int]($sampleRate * 0.4)
    $out = New-Object float[] $n
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $env = [Math]::Exp(-6.0 * $t) * [Math]::Sin([Math]::Min(1.0, $t / 0.05) * [Math]::PI / 2.0)
        $out[$i] = (Noise) * 0.4 * $env
    }
    return $out
}
function SoggyHurt() {
    $n = [int]($sampleRate * 0.25)
    $out = New-Object float[] $n
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $env = [Math]::Exp(-10.0 * $t)
        $out[$i] = (Noise) * 0.5 * $env
    }
    return $out
}
function SoggyDeath() {
    $n = [int]($sampleRate * 0.55)
    $out = New-Object float[] $n
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $env = [Math]::Exp(-3.5 * $t)
        $out[$i] = (Noise) * 0.5 * $env
    }
    return $out
}
function SoggySplash() {
    $n = [int]($sampleRate * 0.5)
    $out = New-Object float[] $n
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $env = [Math]::Exp(-5.0 * $t) * [Math]::Sin([Math]::Min(1.0, $t / 0.02) * [Math]::PI / 2.0)
        $hiss = (Noise) * (1.0 - [Math]::Min(1.0, $t * 3.0))
        $out[$i] = $hiss * 0.6 * $env
    }
    return $out
}

# ---- The Mascot: cheerful chime idle, a "boing" flee, a sad descending
#      chime on death ---------------------------------------------------
function MascotAmbient() {
    $n = [int]($sampleRate * 0.4)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $freq = 880.0 + 220.0 * [Math]::Sin(2.0 * [Math]::PI * 6.0 * $t)
        $phase += 2.0 * [Math]::PI * $freq / $sampleRate
        $env = [Math]::Exp(-3.0 * $t)
        $out[$i] = [Math]::Sin($phase) * 0.35 * $env
    }
    return $out
}
function MascotHurt() {
    $n = [int]($sampleRate * 0.2)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $freq = 660.0
        $phase += 2.0 * [Math]::PI * $freq / $sampleRate
        $env = [Math]::Exp(-14.0 * $t)
        $out[$i] = ([Math]::Sin($phase) * 0.3 + (Noise) * 0.15) * $env
    }
    return $out
}
function MascotDeath() {
    $n = [int]($sampleRate * 0.9)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $freq = 700.0 - 500.0 * $t / 0.9
        $phase += 2.0 * [Math]::PI * $freq / $sampleRate
        $env = [Math]::Exp(-2.5 * $t)
        $out[$i] = [Math]::Sin($phase) * 0.3 * $env
    }
    return $out
}
function MascotFlee() {
    $n = [int]($sampleRate * 0.25)
    $out = New-Object float[] $n
    $phase = 0.0
    for ($i = 0; $i -lt $n; $i++) {
        $t = $i / $sampleRate
        $freq = 500.0 + 900.0 * $t / 0.25
        $phase += 2.0 * [Math]::PI * $freq / $sampleRate
        $env = [Math]::Exp(-6.0 * $t)
        $out[$i] = [Math]::Sin($phase) * 0.35 * $env
    }
    return $out
}

Build "craveling_ambient" { CravelingAmbient }
Build "craveling_hurt" { CravelingHurt }
Build "craveling_death" { CravelingDeath }
Build "craveling_step" { CravelingStep }
Build "krispbone_ambient" { KrispboneAmbient }
Build "krispbone_hurt" { KrispboneHurt }
Build "krispbone_death" { KrispboneDeath }
Build "krispbone_shoot" { KrispboneShoot }
Build "loomweaver_ambient" { LoomweaverAmbient }
Build "loomweaver_hurt" { LoomweaverHurt }
Build "loomweaver_death" { LoomweaverDeath }
Build "loomweaver_web" { LoomweaverWeb }
Build "soggy_ambient" { SoggyAmbient }
Build "soggy_hurt" { SoggyHurt }
Build "soggy_death" { SoggyDeath }
Build "soggy_splash" { SoggySplash }
Build "mascot_ambient" { MascotAmbient }
Build "mascot_hurt" { MascotHurt }
Build "mascot_death" { MascotDeath }
Build "mascot_flee" { MascotFlee }

Remove-Item -Recurse -Force $scratch
"sounds done"
