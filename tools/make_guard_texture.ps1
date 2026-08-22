# The village guard, as a big buff hoodlum.
#
# He was a cereal-box villager in a red apron, which is fine for a grocer and
# absurd for the man standing on the corner of a favela. The settlement is
# meant to read as improvised and slightly dangerous, and the person guarding it
# should look like he does it for the neighbourhood rather than for a council.
#
# So: bare arms with real muscle shading, a black hoodie, a gold chain, a
# bandana round the box, and tracksuit legs. The bulk comes from the renderer
# (see KraveVillagerRenderer.scale) - a texture cannot make a model wider, and
# scaling alone without the shading reads as a balloon rather than as somebody
# who lifts.
#
# UV layout is taken from KraveVillagerModel, not guessed. Every rect below is
# derived from the cube it belongs to using Minecraft's standard box unwrap:
# for a box w x h x d at texOffs(u,v) the faces are
#   top    (u+d,      v,      w x d)
#   bottom (u+d+w,    v,      w x d)
#   right  (u,        v+d,    d x h)
#   front  (u+d,      v+d,    w x h)
#   left   (u+d+w,    v+d,    d x h)
#   back   (u+d+w+d,  v+d,    w x h)
# Get this wrong and you do not get a missing texture, you get a scrambled one.

Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'

$repo = Split-Path $PSScriptRoot -Parent
$out  = Join-Path $repo 'src\main\resources\assets\barbarajones\textures\entity\krave_villager\guard.png'
if (-not (Test-Path (Split-Path $out -Parent))) { throw "villager texture folder missing" }

function C([string]$hex, [int]$a = 255) {
    [System.Drawing.Color]::FromArgb($a,
        [Convert]::ToInt32($hex.Substring(0,2),16),
        [Convert]::ToInt32($hex.Substring(2,2),16),
        [Convert]::ToInt32($hex.Substring(4,2),16))
}

$b = New-Object System.Drawing.Bitmap 64,64,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
function Px($x,$y,$col){ if ($x -ge 0 -and $x -lt 64 -and $y -ge 0 -and $y -lt 64) { $b.SetPixel([int]$x,[int]$y,$col) } }
function Rct($x,$y,$w,$h,$col){ for($i=0;$i -lt $w;$i++){ for($j=0;$j -lt $h;$j++){ Px ($x+$i) ($y+$j) $col } } }

# Deterministic grain, so the file is diffable between runs.
$script:sd = 97
function Rnd([int]$n){ $script:sd=($script:sd*1103515245+12345) -band 0x7fffffff; return $script:sd % $n }

Rct 0 0 64 64 (C '000000' 0)

# --- palette -------------------------------------------------------------
$card     = C '9A6A38'   # the cereal box he still wears for a head
$cardDark = C '6E4A24'
$bandana  = C 'B4231F'
$hoodie   = C '23232A'
$hoodDark = C '141419'
$hoodLite = C '32323C'
$skin     = C '7A4A2A'
$skinLite = C '95603A'
$skinDark = C '55321B'
$gold     = C 'E8B93C'
$track    = C '2A2E38'
$trackLt  = C '3A404E'
$shoe     = C 'E4E4E4'
$ink      = C '110D0A'

# --- head: box 9 x 9 x 6 at (0,0) ---------------------------------------
# top(6,0,9x6) bottom(15,0,9x6) right(0,6,6x9) front(6,6,9x9) left(15,6,6x9) back(21,6,9x9)
Rct 6 0 9 6 $card
Rct 15 0 9 6 $cardDark
Rct 0 6 6 9 $cardDark
Rct 6 6 9 9 $card
Rct 15 6 6 9 $cardDark
Rct 21 6 9 9 $card

# Bandana: a band right round the box, low over the brow.
foreach ($r in @(@(0,6),@(6,9),@(15,6),@(21,9))) {
    Rct $r[0] 7 $r[1] 2 $bandana
}
# Knot on the left side.
Rct 19 7 2 3 $bandana

# The face: a flat scowl. Two hard eyes under the band, mouth set.
Rct 8 11 2 1 $ink
Rct 11 11 2 1 $ink
Rct 9 14 3 1 $ink
Px 8 13 $ink
Px 12 13 $ink

# --- body: box 8 x 11 x 5 at (0,17) -------------------------------------
# top(5,17,8x5) bottom(13,17,8x5) right(0,22,5x11) front(5,22,8x11) left(13,22,5x11) back(18,22,8x11)
Rct 5 17 8 5 $hoodie
Rct 13 17 8 5 $hoodDark
Rct 0 22 5 11 $hoodDark
Rct 5 22 8 11 $hoodie
Rct 13 22 5 11 $hoodDark
Rct 18 22 8 11 $hoodie

