# Ports every asset from the 1.7.10 mod into the 1.20.1 layout and generates
# the JSON data files modern Minecraft needs (item models, recipes, sounds.json,
# lang). Idempotent - safe to re-run.
$repoRoot = Split-Path -Parent $PSScriptRoot
# $old points at the legacy 1.7.10 project, which predates this repo and isn't
# part of it - this migration already ran once and its output is what's in
# src/ today. Only needed again if you have that old project checked out too.
$old  = 'C:\Users\ADMIN\BarbaraJonesMod\src\main\resources\assets\barbarajones'
$new  = "$repoRoot\src\main\resources\assets\barbarajones"
$data = "$repoRoot\src\main\resources\data\barbarajones"

foreach($d in @("$new\textures\item","$new\textures\entity","$new\textures\gui","$new\sounds",
                "$new\models\item","$new\lang","$data\recipes","$data\loot_tables\entities")){
    New-Item -ItemType Directory -Force $d | Out-Null
}

# ---- textures: items/ -> item/ (modern singular), entity + gui straight over --
Copy-Item "$old\textures\items\*.png" "$new\textures\item\" -Force
Copy-Item "$old\textures\entity\*.png" "$new\textures\entity\" -Force
Copy-Item "$old\textures\gui\*.png"    "$new\textures\gui\"    -Force
Copy-Item "$old\sounds\*.ogg"          "$new\sounds\"          -Force

$items = Get-ChildItem "$new\textures\item\*.png" | ForEach-Object { $_.BaseName }
"migrated $($items.Count) item textures"

# ---- item models: one generated/item JSON per texture ----------------------
foreach($i in $items){
@"
{
  "parent": "minecraft:item/generated",
  "textures": { "layer0": "barbarajones:item/$i" }
}
"@ | Set-Content "$new\models\item\$i.json" -Encoding utf8 -NoNewline
}
"wrote $($items.Count) item models"

# ---- sounds.json (modern format: subtitle + sounds array) -------------------
$va = Get-ChildItem "$new\sounds\va_*.ogg" | ForEach-Object { $_.BaseName }
function SoundEntry($subtitle, [string[]]$files){
    $arr = ($files | ForEach-Object { '"barbarajones:' + $_ + '"' }) -join ', '
    return "{ `"subtitle`": `"subtitles.barbarajones.$subtitle`", `"sounds`": [ $arr ] }"
}
$entries = @()
$entries += '  "barbara_idle": '  + (SoundEntry 'barbara' @('va_intro','va_chepina','va_bits','va_nugget','va_donuts','va_og'))
$entries += '  "barbara_hurt": '  + (SoundEntry 'barbara' @('va_ohgod','va_shower'))
$entries += '  "barbara_death": ' + (SoundEntry 'barbara' @('va_ohgod2'))
$entries += '  "barbara_rage": '  + (SoundEntry 'barbara' @('va_house','va_lighter'))
$entries += '  "cayden_idle": '   + (SoundEntry 'cayden'  @('ca_idle1','ca_idle2'))
$entries += '  "cayden_hurt": '   + (SoundEntry 'cayden'  @('ca_hurt'))
$entries += '  "cayden_death": '  + (SoundEntry 'cayden'  @('ca_death'))
foreach($k in @('krave_laugh','krave_screech','krave_boom','krave_tornado','krave_spawn',
                'krave_voice','krave_siren','krave_hell_rumble','krave_roar','krave_hurt','krave_death')){
    $entries += "  `"$k`": " + (SoundEntry 'krave' @($k))
}
foreach($v in $va){
    $entries += "  `"evt_$($v -replace '^va_','')`": " + (SoundEntry 'barbara' @($v))
}
"{`n" + ($entries -join ",`n") + "`n}" | Set-Content "$new\sounds.json" -Encoding utf8
"wrote sounds.json ($($entries.Count) events)"

# ---- shapeless recipe generator ---------------------------------------------
function Ingredient($token){
    if($token -like 'barbarajones:*' -or $token -like 'minecraft:*'){ return "{ `"item`": `"$token`" }" }
    return "{ `"item`": `"minecraft:$token`" }"
}
function Recipe($name, [string[]]$ingredients, $resultItem, $count){
    $ing = ($ingredients | ForEach-Object { Ingredient $_ }) -join ", "
    $res = if($resultItem -like '*:*'){ $resultItem } else { "barbarajones:$resultItem" }
@"
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [ $ing ],
  "result": { "item": "$res", "count": $count }
}
"@ | Set-Content "$data\recipes\$name.json" -Encoding utf8 -NoNewline
}

