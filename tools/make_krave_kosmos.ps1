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

$hide = C '15102A'; $hideL = C '241A45'; $bone = C 'E8E2D8'
function Speckle($b,$base){
    Rct $b 0 0 $b.Width $b.Height $base
    for($i=0;$i -lt ($b.Width*$b.Height/4);$i++){ $px=Rnd $b.Width; $py=Rnd $b.Height; $r=Rnd 7
        if($r -eq 0){ Rct $b $px $py 1 1 (C '8A5CD0') } elseif($r -eq 1){ Rct $b $px $py 1 1 (C '3A6CD8') }
        elseif($r -eq 2){ Rct $b $px $py 1 1 $hideL } }
    for($i=0;$i -lt ($b.Width*$b.Height/14);$i++){ Rct $b (Rnd $b.Width) (Rnd $b.Height) 1 1 (C 'F4F0FF') }
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
function Galaxy($b,$base){
    Rct $b 0 0 $b.Width $b.Height $base
    # a couple of soft nebula blotches - small clusters of mid-tone pixels
    for($n=0;$n -lt 2;$n++){
        $cx=Rnd $b.Width; $cy=Rnd $b.Height
        for($i=0;$i -lt 10;$i++){
            $px=$cx+(Rnd 5)-2; $py=$cy+(Rnd 5)-2
            if($px -ge 0 -and $py -ge 0 -and $px -lt $b.Width -and $py -lt $b.Height){ Rct $b $px $py 1 1 (C '4A2C8A') }
        }
    }
    # sparse bright stars
    for($i=0;$i -lt ($b.Width*$b.Height/10);$i++){ $r=Rnd 5
        if($r -eq 0){ Rct $b (Rnd $b.Width) (Rnd $b.Height) 1 1 (C 'FFFFFF') }
        elseif($r -eq 1){ Rct $b (Rnd $b.Width) (Rnd $b.Height) 1 1 (C 'C9B8FF') } }
}
$dirt = NewImg 16 16
Galaxy $dirt $navy
Save $dirt "$bdir\krave_dirt.png"

function GrassTop($b){
    Rct $b 0 0 $b.Width $b.Height (C '6A2CD0')
    for($i=0;$i -lt ($b.Width*$b.Height/5);$i++){ $r=Rnd 4
        if($r -eq 0){ Rct $b (Rnd $b.Width) (Rnd $b.Height) 1 1 (C '8A5CF0') }
        elseif($r -eq 1){ Rct $b (Rnd $b.Width) (Rnd $b.Height) 1 1 (C '4A1CA0') } }
}
$grassTop = NewImg 16 16
GrassTop $grassTop
Save $grassTop "$bdir\krave_grass_top.png"

$grassSide = NewImg 16 16
Galaxy $grassSide $navy
for($x=0;$x -lt 16;$x++){ $r=Rnd 4
    Rct $grassSide $x 0 1 (3+$r) (C '6A2CD0') }
for($x=0;$x -lt 16;$x++){ Rct $grassSide $x 2 1 1 (C '8A5CF0') }
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
