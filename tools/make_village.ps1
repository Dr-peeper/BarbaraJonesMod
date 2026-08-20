# Generates every texture the KRAVE VILLAGE SYSTEM needs.
#
#   textures/entity/krave_villager/grocer.png       64x64
#   textures/entity/krave_villager/cerealogist.png  64x64
#   textures/entity/krave_villager/builder.png      64x64
#   textures/entity/krave_villager/guard.png        64x64
#   textures/entity/krave_villager/courier.png      64x64
#   textures/item/village_charter.png               16x16
#   textures/item/village_atlas.png                 16x16
#
# All original pixel art, drawn with System.Drawing exactly like the other
# tools/make_*.ps1 scripts. Palette is the mod's: cereal-box red, grape purple,
# chocolate brown, milk cream, gold. Every box surface gets a deterministic
# crumb speckle so the villagers read as cardboard-and-cereal rather than as
# flat colour.
#
# The villager UV map matches com.barbarajones.v2.village.client.KraveVillagerModel
# exactly. If a cube size changes there, the offsets below must change with it.
#
#   head  9x9x6  at ( 0,  0)      flap  9x2x7 at (32, 0)
#   body  8x11x5 at ( 0, 17)
#   armL  3x10x3 at (28, 17)      armR  3x10x3 at (42, 17)
#   legL  4x11x4 at ( 0, 34)      legR  4x11x4 at (18, 34)
#
# Nothing else is touched: no lang, no sounds.json, no models.

Add-Type -AssemblyName System.Drawing

$repoRoot = Split-Path -Parent $PSScriptRoot
$entDir = "$repoRoot\src\main\resources\assets\barbarajones\textures\entity\krave_villager"
$itemDir = "$repoRoot\src\main\resources\assets\barbarajones\textures\item"
foreach ($d in @($entDir, $itemDir)) {
    if (-not (Test-Path $d)) { New-Item -ItemType Directory -Force $d | Out-Null }
}

# ---------------------------------------------------------------- helpers ----

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

# Deterministic crumb speckle - the cereal texture. Same seed, same picture,
# every run; a texture that changes on every regeneration is a texture nobody
# can review a diff of.
function Crumbs($b, $x, $y, $w, $h, $c1, $c2, $rng, $density) {
    for ($i = 0; $i -lt $w; $i++) {
        for ($j = 0; $j -lt $h; $j++) {
            $roll = $rng.Next(100)
            if ($roll -lt $density) {
                Px $b ($x + $i) ($y + $j) $c1
            } elseif ($roll -lt ($density * 2)) {
                Px $b ($x + $i) ($y + $j) $c2
            }
        }
    }
}

# Lays out one cube in Minecraft's standard box net at (u,v).
#   top    (u+d,     v,     w, d)
#   bottom (u+d+w,   v,     w, d)
#   right  (u,       v+d,   d, h)     -X
#   front  (u+d,     v+d,   w, h)     -Z
#   left   (u+d+w,   v+d,   d, h)     +X
#   back   (u+d+w+d, v+d,   w, h)     +Z
function BoxNet($b, $u, $v, $w, $h, $d, $top, $bottom, $front, $back, $side) {
    Rct $b ($u + $d)          $v       $w $d $top
    Rct $b ($u + $d + $w)     $v       $w $d $bottom
    Rct $b  $u               ($v + $d) $d $h $side
    Rct $b ($u + $d)         ($v + $d) $w $h $front
    Rct $b ($u + $d + $w)    ($v + $d) $d $h $side
    Rct $b ($u + $d + $w + $d) ($v + $d) $w $h $back
}

# ---------------------------------------------------------------- palette ----

$clear    = C '000000' 0
$ink      = C '1A1014'
$milk     = C 'F4EDDD'
$milkDim  = C 'CFC6B2'
$boxRed   = C 'C81E24'
$boxRedD  = C '7C0F14'
$boxRedL  = C 'F0574A'
$purple   = C '6B3FA0'
$purpleL  = C 'B07CF0'
$purpleD  = C '2A1436'
$choc     = C '8A5A2A'
$chocD    = C '4A2410'
$chocL    = C 'B0793C'
$gold     = C 'E9B23C'
$goldD    = C 'A87A1E'
$goldL    = C 'FFDA7A'
$grass    = C '57B03A'
$grassD   = C '2F6B20'
$steel    = C '9AA0AA'
$steelD   = C '5A606A'
$card     = C 'C79A62'     # bare cardboard
$cardD    = C '96703F'
$cardL    = C 'E0B87E'
$bootDark = C '30231B'
$bootLite = C '4A382B'

