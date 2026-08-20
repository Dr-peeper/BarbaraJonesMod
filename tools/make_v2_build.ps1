# Schematic & structure placement module textures.
#   item/krave_schematic.png  - a folded building plan on cream paper, chocolate-smudged
#   block/krave_core.png      - the Krave Foundation Stone: dark chocolate with an embossed K
#
# Every file is written and then read back off disk and pixel-checked. A file
# that exists but contains the wrong thing is worse than a missing one, so this
# script fails loudly rather than reporting success it did not earn.

Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
$idir = "$root\src\main\resources\assets\barbarajones\textures\item"
$bdir = "$root\src\main\resources\assets\barbarajones\textures\block"
New-Item -ItemType Directory -Force $idir, $bdir | Out-Null

function C([string]$h, [int]$a = 255) {
    [System.Drawing.Color]::FromArgb($a,
        [Convert]::ToInt32($h.Substring(0, 2), 16),
        [Convert]::ToInt32($h.Substring(2, 2), 16),
        [Convert]::ToInt32($h.Substring(4, 2), 16))
}
function NewImg($w, $h) {
    New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}
function Px($b, $x, $y, $c) {
    if ($x -ge 0 -and $y -ge 0 -and $x -lt $b.Width -and $y -lt $b.Height) { $b.SetPixel($x, $y, $c) }
}
function Rct($b, $x, $y, $w, $h, $c) {
    for ($i = 0; $i -lt $w; $i++) { for ($j = 0; $j -lt $h; $j++) { Px $b ($x + $i) ($y + $j) $c } }
}
function Save($b, $path) {
    $b.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $b.Dispose()
}

# ---------------------------------------------------------------- palette ----
$paper      = C 'F2E3C2'   # cream sheet
$paperShade = C 'DCC69C'   # fold / shading
$paperEdge  = C 'B79A66'   # sheet border
$inkDark    = C '4A2A12'   # drawn lines
$inkMid     = C '7A4A22'
$cocoa      = C '3B2415'   # dark chocolate
$cocoaLo    = C '26160C'   # chocolate shadow
$cocoaHi    = C '6B4526'   # chocolate highlight
$crumb      = C '8A5A30'   # cereal crumb
$cream      = C 'E8D2A8'   # the milky filling, and the embossed K

# ============================================================ SCHEMATIC ======
# A sheet of plan paper with a house elevation drawn on it, one dog-eared
# corner, and a thumbprint of chocolate where somebody ate while reading.
$s = NewImg 16 16

# sheet body with a border
Rct $s 2 1 12 14 $paper
Rct $s 2 1 12 1  $paperEdge
Rct $s 2 14 12 1 $paperEdge
Rct $s 2 1 1 14  $paperEdge
Rct $s 13 1 1 14 $paperEdge

# centre fold crease
for ($y = 2; $y -le 13; $y++) { Px $s 8 $y $paperShade }

# dog-eared top right corner
Px $s 12 1 $paperShade; Px $s 13 1 $paperShade; Px $s 13 2 $paperShade
Px $s 12 2 $paperEdge

# the house drawn on it: gable roof over a boxed wall with a door
Px $s 7 3 $inkDark
Px $s 6 4 $inkDark; Px $s 7 4 $inkMid; Px $s 8 4 $inkDark
Px $s 5 5 $inkDark; Px $s 6 5 $inkMid; Px $s 7 5 $inkMid; Px $s 8 5 $inkMid; Px $s 9 5 $inkDark
Px $s 4 6 $inkDark; Px $s 10 6 $inkDark
for ($x = 5; $x -le 9; $x++) { Px $s $x 6 $inkMid }
# walls
for ($y = 7; $y -le 10; $y++) { Px $s 5 $y $inkDark; Px $s 9 $y $inkDark }
for ($x = 5; $x -le 9; $x++) { Px $s $x 10 $inkDark }
# door
Px $s 7 8 $inkMid; Px $s 7 9 $inkDark; Px $s 7 10 $inkDark

# dimension ticks under the drawing, so it reads as a plan not a picture
for ($x = 4; $x -le 10; $x++) { Px $s $x 12 $paperShade }
Px $s 4 11 $paperShade; Px $s 10 11 $paperShade

# chocolate thumbprint, bottom left
Px $s 3 12 $cocoaHi; Px $s 4 13 $cocoa; Px $s 3 13 $cocoa; Px $s 5 13 $cocoaHi

Save $s "$idir\krave_schematic.png"

# ========================================================== FOUNDATION =======
# Compressed cereal and chocolate, pressed into a slab and stamped with a K.
$k = NewImg 16 16
Rct $k 0 0 16 16 $cocoa

# crumb speckle - deterministic, so re-running the script produces the same file
$seed = 0
for ($y = 0; $y -lt 16; $y++) {
    for ($x = 0; $x -lt 16; $x++) {
        $seed = (($x * 73856093) -bxor ($y * 19349663)) -band 0x7FFFFFFF
        $r = $seed % 100
        if ($r -lt 9)       { Px $k $x $y $crumb }
        elseif ($r -lt 20)  { Px $k $x $y $cocoaHi }
        elseif ($r -lt 27)  { Px $k $x $y $cocoaLo }
    }
}