# the full recipe book, ported 1:1 from the 1.7.10 GameRegistry calls
Recipe 'handful_of_grass' @('tall_grass','tall_grass','tall_grass') 'handful_of_grass' 1
Recipe 'diced_grass'      @('barbarajones:handful_of_grass','barbarajones:grass_knife') 'diced_grass' 2
Recipe 'burnt_grass'      @('barbarajones:diced_grass','barbarajones:blowtorch') 'burnt_grass' 1
Recipe 'rolling_paper'    @('paper') 'rolling_paper' 1
Recipe 'rolled_joint'     @('barbarajones:rolling_paper','barbarajones:burnt_grass','barbarajones:burnt_grass') 'rolled_joint' 1
Recipe 'grass_knife'      @('iron_ingot','stick') 'grass_knife' 1
Recipe 'blowtorch'        @('iron_ingot','flint_and_steel','iron_ingot') 'blowtorch' 1
Recipe 'lighter'          @('flint','iron_ingot') 'lighter' 1
Recipe 'microphone'       @('iron_ingot','iron_ingot','coal') 'microphone' 1
Recipe 'pibb_cocktail'    @('barbarajones:mr_pibb','barbarajones:chepina') 'pibb_cocktail' 1
Recipe 'chicken_nuggets'  @('cooked_chicken') 'chicken_nuggets' 4
Recipe 'donut'            @('wheat','sugar','egg') 'donut' 1
Recipe 'krave_cereal'     @('wheat','wheat','sugar','cocoa_beans') 'krave_cereal' 2
Recipe 'krave_box'        @('paper','paper','barbarajones:krave_cereal') 'krave_box' 1
Recipe 'quest_book'       @('book','barbarajones:krave_cereal') 'quest_book' 1
Recipe 'recipe_book'      @('book','paper') 'recipe_book' 1
Recipe 'ashtray'          @('iron_ingot','cobblestone','cobblestone') 'ashtray' 1
Recipe 'grass_seeds'      @('barbarajones:handful_of_grass','wheat_seeds') 'grass_seeds' 2
Recipe 'golden_joint'     @('barbarajones:rolled_joint','gold_ingot') 'golden_joint' 1
Recipe 'grass_brownie'    @('barbarajones:diced_grass','cocoa_beans','wheat') 'grass_brownie' 1
Recipe 'bong'             @('glass_bottle','iron_ingot','barbarajones:diced_grass') 'bong' 1
Recipe 'pibb_zero'        @('barbarajones:mr_pibb','glass_bottle') 'pibb_zero' 1
Recipe 'fries'            @('potato','potato') 'fries' 2
Recipe 'nugget_box'       @('barbarajones:chicken_nuggets','barbarajones:chicken_nuggets','paper') 'nugget_box' 1
Recipe 'donut_box'        @('barbarajones:donut','barbarajones:donut','barbarajones:donut','paper') 'donut_box' 1
Recipe 'towel'            @('white_wool','white_wool','string') 'towel' 1
Recipe 'soap'             @('slime_ball','sugar') 'soap' 1
Recipe 'toothbrush'       @('stick','bone') 'toothbrush' 1
Recipe 'yellow_teeth'     @('bone','yellow_dye') 'yellow_teeth' 1
Recipe 'camera'           @('iron_ingot','glass','redstone') 'camera' 1
Recipe 'krave_milk'       @('milk_bucket','barbarajones:krave_cereal') 'krave_milk' 1
Recipe 'cereal_bowl'      @('bowl','barbarajones:krave_cereal','barbarajones:krave_milk') 'cereal_bowl' 1
Recipe 'managers_tie'     @('string','red_dye') 'managers_tie' 1
Recipe 'child_support_papers' @('paper','paper','paper') 'child_support_papers' 1
Recipe 'flyrich_poster'   @('paper','paper','purple_dye') 'flyrich_poster' 1
Recipe 'barbara_plush'    @('white_wool','white_wool','barbarajones:handful_of_grass') 'barbara_plush' 1
Recipe 'red_hat'          @('red_wool','string','red_dye') 'red_hat' 1
Recipe 'red_shirt'        @('red_wool','red_wool') 'red_shirt' 1
Recipe 'computer_mouse'   @('iron_ingot','redstone','string') 'computer_mouse' 1
Recipe 'minecraft_disc'   @('paper','redstone') 'minecraft_disc' 1
Recipe 'virus'            @('barbarajones:minecraft_disc','rotten_flesh') 'virus' 1
Recipe 'toaster_pastries' @('bread','sugar') 'toaster_pastries' 1
Recipe 'five_hundred_dollars' @('gold_ingot','gold_ingot','paper') 'five_hundred_dollars' 1
Recipe 'ski_mask'         @('black_wool','string') 'ski_mask' 1
Recipe 'moms_belt'        @('leather','iron_ingot') 'moms_belt' 1
Recipe 'adoption_papers'  @('paper','paper','ink_sac') 'adoption_papers' 1
Recipe 'sewer_grate'      @('iron_ingot','iron_ingot','iron_bars') 'sewer_grate' 1
Recipe 'krave_video_tape' @('paper','barbarajones:krave_cereal','redstone') 'krave_video_tape' 1
Recipe 'fake_cocaine'     @('snowball','paper') 'fake_cocaine' 3
Recipe 'housing_query'    @('stick','glass','iron_ingot') 'housing_query' 1
"wrote $((Get-ChildItem "$data\recipes\*.json").Count) recipes"

# ---- smelting: diced grass -> burnt grass -----------------------------------
@"
{
  "type": "minecraft:smelting",
  "ingredient": { "item": "barbarajones:diced_grass" },
  "result": "barbarajones:burnt_grass",
  "experience": 0.1,
  "cookingtime": 200
}
"@ | Set-Content "$data\recipes\burnt_grass_smelting.json" -Encoding utf8 -NoNewline
"done"
