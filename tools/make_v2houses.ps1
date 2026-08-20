# Item icons for the ten village-building schematics (com.barbarajones.v2.houses):
# a rolled parchment blueprint, tied off with a ribbon whose colour marks the
# tier group (plain twine -> red -> blue -> gold), with a tiny ink-sketched
# silhouette of that building's roofline so the ten read as a progression even
# as a row of inventory icons. Same System.Drawing pixel-art style as the rest
# of tools/make_*.ps1: browns and warm cereal-crumb highlights, no flat colour
# fills. Idempotent - safe to re-run.

Add-Type -AssemblyName System.Drawing
$repoRoot = Split-Path -Parent $PSScriptRoot
$res  = "$repoRoot\src\main\resources"
$idir = "$res\assets\barbarajones\textures\item"
New-Item -ItemType Directory -Force $idir | Out-Null

function C([string]$h,[int]$a=255){ [System.Drawing.Color]::FromArgb($a,
    [Convert]::ToInt32($h.Substring(0,2),16),
    [Convert]::ToInt32($h.Substring(2,2),16),
    [Convert]::ToInt32($h.Substring(4,2),16)) }
function Icon(){ New-Object System.Drawing.Bitmap 16,16,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function P($b,[int]$x,[int]$y,$c){
    if($x -ge 0 -and $y -ge 0 -and $x -lt $b.Width -and $y -lt $b.Height){ $b.SetPixel($x,$y,$c) } }
function Rct($b,[int]$x,[int]$y,[int]$w,[int]$h,$c){
    for($i=0;$i -lt $w;$i++){ for($j=0;$j -lt $h;$j++){ P $b ($x+$i) ($y+$j) $c } } }
function Outl($b,[int]$x,[int]$y,[int]$w,[int]$h,$c){
    Rct $b $x $y $w 1 $c; Rct $b $x ($y+$h-1) $w 1 $c
    Rct $b $x $y 1 $h $c; Rct $b ($x+$w-1) $y 1 $h $c }
function SaveAt($b,$path){ $b.Save($path,[System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }

# Deterministic per-pixel paper grain - fixed seed keeps re-runs byte-identical.
$script:sd = 41207
function Noise(){ $script:sd = ($script:sd*1103515245 + 12345) -band 0x7fffffff; return ($script:sd -shr 16) % 3 }

$paper    = C 'e8d3a0'
$paperLo  = C 'cdb377'
$paperHi  = C 'f5e7bd'
$ink      = C '4a2e17'
$inkFaint = C '6a4a2e'

function New-Parchment(){
    $b = Icon
    # a slightly rolled scroll: full body plus curled ends
    Rct $b 1 2 14 12 $paper
    Rct $b 0 3 1 10 $paperLo
    Rct $b 15 3 1 10 $paperLo
    Rct $b 1 1 14 1 $paperHi
    Rct $b 1 14 14 1 $paperLo
    for ($x = 1; $x -lt 15; $x++) {
        for ($y = 2; $y -lt 14; $y++) {
            $n = Noise
            if ($n -eq 0) { P $b $x $y $paperHi }
            elseif ($n -eq 2) { P $b $x $y $paperLo }
        }
    }
    return $b
}

function Add-Ribbon($b, $ribbon, $ribbonLo){
    Rct $b 1 9 14 2 $ribbon
    Rct $b 1 9 14 1 $ribbonLo
    Rct $b 6 8 4 4 $ribbon
    P $b 7 10 $ribbonLo; P $b 9 10 $ribbonLo
}

$twine = C '9c7a4a'
$twineLo = C '7a5c34'
$red = C 'b4392c'; $redLo = C '852819'
$blue = C '3b6ea5'; $blueLo = C '294c78'
$gold = C 'd9a83b'; $goldLo = C 'a87a1f'

function Finish($b, $path){
    SaveAt $b $path
    if (-not (Test-Path $path)) { Write-Error "FAILED: $path not written."; exit 1 }
    $chk = New-Object System.Drawing.Bitmap $path
    if ($chk.Width -ne 16 -or $chk.Height -ne 16) { Write-Error "FAILED: $path wrong size."; $chk.Dispose(); exit 1 }
    if ($chk.GetPixel(7,4).A -eq 0) { Write-Error "FAILED: $path looks blank where the sketch should be."; $chk.Dispose(); exit 1 }
    $chk.Dispose()
    Write-Host "OK: $path"
}

# ---- 1. Lean-To: a single lean slash, the plainest sketch possible ---------
$b = New-Parchment
Add-Ribbon $b $twine $twineLo
P $b 5 6 $ink; P $b 6 5 $ink; P $b 7 4 $ink; P $b 8 3 $ink
P $b 5 5 $ink; P $b 9 3 $inkFaint
Finish $b "$idir\schematic_lean_to.png"

# ---- 2. Small House: a tiny house glyph ------------------------------------
$b = New-Parchment
Add-Ribbon $b $twine $twineLo
P $b 6 5 $ink; P $b 7 4 $ink; P $b 8 4 $ink; P $b 9 5 $ink
Rct $b 6 5 4 3 $inkFaint
Outl $b 6 5 4 3 $ink
Finish $b "$idir\schematic_small_house.png"

# ---- 3. Cottage: house with a window and a chimney puff --------------------
$b = New-Parchment
Add-Ribbon $b $twine $twineLo
P $b 5 5 $ink; P $b 6 4 $ink; P $b 7 3 $ink; P $b 8 4 $ink; P $b 9 5 $ink
Rct $b 5 5 5 3 $inkFaint
Outl $b 5 5 5 3 $ink
P $b 7 6 $paper
P $b 9 3 $inkFaint; P $b 9 2 $inkFaint
Finish $b "$idir\schematic_cottage.png"

# ---- 4. Two-Storey House: a taller box, two storeys of windows -------------
$b = New-Parchment
Add-Ribbon $b $red $redLo
P $b 6 4 $ink; P $b 7 3 $ink; P $b 8 4 $ink
Rct $b 6 4 3 4 $inkFaint
Outl $b 6 4 3 4 $ink
P $b 7 5 $paper; P $b 7 7 $paper
Finish $b "$idir\schematic_two_storey_house.png"

# ---- 5. Ranch: low and wide, a shallow roof --------------------------------
$b = New-Parchment
Add-Ribbon $b $red $redLo
P $b 4 5 $ink; P $b 5 4 $ink; P $b 6 4 $ink; P $b 7 4 $ink; P $b 8 4 $ink; P $b 9 4 $ink; P $b 10 5 $ink
Rct $b 4 5 7 2 $inkFaint
Outl $b 4 5 7 2 $ink
Finish $b "$idir\schematic_ranch.png"

# ---- 6. Longhouse: long, low, a steep A-frame ------------------------------
$b = New-Parchment
Add-Ribbon $b $red $redLo
P $b 3 6 $ink; P $b 4 5 $ink; P $b 5 4 $ink; P $b 6 3 $ink
P $b 7 3 $ink; P $b 8 4 $ink; P $b 9 5 $ink; P $b 10 6 $ink
Rct $b 3 6 8 2 $inkFaint
Finish $b "$idir\schematic_longhouse.png"

# ---- 7. Manor: a house flanked by two small turret triangles --------------
$b = New-Parchment
Add-Ribbon $b $blue $blueLo
P $b 6 4 $ink; P $b 7 3 $ink; P $b 8 4 $ink
Rct $b 6 4 3 3 $inkFaint
Outl $b 6 4 3 3 $ink
P $b 4 3 $inkFaint; P $b 4 4 $ink; P $b 4 5 $ink
P $b 10 3 $inkFaint; P $b 10 4 $ink; P $b 10 5 $ink
Finish $b "$idir\schematic_manor.png"

# ---- 8. Tower House: tall and thin, a notched battlement top --------------
$b = New-Parchment
Add-Ribbon $b $blue $blueLo
Rct $b 6 3 4 5 $inkFaint
Outl $b 6 3 4 5 $ink
P $b 6 2 $ink; P $b 9 2 $ink
Finish $b "$idir\schematic_tower_house.png"

# ---- 9. Great Hall: a big gable with a round window in the peak -----------
$b = New-Parchment
Add-Ribbon $b $gold $goldLo
P $b 4 6 $ink; P $b 5 5 $ink; P $b 6 4 $ink; P $b 7 3 $ink
P $b 8 3 $ink; P $b 9 4 $ink; P $b 10 5 $ink; P $b 11 6 $ink
Rct $b 4 6 8 2 $inkFaint
P $b 7 4 $gold; P $b 8 4 $gold
Finish $b "$idir\schematic_great_hall.png"

# ---- 10. Krave Mansion: house with two towers and a little flag ------------
$b = New-Parchment
Add-Ribbon $b $gold $goldLo
Rct $b 3 4 2 4 $inkFaint
Outl $b 3 4 2 4 $ink
Rct $b 11 4 2 4 $inkFaint
Outl $b 11 4 2 4 $ink
P $b 6 3 $ink; P $b 7 2 $ink; P $b 8 2 $ink; P $b 9 3 $ink
Rct $b 6 3 4 4 $inkFaint
Outl $b 6 3 4 4 $ink
P $b 7 1 $gold; P $b 8 1 $gold
Finish $b "$idir\schematic_krave_mansion.png"

Write-Host "Done: 10 house schematic icons written to $idir"
