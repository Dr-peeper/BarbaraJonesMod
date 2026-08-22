# The Giant Krave Box, as a cereal box rather than a crime scene.
#
# The old texture was dark blood-maroon with grey panels - it read as something
# horrible falling out of the sky rather than as a comically enormous box of
# breakfast cereal, which is the joke. Krave packaging is bright red-orange with
# a purple-black band, gold lettering and a bowl of chocolate pillows on the
# front, so that is what this draws.
#
# The horror in this mod is meant to live on the title screen and in the boss
# fight, not in the branding. A box that is frightening before anything has
# happened spends the surprise early.
#
# UV layout matches GiantKraveBoxModel: one 256x256 sheet, standard cube unwrap.

Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'

$repo = Split-Path $PSScriptRoot -Parent
$out  = Join-Path $repo 'src\main\resources\assets\barbarajones\textures\entity\giant_krave_box.png'
if (-not (Test-Path (Split-Path $out -Parent))) { throw "entity texture folder missing" }

function C([string]$hex, [int]$a = 255) {
    [System.Drawing.Color]::FromArgb($a,
        [Convert]::ToInt32($hex.Substring(0,2),16),
        [Convert]::ToInt32($hex.Substring(2,2),16),
        [Convert]::ToInt32($hex.Substring(4,2),16))
}
function Rct($g,$x,$y,$w,$h,$col){
    $br = New-Object System.Drawing.SolidBrush $col
    $g.FillRectangle($br,$x,$y,$w,$h); $br.Dispose()
}

$b = New-Object System.Drawing.Bitmap 256,256,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$g = [System.Drawing.Graphics]::FromImage($b)
$g.CompositingMode = 'SourceOver'
$g.SmoothingMode   = 'AntiAlias'
Rct $g 0 0 256 256 (C '000000' 0)

# Palette: the real packaging, not the old maroon.
$red      = C 'D8261C'      # box red
$redDark  = C 'A81810'      # shaded faces
$redLight = C 'F0432C'      # lit top
$purple   = C '2B1436'      # the brand band
$gold     = C 'F2B32B'
$cream    = C 'F6EEDC'
$choc     = C '4A2A16'
$chocLite = C '6E4022'

# --- the four side faces (front, right, back, left), 64 wide each ---
$faces = @(
    @{ x = 0;   shade = $red      },   # front
    @{ x = 64;  shade = $redDark  },   # right
    @{ x = 128; shade = $red      },   # back
    @{ x = 192; shade = $redDark  }    # left
)
foreach ($f in $faces) {
    Rct $g $f.x 64 64 128 $f.shade
    # Purple band across the middle of every face, so the box reads as branded
    # from any angle rather than only from the front.
    Rct $g $f.x 104 64 22 $purple
    # A gold rule under the band.
    Rct $g $f.x 126 64 3 $gold
}

# Top and bottom.
Rct $g 0 0 64 64 $redLight
Rct $g 64 0 64 64 $redDark

# --- front face detail: KRAVE, and a bowl of pillows ---
$font  = New-Object System.Drawing.Font('Arial Black', 10, [System.Drawing.FontStyle]::Bold)
$small = New-Object System.Drawing.Font('Arial', 5, [System.Drawing.FontStyle]::Bold)
$brG   = New-Object System.Drawing.SolidBrush $gold
$brC   = New-Object System.Drawing.SolidBrush $cream
$fmt   = New-Object System.Drawing.StringFormat
$fmt.Alignment = 'Center'

$g.DrawString('KRAVE', $font, $brG,
    (New-Object System.Drawing.RectangleF 0,106,64,20), $fmt)
$g.DrawString('CHOCOLATE', $small, $brC,
    (New-Object System.Drawing.RectangleF 0,131,64,12), $fmt)

# The bowl: cream ellipse with chocolate pillows heaped in it.
$g.FillEllipse((New-Object System.Drawing.SolidBrush $cream), 10, 66, 44, 30)
$rand = New-Object System.Random 7
foreach ($i in 0..13) {
    $px = 14 + $rand.Next(0, 36)
    $py = 70 + $rand.Next(0, 20)
    $sz = 6 + $rand.Next(0, 3)
    $g.FillEllipse((New-Object System.Drawing.SolidBrush $choc), $px, $py, $sz, $sz)
    $g.FillEllipse((New-Object System.Drawing.SolidBrush $chocLite), $px + 1, $py + 1, 2, 2)
}

$font.Dispose(); $small.Dispose(); $brG.Dispose(); $brC.Dispose(); $g.Dispose()

$b.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
$b.Dispose()

# Verify what landed rather than trusting the save - a texture at the wrong size
# renders as garbage rather than as an error, which is far easier to miss.
$check = New-Object System.Drawing.Bitmap $out
$ok = ($check.Width -eq 256 -and $check.Height -eq 256)
$corner = $check.GetPixel(10, 100)   # should be box red, not maroon
$check.Dispose()
if (-not $ok) { throw "giant_krave_box.png written at the wrong size" }
Write-Host "  OK  giant_krave_box.png 256x256, front face RGB $($corner.R)/$($corner.G)/$($corner.B)"
