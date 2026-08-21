# Seven Krave Monster textures, one per form.
#
# The UV layout is copied exactly from KraveMonsterModel - a four-legged,
# spine-spiked, tailed creature, NOT the shared humanoid rig. Box(u,v,w,h,d)
# fills a part's whole footprint using Minecraft's standard cube unwrap
# (width 2*(w+d), height d+h), which is the same maths the model uses. Get this
# wrong and you do not get a missing texture, you get a scrambled one - which is
# far easier to miss in a screenshot.
#
# Each form gets its own palette AND its own surface treatment, because seven
# recolours of one skin read as a palette swap rather than an escalation:
#   1 Awakening    toasted cereal brown, plain and honest
#   2 Chocolate    cracked open, molten chocolate bleeding through
#   3 Double Choc  near-black with a hot amber core burning underneath
#   4 Swarm        crawling green-brown, speckled with too many small eyes
#   5 Milk         pale cream marbled with chocolate, bloated and wet
#   6 Overload     crimson, splitting apart, white-hot at every seam
#   7 KRAVE GOD    void black veined with gold, nothing cereal left about it

Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'

$repo = Split-Path $PSScriptRoot -Parent
$edir = Join-Path $repo 'src\main\resources\assets\barbarajones\textures\entity'
if (-not (Test-Path $edir)) { throw "entity texture folder missing: $edir" }

function C([string]$hex, [int]$a = 255) {
    [System.Drawing.Color]::FromArgb($a,
        [Convert]::ToInt32($hex.Substring(0,2),16),
        [Convert]::ToInt32($hex.Substring(2,2),16),
        [Convert]::ToInt32($hex.Substring(4,2),16))
}
function Rct($bmp,$x,$y,$w,$h,$col){
    $g=[System.Drawing.Graphics]::FromImage($bmp)
    $g.CompositingMode='SourceCopy'
    $br=New-Object System.Drawing.SolidBrush $col
    $g.FillRectangle($br,$x,$y,$w,$h); $br.Dispose(); $g.Dispose()
}
function Box($bmp,$u,$v,$w,$h,$d,$c){ Rct $bmp $u $v (2*($w+$d)) ($d+$h) $c }

# Deterministic noise. A random scatter regenerates differently every run, which
# makes the texture un-diffable and hides real changes in the churn.
$script:sd = 11
function Rnd([int]$n){ $script:sd=($script:sd*1103515245+12345) -band 0x7fffffff; return $script:sd % $n }
function ResetSeed(){ $script:sd = 11 }

# A speckled panel: base colour, then flecks of the accent colours over it.
function Speck($bmp,$u,$v,$w,$h,$d,$base,$flecks,$rate,$sparkle){
    Box $bmp $u $v $w $h $d $base
    $fw=2*($w+$d); $fh=$d+$h
    for($i=0;$i -lt [int]($fw*$fh/$rate);$i++){
        $px=$u+(Rnd $fw); $py=$v+(Rnd $fh)
        Rct $bmp $px $py 1 1 $flecks[(Rnd $flecks.Count)]
    }
    if ($sparkle) {
        for($i=0;$i -lt [int]($fw*$fh/16);$i++){
            Rct $bmp ($u+(Rnd $fw)) ($v+(Rnd $fh)) 1 1 $sparkle
        }
    }
}

# Cracks: short jagged runs of a hot colour, for the forms that are splitting.
function Crack($bmp,$u,$v,$w,$h,$d,$hot,$count){
    $fw=2*($w+$d); $fh=$d+$h
    for($c=0;$c -lt $count;$c++){
        $x=$u+(Rnd $fw); $y=$v+(Rnd $fh)
        for($s=0;$s -lt 5;$s++){
            if ($x -ge $u -and $x -lt ($u+$fw) -and $y -ge $v -and $y -lt ($v+$fh)) {
                Rct $bmp $x $y 1 1 $hot
            }
            if ((Rnd 2) -eq 0) { $x++ } else { $y++ }
        }
    }
}

