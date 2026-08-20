# THE KRAVE MANUAL, 2.0 (com.barbarajones.v2.manual) - GUI background art.
#
#   textures/gui/manual/manual_paper.png  32x32 - tileable parchment page
#                                                  background for the content
#                                                  viewport (client.ManualScreen
#                                                  tiles it across the reading
#                                                  pane instead of a flat fill).
#   textures/gui/manual/manual_cover.png  200x40 - the contents-page banner.
#
# No new items/blocks/recipes here - this module (see ManualModule.java)
# registers nothing; the manual is still the same barbarajones:krave_manual
# item tools/make_manual.ps1 already built (icon/model/recipe/lang untouched,
# not re-run here). This script only adds the book's own background art.
#
# Idempotent - every image is painted from scratch with a fixed RNG seed, so
# re-running produces byte-identical output.
Add-Type -AssemblyName System.Drawing

$repoRoot = Split-Path -Parent $PSScriptRoot
$gdir = "$repoRoot\src\main\resources\assets\barbarajones\textures\gui\manual"
New-Item -ItemType Directory -Force $gdir | Out-Null

function C([string]$h, [int]$a = 255) {
    [System.Drawing.Color]::FromArgb($a,
        [Convert]::ToInt32($h.Substring(0, 2), 16),
        [Convert]::ToInt32($h.Substring(2, 2), 16),
        [Convert]::ToInt32($h.Substring(4, 2), 16))
}
function NewImg([int]$w, [int]$h) {
    New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}
function P($b, [int]$x, [int]$y, $c) {
    if ($x -ge 0 -and $y -ge 0 -and $x -lt $b.Width -and $y -lt $b.Height) { $b.SetPixel($x, $y, $c) }
}
function Rct($b, [int]$x, [int]$y, [int]$w, [int]$h, $c) {
    for ($i = 0; $i -lt $w; $i++) { for ($j = 0; $j -lt $h; $j++) { P $b ($x + $i) ($y + $j) $c } }
}
function Lerp([int]$a, [int]$b, [double]$t) { [int]($a + ($b - $a) * $t) }
function LerpColor($c1, $c2, [double]$t) {
    [System.Drawing.Color]::FromArgb(255, (Lerp $c1.R $c2.R $t), (Lerp $c1.G $c2.G $t), (Lerp $c1.B $c2.B $t))
}

# Deterministic noise - fixed seed keeps re-runs byte-identical.
$script:sd = 20260819
function Rnd([int]$n) { $script:sd = ($script:sd * 1103515245 + 12345) -band 0x7fffffff; (($script:sd -shr 15) % $n) }

# ---- palette (matches client.ui.KraveTheme) --------------------------------
$paperBase = C 'ECDFB6'
$paperDark = C 'D8C696'
$paperLine = C 'C8A868' 40    # much fainter: a hint of a rule, not a wire
# $paperMarg removed: a vertical margin line in a TILED texture repeats every
# 32 pixels, which is not a margin - it is a grid, and it was burying the text.
$chocDark  = C '2A1508'
$chocMid   = C '4A2410'
$bandRed   = C 'C81E24'
$bandRedD  = C '7C0F14'
$bandRedL  = C 'F0574A'
$purple    = C '3A2050'
$purpleD   = C '1E0F2A'
$gold      = C 'E9B23C'
$goldHi    = C 'FFDA7A'
$goldD     = C 'A87A1E'
$cream     = C 'F4EDDD'
$creamD    = C 'C9BC9E'

# =============================================================================
# manual_paper.png - 32x32 tileable parchment
# =============================================================================
$paper = NewImg 32 32
Rct $paper 0 0 32 32 $paperBase
for ($y = 0; $y -lt 32; $y++) {
    for ($x = 0; $x -lt 32; $x++) {
        if ((Rnd 9) -eq 0) { P $paper $x $y $paperDark }
    }
}
# One faint ruled line per tile. There were two, plus a vertical "margin" line
# at x=3 - but this texture is TILED across the whole page, so a margin line
# repeats every 32 pixels and stops being a margin: it becomes a grid. Between
# the two of them the page read as graph paper with the text buried under it.
Rct $paper 0 15 32 1 $paperLine
$paper.Save("$gdir\manual_paper.png", [System.Drawing.Imaging.ImageFormat]::Png)
$paper.Dispose()

