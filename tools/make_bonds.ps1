# Texture for the FEEDING/BREEDING/COMPANION BOND module (com.barbarajones.v2.bonds):
# one item icon, the Krave Cloning Box - a cereal box drawn in the same
# System.Drawing pixel-art style as the rest of tools/make_*.ps1, browns and
# warm cereal-crumb highlights per the mod's Krave palette, with a torn-open
# flap and a "x2" burst so it reads as the cloning gag at 16x16 without
# needing a tooltip. Idempotent - safe to re-run; every pixel is repainted
# from scratch each time.
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

# Deterministic per-pixel crumb noise - fixed seed keeps re-runs byte-identical.
$script:sd = 61829
function Noise() {
    $script:sd = ($script:sd * 1103515245 + 12345) -band 0x7fffffff
    return ($script:sd -shr 16) % 3
}

$cardboard   = C "8a5a34"
$cardboardLo = C "6e4426"
$cardboardHi = C "a8734a"
$band        = C "c23a2b"
$bandLo      = C "8f281d"
$cream       = C "f2d9a8"
$chocolate   = C "3b2413"
$flapShadow  = C "4a2e17" 140
$burstYellow = C "ffd23f"
$burstOrange = C "d97a1f"

function Draw-KraveFamilyBox() {
    $b = Icon
    # cardboard body
    Rct $b 1 2 14 13 $cardboard
    # crumb-texture noise on the body, warm highlights and low spots only -
    # never a flat block of colour.
    for ($x = 1; $x -lt 15; $x++) {
        for ($y = 2; $y -lt 15; $y++) {
            $n = Noise
            if ($n -eq 0) { P $b $x $y $cardboardHi }
            elseif ($n -eq 2) { P $b $x $y $cardboardLo }
        }
    }
    Outl $b 1 2 14 13 $cardboardLo

    # the red band, front-of-box branding stripe
    Rct $b 1 6 14 4 $band
    Rct $b 1 6 14 1 $bandLo
    Rct $b 1 9 14 1 $bandLo
    # cream label patch inside the band
    Rct $b 3 7 10 2 $cream
    Rct $b 4 7 2 1 $chocolate
    Rct $b 7 7 2 1 $chocolate
    Rct $b 10 7 2 1 $chocolate

    # torn-open top flap hanging loose - the box did not survive this
    Rct $b 2 1 5 2 $cardboardHi
    Rct $b 2 1 5 1 $cardboardLo
    for ($x = 2; $x -lt 7; $x++) { if ((Noise) -eq 0) { P $b $x 2 $flapShadow } }

    # a few loose cereal crumbs spilling out the tear
    P $b 5 0 $chocolate
    P $b 6 1 $chocolate
    P $b 9 0 $cardboardHi

    # the "x2" burst - what actually reads at 16x16 as "this makes another one"
    P $b 12 1 $burstYellow
    P $b 13 1 $burstOrange
    P $b 12 2 $burstOrange
    P $b 11 12 $burstYellow
    P $b 12 13 $burstYellow
    P $b 13 12 $burstOrange
    P $b 12 12 $burstOrange
    P $b 13 13 $burstYellow

    return $b
}

$path = "$idir\krave_cloning_box.png"
SaveAt (Draw-KraveFamilyBox) $path

# ---------------------------------------------------------------- verify
if (-not (Test-Path $path)) {
    Write-Error "FAILED: $path was not written."
    exit 1
}
$check = New-Object System.Drawing.Bitmap $path
if ($check.Width -ne 16 -or $check.Height -ne 16) {
    Write-Error "FAILED: $path is $($check.Width)x$($check.Height), expected 16x16."
    $check.Dispose()
    exit 1
}
$px = $check.GetPixel(1,6)
if ($px.A -eq 0) {
    Write-Error "FAILED: $path looks fully transparent where the band should be."
    $check.Dispose()
    exit 1
}
$check.Dispose()
Write-Host "OK: wrote and verified $path"
