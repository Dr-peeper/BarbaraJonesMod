# Duhl Wol's 64x64 skin: the debt collector. Brown work jacket, gold chain,
# permanent scowl. Modern format - left arm at (32,48), left leg at (16,48).
Add-Type -AssemblyName System.Drawing
$repoRoot = Split-Path -Parent $PSScriptRoot
$edir = "$repoRoot\src\main\resources\assets\barbarajones\textures\entity"

function C([string]$h,[int]$a=255){ [System.Drawing.Color]::FromArgb($a,
    [Convert]::ToInt32($h.Substring(0,2),16),[Convert]::ToInt32($h.Substring(2,2),16),[Convert]::ToInt32($h.Substring(4,2),16)) }
function Rct($b,$x,$y,$w,$h,$c){ for($i=0;$i -lt $w;$i++){for($j=0;$j -lt $h;$j++){
    if(($x+$i) -lt $b.Width -and ($y+$j) -lt $b.Height -and ($x+$i) -ge 0 -and ($y+$j) -ge 0){ $b.SetPixel($x+$i,$y+$j,$c) } }} }

$b = New-Object System.Drawing.Bitmap 64,64,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Rct $b 0 0 64 64 (C '000000' 0)

$skin  = C '6B4A32'; $skinD = C '523726'; $skinL = C '7E5A3E'
$coat  = C '5A4028'; $coatD = C '3F2C1B'; $coatL = C '6E5033'
$pants = C '2A2A30'; $pantsD= C '1B1B20'
$gold  = C 'D8A63A'; $boot  = C '1A1410'

# ---- head ----
Rct $b 8 0 8 8 $skinD                      # top
Rct $b 16 0 8 8 $skinD                     # bottom
Rct $b 0 8 8 8 $skin                       # right
Rct $b 8 8 8 8 $skin                       # FRONT
Rct $b 16 8 8 8 $skin                      # left
Rct $b 24 8 8 8 $skinD                     # back
# hairline + fade
Rct $b 8 8 8 2 (C '1E1512')
Rct $b 0 8 8 2 (C '1E1512'); Rct $b 16 8 8 2 (C '1E1512'); Rct $b 24 8 8 2 (C '1E1512')
# eyes, low and flat: the scowl
Rct $b 10 11 2 1 (C 'FFFFFF'); Rct $b 10 11 1 1 (C '20160E')
Rct $b 13 11 2 1 (C 'FFFFFF'); Rct $b 14 11 1 1 (C '20160E')
Rct $b 10 10 2 1 (C '20160E'); Rct $b 13 10 2 1 (C '20160E')   # heavy brow
# nose + hard mouth
Rct $b 11 12 1 2 $skinD
Rct $b 11 14 3 1 (C '3A2418')
# beard shadow along the jaw
Rct $b 9 14 1 2 $skinD; Rct $b 14 14 1 2 $skinD

# ---- body: work jacket ----
Rct $b 16 16 24 16 $coat
Rct $b 20 20 8 12 $coatD                   # open front panel
Rct $b 20 20 1 12 $coatL; Rct $b 27 20 1 12 $coatL
Rct $b 22 22 4 1 $gold                     # chain
Rct $b 23 23 2 1 $gold
Rct $b 16 30 24 2 $coatD                   # hem

# ---- arms ----
Rct $b 40 16 16 16 $coat                   # right arm
Rct $b 40 28 16 4 $skin                    # cuff -> hand
Rct $b 44 16 4 2 $coatL
Rct $b 32 48 16 16 $coat                   # left arm
Rct $b 32 60 16 4 $skin
Rct $b 36 48 4 2 $coatL

# ---- legs ----
Rct $b 0 16 16 16 $pants                   # right leg
Rct $b 0 28 16 4 $boot
Rct $b 0 16 16 1 $pantsD
Rct $b 16 48 16 16 $pants                  # left leg
Rct $b 16 60 16 4 $boot
Rct $b 16 48 16 1 $pantsD

$b.Save("$edir\duhl_wol.png",[System.Drawing.Imaging.ImageFormat]::Png)
$b.Dispose()
if(Test-Path "$edir\duhl_wol.png"){ "  OK  $edir\duhl_wol.png" } else { "  FAILED" }
