# Textures for the airline module: the aircraft skin and the six staff uniforms.
#
# Entity skins use the exact UV math the game uses for a cuboid at texOffs (u,v)
# with size (w,h,d):
#   top    = (u+d,       v,       w, d)
#   bottom = (u+d+w,     v,       w, d)
#   right  = (u,         v+d,     d, h)
#   front  = (u+d,       v+d,     w, h)
#   left   = (u+d+w,     v+d,     d, h)
#   back   = (u+2d+w,    v+d,     w, h)
# Paint-Box below implements exactly that, so every texOffs/size pair here matches
# the addBox() calls in client/PlaneModel.java 1:1.
#
# Staff are drawn on the standard 64x64 player layout because they render through
# HumanoidLikeRenderer, which bakes ModelLayers.PLAYER.
#
# Idempotent - safe to re-run. Verifies every file by reloading it and checking
# actual pixel dimensions, not just Test-Path.

Add-Type -AssemblyName System.Drawing

$repoRoot  = Split-Path -Parent $PSScriptRoot
$entityDir = "$repoRoot\src\main\resources\assets\barbarajones\textures\entity"
New-Item -ItemType Directory -Force $entityDir | Out-Null

function C([string]$h, [int]$a = 255) {
    [System.Drawing.Color]::FromArgb($a,
        [Convert]::ToInt32($h.Substring(0, 2), 16),
        [Convert]::ToInt32($h.Substring(2, 2), 16),
        [Convert]::ToInt32($h.Substring(4, 2), 16))
}

function Rct($bmp, [int]$x, [int]$y, [int]$w, [int]$h, $col) {
    for ($i = 0; $i -lt $w; $i++) {
        for ($j = 0; $j -lt $h; $j++) {
            $px = $x + $i; $py = $y + $j
            if ($px -ge 0 -and $py -ge 0 -and $px -lt $bmp.Width -and $py -lt $bmp.Height) {
                $bmp.SetPixel($px, $py, $col)
            }
        }
    }
}

# Paints all six faces of a cuboid's UV footprint, with the top face lightened and
# the bottom darkened so the shape reads without any shading in the model.
function Paint-Box($bmp, [int]$u, [int]$v, [int]$w, [int]$h, [int]$d, $col) {
    $lighter = [System.Drawing.Color]::FromArgb(255,
        [Math]::Min(255, $col.R + 26), [Math]::Min(255, $col.G + 26), [Math]::Min(255, $col.B + 26))
    $darker = [System.Drawing.Color]::FromArgb(255,
        [Math]::Max(0, $col.R - 34), [Math]::Max(0, $col.G - 34), [Math]::Max(0, $col.B - 34))

    Rct $bmp ($u + $d)          $v        $w $d $lighter   # top
    Rct $bmp ($u + $d + $w)     $v        $w $d $darker    # bottom
    Rct $bmp  $u               ($v + $d)  $d $h $col       # right
    Rct $bmp ($u + $d)         ($v + $d)  $w $h $col       # front
    Rct $bmp ($u + $d + $w)    ($v + $d)  $d $h $col       # left
    Rct $bmp ($u + 2 * $d + $w) ($v + $d) $w $h $darker    # back
}

function Save-Bmp($bmp, [string]$path, [int]$expectW, [int]$expectH) {
    $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    $check = [System.Drawing.Bitmap]::FromFile($path)
    $w = $check.Width; $h = $check.Height
    $check.Dispose()
    if ($w -ne $expectW -or $h -ne $expectH) {
        throw "$path wrote as ${w}x${h}, expected ${expectW}x${expectH}"
    }
    Write-Host ("  {0}  {1}x{2}" -f (Split-Path -Leaf $path), $w, $h)
}