# Per-profession: box body, box band, apron, apron trim, sleeve, boot accent
$professions = @(
    @{ name = 'grocer';      seed = 101; box = $card;   band = $boxRed;  apron = $milk;    trim = $boxRed;  sleeve = $cardL;  accent = $gold    },
    @{ name = 'cerealogist'; seed = 202; box = $purpleD;band = $purple;  apron = $milk;    trim = $purpleL; sleeve = $milkDim;accent = $purpleL },
    @{ name = 'builder';     seed = 303; box = $choc;   band = $gold;    apron = $chocD;   trim = $gold;    sleeve = $chocL;  accent = $goldL   },
    @{ name = 'guard';       seed = 404; box = $boxRedD;band = $steel;   apron = $boxRed;  trim = $steel;   sleeve = $steelD; accent = $steel   },
    @{ name = 'courier';     seed = 505; box = $cardD;  band = $grass;   apron = $grassD;  trim = $milk;    sleeve = $card;   accent = $grass   }
)

# ============================================================================
# The five villager skins
# ============================================================================

function Build-Villager($p) {
    $b = New-Object System.Drawing.Bitmap 64, 64, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    Rct $b 0 0 64 64 $clear
    $rng = New-Object System.Random $p.seed

    $box    = $p.box
    $band   = $p.band
    $apron  = $p.apron
    $trim   = $p.trim
    $sleeve = $p.sleeve
    $accent = $p.accent

    # ---- head: the cereal box, 9x9x6 at (0,0) -------------------------------
    BoxNet $b 0 0 9 9 6 $cardL $cardD $box $box $box
    # crumb grain over every head face
    Crumbs $b 0 0 30 15 $cardD $cardL $rng 7

    # front face is x6..14, y6..14
    $fx = 6; $fy = 6
    # top band, the thing that makes it a cereal box
    Rct $b $fx $fy 9 3 $band
    Rct $b $fx $fy 9 1 $milk
    Rct $b $fx ($fy + 3) 9 1 $ink
    # eye holes cut out of the carton, with a milk glint each
    Rct $b ($fx + 1) ($fy + 5) 2 2 $ink
    Rct $b ($fx + 6) ($fy + 5) 2 2 $ink
    Px  $b ($fx + 1) ($fy + 5) $milk
    Px  $b ($fx + 6) ($fy + 5) $milk
    # a milk K between the eyes, three pixels tall
    Px  $b ($fx + 4) ($fy + 5) $milk
    Px  $b ($fx + 4) ($fy + 6) $milk
    Px  $b ($fx + 4) ($fy + 7) $milk
    Px  $b ($fx + 5) ($fy + 6) $milk
    # mouth slot
    Rct $b ($fx + 3) ($fy + 8) 3 1 $chocD

    # back face x21..29 - a nutrition panel, so the back is not a blank wall
    Rct $b 22 8 7 6 $milkDim
    Rct $b 23 9 5 1 $ink
    Rct $b 23 11 5 1 $ink
    Rct $b 23 13 3 1 $ink

    # side faces get a slim band continuation so the box wraps correctly
    Rct $b 0 6 6 3 $band
    Rct $b 0 6 6 1 $milk
    Rct $b 15 6 6 3 $band
    Rct $b 15 6 6 1 $milk
    Rct $b 21 6 9 3 $band
    Rct $b 21 6 9 1 $milk

    # ---- flap: 9x2x7 at (32,0) ---------------------------------------------
    BoxNet $b 32 0 9 2 7 $cardL $cardD $box $box $cardD
    Crumbs $b 32 0 32 9 $cardD $cardL $rng 6
    # a bright crease along the fold so the flap reads as a separate panel
    Rct $b 39 7 9 1 $cardL

    # ---- body: apron, 8x11x5 at (0,17) -------------------------------------
    BoxNet $b 0 17 8 11 5 $apron $apron $apron $apron $apron
    # front is x5..12, y22..32
    Rct $b 5 22 8 2 $trim              # collar
    Rct $b 5 24 8 1 $ink
    Rct $b 6 26 6 5 $accent            # the apron bib
    Rct $b 7 27 4 3 $apron             # bib inset
    Rct $b 5 31 8 2 $chocD             # belt
    Rct $b 8 31 2 2 $gold              # buckle
    # back gets a strap cross
    Rct $b 18 24 8 1 $trim
    Rct $b 21 24 2 8 $trim
    # Chocolate crumbs, not the trim colour - on a cream apron a red speckle
    # reads as spatter rather than as breakfast.
    Crumbs $b 0 17 26 16 $chocD $choc $rng 3

    # ---- arms: 3x10x3 -------------------------------------------------------
    foreach ($ax in @(28, 42)) {
        BoxNet $b $ax 17 3 10 3 $sleeve $sleeve $sleeve $sleeve $sleeve
        # cuff and hand at the bottom of every face of the arm
        Rct $b $ax 26 12 2 $trim
        Rct $b $ax 28 12 2 $cardL
        # shoulder shading along the top
        Rct $b $ax 20 12 1 $ink
        Crumbs $b $ax 17 12 13 $chocD $choc $rng 3
    }

    # ---- legs: 4x11x4 -------------------------------------------------------
    foreach ($lx in @(0, 18)) {
        BoxNet $b $lx 34 4 11 4 $chocD $bootDark $chocD $chocD $chocD
        # trouser highlight, then the boot
        Rct $b $lx 38 16 5 $chocD
        Rct $b $lx 38 16 1 $bootLite
        Rct $b $lx 43 16 6 $bootDark
        Rct $b $lx 44 16 1 $bootLite
        Rct $b $lx 48 16 1 $ink
        Crumbs $b $lx 34 16 15 $ink $bootLite $rng 3
    }

    return $b
}