# bevel: lit from the top left, like every other block in the game
Rct $k 0 0 16 1 $cocoaHi
Rct $k 0 0 1 16 $cocoaHi
Rct $k 0 15 16 1 $cocoaLo
Rct $k 15 0 1 16 $cocoaLo

# the K, embossed in cream filling, with a shadow one pixel down-right
$kRows = @(
    'X...X',
    'X..X.',
    'X.X..',
    'XX...',
    'X.X..',
    'X..X.',
    'X...X'
)
for ($row = 0; $row -lt $kRows.Length; $row++) {
    $line = $kRows[$row]
    for ($col = 0; $col -lt $line.Length; $col++) {
        if ($line[$col] -eq 'X') {
            Px $k (6 + $col) (5 + $row) $cocoaLo   # shadow first
        }
    }
}
for ($row = 0; $row -lt $kRows.Length; $row++) {
    $line = $kRows[$row]
    for ($col = 0; $col -lt $line.Length; $col++) {
        if ($line[$col] -eq 'X') {
            Px $k (5 + $col) (4 + $row) $cream
        }
    }
}

Save $k "$bdir\krave_core.png"

# ============================================================== VERIFY =======
# Read every file back off disk and check the pixels that carry the design.
# Checking only that the file exists would pass on a 16x16 of nothing.

$failures = @()

function Check([string]$path, [int]$w, [int]$h, $probes) {
    if (-not (Test-Path $path)) {
        $script:failures += "MISSING: $path"
        return
    }
    $bytes = [System.IO.File]::ReadAllBytes($path)
    $ms = New-Object System.IO.MemoryStream(, $bytes)
    $img = New-Object System.Drawing.Bitmap($ms)
    try {
        if ($img.Width -ne $w -or $img.Height -ne $h) {
            $script:failures += "SIZE: $path is $($img.Width)x$($img.Height), expected ${w}x${h}"
            return
        }
        $opaque = 0
        for ($y = 0; $y -lt $img.Height; $y++) {
            for ($x = 0; $x -lt $img.Width; $x++) {
                if ($img.GetPixel($x, $y).A -gt 0) { $opaque++ }
            }
        }
        if ($opaque -lt 20) {
            $script:failures += "EMPTY: $path has only $opaque opaque pixels"
            return
        }
        foreach ($p in $probes) {
            $got = $img.GetPixel($p.x, $p.y)
            $want = $p.c
            if ($got.R -ne $want.R -or $got.G -ne $want.G -or $got.B -ne $want.B -or $got.A -ne $want.A) {
                $script:failures += ("PIXEL: {0} at ({1},{2}) is #{3:X2}{4:X2}{5:X2}A{6:X2}, expected #{7:X2}{8:X2}{9:X2}A{10:X2} [{11}]" -f `
                    $path, $p.x, $p.y, $got.R, $got.G, $got.B, $got.A, $want.R, $want.G, $want.B, $want.A, $p.what)
            }
        }
        Write-Output ("  verified {0}  ({1}x{2}, {3} opaque px)" -f (Split-Path -Leaf $path), $img.Width, $img.Height, $opaque)
    } finally {
        $img.Dispose()
        $ms.Dispose()
    }
}

Check "$idir\krave_schematic.png" 16 16 @(
    @{ x = 0;  y = 0;  c = (C '000000' 0); what = 'transparent margin' },
    @{ x = 6;  y = 2;  c = $paper;         what = 'sheet body' },
    @{ x = 7;  y = 3;  c = $inkDark;       what = 'roof apex' },
    @{ x = 5;  y = 9;  c = $inkDark;       what = 'left wall' },
    @{ x = 7;  y = 9;  c = $inkDark;       what = 'door' },
    @{ x = 8;  y = 13; c = $paperShade;    what = 'fold crease' },
    @{ x = 3;  y = 13; c = $cocoa;         what = 'chocolate thumbprint' }
)

Check "$bdir\krave_core.png" 16 16 @(
    @{ x = 0;  y = 0;  c = $cocoaHi; what = 'top-left bevel' },
    @{ x = 15; y = 15; c = $cocoaLo; what = 'bottom-right bevel' },
    @{ x = 5;  y = 4;  c = $cream;   what = 'K upright, top' },
    @{ x = 5;  y = 10; c = $cream;   what = 'K upright, bottom' },
    @{ x = 9;  y = 4;  c = $cream;   what = 'K upper arm tip' },
    @{ x = 6;  y = 7;  c = $cream;   what = 'K junction' }
)

if ($failures.Count -gt 0) {
    Write-Output ""
    Write-Output "FAILED - $($failures.Count) problem(s):"
    $failures | ForEach-Object { Write-Output "  $_" }
    exit 1
}

Write-Output ""
Write-Output "wrote and verified: item/krave_schematic.png, block/krave_core.png"