function Build-Form {
    param(
        [string]$file,
        $hide, $hideL, $bone, $boneD, $eye, $claw,
        $flecks, $sparkle, [int]$rate,
        $crackColor, [int]$crackCount
    )
    ResetSeed
    $b = New-Object System.Drawing.Bitmap 128,128,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    Rct $b 0 0 128 128 (C '000000' 0)

    # spine: hips -> chest -> neck
    Speck $b 0  0 10 8 10 $hide  $flecks $rate $sparkle
    Speck $b 40 0  9 8 9  $hide  $flecks $rate $sparkle
    Speck $b 76 0  5 5 5  $hide  $flecks $rate $sparkle

    # head: skull, jaw, horns - no speckle, so the face stays readable at range
    Box $b 0  26 7 6 7 $hide
    Box $b 28 26 4 3 6 $hideL
    Box $b 64 26 2 4 2 $bone
    Box $b 72 26 2 4 2 $bone
    Rct $b 3  28 1 1 $eye
    Rct $b 10 28 1 1 $eye
    Rct $b 65 26 2 1 $boneD
    Rct $b 73 26 2 1 $boneD

    # front legs (painted once; the model mirrors this UV for the other side)
    Speck $b 0  46 4 7 4 $hide $flecks $rate $sparkle
    Speck $b 16 46 3 6 3 $hide $flecks $rate $sparkle
    Box   $b 28 46 4 3 5 $claw

    # back legs
    Speck $b 46 46 5 8 5 $hide $flecks $rate $sparkle
    Speck $b 66 46 4 7 4 $hide $flecks $rate $sparkle
    Box   $b 82 46 4 3 6 $claw

    # tail, tapering
    Speck $b 0  64 4 4 5 $hide  $flecks $rate $sparkle
    Speck $b 18 64 3 3 4 $hideL $flecks $rate $sparkle
    Speck $b 32 64 2 2 3 $hideL $flecks $rate $sparkle

    # spine spikes
    Box $b 42 64 2 4 2 $bone
    Box $b 50 64 2 6 2 $bone

    # the seams, for whichever forms are coming apart
    if ($crackCount -gt 0) {
        Crack $b 0  0 10 8 10 $crackColor $crackCount
        Crack $b 40 0  9 8 9  $crackColor $crackCount
        Crack $b 46 46 5 8 5  $crackColor ([int]($crackCount/2))
    }

    $path = Join-Path $edir $file
    $b.Save($path,[System.Drawing.Imaging.ImageFormat]::Png)
    $b.Dispose()

    # Verify what landed rather than trusting the save - a texture whose size is
    # wrong renders as garbage rather than as an error.
    $check = New-Object System.Drawing.Bitmap $path
    $ok = ($check.Width -eq 128 -and $check.Height -eq 128)
    $check.Dispose()
    if (-not $ok) { throw "wrote $file at the wrong size" }
    Write-Host "  OK  $file"
}

# 1 - Krave Awakening: plain toasted cereal. Nothing wrong with him yet.
Build-Form 'krave_monster_1.png' (C '8A5A28') (C 'A97239') (C 'E8E2D8') (C 'B8AE9C') `
    (C 'FFD98A') (C '4A3418') @((C 'A97239'),(C '6E4520'),(C 'C98F4A')) $null 6 $null 0

# 2 - Chocolate-Filled: split open, chocolate coming through the gaps.
Build-Form 'krave_monster_2.png' (C '5A3418') (C '7A4A22') (C 'E8D8C0') (C 'B09878') `
    (C 'FFAE4A') (C '32200E') @((C '7A4A22'),(C '3A2410'),(C '9A5E2A')) $null 5 (C 'C87A2E') 14

# 3 - Double Chocolate: near black, with a core burning under the shell.
Build-Form 'krave_monster_3.png' (C '2A1808') (C '3E2410') (C 'E0CBA8') (C 'A08860') `
    (C 'FFC24A') (C '1A1006') @((C '3E2410'),(C '160C04'),(C '5A3414')) (C 'FFB03A') 4 (C 'FF9E20') 22

# 4 - Krave Swarm: crawling, green-brown, far too many small eyes.
Build-Form 'krave_monster_4.png' (C '3E4A1E') (C '55632A') (C 'D8DCB8') (C '9AA278') `
    (C 'B6FF4A') (C '242C10') @((C '55632A'),(C '6E7A38'),(C '2A3414')) (C 'B6FF4A') 4 $null 0

# 5 - Milk & Chocolate Abomination: pale, bloated, marbled and wet.
Build-Form 'krave_monster_5.png' (C 'E4DCC6') (C 'F2ECDC') (C 'C8B48E') (C 'A08C68') `
    (C '6E4A22') (C '8A6E48') @((C 'C8A46E'),(C 'F2ECDC'),(C '9A7040')) (C 'FFFFFF') 3 (C '7A4E24') 16

# 6 - Krave Overload: crimson, coming apart, white-hot at every seam.
Build-Form 'krave_monster_6.png' (C '6E1414') (C '8E2020') (C 'F0D8D8') (C 'C09090') `
    (C 'FFFFFF') (C '3A0A0A') @((C '8E2020'),(C 'B02C2C'),(C '4A0E0E')) (C 'FFF0C0') 3 (C 'FFE08A') 30

# 7 - THE KRAVE GOD: void black veined with gold. Not cereal any more.
Build-Form 'krave_monster_7.png' (C '0A0812') (C '141026') (C 'F4D98A') (C 'C8A845') `
    (C 'FFE96A') (C '060410') @((C '141026'),(C '241C3E'),(C '0A0812')) (C 'FFE96A') 3 (C 'FFD24A') 26

Write-Host ""
Write-Host "seven Krave Monster forms written, all 128x128 and verified."
