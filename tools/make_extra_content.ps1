# Textures for the extra-content pack: 51 item icons, 17 block textures, the
# animated TV screen (+ its .mcmeta) and the recliner seat's stub entity image.
# Idempotent - every image is painted from scratch, so re-running is safe.
Add-Type -AssemblyName System.Drawing
$repoRoot = Split-Path -Parent $PSScriptRoot
$res  = "$repoRoot\src\main\resources"
$idir = "$res\assets\barbarajones\textures\item"
$bdir = "$res\assets\barbarajones\textures\block"
$edir = "$res\assets\barbarajones\textures\entity"
New-Item -ItemType Directory -Force $idir,$bdir,$edir | Out-Null

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
function Disc($b,[int]$cx,[int]$cy,[int]$r,$c){
    for($x=-$r;$x -le $r;$x++){ for($y=-$r;$y -le $r;$y++){
        if(($x*$x + $y*$y) -le ($r*$r)){ P $b ($cx+$x) ($cy+$y) $c } } } }
function SaveAt($b,$path){ $b.Save($path,[System.Drawing.Imaging.ImageFormat]::Png); $b.Dispose() }

$script:written = New-Object System.Collections.Generic.List[string]
function SaveItem($b,$name){ $p = "$idir\$name.png"; SaveAt $b $p; $script:written.Add($p) }
function SaveBlock($b,$name){ $p = "$bdir\$name.png"; SaveAt $b $p; $script:written.Add($p) }

# Deterministic noise - a fixed seed keeps re-runs byte-identical so the build
# does not churn every texture on every invocation.
$script:sd = 20250818
# The low bits of a linear congruential generator are strongly correlated, and
# feeding them straight into a per-pixel modulo paints visible diagonal banding
# across a texture. Take the high bits instead.
function Rnd([int]$n){ $script:sd = ($script:sd*1103515245 + 12345) -band 0x7fffffff; (($script:sd -shr 15) % $n) }

# ------------------------------------------------------------------- palette
$ink    = C '191114'; $paper  = C 'EDE6D2'; $steel = C 'B4B8BE'; $steelD = C '70757C'
$gold   = C 'F2C21A'; $goldD  = C 'A8830C'; $red   = C 'C0392B'; $redD   = C '7E2119'
$choc   = C '5A3A22'; $chocL  = C '7A5232'; $cream = C 'F0E0C0'; $bun    = C 'D9A45B'
$green  = C '3E8E41'; $grass  = C '5FA83A'; $purple= C '5A3A9A'; $purpleL= C 'B060D0'
$black  = C '14141A'; $grey   = C '4A4A52'; $white = C 'F2F2F4'; $orange = C 'E08A20'
$wood   = C '6A4A28'; $woodL  = C '8A6A42'; $pink  = C 'E4A0A8'

# ------------------------------------------------------------ shape helpers
# A soda can, seen straight on: rim, body, label band, shading down one side.
function Can($b,$body,$band,$trim){
    Rct $b 5 2 6 12 $body
    Rct $b 5 2 6 1 $steel
    Rct $b 5 13 6 1 $steelD
    Rct $b 10 3 1 10 $trim
    Rct $b 5 6 6 3 $band
    P $b 7 1 $steel; P $b 8 1 $steel
}
# A bottle: narrow neck, cap, fat body with a wrapped label.
function Bottle($b,$liquid,$cap,$label){
    Rct $b 6 1 4 1 $cap
    Rct $b 6 2 4 3 $liquid
    Rct $b 4 5 8 10 $liquid
    Rct $b 4 8 8 4 $label
    Rct $b 4 5 1 10 (C '00000030')
    Rct $b 11 5 1 10 (C '00000050')
}
# A cereal-style carton with a colour band across the middle.
function Carton($b,$face,$band,$edge){
    Rct $b 3 1 10 14 $face
    Outl $b 3 1 10 14 $edge
    Rct $b 4 6 8 4 $band
    Rct $b 12 2 1 12 $edge
}
# A sheet of paper with ruled lines.
function Sheet($b,$stock,$rule,[int]$x,[int]$y,[int]$w,[int]$h){
    Rct $b $x $y $w $h $stock
    Outl $b $x $y $w $h (C 'B9B0A0')
    for($j=$y+2;$j -lt ($y+$h-1);$j+=2){ Rct $b ($x+2) $j ($w-4) 1 $rule }
}
# A trophy cup on a plinth.
function Cup($b,$metal,$metalD,$accent){
    Rct $b 4 2 8 5 $metal
    Rct $b 5 7 6 2 $metal
    Rct $b 7 9 2 2 $metalD
    Rct $b 4 11 8 3 $accent
    Rct $b 3 3 1 3 $metal; Rct $b 12 3 1 3 $metal
    Rct $b 4 2 8 1 $metalD
}
# The diagonal wooden handle every lawn tool hangs off.
function Handle($b,$lo,$hi){
    for($i=0;$i -lt 9;$i++){ P $b (2+$i) (14-$i) $lo; P $b (3+$i) (14-$i) $hi } }

