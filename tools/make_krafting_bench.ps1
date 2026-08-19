# Krafting Bench: block textures (top/side) and the container GUI background.
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
$bdir = "$root\src\main\resources\assets\barbarajones\textures\block"
$gdir = "$root\src\main\resources\assets\barbarajones\textures\gui\container"
New-Item -ItemType Directory -Force $bdir,$gdir | Out-Null

function C([string]$h,[int]$a=255){ [System.Drawing.Color]::FromArgb($a,
    [Convert]::ToInt32($h.Substring(0,2),16),[Convert]::ToInt32($h.Substring(2,2),16),[Convert]::ToInt32($h.Substring(4,2),16)) }
function NewImg($w,$h){ New-Object System.Drawing.Bitmap $w,$h,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function Rct($b,$x,$y,$w,$h,$c){ for($i=0;$i -lt $w;$i++){for($j=0;$j -lt $h;$j++){
    if(($x+$i) -lt $b.Width -and ($y+$j) -lt $b.Height -and ($x+$i) -ge 0 -and ($y+$j) -ge 0){ $b.SetPixel($x+$i,$y+$j,$c) } }} }
function Save($b,$path){ $b.Save($path,[System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }

$boxRed = C 'C62828'; $boxDeep = C '7A1414'; $boxLight = C 'FFE9D2'; $boxGold = C 'FFD23F'

# ---- top: red field with a 3x3 "crafting surface" grid inset -----------------
$top = NewImg 16 16
Rct $top 0 0 16 16 $boxRed
Rct $top 0 0 16 1 $boxGold; Rct $top 0 15 16 1 $boxGold
Rct $top 0 0 1 16 $boxGold; Rct $top 15 0 1 16 $boxGold
Rct $top 3 3 10 10 $boxDeep
for($i=0;$i -lt 3;$i++){ for($j=0;$j -lt 3;$j++){
    Rct $top (4+$i*3) (4+$j*3) 2 2 $boxRed
}}
Save $top "$bdir\krafting_bench_top.png"

# ---- side: red box branding, simplified (block-texture resolution) -----------
$side = NewImg 16 16
Rct $side 0 0 16 16 $boxRed
Rct $side 0 0 16 3 $boxDeep
Rct $side 1 1 14 1 $boxLight
Rct $side 12 11 3 3 $boxGold
Rct $side 0 13 16 3 $boxDeep
Save $side "$bdir\krafting_bench_side.png"

# ---- GUI background: 176x166, vanilla-proportioned slot recesses -------------
$panel = C 'C6C6C6'; $panelDark = C '8B8B8B'; $panelLight = C 'FFFFFF' 90; $slotBg = C '8B8B8B'; $slotShadow = C '373737'
$gui = NewImg 176 166
Rct $gui 0 0 176 166 $panel
Rct $gui 0 0 176 1 $panelLight; Rct $gui 0 0 1 166 $panelLight
Rct $gui 0 165 176 1 $panelDark; Rct $gui 175 0 1 166 $panelDark

function DrawSlot($b,$x,$y){
    Rct $b $x $y 18 18 $slotShadow
    Rct $b ($x+1) ($y+1) 16 16 $slotBg
}

# the three restricted inputs + the output, matching KraftingBenchMenu's coords
DrawSlot $gui 43 16
DrawSlot $gui 61 16
DrawSlot $gui 52 34
DrawSlot $gui 115 25

# player inventory: 3 rows + hotbar, standard layout
for($row=0;$row -lt 3;$row++){ for($col=0;$col -lt 9;$col++){ DrawSlot $gui (7+$col*18) (83+$row*18) } }
for($col=0;$col -lt 9;$col++){ DrawSlot $gui (7+$col*18) 141 }

Save $gui "$gdir\krafting_bench.png"
Write-Output "wrote krafting_bench_top, krafting_bench_side, krafting_bench (gui)"
