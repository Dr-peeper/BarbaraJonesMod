# KRAVE CRAFTING ECONOMY (com.barbarajones.v2.economy) - textures.
# Item icons: rich_krave, krave_syrup. Block texture: krave_mortar.
# Idempotent - every image is painted from scratch, so re-running is safe.
Add-Type -AssemblyName System.Drawing
$repoRoot = Split-Path -Parent $PSScriptRoot
$res  = "$repoRoot\src\main\resources"
$idir = "$res\assets\barbarajones\textures\item"
$bdir = "$res\assets\barbarajones\textures\block"
New-Item -ItemType Directory -Force $idir,$bdir | Out-Null

# ---------------------------------------------------------------- primitives
function C([string]$h,[int]$a=255){ [System.Drawing.Color]::FromArgb($a,
    [Convert]::ToInt32($h.Substring(0,2),16),
    [Convert]::ToInt32($h.Substring(2,2),16),
    [Convert]::ToInt32($h.Substring(4,2),16)) }
function NewImg([int]$w,[int]$h){
    New-Object System.Drawing.Bitmap $w,$h,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function Icon(){ NewImg 16 16 }
function P($b,[int]$x,[int]$y,$c){
    if($x -ge 0 -and $y -ge 0 -and $x -lt $b.Width -and $y -lt $b.Height){ $b.SetPixel($x,$y,$c) } }
function Rct($b,[int]$x,[int]$y,[int]$w,[int]$h,$c){
    for($i=0;$i -lt $w;$i++){ for($j=0;$j -lt $h;$j++){ P $b ($x+$i) ($y+$j) $c } } }
function Outl($b,[int]$x,[int]$y,[int]$w,[int]$h,$c){
    Rct $b $x $y $w 1 $c; Rct $b $x ($y+$h-1) $w 1 $c
    Rct $b $x $y 1 $h $c; Rct $b ($x+$w-1) $y 1 $h $c }
function SaveAt($b,$path){ $b.Save($path,[System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }

$script:written = New-Object System.Collections.Generic.List[string]
function SaveItem($b,$name){ $p = "$idir\$name.png"; SaveAt $b $p; $script:written.Add($p) }
function SaveBlock($b,$name){ $p = "$bdir\$name.png"; SaveAt $b $p; $script:written.Add($p) }

# Deterministic noise - fixed seed keeps re-runs byte-identical.
$script:sd = 20260819
function Rnd([int]$n){ $script:sd = ($script:sd*1103515245 + 12345) -band 0x7fffffff; (($script:sd -shr 15) % $n) }

# ------------------------------------------------------------------- palette
$chocDark  = C '3E2412'; $chocMid  = C '5A3320'; $chocLite = C '8A5A2A'
$bronze    = C '9C6B32'; $bronzeD  = C '5E4018'
$cream     = C 'E8D9B0'; $creamD   = C 'C8B078'
$amber     = C '6B3A16'; $amberLite= C 'A8621F'; $amberHi = C 'D9884A'
$glass     = C 'DCE8E4' 130; $glassEdge = C 'FFFFFF' 200
$cork      = C '8A6038'; $corkD    = C '4A3018'
$stone     = C '9A9A96'; $stoneLite= C 'B8B8B2'; $stoneDark= C '6E6E68'
$stain     = C '6A4426'; $stainD   = C '4A2E18'
$white     = C 'F2F2F4'

# A carton-style box face with a horizontal accent band, matching the rest of
# the Krave line's item-icon language (see make_extra_content.ps1's Carton()).
function Carton($b,$face,$band,$edge){
    Rct $b 3 1 10 14 $face
    Outl $b 3 1 10 14 $edge
    Rct $b 4 6 8 4 $band
    Rct $b 12 2 1 12 $edge
}

# ===========================================================================
# ITEM: rich_krave - a darker, bronze-trimmed carton one step up from the
# plain box, one step short of the golden one. Deliberately calmer than
# golden_krave.png (fewer sparkles, no full-gold body) so the tier reads at a
# glance even in a crowded hotbar.
# ===========================================================================
$b = Icon
Carton $b $chocMid $bronze $chocDark
Rct $b 5 7 1 2 $cream; P $b 6 8 $cream; Rct $b 7 7 1 2 $cream   # crude "R"
for($i=0;$i -lt 6;$i++){ P $b (4+(Rnd 8)) (2+(Rnd 4)) $chocLite } # box-top fleck
P $b 5 2 $white; P $b 10 12 $white                                # two sparkles, not thirty
SaveItem $b 'rich_krave'

# ===========================================================================
# ITEM: krave_syrup - a honey-bottle silhouette, dark chocolate-amber syrup
# instead of gold, so it reads as "Krave's own" rather than a Honey reskin.
# ===========================================================================
$b = Icon
# neck + cap
Rct $b 7 1 2 2 $cork
Rct $b 6 3 4 1 $corkD
# bottle glass body (outline only, transparent middle so the liquid shows)
Rct $b 5 4 6 10 $glassEdge
Rct $b 5 4 1 10 $glassEdge; Rct $b 10 4 1 10 $glassEdge
Rct $b 5 13 6 1 $glassEdge
# liquid fill
Rct $b 6 6 4 7 $amber
Rct $b 6 6 4 1 $amberLite
for($i=0;$i -lt 8;$i++){ P $b (6+(Rnd 4)) (7+(Rnd 6)) $amberLite }
P $b 7 8 $amberHi; P $b 8 9 $amberHi
SaveItem $b 'krave_syrup'

# ===========================================================================
# BLOCK: krave_mortar - a stone grinding bowl, speckled and stained brown
# from years (well, minutes) of cocoa. Same texture tiled on every face by
# the model, so it has to read fine from any angle.
# ===========================================================================
$b = NewImg 16 16
Rct $b 0 0 16 16 $stone
for($x=0;$x -lt 16;$x++){ for($y=0;$y -lt 16;$y++){
    $n = Rnd 10
    if($n -eq 0){ P $b $x $y $stoneLite }
    elseif($n -eq 1){ P $b $x $y $stoneDark }
} }
# darker carved-bowl vignette toward the centre
for($x=4;$x -lt 12;$x++){ for($y=4;$y -lt 12;$y++){
    $dx = $x-7.5; $dy = $y-7.5
    if(($dx*$dx+$dy*$dy) -le 16){ P $b $x $y $stoneDark }
} }
# cocoa-dust stains, scattered
for($i=0;$i -lt 10;$i++){ P $b (Rnd 16) (Rnd 16) $stain }
for($i=0;$i -lt 5;$i++){ P $b (Rnd 16) (Rnd 16) $stainD }
Outl $b 0 0 16 16 $stoneDark
SaveBlock $b 'krave_mortar'

# ===========================================================================
# VERIFY - read every file back and check it decodes to the expected size.
# ===========================================================================
$expected = @(
    @{ path = "$idir\rich_krave.png";  w = 16; h = 16 },
    @{ path = "$idir\krave_syrup.png"; w = 16; h = 16 },
    @{ path = "$bdir\krave_mortar.png"; w = 16; h = 16 }
)
$missing = 0
foreach($e in $expected){
    if(-not (Test-Path $e.path)){ "  MISSING  $($e.path)"; $missing++; continue }
    $img = [System.Drawing.Image]::FromFile($e.path)
    $okSize = ($img.Width -eq $e.w -and $img.Height -eq $e.h)
    $img.Dispose()
    if($okSize){ "  OK       $($e.path)  ($($e.w)x$($e.h))" }
    else { "  BAD-SIZE $($e.path)"; $missing++ }
}
""
"$($expected.Count) expected, $missing missing/bad."
if($missing -gt 0){ throw "make_economy.ps1 did not write $missing file(s) correctly." }
