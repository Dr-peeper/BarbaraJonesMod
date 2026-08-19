# Textures for Barbara's smoking kit.
#   barbara_smoke_o.png - the band wrapped around one of the O's. U runs around
#     the circumference so the wisps have to tile horizontally; V runs inner
#     edge to outer edge, so the alpha has to fade at BOTH ends or the ring
#     reads as a hard washer instead of smoke.
#   barbara_cherry.png  - the lit cherry she flicks: white-hot core out through
#     orange and ember red to a cold ash rim.
# Both are alpha-graded per pixel, so everything here goes through SetPixel
# rather than the block helpers the other scripts use.
Add-Type -AssemblyName System.Drawing
$repoRoot = Split-Path -Parent $PSScriptRoot
$edir = "$repoRoot\src\main\resources\assets\barbarajones\textures\entity"
New-Item -ItemType Directory -Force $edir | Out-Null

function NewBmp([int]$w,[int]$h){
    $b = New-Object System.Drawing.Bitmap $w,$h,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for($x=0;$x -lt $w;$x++){ for($y=0;$y -lt $h;$y++){
        $b.SetPixel($x,$y,[System.Drawing.Color]::FromArgb(0,0,0,0)) } }
    return $b
}
function Px($b,[int]$x,[int]$y,[int]$a,[int]$r,[int]$g,[int]$bl){
    if($a -lt 0){$a=0}; if($a -gt 255){$a=255}
    if($r -lt 0){$r=0}; if($r -gt 255){$r=255}
    if($g -lt 0){$g=0}; if($g -gt 255){$g=255}
    if($bl -lt 0){$bl=0}; if($bl -gt 255){$bl=255}
    $b.SetPixel($x,$y,[System.Drawing.Color]::FromArgb($a,$r,$g,$bl))
}

$written = New-Object System.Collections.Generic.List[string]
function Save($b,$path){ $b.Save($path,[System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose(); $script:written.Add($path) }

# ---------------------------------------------------------------------------
# 1. barbara_smoke_o.png - 128x32 tiling smoke band
# ---------------------------------------------------------------------------
$W = 128; $H = 32
$b = NewBmp $W $H
$tau = [Math]::PI * 2.0
for($x=0; $x -lt $W; $x++){
    $u = $x / [double]$W
    # Whole-number cycle counts only, so the left edge meets the right edge.
    $wobble = 0.10 * [Math]::Sin($tau * 2 * $u) + 0.06 * [Math]::Sin($tau * 5 * $u + 1.1)
    $thick  = 0.78 + 0.22 * [Math]::Sin($tau * 3 * $u + 0.4)
    $puff   = 0.80 + 0.20 * [Math]::Sin($tau * 7 * $u + 2.3)
    for($y=0; $y -lt $H; $y++){
        $v = ($y + 0.5) / [double]$H
        # distance from the (wobbling) centre line of the band, 0 middle .. 1 edge
        $d = [Math]::Abs(($v - 0.5 - $wobble) * 2.0) / $thick
        if($d -ge 1.0){ continue }
        $fall = 1.0 - [Math]::Pow($d, 1.7)
        $a = [int](236 * $fall * $puff)
        if($a -le 2){ continue }
        # cooler grey toward the outside, warmer near the core of the roll
        $tone = [int](196 + 44 * (1.0 - $d) - 26 * [Math]::Sin($tau * 4 * $u))
        Px $b $x $y $a $tone $tone ([int]($tone * 0.96))
    }
}
# a few brighter curls so the band is not a flat gradient in motion
for($i=0; $i -lt 90; $i++){
    $x = Get-Random -Maximum $W
    $y = 8 + (Get-Random -Maximum 16)
    $cur = $b.GetPixel($x,$y)
    if($cur.A -gt 40){ Px $b $x $y ([int]($cur.A * 0.92)) 246 246 240 }
}
Save $b "$edir\barbara_smoke_o.png"

# ---------------------------------------------------------------------------
# 2. barbara_cherry.png - 32x32 burning ember
# ---------------------------------------------------------------------------
$S = 32
$b = NewBmp $S $S
$cx = ($S - 1) / 2.0; $cy = ($S - 1) / 2.0
for($x=0; $x -lt $S; $x++){
    for($y=0; $y -lt $S; $y++){
        $dx = ($x - $cx) / $cx
        $dy = ($y - $cy) / $cy
        $r = [Math]::Sqrt($dx*$dx + $dy*$dy)
        if($r -ge 1.0){ continue }
        if($r -lt 0.22)     { $cr=255; $cg=248; $cb=214 }
        elseif($r -lt 0.42) { $cr=255; $cg=196; $cb=78 }
        elseif($r -lt 0.62) { $cr=246; $cg=116; $cb=28 }
        elseif($r -lt 0.80) { $cr=158; $cg=48;  $cb=16 }
        else                { $cr=96;  $cg=90;  $cb=84 }
        $a = [int](255 * (1.0 - [Math]::Pow($r, 2.1)))
        Px $b $x $y $a $cr $cg $cb
    }
}
# live sparks in the hot half, cold ash flecks on the rim
for($i=0; $i -lt 26; $i++){
    $x = Get-Random -Maximum $S; $y = Get-Random -Maximum $S
    $dx = ($x - $cx) / $cx; $dy = ($y - $cy) / $cy
    $r = [Math]::Sqrt($dx*$dx + $dy*$dy)
    if($r -lt 0.55){ Px $b $x $y 255 255 236 176 }
    elseif($r -lt 0.95){ Px $b $x $y 220 132 124 118 }
}
Save $b "$edir\barbara_cherry.png"

# ---------------------------------------------------------------------------
# report - a silent asset failure is the one that costs a whole build cycle
# ---------------------------------------------------------------------------
$missing = 0
foreach($p in $written){
    if(Test-Path $p){ "  OK       $p" } else { "  MISSING  $p"; $missing++ }
}
if($missing -gt 0){ Write-Error "$missing Barbara-kit asset(s) were not written." }
else { "All $($written.Count) Barbara-kit assets written." }
