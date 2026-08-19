# Every texture and model the Mom Cobb boss fight needs:
#   - her 64x64 boss skin (deliberately NOT the plain mom.png NPC skin)
#   - the five household objects she throws
#   - the confiscated Krave box she eats to heal
#   - two item icons + models for her loot, and her spawn egg model
# Modern skin format: left arm at (32,48), left leg at (16,48).
Add-Type -AssemblyName System.Drawing
$repoRoot = Split-Path -Parent $PSScriptRoot
$edir = "$repoRoot\src\main\resources\assets\barbarajones\textures\entity"
$idir = "$repoRoot\src\main\resources\assets\barbarajones\textures\item"
$mdir = "$repoRoot\src\main\resources\assets\barbarajones\models\item"
New-Item -ItemType Directory -Force $edir,$idir,$mdir | Out-Null

function C([string]$h,[int]$a=255){ [System.Drawing.Color]::FromArgb($a,
    [Convert]::ToInt32($h.Substring(0,2),16),[Convert]::ToInt32($h.Substring(2,2),16),[Convert]::ToInt32($h.Substring(4,2),16)) }
function Rct($b,$x,$y,$w,$h,$c){ for($i=0;$i -lt $w;$i++){for($j=0;$j -lt $h;$j++){
    if(($x+$i) -lt $b.Width -and ($y+$j) -lt $b.Height -and ($x+$i) -ge 0 -and ($y+$j) -ge 0){ $b.SetPixel($x+$i,$y+$j,$c) } }} }
function Disc($b,$cx,$cy,$r,$c){ for($x=0;$x -lt $b.Width;$x++){ for($y=0;$y -lt $b.Height;$y++){
    $dx=$x-$cx; $dy=$y-$cy; if(($dx*$dx+$dy*$dy) -le ($r*$r)){ $b.SetPixel($x,$y,$c) } }} }
function NewBmp([int]$w,[int]$h){ $b = New-Object System.Drawing.Bitmap $w,$h,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    Rct $b 0 0 $w $h (C '000000' 0); return $b }

