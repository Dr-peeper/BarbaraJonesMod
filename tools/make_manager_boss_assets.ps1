# The Manager boss fight: two 64x64 skins (the boss and his Associates), the
# termination-notice sheet, the filing-cabinet atlas, three loot icons, and the
# item / spawn-egg models that go with them.
#
# Skins are modern format - left arm at (32,48), left leg at (16,48). The boss
# skin also paints two regions the vanilla humanoid rig never touches, because
# ManagerModel bolts extra cubes onto the torso there:
#     tie    -> texOffs(0,32), an 8x10 footprint
#     collar -> texOffs(0,44), an 18x3 footprint
# Leave those blank and the tie renders as transparent nothing.
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
function P($b,$x,$y,$c){ if($x -ge 0 -and $x -lt $b.Width -and $y -ge 0 -and $y -lt $b.Height){ $b.SetPixel($x,$y,$c) } }
function NewBmp($w,$h){ New-Object System.Drawing.Bitmap $w,$h,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }

$written = @()
function Save($b,$dir,$name){
    $path = Join-Path $dir "$name.png"
    $b.Save($path,[System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose()
    $script:written += $path
}

# =============================================================================
# 1. manager_boss.png - charcoal suit, dead grey face, blood-red tie
# =============================================================================
$b = NewBmp 64 64
Rct $b 0 0 64 64 (C '000000' 0)

$suit  = C '2A2C33'; $suitD = C '1A1C21'; $suitL = C '3B3E48'
$shirt = C 'E9E9F0'; $shirtD= C 'C7C7D2'
$tie   = C '8E1218'; $tieL  = C 'B4181F'; $tieD  = C '5C0A0E'
$skin  = C 'C6B4A4'; $skinD = C 'A6947F'; $skinL = C 'D8C8B8'
$hair  = C '15151A'; $hairL = C '26262E'
$gold  = C 'D8A63A'; $shoe  = C '0E0E12'

# ---- head ----
Rct $b 8 0 8 8 $hair                        # top of skull
Rct $b 16 0 8 8 $skinD                      # underside
Rct $b 0 8 8 8 $skin                        # right
Rct $b 8 8 8 8 $skin                        # FRONT
Rct $b 16 8 8 8 $skin                       # left
Rct $b 24 8 8 8 $skin                       # back
# slicked hair, hard side part on the left of the face
Rct $b 8 8 8 3 $hair
Rct $b 0 8 8 3 $hair; Rct $b 16 8 8 3 $hair; Rct $b 24 8 8 4 $hair
Rct $b 12 8 1 3 $hairL                      # the part
Rct $b 8 11 1 1 $hair; Rct $b 15 11 1 1 $hair
# dead eyes: no iris colour, just a pinprick pupil in too much white
Rct $b 10 12 2 1 (C 'FFFFFF'); P $b 11 12 (C '101014')
Rct $b 13 12 2 1 (C 'FFFFFF'); P $b 13 12 (C '101014')
Rct $b 10 11 2 1 $skinD; Rct $b 13 11 2 1 $skinD    # heavy brow
# nose, and a mouth that has never once been a smile
Rct $b 12 13 1 2 $skinD
Rct $b 11 15 3 1 (C '7A6250')
Rct $b 9 14 1 2 $skinD; Rct $b 14 14 1 2 $skinD     # gaunt cheeks
P $b 10 9 $skinL; P $b 14 9 $skinL

# ---- body: buttoned suit jacket over a white shirt ----
Rct $b 16 16 24 16 $suit
Rct $b 20 20 8 12 $suit                     # front panel
Rct $b 23 20 2 12 $shirt                    # shirt showing between the lapels
Rct $b 22 20 1 12 $suitL; Rct $b 25 20 1 12 $suitL   # lapel edges
Rct $b 21 20 1 12 $suitD; Rct $b 26 20 1 12 $suitD
Rct $b 23 20 2 2 $tie                       # knot peeking above the collar cube
Rct $b 16 30 24 2 $suitD                    # hem shadow
Rct $b 32 20 8 12 $suit; Rct $b 32 20 8 1 $suitL     # back, with a yoke seam
P $b 26 26 $gold                            # jacket button

# ---- arms ----
Rct $b 40 16 16 16 $suit                    # right arm
Rct $b 40 28 16 2 $shirt                    # cuff
Rct $b 40 30 16 2 $skin                     # hand
Rct $b 44 16 4 2 $suitL                     # shoulder highlight
P $b 44 29 $gold                            # cufflink
Rct $b 32 48 16 16 $suit                    # left arm
Rct $b 32 60 16 2 $shirt
Rct $b 32 62 16 2 $skin
Rct $b 36 48 4 2 $suitL
P $b 36 61 $gold

# ---- legs: matching trousers, polished shoes ----
Rct $b 0 16 16 16 $suit                     # right leg
Rct $b 0 16 16 1 $suitL
Rct $b 4 20 1 12 $suitD                     # crease down the front
Rct $b 0 30 16 2 $shoe
Rct $b 16 48 16 16 $suit                    # left leg
Rct $b 16 48 16 1 $suitL
Rct $b 20 52 1 12 $suitD
Rct $b 16 62 16 2 $shoe

# ---- extra cubes: the necktie (0,32) and the shirt collar (0,44) ----
Rct $b 0 32 8 10 $tie                       # whole tie footprint, all six faces
Rct $b 1 33 3 3 $tieD                       # knot, on the front face
Rct $b 1 36 3 6 $tieL                       # blade catching the light
P $b 2 40 $tieD; P $b 2 38 $tieD            # diagonal stripe hint
Rct $b 1 32 3 1 $tieD                       # top face reads as the knot's crown
Rct $b 0 44 18 3 $shirt                     # collar footprint, all six faces
Rct $b 0 44 18 1 $shirtD                    # a little grime where it meets the neck

Save $b $edir 'manager_boss'

# =============================================================================
# 2. manager_minion.png - the Associate: cheap grey suit, sunglasses
# =============================================================================
$b = NewBmp 64 64
Rct $b 0 0 64 64 (C '000000' 0)

$gsuit = C '4C505A'; $gsuitD= C '35383F'; $gsuitL= C '5E626D'
$gshirt= C 'DCDCD4'
$gtie  = C '1E2C50'; $gtieL = C '2E4272'
$gskin = C 'B78F6E'; $gskinD= C '96745A'
$ghair = C '3A2A1C'
$gshoe = C '1A1A1E'

Rct $b 8 0 8 8 $ghair
Rct $b 16 0 8 8 $gskinD
Rct $b 0 8 8 8 $gskin
Rct $b 8 8 8 8 $gskin
Rct $b 16 8 8 8 $gskin
Rct $b 24 8 8 8 $gskin
Rct $b 8 8 8 2 $ghair
Rct $b 0 8 8 2 $ghair; Rct $b 16 8 8 2 $ghair; Rct $b 24 8 8 3 $ghair
# sunglasses: one black bar straight across, indoors, at night
Rct $b 8 11 8 2 (C '101014')
Rct $b 9 11 2 1 (C '2A2A34'); Rct $b 13 11 2 1 (C '2A2A34')
Rct $b 0 11 8 2 (C '101014'); Rct $b 16 11 8 2 (C '101014')   # temples
Rct $b 12 13 1 2 $gskinD
Rct $b 11 15 3 1 (C '6A5038')

Rct $b 16 16 24 16 $gsuit
Rct $b 20 20 8 12 $gsuit
Rct $b 23 20 2 12 $gshirt
Rct $b 22 20 1 12 $gsuitL; Rct $b 25 20 1 12 $gsuitL
# the tie is painted straight onto the shirt here - Associates get no extra cubes
Rct $b 23 21 2 8 $gtie
P $b 23 21 $gtieL; P $b 24 25 $gtieL
Rct $b 16 30 24 2 $gsuitD
Rct $b 32 20 8 12 $gsuit

Rct $b 40 16 16 16 $gsuit
Rct $b 40 28 16 2 $gshirt
Rct $b 40 30 16 2 $gskin
Rct $b 32 48 16 16 $gsuit
Rct $b 32 60 16 2 $gshirt
Rct $b 32 62 16 2 $gskin

Rct $b 0 16 16 16 $gsuit
Rct $b 0 30 16 2 $gshoe
Rct $b 16 48 16 16 $gsuit
Rct $b 16 62 16 2 $gshoe

Save $b $edir 'manager_minion'

# =============================================================================
# 3. termination_notice.png - the homing paperwork, 16x16
# =============================================================================
$b = NewBmp 16 16
Rct $b 0 0 16 16 (C '000000' 0)
$paper = C 'F2F0E6'; $paperD = C 'D4D1C4'; $ink = C '30303A'; $red = C 'B81C1C'
Rct $b 2 0 12 16 $paper
Rct $b 2 0 12 1 $paperD; Rct $b 2 15 12 1 $paperD
Rct $b 13 0 1 16 $paperD                       # curled right edge
Rct $b 3 2 10 2 $red                           # TERMINATED banner
Rct $b 4 2 1 2 (C 'E04A4A'); Rct $b 8 2 1 2 (C 'E04A4A')
foreach($y in 6,8,10){ Rct $b 3 $y 9 1 $ink }  # body text
Rct $b 3 12 5 1 $ink                           # short signature line
Rct $b 9 11 4 4 $red                           # stamp block
Rct $b 10 12 2 2 (C 'E8E0D8')

Save $b $edir 'termination_notice'

# =============================================================================
# 4. filing_cabinet.png - 32x32 atlas: front (0,0), side (16,0), top (0,16)
# =============================================================================
$b = NewBmp 32 32
Rct $b 0 0 32 32 (C '000000' 0)
$steel = C '6E737A'; $steelD = C '4C5057'; $steelL = C '8A9099'
$label = C 'D8D4C4'; $handle = C 'B8BCC2'

# --- front: four drawers ---
Rct $b 0 0 16 16 $steel
for($d=0;$d -lt 4;$d++){
    $y = $d*4
    Rct $b 0 $y 16 4 $steel
    Rct $b 0 $y 16 1 $steelL                   # drawer lip
    Rct $b 0 ($y+3) 16 1 $steelD               # gap below
    Rct $b 6 ($y+1) 4 1 $handle                # pull
    Rct $b 2 ($y+1) 3 2 $label                 # label slot
}
Rct $b 0 0 1 16 $steelD; Rct $b 15 0 1 16 $steelL

# --- side: plain panel with a seam ---
Rct $b 16 0 16 16 $steel
Rct $b 16 0 16 1 $steelL
Rct $b 16 15 16 1 $steelD
Rct $b 18 0 1 16 $steelD                       # rolled edge
Rct $b 29 0 1 16 $steelD

# --- top: dusty steel, a coffee ring and a stack of loose paper ---
Rct $b 0 16 16 16 $steelL
Rct $b 0 16 16 1 $steel
Rct $b 0 31 16 1 $steelD
Rct $b 9 20 4 1 (C '5A3A22'); Rct $b 9 23 4 1 (C '5A3A22')
Rct $b 9 21 1 2 (C '5A3A22'); Rct $b 12 21 1 2 (C '5A3A22')
Rct $b 2 25 6 5 (C 'F2F0E6')
Rct $b 2 25 6 1 (C 'D4D1C4'); Rct $b 3 27 4 1 (C '30303A')

Save $b $edir 'filing_cabinet'

# =============================================================================
# 5. loot icons
# =============================================================================
# pink_slip: the actual pink slip
$b = NewBmp 16 16
Rct $b 0 0 16 16 (C '000000' 0)
$pink = C 'F2A8C0'; $pinkD = C 'D2809C'; $ink2 = C '3A2A34'
Rct $b 3 1 10 14 $pink
Rct $b 3 1 10 1 $pinkD; Rct $b 12 1 1 14 $pinkD
Rct $b 4 3 8 2 (C 'B81C4A')
foreach($y in 7,9,11){ Rct $b 4 $y 7 1 $ink2 }
Rct $b 4 13 4 1 $ink2
Save $b $idir 'pink_slip'

# severance_check: a cheque for an insulting amount
$b = NewBmp 16 16
Rct $b 0 0 16 16 (C '000000' 0)
$chk = C 'CFE0C4'; $chkD = C 'A8BC9C'; $chkL = C 'E4F0DC'
Rct $b 1 4 14 8 $chk
Rct $b 1 4 14 1 $chkL; Rct $b 1 11 14 1 $chkD
Rct $b 2 6 5 1 (C '2A3A2A'); Rct $b 2 8 8 1 (C '2A3A2A')
Rct $b 10 5 4 4 (C 'FFFFFF'); Rct $b 10 5 4 1 $chkD
Rct $b 11 6 1 2 (C '1A6A2A'); Rct $b 10 6 3 1 (C '1A6A2A')   # dollar sign
Rct $b 2 10 6 1 (C '5A6A5A')
Save $b $idir 'severance_check'

# employee_of_the_month: a plaque nobody wanted
$b = NewBmp 16 16
Rct $b 0 0 16 16 (C '000000' 0)
$wood = C '6B4A2E'; $woodD = C '4A3320'; $plate = C 'D8B44A'; $plateL = C 'F0D878'
Rct $b 1 2 14 12 $wood
Rct $b 1 2 14 1 (C '8A6440'); Rct $b 1 13 14 1 $woodD
Rct $b 1 2 1 12 $woodD; Rct $b 14 2 1 12 $woodD
Rct $b 3 4 10 8 $plate
Rct $b 3 4 10 1 $plateL
Rct $b 5 6 6 1 (C '6A5218'); Rct $b 5 8 6 1 (C '6A5218')
P $b 8 10 (C '6A5218'); P $b 7 10 (C '6A5218'); P $b 9 10 (C '6A5218')
Save $b $idir 'employee_of_the_month'

# =============================================================================
# 6. item + spawn-egg models
# =============================================================================
$flatItems = @('pink_slip','severance_check','employee_of_the_month')
foreach($i in $flatItems){
    $path = "$mdir\$i.json"
@"
{
  "parent": "minecraft:item/generated",
  "textures": { "layer0": "barbarajones:item/$i" }
}
"@ | Set-Content $path -Encoding utf8 -NoNewline
    $written += $path
}

$eggs = @('the_manager_spawn_egg','manager_minion_spawn_egg')
foreach($e in $eggs){
    $path = "$mdir\$e.json"
@"
{
  "parent": "minecraft:item/template_spawn_egg"
}
"@ | Set-Content $path -Encoding utf8 -NoNewline
    $written += $path
}

# =============================================================================
# report - a silent failure to write an asset has burned this project before
# =============================================================================
$ok = 0; $missing = 0
foreach($p in $written){
    if(Test-Path $p){ "  OK       $p"; $ok++ } else { "  MISSING  $p"; $missing++ }
}
"make_manager_boss_assets: $ok written, $missing missing"
if($missing -gt 0){ exit 1 }
