# ============================================================================
#  Krave Automation - all block, item and GUI textures for
#  com.barbarajones.v2.machines
#
#  Original pixel art, drawn with System.Drawing exactly like the other
#  tools/make_*.ps1 scripts. Nothing outside these three texture folders is
#  touched: no lang, no models, no blockstates, no sounds.json.
#
#  Palette is the mod's: chocolate browns, cereal-crumb tan, warm highlights,
#  Krave box red and purple. Every machine shares one casing so a production
#  line reads as one factory, and each machine gets a distinct front so you can
#  tell a Grinder from a Toaster at a glance across a room.
#
#  Animated textures are vertical strips (16 wide, N*16 tall) with a matching
#  .png.mcmeta, which is how vanilla does prismarine and magma.
#
#  Every file written is read back off disk and checked for dimensions and for
#  having real pixel art in it (a flat-colour square fails), and the script
#  exits non-zero if anything does not verify.
# ============================================================================

Add-Type -AssemblyName System.Drawing

$repoRoot = Split-Path -Parent $PSScriptRoot
$blockDir = "$repoRoot\src\main\resources\assets\barbarajones\textures\block"
$itemDir  = "$repoRoot\src\main\resources\assets\barbarajones\textures\item"
$guiDir   = "$repoRoot\src\main\resources\assets\barbarajones\textures\gui\container"
New-Item -ItemType Directory -Force $blockDir, $itemDir, $guiDir | Out-Null

$script:written = @()

function C([string]$h, [int]$a = 255) {
    [System.Drawing.Color]::FromArgb($a,
        [Convert]::ToInt32($h.Substring(0, 2), 16),
        [Convert]::ToInt32($h.Substring(2, 2), 16),
        [Convert]::ToInt32($h.Substring(4, 2), 16))
}

function NewImg([int]$w, [int]$h) {
    New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
}

function Rct($b, [int]$x, [int]$y, [int]$w, [int]$h, $c) {
    for ($i = 0; $i -lt $w; $i++) {
        for ($j = 0; $j -lt $h; $j++) {
            $px = $x + $i; $py = $y + $j
            if ($px -ge 0 -and $py -ge 0 -and $px -lt $b.Width -and $py -lt $b.Height) {
                $b.SetPixel($px, $py, $c)
            }
        }
    }
}

function Px($b, [int]$x, [int]$y, $c) { Rct $b $x $y 1 1 $c }

function Save($b, [string]$path) {
    $b.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $b.Dispose()
    $script:written += $path
}

function WriteMeta([string]$pngPath, [int]$frametime) {
    $meta = "{`n  `"animation`": {`n    `"frametime`": $frametime`n  }`n}`n"
    # Explicit BOM-less UTF-8. Set-Content -Encoding utf8 on Windows PowerShell
    # 5.1 writes a BOM, and a BOM at the head of a .mcmeta makes the resource
    # loader reject the file with a JSON syntax error.
    $noBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText("$pngPath.mcmeta", $meta, $noBom)
    $script:written += "$pngPath.mcmeta"
}

# ---- palette ---------------------------------------------------------------
$clear     = C '000000' 0
$outline   = C '221610'
$steelD    = C '473528'
$steel     = C '6A5140'
$steelL    = C '8E7059'
$steelHi   = C 'B0906F'
$chocD     = C '3A2010'
$choc      = C '5C3418'
$chocL     = C '81501F'
$crumb     = C 'B4813F'
$crumbL    = C 'D8A85C'
$crumbHi   = C 'F0CC8A'
$milk      = C 'F2E9D6'
$boxRed    = C 'C2262A'
$boxRedD   = C '7C1216'
$purple    = C '6B3FA0'
$purpleL   = C 'A57BD8'
$glow      = C 'FFA23C'
$glowHot   = C 'FFD06A'
$glowCore  = C 'FFF0C0'
$syrup     = C '46220C'
$syrupL    = C '7A3D14'
$glass     = C 'C8DCE4'
$glassD    = C '8FA9B4'
$card      = C 'C08E52'
$cardD     = C '86602F'
$green     = C '4C8A3A'

# ============================================================================
#  Shared machine casing
# ============================================================================