$written = @()
foreach ($p in $professions) {
    $bmp = Build-Villager $p
    $path = "$entDir\$($p.name).png"
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $written += $path
    "villager written: $path"
}

# ============================================================================
# village_charter.png - a cardboard placard with a red wax seal
# ============================================================================

$charter = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Rct $charter 0 0 16 16 $clear

# the placard, tilted by stepping the edges
Rct $charter 2 2 12 12 $ink
Rct $charter 3 3 10 10 $card
Rct $charter 3 3 10 1 $cardL
Rct $charter 3 12 10 1 $cardD
# writing lines
Rct $charter 4 5 6 1 $chocD
Rct $charter 4 7 8 1 $chocD
Rct $charter 4 9 5 1 $chocD
# wax seal, bottom right
Rct $charter 9 9 4 4 $boxRedD
Rct $charter 10 10 3 3 $boxRed
Px  $charter 10 10 $boxRedL
# a gold K stamped into the wax
Px  $charter 11 10 $gold
Px  $charter 11 11 $gold
Px  $charter 12 11 $gold
# a rolled corner at the top left, so it reads as paper not tile
Px  $charter 3 3 $clear
Px  $charter 4 3 $cardD
Px  $charter 3 4 $cardD

$charter.Save("$itemDir\village_charter.png", [System.Drawing.Imaging.ImageFormat]::Png)
$charter.Dispose()
$written += "$itemDir\village_charter.png"
"charter written: $itemDir\village_charter.png"

# ============================================================================
# village_atlas.png - a chocolate-bound book with a village stamped on it
# ============================================================================

$atlas = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Rct $atlas 0 0 16 16 $clear

# cover
Rct $atlas 2 1 12 14 $ink
Rct $atlas 3 2 10 12 $choc
Rct $atlas 3 2 10 1 $chocL
Rct $atlas 3 13 10 1 $chocD
# spine
Rct $atlas 3 2 2 12 $chocD
Rct $atlas 4 2 1 12 $chocL
# gold corner brackets
Rct $atlas 11 2 2 1 $gold
Rct $atlas 12 2 1 2 $gold
Rct $atlas 11 13 2 1 $gold
Rct $atlas 12 12 1 2 $gold
# a tiny village stamped on the cover: two roofs and a fence
Rct $atlas 6 8 3 3 $goldD
Px  $atlas 7 7 $gold
Rct $atlas 6 8 3 1 $gold
Rct $atlas 10 9 2 2 $goldD
Px  $atlas 10 8 $gold
Rct $atlas 6 11 6 1 $goldD
# red ribbon bookmark hanging out of the bottom
Rct $atlas 8 14 1 2 $boxRed
Px  $atlas 8 15 $boxRedD

$atlas.Save("$itemDir\village_atlas.png", [System.Drawing.Imaging.ImageFormat]::Png)
$atlas.Dispose()
$written += "$itemDir\village_atlas.png"
"atlas written: $itemDir\village_atlas.png"

# ============================================================================
# verify - read every file back and check it really is what we claimed.
# A silent write failure has burned this project before, and "the file exists"
# is not the same claim as "the file is a 64x64 PNG with pixels in it".
# ============================================================================

$expected = @{}
foreach ($p in $professions) { $expected["$entDir\$($p.name).png"] = @(64, 64) }
$expected["$itemDir\village_charter.png"] = @(16, 16)
$expected["$itemDir\village_atlas.png"] = @(16, 16)

$problems = 0
foreach ($path in $expected.Keys) {
    if (-not (Test-Path $path)) {
        "  MISSING   $path"
        $problems++
        continue
    }
    try {
        $check = [System.Drawing.Bitmap]::FromFile($path)
        $w = $check.Width
        $h = $check.Height
        # count non-transparent pixels, so an all-blank write is caught too
        $opaque = 0
        for ($x = 0; $x -lt $w; $x++) {
            for ($y = 0; $y -lt $h; $y++) {
                if ($check.GetPixel($x, $y).A -gt 0) { $opaque++ }
            }
        }
        $check.Dispose()
        $wantW = $expected[$path][0]
        $wantH = $expected[$path][1]
        if ($w -ne $wantW -or $h -ne $wantH) {
            "  WRONG SIZE  $path is ${w}x${h}, expected ${wantW}x${wantH}"
            $problems++
        } elseif ($opaque -lt 40) {
            "  EMPTY     $path has only $opaque opaque pixels"
            $problems++
        } else {
            "  OK  ${w}x${h}  $opaque px  $path"
        }
    } catch {
        "  UNREADABLE  $path : $_"
        $problems++
    }
}

if ($problems -gt 0) {
    "make_village: $problems problem(s) - textures are NOT complete"
    exit 1
} else {
    "make_village: all $($expected.Count) textures written and verified"
}