# Hood bunched at the shoulders, lighter where it catches the light.
Rct 5 22 8 2 $hoodLite
Rct 18 22 8 2 $hoodLite
# Pocket seam and a drawstring, so the front is not a flat rectangle.
Rct 6 29 6 1 $hoodDark
Px 8 24 $hoodLite; Px 9 24 $hoodLite
Px 8 25 (C 'CFCFCF'); Px 9 25 (C 'CFCFCF')

# The chain. Two links wide, hanging in a shallow V.
foreach ($pair in @(@(6,25),@(7,26),@(8,27),@(9,27),@(10,26),@(11,25))) {
    Px $pair[0] $pair[1] $gold
}
Px 8 28 $gold; Px 9 28 $gold

# Worn patches, because nothing here is new.
for ($i = 0; $i -lt 10; $i++) { Px (5 + (Rnd 8)) (24 + (Rnd 9)) $hoodDark }

# --- arms: 3 x 10 x 3, left at (28,17), right at (42,17) ----------------
# For each: top(u+3,v,3x3) bottom(u+6,v,3x3) right(u,v+3,3x10)
#           front(u+3,v+3,3x10) left(u+6,v+3,3x10) back(u+9,v+3,3x10)
foreach ($u in 28, 42) {
    Rct ($u+3) 17 3 3 $skinLite      # shoulder cap, catching light
    Rct ($u+6) 17 3 3 $skinDark      # underside
    Rct $u     20 3 10 $skinDark
    Rct ($u+3) 20 3 10 $skin
    Rct ($u+6) 20 3 10 $skinLite
    Rct ($u+9) 20 3 10 $skin

    # A short sleeve, so the hoodie stops at the bicep and the arm is bare.
    foreach ($col in $u, ($u+3), ($u+6), ($u+9)) { Rct $col 20 3 3 $hoodie }
    Rct ($u+3) 22 3 1 $hoodLite

    # Muscle: a highlight down the outside of the bicep and forearm, a shadow
    # inside. This is what sells the bulk once the renderer scales him up.
    Px ($u+8) 24 $skinLite; Px ($u+8) 25 $skinLite; Px ($u+8) 26 $skinLite
    Px ($u+3) 24 $skinDark; Px ($u+3) 25 $skinDark
    Rct ($u+3) 27 3 1 $skinDark      # the crease at the elbow
    Px ($u+4) 29 $skinDark           # knuckles
    Px ($u+5) 29 $skinDark
}

# --- legs: 4 x 11 x 4, left at (0,34), right at (18,34) -----------------
# top(u+4,v,4x4) bottom(u+8,v,4x4) right(u,v+4,4x11)
# front(u+4,v+4,4x11) left(u+8,v+4,4x11) back(u+12,v+4,4x11)
foreach ($u in 0, 18) {
    Rct ($u+4) 34 4 4 $track
    Rct ($u+8) 34 4 4 $shoe
    Rct $u     38 4 11 $trackLt
    Rct ($u+4) 38 4 11 $track
    Rct ($u+8) 38 4 11 $trackLt
    Rct ($u+12) 38 4 11 $track

    # The stripe down the outside of the tracksuit leg.
    Rct ($u+8) 38 1 8 $shoe
    # Trainers: the bottom three rows, white, with a dark sole line.
    foreach ($col in $u, ($u+4), ($u+8), ($u+12)) {
        Rct $col 46 4 3 $shoe
        Rct $col 48 4 1 $ink
    }
}

# --- flap: 9 x 2 x 7 at (32,0). Kept cardboard - it is the box lid ------
Rct 39 0 9 7 $cardDark
Rct 48 0 9 7 $card
Rct 32 7 7 2 $cardDark
Rct 39 7 9 2 $card
Rct 48 7 7 2 $cardDark
Rct 55 7 9 2 $card

$b.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$b.Dispose()

# Verify what landed. A texture at the wrong size renders as garbage rather than
# as an error, which is much easier to miss than a crash.
$chk = New-Object System.Drawing.Bitmap $out
$ok = ($chk.Width -eq 64 -and $chk.Height -eq 64)
$face = $chk.GetPixel(9, 8)      # should be bandana red
$arm  = $chk.GetPixel(32, 25)    # should be skin, not apron
$chk.Dispose()
if (-not $ok) { throw "guard.png written at the wrong size" }
Write-Host "  OK  guard.png 64x64 - bandana $($face.R)/$($face.G)/$($face.B), arm $($arm.R)/$($arm.G)/$($arm.B)"