# ===========================================================================
# ITEM ICONS
# ===========================================================================

# --- the Krave line ---------------------------------------------------------
$b = Icon; Carton $b $gold $choc $goldD
Rct $b 5 7 1 2 $cream; P $b 6 8 $cream; Rct $b 7 7 1 2 $cream   # crude "K"
P $b 4 3 $white; P $b 11 4 $white; P $b 6 12 $white              # sparkle
SaveItem $b 'golden_krave'

$b = Icon; Carton $b (C '8A6038') (C '4A2C18') (C '3A2010')
Rct $b 5 7 1 2 (C 'C8B896'); P $b 6 8 (C 'C8B896'); Rct $b 7 7 1 2 (C 'C8B896')
for($i=0;$i -lt 10;$i++){ P $b (3+(Rnd 10)) (1+(Rnd 14)) (C '6A4A30') }
SaveItem $b 'stale_krave'

$b = Icon; Carton $b (C 'C0A040') (C '6A5A30') (C '4A3E20')
Rct $b 6 7 1 2 (C 'E8E0C0'); P $b 7 8 (C 'E8E0C0')               # mis-set letter
SaveItem $b 'off_brand_krave'

$b = Icon; Rct $b 2 1 12 14 (C '6A4020'); Outl $b 2 1 12 14 (C '3A2010')
Rct $b 3 5 10 5 (C '4A2C18'); Rct $b 3 11 10 1 (C '8A6038')
Rct $b 6 6 1 3 $cream; P $b 7 7 $cream; Rct $b 8 6 1 3 $cream
SaveItem $b 'krave_family_box'

$b = Icon; Rct $b 3 11 10 3 (C '4A2C18'); Rct $b 4 9 8 2 (C '5A3A22')
Rct $b 6 8 4 1 (C '6A4830')
for($i=0;$i -lt 14;$i++){ P $b (3+(Rnd 10)) (9+(Rnd 5)) (C '7A5A3A') }
SaveItem $b 'krave_dust'

# --- smoking ----------------------------------------------------------------
$b = Icon
for($i=0;$i -lt 10;$i++){ P $b (3+$i) (12-$i) $white; P $b (4+$i) (12-$i) (C 'D8D8DC') }
Rct $b 2 12 3 2 (C 'D9A45B')                                     # filter
P $b 12 3 (C 'FF7A18'); P $b 13 2 (C 'FFC24A')                   # ember
SaveItem $b 'cigarette'

$b = Icon; Rct $b 4 3 8 11 $white; Outl $b 4 3 8 11 (C 'B0B0B4')
Rct $b 4 3 8 4 $red; Rct $b 5 8 6 1 (C 'C8C8CC'); Rct $b 5 10 6 1 (C 'C8C8CC')
Rct $b 6 1 4 2 $white; Outl $b 6 1 4 2 (C 'B0B0B4')
SaveItem $b 'cigarette_pack'

$b = Icon
for($i=0;$i -lt 11;$i++){ P $b (2+$i) (12-$i) (C '6A4020'); P $b (3+$i) (12-$i) (C '8A5A30') }
Rct $b 5 8 3 3 (C 'C08A3A')                                      # band
P $b 13 1 (C 'FF7A18'); P $b 12 2 (C 'FFC24A')
SaveItem $b 'cigar'

$b = Icon; Rct $b 4 5 8 9 $steel; Outl $b 4 5 8 9 $steelD
Rct $b 4 5 8 2 (C 'DADEE4'); Rct $b 6 8 4 4 (C '8A8E96')
Rct $b 7 1 2 4 (C 'FF9A20'); Rct $b 7 1 2 2 (C 'FFE07A')
SaveItem $b 'daniels_zippo'

$b = Icon; Rct $b 3 5 10 8 (C 'C8B48A'); Outl $b 3 5 10 8 (C '8A7A56')
Rct $b 3 5 10 3 $red
for($i=0;$i -lt 4;$i++){ Rct $b (4+$i*2) 9 1 3 (C 'E8E0C8'); P $b (4+$i*2) 12 (C '8A2A18') }
SaveItem $b 'matchbook'

$b = Icon; Rct $b 3 11 10 3 (C '6A6A70'); Rct $b 4 9 8 2 (C '8A8A90')
Rct $b 6 8 4 1 (C 'A8A8AE')
for($i=0;$i -lt 12;$i++){ P $b (3+(Rnd 10)) (9+(Rnd 5)) (C 'C8C8CE') }
SaveItem $b 'ash'

# --- lawn and garden --------------------------------------------------------
$b = Icon; Handle $b $wood $woodL
Rct $b 2 2 11 5 (C '2E7D32'); Outl $b 2 2 11 5 (C '1A4A1E')      # deck
Rct $b 3 4 9 2 (C '3E9E42')
Disc $b 3 8 1 $black; Disc $b 11 8 1 $black                       # wheels
SaveItem $b 'lawn_mower'

