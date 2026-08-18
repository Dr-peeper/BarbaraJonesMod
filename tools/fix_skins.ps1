# Converts the legacy 64x32 skins to modern 64x64 (1.20.1 humanoid models sample
# the left arm/leg from y=48, which simply does not exist in the old format), and
# regenerates the Krave Monster as a proper 64x64 humanoid skin. Also creates the
# missing housing_query icon + model.
Add-Type -AssemblyName System.Drawing

$repoRoot = Split-Path -Parent $PSScriptRoot
$root = "$repoRoot\src\main\resources\assets\barbarajones"
$edir = "$root\textures\entity"
$idir = "$root\textures\item"
$mdir = "$root\models\item"

function C([string]$h,[int]$a=255){ [System.Drawing.Color]::FromArgb($a,
    [Convert]::ToInt32($h.Substring(0,2),16),[Convert]::ToInt32($h.Substring(2,2),16),[Convert]::ToInt32($h.Substring(4,2),16)) }
function Rct($b,$x,$y,$w,$h,$c){ for($i=0;$i -lt $w;$i++){for($j=0;$j -lt $h;$j++){
    if(($x+$i) -lt $b.Width -and ($y+$j) -lt $b.Height -and ($x+$i) -ge 0 -and ($y+$j) -ge 0){ $b.SetPixel($x+$i,$y+$j,$c) } }} }

# ---- legacy 64x32 -> 64x64 -------------------------------------------------
# The modern format adds a left arm at (32,48) and a left leg at (16,48), each a
# mirrored copy of its right-hand counterpart.
function Upgrade($name){
    $path = "$edir\$name.png"
    $old = [System.Drawing.Bitmap]::FromFile($path)
    if($old.Height -ne 32){ $old.Dispose(); "  $name already modern"; return }

    $new = New-Object System.Drawing.Bitmap 64,64,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    # copy the whole legacy sheet into the top half
    for($x=0;$x -lt 64;$x++){ for($y=0;$y -lt 32;$y++){ $new.SetPixel($x,$y,$old.GetPixel($x,$y)) } }

    # left leg (16,48) <- mirror of right leg (0,16)
    for($x=0;$x -lt 16;$x++){ for($y=0;$y -lt 16;$y++){
        $new.SetPixel(16+$x, 48+$y, $old.GetPixel(15-$x, 16+$y)) } }
    # left arm (32,48) <- mirror of right arm (40,16)
    for($x=0;$x -lt 16;$x++){ for($y=0;$y -lt 16;$y++){
        $new.SetPixel(32+$x, 48+$y, $old.GetPixel(40+15-$x, 16+$y)) } }

    $old.Dispose()
    $new.Save($path,[System.Drawing.Imaging.ImageFormat]::Png)
    $new.Dispose()
    "  $name upgraded to 64x64"
}
"upgrading legacy skins:"
foreach($n in 'cayden','daniel','mom','plug','manager'){ Upgrade $n }

# ---- Krave Monster: custom beast model, 128x128 ------------------------------
# Matches the UV layout baked into KraveMonsterModel.java exactly - a
# four-legged, spine-spiked, tailed creature, not the shared humanoid rig.
# Box(u,v,w,h,d) fills a part's whole UV footprint (Minecraft's standard cube
# unwrap: width 2*(w+d), height d+h), same footprint math the model uses.
$b = New-Object System.Drawing.Bitmap 128,128,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Rct $b 0 0 128 128 (C '000000' 0)
function Box($bmp,$u,$v,$w,$h,$d,$c){ Rct $bmp $u $v (2*($w+$d)) ($d+$h) $c }

$hide=C '15102A'; $hideL=C '241A45'; $bone=C 'E8E2D8'; $boneD=C 'B8AE9C'
$glowEye=C '9CF0FF'; $claw=C '2A2436'
$script:sd=11
function Rnd([int]$n){ $script:sd=($script:sd*1103515245+12345) -band 0x7fffffff; return $script:sd % $n }
function Galaxy($bmp,$u,$v,$w,$h,$d,$base){
    Box $bmp $u $v $w $h $d $base
    $fw=2*($w+$d); $fh=$d+$h
    for($i=0;$i -lt ($fw*$fh/4);$i++){ $px=$u+(Rnd $fw); $py=$v+(Rnd $fh); $r=Rnd 7
        if($r -eq 0){ Rct $bmp $px $py 1 1 (C '8A5CD0') } elseif($r -eq 1){ Rct $bmp $px $py 1 1 (C '3A6CD8') }
        elseif($r -eq 2){ Rct $bmp $px $py 1 1 $hideL } }
    for($i=0;$i -lt ($fw*$fh/14);$i++){ Rct $bmp ($u+(Rnd $fw)) ($v+(Rnd $fh)) 1 1 (C 'F4F0FF') }
}

