# Icon for the rebuilt quest system's one registered item: barbarajones:quest_atlas.
#
# Krave is a chocolate cereal, so the palette is cocoa browns with a warm caramel
# highlight and cereal-crumb speckle - the same family as the rest of the mod's items.
# The drawing is a closed ledger seen at a slight angle: dark cocoa cover, a caramel
# spine down the left, cream page edges on the right, and a small three-node quest
# tree branded into the front so it reads as "the quest book" and not "a book".
#
# Writes the texture and the matching item model, then reads BOTH back off disk and
# checks the pixels it claims to have drawn are actually there.

Add-Type -AssemblyName System.Drawing

$repoRoot = Split-Path -Parent $PSScriptRoot
$idir = "$repoRoot\src\main\resources\assets\barbarajones\textures\item"
$mdir = "$repoRoot\src\main\resources\assets\barbarajones\models\item"
foreach ($d in @($idir, $mdir)) {
    if (-not (Test-Path $d)) { New-Item -ItemType Directory -Force $d | Out-Null }
}

function C([string]$h, [int]$a = 255) {
    [System.Drawing.Color]::FromArgb($a,
        [Convert]::ToInt32($h.Substring(0, 2), 16),
        [Convert]::ToInt32($h.Substring(2, 2), 16),
        [Convert]::ToInt32($h.Substring(4, 2), 16))
}

function Rct($b, $x, $y, $w, $h, $c) {
    for ($i = 0; $i -lt $w; $i++) {
        for ($j = 0; $j -lt $h; $j++) {
            $px = $x + $i; $py = $y + $j
            if ($px -ge 0 -and $py -ge 0 -and $px -lt $b.Width -and $py -lt $b.Height) {
                $b.SetPixel($px, $py, $c)
            }
        }
    }
}

# ---- the drawing --------------------------------------------------------------

$COCOA_DARK  = C '2B1608'   # cover shadow / outline
$COCOA       = C '4A2A12'   # cover
$COCOA_LIGHT = C '653A1A'   # cover top bevel
$CARAMEL     = C 'A9682A'   # spine
$CARAMEL_HI  = C 'D4913F'   # spine highlight
$CREAM       = C 'E8DCC0'   # page edges
$CREAM_DIM   = C 'C4B492'   # page shadow
$GOLD        = C 'F0C24A'   # clasp / brand
$GOLD_DIM    = C 'A8842A'
$CRUMB       = C '7A4A22'   # cereal speckle

$bmp = New-Object System.Drawing.Bitmap 16, 16
for ($x = 0; $x -lt 16; $x++) { for ($y = 0; $y -lt 16; $y++) { $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb(0, 0, 0, 0)) } }

# outline / body
Rct $bmp 2 1 12 14 $COCOA_DARK
Rct $bmp 3 2 10 12 $COCOA
Rct $bmp 3 2 10 1  $COCOA_LIGHT      # top bevel catches the light

# spine down the left
Rct $bmp 3 2 2 12 $CARAMEL
Rct $bmp 3 2 1 12 $CARAMEL_HI

# page edges on the right, slightly proud of the cover
Rct $bmp 12 3 2 10 $CREAM
Rct $bmp 13 4 1 8  $CREAM_DIM
Rct $bmp 12 3 1 1  $CREAM_DIM
Rct $bmp 12 12 1 1 $CREAM_DIM

# cereal-crumb speckle across the cover so it is not a flat brown slab
Rct $bmp 6 4 1 1 $CRUMB
Rct $bmp 9 6 1 1 $CRUMB
Rct $bmp 7 10 1 1 $CRUMB
Rct $bmp 10 11 1 1 $CRUMB
Rct $bmp 6 12 1 1 $CRUMB

# the brand: a three-node quest tree, one root branching to two children
Rct $bmp 8 4 2 2 $GOLD          # root node
Rct $bmp 8 6 1 2 $GOLD_DIM      # trunk
Rct $bmp 6 8 1 1 $GOLD_DIM      # left branch
Rct $bmp 7 8 2 1 $GOLD_DIM      # crossbar
Rct $bmp 9 8 2 1 $GOLD_DIM
Rct $bmp 6 9 2 2 $GOLD          # left child
Rct $bmp 10 9 2 2 $GOLD         # right child

