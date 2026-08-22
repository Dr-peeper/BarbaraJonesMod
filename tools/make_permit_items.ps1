# Textures and models for Barbara's ten permits and kits.
#
# They were registered without either, which in 1.20.1 is not a crash but is
# worse to diagnose: the item exists, works, stacks and can be handed over, and
# renders as the black-and-magenta missing square everywhere it appears. Every
# other item in the mod has a model, so this was the only gap.
#
# Two visual families, because they are two different objects. A PERMIT is
# paperwork - a grubby stamped document, because the joke is that this shambles
# of a settlement has a bureaucracy. A KIT is a crate of materials. You should
# be able to tell which is which in a hotbar without reading the tooltip.

Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'

$repo = Split-Path $PSScriptRoot -Parent
$tex  = Join-Path $repo 'src\main\resources\assets\barbarajones\textures\item'
$mdl  = Join-Path $repo 'src\main\resources\assets\barbarajones\models\item'
foreach ($d in @($tex, $mdl)) { if (-not (Test-Path $d)) { throw "missing folder: $d" } }

function C([string]$hex, [int]$a = 255) {
    [System.Drawing.Color]::FromArgb($a,
        [Convert]::ToInt32($hex.Substring(0,2),16),
        [Convert]::ToInt32($hex.Substring(2,2),16),
        [Convert]::ToInt32($hex.Substring(4,2),16))
}
function Px($b,$x,$y,$col){ if ($x -ge 0 -and $x -lt 16 -and $y -ge 0 -and $y -lt 16) { $b.SetPixel($x,$y,$col) } }
function Box($b,$x,$y,$w,$h,$col){ for($i=0;$i -lt $w;$i++){ for($j=0;$j -lt $h;$j++){ Px $b ($x+$i) ($y+$j) $col } } }

# Deterministic speckle. A random scatter regenerates differently every run,
# which makes the texture un-diffable and hides real changes in the churn.
$script:sd = 29
function Rnd([int]$n){ $script:sd=($script:sd*1103515245+12345) -band 0x7fffffff; return $script:sd % $n }

function New-Permit {
    param([string]$name, $stamp)
    $script:sd = 29
    $b = New-Object System.Drawing.Bitmap 16,16,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    Box $b 0 0 16 16 (C '000000' 0)

    $paper  = C 'E8DFC4'
    $shade  = C 'CFC3A0'
    $ink    = C '3A3128'
    # The sheet, deliberately not square on the canvas - a permit that is
    # perfectly aligned looks official, and nothing here is.
    Box $b 3 1 11 14 $paper
    Box $b 3 1 11 1 $shade
    Box $b 3 14 11 1 $shade
    # A dog-eared corner.
    Px $b 13 1 (C '000000' 0); Px $b 12 1 $shade; Px $b 13 2 $shade

    # Lines of "text".
    foreach ($row in 4,6,8,10) {
        $len = 5 + (Rnd 4)
        Box $b 5 $row $len 1 $ink
    }
    # The stamp: a coloured smudge over the text, off-centre and overlapping,
    # because it was banged on by someone who was not looking.
    Box $b 8 9 5 4 $stamp
    Px $b 7 10 $stamp; Px $b 13 11 $stamp
    Box $b 9 10 3 2 (C 'FFFFFF' 70)
    return $b
}

function New-Kit {
    param([string]$name, $accent)
    $script:sd = 41
    $b = New-Object System.Drawing.Bitmap 16,16,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    Box $b 0 0 16 16 (C '000000' 0)

    $wood  = C '8A6034'
    $dark  = C '5E3F20'
    $light = C 'A87A46'
    # A crate, with the slats uneven on purpose.
    Box $b 1 3 14 12 $wood
    Box $b 1 3 14 1 $light
    Box $b 1 14 14 1 $dark
    Box $b 1 3 1 12 $dark
    Box $b 14 3 1 12 $dark
    foreach ($row in 6,9,12) { Box $b 2 $row 12 1 $dark }
    # Speckle: knots and wear, so it does not read as a flat brown rectangle.
    for ($i = 0; $i -lt 14; $i++) { Px $b (2 + (Rnd 12)) (4 + (Rnd 10)) $dark }
    # A band of whatever this kit is for, slapped across the front.
    Box $b 2 7 12 2 $accent
    Box $b 2 7 12 1 (C 'FFFFFF' 60)
    return $b
}

# id -> builder, colour. Permits get a stamp colour, kits get a band colour.
$items = @(
    @{ id = 'permit_house';             kind = 'permit'; col = C 'C4402E' },
    @{ id = 'permit_road';              kind = 'permit'; col = C '6E6A60' },
    @{ id = 'permit_market_stall';      kind = 'permit'; col = C 'D8A02A' },
    @{ id = 'permit_corner_store';      kind = 'permit'; col = C '3E7A4A' },
    @{ id = 'permit_tenement';          kind = 'permit'; col = C '7A4A9E' },
    @{ id = 'permit_plug_headquarters'; kind = 'permit'; col = C '2E6EC4' },
    @{ id = 'permit_krave_spire';       kind = 'permit'; col = C 'F2B32B' },
    @{ id = 'kit_krave_shack';          kind = 'kit';    col = C 'C4402E' },
    @{ id = 'kit_trap_house';           kind = 'kit';    col = C '2B1436' },
    @{ id = 'kit_workshop';             kind = 'kit';    col = C '4A6E8A' }
)

$model = @'
{
  "parent": "minecraft:item/generated",
  "textures": { "layer0": "barbarajones:item/ID" }
}
'@

foreach ($it in $items) {
    $bmp = if ($it.kind -eq 'permit') { New-Permit $it.id $it.col } else { New-Kit $it.id $it.col }
    $png = Join-Path $tex "$($it.id).png"
    $bmp.Save($png, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()

    # Written without a BOM. Minecraft's Gson skips one, but the mod already has
    # 187 BOM'd resource files and adding more is not an improvement.
    $json = $model.Replace('ID', $it.id)
    [System.IO.File]::WriteAllText((Join-Path $mdl "$($it.id).json"), $json,
        (New-Object System.Text.UTF8Encoding($false)))

    # Verify what landed. A texture at the wrong size renders as garbage rather
    # than as an error, which is far easier to miss than a crash.
    $chk = New-Object System.Drawing.Bitmap $png
    $ok = ($chk.Width -eq 16 -and $chk.Height -eq 16)
    $chk.Dispose()
    if (-not $ok) { throw "$($it.id).png written at the wrong size" }
    Write-Host "  OK  $($it.id)  (16x16 + model)"
}

Write-Host ""
Write-Host "$($items.Count) permit/kit textures and models written."