function BaseCasing($b) {
    # riveted brown-steel panel with a darker skirt and a lit top edge
    Rct $b 0 0 16 16 $steel
    Rct $b 0 0 16 1 $steelHi
    Rct $b 0 1 16 1 $steelL
    Rct $b 0 14 16 2 $steelD
    Rct $b 0 0 1 16 $steelL
    Rct $b 15 0 1 16 $steelD
    # rivets in the corners
    foreach ($p in @(@(2, 3), @(13, 3), @(2, 12), @(13, 12))) {
        Px $b $p[0] $p[1] $steelHi
        Px $b $p[0] ($p[1] + 1) $steelD
    }
}

# ---- machine_side ----------------------------------------------------------
$side = NewImg 16 16
BaseCasing $side
# a vertical cooling louvre down the middle so plain sides are not flat
for ($y = 4; $y -le 11; $y++) {
    Rct $side 6 $y 4 1 $steelD
    Px $side 6 $y $steelL
}
Rct $side 5 3 6 1 $outline        # louvre lintel
Rct $side 5 12 6 1 $outline
Px $side 9 5 $chocD               # grime in the vents
Px $side 8 9 $chocD
Rct $side 2 6 2 4 $chocL          # a smear of chocolate down one side
Px $side 2 10 $choc
Save $side "$blockDir\krave_machine_side.png"

# ---- machine_top -----------------------------------------------------------
$top = NewImg 16 16
BaseCasing $top
Rct $top 3 3 10 10 $steelD
Rct $top 4 4 8 8 $steel
Rct $top 5 5 6 6 $chocD
Rct $top 6 6 4 4 $choc
Rct $top 7 7 2 2 $chocL
Save $top "$blockDir\krave_machine_top.png"

# ---- machine_bottom --------------------------------------------------------
$bottom = NewImg 16 16
Rct $bottom 0 0 16 16 $steelD
# drainage grate: alternating bars with a lit edge, plus four mounting feet
for ($y = 2; $y -lt 15; $y += 3) {
    Rct $bottom 2 $y 12 2 $outline
    Rct $bottom 2 $y 12 1 $steel
}
Rct $bottom 0 0 16 1 $steelL
Rct $bottom 0 15 16 1 $outline
Rct $bottom 0 0 1 16 $steel
foreach ($p in @(@(1, 1), @(13, 1), @(1, 13), @(13, 13))) {
    Rct $bottom $p[0] $p[1] 2 2 $steelHi
    Px $bottom ($p[0] + 1) ($p[1] + 1) $chocD
}
Save $bottom "$blockDir\krave_machine_bottom.png"

# ============================================================================
#  Machine fronts. Each is the casing plus a distinctive mechanism, and each
#  has a two-frame animated "_on" twin that the RUNNING blockstate swaps to.
# ============================================================================

# helper: draw one frame of a front into a strip at vertical offset $oy
function FrontPlantation($b, [int]$oy, [bool]$on) {
    Rct $b 0 $oy 16 16 $steel
    Rct $b 0 $oy 16 1 $steelHi
    Rct $b 0 ($oy + 14) 16 2 $steelD
    # window onto growing pods
    Rct $b 2 ($oy + 2) 12 10 $outline
    Rct $b 3 ($oy + 3) 10 8 $chocD
    # trunk
    Rct $b 7 ($oy + 3) 2 8 $choc
    Px $b 7 ($oy + 4) $chocL
    # three pods, brighter when running
    $podA = if ($on) { $crumbL } else { $crumb }
    $podB = if ($on) { $crumbHi } else { $crumbL }
    Rct $b 4 ($oy + 4) 2 3 $podA
    Px $b 4 ($oy + 4) $podB
    Rct $b 10 ($oy + 5) 2 3 $podA
    Px $b 11 ($oy + 5) $podB
    Rct $b 5 ($oy + 8) 2 2 $podA
    if ($on) { Rct $b 3 ($oy + 3) 10 1 (C 'FFD06A' 90) }
}

