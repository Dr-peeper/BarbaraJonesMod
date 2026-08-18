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

# ---- Krave Monster: rebuild as a 64x64 humanoid galaxy-hoodie skin ----------
$b = New-Object System.Drawing.Bitmap 64,64,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
Rct $b 0 0 64 64 (C '000000' 0)
$skin=C 'D9A57F'; $skinD=C 'B98A66'; $purp=C '3A1E6E'; $purpD=C '281046'; $purpL=C '5A3A9A'; $pants=C '1E1630'
$script:sd=7
function Rnd([int]$n){ $script:sd=($script:sd*1103515245+12345) -band 0x7fffffff; return $script:sd % $n }
function Galaxy($x,$y,$w,$h){
    Rct $b $x $y $w $h $purp
    for($i=0;$i -lt ($w*$h/3);$i++){ $px=$x+(Rnd $w); $py=$y+(Rnd $h); $r=Rnd 6
        if($r -eq 0){ Rct $b $px $py 1 1 (C 'B060D0') } elseif($r -eq 1){ Rct $b $px $py 1 1 (C '4060C0') }
        elseif($r -eq 2){ Rct $b $px $py 1 1 $purpD } elseif($r -eq 3){ Rct $b $px $py 1 1 $purpL } }
    for($i=0;$i -lt ($w*$h/9);$i++){ Rct $b ($x+(Rnd $w)) ($y+(Rnd $h)) 1 1 (C 'FFFFFF') }
}
# head (chubby face) - standard humanoid UV
Rct $b 8 0 8 8 $skinD; Rct $b 16 0 8 8 $skinD
Rct $b 0 8 8 8 $skin; Rct $b 8 8 8 8 $skin; Rct $b 16 8 8 8 $skin; Rct $b 24 8 8 8 $skinD
Rct $b 10 11 1 1 (C 'FFFFFF'); Rct $b 11 11 1 1 (C '2A2018')
Rct $b 13 11 1 1 (C '2A2018'); Rct $b 14 11 1 1 (C 'FFFFFF')
Rct $b 11 13 2 1 $skinD
Rct $b 10 15 4 1 (C '7A4A44')
Rct $b 8 14 2 2 (C 'E0B48A'); Rct $b 14 14 2 2 (C 'E0B48A')
# hood on the hat layer
Galaxy 40 0 8 8; Galaxy 32 8 8 8; Galaxy 48 8 8 8; Galaxy 56 8 8 8
Rct $b 40 8 8 2 $purpD
# body (galaxy hoodie)
Galaxy 16 16 24 16
Rct $b 20 28 8 4 (C 'B060D0')
# right arm (40,16) + left arm (32,48)
Galaxy 40 16 16 16
Rct $b 44 28 4 4 $skin; Rct $b 40 28 4 4 $skin; Rct $b 48 28 4 4 $skin; Rct $b 52 28 4 4 $skin
Galaxy 32 48 16 16
Rct $b 36 60 4 4 $skin; Rct $b 32 60 4 4 $skin; Rct $b 40 60 4 4 $skin; Rct $b 44 60 4 4 $skin
# right leg (0,16) + left leg (16,48)
Rct $b 0 16 16 16 $pants; Rct $b 0 30 16 2 (C '101018')
Rct $b 16 48 16 16 $pants; Rct $b 16 62 16 2 (C '101018')
$b.Save("$edir\krave_monster.png",[System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose()
"  krave_monster rebuilt as 64x64 humanoid"

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