$b = Icon; Handle $b $grey (C '6A6A72')
Rct $b 1 10 5 4 (C 'D94A2A'); Outl $b 1 10 5 4 (C '7E2119')
Rct $b 2 11 3 2 (C '2A2A30')
Rct $b 9 3 4 2 (C 'E0E0E4')                                       # motor housing
SaveItem $b 'weed_whacker'

$b = Icon; Handle $b $wood $woodL
Rct $b 8 2 5 2 $steel; Rct $b 8 4 5 1 $steelD
for($i=0;$i -lt 5;$i++){ P $b (8+$i) 5 $steel }
Rct $b 6 5 3 3 (C 'E08A20')                                       # grip
SaveItem $b 'hedge_trimmer'

$b = Icon; Handle $b $wood $woodL
Rct $b 8 2 6 1 $steelD
for($i=0;$i -lt 6;$i++){ Rct $b (8+$i) 3 1 3 $steel }
SaveItem $b 'rake'

$b = Icon; Rct $b 4 6 8 8 (C '3E8E9E'); Outl $b 4 6 8 8 (C '20606E')
Rct $b 5 4 4 2 (C '3E8E9E')                                       # neck
Rct $b 11 3 3 2 (C '5AAEBE'); Rct $b 13 1 2 3 (C '5AAEBE')        # spout
Rct $b 4 9 8 1 (C '2A707E')
P $b 14 0 (C '9ADCE8'); P $b 15 2 (C '9ADCE8')
SaveItem $b 'watering_can'

$b = Icon; Rct $b 3 4 10 10 (C 'B9A17A'); Outl $b 3 4 10 10 (C '7A6848')
Rct $b 4 2 8 2 (C 'A08A64')
Rct $b 5 7 6 4 $green; Rct $b 7 6 2 5 (C '5FC85A')                # leaf logo
SaveItem $b 'fertilizer_bag'

# --- TV, tape and home ------------------------------------------------------
$b = Icon; Rct $b 2 3 12 10 $black; Outl $b 2 3 12 10 (C '2E2E36')
Rct $b 3 4 10 4 (C 'E8E4D8'); Rct $b 4 5 8 2 (C 'C8C4B8')         # label
Disc $b 6 10 1 (C '5A5A62'); Disc $b 10 10 1 (C '5A5A62')
SaveItem $b 'vhs_blank'

$b = Icon; Rct $b 2 3 12 10 $black; Outl $b 2 3 12 10 (C '2E2E36')
Rct $b 3 4 10 4 (C 'E8E4D8')
Rct $b 4 5 6 1 (C '3A3A44'); Rct $b 4 6 5 1 (C '3A3A44')          # handwriting
P $b 11 5 $red; P $b 12 5 $red; P $b 11 6 $red; P $b 12 6 $red
Disc $b 6 10 1 (C '5A5A62'); Disc $b 10 10 1 (C '5A5A62')
SaveItem $b 'vhs_barbara_interview'

$b = Icon; Rct $b 5 11 6 3 $black; Rct $b 7 8 2 3 $steelD
for($i=0;$i -lt 7;$i++){ P $b (7-$i) (7-$i) $steel; P $b (9+$i) (7-$i) $steel }
P $b 1 1 $white; P $b 15 1 $white
SaveItem $b 'tv_antenna'

$b = Icon; Rct $b 2 4 12 8 (C '2A2A32'); Outl $b 2 4 12 8 (C '14141A')
Rct $b 3 5 10 6 (C '3A5A8A'); Rct $b 4 6 8 4 (C '5A8ACA')         # screen
Rct $b 2 12 12 2 (C '4A4A54')                                     # keyboard deck
SaveItem $b 'laptop'

$b = Icon; Rct $b 3 6 9 5 $black; Outl $b 3 6 9 5 (C '2E2E36')
Rct $b 11 7 3 3 $steel; Rct $b 12 8 2 1 $steelD
Rct $b 4 7 3 1 (C '4A9AD8')
SaveItem $b 'usb_drive'

$b = Icon; Rct $b 5 2 6 12 (C '2A2A32'); Outl $b 5 2 6 12 (C '14141A')
Rct $b 6 3 4 4 (C '4A8AD8')
for($j=0;$j -lt 3;$j++){ for($i=0;$i -lt 3;$i++){ P $b (6+$i) (9+$j) (C '6A6A74') } }
SaveItem $b 'burner_phone'

$b = Icon; Rct $b 4 1 8 6 (C '3A3A44'); Outl $b 4 1 8 6 (C '1A1A22')
Rct $b 5 2 6 4 (C '6ADCE8')                                       # open screen
Rct $b 4 8 8 7 (C '3A3A44'); Outl $b 4 8 8 7 (C '1A1A22')
for($j=0;$j -lt 3;$j++){ for($i=0;$i -lt 3;$i++){ P $b (5+$i*2) (9+$j*2) (C '9A9AA4') } }
SaveItem $b 'flip_phone'