# =============================================================================
# manual_cover.png - 200x40 contents-page banner
# =============================================================================
$cover = NewImg 200 40
for ($y = 0; $y -lt 40; $y++) {
    $t = $y / 39.0
    $rowColor = LerpColor $purpleD $bandRedD $t
    Rct $cover 0 $y 200 1 $rowColor
}
# gold top/bottom trim
Rct $cover 0 0 200 2 $gold
Rct $cover 0 0 200 1 $goldHi
Rct $cover 0 38 200 2 $gold
Rct $cover 0 39 200 1 $goldD

# scattered cereal-crumb flecks across the banner for texture
for ($i = 0; $i -lt 90; $i++) {
    $fx = Rnd 200; $fy = 4 + (Rnd 32)
    $c = if ((Rnd 2) -eq 0) { $chocMid } else { $goldD }
    P $cover $fx $fy $c
}

# an open book, left of centre: two pages meeting at a spine
$bx = 18; $by = 10
Rct $cover ($bx)      ($by + 2) 26 18 $cream
Rct $cover ($bx + 30) ($by + 2) 26 18 $cream
Rct $cover ($bx)      ($by + 1) 26 1  $creamD
Rct $cover ($bx + 30) ($by + 1) 26 1  $creamD
for ($i = 0; $i -lt 26; $i++) {
    # page curvature - top edge arcs up toward the spine
    $arc = [int][Math]::Round(2.0 * [Math]::Sin([Math]::PI * $i / 26.0))
    P $cover ($bx + $i) ($by + 1 - $arc) $creamD
    P $cover ($bx + 30 + 25 - $i) ($by + 1 - $arc) $creamD
}
Rct $cover ($bx + 26) $by 4 22 $chocDark
Rct $cover ($bx + 27) $by 2 22 $chocMid
# faint text rules on each page
for ($ly = $by + 5; $ly -lt $by + 18; $ly += 3) {
    Rct $cover ($bx + 3)  $ly 20 1 (C 'B8A87C' 140)
    Rct $cover ($bx + 33) $ly 20 1 (C 'B8A87C' 140)
}
# a gold "K" mark on the right-hand page
Rct $cover ($bx + 35) ($by + 6)  1 10 $gold
Rct $cover ($bx + 40) ($by + 6)  1 10 $gold
Rct $cover ($bx + 36) ($by + 10) 1 1 $gold
Rct $cover ($bx + 37) ($by + 9)  1 1 $gold
Rct $cover ($bx + 37) ($by + 11) 1 1 $gold
Rct $cover ($bx + 38) ($by + 8)  1 1 $gold
Rct $cover ($bx + 38) ($by + 12) 1 1 $gold
Rct $cover ($bx + 39) ($by + 7)  1 1 $gold
Rct $cover ($bx + 39) ($by + 13) 1 1 $gold

# sparkle stars scattered to the right of the book
function Spark($b, [int]$x, [int]$y, $c) {
    P $b $x $y $c; P $b ($x - 1) $y $c; P $b ($x + 1) $y $c; P $b $x ($y - 1) $c; P $b $x ($y + 1) $c
}
Spark $cover 100 9  $goldHi
Spark $cover 118 26 $gold
Spark $cover 135 13 $goldHi
Spark $cover 152 22 $gold
Spark $cover 168 9  $goldHi
Spark $cover 182 20 $gold
Spark $cover 110 32 $goldD
Spark $cover 145 6  $goldD

$cover.Save("$gdir\manual_cover.png", [System.Drawing.Imaging.ImageFormat]::Png)
$cover.Dispose()

# ---- verify: re-read every file back and check decoded size ----------------
$expect = @{ "$gdir\manual_paper.png" = @(32, 32); "$gdir\manual_cover.png" = @(200, 40) }
foreach ($path in $expect.Keys) {
    if (Test-Path $path) {
        $img = [System.Drawing.Image]::FromFile($path)
        $w = $img.Width; $h = $img.Height
        $img.Dispose()
        $want = $expect[$path]
        if ($w -eq $want[0] -and $h -eq $want[1]) {
            "OK    $path  ($w`x$h)"
        } else {
            "WRONG $path  got $w`x$h, wanted $($want[0])x$($want[1])"
        }
    } else {
        "MISSING $path"
    }
}
