# Converts the legacy 64x32 skins to modern 64x64 (1.20.1 humanoid models sample
# the left arm/leg from y=48, which simply does not exist in the old format), and
# regenerates the Krave Monster as a proper 64x64 humanoid skin. Also creates the
# missing housing_query icon + model.
Add-Type -AssemblyName System.Drawing

$root = 'C:\Users\ADMIN\BarbaraJonesMod1201\src\main\resources\assets\barbarajones'
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
"done"
