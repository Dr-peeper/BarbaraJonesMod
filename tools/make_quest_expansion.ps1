# Icons for the four items the expansion quest board asks you to craft:
#   stash_jar, pocket_scale, chocolate_bar, nugget_collar
# Writes both the 16x16 texture and the matching item model, then reports on every file.
Add-Type -AssemblyName System.Drawing
$repoRoot = Split-Path -Parent $PSScriptRoot
$idir = "$repoRoot\src\main\resources\assets\barbarajones\textures\item"
$mdir = "$repoRoot\src\main\resources\assets\barbarajones\models\item"

function C([string]$h,[int]$a=255){ [System.Drawing.Color]::FromArgb($a,
    [Convert]::ToInt32($h.Substring(0,2),16),[Convert]::ToInt32($h.Substring(2,2),16),[Convert]::ToInt32($h.Substring(4,2),16)) }
function Rct($b,$x,$y,$w,$h,$c){ for($i=0;$i -lt $w;$i++){for($j=0;$j -lt $h;$j++){
    if(($x+$i) -lt $b.Width -and ($y+$j) -lt $b.Height -and ($x+$i) -ge 0 -and ($y+$j) -ge 0){ $b.SetPixel($x+$i,$y+$j,$c) } }} }

# ---- the drawings -------------------------------------------------------------

$draw = @{}

# Mason jar packed with cured grass. Steel lid, glass body, three clumps inside.
$draw['stash_jar'] = {
    param($b)
    Rct $b 5 0 6 2 (C 'C8C8C8')
    Rct $b 4 2 8 2 (C '9A9A9A')
    Rct $b 3 4 10 11 (C 'A9C0B7')          # glass edge
    Rct $b 4 5 8 9  (C 'DCEDE6')           # glass body
    Rct $b 4 7 8 7  (C '3E7A2A')           # the product
    Rct $b 5 8 3 5  (C '58A83A')
    Rct $b 9 9 2 4  (C '2E5C1F')
    Rct $b 4 5 1 3  (C 'FFFFFF')           # highlight down the left edge
    Rct $b 3 14 10 1 (C '7E9A90')
}

# Pocket scale: black slab, brushed steel pan, one red readout.
$draw['pocket_scale'] = {
    param($b)
    Rct $b 2 4 12 2 (C 'C8C8C8')           # weighing pan
    Rct $b 2 6 12 8 (C '1C1C1C')           # body
    Rct $b 3 7 10 6 (C '2E2E2E')
    Rct $b 4 8 5 3  (C '0A0A0A')           # display well
    Rct $b 5 9 1 1  (C 'D02020')
    Rct $b 7 9 1 1  (C 'D02020')
    Rct $b 6 9 1 1  (C '601010')
    Rct $b 11 9 2 2 (C '7A7A7A')           # tare button
    Rct $b 2 13 12 1 (C '000000')
}

# Kosmic chocolate bar: purple Krave foil peeled back off four brown segments.
$draw['chocolate_bar'] = {
    param($b)
    Rct $b 2 2 12 12 (C '3A1E6E')          # foil
    Rct $b 3 3 10 3  (C 'B060D0')
    Rct $b 4 4 2 1   (C 'FFFFFF')          # foil glint
    Rct $b 3 6 10 8  (C '5A3216')          # the bar
    Rct $b 3 6 10 1  (C '7A4A24')
    Rct $b 8 6 1 8   (C '3B2010')          # segment scoring
    Rct $b 3 10 10 1 (C '3B2010')
    Rct $b 2 13 12 1 (C '2A1450')
}

# Nugget's collar: orange band, stitching, gold name tag hanging off it.
$draw['nugget_collar'] = {
    param($b)
    Rct $b 1 5 14 5 (C 'E07A24')
    Rct $b 1 5 14 1 (C 'F2A55A')
    Rct $b 1 9 14 1 (C '9A4A0E')
    for($i=2;$i -lt 14;$i+=3){ Rct $b $i 7 1 1 (C '9A4A0E') }
    Rct $b 6 4 4 2  (C 'C8C8C8')           # buckle
    Rct $b 6 10 4 4 (C 'E8C24A')           # tag
    Rct $b 7 11 2 2 (C 'A8862A')
}

# ---- write everything ---------------------------------------------------------

$written = @()
foreach($name in @('stash_jar','pocket_scale','chocolate_bar','nugget_collar')){
    $b = New-Object System.Drawing.Bitmap 16,16,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    Rct $b 0 0 16 16 (C '000000' 0)
    & $draw[$name] $b
    $b.Save("$idir\$name.png",[System.Drawing.Imaging.ImageFormat]::Png)
    $b.Dispose()

    $model = '{' + [Environment]::NewLine + '  "parent": "minecraft:item/generated",' + [Environment]::NewLine + '  "textures": { "layer0": "barbarajones:item/' + $name + '" }' + [Environment]::NewLine + '}'
    [System.IO.File]::WriteAllText("$mdir\$name.json", $model, (New-Object System.Text.UTF8Encoding($false)))

    $written += "$idir\$name.png"
    $written += "$mdir\$name.json"
}

foreach($p in $written){ if(Test-Path $p){ "  OK  $p" } else { "  MISSING  $p" } }
