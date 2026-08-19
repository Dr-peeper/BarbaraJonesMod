# Generates the custom GUI atlases used by com.barbarajones.client.ui.KraveTheme.
#
#   krave_icons.png  64x64 - a 4x4 grid of 16px icons, index = row*4 + col
#   krave_frame.png  16x16 - a 2x2 grid of 8px corner brackets (TL TR / BL BR)
#
# Both are pure pixel art on a transparent background, drawn with System.Drawing
# exactly like the other tools/make_*.ps1 scripts. Nothing else is touched - no
# lang, no sounds.json, no models.
Add-Type -AssemblyName System.Drawing

$repoRoot = Split-Path -Parent $PSScriptRoot
$gdir = "$repoRoot\src\main\resources\assets\barbarajones\textures\gui"
if (-not (Test-Path $gdir)) { New-Item -ItemType Directory -Force $gdir | Out-Null }

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

# ---- palette (matches KraveTheme.java) -------------------------------------
$clear   = C '000000' 0
$black   = C '141014'
$boxRed  = C 'C81E24'
$boxRedD = C '7C0F14'
$boxRedL = C 'F0574A'
$purple  = C '6B3FA0'
$purpleL = C 'B07CF0'
$purpleD = C '2A1436'
$milk    = C 'F4EDDD'
$gold    = C 'E9B23C'
$goldL   = C 'FFDA7A'
$goldD   = C 'A87A1E'
$grass   = C '57B03A'
$grassD  = C '2F6B20'
$choc    = C '8A5A2A'
$chocD   = C '4A2410'
$steel   = C '9AA0AA'
$steelD  = C '5A606A'
$pink    = C 'F08CB4'
$fry     = C 'D89A34'
$fryL    = C 'F0BC58'
$fryD    = C 'A06E1E'
$ginger  = C 'E08A38'
$gingerD = C 'A85E1E'

# ============================================================================
# krave_icons.png
# ============================================================================
$icons = New-Object System.Drawing.Bitmap 64, 64, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Rct $icons 0 0 64 64 $clear

# --- 0  Krave cereal box ----------------------------------------------------
$ox = 0; $oy = 0
Rct $icons ($ox + 2) ($oy + 1) 12 14 $black
Rct $icons ($ox + 3) ($oy + 2) 10 12 $purple
Rct $icons ($ox + 3) ($oy + 2) 10 3  $boxRed
Rct $icons ($ox + 3) ($oy + 2) 10 1  $boxRedL
Rct $icons ($ox + 3) ($oy + 13) 10 1 $purpleD
# a milk K on the front
Rct $icons ($ox + 5) ($oy + 6) 1 6 $milk
Rct $icons ($ox + 9) ($oy + 6) 1 1 $milk
Rct $icons ($ox + 8) ($oy + 7) 1 1 $milk
Rct $icons ($ox + 7) ($oy + 8) 1 2 $milk
Rct $icons ($ox + 8) ($oy + 10) 1 1 $milk
Rct $icons ($ox + 9) ($oy + 11) 1 1 $milk

# --- 1  Handful of grass ----------------------------------------------------
$ox = 16; $oy = 0
Rct $icons ($ox + 7) ($oy + 4) 2 10 $grass
Rct $icons ($ox + 7) ($oy + 12) 2 2 $grassD
Rct $icons ($ox + 4) ($oy + 7) 1 6 $grass
Rct $icons ($ox + 5) ($oy + 6) 1 3 $grass
Rct $icons ($ox + 11) ($oy + 7) 1 6 $grass
Rct $icons ($ox + 10) ($oy + 6) 1 3 $grass
Rct $icons ($ox + 3) ($oy + 13) 10 2 $chocD

# --- 2  Check ---------------------------------------------------------------
$ox = 32; $oy = 0
Rct $icons ($ox + 3) ($oy + 8) 2 2 $grassD
Rct $icons ($ox + 4) ($oy + 9) 2 2 $grass
Rct $icons ($ox + 5) ($oy + 10) 2 2 $grass
Rct $icons ($ox + 6) ($oy + 9) 2 2 $grass
Rct $icons ($ox + 7) ($oy + 7) 2 2 $grass
Rct $icons ($ox + 8) ($oy + 5) 2 2 $grass
Rct $icons ($ox + 9) ($oy + 3) 2 2 $grass
Rct $icons ($ox + 10) ($oy + 2) 2 2 $grassD

# --- 3  Lock ----------------------------------------------------------------
$ox = 48; $oy = 0
Rct $icons ($ox + 5) ($oy + 3) 6 2 $steelD
Rct $icons ($ox + 4) ($oy + 4) 2 4 $steelD
Rct $icons ($ox + 10) ($oy + 4) 2 4 $steelD
Rct $icons ($ox + 3) ($oy + 7) 10 7 $black
Rct $icons ($ox + 4) ($oy + 8) 8 5 $steel
Rct $icons ($ox + 7) ($oy + 9) 2 3 $steelD

