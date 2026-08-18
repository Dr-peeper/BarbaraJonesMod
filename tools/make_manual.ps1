# Generates the Krave Manual item icon + its model, and wires the recipe/lang.
Add-Type -AssemblyName System.Drawing

$root = 'C:\Users\ADMIN\BarbaraJonesMod1201\src\main\resources'
$idir = "$root\assets\barbarajones\textures\item"
$mdir = "$root\assets\barbarajones\models\item"
$rdir = "$root\data\barbarajones\recipes"

function C([string]$h,[int]$a=255){ [System.Drawing.Color]::FromArgb($a,
    [Convert]::ToInt32($h.Substring(0,2),16),[Convert]::ToInt32($h.Substring(2,2),16),[Convert]::ToInt32($h.Substring(4,2),16)) }
function Rct($b,$x,$y,$w,$h,$c){ for($i=0;$i -lt $w;$i++){for($j=0;$j -lt $h;$j++){
    if(($x+$i) -lt $b.Width -and ($y+$j) -lt $b.Height -and ($x+$i) -ge 0 -and ($y+$j) -ge 0){ $b.SetPixel($x+$i,$y+$j,$c) } }} }

# A hardback manual: deep purple Krave cover, gold band, red bookmark ribbon.
$b = New-Object System.Drawing.Bitmap 16,16,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Rct $b 0 0 16 16 (C '000000' 0)

$cover  = C '3A1E6E'
$coverD = C '241043'
$coverL = C '5A3A9A'
$pages  = C 'EFE6D2'
$pagesD = C 'C9BC9E'
$gold   = C 'D8A63A'
$ribbon = C 'B01818'

# page block on the right
Rct $b 4 2 9 13 $pages
Rct $b 4 2 9 1  $pagesD
Rct $b 12 2 1 13 $pagesD
for($y=3;$y -lt 15;$y+=2){ Rct $b 6 $y 6 1 $pagesD }

# the cover, wrapping the spine
Rct $b 2 1 3 14 $cover
Rct $b 2 1 1 14 $coverD
Rct $b 3 1 1 14 $coverL
Rct $b 4 1 9 1  $cover
Rct $b 4 14 9 1 $cover
Rct $b 13 1 1 14 $cover
Rct $b 13 1 1 14 $coverD

# gold band across the spine + a K on the cover edge
Rct $b 2 6 3 2 $gold
Rct $b 13 6 1 2 $gold

# red bookmark ribbon hanging out of the bottom
Rct $b 9 14 1 2 $ribbon
Rct $b 9 15 1 1 (C '7A0E0E')

$b.Save("$idir\krave_manual.png",[System.Drawing.Imaging.ImageFormat]::Png)
$b.Dispose()
"icon written: $idir\krave_manual.png"

$model = @"
{
  "parent": "minecraft:item/generated",
  "textures": { "layer0": "barbarajones:item/krave_manual" }
}
"@
[System.IO.File]::WriteAllText("$mdir\krave_manual.json", $model, (New-Object System.Text.UTF8Encoding($false)))
"model written"

$recipe = @"
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    { "item": "minecraft:book" },
    { "item": "barbarajones:krave_box" }
  ],
  "result": { "item": "barbarajones:krave_manual", "count": 1 }
}
"@
[System.IO.File]::WriteAllText("$rdir\krave_manual.json", $recipe, (New-Object System.Text.UTF8Encoding($false)))
"recipe written"

# lang entry
$lang = "$root\assets\barbarajones\lang\en_us.json"
$txt  = [System.IO.File]::ReadAllText($lang)
if($txt -notmatch 'krave_manual'){
    $txt = $txt -replace '(\s*)"item\.barbarajones\.recipe_book"',
        "`$1`"item.barbarajones.krave_manual`": `"The Krave Manual`",`$1`"item.barbarajones.recipe_book`""
    [System.IO.File]::WriteAllText($lang, $txt, (New-Object System.Text.UTF8Encoding($false)))
    "lang entry added"
} else { "lang entry already present" }

# verify everything actually landed on disk
foreach($p in @("$idir\krave_manual.png","$mdir\krave_manual.json","$rdir\krave_manual.json")){
    if(Test-Path $p){ "  OK  $p" } else { "  MISSING  $p" }
}
if((Get-Content $lang -Raw) -match 'krave_manual'){ "  OK  lang" } else { "  MISSING  lang" }