$written = New-Object System.Collections.Generic.List[string]
function Save($b,$path){ $b.Save($path,[System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose(); $script:written.Add($path) }

# ---------------------------------------------------------------------------
# 1. mom_boss.png - the boss skin
# ---------------------------------------------------------------------------
$skin   = C 'C99A72'; $skinD = C 'A87A55'; $skinL = C 'DBAE86'
$hair   = C '2A1D18'; $hairL = C '3E2C24'; $grey  = C '8A8078'
$top    = C '7A2233'; $topD  = C '55151F'; $topL  = C '93304A'
$apron  = C 'D8CFC0'; $apronD= C 'B4A896'
$jean   = C '2C3550'; $jeanD = C '1D2436'
$shoe   = C '30231C'

$b = NewBmp 64 64

# ---- head ----
Rct $b 8 0 8 8 $hair                        # top of head
Rct $b 16 0 8 8 $skinD                      # underside
Rct $b 0 8 8 8 $skin                        # right side
Rct $b 8 8 8 8 $skin                        # FRONT
Rct $b 16 8 8 8 $skin                       # left side
Rct $b 24 8 8 8 $hair                       # back of head - all hair
# hair sweeps down over the sides and a hard fringe across the front
Rct $b 8 8 8 3 $hair
Rct $b 0 8 8 4 $hair; Rct $b 16 8 8 4 $hair
Rct $b 0 8 2 8 $hair; Rct $b 22 8 2 8 $hair
Rct $b 10 8 1 2 $hairL; Rct $b 13 8 1 2 $hairL
Rct $b 25 9 1 6 $grey                       # the grey streak she is very aware of
Rct $b 1 10 1 4 $grey
# eyes: narrow, level, absolutely unimpressed
Rct $b 10 12 2 1 (C 'F2EDE6'); Rct $b 10 12 1 1 (C '3B2A20')
Rct $b 13 12 2 1 (C 'F2EDE6'); Rct $b 14 12 1 1 (C '3B2A20')
Rct $b 10 11 2 1 $hair; Rct $b 13 11 2 1 $hair          # low, flat brows
# nose + a mouth pressed into a line
Rct $b 12 13 1 2 $skinD
Rct $b 11 15 3 1 (C '7E4A46')
Rct $b 10 14 1 1 $skinD; Rct $b 14 14 1 1 $skinD        # tension lines at the jaw
Rct $b 9 13 1 1 $skinL; Rct $b 15 13 1 1 $skinL

# ---- body: blouse under a house apron ----
Rct $b 16 16 24 16 $top
Rct $b 20 20 8 12 $top                      # front panel
Rct $b 21 21 6 10 $apron                    # apron bib
Rct $b 21 21 6 1 $apronD
Rct $b 20 26 8 1 $apronD                    # apron tie across the waist
Rct $b 23 23 2 3 $apronD                    # pocket
Rct $b 16 16 24 1 $topD                     # shoulder seam
Rct $b 16 30 24 2 $topD                     # hem
Rct $b 32 20 8 12 $topD                     # back panel reads darker

# ---- arms: sleeves to the elbow, bare forearm, folded-arm look ----
Rct $b 40 16 16 16 $top                     # right arm
Rct $b 40 24 16 8 $skin                     # forearm
Rct $b 40 29 16 3 $skinD                    # hand
Rct $b 44 16 4 2 $topL
Rct $b 32 48 16 16 $top                     # left arm
Rct $b 32 56 16 8 $skin
Rct $b 32 61 16 3 $skinD
Rct $b 36 48 4 2 $topL

# ---- legs: jeans and house shoes ----
Rct $b 0 16 16 16 $jean                     # right leg
Rct $b 0 29 16 3 $shoe
Rct $b 0 16 16 1 $jeanD
Rct $b 4 22 2 4 $jeanD                      # crease
Rct $b 16 48 16 16 $jean                    # left leg
Rct $b 16 61 16 3 $shoe
Rct $b 16 48 16 1 $jeanD
Rct $b 20 54 2 4 $jeanD

Save $b "$edir\mom_boss.png"

# ---------------------------------------------------------------------------
# 2. the five thrown household objects (16x16 billboards)
# ---------------------------------------------------------------------------
# TV remote
$b = NewBmp 16 16
Rct $b 6 1 5 14 (C '1C1C1E'); Rct $b 6 1 5 1 (C '3A3A3E'); Rct $b 6 14 5 1 (C '0C0C0E')
Rct $b 7 3 3 2 (C 'C0392B'); Rct $b 7 6 1 1 (C '888888'); Rct $b 9 6 1 1 (C '888888')
Rct $b 7 8 1 1 (C '888888'); Rct $b 9 8 1 1 (C '888888'); Rct $b 7 10 3 1 (C '5A5A60')
Save $b "$edir\mom_object_remote.png"

# slipper
$b = NewBmp 16 16
Rct $b 2 7 12 6 (C 'B0587A'); Rct $b 2 7 12 1 (C 'D07A9A'); Rct $b 2 12 12 1 (C '7E3A55')
Rct $b 3 5 7 3 (C 'E0A8C0'); Rct $b 3 5 7 1 (C 'F2CBDC')       # fluffy trim
Rct $b 11 9 3 2 (C '8E4462')
Save $b "$edir\mom_object_slipper.png"

# frying pan
$b = NewBmp 16 16
Disc $b 6 8 5 (C '2A2A2E'); Disc $b 6 8 4 (C '3E3E44'); Disc $b 6 7 2 (C '55555C')
Rct $b 10 7 6 2 (C '20201F'); Rct $b 14 6 2 4 (C '141414')
Save $b "$edir\mom_object_pan.png"

# phone charger: brick plus a whipping cable
$b = NewBmp 16 16
Rct $b 3 3 6 6 (C 'ECECEC'); Rct $b 3 3 6 1 (C 'FFFFFF'); Rct $b 3 8 6 1 (C 'B8B8B8')
Rct $b 4 5 1 2 (C '8A8A8A'); Rct $b 7 5 1 2 (C '8A8A8A')
foreach($p in @(@(9,9),@(10,10),@(11,11),@(12,11),@(13,12),@(13,13),@(12,14))){ Rct $b $p[0] $p[1] 1 1 (C 'E8E8E8') }
Rct $b 11 14 3 1 (C 'C8C8C8')
Save $b "$edir\mom_object_phone.png"

# laundry basket
$b = NewBmp 16 16
Rct $b 2 4 12 9 (C '3E6FA8'); Rct $b 2 4 12 1 (C '5B90CC'); Rct $b 2 12 12 1 (C '2A4E78')
for($x=3;$x -lt 14;$x+=2){ Rct $b $x 5 1 7 (C '2A4E78') }
for($y=6;$y -lt 12;$y+=2){ Rct $b 3 $y 10 1 (C '5B90CC') }
Rct $b 1 5 1 3 (C '2A4E78'); Rct $b 14 5 1 3 (C '2A4E78')       # handles
Rct $b 4 2 8 2 (C 'E8E4DC')                                     # laundry still in it
Save $b "$edir\mom_object_basket.png"

# ---------------------------------------------------------------------------
# 3. mom_krave_stash.png - the confiscated box (32x32, whole-face UV)
# ---------------------------------------------------------------------------
$box  = C '4A2A16'; $boxL = C '6B3E22'; $choc = C '2E1A0E'; $cream = C 'F0E0C0'
$b = NewBmp 32 32
Rct $b 0 0 32 32 $box
Rct $b 0 0 32 3 $boxL; Rct $b 0 29 32 3 $choc                   # lid and shadowed base
Rct $b 3 5 26 12 $choc                                          # front panel
for($i=0;$i -lt 40;$i++){
    $px = 4 + (Get-Random -Maximum 24); $py = 6 + (Get-Random -Maximum 10)
    if((Get-Random -Maximum 3) -eq 0){ Rct $b $px $py 2 1 $cream } else { Rct $b $px $py 1 1 $boxL }
}
Rct $b 5 8 3 2 $cream; Rct $b 9 8 2 2 $cream                    # a suggestion of the wordmark
Rct $b 12 8 3 2 $cream; Rct $b 16 8 2 2 $cream; Rct $b 19 8 3 2 $cream
# the red tape she put across it
Rct $b 0 19 32 4 (C 'B02020'); Rct $b 0 19 32 1 (C 'D84040'); Rct $b 0 22 32 1 (C '801414')
for($x=1;$x -lt 31;$x+=4){ Rct $b $x 20 2 2 (C 'F0E8E0') }      # torn-off lettering
Save $b "$edir\mom_krave_stash.png"

# ---------------------------------------------------------------------------
# 4. loot item icons + models
# ---------------------------------------------------------------------------
# moms_tv_remote
$b = NewBmp 16 16
Rct $b 5 1 6 14 (C '1C1C1E'); Rct $b 5 1 6 1 (C '46464C'); Rct $b 5 14 6 1 (C '0A0A0C')
Rct $b 6 2 4 3 (C '2A2A30'); Rct $b 6 3 4 1 (C '101012')        # screen strip
Rct $b 6 6 4 2 (C 'C0392B')
Rct $b 6 9 1 1 (C '9A9A9A'); Rct $b 8 9 1 1 (C '9A9A9A')
Rct $b 6 11 1 1 (C '9A9A9A'); Rct $b 8 11 1 1 (C '9A9A9A')
Rct $b 6 13 3 1 (C '5A5A60')
Save $b "$idir\moms_tv_remote.png"

# confiscated_krave
$b = NewBmp 16 16
Rct $b 3 2 10 12 $box; Rct $b 3 2 10 1 $boxL; Rct $b 3 13 10 1 $choc
Rct $b 4 4 8 6 $choc
Rct $b 5 5 2 1 $cream; Rct $b 8 5 1 1 $cream; Rct $b 10 5 1 1 $cream
Rct $b 5 7 1 1 $boxL; Rct $b 7 8 1 1 $cream; Rct $b 9 7 1 1 $boxL
for($i=0;$i -lt 9;$i++){ Rct $b (3+$i) (2+$i) 1 1 (C 'B02020'); Rct $b (12-$i) (2+$i) 1 1 (C 'B02020') }
Rct $b 7 7 2 2 (C 'D84040')
Save $b "$idir\confiscated_krave.png"

$gen = '{' + [Environment]::NewLine + '  "parent": "minecraft:item/generated",' + [Environment]::NewLine +
       '  "textures": { "layer0": "barbarajones:item/NAME" }' + [Environment]::NewLine + '}'
$enc = New-Object System.Text.UTF8Encoding($false)
foreach($n in @('moms_tv_remote','confiscated_krave')){
    [System.IO.File]::WriteAllText("$mdir\$n.json", $gen.Replace('NAME',$n), $enc)
    $written.Add("$mdir\$n.json")
}
$egg = '{' + [Environment]::NewLine + '  "parent": "minecraft:item/template_spawn_egg"' + [Environment]::NewLine + '}'
[System.IO.File]::WriteAllText("$mdir\mom_cobb_boss_spawn_egg.json", $egg, $enc)
$written.Add("$mdir\mom_cobb_boss_spawn_egg.json")

# ---------------------------------------------------------------------------
# report - a silent asset failure is the one that costs a whole build cycle
# ---------------------------------------------------------------------------
$missing = 0
foreach($p in $written){
    if(Test-Path $p){ "  OK       $p" } else { "  MISSING  $p"; $missing++ }
}
if($missing -gt 0){ Write-Error "$missing Mom-boss asset(s) were not written." } else { "All $($written.Count) Mom-boss assets written." }