function FrontGrinder($b, [int]$oy, [bool]$on) {
    Rct $b 0 $oy 16 16 $steel
    Rct $b 0 $oy 16 1 $steelHi
    Rct $b 0 ($oy + 14) 16 2 $steelD
    # hopper mouth
    Rct $b 2 ($oy + 1) 12 3 $steelD
    Rct $b 3 ($oy + 2) 10 1 $chocD
    # grinding drum with teeth
    Rct $b 2 ($oy + 5) 12 6 $outline
    Rct $b 3 ($oy + 6) 10 4 $steelD
    $tooth = if ($on) { $crumbHi } else { $steelL }
    for ($x = 3; $x -lt 13; $x += 3) {
        Rct $b $x ($oy + 6) 2 4 $tooth
        Px $b $x ($oy + 7) $chocD
    }
    if ($on) { Rct $b 3 ($oy + 9) 10 1 $crumb }
    # dust chute
    Rct $b 5 ($oy + 12) 6 2 $chocD
    if ($on) { Rct $b 6 ($oy + 13) 4 1 $crumb }
}

function FrontMixer($b, [int]$oy, [bool]$on) {
    Rct $b 0 $oy 16 16 $steel
    Rct $b 0 $oy 16 1 $steelHi
    Rct $b 0 ($oy + 14) 16 2 $steelD
    # glass bowl
    Rct $b 2 ($oy + 3) 12 9 $outline
    Rct $b 3 ($oy + 4) 10 7 $glassD
    Rct $b 4 ($oy + 5) 8 5 $glass
    # batter level
    $batter = if ($on) { $chocL } else { $choc }
    Rct $b 4 ($oy + 8) 8 2 $batter
    if ($on) {
        Px $b 5 ($oy + 7) $chocL
        Px $b 9 ($oy + 7) $chocL
        Px $b 11 ($oy + 8) $crumb
    }
    # paddle shaft
    Rct $b 7 ($oy + 1) 2 4 $steelD
    Rct $b 5 ($oy + 6) 6 1 $steelL
}

function FrontExtruder($b, [int]$oy, [bool]$on) {
    Rct $b 0 $oy 16 16 $steel
    Rct $b 0 $oy 16 1 $steelHi
    Rct $b 0 ($oy + 14) 16 2 $steelD
    # pressure barrel
    Rct $b 1 ($oy + 3) 14 5 $steelD
    Rct $b 2 ($oy + 4) 12 3 $steel
    Rct $b 2 ($oy + 4) 12 1 $steelL
    # die plate with four holes
    Rct $b 3 ($oy + 9) 10 3 $outline
    $holeC = if ($on) { $crumbHi } else { $chocD }
    for ($x = 4; $x -lt 13; $x += 3) { Rct $b $x ($oy + 10) 2 1 $holeC }
    # extruded pieces falling
    if ($on) {
        Rct $b 4 ($oy + 12) 2 2 $crumb
        Rct $b 10 ($oy + 13) 2 1 $crumbL
    }
}

function FrontToaster($b, [int]$oy, [bool]$on) {
    Rct $b 0 $oy 16 16 $steel
    Rct $b 0 $oy 16 1 $steelHi
    Rct $b 0 ($oy + 14) 16 2 $steelD
    # oven mouth
    Rct $b 1 ($oy + 3) 14 9 $outline
    $inner = if ($on) { $glow } else { $chocD }
    Rct $b 2 ($oy + 4) 12 7 $inner
    if ($on) {
        Rct $b 3 ($oy + 5) 10 5 $glowHot
        Rct $b 5 ($oy + 6) 6 3 $glowCore
    }
    # heating bars
    $bar = if ($on) { $boxRed } else { $steelD }
    Rct $b 2 ($oy + 5) 12 1 $bar
    Rct $b 2 ($oy + 9) 12 1 $bar
    # cereal on the belt inside
    Rct $b 4 ($oy + 7) 2 2 $crumb
    Rct $b 9 ($oy + 7) 2 2 $crumbL
}