# --- 4  Chevron (an open objective) -----------------------------------------
$ox = 0; $oy = 16
Rct $icons ($ox + 5) ($oy + 3) 2 2 $gold
Rct $icons ($ox + 6) ($oy + 5) 2 2 $gold
Rct $icons ($ox + 7) ($oy + 7) 2 2 $gold
Rct $icons ($ox + 8) ($oy + 8) 2 2 $goldD
Rct $icons ($ox + 7) ($oy + 9) 2 2 $gold
Rct $icons ($ox + 6) ($oy + 11) 2 2 $gold
Rct $icons ($ox + 5) ($oy + 13) 2 2 $gold

# --- 5  Ski mask (The Plug / danger) ----------------------------------------
$ox = 16; $oy = 16
Rct $icons ($ox + 3) ($oy + 2) 10 12 $black
Rct $icons ($ox + 4) ($oy + 3) 8 10 (C '242028')
Rct $icons ($ox + 5) ($oy + 6) 3 2 $milk
Rct $icons ($ox + 9) ($oy + 6) 3 2 $milk
Rct $icons ($ox + 6) ($oy + 10) 4 1 (C '8A2A2A')

# --- 6  Star (level up) -----------------------------------------------------
$ox = 32; $oy = 16
Rct $icons ($ox + 7) ($oy + 2) 2 12 $gold
Rct $icons ($ox + 2) ($oy + 7) 12 2 $gold
Rct $icons ($ox + 4) ($oy + 4) 2 2 $gold
Rct $icons ($ox + 10) ($oy + 4) 2 2 $gold
Rct $icons ($ox + 4) ($oy + 10) 2 2 $gold
Rct $icons ($ox + 10) ($oy + 10) 2 2 $gold
Rct $icons ($ox + 6) ($oy + 6) 4 4 $goldL

# --- 7  Mr Pibb -------------------------------------------------------------
$ox = 48; $oy = 16
Rct $icons ($ox + 4) ($oy + 2) 8 12 $black
Rct $icons ($ox + 5) ($oy + 3) 6 10 (C '8B1A1A')
Rct $icons ($ox + 5) ($oy + 3) 6 1 $steel
Rct $icons ($ox + 5) ($oy + 6) 6 3 (C 'D8B24A')
Rct $icons ($ox + 5) ($oy + 12) 6 1 (C '5A0F0F')

# --- 8  Chicken nugget ------------------------------------------------------
$ox = 0; $oy = 32
Rct $icons ($ox + 4) ($oy + 5) 8 6 $fry
Rct $icons ($ox + 3) ($oy + 6) 10 4 $fry
Rct $icons ($ox + 4) ($oy + 5) 8 2 $fryL
Rct $icons ($ox + 4) ($oy + 10) 8 1 $fryD
Rct $icons ($ox + 6) ($oy + 7) 2 1 (C 'F5D48C')

# --- 9  Donut ---------------------------------------------------------------
$ox = 16; $oy = 32
Rct $icons ($ox + 3) ($oy + 5) 10 6 $choc
Rct $icons ($ox + 4) ($oy + 4) 8 8 $choc
Rct $icons ($ox + 4) ($oy + 4) 8 4 $pink
Rct $icons ($ox + 3) ($oy + 5) 10 3 $pink
Rct $icons ($ox + 6) ($oy + 6) 4 4 $clear
Rct $icons ($ox + 5) ($oy + 5) 1 1 $milk
Rct $icons ($ox + 10) ($oy + 6) 1 1 $grass
Rct $icons ($ox + 8) ($oy + 4) 1 1 $gold

# --- 10  Backwards red hat --------------------------------------------------
$ox = 32; $oy = 32
Rct $icons ($ox + 4) ($oy + 4) 10 6 $boxRed
Rct $icons ($ox + 4) ($oy + 4) 10 1 $boxRedL
Rct $icons ($ox + 3) ($oy + 10) 11 2 $boxRedD
Rct $icons ($ox + 0) ($oy + 9) 4 2 $boxRedD   # brim, pointing backwards
Rct $icons ($ox + 12) ($oy + 6) 2 4 $milk     # snapback strap at the front

# --- 11  The Krave Monster --------------------------------------------------
$ox = 48; $oy = 32
Rct $icons ($ox + 3) ($oy + 3) 10 11 $purple
Rct $icons ($ox + 3) ($oy + 3) 10 2 $purpleL
Rct $icons ($ox + 4) ($oy + 6) 8 7 $purpleD
Rct $icons ($ox + 5) ($oy + 8) 2 2 (C 'FF4A4A')
Rct $icons ($ox + 9) ($oy + 8) 2 2 (C 'FF4A4A')
Rct $icons ($ox + 6) ($oy + 11) 4 1 $milk