# spine: hips -> chest -> neck
Galaxy $b 0  0 10 8 10 $hide
Galaxy $b 40 0  9 8 9  $hide
Galaxy $b 76 0  5 5 5  $hide

# head: skull, jaw, horns (bone-colored, no galaxy speckle - keeps the face readable)
Box $b 0  26 7 6 7 $hide
Box $b 28 26 4 3 6 $hideL
Box $b 64 26 2 4 2 $bone
Box $b 72 26 2 4 2 $bone
# glowing eyes + claws/horn tips painted after the base fill
Rct $b 3  28 1 1 $glowEye
Rct $b 10 28 1 1 $glowEye
Rct $b 65 26 2 1 $boneD
Rct $b 73 26 2 1 $boneD

# front legs (paint once; model mirrors this UV for the opposite side)
Galaxy $b 0  46 4 7 4 $hide
Galaxy $b 16 46 3 6 3 $hide
Box    $b 28 46 4 3 5 $claw

# back legs
Galaxy $b 46 46 5 8 5 $hide
Galaxy $b 66 46 4 7 4 $hide
Box    $b 82 46 4 3 6 $claw

# tail, tapering
Galaxy $b 0  64 4 4 5 $hide
Galaxy $b 18 64 3 3 4 $hideL
Galaxy $b 32 64 2 2 3 $hideL

# spine spikes: bone-colored, sharp accent against the dark hide
Box $b 42 64 2 4 2 $bone
Box $b 50 64 2 6 2 $bone

$b.Save("$edir\krave_monster.png",[System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose()
"  krave_monster rebuilt as a custom 128x128 dark galaxy beast"

# ---- the missing housing_query icon + model --------------------------------
$q = New-Object System.Drawing.Bitmap 16,16,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Rct $q 0 0 16 16 (C '000000' 0)
# a surveyor's glass: brown handle, brass ring, blue lens with a house glyph
for($i=0;$i -lt 6;$i++){ Rct $q (3+$i) (12-$i) 1 1 (C '5A3A22'); Rct $q (4+$i) (12-$i) 1 1 (C '7A5232') }
Rct $q 7 2 7 7 (C 'C0A030')
Rct $q 8 3 5 5 (C '9AD8F0')
Rct $q 9 5 3 2 (C '3A5A8A')
Rct $q 10 4 1 1 (C '3A5A8A')
Rct $q 8 3 5 1 (C 'CFE8FF')
$q.Save("$idir\housing_query.png",[System.Drawing.Imaging.ImageFormat]::Png); $q.Dispose()
@"
{
  "parent": "minecraft:item/handheld",
  "textures": { "layer0": "barbarajones:item/housing_query" }
}
"@ | Set-Content "$mdir\housing_query.json" -Encoding utf8 -NoNewline
"  housing_query icon + model created"

# ---- Krave Cleanse: a small glass vial of something too green to be safe ----
$k = New-Object System.Drawing.Bitmap 16,16,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Rct $k 0 0 16 16 (C '000000' 0)
Rct $k 6 2 4 2 (C '8A6A42')                      # cork
Rct $k 5 4 6 9 (C 'B8D8E8' 160)                    # glass body (translucent)
Rct $k 6 6 4 6 (C '3AC060')                       # green liquid
Rct $k 7 7 1 1 (C '80F0A0'); Rct $k 9 9 1 1 (C '80F0A0')
Rct $k 5 12 6 2 (C '90B0C0' 160)                  # glass base
$k.Save("$idir\krave_cleanse.png",[System.Drawing.Imaging.ImageFormat]::Png); $k.Dispose()
@"
{
  "parent": "minecraft:item/generated",
  "textures": { "layer0": "barbarajones:item/krave_cleanse" }
}
"@ | Set-Content "$mdir\krave_cleanse.json" -Encoding utf8 -NoNewline
$rdir = "$repoRoot\src\main\resources\data\barbarajones\recipes"
New-Item -ItemType Directory -Force $rdir | Out-Null
@"
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    { "item": "minecraft:milk_bucket" },
    { "item": "barbarajones:quest_book" },
    { "item": "barbarajones:handful_of_grass" }
  ],
  "result": { "item": "barbarajones:krave_cleanse", "count": 1 }
}
"@ | Set-Content "$rdir\krave_cleanse.json" -Encoding utf8 -NoNewline
"  krave_cleanse icon + model + recipe created"

