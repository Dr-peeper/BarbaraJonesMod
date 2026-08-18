# Generates en_us.json from the item registry names, so nothing shows a raw key.
$new = 'C:\Users\ADMIN\BarbaraJonesMod1201\src\main\resources\assets\barbarajones'
New-Item -ItemType Directory -Force "$new\lang" | Out-Null

$names = [ordered]@{
  handful_of_grass='Handful of Grass'; diced_grass='Diced Grass'; burnt_grass='Burnt Grass'
  rolling_paper='Rolling Paper'; rolled_joint='Rolled Joint'; grass_knife='Grass Knife'
  blowtorch='Blowtorch'; lighter="Daniel's Lighter"; microphone='Interview Microphone'
  camera='The Camera'; housing_query='Housing Query'; mr_pibb='Mr. Pibb'; chepina='Chepina'
  pibb_cocktail='Red Cup (Pibb Cocktail)'; gatorade='Zero Sugar Gatorade'; pibb_zero='Pibb ZERO'
  krave_milk='Krave Milk'; chicken_nuggets='Chicken Nuggets'; donut='Donut'; fries="McDonald's Fries"
  nugget_box='10-Piece Nugget Box'; donut_box='Donut Box'; cereal_bowl='Cereal Bowl of the Gods'
  grass_brownie='Grass Brownie'; golden_joint='Golden Joint'; krave_cereal='Krave Cereal'
  krave_box='Krave Box'; quest_book='Krave Quest Book'; recipe_book="Barbara's Cookbook"
  ashtray="Barbara's Ashtray"; grass_seeds='Premium Grass Seeds'; bong='Bong'; towel="Barbara's Towel"
  soap='Bar of Soap'; toothbrush="Barbara's Toothbrush"; yellow_teeth='Them Teeth'
  managers_tie="The Manager's Tie"; child_support_papers='Child Support Papers'
  flyrich_poster='Fly Rich Poster'; barbara_plush='Barbara Plush'; record_flyrich='Music Disc'
  red_hat='Backwards Red Hat'; red_shirt='All-Red Fit'; computer_mouse='Computer Mouse'
  virus='COMPUTER VIRUS'; minecraft_disc='Pirated Minecraft Download'
  krave_video_tape='Krave Video 1 (VHS)'; toaster_pastries='Name-Brand Toaster Pastries'
  off_brand_pastries='Off-Brand Toaster Pastries'; five_hundred_dollars="Mom's `$500"
  fake_cocaine='"Cocaine" (It''s Snow)'; fake_weed='Fake Weed'; ski_mask='Black Ski Mask'
  sniper_scope="The Plug's Scope"; moms_belt="Mom's Belt"; adoption_papers='Adoption Papers'
  sewer_grate='Sewer Grate'
  krave_pickaxe='Krave Pickaxe'; krave_sword='Krave Sword'; krave_axe='Krave Axe'
  krave_shovel='Krave Shovel'; krave_hoe='Krave Hoe'; cayden_compass='Cayden Compass'
  roasted_husk='Roasted Husk'; cocoa_substitute='Cocoa Substitute'
}
$entities = [ordered]@{
  barbara_jones='Barbara Jones'; cayden_cobb='Cayden Cobb'; krave_monster='The Krave Monster'
  nugget='Nugget'; daniel='Daniel'; mom_cobb="Cayden's Mom"; the_plug='The Plug'
  krave_meteor='Krave Meteor'; giant_krave_box='Giant Krave Box'; krave_tornado='Krave Tornado'
  sky_cinematic='Sky Cinematic'
}

$lines = @()
$lines += '  "itemGroup.barbarajones.main": "Barbara Jones"'
foreach($k in $names.Keys){ $v = $names[$k] -replace '"','\"'; $lines += "  `"item.barbarajones.$k`": `"$v`"" }
foreach($k in $entities.Keys){ $lines += "  `"entity.barbarajones.$k`": `"$($entities[$k])`"" }
foreach($s in @('barbara','cayden','krave')){ $lines += "  `"subtitles.barbarajones.$s`": `"Barbara Jones Mod`"" }

"{`n" + ($lines -join ",`n") + "`n}" | Set-Content "$new\lang\en_us.json" -Encoding utf8
"wrote en_us.json ($($lines.Count) keys)"