$b = Icon; Rct $b 5 1 6 14 (C '25252C'); Outl $b 5 1 6 14 (C '12121A')
Rct $b 6 3 4 2 $red                                               # power
for($j=0;$j -lt 4;$j++){ Rct $b 6 (7+$j*2) 4 1 (C '5A5A64') }
SaveItem $b 'remote_control'

$b = Icon; Rct $b 1 5 14 8 (C '2A2A32'); Outl $b 1 5 14 8 (C '12121A')
Disc $b 4 9 2 (C '4A4A54'); Disc $b 11 9 2 (C '4A4A54')           # speakers
Rct $b 7 7 2 4 (C '8A8A94')
for($i=0;$i -lt 5;$i++){ P $b (10-$i) (4-[int]($i/2)) $steel }    # antenna
SaveItem $b 'krave_radio'

# --- fast food and drink ----------------------------------------------------
$b = Icon; Can $b (C '7E1F14') (C 'D9B44A') (C '5A1410'); SaveItem $b 'mr_pibb_xtra'
$b = Icon; Bottle $b (C '4A140E') (C 'C0392B') (C 'D9B44A'); SaveItem $b 'mr_pibb_two_liter'
$b = Icon; Bottle $b (C 'E08A20') (C '3E8E41') (C 'F2E4C0'); SaveItem $b 'chepina_jug'
$b = Icon; Bottle $b (C '4A5A2A') (C '6A6A70') (C '8A9A5A'); SaveItem $b 'sewer_water'

$b = Icon; Rct $b 2 3 12 3 $bun; Rct $b 3 2 10 1 (C 'E8B96E')     # top bun
Rct $b 2 6 12 2 (C '5A3A22'); Rct $b 2 8 12 1 (C 'E8C23A')        # patty, cheese
Rct $b 2 9 12 2 (C '5A3A22'); Rct $b 2 11 12 1 (C 'E8C23A')
Rct $b 2 12 12 3 $bun
for($i=0;$i -lt 6;$i++){ P $b (3+(Rnd 10)) 3 (C 'F2DCA8') }
SaveItem $b 'double_cheeseburger'

$b = Icon; Rct $b 2 4 12 3 $bun; Rct $b 3 3 10 1 (C 'E8B96E')
Rct $b 2 7 12 3 (C 'C08A3A'); Rct $b 2 8 12 1 (C 'A87028')
Rct $b 2 10 12 3 $bun
SaveItem $b 'chicken_sandwich'

$b = Icon; Rct $b 2 5 12 6 (C 'C99A3A'); Outl $b 2 5 12 6 (C '9A6E20')
for($i=0;$i -lt 5;$i++){ Rct $b (3+$i*2) 6 1 4 (C 'E0B657') }
SaveItem $b 'hash_browns'

$b = Icon; Rct $b 3 5 10 8 (C 'E8DCC0'); Outl $b 3 5 10 8 (C 'B09A70')
Rct $b 3 5 10 2 (C 'D9C8A0'); Rct $b 5 8 6 3 (C 'C0392B')
Rct $b 4 3 8 2 (C 'E8DCC0')
SaveItem $b 'apple_pie'

$b = Icon; Rct $b 4 5 8 9 (C 'F2E8E0'); Outl $b 4 5 8 9 (C 'C0B4A8')
Rct $b 4 5 8 3 (C 'E4B4C8'); Rct $b 3 3 10 2 (C 'F2E8E0')         # dome lid
Rct $b 10 0 2 4 (C 'C0392B')                                      # straw
Rct $b 5 9 6 1 (C 'D9CCC0')
SaveItem $b 'milkshake'

$b = Icon; Rct $b 4 4 8 10 (C 'C98A2A' 200); Outl $b 4 4 8 10 (C '8A6A48')
Rct $b 4 4 8 2 (C 'E8DCC8' 180)
Rct $b 6 6 2 2 (C 'F2F2F8' 200); Rct $b 9 9 2 2 (C 'F2F2F8' 200)  # ice
Rct $b 10 1 2 4 (C 'F2F2F8')
SaveItem $b 'sweet_tea'

$b = Icon; Rct $b 2 5 12 7 (C 'E8C88A'); Outl $b 2 5 12 7 (C 'B08A48')
for($i=0;$i -lt 4;$i++){ Rct $b (3+$i*3) 6 1 5 (C 'C99A3A') }
Rct $b 2 4 12 1 (C 'F2F2F4'); Rct $b 2 12 12 1 (C 'F2F2F4')       # wrapper
SaveItem $b 'honey_bun'

