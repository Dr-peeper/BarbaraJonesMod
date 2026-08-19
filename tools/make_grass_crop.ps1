# Four growth stages for Barbara's grass crop, plus the blockstate, models and
# loot table. Sparse blades early, a thick tuft with seed heads at maturity.
Add-Type -AssemblyName System.Drawing
$repoRoot = Split-Path -Parent $PSScriptRoot
$tdir = "$repoRoot\src\main\resources\assets\barbarajones\textures\block"
$mdir = "$repoRoot\src\main\resources\assets\barbarajones\models\block"
$bdir = "$repoRoot\src\main\resources\assets\barbarajones\blockstates"
$ldir = "$repoRoot\src\main\resources\data\barbarajones\loot_tables\blocks"
foreach($d in @($tdir,$mdir,$bdir,$ldir)){ if(-not (Test-Path $d)){ New-Item -ItemType Directory -Force $d | Out-Null } }

function C([string]$h,[int]$a=255){ [System.Drawing.Color]::FromArgb($a,
    [Convert]::ToInt32($h.Substring(0,2),16),[Convert]::ToInt32($h.Substring(2,2),16),[Convert]::ToInt32($h.Substring(4,2),16)) }
function Px($b,$x,$y,$c){ if($x -ge 0 -and $y -ge 0 -and $x -lt 16 -and $y -lt 16){ $b.SetPixel($x,$y,$c) } }

$script:seed = 1337
function Rnd([int]$n){ $script:seed = ($script:seed*1103515245 + 12345) -band 0x7fffffff; return $script:seed % $n }

$greens = @((C '4F7A32'), (C '5E8F3B'), (C '6FA347'), (C '43682A'))
$dry    = C '8A9A46'

for($stage=0; $stage -lt 4; $stage++){
    $b = New-Object System.Drawing.Bitmap 16,16,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    for($x=0;$x -lt 16;$x++){ for($y=0;$y -lt 16;$y++){ Px $b $x $y (C '000000' 0) } }

    $blades = 4 + $stage * 5
    $height = 4 + $stage * 3
    $script:seed = 1337 + $stage * 97

    for($i=0;$i -lt $blades;$i++){
        $bx = 1 + (Rnd 14)
        $h  = [Math]::Max(2, $height - (Rnd 3))
        $col = $greens[(Rnd 4)]
        $lean = (Rnd 3) - 1
        for($j=0;$j -lt $h;$j++){
            $y = 15 - $j
            $x = $bx + [int]([Math]::Floor($j * $lean / 3.0))
            Px $b $x $y $col
            if($j -eq ($h-1) -and $stage -ge 2){ Px $b $x ($y-1) $dry }   # seed head
        }
    }
    $b.Save("$tdir\grass_crop_stage$stage.png",[System.Drawing.Imaging.ImageFormat]::Png)
    $b.Dispose()

    $model = '{' + "`n" + '  "parent": "minecraft:block/crop",' + "`n" +
             '  "textures": { "crop": "barbarajones:block/grass_crop_stage' + $stage + '" }' + "`n" + '}'
    [System.IO.File]::WriteAllText("$mdir\grass_crop_stage$stage.json", $model, (New-Object System.Text.UTF8Encoding($false)))
}

# blockstate: AGE_3 maps 0..3 onto the four models
$bs = @'
{
  "variants": {
    "age=0": { "model": "barbarajones:block/grass_crop_stage0" },
    "age=1": { "model": "barbarajones:block/grass_crop_stage1" },
    "age=2": { "model": "barbarajones:block/grass_crop_stage2" },
    "age=3": { "model": "barbarajones:block/grass_crop_stage3" }
  }
}
'@
[System.IO.File]::WriteAllText("$bdir\grass_crop.json", $bs, (New-Object System.Text.UTF8Encoding($false)))

# loot: seeds always, plus a handful of grass only when fully grown
$loot = @'
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [ { "type": "minecraft:item", "name": "barbarajones:grass_seeds" } ]
    },
    {
      "rolls": 1,
      "conditions": [
        {
          "condition": "minecraft:block_state_property",
          "block": "barbarajones:grass_crop",
          "properties": { "age": "3" }
        }
      ],
      "entries": [
        {
          "type": "minecraft:item",
          "name": "barbarajones:handful_of_grass",
          "functions": [
            { "function": "minecraft:set_count", "count": { "min": 1.0, "max": 2.0 } }
          ]
        },
        { "type": "minecraft:item", "name": "barbarajones:grass_seeds" }
      ]
    }
  ]
}
'@
[System.IO.File]::WriteAllText("$ldir\grass_crop.json", $loot, (New-Object System.Text.UTF8Encoding($false)))

$expected = @()
0..3 | ForEach-Object { $expected += "$tdir\grass_crop_stage$_.png"; $expected += "$mdir\grass_crop_stage$_.json" }
$expected += "$bdir\grass_crop.json"; $expected += "$ldir\grass_crop.json"
$missing = 0
foreach($p in $expected){ if(Test-Path $p){ "  OK  $(Split-Path -Leaf $p)" } else { "  MISSING  $p"; $missing++ } }
if($missing -gt 0){ throw "make_grass_crop.ps1 did not write $missing file(s)." }
"grass crop: $($expected.Count) files written."
