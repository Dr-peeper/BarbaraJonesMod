# Generates the art for the Player Abilities module (com.barbarajones.abilities):
#
#   textures/item/krave_gauntlet.png   16x16 - a chocolate-cereal-box knuckle fist
#   textures/item/laser_lens.png       16x16 - a red lens ringed in dark metal
#   textures/item/ascension_charm.png  16x16 - a gold winged pendant on a chain
#   textures/item/meteor_totem.png     16x16 - a small carved totem, fire at its crown
#   textures/item/instinct_band.png    16x16 - a silver-white wristband, one calm eye
#   textures/item/god_core.png         16x16 - a red core in a dark socket, faint glow
#
# Pure pixel art on a transparent background, drawn with System.Drawing exactly
# like the other tools/make_*.ps1 scripts. Krave's own palette (browns, cereal
# crumb, warm highlight) carries through the gauntlet and totem; the other four
# borrow their colours straight from AscensionLadder's rung tints so a player's
# own power reads as the same power on Cayden's chest.
Add-Type -AssemblyName System.Drawing

$repoRoot = Split-Path -Parent $PSScriptRoot
$idir = "$repoRoot\src\main\resources\assets\barbarajones\textures\item"
if (-not (Test-Path $idir)) { New-Item -ItemType Directory -Force $idir | Out-Null }

function C([string]$h, [int]$a = 255) {
    [System.Drawing.Color]::FromArgb($a,
        [Convert]::ToInt32($h.Substring(0, 2), 16),
        [Convert]::ToInt32($h.Substring(2, 2), 16),
        [Convert]::ToInt32($h.Substring(4, 2), 16))
}

function Rct($b, $x, $y, $w, $h, $c) {
    for ($i = 0; $i -lt $w; $i++) {
        for ($j = 0; $j -lt $h; $j++) {
            $px = $x + $i; $py = $y + $j
            if ($px -ge 0 -and $py -ge 0 -and $px -lt $b.Width -and $py -lt $b.Height) {
                $b.SetPixel($px, $py, $c)
            }
        }
    }
}

function Px($b, $x, $y, $c) { Rct $b $x $y 1 1 $c }

function NewCanvas {
    $bmp = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    Rct $bmp 0 0 16 16 (C '000000' 0)
    return $bmp
}

# ---- palette --------------------------------------------------------------
$clear   = C '000000' 0
$black   = C '0C0A0E'

# Krave cereal-box browns, for the gauntlet and the totem
$choc    = C '3A1D0C'
$brown   = C '6B3F23'
$brownL  = C '8B5A34'
$crumb   = C 'C9A06B'

$steel   = C '9AA0AA'
$steelD  = C '5F6570'
$steelL  = C 'C7CDD4'

$gold    = C 'E9B23C'
$goldL   = C 'FFDA7A'
$goldD   = C 'A97A22'

$red     = C 'C81E24'
$redL    = C 'F0574A'
$redD    = C '7C0F14'

$milk    = C 'F4EDDD'
$silver  = C 'C9C0AE'
$blue    = C '4FA8FF'

# ============================================================================
# krave_gauntlet.png - a knuckle fist: steel plate over a chocolate cereal-box glove
# ============================================================================
$g = NewCanvas
# wrist/cuff
Rct $g 5 12 6 3 $choc
Rct $g 5 12 6 1 $brownL
# glove body
Rct $g 3 6 10 7 $brown
Rct $g 3 6 10 1 $brownL
Rct $g 3 12 10 1 $choc
# crumb texture flecks - the cereal-box read
Px $g 4 8 $crumb; Px $g 9 7 $crumb; Px $g 11 10 $crumb; Px $g 6 11 $crumb; Px $g 12 8 $crumb
# steel knuckle plate across the top
Rct $g 3 5 10 2 $steel
Rct $g 3 5 10 1 $steelL
Rct $g 3 6 10 1 $steelD
# four knuckle studs
Px $g 4 4 $steelL; Px $g 4 5 $steelD
Px $g 7 4 $steelL; Px $g 7 5 $steelD
Px $g 10 4 $steelL; Px $g 10 5 $steelD
Px $g 12 4 $steelL; Px $g 12 5 $steelD
# thumb
Rct $g 1 8 2 4 $brown
Rct $g 1 8 1 4 $brownL
# a red band, the mod's cereal-box signature
Rct $g 3 9 10 1 $red
$g.Save("$idir\krave_gauntlet.png", [System.Drawing.Imaging.ImageFormat]::Png)
$g.Dispose()

# ============================================================================
# laser_lens.png - a red lens ringed in dark steel, the exact red of KraveLaser
# ============================================================================
$l = NewCanvas
# handle/socket at the base
Rct $l 6 12 4 3 $steelD
Rct $l 6 12 4 1 $steel
# outer ring
for ($a = 0; $a -lt 360; $a += 8) {
    $rad = $a * [Math]::PI / 180
    $x = [int](8 + 6 * [Math]::Cos($rad))
    $y = [int](7 + 6 * [Math]::Sin($rad))
    Px $l $x $y $steel
}
Rct $l 5 3 6 1 $steelL
# lens body
Rct $l 5 2 6 6 $choc
Rct $l 5 2 6 6 $black
for ($yy = 2; $yy -lt 8; $yy++) {
    for ($xx = 5; $xx -lt 11; $xx++) {
        $dx = $xx - 7.5; $dy = $yy - 4.5
        if (($dx * $dx + $dy * $dy) -le 8) { Px $l $xx $yy $redD }
    }
}
for ($yy = 3; $yy -lt 7; $yy++) {
    for ($xx = 6; $xx -lt 10; $xx++) {
        $dx = $xx - 7.5; $dy = $yy - 4.5
        if (($dx * $dx + $dy * $dy) -le 3.4) { Px $l $xx $yy $red }
    }
}
Px $l 6 3 $redL; Px $l 7 3 $redL
Px $l 6 4 $milk
$l.Save("$idir\laser_lens.png", [System.Drawing.Imaging.ImageFormat]::Png)
$l.Dispose()

