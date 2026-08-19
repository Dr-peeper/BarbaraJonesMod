# v6 additions: Krave tool set, Cayden Compass, and the cocoa-substitute chain.
# Generates icons, item models and recipe JSON for each.
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
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

# --- shared: a literal stick handle with small red Krave Box heads, one per
# tool shape - a real product mascot, not an abstract cereal-textured blob. --
$stickBrown = C '8B5A2B'; $stickDark = C '6A421D'
$boxRed = C 'C62828'; $boxDeep = C '7A1414'; $boxGold = C 'FFD23F'

function DiagHandle($b,$x0,$y0,$len,$dx,$dy){
    $x=$x0; $y=$y0
    for($i=0;$i -lt $len;$i++){
        P $b $x $y $stickBrown
        P $b ($x+1) $y $stickDark
        $x += $dx; $y += $dy
    }
}
function BoxHead($b,$x,$y,$w,$h){
    Rct $b $x $y $w $h $boxRed
    Rct $b $x $y $w 1 $boxDeep
    P $b $x ($y+$h-1) $boxGold
    P $b ($x+$w-1) $y $boxGold
}

# krave_pickaxe: diagonal handle, two box heads splayed in a V like a real pick
$b=NewIcon; DiagHandle $b 2 13 8 1 -1; BoxHead $b 3 3 4 3; BoxHead $b 9 3 4 3
SaveIcon $b 'krave_pickaxe'
# krave_axe: diagonal handle, one wide asymmetric box head (single-bladed)
$b=NewIcon; DiagHandle $b 2 13 8 1 -1; BoxHead $b 8 2 6 5
SaveIcon $b 'krave_axe'
# krave_shovel: straight vertical handle, one box "blade" capping the top
$b=NewIcon; DiagHandle $b 7 13 9 0 -1; BoxHead $b 5 2 6 4
SaveIcon $b 'krave_shovel'
# krave_sword: short grip, crossguard, tall box blade straight up
$b=NewIcon; DiagHandle $b 7 13 4 0 -1; Rct $b 5 9 6 1 $stickDark; BoxHead $b 6 2 4 7
SaveIcon $b 'krave_sword'
# krave_hoe: diagonal handle, flat perpendicular box blade at the tip
$b=NewIcon; DiagHandle $b 2 13 9 1 -1; Rct $b 7 3 6 2 $boxRed; Rct $b 7 3 6 1 $boxDeep; P $b 12 3 $boxGold
SaveIcon $b 'krave_hoe'
# krave_multitool: bigger handle, three box heads fused (pick+axe+shovel in one)
$b=NewIcon; DiagHandle $b 1 14 8 1 -1; BoxHead $b 2 2 4 3; BoxHead $b 7 1 5 4; BoxHead $b 11 4 4 4
SaveIcon $b 'krave_multitool'

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
$newItems = @('krave_pickaxe','krave_sword','krave_axe','krave_shovel','krave_hoe','krave_multitool',
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

# Netherite ingot (N) as the real cost - genuinely difficult to obtain, not
# gated behind anything mod-specific we can't name for certain - plus one
# Krave Cereal (C) keeping the mod's own flavor in the recipe. Stick (S) is
# the handle, same as vanilla and matching the new "literal stick" look.
$T = @{ 'N' = 'minecraft:netherite_ingot'; 'C' = 'barbarajones:krave_cereal'; 'S' = 'minecraft:stick' }
Shaped 'krave_pickaxe' @('NCN',' S ',' S ') $T 'krave_pickaxe' 1
Shaped 'krave_sword'   @(' N ',' N ',' S ') $T 'krave_sword' 1
Shaped 'krave_axe'     @('NC ','NS ',' S ') $T 'krave_axe' 1
Shaped 'krave_shovel'  @(' N ',' S ',' S ') $T 'krave_shovel' 1
Shaped 'krave_hoe'     @('NC ',' S ',' S ') $T 'krave_hoe' 1

Shapeless 'cayden_compass' @('minecraft:compass','barbarajones:krave_cereal','minecraft:lapis_lazuli') 'cayden_compass' 1

# the cocoa chain: mushroom -> husk -> substitute -> real cocoa beans
Smelt 'roasted_husk' 'minecraft:brown_mushroom' 'barbarajones:roasted_husk' 0.1
Shapeless 'cocoa_substitute' @('barbarajones:roasted_husk','barbarajones:roasted_husk','minecraft:sugar','minecraft:coal') 'cocoa_substitute' 1
Smelt 'cocoa_beans_from_substitute' 'barbarajones:cocoa_substitute' 'minecraft:cocoa_beans' 0.2

Write-Output "wrote 9 icons, 9 models, 9 recipes"