# ---------------------------------------------------------------------------
# The aircraft. 256x256; offsets mirror PlaneModel.createBodyLayer exactly.
# ---------------------------------------------------------------------------
Write-Host "Aircraft:"
$plane = New-Object System.Drawing.Bitmap 256, 256
$g = [System.Drawing.Graphics]::FromImage($plane)
$g.Clear([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
$g.Dispose()

$hull   = C 'ECEFF4'   # fuselage white
$navy   = C '1B3A6B'   # cheatline / tail navy
$glass  = C '2D4F7C'   # flight-deck glazing
$metal  = C '9AA5B1'   # engine nacelles
$rubber = C '23262B'   # gear legs

Paint-Box $plane   0   0  64 10 10 $hull      # fuselage
Paint-Box $plane 110  32   8  8  8 $glass     # cockpit
Paint-Box $plane   0  32  14 18  2 $navy      # tail fin
Paint-Box $plane  36  32  10  2 24 $hull      # tailplane
Paint-Box $plane   0  88  14  2 34 $hull      # left wing
Paint-Box $plane 100  88  14  2 34 $hull      # right wing
Paint-Box $plane   0 164   8  8 14 $metal     # left engine
Paint-Box $plane  48 164   8  8 14 $metal     # right engine
Paint-Box $plane  96 164   2 10  2 $rubber    # nose gear
Paint-Box $plane 108 164   2 10  2 $rubber    # left main gear
Paint-Box $plane 120 164   2 10  2 $rubber    # right main gear

# A cheatline down both flanks of the fuselage. The left/right faces of the
# fuselage box sit at (u, v+d) and (u+d+w, v+d) - 10 wide, 10 tall each.
Rct $plane   0 16 10 3 $navy
Rct $plane  74 16 10 3 $navy
# Window strip along the front face (u+d, v+d) = (10,10), 64 wide.
for ($i = 0; $i -lt 15; $i++) { Rct $plane (14 + $i * 4) 13 2 2 $glass }

Save-Bmp $plane "$entityDir\plane.png" 256 256

# ---------------------------------------------------------------------------
# Staff uniforms. Standard 64x64 player layout: head at (0,0), body at (16,16),
# arms at (40,16)/(32,48), legs at (0,16)/(16,48).
# ---------------------------------------------------------------------------
Write-Host "Staff:"

function New-Staff([string]$name, $jacket, $trousers, $accent) {
    $skin = C 'C8956C'
    $hair = C '3A2A1E'
    $shoe = C '1C1C1E'

    $b = New-Object System.Drawing.Bitmap 64, 64
    $g = [System.Drawing.Graphics]::FromImage($b)
    $g.Clear([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
    $g.Dispose()

    # head 8x8x8 at (0,0) - face in skin, crown and back in hair
    Paint-Box $b 0 0 8 8 8 $skin
    Rct $b 8 0 8 8 $hair          # top of head
    Rct $b 24 8 8 4 $hair         # back of head, upper half
    Rct $b 10 11 2 1 (C '241C16') # eyes
    Rct $b 14 11 2 1 (C '241C16')

    # body 8x12x4 at (16,16)
    Paint-Box $b 16 16 8 12 4 $jacket
    Rct $b 20 20 8 3 $accent      # shoulder flash / lanyard across the chest

    # right arm 4x12x4 at (40,16); left arm at (32,48)
    Paint-Box $b 40 16 4 12 4 $jacket
    Paint-Box $b 32 48 4 12 4 $jacket
    Rct $b 44 24 4 4 $skin        # right hand
    Rct $b 36 56 4 4 $skin        # left hand

    # right leg 4x12x4 at (0,16); left leg at (16,48)
    Paint-Box $b 0 16 4 12 4 $trousers
    Paint-Box $b 16 48 4 12 4 $trousers
    Rct $b 4 24 4 4 $shoe
    Rct $b 20 56 4 4 $shoe

    Save-Bmp $b "$entityDir\$name.png" 64 64
}

New-Staff 'pilot'                  (C '1B2A4A') (C '1B2A4A') (C 'D4AF37')  # navy, gold braid
New-Staff 'flight_attendant'       (C '8C1D33') (C '2B2B32') (C 'E8C7CE')  # burgundy jacket
New-Staff 'gate_agent'             (C '15497A') (C '2B2B32') (C 'F2F4F7')  # airline blue
New-Staff 'security_officer'       (C '2E3238') (C '2E3238') (C 'F5D547')  # charcoal, hi-vis flash
New-Staff 'ground_crew'            (C 'E8641C') (C '2E3238') (C 'F5F27A')  # hi-vis orange
New-Staff 'air_traffic_controller' (C '3C6E52') (C '2B2B32') (C 'DDE4DA')  # tower green

# ---------------------------------------------------------------------------
# Block and item assets.
#
# Without a blockstate + model + item model, a registered block renders as the
# missing-texture cube and its item shows a purple/black square in the hand. All
# five airline blocks shipped that way in the first pass.
# ---------------------------------------------------------------------------
Write-Host "Blocks:"
$assets     = "$repoRoot\src\main\resources\assets\barbarajones"
$blockTex   = "$assets\textures\block"
$itemTex    = "$assets\textures\item"
$stateDir   = "$assets\blockstates"
$bModelDir  = "$assets\models\block"
$iModelDir  = "$assets\models\item"
New-Item -ItemType Directory -Force $blockTex, $itemTex, $stateDir, $bModelDir, $iModelDir | Out-Null

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Write-Json([string]$path, [string]$json) {
    [System.IO.File]::WriteAllText($path, $json, $utf8NoBom)
}

# A 16x16 block face: base colour, subtle noise, and an optional detail painter.
function New-BlockTexture([string]$name, $base, [scriptblock]$detail = $null) {
    $b = New-Object System.Drawing.Bitmap 16, 16
    $rand = New-Object System.Random 20260820
    for ($x = 0; $x -lt 16; $x++) {
        for ($y = 0; $y -lt 16; $y++) {
            $n = $rand.Next(-12, 13)
            $b.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(255,
                [Math]::Max(0, [Math]::Min(255, $base.R + $n)),
                [Math]::Max(0, [Math]::Min(255, $base.G + $n)),
                [Math]::Max(0, [Math]::Min(255, $base.B + $n))))
        }
    }
    if ($detail) { & $detail $b }
    Save-Bmp $b "$blockTex\$name.png" 16 16
}

New-BlockTexture 'airport_core' (C '4C566A') {
    param($b)
    Rct $b 3 3 10 10 (C '2E3440')
    Rct $b 5 5 6 6 (C '5E81AC')
    Rct $b 7 7 2 2 (C 'ECEFF4')
}
New-BlockTexture 'runway' (C '3B3F45') {
    param($b)
    # Centreline dashes, so a laid runway reads as a runway from the air.
    Rct $b 7 1 2 5 (C 'E5E9F0')
    Rct $b 7 10 2 5 (C 'E5E9F0')
}
New-BlockTexture 'gate' (C '5E81AC') {
    param($b)
    Rct $b 2 2 12 12 (C '88C0D0')
    Rct $b 7 2 2 12 (C '4C6A8A')
    Rct $b 2 7 12 2 (C '4C6A8A')
}
New-BlockTexture 'security_check' (C '6E7681') {
    param($b)
    Rct $b 1 1 14 3 (C '2E3440')
    Rct $b 1 12 14 3 (C '2E3440')
    Rct $b 6 5 4 6 (C 'F5D547')
}
New-BlockTexture 'baggage_claim' (C '4A4F57') {
    param($b)
    # Conveyor slats.
    for ($i = 0; $i -lt 16; $i += 4) { Rct $b 0 $i 16 2 (C '32363C') }
}

foreach ($blk in @('airport_core', 'runway', 'gate', 'security_check', 'baggage_claim')) {
    Write-Json "$stateDir\$blk.json" "{ `"variants`": { `"`": { `"model`": `"barbarajones:block/$blk`" } } }"
    Write-Json "$bModelDir\$blk.json" "{ `"parent`": `"minecraft:block/cube_all`", `"textures`": { `"all`": `"barbarajones:block/$blk`" } }"
    Write-Json "$iModelDir\$blk.json" "{ `"parent`": `"barbarajones:block/$blk`" }"
    Write-Host "  $blk  (blockstate + model + item model)"
}

Write-Host "Items:"
$pass = New-Object System.Drawing.Bitmap 16, 16
$g = [System.Drawing.Graphics]::FromImage($pass)
$g.Clear([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
$g.Dispose()
Rct $pass 1 4 14 9 (C 'F7F3E8')        # card stock
Rct $pass 1 4 14 1 (C 'C9C2B0')        # top edge shadow
Rct $pass 10 5 1 7 (C 'B8AE98')        # perforated stub line
Rct $pass 2 6 6 1 (C '15497A')         # printed route
Rct $pass 2 8 5 1 (C '6E7681')
Rct $pass 2 10 7 1 (C '6E7681')
for ($i = 0; $i -lt 4; $i++) { Rct $pass (11 + $i) 6 1 5 (C '2E3440') }  # barcode
Save-Bmp $pass "$itemTex\boarding_pass.png" 16 16
Write-Json "$iModelDir\boarding_pass.json" "{ `"parent`": `"minecraft:item/generated`", `"textures`": { `"layer0`": `"barbarajones:item/boarding_pass`" } }"
Write-Host "  boarding_pass  (texture + model)"

# ---------------------------------------------------------------------------
# Language entries.
#
# Merged into en_us.json in place rather than regenerated. tools/make_lang.ps1
# rewrites that file from scratch off an item list that has been stale for
# several releases - running it now would delete the screen.*, advancement.* and
# mob entries added since. So follow make_manual.ps1's lead: read, insert if
# absent, write back. Idempotent.
# ---------------------------------------------------------------------------
Write-Host "Language:"
$langPath = "$repoRoot\src\main\resources\assets\barbarajones\lang\en_us.json"

$entries = [ordered]@{
    'entity.barbarajones.plane'                   = 'Airplane'
    'entity.barbarajones.pilot'                   = 'Pilot'
    'entity.barbarajones.flight_attendant'        = 'Flight Attendant'
    'entity.barbarajones.gate_agent'              = 'Gate Agent'
    'entity.barbarajones.security_officer'        = 'Security Officer'
    'entity.barbarajones.ground_crew'             = 'Ground Crew'
    'entity.barbarajones.air_traffic_controller'  = 'Air Traffic Controller'
    'block.barbarajones.airport_core'             = 'Airport Core'
    'block.barbarajones.runway'                   = 'Runway'
    'block.barbarajones.gate'                     = 'Gate'
    'block.barbarajones.security_check'           = 'Security Checkpoint'
    'block.barbarajones.baggage_claim'            = 'Baggage Claim'
    'item.barbarajones.boarding_pass'             = 'Boarding Pass'
    'item.barbarajones.airport_core'              = 'Airport Core'
    'item.barbarajones.runway'                    = 'Runway'
    'item.barbarajones.gate'                      = 'Gate'
    'item.barbarajones.security_check'            = 'Security Checkpoint'
    'item.barbarajones.baggage_claim'             = 'Baggage Claim'
}

$txt = [System.IO.File]::ReadAllText($langPath)
$added = 0
$pending = @()
foreach ($key in $entries.Keys) {
    if ($txt -notmatch [regex]::Escape("`"$key`"")) {
        $pending += "  `"$key`": `"$($entries[$key])`""
        $added++
    }
}

if ($added -gt 0) {
    # Splice in just before the closing brace, extending the previous line's comma.
    $trimmed = $txt.TrimEnd()
    if (-not $trimmed.EndsWith('}')) { throw "en_us.json does not end in '}' - refusing to edit" }
    $body = $trimmed.Substring(0, $trimmed.LastIndexOf('}')).TrimEnd()
    if ($body.EndsWith(',')) { $body = $body.Substring(0, $body.Length - 1) }
    $out = $body + ",`n" + ($pending -join ",`n") + "`n}`n"
    [System.IO.File]::WriteAllText($langPath, $out, (New-Object System.Text.UTF8Encoding($false)))
    Write-Host "  $added entries added"
} else {
    Write-Host "  already present"
}

# Parse it back - a lang file with a trailing comma or a stray brace loads as an
# empty table and every name in the game silently becomes a raw key.
try {
    $null = Get-Content $langPath -Raw | ConvertFrom-Json
    Write-Host "  en_us.json parses"
} catch {
    throw "en_us.json is not valid JSON after the merge: $_"
}

Write-Host ""
Write-Host "Airline textures written to $entityDir" -ForegroundColor Green
