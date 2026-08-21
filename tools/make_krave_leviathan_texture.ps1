# Krave Leviathan texture: a procedural paint job in the exact palette
# sampled off the Kravajo texture (#8E5F26/#A8752F/#6E4718/#D6A455/#FFF0C0),
# not a recolored vanilla source - there is no real-world "giant sea slug"
# texture to start from. Countershaded like the real Glaucus atlanticus it's
# modeled after (paler underside, darker back), just in this mod's cereal-
# brown palette instead of the real animal's blue/silver.
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
$bdir = "$root\src\main\resources\assets\barbarajones\textures\entity"
New-Item -ItemType Directory -Force $bdir | Out-Null

$w = 128
$h = 128

function ToRgb([string]$hex){ [Convert]::ToInt32($hex.Substring(0,2),16),[Convert]::ToInt32($hex.Substring(2,2),16),[Convert]::ToInt32($hex.Substring(4,2),16) }

$dark   = ToRgb '6E4718'
$mid    = ToRgb '8E5F26'
$tan    = ToRgb 'A8752F'
$light  = ToRgb 'D6A455'
$cream  = ToRgb 'FFF0C0'

$script:sd = 4242
function Noise() {
    $script:sd = ($script:sd * 1103515245 + 12345) -band 0x7fffffff
    return ($script:sd % 1000) / 1000.0
}

function Lerp($a, $b, $t) {
    return [int]($a + ($b - $a) * $t)
}

function BlendColor($c1, $c2, $t) {
    return (Lerp $c1[0] $c2[0] $t), (Lerp $c1[1] $c2[1] $t), (Lerp $c1[2] $c2[2] $t)
}

$bmp = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)

# Y in the model's own UV layout runs roughly: 0-20 body segments (back
# half toward the top of the sheet, belly implied lower per-row), 20-90 the
# rest of the segments, 90-128 the cerata fans. Rather than hand-map every
# named region exactly - this thing is never seen up close, see
# KraveLeviathanModel's own doc comment - every row gets the same
# top-to-bottom countershading treatment (light rows near the top third of
# each cube's V range read as the underside, dark rows the back), which
# looks right regardless of which specific box a given pixel lands on.
for ($y = 0; $y -lt $h; $y++) {
    # Countershading band: darker at the very top (the back, y small within
    # each part's own box), lighter toward the middle/bottom.
    $bandT = [Math]::Sin(($y % 20) / 20.0 * [Math]::PI)
    for ($x = 0; $x -lt $w; $x++) {
        $n = Noise
        $mottle = $n * 0.35

        $base = BlendColor $dark $tan ([Math]::Max(0.0, [Math]::Min(1.0, $bandT + $mottle)))
        # A thin lighter belly/highlight seam roughly a third of the way
        # down each 20px band - reads as a soft racing stripe along the
        # underside from a distance.
        $seam = ($y % 20)
        if ($seam -ge 8 -and $seam -le 11) {
            $base = BlendColor $base $light 0.5
        }
        if (($n) -gt 0.94) {
            $base = BlendColor $base $cream 0.6
        }
        $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255, $base[0], $base[1], $base[2]))
    }
}

$bmp.Save("$bdir\krave_leviathan.png", [System.Drawing.Imaging.ImageFormat]::Png)
"wrote krave_leviathan.png ($w x $h, Kravajo palette, procedural)"