function FrontBoxer($b, [int]$oy, [bool]$on) {
    Rct $b 0 $oy 16 16 $steel
    Rct $b 0 $oy 16 1 $steelHi
    Rct $b 0 ($oy + 14) 16 2 $steelD
    # folding arms
    $arm = if ($on) { $steelHi } else { $steelL }
    Rct $b 1 ($oy + 2) 3 6 $arm
    Rct $b 12 ($oy + 2) 3 6 $arm
    # the box being folded
    Rct $b 4 ($oy + 4) 8 9 $outline
    Rct $b 5 ($oy + 5) 6 7 $boxRed
    Rct $b 5 ($oy + 5) 6 2 $boxRedD
    Rct $b 5 ($oy + 11) 6 1 $purple
    # a milk K on the front of the box
    Rct $b 6 ($oy + 7) 1 4 $milk
    Px $b 9 ($oy + 7) $milk
    Px $b 8 ($oy + 8) $milk
    Px $b 8 ($oy + 9) $milk
    Px $b 9 ($oy + 10) $milk
    if ($on) { Rct $b 4 ($oy + 3) 8 1 $glowHot }
}

function FrontDepot($b, [int]$oy, [bool]$on) {
    Rct $b 0 $oy 16 16 $steel
    Rct $b 0 $oy 16 1 $steelHi
    Rct $b 0 ($oy + 14) 16 2 $steelD
    # roller shutter
    Rct $b 1 ($oy + 2) 14 10 $outline
    for ($y = 3; $y -le 11; $y += 2) { Rct $b 2 ($oy + $y) 12 1 $steelL }
    for ($y = 4; $y -le 11; $y += 2) { Rct $b 2 ($oy + $y) 12 1 $steelD }
    # stacked cases visible through the gap
    Rct $b 3 ($oy + 8) 4 3 $boxRed
    Rct $b 8 ($oy + 9) 4 2 $purple
    # shipping lamp
    $lamp = if ($on) { $green } else { $steelD }
    Rct $b 12 ($oy + 3) 2 2 $lamp
    if ($on) { Px $b 12 ($oy + 3) $glowCore }
}

$fronts = @{
    'cocoa_plantation' = ${function:FrontPlantation}
    'krave_grinder'    = ${function:FrontGrinder}
    'krave_mixer'      = ${function:FrontMixer}
    'krave_extruder'   = ${function:FrontExtruder}
    'krave_toaster'    = ${function:FrontToaster}
    'krave_boxer'      = ${function:FrontBoxer}
    'krave_depot'      = ${function:FrontDepot}
}

foreach ($name in $fronts.Keys) {
    $draw = $fronts[$name]

    # static front, machine idle
    $img = NewImg 16 16
    & $draw $img 0 $false
    Save $img "$blockDir\${name}_front.png"

    # animated front, machine running: two 16x16 frames stacked vertically, lit
    $anim = NewImg 16 32
    & $draw $anim 0 $true
    & $draw $anim 16 $false
    # then unlit, so a working machine visibly pulses at frametime 6 (~0.3s)
    Save $anim "$blockDir\${name}_front_on.png"
    WriteMeta "$blockDir\${name}_front_on.png" 6
}

# ============================================================================
#  Conveyor
# ============================================================================

# belt surface, 4 frames scrolling one pixel per frame
$belt = NewImg 16 64
for ($f = 0; $f -lt 4; $f++) {
    $oy = $f * 16
    Rct $belt 0 $oy 16 16 $chocD
    Rct $belt 0 $oy 2 16 $steelD
    Rct $belt 14 $oy 2 16 $steelD
    Rct $belt 2 $oy 12 16 $choc
    # tread bars marching along the belt
    for ($k = 0; $k -lt 4; $k++) {
        $y = ($k * 4 + $f) % 16
        Rct $belt 2 ($oy + $y) 12 1 $chocL
        Rct $belt 2 ($oy + (($y + 1) % 16)) 12 1 $chocD
    }
    # rail highlights
    Rct $belt 0 $oy 1 16 $steelL
    Rct $belt 15 $oy 1 16 $steelD
}
Save $belt "$blockDir\krave_conveyor_belt.png"
WriteMeta "$blockDir\krave_conveyor_belt.png" 2

# belt side rail
$beltSide = NewImg 16 16
Rct $beltSide 0 0 16 16 $clear
Rct $beltSide 0 13 16 3 $steelD
Rct $beltSide 0 13 16 1 $steelL
Rct $beltSide 0 15 16 1 $outline
for ($x = 1; $x -lt 16; $x += 4) { Px $beltSide $x 14 $steelHi }
Save $beltSide "$blockDir\krave_conveyor_side.png"

