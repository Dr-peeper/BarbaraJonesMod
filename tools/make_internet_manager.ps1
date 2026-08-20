# The Internet Manager boss fight: his 64x64 combat skin (hi-vis/hard-hat
# palette matching InternetManagerActor - see cinematic/actor/InternetManagerActor.java
# for the reference palette), four item icons, and three block faces for the
# Service Call Box, plus their item/spawn-egg models.
#
# UV layout for internet_manager.png mirrors make_manager_boss_assets.ps1's
# manager_boss.png exactly (head/body/arms/legs at the same rectangles - that
# script is the proven-working reference for this rig) with ONE extra cube at
# (0,32,8,10): the headset earpiece InternetManagerModel.java adds at that
# exact free UV origin. Nothing else on a base HumanoidModel mesh (no
# PlayerModel jacket/hat overlay - see InternetManagerModel's class doc) ever
# reads past y=32, so that rectangle is guaranteed free on ANY entity's sheet.
Add-Type -AssemblyName System.Drawing
$repoRoot = Split-Path -Parent $PSScriptRoot
$edir = "$repoRoot\src\main\resources\assets\barbarajones\textures\entity"
$idir = "$repoRoot\src\main\resources\assets\barbarajones\textures\item"
$bdir = "$repoRoot\src\main\resources\assets\barbarajones\textures\block"
New-Item -ItemType Directory -Force $edir,$idir,$bdir | Out-Null

function C([string]$h,[int]$a=255){ [System.Drawing.Color]::FromArgb($a,
    [Convert]::ToInt32($h.Substring(0,2),16),[Convert]::ToInt32($h.Substring(2,2),16),[Convert]::ToInt32($h.Substring(4,2),16)) }
function Rct($b,$x,$y,$w,$h,$c){ for($i=0;$i -lt $w;$i++){for($j=0;$j -lt $h;$j++){
    if(($x+$i) -lt $b.Width -and ($y+$j) -lt $b.Height -and ($x+$i) -ge 0 -and ($y+$j) -ge 0){ $b.SetPixel($x+$i,$y+$j,$c) } }} }
function P($b,$x,$y,$c){ if($x -ge 0 -and $x -lt $b.Width -and $y -ge 0 -and $y -lt $b.Height){ $b.SetPixel($x,$y,$c) } }
function NewBmp($w,$h){ New-Object System.Drawing.Bitmap $w,$h,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }

$written = @()
function Save($b,$dir,$name){
    $path = Join-Path $dir "$name.png"
    $b.Save($path,[System.Drawing.Imaging.ImageFormat]::Png)
    $w = $b.Width; $h = $b.Height
    $b.Dispose()
    $script:written += [PSCustomObject]@{ Path = $path; Width = $w; Height = $h }
}

# =============================================================================
# 1. internet_manager.png - hi-vis, hard-hat-yellow, headset. Palette pulled
#    straight from InternetManagerActor's constants so the boss reads as the
#    same person as the cinematic contractor.
# =============================================================================
$b = NewBmp 64 64
Rct $b 0 0 64 64 (C '000000' 0)

$hivis  = C 'C9E93A'; $hivisD = C '9EBA26'
$hat    = C 'E8B21A'; $hatD   = C 'C5900E'
$leather= C '33241A'; $leatherD = C '1D1510'
$steel  = C '9BA3AA'
$plastic= C '2A2E34'
$skin   = C 'B98157'; $skinD  = C '8A6242'; $skinL = C 'D0A374'
$hair   = C '594B3C'; $shirt  = C '8FA6BE'
$tape   = C 'E4E9EC'