# clasp on the page side
Rct $bmp 11 7 2 2 $GOLD
Rct $bmp 11 8 2 1 $GOLD_DIM

$texPath = "$idir\quest_atlas.png"
$bmp.Save($texPath, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Dispose()

# ---- model --------------------------------------------------------------------

$model = '{' + "`n" +
         '  "parent": "minecraft:item/generated",' + "`n" +
         '  "textures": { "layer0": "barbarajones:item/quest_atlas" }' + "`n" +
         '}' + "`n"
$modelPath = "$mdir\quest_atlas.json"
Set-Content -Path $modelPath -Value $model -Encoding utf8

# ---- verify by reading back ---------------------------------------------------

$fail = 0

if (-not (Test-Path $texPath)) {
    Write-Host "FAIL texture missing: $texPath"; $fail++
} else {
    $check = New-Object System.Drawing.Bitmap $texPath
    Write-Host ("texture {0}  {1}x{2}  {3} bytes" -f $texPath, $check.Width, $check.Height, (Get-Item $texPath).Length)
    if ($check.Width -ne 16 -or $check.Height -ne 16) { Write-Host "FAIL not 16x16"; $fail++ }

    # Sample the pixels this script claims to have drawn, by colour not by "file exists".
    $expect = @(
        @{ x = 0;  y = 0;  hex = '00000000'; what = 'transparent corner' },
        @{ x = 3;  y = 7;  hex = 'FFD4913F'; what = 'caramel spine highlight' },
        @{ x = 6;  y = 6;  hex = 'FF4A2A12'; what = 'cocoa cover' },
        @{ x = 12; y = 6;  hex = 'FFE8DCC0'; what = 'cream page edge' },
        @{ x = 8;  y = 4;  hex = 'FFF0C24A'; what = 'gold root node' },
        @{ x = 6;  y = 9;  hex = 'FFF0C24A'; what = 'gold left child' },
        @{ x = 10; y = 9;  hex = 'FFF0C24A'; what = 'gold right child' },
        @{ x = 9;  y = 6;  hex = 'FF7A4A22'; what = 'cereal crumb' }
    )
    foreach ($e in $expect) {
        $p = $check.GetPixel($e.x, $e.y)
        $got = '{0:X2}{1:X2}{2:X2}{3:X2}' -f $p.A, $p.R, $p.G, $p.B
        if ($got -ne $e.hex) {
            Write-Host ("FAIL ({0},{1}) {2}: expected {3}, got {4}" -f $e.x, $e.y, $e.what, $e.hex, $got)
            $fail++
        } else {
            Write-Host ("  ok ({0},{1}) {2} = {3}" -f $e.x, $e.y, $e.what, $got)
        }
    }

    # A texture that is mostly transparent would read as a missing icon in the hotbar.
    $opaque = 0
    for ($x = 0; $x -lt 16; $x++) {
        for ($y = 0; $y -lt 16; $y++) {
            if ($check.GetPixel($x, $y).A -gt 0) { $opaque++ }
        }
    }
    Write-Host ("  {0}/256 pixels opaque" -f $opaque)
    if ($opaque -lt 120) { Write-Host "FAIL icon is too sparse to read at 16x16"; $fail++ }
    $check.Dispose()
}

if (-not (Test-Path $modelPath)) {
    Write-Host "FAIL model missing: $modelPath"; $fail++
} else {
    $text = Get-Content -Raw $modelPath
    try {
        $obj = $text | ConvertFrom-Json
        if ($obj.textures.layer0 -ne 'barbarajones:item/quest_atlas') {
            Write-Host "FAIL model layer0 is '$($obj.textures.layer0)'"; $fail++
        } else {
            Write-Host "model  $modelPath  layer0 = $($obj.textures.layer0)"
        }
    } catch {
        Write-Host "FAIL model is not valid JSON: $_"; $fail++
    }
}

Write-Host ""
if ($fail -eq 0) {
    Write-Host "make_questsv2: 2 files written and verified."
} else {
    Write-Host "make_questsv2: $fail problem(s)."
    exit 1
}