# ============================================================================
#  Extractor
# ============================================================================

$exFront = NewImg 16 16
BaseCasing $exFront
Rct $exFront 3 3 10 10 $outline
Rct $exFront 4 4 8 8 $chocD
# funnel throat
for ($k = 0; $k -lt 4; $k++) {
    Rct $exFront (4 + $k) (4 + $k) (8 - 2 * $k) 1 $crumb
}
Rct $exFront 7 8 2 4 $steelL
Save $exFront "$blockDir\krave_extractor_front.png"

$exSide = NewImg 16 16
BaseCasing $exSide
Rct $exSide 4 6 8 4 $steelD
Rct $exSide 5 7 6 2 $chocL
Rct $exSide 2 7 2 2 $boxRed
Save $exSide "$blockDir\krave_extractor_side.png"

$exBack = NewImg 16 16
BaseCasing $exBack
Rct $exBack 5 5 6 6 $outline
Rct $exBack 6 6 4 4 $steelD
Rct $exBack 7 7 2 2 $crumb
Save $exBack "$blockDir\krave_extractor_back.png"

# ============================================================================
#  Items
# ============================================================================

# ---- dense_krave_syrup: same bottle, fuller, gold banded -------------------
$it = NewImg 16 16
Rct $it 0 0 16 16 $clear
Rct $it 6 1 4 2 $crumbHi
Rct $it 6 3 4 1 $crumbL
Rct $it 5 4 6 1 $glassD
Rct $it 3 5 10 10 $outline
Rct $it 4 6 8 8 $glass
Rct $it 4 6 8 8 $syrup
Rct $it 4 6 8 1 $syrupL
Rct $it 3 9 10 2 $crumbHi        # gold label band
Rct $it 3 9 10 1 $crumbL
Px $it 5 7 $milk
Px $it 11 12 $syrupL
Save $it "$itemDir\dense_krave_syrup.png"

# ---- krave_batter: a bowl of glossy dough ----------------------------------
$it = NewImg 16 16
Rct $it 0 0 16 16 $clear
Rct $it 2 7 12 6 $outline
Rct $it 3 8 10 4 $glassD
Rct $it 3 8 10 3 $choc
Rct $it 4 8 8 1 $chocL
Px $it 5 9 $crumb
Px $it 9 10 $crumb
Rct $it 6 5 4 3 $chocL           # a dollop rising out of the bowl
Px $it 7 4 $crumbL
Rct $it 2 12 12 1 $steelD
Save $it "$itemDir\krave_batter.png"

# ---- raw_krave_piece: three pale unbaked pillows ---------------------------
$it = NewImg 16 16
Rct $it 0 0 16 16 $clear
foreach ($p in @(@(2, 4), @(8, 3), @(5, 9))) {
    $x = $p[0]; $y = $p[1]
    Rct $it $x $y 6 5 $outline
    Rct $it ($x + 1) ($y + 1) 4 3 $crumb
    Rct $it ($x + 1) ($y + 1) 4 1 $crumbL
    Px $it ($x + 2) ($y + 2) $chocL
    Px $it ($x + 4) ($y + 3) $chocD
}
Save $it "$itemDir\raw_krave_piece.png"

# ---- krave_carton: flat-packed cardboard -----------------------------------
$it = NewImg 16 16
Rct $it 0 0 16 16 $clear
Rct $it 2 3 12 10 $cardD
Rct $it 3 4 10 8 $card
Rct $it 3 7 10 1 $cardD          # fold line
Rct $it 7 4 1 8 $cardD           # fold line
Rct $it 4 5 3 1 $chocD           # printed mark
Rct $it 4 9 5 1 $chocD
Rct $it 2 3 12 1 $crumbL
Save $it "$itemDir\krave_carton.png"