# ---- head: hard-hat yellow crown, skin face, grey headset band ----
Rct $b 8 0 8 8 $hat                         # top of skull - the hat shell
Rct $b 16 0 8 8 $hatD                       # underside/brim shadow
Rct $b 0 8 8 8 $skin                        # right
Rct $b 8 8 8 8 $skin                        # FRONT
Rct $b 16 8 8 8 $skin                       # left
Rct $b 24 8 8 8 $skin                       # back
Rct $b 8 8 8 2 $hat                         # brim line across the forehead
Rct $b 0 8 8 2 $hat; Rct $b 16 8 8 2 $hat; Rct $b 24 8 8 2 $hat
# a grey headset band wrapping the sides, and a moustache under the nose
Rct $b 0 11 2 2 $plastic; Rct $b 16 11 2 2 $plastic
Rct $b 10 12 2 1 (C 'FFFFFF'); P $b 11 12 (C '101014')
Rct $b 13 12 2 1 (C 'FFFFFF'); P $b 13 12 (C '101014')
Rct $b 11 15 3 1 (C '6B5B48')                # moustache
P $b 10 9 $skinL; P $b 14 9 $skinL

# ---- body: hi-vis vest over a chambray shirt, reflective tape band ----
Rct $b 16 16 24 16 $shirt
Rct $b 20 20 8 12 $hivis                    # front panel
Rct $b 21 20 1 12 $hivisD; Rct $b 26 20 1 12 $hivisD
Rct $b 32 20 8 12 $hivis; Rct $b 32 20 8 1 $hivisD   # back panel
Rct $b 20 26 8 1 $tape                      # reflective band, front
Rct $b 32 26 8 1 $tape                      # reflective band, back
Rct $b 16 30 24 2 $hivisD                   # hem shadow

# ---- arms: hi-vis sleeves, skin hands ----
Rct $b 40 16 16 16 $hivis
Rct $b 40 28 16 2 $hivisD
Rct $b 40 30 16 2 $skin
Rct $b 44 16 4 2 $hivisD
Rct $b 32 48 16 16 $hivis
Rct $b 32 60 16 2 $hivisD
Rct $b 32 62 16 2 $skin
Rct $b 36 48 4 2 $hivisD

# ---- legs: khaki trousers, leather boots ----
$khaki = C '6F6449'; $khakiD = C '4E4632'
Rct $b 0 16 16 16 $khaki
Rct $b 0 16 16 1 $khakiD
Rct $b 4 20 1 12 $khakiD
Rct $b 0 30 16 2 $leatherD
Rct $b 16 48 16 16 $khaki
Rct $b 16 48 16 1 $khakiD
Rct $b 20 52 1 12 $khakiD
Rct $b 16 62 16 2 $leatherD

# ---- extra cube: the headset earpiece (0,32) - free on any humanoid mesh ----
Rct $b 0 32 8 10 $plastic
Rct $b 1 33 2 3 $steel
P $b 2 34 (C 'D8DEE4')

Save $b $edir 'internet_manager'

# =============================================================================
# 2. item icons
# =============================================================================
$b = NewBmp 16 16
Rct $b 0 0 16 16 (C '000000' 0)
Rct $b 4 3 8 10 $plastic
Rct $b 5 4 6 3 $steel
Rct $b 6 8 4 1 $leatherD
for($i=0;$i -lt 3;$i++){ P $b (5+$i*2) 5 $tape }
Rct $b 4 12 8 2 $leather
Save $b $idir 'rotary_phone'

$b = NewBmp 16 16
Rct $b 0 0 16 16 (C '000000' 0)
$ipBg = C '1D2A44'; $ipGlow = C '55E0FF'
Rct $b 3 4 10 8 $ipBg
Rct $b 4 5 8 6 $plastic
for($i=0;$i -lt 4;$i++){ P $b (5+$i*2) 6 $ipGlow; P $b (5+$i*2) 9 $ipGlow }
Rct $b 5 3 6 1 $steel
Save $b $idir 'static_ip'

$b = NewBmp 16 16
Rct $b 0 0 16 16 (C '000000' 0)
$fiber = C 'C85B0D'; $fiberD = C '8A3C07'; $fiberGlow = C 'FFF3B0'
for($i=0;$i -lt 5;$i++){
    $y = 2 + $i * 2
    Rct $b (2+($i%2)) $y 11 1 ($(if($i % 2 -eq 0){$fiber}else{$fiberD}))
}
P $b 7 7 $fiberGlow; P $b 8 7 $fiberGlow
Save $b $idir 'fiber_optic_coil'

