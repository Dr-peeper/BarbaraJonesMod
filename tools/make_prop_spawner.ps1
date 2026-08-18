# Icon for the Krave Prop Spawner: a purple summoning wand with a star tip.
Add-Type -AssemblyName System.Drawing
$repoRoot = Split-Path -Parent $PSScriptRoot
$idir = "$repoRoot\src\main\resources\assets\barbarajones\textures\item"
$mdir = "$repoRoot\src\main\resources\assets\barbarajones\models\item"

function C([string]$h,[int]$a=255){ [System.Drawing.Color]::FromArgb($a,
    [Convert]::ToInt32($h.Substring(0,2),16),[Convert]::ToInt32($h.Substring(2,2),16),[Convert]::ToInt32($h.Substring(4,2),16)) }
function Rct($b,$x,$y,$w,$h,$c){ for($i=0;$i -lt $w;$i++){for($j=0;$j -lt $h;$j++){
    if(($x+$i) -lt $b.Width -and ($y+$j) -lt $b.Height -and ($x+$i) -ge 0 -and ($y+$j) -ge 0){ $b.SetPixel($x+$i,$y+$j,$c) } }} }

$b = New-Object System.Drawing.Bitmap 16,16,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Rct $b 0 0 16 16 (C '000000' 0)
# shaft, running corner to corner
for($i=0;$i -lt 9;$i++){ Rct $b (2+$i) (13-$i) 1 1 (C '5A3A22'); Rct $b (3+$i) (13-$i) 1 1 (C '7A5232') }
# star head
Rct $b 10 2 4 4 (C '3A1E6E')
Rct $b 11 1 2 6 (C '5A3A9A')
Rct $b 9 3 6 2 (C '5A3A9A')
Rct $b 11 3 2 2 (C 'B060D0')
Rct $b 11 2 1 1 (C 'FFFFFF'); Rct $b 12 4 1 1 (C 'FFFFFF')
$b.Save("$idir\krave_prop_spawner.png",[System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose()

$model = '{' + [Environment]::NewLine + '  "parent": "minecraft:item/generated",' + [Environment]::NewLine + '  "textures": { "layer0": "barbarajones:item/krave_prop_spawner" }' + [Environment]::NewLine + '}'
[System.IO.File]::WriteAllText("$mdir\krave_prop_spawner.json", $model, (New-Object System.Text.UTF8Encoding($false)))

foreach($p in @("$idir\krave_prop_spawner.png","$mdir\krave_prop_spawner.json")){
    if(Test-Path $p){ "  OK  $p" } else { "  MISSING  $p" } }