$b = Icon; Rct $b 1 6 14 4 $bun; Outl $b 1 6 14 4 (C 'B0803A')
Rct $b 2 7 12 2 (C 'A0442A')                                       # sausage
for($i=0;$i -lt 6;$i++){ P $b (3+$i*2) 7 (C 'E8C23A') }            # mustard
SaveItem $b 'gas_station_hot_dog'

$b = Icon; Rct $b 2 5 12 6 (C 'C8CCD2'); Outl $b 2 5 12 6 (C '8A8E96')
for($i=0;$i -lt 5;$i++){ Rct $b (3+$i*2) 5 1 6 (C 'E0E4EA') }      # foil creases
Rct $b 2 5 3 6 (C 'D9A45B')                                        # open end
SaveItem $b 'microwave_burrito'

# --- money and scams --------------------------------------------------------
$b = Icon; Sheet $b (C '9AC08A') (C '5A8A4A') 1 5 14 7
Disc $b 8 8 2 (C 'C8DCB8'); P $b 8 8 (C '3A6A2A')
Rct $b 2 6 3 1 $red                                                # off-register ink
SaveItem $b 'counterfeit_bill'

$b = Icon; Sheet $b $paper (C 'B0A890') 3 3 10 11
Rct $b 5 5 1 4 $ink; Rct $b 7 5 1 4 $ink; Rct $b 9 5 1 4 $ink      # I O U
SaveItem $b 'iou_note'

$b = Icon; Sheet $b $paper (C 'B0A890') 3 2 10 12
Outl $b 4 4 8 5 $red; Rct $b 5 6 6 1 $red
Rct $b 4 11 8 1 (C '3A3A44')
SaveItem $b 'debt_notice'

$b = Icon; Sheet $b (C 'F2F0E8') (C 'C0BCB0') 5 0 6 16
Rct $b 6 12 4 1 (C '8A8A90')
SaveItem $b 'scam_receipt'

# --- trophies ---------------------------------------------------------------
$b = Icon; Cup $b $gold $goldD $choc
Rct $b 6 3 4 3 $purple; P $b 6 4 $purpleL; P $b 9 4 $purpleL       # monster face
P $b 7 5 $white; P $b 8 5 $white
SaveItem $b 'krave_monster_trophy'

$b = Icon; Cup $b $steel $steelD $black
Rct $b 6 3 4 3 $black; P $b 6 4 $white; P $b 9 4 $white            # ski mask
SaveItem $b 'plug_trophy'

$b = Icon; Disc $b 8 7 6 $steel; Disc $b 8 7 4 (C 'D8DCE2'); Disc $b 8 7 1 $steelD
for($i=0;$i -lt 5;$i++){ P $b (8+[int](4*[Math]::Cos($i*1.256))) (7+[int](4*[Math]::Sin($i*1.256))) $steelD }
Rct $b 4 13 8 2 $black
SaveItem $b 'duhl_wol_trophy'

$b = Icon; Rct $b 2 3 12 10 $wood; Outl $b 2 3 12 10 (C '3A2A18')
Rct $b 4 5 8 6 $gold; Outl $b 4 5 8 6 $goldD
Rct $b 7 6 2 1 $red; Rct $b 7 7 2 3 $red                            # necktie
SaveItem $b 'barbara_trophy'

# --- the sewer and the red fit ----------------------------------------------
$b = Icon; Rct $b 3 4 3 3 $steel; Rct $b 5 6 6 2 $steel
Rct $b 11 5 3 4 $steel; Rct $b 12 6 1 1 $steelD; Rct $b 12 8 1 1 $steelD
Outl $b 3 4 3 3 $steelD
SaveItem $b 'sewer_key'

$b = Icon
for($i=0;$i -lt 12;$i++){ P $b (2+$i) (11-[int](3*[Math]::Sin($i*0.6))) $pink
                          P $b (2+$i) (12-[int](3*[Math]::Sin($i*0.6))) (C 'C07E88')  }
Rct $b 1 11 2 2 (C '8A5A60')
SaveItem $b 'rat_tail'

$b = Icon; Rct $b 4 2 8 4 $red; Rct $b 4 6 3 8 $red; Rct $b 9 6 3 8 $red
Rct $b 4 2 8 1 $redD; Rct $b 7 6 2 8 (C '00000000')
Rct $b 4 13 3 1 $redD; Rct $b 9 13 3 1 $redD
SaveItem $b 'red_pants'

$b = Icon; Rct $b 2 8 5 4 $red; Rct $b 2 11 6 1 $white
Rct $b 9 8 5 4 $red; Rct $b 9 11 6 1 $white
Rct $b 3 9 3 1 $white; Rct $b 10 9 3 1 $white
SaveItem $b 'red_shoes'

# ===========================================================================
# BLOCK TEXTURES
# ===========================================================================

