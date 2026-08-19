# Generates the art for Cayden's upgrade system.
#
#   textures/item/ascension_ledger.png   16x16 - the spiral notebook that opens
#                                               his upgrade screen
#
# Pure pixel art on a transparent background, drawn with System.Drawing exactly
# like the other tools/make_*.ps1 scripts. Nothing else is touched - no lang, no
# sounds.json, no models.
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

# ---- palette (matches KraveTheme.java and AscensionLadder's rung colours) ----
$clear   = C '000000' 0
$black   = C '141014'
$boxRed  = C 'C81E24'
$boxRedL = C 'F0574A'
$cover   = C '2A1436'
$coverL  = C '3D2049'
$paper   = C 'F4EDDD'
$paperD  = C 'C9C0AE'
$gold    = C 'E9B23C'
$goldL   = C 'FFDA7A'
$blue    = C '4FA8FF'
$steel   = C '9AA0AA'

# ============================================================================
# ascension_ledger.png - a battered spiral notebook, gold star on a purple cover
# ============================================================================
$led = New-Object System.Drawing.Bitmap 16, 16, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Rct $led 0 0 16 16 $clear

# page block behind the cover, so it reads as a notebook and not a card
Rct $led 4 1 10 14 $black
Rct $led 5 2 8 12 $paper
Rct $led 12 2 1 12 $paperD

# the cover itself, laid over the left three quarters
Rct $led 3 1 9 14 $black
Rct $led 4 2 7 12 $cover
Rct $led 4 2 7 1  $coverL
Rct $led 4 13 7 1 $boxRedL

# the red band across the top - the cereal-box motif every UI in this mod uses
Rct $led 4 3 7 2 $boxRed
Rct $led 4 3 7 1 $boxRedL

# spiral binding down the left edge
for ($y = 2; $y -lt 14; $y += 3) {
    Rct $led 2 $y 3 1 $steel
    Rct $led 2 $y 1 1 $black
}

# a gold star on the cover: the rung marker from the upgrade screen
Rct $led 7 7  1 1 $goldL
Rct $led 6 8  3 1 $gold
Rct $led 5 9  5 1 $gold
Rct $led 6 10 3 1 $gold
Rct $led 6 11 1 1 $gold
Rct $led 8 11 1 1 $gold
Rct $led 7 9  1 1 $goldL

# one blue tick on the pages: the form he has already learned
Rct $led 12 6 1 1 $blue
Rct $led 13 5 1 1 $blue

$led.Save("$idir\ascension_ledger.png", [System.Drawing.Imaging.ImageFormat]::Png)
$led.Dispose()
"ledger written: $idir\ascension_ledger.png"

# ---- verify: a silent failure to write assets has burned this project before
$expected = @("$idir\ascension_ledger.png")
$missing = 0
foreach ($p in $expected) {
    if (Test-Path $p) { "  OK  $p" } else { "  MISSING  $p"; $missing++ }
}
if ($missing -gt 0) { "make_ascension_assets: $missing file(s) MISSING" }
else { "make_ascension_assets: all OK" }