# ============================================================================
# ascension_charm.png - a small gold winged pendant on a chain, worn not grown into
# ============================================================================
$c = NewCanvas
# chain
Px $c 7 1 $steel; Px $c 8 1 $steel
Px $c 7 2 $steelD; Px $c 8 2 $steelD
Px $c 7 3 $steel; Px $c 8 3 $steel
# wings, one either side of the pendant
Rct $c 2 6 3 1 $goldL
Rct $c 1 7 4 1 $gold
Rct $c 2 8 3 1 $goldD
Rct $c 11 6 3 1 $goldL
Rct $c 11 7 4 1 $gold
Rct $c 11 8 3 1 $goldD
# pendant body - a droplet
Rct $c 6 5 4 2 $choc
Rct $c 6 5 4 1 $brownL
Rct $c 6 6 4 2 $gold
Rct $c 7 6 2 2 $goldL
Rct $c 6 8 4 2 $goldD
Rct $c 7 10 2 2 $goldD
Px $c 7 12 $goldD
# a single blue tick at the setting, the ladder's flight-unlocked colour
Px $c 7 6 $blue
$c.Save("$idir\ascension_charm.png", [System.Drawing.Imaging.ImageFormat]::Png)
$c.Dispose()

# ============================================================================
# meteor_totem.png - a small carved totem, fire cresting its top like the sky's own
# ============================================================================
$t = NewCanvas
# base
Rct $t 5 13 6 2 $choc
Rct $t 5 13 6 1 $brownL
# shaft, carved in bands
Rct $t 6 6 4 7 $brown
Rct $t 6 6 4 1 $brownL
Rct $t 6 9 4 1 $choc
Rct $t 6 12 4 1 $choc
# a carved face - two dark eyes, cereal-crumb cheeks
Px $t 7 7 $black; Px $t 9 7 $black
Rct $t 6 10 4 1 $crumb
# crown: flame licking off the top, the apocalypse's own colours
Rct $t 6 4 4 2 $red
Rct $t 7 3 2 2 $redL
Px $t 7 2 $goldL
Px $t 8 2 $goldL
Px $t 5 5 $redD
Px $t 10 5 $redD
$t.Save("$idir\meteor_totem.png", [System.Drawing.Imaging.ImageFormat]::Png)
$t.Dispose()

# ============================================================================
# instinct_band.png - a plain silver-white wristband, one calm half-lidded eye
# ============================================================================
$i = NewCanvas
# the band, worn open at the wrist
Rct $i 3 5 10 6 $silver
Rct $i 3 5 10 1 $milk
Rct $i 3 10 10 1 $steelD
Rct $i 3 5 2 6 $steelD
Rct $i 11 5 2 6 $steelD
# the eye set into it - closed, unbothered, the whole point of the thing
Rct $i 6 7 4 1 $black
Px $i 5 7 $steelD
Px $i 10 7 $steelD
Rct $i 6 8 4 1 $steelL
$i.Save("$idir\instinct_band.png", [System.Drawing.Imaging.ImageFormat]::Png)
$i.Dispose()

# ============================================================================
# god_core.png - a red core seated in a dark socket, the God rung's colour, faint glow
# ============================================================================
$d = NewCanvas
# socket
Rct $d 3 3 10 10 $black
Rct $d 4 4 8 8 $choc
Rct $d 4 4 8 1 $brownL
# four steel clasps holding it in
Px $d 3 3 $steel; Px $d 12 3 $steel; Px $d 3 12 $steel; Px $d 12 12 $steel
# the core itself, round, hot at the centre
for ($yy = 4; $yy -lt 12; $yy++) {
    for ($xx = 4; $xx -lt 12; $xx++) {
        $dx = $xx - 7.5; $dy = $yy - 7.5
        $dist = $dx * $dx + $dy * $dy
        if ($dist -le 14) { Px $d $xx $yy $redD }
        if ($dist -le 8) { Px $d $xx $yy $red }
        if ($dist -le 3) { Px $d $xx $yy $redL }
    }
}
Px $d 6 6 $milk
Px $d 7 6 $goldL
$d.Save("$idir\god_core.png", [System.Drawing.Imaging.ImageFormat]::Png)
$d.Dispose()

# ---- verify: a silent failure to write assets has burned this project before
$expected = @(
    "$idir\krave_gauntlet.png",
    "$idir\laser_lens.png",
    "$idir\ascension_charm.png",
    "$idir\meteor_totem.png",
    "$idir\instinct_band.png",
    "$idir\god_core.png"
)
$missing = 0
foreach ($p in $expected) {
    if (Test-Path $p) {
        $img = [System.Drawing.Image]::FromFile($p)
        $ok = ($img.Width -eq 16 -and $img.Height -eq 16)
        $img.Dispose()
        if ($ok) { "  OK  $p" } else { "  BAD SIZE  $p"; $missing++ }
    } else {
        "  MISSING  $p"; $missing++
    }
}
if ($missing -gt 0) { "make_abilities: $missing file(s) MISSING or bad" }
else { "make_abilities: all OK" }
