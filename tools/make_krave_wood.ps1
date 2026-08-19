# Textures, blockstates and models for the Krave tree and its ore.
#
# The blocks and their recipes were written but no visual assets ever were, so
# all eighteen registered as missing-texture. Everything here is generated so it
# can be regenerated; nothing is hand-placed.
Add-Type -AssemblyName System.Drawing

$repoRoot = Split-Path -Parent $PSScriptRoot
$res  = "$repoRoot\src\main\resources\assets\barbarajones"
$tdir = "$res\textures\block"
$idir = "$res\textures\item"
$bmod = "$res\models\block"
$imod = "$res\models\item"
$bs   = "$res\blockstates"
foreach ($d in @($tdir, $idir, $bmod, $imod, $bs)) {
    if (-not (Test-Path $d)) { New-Item -ItemType Directory -Force $d | Out-Null }
}

function C([string]$h, [int]$a = 255) {
    [System.Drawing.Color]::FromArgb($a,
        [Convert]::ToInt32($h.Substring(0,2),16),
        [Convert]::ToInt32($h.Substring(2,2),16),
        [Convert]::ToInt32($h.Substring(4,2),16))
}
function Px($b,$x,$y,$c) { if ($x -ge 0 -and $y -ge 0 -and $x -lt 16 -and $y -lt 16) { $b.SetPixel($x,$y,$c) } }
function Rct($b,$x,$y,$w,$h,$c) {
    for ($i=0; $i -lt $w; $i++) { for ($j=0; $j -lt $h; $j++) { Px $b ($x+$i) ($y+$j) $c } }
}
function NewImg { New-Object System.Drawing.Bitmap 16,16,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function Save($b,$path) { $b.Save($path,[System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }

$script:seed = 90210
function Rnd([int]$n) { $script:seed = ($script:seed*1103515245 + 12345) -band 0x7fffffff; return $script:seed % $n }

# ---- textures --------------------------------------------------------------
# Krave bark: dark cocoa with lighter grain, so it reads as wood not as chocolate.
$b = NewImg
Rct $b 0 0 16 16 (C '4A3220')
for ($i=0; $i -lt 34; $i++) {
    $x = Rnd 16; $y = Rnd 16; $h = 3 + (Rnd 6)
    $c = @((C '5C3F28'), (C '3B2718'), (C '6A4A2E'))[(Rnd 3)]
    Rct $b $x $y 1 $h $c
}
Rct $b 0 0 16 1 (C '3B2718'); Rct $b 15 0 1 16 (C '3B2718')
Save $b "$tdir\krave_log.png"

# Log top: rings, with a pale core so the cut end reads as a cut end.
$b = NewImg
Rct $b 0 0 16 16 (C '6A4A2E')
foreach ($r in @(6,4,2)) {
    for ($a=0; $a -lt 360; $a += 6) {
        $x = [int](8 + [Math]::Cos($a*[Math]::PI/180)*$r)
        $y = [int](8 + [Math]::Sin($a*[Math]::PI/180)*$r)
        Px $b $x $y (C '4A3220')
    }
}
Rct $b 7 7 2 2 (C 'B9905F')
Save $b "$tdir\krave_log_top.png"

$b = NewImg
Rct $b 0 0 16 16 (C '9A7346')
for ($i=0; $i -lt 22; $i++) { Rct $b (Rnd 16) (Rnd 16) 1 (2+(Rnd 4)) (C 'AA824F') }
Save $b "$tdir\stripped_krave_log.png"

$b = NewImg
Rct $b 0 0 16 16 (C 'AA824F')
foreach ($r in @(5,3)) {
    for ($a=0; $a -lt 360; $a += 8) {
        $x = [int](8 + [Math]::Cos($a*[Math]::PI/180)*$r)
        $y = [int](8 + [Math]::Sin($a*[Math]::PI/180)*$r)
        Px $b $x $y (C '8A6A40')
    }
}
Save $b "$tdir\stripped_krave_log_top.png"

# Leaves: dark foliage with visible cereal pieces caught in it.
$b = NewImg
Rct $b 0 0 16 16 (C '000000' 0)
for ($x=0; $x -lt 16; $x++) {
    for ($y=0; $y -lt 16; $y++) {
        if ((Rnd 10) -lt 8) {
            Px $b $x $y @((C '2F5A22'), (C '3A6B29'), (C '27491C'))[(Rnd 3)]
        }
    }
}
for ($i=0; $i -lt 11; $i++) {
    $x = Rnd 14; $y = Rnd 14
    Rct $b $x $y 2 2 (C '8A5A2A')
    Px $b $x $y (C 'B4783A')
}
Save $b "$tdir\krave_leaves.png"

# Sapling: a sprig with one cereal pip on it.
$b = NewImg
Rct $b 0 0 16 16 (C '000000' 0)
Rct $b 7 8 2 7 (C '4A3220')
foreach ($p in @(@(4,7),@(10,7),@(5,5),@(9,4),@(7,3))) {
    Rct $b $p[0] $p[1] 3 3 (C '3A6B29')
}
Rct $b 6 2 3 3 (C '8A5A2A')
Save $b "$tdir\krave_sapling.png"

# Planks: horizontal boarding.
$b = NewImg
Rct $b 0 0 16 16 (C '6A4A2E')
foreach ($y in @(0,4,8,12)) {
    Rct $b 0 $y 16 1 (C '4A3220')
    for ($i=0; $i -lt 5; $i++) { Rct $b (Rnd 16) ($y+1+(Rnd 3)) (1+(Rnd 3)) 1 (C '7A5636') }
}
Rct $b 5 0 1 4 (C '4A3220'); Rct $b 11 4 1 4 (C '4A3220')
Rct $b 3 8 1 4 (C '4A3220'); Rct $b 9 12 1 4 (C '4A3220')
Save $b "$tdir\krave_planks.png"

# Pod: a cereal box growing off the trunk.
$b = NewImg
Rct $b 0 0 16 16 (C '000000' 0)
Rct $b 4 5 8 9 (C '3A1E6E')
Rct $b 4 5 8 1 (C '5A3A9A'); Rct $b 4 13 8 1 (C '281046')
Rct $b 6 7 4 4 (C 'B060D0')
Rct $b 7 3 2 2 (C '4A3220')
Save $b "$tdir\krave_pod.png"

# Ore: stone with cereal-coloured seams.
foreach ($v in @(@('krave_ore','7A7A7A','6E6E6E'), @('deepslate_krave_ore','4A4A4F','3E3E43'))) {
    $b = NewImg
    Rct $b 0 0 16 16 (C $v[1])
    for ($i=0; $i -lt 26; $i++) { Px $b (Rnd 16) (Rnd 16) (C $v[2]) }
    foreach ($p in @(@(3,4),@(9,3),@(5,9),@(11,10),@(7,12))) {
        Rct $b $p[0] $p[1] 3 3 (C '8A5A2A')
        Rct $b ($p[0]+1) ($p[1]+1) 1 1 (C 'D89A4A')
    }
    Save $b "$tdir\$($v[0]).png"
}

# ---- helpers ---------------------------------------------------------------
function WriteJson($path, $text) {
    [System.IO.File]::WriteAllText($path, $text, (New-Object System.Text.UTF8Encoding($false)))
}
$BLK = 'barbarajones:block'

# ---- blockstates + block models -------------------------------------------
# pillar-shaped: log, wood, and their stripped forms
foreach ($p in @(
    @('krave_log','krave_log','krave_log_top'),
    @('stripped_krave_log','stripped_krave_log','stripped_krave_log_top'),
    @('krave_wood','krave_log','krave_log'),
    @('stripped_krave_wood','stripped_krave_log','stripped_krave_log'))) {
    $id = $p[0]
    WriteJson "$bmod\$id.json" "{`n  `"parent`": `"minecraft:block/cube_column`",`n  `"textures`": { `"side`": `"$BLK/$($p[1])`", `"end`": `"$BLK/$($p[2])`" }`n}"
    WriteJson "$bmod\${id}_horizontal.json" "{`n  `"parent`": `"minecraft:block/cube_column_horizontal`",`n  `"textures`": { `"side`": `"$BLK/$($p[1])`", `"end`": `"$BLK/$($p[2])`" }`n}"
    WriteJson "$bs\$id.json" "{`n  `"variants`": {`n    `"axis=y`": { `"model`": `"$BLK/$id`" },`n    `"axis=z`": { `"model`": `"$BLK/${id}_horizontal`", `"x`": 90 },`n    `"axis=x`": { `"model`": `"$BLK/${id}_horizontal`", `"x`": 90, `"y`": 90 }`n  }`n}"
    WriteJson "$imod\$id.json" "{ `"parent`": `"$BLK/$id`" }"
}

# simple full cubes
foreach ($p in @(
    @('krave_leaves','krave_leaves'),
    @('krave_planks','krave_planks'),
    @('krave_ore','krave_ore'),
    @('deepslate_krave_ore','deepslate_krave_ore'))) {
    $id = $p[0]
    WriteJson "$bmod\$id.json" "{`n  `"parent`": `"minecraft:block/cube_all`",`n  `"textures`": { `"all`": `"$BLK/$($p[1])`" }`n}"
    WriteJson "$bs\$id.json" "{`n  `"variants`": { `"`": { `"model`": `"$BLK/$id`" } }`n}"
    WriteJson "$imod\$id.json" "{ `"parent`": `"$BLK/$id`" }"
}

# sapling - a cross, and a flat inventory sprite
WriteJson "$bmod\krave_sapling.json" "{`n  `"parent`": `"minecraft:block/cross`",`n  `"textures`": { `"cross`": `"$BLK/krave_sapling`" }`n}"
WriteJson "$bs\krave_sapling.json" "{`n  `"variants`": { `"`": { `"model`": `"$BLK/krave_sapling`" } }`n}"
WriteJson "$imod\krave_sapling.json" "{`n  `"parent`": `"minecraft:item/generated`",`n  `"textures`": { `"layer0`": `"$BLK/krave_sapling`" }`n}"

# stairs
WriteJson "$bmod\krave_stairs.json"       "{`n  `"parent`": `"minecraft:block/stairs`",`n  `"textures`": { `"bottom`": `"$BLK/krave_planks`", `"top`": `"$BLK/krave_planks`", `"side`": `"$BLK/krave_planks`" }`n}"
WriteJson "$bmod\krave_stairs_inner.json" "{`n  `"parent`": `"minecraft:block/inner_stairs`",`n  `"textures`": { `"bottom`": `"$BLK/krave_planks`", `"top`": `"$BLK/krave_planks`", `"side`": `"$BLK/krave_planks`" }`n}"
WriteJson "$bmod\krave_stairs_outer.json" "{`n  `"parent`": `"minecraft:block/outer_stairs`",`n  `"textures`": { `"bottom`": `"$BLK/krave_planks`", `"top`": `"$BLK/krave_planks`", `"side`": `"$BLK/krave_planks`" }`n}"
WriteJson "$imod\krave_stairs.json" "{ `"parent`": `"$BLK/krave_stairs`" }"

$stairVars = New-Object System.Collections.Generic.List[string]
$yawOf = @{ north = 180; south = 0; west = 90; east = 270 }
foreach ($facing in @('north','south','west','east')) {
    foreach ($half in @('bottom','top')) {
        foreach ($shape in @('straight','inner_left','inner_right','outer_left','outer_right')) {
            $model = switch -Wildcard ($shape) {
                'inner*' { "$BLK/krave_stairs_inner" }
                'outer*' { "$BLK/krave_stairs_outer" }
                default  { "$BLK/krave_stairs" }
            }
            $y = $yawOf[$facing]
            if ($shape -like '*_left') { $y = ($y + 270) % 360 }
            $parts = @("`"model`": `"$model`"")
            if ($half -eq 'top') { $parts += "`"x`": 180" }
            if ($y -ne 0) { $parts += "`"y`": $y" }
            if ($half -eq 'top' -or $y -ne 0) { $parts += "`"uvlock`": true" }
            $stairVars.Add("    `"facing=$facing,half=$half,shape=$shape`": { $($parts -join ', ') }")
        }
    }
}
WriteJson "$bs\krave_stairs.json" "{`n  `"variants`": {`n$($stairVars -join ",`n")`n  }`n}"

# slab
WriteJson "$bmod\krave_slab.json"     "{`n  `"parent`": `"minecraft:block/slab`",`n  `"textures`": { `"bottom`": `"$BLK/krave_planks`", `"top`": `"$BLK/krave_planks`", `"side`": `"$BLK/krave_planks`" }`n}"
WriteJson "$bmod\krave_slab_top.json" "{`n  `"parent`": `"minecraft:block/slab_top`",`n  `"textures`": { `"bottom`": `"$BLK/krave_planks`", `"top`": `"$BLK/krave_planks`", `"side`": `"$BLK/krave_planks`" }`n}"
WriteJson "$bs\krave_slab.json" "{`n  `"variants`": {`n    `"type=bottom`": { `"model`": `"$BLK/krave_slab`" },`n    `"type=top`": { `"model`": `"$BLK/krave_slab_top`" },`n    `"type=double`": { `"model`": `"$BLK/krave_planks`" }`n  }`n}"
WriteJson "$imod\krave_slab.json" "{ `"parent`": `"$BLK/krave_slab`" }"

# fence
foreach ($n in @(@('krave_fence_post','fence_post'), @('krave_fence_side','fence_side'), @('krave_fence_inventory','fence_inventory'))) {
    WriteJson "$bmod\$($n[0]).json" "{`n  `"parent`": `"minecraft:block/$($n[1])`",`n  `"textures`": { `"texture`": `"$BLK/krave_planks`" }`n}"
}
$fenceSides = @(
    "    `"north`": { `"true`": { `"model`": `"$BLK/krave_fence_side`", `"uvlock`": true } }",
    "    `"east`":  { `"true`": { `"model`": `"$BLK/krave_fence_side`", `"y`": 90, `"uvlock`": true } }",
    "    `"south`": { `"true`": { `"model`": `"$BLK/krave_fence_side`", `"y`": 180, `"uvlock`": true } }",
    "    `"west`":  { `"true`": { `"model`": `"$BLK/krave_fence_side`", `"y`": 270, `"uvlock`": true } }")
WriteJson "$bs\krave_fence.json" "{`n  `"multipart`": [`n    { `"apply`": { `"model`": `"$BLK/krave_fence_post`" } },`n    { `"when`": { `"north`": `"true`" }, `"apply`": { `"model`": `"$BLK/krave_fence_side`", `"uvlock`": true } },`n    { `"when`": { `"east`": `"true`" }, `"apply`": { `"model`": `"$BLK/krave_fence_side`", `"y`": 90, `"uvlock`": true } },`n    { `"when`": { `"south`": `"true`" }, `"apply`": { `"model`": `"$BLK/krave_fence_side`", `"y`": 180, `"uvlock`": true } },`n    { `"when`": { `"west`": `"true`" }, `"apply`": { `"model`": `"$BLK/krave_fence_side`", `"y`": 270, `"uvlock`": true } }`n  ]`n}"
WriteJson "$imod\krave_fence.json" "{ `"parent`": `"$BLK/krave_fence_inventory`" }"

# fence gate
foreach ($n in @(@('krave_fence_gate','template_fence_gate'), @('krave_fence_gate_open','template_fence_gate_open'),
                 @('krave_fence_gate_wall','template_fence_gate_wall'), @('krave_fence_gate_wall_open','template_fence_gate_wall_open'))) {
    WriteJson "$bmod\$($n[0]).json" "{`n  `"parent`": `"minecraft:block/$($n[1])`",`n  `"textures`": { `"texture`": `"$BLK/krave_planks`" }`n}"
}
$gateVars = New-Object System.Collections.Generic.List[string]
$gateYaw = @{ north = 180; south = 0; west = 90; east = 270 }
foreach ($facing in @('north','south','west','east')) {
    foreach ($inWall in @('false','true')) {
        foreach ($open in @('false','true')) {
            $m = "$BLK/krave_fence_gate"
            if ($inWall -eq 'true') { $m += '_wall' }
            if ($open -eq 'true') { $m += '_open' }
            $y = $gateYaw[$facing]
            $parts = @("`"model`": `"$m`"", "`"uvlock`": true")
            if ($y -ne 0) { $parts = @("`"model`": `"$m`"", "`"y`": $y", "`"uvlock`": true") }
            $gateVars.Add("    `"facing=$facing,in_wall=$inWall,open=$open`": { $($parts -join ', ') }")
        }
    }
}
WriteJson "$bs\krave_fence_gate.json" "{`n  `"variants`": {`n$($gateVars -join ",`n")`n  }`n}"
WriteJson "$imod\krave_fence_gate.json" "{ `"parent`": `"$BLK/krave_fence_gate`" }"

# door - registered as krave_door_block to avoid clashing with the portal item
foreach ($n in @('top','top_hinge','bottom','bottom_hinge')) {
    WriteJson "$bmod\krave_door_block_$n.json" "{`n  `"parent`": `"minecraft:block/door_$n`",`n  `"textures`": { `"top`": `"$BLK/krave_door_top`", `"bottom`": `"$BLK/krave_door_bottom`" }`n}"
}
$doorVars = New-Object System.Collections.Generic.List[string]
$doorYaw = @{ north = 180; south = 0; west = 90; east = 270 }
foreach ($facing in @('north','south','west','east')) {
    foreach ($half in @('lower','upper')) {
        foreach ($hinge in @('left','right')) {
            foreach ($open in @('false','true')) {
                $m = "$BLK/krave_door_block_" + $(if ($half -eq 'upper') { 'top' } else { 'bottom' })
                $flip = ($hinge -eq 'right')
                if ($open -eq 'true') { $flip = -not $flip }
                if ($flip) { $m += '_hinge' }
                $y = $doorYaw[$facing]
                if ($open -eq 'true') { $y = ($y + $(if ($hinge -eq 'right') { 270 } else { 90 })) % 360 }
                $parts = @("`"model`": `"$m`"")
                if ($y -ne 0) { $parts += "`"y`": $y" }
                $doorVars.Add("    `"facing=$facing,half=$half,hinge=$hinge,open=$open`": { $($parts -join ', ') }")
            }
        }
    }
}
WriteJson "$bs\krave_door_block.json" "{`n  `"variants`": {`n$($doorVars -join ",`n")`n  }`n}"
WriteJson "$imod\krave_door_block.json" "{`n  `"parent`": `"minecraft:item/generated`",`n  `"textures`": { `"layer0`": `"barbarajones:item/krave_door_block`" }`n}"

# door needs its own two-part texture and an inventory sprite
$b = NewImg
Rct $b 0 0 16 16 (C '6A4A2E'); Rct $b 1 1 14 14 (C '5C3F28')
Rct $b 3 3 10 8 (C '3A1E6E'); Rct $b 5 5 6 4 (C 'B060D0')
Rct $b 12 12 2 2 (C 'D8A63A')
Save $b "$tdir\krave_door_top.png"
$b = NewImg
Rct $b 0 0 16 16 (C '6A4A2E'); Rct $b 1 1 14 14 (C '5C3F28')
Rct $b 3 4 10 9 (C '4A3220')
Rct $b 12 6 2 2 (C 'D8A63A')
Save $b "$tdir\krave_door_bottom.png"
$b = NewImg
Rct $b 0 0 16 16 (C '000000' 0)
Rct $b 4 1 8 14 (C '5C3F28'); Rct $b 5 2 6 5 (C '3A1E6E'); Rct $b 6 3 4 3 (C 'B060D0')
Rct $b 5 8 6 6 (C '4A3220'); Rct $b 10 10 1 2 (C 'D8A63A')
Save $b "$idir\krave_door_block.png"

# trapdoor
foreach ($n in @(@('krave_trapdoor_bottom','template_orientable_trapdoor_bottom'),
                 @('krave_trapdoor_top','template_orientable_trapdoor_top'),
                 @('krave_trapdoor_open','template_orientable_trapdoor_open'))) {
    WriteJson "$bmod\$($n[0]).json" "{`n  `"parent`": `"minecraft:block/$($n[1])`",`n  `"textures`": { `"texture`": `"$BLK/krave_trapdoor`" }`n}"
}
$b = NewImg
Rct $b 0 0 16 16 (C '5C3F28')
Rct $b 0 0 16 2 (C '4A3220'); Rct $b 0 14 16 2 (C '4A3220')
Rct $b 2 4 12 8 (C '6A4A2E'); Rct $b 3 5 10 6 (C '7A5636')
Rct $b 7 7 2 2 (C 'D8A63A')
Save $b "$tdir\krave_trapdoor.png"
$tdVars = New-Object System.Collections.Generic.List[string]
$tdYaw = @{ north = 0; south = 180; west = 270; east = 90 }
foreach ($facing in @('north','south','east','west')) {
    foreach ($half in @('bottom','top')) {
        foreach ($open in @('false','true')) {
            $m = if ($open -eq 'true') { "$BLK/krave_trapdoor_open" }
                 elseif ($half -eq 'top') { "$BLK/krave_trapdoor_top" }
                 else { "$BLK/krave_trapdoor_bottom" }
            $y = $tdYaw[$facing]
            $parts = @("`"model`": `"$m`"")
            if ($y -ne 0) { $parts += "`"y`": $y" }
            $parts += "`"uvlock`": true"
            $tdVars.Add("    `"facing=$facing,half=$half,open=$open`": { $($parts -join ', ') }")
        }
    }
}
WriteJson "$bs\krave_trapdoor.json" "{`n  `"variants`": {`n$($tdVars -join ",`n")`n  }`n}"
WriteJson "$imod\krave_trapdoor.json" "{ `"parent`": `"$BLK/krave_trapdoor_bottom`" }"

# button
foreach ($n in @(@('krave_button','button'), @('krave_button_pressed','button_pressed'), @('krave_button_inventory','button_inventory'))) {
    WriteJson "$bmod\$($n[0]).json" "{`n  `"parent`": `"minecraft:block/$($n[1])`",`n  `"textures`": { `"texture`": `"$BLK/krave_planks`" }`n}"
}
$btnVars = New-Object System.Collections.Generic.List[string]
$btnYaw = @{ north = 180; south = 0; west = 90; east = 270 }
foreach ($face in @('floor','wall','ceiling')) {
    foreach ($facing in @('north','south','east','west')) {
        foreach ($powered in @('false','true')) {
            $m = if ($powered -eq 'true') { "$BLK/krave_button_pressed" } else { "$BLK/krave_button" }
            $x = switch ($face) { 'ceiling' { 180 } 'wall' { 90 } default { 0 } }
            $y = $btnYaw[$facing]
            if ($face -eq 'ceiling') { $y = ($y + 180) % 360 }
            $parts = @("`"model`": `"$m`"")
            if ($x -ne 0) { $parts += "`"x`": $x" }
            if ($y -ne 0) { $parts += "`"y`": $y" }
            if ($face -eq 'wall') { $parts += "`"uvlock`": true" }
            $btnVars.Add("    `"face=$face,facing=$facing,powered=$powered`": { $($parts -join ', ') }")
        }
    }
}
WriteJson "$bs\krave_button.json" "{`n  `"variants`": {`n$($btnVars -join ",`n")`n  }`n}"
WriteJson "$imod\krave_button.json" "{ `"parent`": `"$BLK/krave_button_inventory`" }"

# pressure plate
WriteJson "$bmod\krave_pressure_plate.json"      "{`n  `"parent`": `"minecraft:block/pressure_plate_up`",`n  `"textures`": { `"texture`": `"$BLK/krave_planks`" }`n}"
WriteJson "$bmod\krave_pressure_plate_down.json" "{`n  `"parent`": `"minecraft:block/pressure_plate_down`",`n  `"textures`": { `"texture`": `"$BLK/krave_planks`" }`n}"
WriteJson "$bs\krave_pressure_plate.json" "{`n  `"variants`": {`n    `"powered=false`": { `"model`": `"$BLK/krave_pressure_plate`" },`n    `"powered=true`": { `"model`": `"$BLK/krave_pressure_plate_down`" }`n  }`n}"
WriteJson "$imod\krave_pressure_plate.json" "{ `"parent`": `"$BLK/krave_pressure_plate`" }"

# pod - CocoaBlock states: age 0-2 x facing
foreach ($age in 0..2) {
    WriteJson "$bmod\krave_pod_stage$age.json" "{`n  `"parent`": `"minecraft:block/cocoa_$age`",`n  `"textures`": { `"cocoa`": `"$BLK/krave_pod`" }`n}"
}
$podVars = New-Object System.Collections.Generic.List[string]
$podYaw = @{ north = 0; south = 180; west = 270; east = 90 }
foreach ($age in 0..2) {
    foreach ($facing in @('north','south','east','west')) {
        $y = $podYaw[$facing]
        $parts = @("`"model`": `"$BLK/krave_pod_stage$age`"")
        if ($y -ne 0) { $parts += "`"y`": $y" }
        $podVars.Add("    `"age=$age,facing=$facing`": { $($parts -join ', ') }")
    }
}
WriteJson "$bs\krave_pod.json" "{`n  `"variants`": {`n$($podVars -join ",`n")`n  }`n}"
WriteJson "$imod\krave_pod.json" "{`n  `"parent`": `"minecraft:item/generated`",`n  `"textures`": { `"layer0`": `"$BLK/krave_pod`" }`n}"

# ---- verify ----------------------------------------------------------------
$ids = @('krave_log','krave_wood','stripped_krave_log','stripped_krave_wood','krave_leaves',
         'krave_sapling','krave_planks','krave_stairs','krave_slab','krave_fence',
         'krave_fence_gate','krave_door_block','krave_trapdoor','krave_button',
         'krave_pressure_plate','krave_pod','krave_ore','deepslate_krave_ore')
$missing = 0
foreach ($id in $ids) {
    foreach ($f in @("$bs\$id.json", "$imod\$id.json")) {
        if (-not (Test-Path $f)) { "  MISSING  $f"; $missing++ }
    }
}
foreach ($t in @('krave_log','krave_log_top','stripped_krave_log','stripped_krave_log_top',
                 'krave_leaves','krave_sapling','krave_planks','krave_pod','krave_ore',
                 'deepslate_krave_ore','krave_door_top','krave_door_bottom','krave_trapdoor')) {
    if (-not (Test-Path "$tdir\$t.png")) { "  MISSING  $tdir\$t.png"; $missing++ }
}
if ($missing -gt 0) { throw "make_krave_wood.ps1 did not write $missing file(s)." }
# The old check only asserted each file EXISTED. That passed happily while every
# model path inside was garbage, so it now reads the contents: every "model"
# value must be a real barbarajones:block/... path naming a file on disk.
$bad = 0
foreach ($f in Get-ChildItem "$bs\*.json") {
    foreach ($m in ([regex]'"model"\s*:\s*"([^"]*)"').Matches((Get-Content $f -Raw))) {
        $v = $m.Groups[1].Value
        if ($v -notmatch '^barbarajones:block/[a-z0-9_]+$') {
            Write-Host "  BAD path in $($f.Name): '$v'"; $bad++
        } elseif (-not (Test-Path "$bmod\$($v -replace '^barbarajones:block/','').json")) {
            Write-Host "  MISSING model for $($f.Name): '$v'"; $bad++
        }
    }
}
if ($bad) { throw "$bad broken model reference(s) in the Krave blockstates." }

"krave wood + ore: $($ids.Count) blockstates, $($ids.Count) item models, 13 textures - all present."