# --- 12  Daniel's lighter ---------------------------------------------------
$ox = 0; $oy = 48
Rct $icons ($ox + 6) ($oy + 9) 4 5 $steelD
Rct $icons ($ox + 6) ($oy + 9) 4 1 $steel
Rct $icons ($ox + 7) ($oy + 4) 2 5 (C 'FFB020')
Rct $icons ($ox + 7) ($oy + 3) 2 1 (C 'FFE070')
Rct $icons ($ox + 6) ($oy + 6) 1 3 (C 'FF6A10')
Rct $icons ($ox + 9) ($oy + 6) 1 3 (C 'FF6A10')

# --- 13  Duhl Wol's car -----------------------------------------------------
$ox = 16; $oy = 48
Rct $icons ($ox + 2) ($oy + 8) 12 4 (C '2A5AA8')
Rct $icons ($ox + 4) ($oy + 5) 7 3 (C '3A6FC8')
Rct $icons ($ox + 5) ($oy + 6) 5 2 (C 'A8D0F0')
Rct $icons ($ox + 2) ($oy + 11) 12 1 (C '18325E')
Rct $icons ($ox + 3) ($oy + 12) 3 2 $black
Rct $icons ($ox + 10) ($oy + 12) 3 2 $black
Rct $icons ($ox + 13) ($oy + 8) 1 2 (C 'FFE070')

# --- 14  The Manager's necktie ----------------------------------------------
$ox = 32; $oy = 48
Rct $icons ($ox + 5) ($oy + 2) 2 2 $milk
Rct $icons ($ox + 9) ($oy + 2) 2 2 $milk
Rct $icons ($ox + 6) ($oy + 4) 4 3 (C '8A1218')
Rct $icons ($ox + 6) ($oy + 7) 4 3 $boxRed
Rct $icons ($ox + 5) ($oy + 10) 6 3 $boxRed
Rct $icons ($ox + 6) ($oy + 13) 4 1 $boxRedD

# --- 15  Nugget the cat -----------------------------------------------------
$ox = 48; $oy = 48
Rct $icons ($ox + 3) ($oy + 2) 3 4 $ginger
Rct $icons ($ox + 10) ($oy + 2) 3 4 $ginger
Rct $icons ($ox + 4) ($oy + 3) 1 2 (C 'F0B080')
Rct $icons ($ox + 11) ($oy + 3) 1 2 (C 'F0B080')
Rct $icons ($ox + 4) ($oy + 5) 8 8 $ginger
Rct $icons ($ox + 4) ($oy + 12) 8 1 $gingerD
Rct $icons ($ox + 5) ($oy + 8) 2 2 $grassD
Rct $icons ($ox + 9) ($oy + 8) 2 2 $grassD
Rct $icons ($ox + 7) ($oy + 10) 2 1 (C 'F0A0B0')

$icons.Save("$gdir\krave_icons.png", [System.Drawing.Imaging.ImageFormat]::Png)
$icons.Dispose()
"icons written: $gdir\krave_icons.png"

# ============================================================================
# krave_frame.png - four 8x8 corner brackets
# ============================================================================
$frame = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Rct $frame 0 0 16 16 $clear

# top-left
Rct $frame 0 0 6 2 $gold
Rct $frame 0 0 2 6 $gold
Rct $frame 2 2 1 1 $milk
# top-right
Rct $frame 10 0 6 2 $gold
Rct $frame 14 0 2 6 $gold
Rct $frame 13 2 1 1 $milk
# bottom-left
Rct $frame 0 14 6 2 $gold
Rct $frame 0 10 2 6 $gold
Rct $frame 2 13 1 1 $milk
# bottom-right
Rct $frame 10 14 6 2 $gold
Rct $frame 14 10 2 6 $gold
Rct $frame 13 13 1 1 $milk

$frame.Save("$gdir\krave_frame.png", [System.Drawing.Imaging.ImageFormat]::Png)
$frame.Dispose()
"frame written: $gdir\krave_frame.png"

# ---- verify: a silent failure to write assets has burned this project before
$expected = @("$gdir\krave_icons.png", "$gdir\krave_frame.png")
$missing = 0
foreach ($p in $expected) {
    if (Test-Path $p) { "  OK  $p" } else { "  MISSING  $p"; $missing++ }
}
if ($missing -gt 0) { "make_gui_assets: $missing file(s) MISSING" } else { "make_gui_assets: all OK" }