$b = NewBmp 16 16
Rct $b 0 0 16 16 (C '000000' 0)
Rct $b 3 6 10 3 $plastic
Rct $b 2 4 2 6 $steel; Rct $b 12 4 2 6 $steel
Rct $b 3 3 3 2 $plastic; Rct $b 10 3 3 2 $plastic
Rct $b 6 9 4 2 $leatherD
Save $b $idir 'managers_headset'

# =============================================================================
# 3. service call box - a wall junction box with a red handset on the front
# =============================================================================
$boxBody = C '2A2E34'; $boxBodyD = C '17191D'; $boxTrim = $hat
$b = NewBmp 16 16
Rct $b 0 0 16 16 $boxBodyD
Rct $b 1 1 14 14 $boxBody
for($i=0;$i -lt 4;$i++){ P $b (2+$i*4) 2 $boxTrim }
Save $b $bdir 'service_call_box_top'

$b = NewBmp 16 16
Rct $b 0 0 16 16 $boxBody
Rct $b 0 0 16 2 $boxBodyD; Rct $b 0 14 16 2 $boxBodyD
Rct $b 0 0 2 16 $boxBodyD; Rct $b 14 0 2 16 $boxBodyD
Save $b $bdir 'service_call_box_side'

$b = NewBmp 16 16
Rct $b 0 0 16 16 $boxBody
Rct $b 0 0 16 2 $boxTrim; Rct $b 0 14 16 2 $boxTrim
$phoneRed = C '8E1218'; $phoneRedL = C 'B4181F'
Rct $b 4 5 8 6 $phoneRed
Rct $b 4 5 8 1 $phoneRedL
Rct $b 6 4 4 1 $plastic
Rct $b 5 10 6 1 $plastic
P $b 8 8 (C 'FFE86B')
Save $b $bdir 'service_call_box_front'

# =============================================================================
# 4. item/block/spawn-egg models
# =============================================================================
$mdir = "$repoRoot\src\main\resources\assets\barbarajones\models\item"
New-Item -ItemType Directory -Force $mdir | Out-Null
$flatItems = @('rotary_phone','static_ip','fiber_optic_coil','managers_headset')
foreach($i in $flatItems){
    $path = "$mdir\$i.json"
@"
{
  "parent": "minecraft:item/generated",
  "textures": { "layer0": "barbarajones:item/$i" }
}
"@ | Set-Content $path -Encoding utf8 -NoNewline
    $written += [PSCustomObject]@{ Path = $path; Width = 0; Height = 0 }
}
$eggPath = "$mdir\internet_manager_spawn_egg.json"
'{
  "parent": "minecraft:item/template_spawn_egg"
}' | Set-Content $eggPath -Encoding utf8 -NoNewline
$written += [PSCustomObject]@{ Path = $eggPath; Width = 0; Height = 0 }

# =============================================================================
# report - read every PNG back and check its real dimensions, not just that a
# file landed on disk. A silent zero-byte or wrong-size write has burned this
# project before.
# =============================================================================
$ok = 0; $bad = 0
foreach($entry in $written){
    $p = $entry.Path
    if(-not (Test-Path $p)){ "  MISSING  $p"; $bad++; continue }
    if($p -like '*.png'){
        try{
            $img = [System.Drawing.Image]::FromFile($p)
            $wOk = $img.Width -eq $entry.Width
            $hOk = $img.Height -eq $entry.Height
            $img.Dispose()
            if($wOk -and $hOk){ "  OK       $p  ($($entry.Width)x$($entry.Height))"; $ok++ }
            else { "  BAD-SIZE $p  (expected $($entry.Width)x$($entry.Height))"; $bad++ }
        } catch {
            "  UNREADABLE $p"; $bad++
        }
    } else {
        $len = (Get-Item $p).Length
        if($len -gt 0){ "  OK       $p  ($len bytes)"; $ok++ } else { "  EMPTY    $p"; $bad++ }
    }
}
"make_internet_manager: $ok written, $bad bad"
if($bad -gt 0){ exit 1 }
