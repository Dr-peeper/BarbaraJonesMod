# Generates every asset for the Krave Kosmos feature: the portal block/door,
# liquid chocolate, the bucket, the Krave Tether, and the Krave Minion skin.
# Idempotent - safe to re-run (each image is painted fresh from scratch).
Add-Type -AssemblyName System.Drawing
$repoRoot = Split-Path -Parent $PSScriptRoot
$root = "$repoRoot\src\main\resources"
$bdir = "$root\assets\barbarajones\textures\block"
$idir = "$root\assets\barbarajones\textures\item"
$edir = "$root\assets\barbarajones\textures\entity"
$mbdir = "$root\assets\barbarajones\models\block"
$midir = "$root\assets\barbarajones\models\item"
$bsdir = "$root\assets\barbarajones\blockstates"
$rdir = "$root\data\barbarajones\recipes"
New-Item -ItemType Directory -Force $bdir,$idir,$edir,$mbdir,$midir,$bsdir,$rdir | Out-Null

function C([string]$h,[int]$a=255){ [System.Drawing.Color]::FromArgb($a,
    [Convert]::ToInt32($h.Substring(0,2),16),[Convert]::ToInt32($h.Substring(2,2),16),[Convert]::ToInt32($h.Substring(4,2),16)) }
function NewImg($w,$h){ New-Object System.Drawing.Bitmap $w,$h,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function Rct($b,$x,$y,$w,$h,$c){ for($i=0;$i -lt $w;$i++){for($j=0;$j -lt $h;$j++){
    if(($x+$i) -lt $b.Width -and ($y+$j) -lt $b.Height -and ($x+$i) -ge 0 -and ($y+$j) -ge 0){ $b.SetPixel($x+$i,$y+$j,$c) } }} }
function Save($b,$path){ $b.Save($path,[System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }

$script:sd = 42
function Rnd([int]$n){ $script:sd=($script:sd*1103515245+12345) -band 0x7fffffff; return $script:sd % $n }

# Radial darkening pass - gives every texture a bit of shaped depth instead of
# reading as a flat tint, and ties the whole Krave Kosmos palette together.
function Vignette($b,[double]$strength){
    $w=$b.Width; $h=$b.Height; $cx=($w-1)/2.0; $cy=($h-1)/2.0; $maxD=[Math]::Sqrt($cx*$cx+$cy*$cy)
    for($x=0;$x -lt $w;$x++){ for($y=0;$y -lt $h;$y++){
        $dx=$x-$cx; $dy=$y-$cy; $d=[Math]::Sqrt($dx*$dx+$dy*$dy)/$maxD
        $f=1.0-($strength*$d*$d)
        $p=$b.GetPixel($x,$y)
        $nr=[Math]::Max(0,[int]($p.R*$f)); $ng=[Math]::Max(0,[int]($p.G*$f)); $nb=[Math]::Max(0,[int]($p.B*$f))
        $b.SetPixel($x,$y,[System.Drawing.Color]::FromArgb($p.A,$nr,$ng,$nb))
    }}
}

$hide = C '15102A'; $hideL = C '241A45'; $bone = C 'E8E2D8'
function Speckle($b,$base){
    Rct $b 0 0 $b.Width $b.Height $base
    # a faint regular diagonal accent underneath the noise - the repeating
    # "wallpaper" motif that pure random speckle was missing
    for($x=0;$x -lt $b.Width;$x++){ for($y=0;$y -lt $b.Height;$y++){
        if((($x+$y) % 4) -eq 0){ Rct $b $x $y 1 1 $hideL }
    }}
    for($i=0;$i -lt ($b.Width*$b.Height/4);$i++){ $px=Rnd $b.Width; $py=Rnd $b.Height; $r=Rnd 7
        if($r -eq 0){ Rct $b $px $py 1 1 (C '8A5CD0') } elseif($r -eq 1){ Rct $b $px $py 1 1 (C '3A6CD8') }
        elseif($r -eq 2){ Rct $b $px $py 1 1 $hideL } }
    for($i=0;$i -lt ($b.Width*$b.Height/14);$i++){ Rct $b (Rnd $b.Width) (Rnd $b.Height) 1 1 (C 'F4F0FF') }
    Vignette $b 0.3
}

# ---- Krave Block: the portal frame -----------------------------------------
$b = NewImg 16 16
Speckle $b $hide
Rct $b 0 0 16 1 $bone; Rct $b 0 15 16 1 $bone; Rct $b 0 0 1 16 $bone; Rct $b 15 0 1 16 $bone
Save $b "$bdir\krave_block.png"

@"
{ "variants": { "": { "model": "barbarajones:block/krave_block" } } }
"@ | Set-Content "$bsdir\krave_block.json" -Encoding utf8
@"
{ "parent": "minecraft:block/cube_all", "textures": { "all": "barbarajones:block/krave_block" } }
"@ | Set-Content "$mbdir\krave_block.json" -Encoding utf8
@"
{ "parent": "barbarajones:block/krave_block" }
"@ | Set-Content "$midir\krave_block.json" -Encoding utf8
"  krave_block: texture + blockstate + models"

# ---- Krave Grass + Krave Dirt: coherent terrain pair, vanilla-style ---------
$navy = C '150C28'; $navyL = C '241A45'
$nebulaColors = @((C '4A2C8A'),(C '3A1C70'),(C '5A3CA0'))
function Galaxy($b,$base){
    Rct $b 0 0 $b.Width $b.Height $base
    # several soft nebula blotches of varied color/size, not just two flat ones
    for($n=0;$n -lt 5;$n++){
        $cx=Rnd $b.Width; $cy=Rnd $b.Height
        $col = $nebulaColors[(Rnd $nebulaColors.Count)]
        $blobR = 2 + (Rnd 3)
        for($i=0;$i -lt 14;$i++){
            $px=$cx+(Rnd (2*$blobR+1))-$blobR; $py=$cy+(Rnd (2*$blobR+1))-$blobR
            if($px -ge 0 -and $py -ge 0 -and $px -lt $b.Width -and $py -lt $b.Height){ Rct $b $px $py 1 1 $col }
        }
    }
    # three-tier star field instead of one flat brightness
    for($i=0;$i -lt ($b.Width*$b.Height/7);$i++){ $r=Rnd 6
        if($r -eq 0){ Rct $b (Rnd $b.Width) (Rnd $b.Height) 1 1 (C 'FFFFFF') }
        elseif($r -eq 1){ Rct $b (Rnd $b.Width) (Rnd $b.Height) 1 1 (C 'C9B8FF') }
        elseif($r -eq 2){ Rct $b (Rnd $b.Width) (Rnd $b.Height) 1 1 (C '8878B0') } }
    Vignette $b 0.35
}
$dirt = NewImg 16 16
Galaxy $dirt $navy
Save $dirt "$bdir\krave_dirt.png"

# Blade-streak texture (short vertical strokes of varying shade/length) reads
# as real grass instead of flat speckle, the same way vanilla's grass_block
# top isn't just a solid tint.
function GrassBlades($b,$base,$light,$dark){
    Rct $b 0 0 $b.Width $b.Height $base
    for($x=0;$x -lt $b.Width;$x++){
        for($i=0;$i -lt 3;$i++){
            $y0 = Rnd $b.Height
            $len = 1 + (Rnd 3)
            $col = if((Rnd 2) -eq 0){ $light } else { $dark }
            for($k=0;$k -lt $len;$k++){
                $yy = $y0 + $k
                if($yy -lt $b.Height){ Rct $b $x $yy 1 1 $col }
            }
        }
    }
}
function GrassTop($b){
    GrassBlades $b (C '6A2CD0') (C '8A5CF0') (C '4A1CA0')
    Vignette $b 0.25
}
$grassTop = NewImg 16 16
GrassTop $grassTop
Save $grassTop "$bdir\krave_grass_top.png"

$grassSide = NewImg 16 16
Galaxy $grassSide $navy
for($x=0;$x -lt 16;$x++){ $r=Rnd 4
    Rct $grassSide $x 0 1 (3+$r) (C '6A2CD0') }
for($x=0;$x -lt 16;$x++){
    $col = if((Rnd 2) -eq 0){ C '8A5CF0' } else { C '9A6CFF' }
    Rct $grassSide $x 2 1 1 $col
}
Save $grassSide "$bdir\krave_grass_side.png"

@"
{ "variants": { "": { "model": "barbarajones:block/krave_dirt" } } }
"@ | Set-Content "$bsdir\krave_dirt.json" -Encoding utf8
@"
{ "parent": "minecraft:block/cube_all", "textures": { "all": "barbarajones:block/krave_dirt" } }
"@ | Set-Content "$mbdir\krave_dirt.json" -Encoding utf8
@"
{ "parent": "barbarajones:block/krave_dirt" }
"@ | Set-Content "$midir\krave_dirt.json" -Encoding utf8

@"
{ "variants": { "": { "model": "barbarajones:block/krave_grass" } } }
"@ | Set-Content "$bsdir\krave_grass.json" -Encoding utf8
@"
{
  "parent": "minecraft:block/cube_bottom_top",
  "textures": {
    "bottom": "barbarajones:block/krave_dirt",
    "top": "barbarajones:block/krave_grass_top",
    "side": "barbarajones:block/krave_grass_side"
  }
}
"@ | Set-Content "$mbdir\krave_grass.json" -Encoding utf8
@"
{ "parent": "barbarajones:block/krave_grass" }
"@ | Set-Content "$midir\krave_grass.json" -Encoding utf8
"  krave_grass + krave_dirt: textures + blockstates + models"

# ---- Krave Door -------------------------------------------------------------
function DoorPanel($b, $withHandle){
    Speckle $b $hide
    Rct $b 1 1 14 6 $hideL
    Rct $b 1 9 14 6 $hideL
    if($withHandle){ Rct $b 12 8 2 2 $bone }
}
$db = NewImg 16 16; DoorPanel $db $true; Save $db "$bdir\krave_door_bottom.png"
$dt = NewImg 16 16; DoorPanel $dt $false; Save $dt "$bdir\krave_door_top.png"

$icon = NewImg 16 16
Rct $icon 2 1 12 14 $hide
Rct $icon 3 2 10 12 $hideL
Rct $icon 11 8 1 1 $bone
Save $icon "$idir\krave_door.png"

foreach($half in 'bottom','top'){
    foreach($side in 'left','right'){
        foreach($open in '','_open'){
@"
{
  "parent": "minecraft:block/door_${half}_${side}${open}",
  "textures": { "bottom": "barbarajones:block/krave_door_bottom", "top": "barbarajones:block/krave_door_top" }
}
"@ | Set-Content "$mbdir\krave_door_${half}_${side}${open}.json" -Encoding utf8
        }
    }
}
@"
{ "parent": "minecraft:item/generated", "textures": { "layer0": "barbarajones:item/krave_door" } }
"@ | Set-Content "$midir\krave_door.json" -Encoding utf8

# blockstate: same 32-variant structure as vanilla's oak_door.json, just
# pointing at our own models.
$variants = [ordered]@{}
$dirs = @{ 'east'=0; 'south'=90; 'west'=180; 'north'=270 }
foreach($facing in $dirs.Keys){
    foreach($half in 'lower','upper'){
        $halfWord = if($half -eq 'lower'){'bottom'}else{'top'}
        foreach($hinge in 'left','right'){
            foreach($openState in 'false','true'){
                $openSuffix = if($openState -eq 'true'){'_open'}else{''}
                $baseY = $dirs[$facing]
                # matches vanilla's per-state extra rotation for the open variants
                $extra = 0
                if($openState -eq 'true'){
                    $extra = if($hinge -eq 'left'){90}else{270}
                }
                $y = ($baseY + $extra) % 360
                $model = "barbarajones:block/krave_door_${halfWord}_${hinge}${openSuffix}"
                $key = "facing=$facing,half=$half,hinge=$hinge,open=$openState"
                $entry = [ordered]@{ model = $model }
                if($y -ne 0){ $entry.y = $y }
                $variants[$key] = $entry
            }
        }
    }
}
$json = [ordered]@{ variants = $variants } | ConvertTo-Json -Depth 6
$json | Set-Content "$bsdir\krave_door.json" -Encoding utf8
"  krave_door: textures + blockstate + $($variants.Count) model variants"

# ---- liquid chocolate --------------------------------------------------------
$choc = NewImg 16 16
Rct $choc 0 0 16 16 (C '3A2412')
for($i=0;$i -lt 40;$i++){ Rct $choc (Rnd 16) (Rnd 16) 1 1 (C '5A3A22') }
for($i=0;$i -lt 15;$i++){ Rct $choc (Rnd 16) (Rnd 16) 1 1 (C '241408') }
Save $choc "$bdir\chocolate_still.png"

$chocF = NewImg 16 16
Rct $chocF 0 0 16 16 (C '3A2412')
for($y=0;$y -lt 16;$y++){ for($x=0;$x -lt 16;$x++){
    if((($x+$y) % 4) -eq 0){ Rct $chocF $x $y 1 1 (C '5A3A22') } } }
Save $chocF "$bdir\chocolate_flow.png"

$bucket = NewImg 16 16
Rct $bucket 3 3 10 1 (C 'A0A0A0'); Rct $bucket 3 4 1 9 (C 'A0A0A0'); Rct $bucket 12 4 1 9 (C 'A0A0A0')
Rct $bucket 3 12 10 1 (C 'A0A0A0')
Rct $bucket 4 5 8 7 (C '5A3A22')
Save $bucket "$idir\chocolate_bucket.png"
@"
{ "parent": "minecraft:item/generated", "textures": { "layer0": "barbarajones:item/chocolate_bucket" } }
"@ | Set-Content "$midir\chocolate_bucket.json" -Encoding utf8
"  chocolate: still/flow/bucket textures + bucket model"

# ---- Krave Tether ------------------------------------------------------------
$tether = NewImg 16 16
Rct $tether 6 1 4 4 (C '8A5CD0')
for($i=0;$i -lt 8;$i++){ Rct $tether (7+($i%2)) (5+$i) 2 1 (C 'C0C0C0') }
Rct $tether 5 13 6 2 (C '2A2436')
Save $tether "$idir\krave_tether.png"
@"
{ "parent": "minecraft:item/generated", "textures": { "layer0": "barbarajones:item/krave_tether" } }
"@ | Set-Content "$midir\krave_tether.json" -Encoding utf8
"  krave_tether: texture + model"

# ---- Krave Minion: small dark humanoid --------------------------------------
$m = NewImg 64 64
Rct $m 0 0 64 64 (C '000000' 0)
function Box2($bmp,$u,$v,$w,$h,$d,$c){ Rct $bmp $u $v (2*($w+$d)) ($d+$h) $c }
Box2 $m 0 0 8 8 8 $hide      # head
Rct $m 10 11 1 1 (C 'C03030'); Rct $m 13 11 1 1 (C 'C03030')  # small red eyes
Box2 $m 16 16 8 12 4 $hideL  # body
Box2 $m 40 16 4 12 4 $hide   # right arm
Box2 $m 0 16 4 12 4 $hide    # right leg
Box2 $m 32 48 4 12 4 $hide   # left arm
Box2 $m 16 48 4 12 4 $hide   # left leg
Save $m "$edir\krave_minion.png"
"  krave_minion: entity texture"

# ---- Krave Laser bolt ---------------------------------------------------------
$laser = NewImg 16 16
Rct $laser 0 0 16 16 (C '000000' 0)
Rct $laser 5 0 6 16 (C 'FFD060' 180)
Rct $laser 7 0 2 16 (C 'FFFFFF' 230)
Save $laser "$edir\krave_laser.png"
"  krave_laser: texture"

# ---- Krave Mouth Beam: blue "kamehameha" bolt from the boss ------------------
$beam = NewImg 16 16
Rct $beam 0 0 16 16 (C '000000' 0)
Rct $beam 3 0 10 16 (C '2050FF' 190)
Rct $beam 6 0 4 16 (C 'C8E0FF' 235)
Save $beam "$edir\krave_beam.png"
"  krave_beam: texture"

# ---- Krave Healing Box: a real cereal-box front, not a plain tinted panel ----
# 48x64 (vs the old 16x16) so the wordmark, bowl-and-spoon pictogram and
# nutrition-style panel actually read once stretched over the much bigger
# entity model below - a flat tint at 16x16 just went to mush at that size.
function Glyph($bmp,$x,$y,$rows,$c,$s){
    for($r=0; $r -lt $rows.Length; $r++){
        for($col=0; $col -lt $rows[$r].Length; $col++){
            if($rows[$r].Substring($col,1) -eq '1'){ Rct $bmp ($x+$col*$s) ($y+$r*$s) $s $s $c }
        }
    }
}
$glyphK = @('101','101','110','101','101')
$glyphR = @('110','101','110','101','101')
$glyphA = @('010','101','111','101','101')
$glyphV = @('101','101','101','101','010')
$glyphE = @('111','100','110','100','111')

# Red box, gold wordmark - the classic cereal-box combo - rather than the
# Kosmos's usual purple, so the box itself reads as a distinct "product" and
# not just more dimension scenery.
$boxBase = C 'C62828'; $boxDeep = C '7A1414'; $boxLight = C 'FFE9D2'
$boxLogo = C 'FFD23F'; $boxCream = C 'FFF3D6'; $boxAccent = C 'FF9A3D'; $boxBowl = C 'E8E8F0'

$box = NewImg 48 64
Rct $box 0 0 48 64 $boxBase
Rct $box 0 0 48 1 $boxDeep; Rct $box 0 63 48 1 $boxDeep
Rct $box 0 0 1 64 $boxDeep; Rct $box 47 0 1 64 $boxDeep
Rct $box 1 1 46 5 $boxDeep          # top brand band

$gs = 2
$letters = @($glyphK,$glyphR,$glyphA,$glyphV,$glyphE)
$lw = 3 * $gs; $gap = 2
$lx = [int]((48 - ($letters.Length * $lw + ($letters.Length - 1) * $gap)) / 2)
foreach($g in $letters){ Glyph $box $lx 8 $g $boxLogo $gs; $lx += $lw + $gap }
Rct $box 4 20 40 2 $boxLight        # underline stripe

# bowl (layered rects narrowing toward the base) with cereal pieces on the rim
Rct $box 12 34 24 3 $boxBowl
Rct $box 13 37 22 3 $boxBowl
Rct $box 15 40 18 3 $boxBowl
Rct $box 17 43 14 2 $boxBowl
Rct $box 14 32 2 2 $boxAccent; Rct $box 20 31 2 2 $boxLogo; Rct $box 26 32 2 2 $boxAccent
Rct $box 31 33 2 2 $boxLogo; Rct $box 18 33 2 2 $boxCream; Rct $box 28 34 2 2 $boxCream
# spoon
Rct $box 34 24 2 14 $boxCream
Rct $box 32 22 6 4 $boxCream

Rct $box 1 50 46 13 $boxDeep        # nutrition-style panel
Rct $box 4 53 40 1 $boxLight
Rct $box 4 56 32 1 $boxLight
Rct $box 4 59 36 1 $boxLight
Rct $box 38 51 6 6 $boxLogo         # brand seal
Save $box "$edir\krave_healing_box.png"
"  krave_healing_box: texture (48x64, cereal box branding)"

# ---- Krave Shield: translucent overlay tint (32x32, scaled up with the box) --
$shield = NewImg 32 32
Rct $shield 0 0 32 32 (C 'B080FF' 150)
for($x=0;$x -lt 32;$x++){ for($y=0;$y -lt 32;$y++){
    if((($x+$y) % 6) -eq 0){ Rct $shield $x $y 1 1 (C 'E8D8FF' 190) }
}}
Save $shield "$edir\krave_shield.png"
"  krave_shield: texture"

# ---- recipes ------------------------------------------------------------------
@"
{
  "type": "minecraft:crafting_shaped",
  "pattern": [ "###", "#X#", "###" ],
  "key": { "#": { "item": "minecraft:obsidian" }, "X": { "item": "minecraft:amethyst_shard" } },
  "result": { "item": "barbarajones:krave_block", "count": 4 }
}
"@ | Set-Content "$rdir\krave_block.json" -Encoding utf8

@"
{
  "type": "minecraft:crafting_shaped",
  "pattern": [ "#X", "#X", "#X" ],
  "key": { "#": { "item": "barbarajones:krave_block" }, "X": { "item": "minecraft:amethyst_shard" } },
  "result": { "item": "barbarajones:krave_door", "count": 3 }
}
"@ | Set-Content "$rdir\krave_door.json" -Encoding utf8

@"
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    { "item": "minecraft:ender_pearl" },
    { "item": "minecraft:amethyst_shard" },
    { "item": "barbarajones:krave_cereal" }
  ],
  "result": { "item": "barbarajones:krave_tether", "count": 2 }
}
"@ | Set-Content "$rdir\krave_tether.json" -Encoding utf8
"  recipes: krave_block, krave_door, krave_tether"

"done"