function Plank($b,$base,$dark,$light){
    Rct $b 0 0 16 16 $base
    for($x=0;$x -lt 16;$x++){ for($y=0;$y -lt 16;$y++){
        $r = Rnd 9
        if($r -eq 0){ P $b $x $y $dark } elseif($r -eq 1){ P $b $x $y $light } } }
}

# stash box: a beaten wooden crate with iron corners
$b = NewImg 16 16; Plank $b $wood (C '4A3218') $woodL
Rct $b 0 0 16 1 (C '4A3218'); Rct $b 0 15 16 1 (C '4A3218')
Rct $b 0 0 1 16 (C '4A3218'); Rct $b 15 0 1 16 (C '4A3218')
Rct $b 0 7 16 2 (C '5A3E20')
Rct $b 0 0 2 2 $steelD; Rct $b 14 0 2 2 $steelD
Rct $b 0 14 2 2 $steelD; Rct $b 14 14 2 2 $steelD
SaveBlock $b 'stash_box_side'

$b = NewImg 16 16; Plank $b $woodL (C '5A3E20') (C 'A88A5A')
Outl $b 0 0 16 16 (C '4A3218')
Rct $b 6 6 4 4 $steelD; Rct $b 7 7 2 2 (C 'C8CCD2')                # latch
SaveBlock $b 'stash_box_top'

$b = NewImg 16 16; Plank $b $woodL (C '5A3E20') (C 'A88A5A')
Outl $b 0 0 16 16 (C '4A3218')
Rct $b 3 3 10 10 (C '2A2418')
for($i=0;$i -lt 40;$i++){ P $b (3+(Rnd 10)) (3+(Rnd 10)) $grass }
SaveBlock $b 'stash_box_top_half'

$b = NewImg 16 16; Plank $b $woodL (C '5A3E20') (C 'A88A5A')
Outl $b 0 0 16 16 (C '4A3218')
Rct $b 2 2 12 12 (C '3E7A28')
for($i=0;$i -lt 90;$i++){ $r = Rnd 3
    if($r -eq 0){ P $b (2+(Rnd 12)) (2+(Rnd 12)) $grass }
    elseif($r -eq 1){ P $b (2+(Rnd 12)) (2+(Rnd 12)) (C '76C44A') }
    else { P $b (2+(Rnd 12)) (2+(Rnd 12)) (C '2A5A18') } }
SaveBlock $b 'stash_box_top_full'

# boombox
$b = NewImg 16 16; Rct $b 0 0 16 16 (C '25252C')
Rct $b 0 0 16 2 (C '3A3A44'); Rct $b 0 14 16 2 (C '16161C')
Disc $b 4 9 3 (C '3A3A44'); Disc $b 4 9 2 $black
Disc $b 12 9 3 (C '3A3A44'); Disc $b 12 9 2 $black
Rct $b 6 3 4 3 (C '4A4A54'); Rct $b 7 4 2 1 (C '8A8A94')
SaveBlock $b 'boombox_front'

$b = NewImg 16 16; Rct $b 0 0 16 16 (C '25252C')
Rct $b 0 0 16 2 (C '3A3A44'); Rct $b 0 14 16 2 (C '16161C')
Disc $b 4 9 3 (C '4A4A54'); Disc $b 4 9 2 (C '2A2A32')
Disc $b 12 9 3 (C '4A4A54'); Disc $b 12 9 2 (C '2A2A32')
Rct $b 6 3 4 3 (C '4A4A54')
Rct $b 6 5 1 1 (C '5FE85A'); Rct $b 7 4 1 2 (C '5FE85A')
Rct $b 8 3 1 3 (C 'E8E23A'); Rct $b 9 4 1 2 (C 'E8523A')            # EQ bars lit
SaveBlock $b 'boombox_front_on'

$b = NewImg 16 16; Rct $b 0 0 16 16 (C '2A2A32')
for($j=0;$j -lt 6;$j++){ Rct $b 3 (3+$j*2) 10 1 (C '1A1A20') }
Rct $b 0 0 16 2 (C '3A3A44'); Rct $b 0 14 16 2 (C '16161C')
SaveBlock $b 'boombox_side'

$b = NewImg 16 16; Rct $b 0 0 16 16 (C '3A3A44')
Rct $b 3 2 10 2 (C '16161C')                                        # carry handle
Rct $b 2 8 12 4 (C '2A2A32')
for($i=0;$i -lt 5;$i++){ Rct $b (3+$i*2) 9 1 2 (C '8A8A94') }
SaveBlock $b 'boombox_top'

# television cabinet
function Cabinet($b){
    Plank $b (C '6A4020') (C '48280F') (C '8A5A30')
    Rct $b 0 0 16 1 (C '48280F'); Rct $b 0 15 16 1 (C '48280F')
    Rct $b 0 0 1 16 (C '48280F'); Rct $b 15 0 1 16 (C '48280F') }