# ---- The Plug: stoner redesign (was a plain black ninja-mask skin) ---------
# Repaints the standard-UV humanoid regions in place: skin-toned face with
# bloodshot eyes and a joint at the mouth, black hoodie with white drawstrings,
# denim legs, sneakers. Idempotent - always paints the same result.
function RedesignPlug($bmp){
    $hoodieDark=C '101014'; $hair=C '23180E'; $skin=C 'C68642'; $skinSh=C 'A26C35'
    $eyeRed=C 'E15A5A'; $pupil=C '0F0804'
    $denimDark=C '283852'; $denimMid=C '384C6C'; $denimLight=C '466084'
    $shoeWhite=C 'E8E8E8'; $shoeSole=C '191919'
    $jointPaper=C 'F5EBD2'; $jointEmber=C 'FF6E19'; $stringW=C 'EEEEEE'

    # head
    Rct $bmp 8 0 8 8 $hair; Rct $bmp 16 0 8 8 $hair
    Rct $bmp 0 8 8 8 $skinSh; Rct $bmp 16 8 8 8 $skinSh; Rct $bmp 24 8 8 8 $hair
    # front face, row by row (8,8)-(16,16)
    Rct $bmp 8 8 8 1 $hair
    Rct $bmp 8 9 1 1 $hair; Rct $bmp 9 9 1 1 $hair; Rct $bmp 10 9 4 1 $skin; Rct $bmp 14 9 1 1 $hair; Rct $bmp 15 9 1 1 $hair
    Rct $bmp 8 10 8 1 $skin
    Rct $bmp 8 11 1 1 $skin; Rct $bmp 9 11 1 1 $eyeRed; Rct $bmp 10 11 1 1 $pupil; Rct $bmp 11 11 2 1 $skin
    Rct $bmp 13 11 1 1 $pupil; Rct $bmp 14 11 1 1 $eyeRed; Rct $bmp 15 11 1 1 $skin
    Rct $bmp 8 12 8 1 $skin
    Rct $bmp 8 13 2 1 $skin; Rct $bmp 10 13 2 1 $pupil; Rct $bmp 12 13 2 1 $jointPaper; Rct $bmp 14 13 1 1 $jointEmber; Rct $bmp 15 13 1 1 $skin
    Rct $bmp 8 14 8 1 $skin
    Rct $bmp 8 15 8 1 $skinSh

    # body: hoodie with white drawstrings on the front
    Rct $bmp 20 16 8 4 $hoodieDark; Rct $bmp 28 16 8 4 $hoodieDark
    Rct $bmp 16 20 4 12 $hoodieDark; Rct $bmp 28 20 4 12 $hoodieDark; Rct $bmp 32 20 8 12 $hoodieDark
    Rct $bmp 20 20 8 12 $hoodieDark
    Rct $bmp 22 20 1 5 $stringW; Rct $bmp 25 20 1 5 $stringW
    # sleeves: right arm (40,16) + left arm (32,48) - same hoodie black as the body.
    # Explicitly repainted (not just left as whatever was already there) so this
    # function fully resets every pixel it's responsible for on every run.
    Rct $bmp 40 16 16 16 $hoodieDark
    Rct $bmp 32 48 16 16 $hoodieDark

    # legs: jeans + sneakers, applied to right leg (0,16) and left leg (16,48)
    foreach($base in @(@(0,16),@(16,48))){
        $bx=$base[0]; $by=$base[1]
        Rct $bmp ($bx+4) $by 4 4 $denimDark; Rct $bmp ($bx+8) $by 4 4 $denimDark
        Rct $bmp $bx ($by+4) 4 12 $denimMid
        Rct $bmp ($bx+4) ($by+4) 4 12 $denimLight
        Rct $bmp ($bx+8) ($by+4) 4 12 $denimMid
        Rct $bmp ($bx+12) ($by+4) 4 12 $denimDark
        foreach($x0 in @($bx,$bx+4,$bx+8,$bx+12)){
            Rct $bmp $x0 ($by+14) 4 1 $shoeWhite
            Rct $bmp $x0 ($by+15) 4 1 $shoeSole
        }
    }
}

