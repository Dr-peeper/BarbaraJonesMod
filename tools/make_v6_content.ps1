# v6 additions: Krave tool set, Cayden Compass, and the cocoa-substitute chain.
# Generates icons, item models and recipe JSON for each.
Add-Type -AssemblyName System.Drawing
$root = 'C:\Users\ADMIN\BarbaraJonesMod1201'
$idir = "$root\src\main\resources\assets\barbarajones\textures\item"
$mdir = "$root\src\main\resources\assets\barbarajones\models\item"
$rdir = "$root\src\main\resources\data\barbarajones\recipes"
New-Item -ItemType Directory -Force $idir,$mdir,$rdir | Out-Null

function C([string]$h,[int]$a=255){ [System.Drawing.Color]::FromArgb($a,
    [Convert]::ToInt32($h.Substring(0,2),16),[Convert]::ToInt32($h.Substring(2,2),16),[Convert]::ToInt32($h.Substring(4,2),16)) }
function NewIcon(){ New-Object System.Drawing.Bitmap 16,16,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function P($b,$x,$y,$c){ if($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16){ $b.SetPixel($x,$y,$c) } }
function Rct($b,$x,$y,$w,$h,$c){ for($i=0;$i -lt $w;$i++){for($j=0;$j -lt $h;$j++){ P $b ($x+$i) ($y+$j) $c }} }
function SaveIcon($b,$n){ $b.Save((Join-Path $idir "$n.png"),[System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }

$choc=C '5A3A22'; $chocL=C '7A5232'; $cream=C 'F0E0C0'; $red=C 'C0392B'; $gold=C 'F2C21A'
$stick=C '8A6A42'; $glow=C 'FFB020'

# --- shared: a cereal-textured tool head drawn over a stick handle -----------
function Handle($b){ for($i=0;$i -lt 8;$i++){ P $b (3+$i) (12-$i) $stick; P $b (4+$i) (12-$i) (C '6A4A28') } }
function Bits($b,$x,$y,$w,$h){
    Rct $b $x $y $w $h $choc
    for($i=0;$i -lt ($w*$h/2);$i++){
        $px=$x+(Get-Random -Maximum $w); $py=$y+(Get-Random -Maximum $h)
        if((Get-Random -Maximum 3) -eq 0){ P $b $px $py $chocL } elseif((Get-Random -Maximum 5) -eq 0){ P $b $px $py $cream }
    }
}

# krave_pickaxe
$b=NewIcon; Handle $b; Bits $b 3 2 10 4; Rct $b 3 2 10 1 $chocL; P $b 2 3 $glow; P $b 13 3 $glow; SaveIcon $b 'krave_pickaxe'
# krave_sword
$b=NewIcon; Handle $b; Bits $b 5 1 5 9; Rct $b 6 1 3 1 $cream; Rct $b 4 10 7 2 $gold; SaveIcon $b 'krave_sword'
# krave_axe
$b=NewIcon; Handle $b; Bits $b 3 2 8 6; Rct $b 3 2 1 6 $chocL; P $b 11 4 $glow; SaveIcon $b 'krave_axe'
# krave_shovel
$b=NewIcon; Handle $b; Bits $b 4 2 6 6; Rct $b 4 7 6 1 $chocL; SaveIcon $b 'krave_shovel'
# krave_hoe
$b=NewIcon; Handle $b; Bits $b 3 2 9 3; Rct $b 3 5 4 2 $choc; SaveIcon $b 'krave_hoe'

# cayden_compass: blue rim, cereal needle
$b=NewIcon; Rct $b 3 3 10 10 (C '2A3A5A'); Rct $b 4 4 8 8 (C 'E8E8EC'); Rct $b 5 5 6 6 (C 'C8CCD8')
Rct $b 7 6 2 4 $choc; P $b 8 5 $red; P $b 7 10 (C '404048'); SaveIcon $b 'cayden_compass'

# roasted_husk: dark shrivelled mushroom cap
$b=NewIcon; Rct $b 4 5 8 5 (C '4A3020'); Rct $b 4 5 8 1 (C '6A4830'); Rct $b 6 10 4 3 (C 'C0B0A0')
P $b 6 6 (C '2A1810'); P $b 9 7 (C '2A1810'); SaveIcon $b 'roasted_husk'
# cocoa_substitute: pouch of dark powder
$b=NewIcon; Rct $b 4 6 8 7 (C 'A9865A'); Rct $b 5 5 6 1 (C '80663E'); Rct $b 5 8 6 4 (C '4A2C18')
P $b 7 9 (C '6A4028'); P $b 9 10 (C '6A4028'); SaveIcon $b 'cocoa_substitute'

# --- item models -------------------------------------------------------------
$newItems = @('krave_pickaxe','krave_sword','krave_axe','krave_shovel','krave_hoe',
              'cayden_compass','roasted_husk','cocoa_substitute')
foreach($i in $newItems){
    $parent = if($i -like 'krave_*' -and $i -ne 'krave_cereal'){ 'minecraft:item/handheld' } else { 'minecraft:item/generated' }
@"
{
  "parent": "$parent",
  "textures": { "layer0": "barbarajones:item/$i" }
}
"@ | Set-Content "$mdir\$i.json" -Encoding utf8 -NoNewline
}

# --- recipes -----------------------------------------------------------------
function Shaped($name, $pattern, $keys, $result, $count){
    $pat = ($pattern | ForEach-Object { '"' + $_ + '"' }) -join ', '
    $keyLines = ($keys.Keys | ForEach-Object { "`"$_`": { `"item`": `"$($keys[$_])`" }" }) -join ", "
@"
{
  "type": "minecraft:crafting_shaped",
  "pattern": [ $pat ],
  "key": { $keyLines },
  "result": { "item": "barbarajones:$result", "count": $count }
}
"@ | Set-Content "$rdir\$name.json" -Encoding utf8 -NoNewline
}
function Shapeless($name, [string[]]$ing, $result, $count){
    $list = ($ing | ForEach-Object { "{ `"item`": `"$_`" }" }) -join ", "
@"
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [ $list ],
  "result": { "item": "barbarajones:$result", "count": $count }
}
"@ | Set-Content "$rdir\$name.json" -Encoding utf8 -NoNewline
}
function Smelt($name, $ingredient, $result, $xp){
@"
{
  "type": "minecraft:smelting",
  "ingredient": { "item": "$ingredient" },
  "result": "$result",
  "experience": $xp,
  "cookingtime": 200
}
"@ | Set-Content "$rdir\$name.json" -Encoding utf8 -NoNewline
}

$K = @{ 'K' = 'barbarajones:krave_cereal'; 'S' = 'minecraft:stick' }
Shaped 'krave_pickaxe' @('KKK',' S ',' S ') $K 'krave_pickaxe' 1
Shaped 'krave_sword'   @(' K ',' K ',' S ') $K 'krave_sword' 1
Shaped 'krave_axe'     @('KK ','KS ',' S ') $K 'krave_axe' 1
Shaped 'krave_shovel'  @(' K ',' S ',' S ') $K 'krave_shovel' 1
Shaped 'krave_hoe'     @('KK ',' S ',' S ') $K 'krave_hoe' 1

Shapeless 'cayden_compass' @('minecraft:compass','barbarajones:krave_cereal','minecraft:lapis_lazuli') 'cayden_compass' 1

# the cocoa chain: mushroom -> husk -> substitute -> real cocoa beans
Smelt 'roasted_husk' 'minecraft:brown_mushroom' 'barbarajones:roasted_husk' 0.1
Shapeless 'cocoa_substitute' @('barbarajones:roasted_husk','barbarajones:roasted_husk','minecraft:sugar','minecraft:coal') 'cocoa_substitute' 1
Smelt 'cocoa_beans_from_substitute' 'barbarajones:cocoa_substitute' 'minecraft:cocoa_beans' 0.2

Write-Output "wrote 8 icons, 8 models, 9 recipes"