$b = NewImg 16 16; Cabinet $b; SaveBlock $b 'tv_side'
$b = NewImg 16 16; Cabinet $b; Rct $b 2 2 12 12 (C '7A4A28'); SaveBlock $b 'tv_top'

$b = NewImg 16 16; Cabinet $b
Rct $b 1 2 11 11 (C '2A2A30'); Outl $b 1 2 11 11 (C '14141A')
Rct $b 2 3 9 9 (C '3A3A42')
Disc $b 14 5 1 (C 'C8CCD2'); Disc $b 14 9 1 (C 'C8CCD2')            # tuning knobs
SaveBlock $b 'tv_front_off'

# The animated screen: eight frames of snow plus one frame with a face in it.
$frameCount = 9
$tv = NewImg 16 (16*$frameCount)
for($f=0;$f -lt 8;$f++){
    $oy = $f*16
    # cabinet border, repeated per frame
    for($x=0;$x -lt 16;$x++){ for($y=0;$y -lt 16;$y++){
        $r = Rnd 9
        $col = C '6A4020'
        if($r -eq 0){ $col = C '48280F' } elseif($r -eq 1){ $col = C '8A5A30' }
        P $tv $x ($oy+$y) $col } }
    Rct $tv 1 ($oy+2) 11 11 (C '303038')
    for($x=2;$x -lt 11;$x++){ for($y=3;$y -lt 12;$y++){
        $r = Rnd 5
        if($r -eq 0){ P $tv $x ($oy+$y) (C 'D8D8E0') }
        elseif($r -eq 1){ P $tv $x ($oy+$y) (C '8A8A96') }
        elseif($r -eq 2){ P $tv $x ($oy+$y) (C '3A3A44') }
        else { P $tv $x ($oy+$y) (C '5A5A66') } } }
    Rct $tv 2 ($oy+3+($f % 9)) 9 1 (C 'F2F2F8')                     # rolling scanline
    Disc $tv 14 ($oy+5) 1 (C 'C8CCD2'); Disc $tv 14 ($oy+9) 1 (C 'C8CCD2')
}
# frame 8: the face
$oy = 8*16
for($x=0;$x -lt 16;$x++){ for($y=0;$y -lt 16;$y++){
    $r = Rnd 9
    $col = C '6A4020'
    if($r -eq 0){ $col = C '48280F' } elseif($r -eq 1){ $col = C '8A5A30' }
    P $tv $x ($oy+$y) $col } }
Rct $tv 1 ($oy+2) 11 11 (C '181820')
for($x=2;$x -lt 11;$x++){ for($y=3;$y -lt 12;$y++){
    if((Rnd 4) -eq 0){ P $tv $x ($oy+$y) (C '4A4A54') } } }
Rct $tv 3 ($oy+5) 2 2 (C 'F2F2F8'); Rct $tv 8 ($oy+5) 2 2 (C 'F2F2F8')   # eyes
P $tv 4 ($oy+6) $black; P $tv 9 ($oy+6) $black
Rct $tv 3 ($oy+9) 7 1 (C 'F2F2F8')
P $tv 4 ($oy+10) (C 'F2F2F8'); P $tv 6 ($oy+10) (C 'F2F2F8'); P $tv 8 ($oy+10) (C 'F2F2F8')
Disc $tv 14 ($oy+5) 1 (C 'C8CCD2'); Disc $tv 14 ($oy+9) 1 (C 'C8CCD2')
SaveBlock $tv 'tv_front'

# One face frame in eighty: often enough that people notice, rare enough that
# the first person to see it is not believed.
$frames = @()
for($i=0;$i -lt 80;$i++){ $frames += (Rnd 8) }
$frames[57] = 8
$mcmeta = '{' + [Environment]::NewLine +
          '  "animation": { "frametime": 1, "frames": [ ' + ($frames -join ', ') + ' ] }' +
          [Environment]::NewLine + '}'
$mcmetaPath = "$bdir\tv_front.png.mcmeta"
[System.IO.File]::WriteAllText($mcmetaPath, $mcmeta, (New-Object System.Text.UTF8Encoding($false)))
$script:written.Add($mcmetaPath)

# recliner upholstery
$b = NewImg 16 16; Rct $b 0 0 16 16 (C '7E2A22')
for($x=0;$x -lt 16;$x++){ for($y=0;$y -lt 16;$y++){
    $r = Rnd 7
    if($r -eq 0){ P $b $x $y (C '9A362C') } elseif($r -eq 1){ P $b $x $y (C '641E18') } } }
Rct $b 0 0 16 1 (C '4E1610'); Rct $b 0 15 16 1 (C '4E1610')
Rct $b 5 0 1 16 (C '5E1A14'); Rct $b 11 0 1 16 (C '5E1A14')          # seams
SaveBlock $b 'recliner_side'

$b = NewImg 16 16; Rct $b 0 0 16 16 (C '8E3028')
for($x=0;$x -lt 16;$x++){ for($y=0;$y -lt 16;$y++){
    $r = Rnd 7
    if($r -eq 0){ P $b $x $y (C 'AA4038') } elseif($r -eq 1){ P $b $x $y (C '6E241C') } } }