# ---- generic AO-style shading pass: kills the flat "plastic" look ----------
# Finds each maximal same-color opaque region (i.e. each cuboid face as painted
# above) and adds a top-lit gradient, edge darkening, and a little grain, all
# without touching hue/identity. Safe to run on any of these skins repeatedly.
function Shade($bmp,[double]$gradStrength=0.28,[double]$edgeDark=0.22,[int]$grain=10,[int]$minArea=5){
    $w=$bmp.Width; $h=$bmp.Height
    $visited = New-Object 'bool[,]' $w,$h
    $dirs = @(@(1,0),@(-1,0),@(0,1),@(0,-1))
    for($y0=0;$y0 -lt $h;$y0++){
        for($x0=0;$x0 -lt $w;$x0++){
            if($visited[$x0,$y0]){ continue }
            $c0 = $bmp.GetPixel($x0,$y0)
            if($c0.A -eq 0){ $visited[$x0,$y0]=$true; continue }
            $stack = New-Object System.Collections.Generic.Stack[object]
            $stack.Push(@($x0,$y0)); $visited[$x0,$y0]=$true
            $region = New-Object System.Collections.Generic.List[object]
            $region.Add(@($x0,$y0))
            while($stack.Count -gt 0){
                $cur=$stack.Pop(); $cx=$cur[0]; $cy=$cur[1]
                foreach($d in $dirs){
                    $nx=$cx+$d[0]; $ny=$cy+$d[1]
                    if($nx -ge 0 -and $nx -lt $w -and $ny -ge 0 -and $ny -lt $h -and -not $visited[$nx,$ny]){
                        $nc=$bmp.GetPixel($nx,$ny)
                        if($nc.A -ne 0 -and $nc.R -eq $c0.R -and $nc.G -eq $c0.G -and $nc.B -eq $c0.B){
                            $visited[$nx,$ny]=$true; $stack.Push(@($nx,$ny)); $region.Add(@($nx,$ny))
                        }
                    }
                }
            }
            if($region.Count -lt $minArea){ continue }
            $rx0=($region | ForEach-Object{$_[0]} | Measure-Object -Minimum).Minimum
            $rx1=($region | ForEach-Object{$_[0]} | Measure-Object -Maximum).Maximum
            $ry0=($region | ForEach-Object{$_[1]} | Measure-Object -Minimum).Minimum
            $ry1=($region | ForEach-Object{$_[1]} | Measure-Object -Maximum).Maximum
            $rw=[Math]::Max($rx1-$rx0,1); $rh=[Math]::Max($ry1-$ry0,1)
            $regionSet = New-Object 'System.Collections.Generic.HashSet[string]'
            foreach($p in $region){ [void]$regionSet.Add("$($p[0]),$($p[1])") }
            foreach($p in $region){
                $x=$p[0]; $y=$p[1]
                $tx=($x-$rx0)/$rw; $ty=($y-$ry0)/$rh
                $grad = 1.0 + $gradStrength*((1-$ty)-0.5) + $gradStrength*0.4*((1-$tx)-0.5)
                $isEdge=$false
                foreach($d in $dirs){
                    $nx=$x+$d[0]; $ny=$y+$d[1]
                    if($nx -lt 0 -or $nx -ge $w -or $ny -lt 0 -or $ny -ge $h -or -not $regionSet.Contains("$nx,$ny")){ $isEdge=$true; break }
                }
                if($isEdge){ $grad -= $edgeDark }
                $hash = (([int64]$x*374761393) -bxor ([int64]$y*668265263))
                $hash = ($hash -bxor ($hash -shr 13)) * 1274126177
                $noiseUnit = ((([Math]::Abs($hash)) % 255)/255.0) - 0.5
                $noise = $noiseUnit * ($grain/255.0)
                $nr=[Math]::Max(0,[Math]::Min(255,[int][Math]::Round($c0.R*$grad + $noise*255)))
                $ng=[Math]::Max(0,[Math]::Min(255,[int][Math]::Round($c0.G*$grad + $noise*255)))
                $nb=[Math]::Max(0,[Math]::Min(255,[int][Math]::Round($c0.B*$grad + $noise*255)))
                $bmp.SetPixel($x,$y,[System.Drawing.Color]::FromArgb(255,$nr,$ng,$nb))
            }
        }
    }
}


# CAVEAT: Shade is not fully idempotent on already-shaded, very dark pixels.
# Its edge-darkening can clamp several neighboring pixels to the same floor
# value, which still reads as one flat region to the next run's flood-fill,
# so re-running this script on already-shaded art can darken those spots
# further each time. plug/barbara/cayden/daniel/mom/manager/krave_monster
# should only need shading once after a real change; if you need to re-shade
# from scratch, restore the pre-shading PNGs from git history first rather
# than repeatedly re-running this on top of already-shaded output.
"applying Plug redesign + shading pass to all humanoid-rig skins:"
$plugPath = "$edir\plug.png"
$orig = [System.Drawing.Bitmap]::FromFile($plugPath)
$plugBmp = New-Object System.Drawing.Bitmap $orig
$orig.Dispose()
RedesignPlug $plugBmp
$plugBmp.Save($plugPath,[System.Drawing.Imaging.ImageFormat]::Png)
$plugBmp.Dispose()
"  plug: redesigned (stoner look - was a plain ninja mask)"

foreach($n in 'plug','barbara','cayden','daniel','mom','manager','krave_monster'){
    $p = "$edir\$n.png"
    $orig = [System.Drawing.Bitmap]::FromFile($p)
    $copy = New-Object System.Drawing.Bitmap $orig
    $orig.Dispose()
    Shade $copy
    $copy.Save($p,[System.Drawing.Imaging.ImageFormat]::Png)
    $copy.Dispose()
    "  $n`: shaded"
}

"done"