# ---- boxed_krave: the sealed shipping case ---------------------------------
$it = NewImg 16 16
Rct $it 0 0 16 16 $clear
Rct $it 1 2 14 12 $outline
Rct $it 2 3 12 10 $boxRed
Rct $it 2 3 12 3 $boxRedD
Rct $it 2 4 12 1 $boxRed
Rct $it 2 11 12 2 $purple
Rct $it 2 11 12 1 $purpleL
Rct $it 7 3 2 10 $crumbL         # packing tape down the middle
Rct $it 7 3 1 10 $crumbHi
# milk K stencil on the left panel
Rct $it 3 6 1 4 $milk
Px $it 6 6 $milk
Px $it 5 7 $milk
Px $it 5 8 $milk
Px $it 6 9 $milk
# stencilled dots on the right panel
Px $it 11 6 $milk
Px $it 12 8 $milk
Px $it 10 9 $milk
Save $it "$itemDir\boxed_krave.png"

# ============================================================================
#  GUI backgrounds - 256x256, vanilla furnace layout
#
#  panel        (0,0)   176x166
#  flame full   (176,0)  14x14
#  arrow full   (176,14) 24x17
# ============================================================================

$panel      = C 'C6C6C6'
$panelDark  = C '8B8B8B'
$panelLight = C 'FFFFFF'
$slotBg     = C '8B8B8B'
$slotShadow = C '373737'
$labelInk   = C '404040'

function DrawSlot($b, [int]$x, [int]$y) {
    Rct $b $x $y 18 18 $slotShadow
    Rct $b ($x + 1) ($y + 1) 16 16 $slotBg
    Rct $b ($x + 1) ($y + 17) 17 1 $panelLight
    Rct $b ($x + 17) ($y + 1) 1 17 $panelLight
}

function DrawArrowShape($b, [int]$ox, [int]$oy, $shaft, $edge) {
    # 24x17: a 16px shaft and an 8px head, vanilla proportions
    for ($i = 0; $i -lt 16; $i++) {
        Rct $b ($ox + $i) ($oy + 5) 1 7 $shaft
    }
    Rct $b $ox ($oy + 5) 16 1 $edge
    Rct $b $ox ($oy + 11) 16 1 $edge
    for ($k = 0; $k -lt 8; $k++) {
        $half = 8 - $k
        $top = 8 - $half
        Rct $b ($ox + 16 + $k) ($oy + $top) 1 (2 * $half + 1) $shaft
        Px $b ($ox + 16 + $k) ($oy + $top) $edge
        Px $b ($ox + 16 + $k) ($oy + $top + 2 * $half) $edge
    }
}

function DrawFlameShape($b, [int]$ox, [int]$oy, $body, $core, $tip) {
    # 14x14 teardrop
    Rct $b ($ox + 4) ($oy + 11) 6 3 $body
    Rct $b ($ox + 3) ($oy + 8) 8 3 $body
    Rct $b ($ox + 4) ($oy + 5) 6 3 $body
    Rct $b ($ox + 5) ($oy + 3) 4 2 $body
    Rct $b ($ox + 6) ($oy + 1) 2 2 $tip
    Rct $b ($ox + 5) ($oy + 9) 4 4 $core
    Rct $b ($ox + 6) ($oy + 6) 2 3 $core
    Px $b ($ox + 6) ($oy + 11) $tip
}