Rct $b 2 2 12 12 (C '9A362C'); Outl $b 2 2 12 12 (C '5E1A14')        # cushion
SaveBlock $b 'recliner_top'

# sewer pipe: wet concrete ring
$b = NewImg 16 16; Rct $b 0 0 16 16 (C '6A6E70')
for($x=0;$x -lt 16;$x++){ for($y=0;$y -lt 16;$y++){
    $r = Rnd 8
    if($r -eq 0){ P $b $x $y (C '585C5E') } elseif($r -eq 1){ P $b $x $y (C '7C8082') } } }
Disc $b 8 8 6 (C '3A3E40'); Disc $b 8 8 5 (C '1E2224')
for($i=0;$i -lt 16;$i++){ P $b (3+(Rnd 11)) (3+(Rnd 11)) (C '3E6A38') }   # slime
SaveBlock $b 'sewer_pipe'

# shag carpet: long-pile 70s brown/orange
$b = NewImg 16 16; Rct $b 0 0 16 16 (C '7A4A20')
for($x=0;$x -lt 16;$x++){ for($y=0;$y -lt 16;$y++){
    $r = Rnd 6
    if($r -eq 0){ P $b $x $y (C 'A86A28') } elseif($r -eq 1){ P $b $x $y (C '5A3418') }
    elseif($r -eq 2){ P $b $x $y (C 'C08A32') } } }
for($i=0;$i -lt 24;$i++){ $x = Rnd 16; $y = Rnd 15; P $b $x $y (C 'D9A45B'); P $b $x ($y+1) (C '4A2A12') }
SaveBlock $b 'shag_carpet'

# wood paneling: vertical grain, the wall of every basement in the tape
$b = NewImg 16 16; Rct $b 0 0 16 16 (C '7A5230')
for($x=0;$x -lt 16;$x++){
    $shade = Rnd 5
    for($y=0;$y -lt 16;$y++){
        if($shade -eq 0){ P $b $x $y (C '8E6238') }
        elseif($shade -eq 1){ P $b $x $y (C '644226') }
        if((Rnd 8) -eq 0){ P $b $x $y (C '523618') } } }
Rct $b 0 0 1 16 (C '3E2A14'); Rct $b 7 0 1 16 (C '3E2A14'); Rct $b 15 0 1 16 (C '4A3018')
SaveBlock $b 'wood_paneling'

# The recliner seat is never drawn (see ExtraClientSetup.SeatRenderer); this
# exists only so the renderer's texture handle resolves to something real.
$b = NewImg 2 2; Rct $b 0 0 2 2 (C '000000' 0)
$seatPath = "$edir\recliner_seat.png"
SaveAt $b $seatPath
$script:written.Add($seatPath)

# ===========================================================================
# VERIFY - a silent failure to write assets has burned this project before.
# ===========================================================================
$expected = @()
$itemNames = @(
    'golden_krave','cigarette','daniels_zippo','remote_control','flip_phone','krave_radio',
    'counterfeit_bill','krave_monster_trophy','plug_trophy','duhl_wol_trophy','barbara_trophy',
    'mr_pibb_xtra','mr_pibb_two_liter','chepina_jug','double_cheeseburger','chicken_sandwich',
    'hash_browns','apple_pie','milkshake','sweet_tea','honey_bun','gas_station_hot_dog',
    'microwave_burrito','stale_krave','off_brand_krave','sewer_water',
    'krave_dust','krave_family_box','cigarette_pack','cigar','matchbook','ash',
    'fertilizer_bag','vhs_blank','vhs_barbara_interview','tv_antenna','sewer_key','rat_tail',
    'red_pants','red_shoes','burner_phone','laptop','usb_drive','iou_note','debt_notice',
    'scam_receipt','lawn_mower','weed_whacker','hedge_trimmer','rake','watering_can')
$blockNames = @(
    'stash_box_side','stash_box_top','stash_box_top_half','stash_box_top_full',
    'boombox_front','boombox_front_on','boombox_side','boombox_top',
    'tv_front','tv_front_off','tv_side','tv_top',
    'recliner_side','recliner_top','sewer_pipe','shag_carpet','wood_paneling')
foreach($n in $itemNames){ $expected += "$idir\$n.png" }
foreach($n in $blockNames){ $expected += "$bdir\$n.png" }
$expected += "$bdir\tv_front.png.mcmeta"
$expected += "$edir\recliner_seat.png"

$missing = 0
foreach($p in $expected){
    if(Test-Path $p){ "  OK       $p" } else { "  MISSING  $p"; $missing++ } }
""
"$($expected.Count) expected, $missing missing."
if($missing -gt 0){ throw "make_extra_content.ps1 did not write $missing file(s)." }