function BuildGui([string]$file, [int[]]$inputXs, [int]$inputY, [bool]$hasFuel, [bool]$hasOutput, $accent) {
    $g = NewImg 256 256

    # transparent everywhere outside the 176x166 panel and the two sprites
    Rct $g 0 0 256 256 $clear

    Rct $g 0 0 176 166 $panel
    Rct $g 0 0 176 1 $panelLight
    Rct $g 0 0 1 166 $panelLight
    Rct $g 0 165 176 1 $panelDark
    Rct $g 175 0 1 166 $panelDark

    # a cream title band with a per-machine accent underline. Cream rather than
    # chocolate because vanilla draws container titles in dark grey and does not
    # let a screen override that without reimplementing renderLabels - a dark
    # band here would make every machine's name unreadable.
    Rct $g 4 4 168 11 $crumbHi
    Rct $g 4 4 168 1 $milk
    Rct $g 4 13 168 2 $accent

    foreach ($x in $inputXs) { DrawSlot $g ($x - 1) ($inputY - 1) }
    if ($hasFuel) { DrawSlot $g 55 52 }
    if ($hasOutput) {
        DrawSlot $g 115 34
    }
    else {
        # The Depot has nowhere for product to land - it leaves the world. Draw
        # the village the arrow points at, so the screen answers "where does this
        # go" without a line of text.
        Rct $g 112 42 22 10 $chocD          # walls
        Rct $g 113 43 20 8 $card
        Rct $g 110 34 26 8 $boxRedD         # roof
        Rct $g 111 35 24 6 $boxRed
        Rct $g 118 45 4 6 $chocD            # door
        Rct $g 126 44 4 4 $glass            # window
        Rct $g 126 44 4 1 $glassD
    }

    # empty arrow and, when there is a burner, the empty flame recess
    DrawArrowShape $g 79 34 $panelDark $slotShadow
    if ($hasFuel) { DrawFlameShape $g 56 36 $panelDark $slotShadow $slotShadow }

    # player inventory: three rows plus hotbar, standard geometry
    for ($row = 0; $row -lt 3; $row++) {
        for ($col = 0; $col -lt 9; $col++) { DrawSlot $g (7 + $col * 18) (83 + $row * 18) }
    }
    for ($col = 0; $col -lt 9; $col++) { DrawSlot $g (7 + $col * 18) 141 }

    # sprites in the right-hand strip
    DrawFlameShape $g 176 0 $glow $glowHot $glowCore
    DrawArrowShape $g 176 14 $crumbHi $chocL

    Save $g "$guiDir\$file.png"
}

BuildGui 'cocoa_plantation' @(56) 17 $true  $true  $choc
BuildGui 'krave_grinder'    @(56) 17 $true  $true  $choc
BuildGui 'krave_mixer'      @(38, 56, 74) 17 $true $true $choc
BuildGui 'krave_extruder'   @(56) 17 $true  $true  $choc
BuildGui 'krave_toaster'    @(56) 17 $true  $true  $choc
BuildGui 'krave_boxer'      @(47, 65) 17 $true $true $choc
BuildGui 'krave_depot'      @(56) 35 $false $false $purple

# ============================================================================
#  Verification - read every file back off disk
# ============================================================================

$failed = @()
foreach ($path in $script:written) {
    if (-not (Test-Path $path)) {
        $failed += "MISSING  $path"
        continue
    }
    if ($path.EndsWith('.mcmeta')) {
        $text = Get-Content -Path $path -Raw
        if ($text -notmatch '"frametime"') { $failed += "BAD META $path" }
        else { Write-Output ("ok  {0,-42} {1}" -f (Split-Path -Leaf $path), $text.Replace("`r", '').Replace("`n", ' ')) }
        continue
    }

    $img = [System.Drawing.Image]::FromFile($path)
    try {
        $bmp = New-Object System.Drawing.Bitmap $img
        $w = $bmp.Width; $h = $bmp.Height
        # count distinct colours, and how many pixels carry any alpha at all
        $colours = New-Object 'System.Collections.Generic.HashSet[int]'
        $opaque = 0
        for ($x = 0; $x -lt $w; $x++) {
            for ($y = 0; $y -lt $h; $y++) {
                $p = $bmp.GetPixel($x, $y)
                if ($p.A -gt 0) { $opaque++ }
                [void]$colours.Add($p.ToArgb())
            }
        }
        $bmp.Dispose()
    }
    finally {
        $img.Dispose()
    }

    if ($opaque -eq 0) {
        $failed += "EMPTY    $path"
    }
    elseif ($colours.Count -lt 4) {
        $failed += ("FLAT     {0} (only {1} colours - placeholder, not art)" -f $path, $colours.Count)
    }
    else {
        Write-Output ("ok  {0,-42} {1}x{2}  {3} colours  {4} opaque px" -f (Split-Path -Leaf $path), $w, $h, $colours.Count, $opaque)
    }
}

Write-Output ''
if ($failed.Count -gt 0) {
    Write-Output "FAILED ($($failed.Count)):"
    $failed | ForEach-Object { Write-Output "  $_" }
    exit 1
}
Write-Output "all $($script:written.Count) files written and verified"
