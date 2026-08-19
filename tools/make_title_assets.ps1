# make_title_assets.ps1 - artwork for the custom Krave title screen.
#
# Writes three textures into assets/barbarajones/textures/gui/title:
#
#   logo.png    512x128   the KRAVE mod mark - galaxy, stars, tumbling cereal and
#                         chunky gold letters. Blitted large behind the menu.
#   cereal.png   64x16    a 4x1 strip of 16x16 cereal pieces. The falling field on
#                         the title screen picks a variant per piece.
#   haze.png    128x128   a SEAMLESS purple nebula tile. The title screen scrolls two
#                         copies of it at different speeds, so any seam would show up
#                         in game as a moving grid - hence the nine-offset draw below.
#
# The menu plates, the button frames and the vignette are NOT textures: they are
# drawn procedurally in KraveTitleScreen so they stay crisp at any button width.
#
# Usage:
#   powershell -NoProfile -ExecutionPolicy Bypass -File tools\make_title_assets.ps1

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$repoRoot = Split-Path -Parent $PSScriptRoot
$outDir   = Join-Path $repoRoot "src\main\resources\assets\barbarajones\textures\gui\title"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

# ---------------------------------------------------------------- helpers ----

function C([int]$a, [int]$r, [int]$g, [int]$b) {
    return [System.Drawing.Color]::FromArgb($a, $r, $g, $b)
}

function New-Gfx($bmp) {
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode      = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode  = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode    = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.TextRenderingHint  = [System.Drawing.Text.TextRenderingHint]::AntiAlias
    $g.Clear((C 0 0 0 0))
    return $g
}

# A soft radial glow that fades to fully transparent at its rim. Every nebula cloud
# and every bloom in this file is just a pile of these stacked on top of each other.
function Add-Blob($g, [single]$cx, [single]$cy, [single]$r, $col, [int]$alpha) {
    if ($r -le 0.5) { return }
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $p.AddEllipse(($cx - $r), ($cy - $r), ($r * 2), ($r * 2))
    $b = New-Object System.Drawing.Drawing2D.PathGradientBrush -ArgumentList $p
    $b.CenterColor    = (C $alpha $col.R $col.G $col.B)
    $b.SurroundColors = @((C 0 $col.R $col.G $col.B))
    $g.FillPath($b, $p)
    $b.Dispose()
    $p.Dispose()
}

function New-RoundRect([single]$x, [single]$y, [single]$w, [single]$h, [single]$r) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $r * 2
    $p.AddArc($x, $y, $d, $d, 180, 90)
    $p.AddArc(($x + $w - $d), $y, $d, $d, 270, 90)
    $p.AddArc(($x + $w - $d), ($y + $h - $d), $d, $d, 0, 90)
    $p.AddArc($x, ($y + $h - $d), $d, $d, 90, 90)
    $p.CloseFigure()
    return $p
}

# Shell / rim / filling colours per cereal variant:
#   0 toasted   1 dark toasted   2 chocolate-heavy   3 Krave Kosmos pillow
$shellOuter = @( (C 255 206 146  78), (C 255 176 116  56), (C 255 122  70  36), (C 255 138  84 206) )
$shellEdge  = @( (C 255 128  78  34), (C 255 104  60  24), (C 255  66  34  14), (C 255  74  34 128) )
$shellCore  = @( (C 255  74  38  18), (C 255  58  28  12), (C 255  36  16   6), (C 255  40  14  74) )

$nebula = @(
    (C 255 126  58 196), (C 255  74  32 150), (C 255 176  74 214),
    (C 255  52  26 122), (C 255  96  44 176), (C 255 200 120 235)
)

# Draw one cereal piece centred on the current transform origin.
function Add-Piece($g, [single]$size, [int]$variant) {
    $half = $size / 2
    $body = New-RoundRect (0 - $half) (0 - $half) $size ($size * 0.86) ($size * 0.28)
    $fill = New-Object System.Drawing.SolidBrush -ArgumentList $shellOuter[$variant]
    $g.FillPath($fill, $body)
    $rim = New-Object System.Drawing.Pen -ArgumentList @($shellEdge[$variant], [single]($size * 0.09))
    $g.DrawPath($rim, $body)

    # The split down the middle where the chocolate shows through.
    $core = New-RoundRect (0 - $size * 0.26) (0 - $size * 0.16) ($size * 0.52) ($size * 0.30) ($size * 0.14)
    $coreFill = New-Object System.Drawing.SolidBrush -ArgumentList $shellCore[$variant]
    $g.FillPath($coreFill, $core)

    $gloss = New-Object System.Drawing.SolidBrush -ArgumentList (C 90 255 255 255)
    $g.FillEllipse($gloss, (0 - $size * 0.34), (0 - $size * 0.40), ($size * 0.26), ($size * 0.16))

    $fill.Dispose(); $rim.Dispose(); $coreFill.Dispose(); $gloss.Dispose()
    $body.Dispose(); $core.Dispose()
}

# ------------------------------------------------------------- cereal.png ----

$cereal = New-Object System.Drawing.Bitmap 64,16,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$cg = New-Gfx $cereal
for ($v = 0; $v -lt 4; $v++) {
    $state = $cg.Save()
    $cg.TranslateTransform(($v * 16 + 8), 8)
    Add-Piece $cg 13.0 $v
    if ($v -eq 3) {
        # The Kosmos pillow gets two stars in it, so galaxy pieces read as galaxy.
        $star = New-Object System.Drawing.SolidBrush -ArgumentList (C 235 255 250 220)
        $cg.FillRectangle($star, -3, -2, 1, 1)
        $cg.FillRectangle($star,  2,  1, 1, 1)
        $star.Dispose()
    }
    $cg.Restore($state)
}
$cg.Dispose()
$cereal.Save((Join-Path $outDir "cereal.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$cereal.Dispose()

# --------------------------------------------------------------- haze.png ----

$haze = New-Object System.Drawing.Bitmap 128,128,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$hg = New-Gfx $haze
$hrand = New-Object System.Random -ArgumentList 90210
for ($i = 0; $i -lt 30; $i++) {
    $bx  = [single]($hrand.NextDouble() * 128.0)
    $by  = [single]($hrand.NextDouble() * 128.0)
    $r   = [single](14.0 + $hrand.NextDouble() * 40.0)
    $col = $nebula[$hrand.Next($nebula.Count)]
    $a   = 24 + $hrand.Next(46)
    # Nine copies, one per wrap offset. Every blob that pokes over an edge is
    # therefore also drawn on the opposite edge, which is what makes the tile
    # seamless when the title screen scrolls it.
    foreach ($ox in -128, 0, 128) {
        foreach ($oy in -128, 0, 128) {
            Add-Blob $hg ($bx + $ox) ($by + $oy) $r $col $a
        }
    }
}
$hg.Dispose()
$haze.Save((Join-Path $outDir "haze.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$haze.Dispose()

# --------------------------------------------------------------- logo.png ----

$logo = New-Object System.Drawing.Bitmap 512,128,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$lg = New-Gfx $logo
$lrand = New-Object System.Random -ArgumentList 1337

# galaxy behind the letters
for ($i = 0; $i -lt 34; $i++) {
    $bx  = [single](30.0 + $lrand.NextDouble() * 452.0)
    $by  = [single](10.0 + $lrand.NextDouble() * 108.0)
    $r   = [single](22.0 + $lrand.NextDouble() * 56.0)
    $col = $nebula[$lrand.Next($nebula.Count)]
    Add-Blob $lg $bx $by $r $col (28 + $lrand.Next(58))
}

# stars
for ($i = 0; $i -lt 130; $i++) {
    $sx = $lrand.Next(0, 512)
    $sy = $lrand.Next(0, 128)
    $sz = if ($lrand.Next(8) -eq 0) { 2 } else { 1 }
    $sb = New-Object System.Drawing.SolidBrush -ArgumentList (C (90 + $lrand.Next(160)) 255 244 214)
    $lg.FillRectangle($sb, $sx, $sy, $sz, $sz)
    $sb.Dispose()
}

# cereal tumbling through the mark, behind the letters
for ($i = 0; $i -lt 16; $i++) {
    $px  = [single](10.0 + $lrand.NextDouble() * 492.0)
    $py  = [single](10.0 + $lrand.NextDouble() * 108.0)
    $sz  = [single](12.0 + $lrand.NextDouble() * 11.0)
    $ang = [single]($lrand.NextDouble() * 360.0)
    $state = $lg.Save()
    $lg.TranslateTransform($px, $py)
    $lg.RotateTransform($ang)
    Add-Piece $lg $sz ($lrand.Next(4))
    $lg.Restore($state)
}

# "KRAVE" - the loudest face this mod has
$famNames = @("Impact", "Arial Black", "Segoe UI Black", "Franklin Gothic Heavy", "Arial")
$fam = $null
foreach ($n in $famNames) {
    try { $fam = New-Object System.Drawing.FontFamily -ArgumentList $n; break } catch { }
}
if ($null -eq $fam) { $fam = [System.Drawing.FontFamily]::GenericSansSerif }

# Impact ships without a synthesised bold face; asking for one throws outright.
$style = [System.Drawing.FontStyle]::Bold
if (-not $fam.IsStyleAvailable($style)) { $style = [System.Drawing.FontStyle]::Regular }

$word = New-Object System.Drawing.Drawing2D.GraphicsPath
$origin = New-Object System.Drawing.PointF -ArgumentList @([single]0, [single]0)
$word.AddString("KRAVE", $fam, [int]$style, [single]120, $origin,
                [System.Drawing.StringFormat]::GenericTypographic)

$bb = $word.GetBounds()
if ($bb.Width -le 1 -or $bb.Height -le 1) { throw "font '$($fam.Name)' produced an empty KRAVE outline" }

# Fit the glyph outline to the canvas, then centre it on what it actually measures -
# nominal font metrics include leading that would push the word visibly off-centre.
$fit = [Math]::Min((450.0 / $bb.Width), (84.0 / $bb.Height))
$mScale = New-Object System.Drawing.Drawing2D.Matrix
$mScale.Scale([single]$fit, [single]$fit)
$word.Transform($mScale)

$bb = $word.GetBounds()
$mMove = New-Object System.Drawing.Drawing2D.Matrix
$mMove.Translate([single](256.0 - ($bb.X + $bb.Width / 2.0)), [single](62.0 - ($bb.Y + $bb.Height / 2.0)))
$word.Transform($mMove)
$bb = $word.GetBounds()

# three outline passes, widest first: black shadow, deep purple, then a violet rim
$penDark = New-Object System.Drawing.Pen -ArgumentList @((C 255 10 2 20), [single]15)
$penDark.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
$lg.DrawPath($penDark, $word)
$penDark.Dispose()

$penDeep = New-Object System.Drawing.Pen -ArgumentList @((C 255 62 20 104), [single]9)
$penDeep.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
$lg.DrawPath($penDeep, $word)
$penDeep.Dispose()

$penRim = New-Object System.Drawing.Pen -ArgumentList @((C 255 158 88 228), [single]4)
$penRim.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Round
$lg.DrawPath($penRim, $word)
$penRim.Dispose()

$goldRect = New-Object System.Drawing.RectangleF -ArgumentList @(
    [single]$bb.X, [single]($bb.Y - 2), [single]$bb.Width, [single]($bb.Height + 4))
$gold = New-Object System.Drawing.Drawing2D.LinearGradientBrush -ArgumentList @(
    $goldRect, (C 255 255 232 150), (C 255 224 120 26), [single]90)
$lg.FillPath($gold, $word)
$gold.Dispose()

$lg.SetClip($word)
$shineRect = New-Object System.Drawing.RectangleF -ArgumentList @(
    [single]$bb.X, [single]$bb.Y, [single]$bb.Width, [single]($bb.Height * 0.55))
$shine = New-Object System.Drawing.Drawing2D.LinearGradientBrush -ArgumentList @(
    $shineRect, (C 150 255 255 255), (C 0 255 255 255), [single]90)
$lg.FillRectangle($shine, $shineRect)
$shine.Dispose()
$lg.ResetClip()
$word.Dispose()

$lg.Dispose()
$logo.Save((Join-Path $outDir "logo.png"), [System.Drawing.Imaging.ImageFormat]::Png)
$logo.Dispose()

# ---------------------------------------------------------------- report ----
# A silently missing asset has burned this project before: a title screen with no
# logo just renders nothing and looks like the code is broken. Fail loudly instead.

$expected = @("logo.png", "cereal.png", "haze.png")
$missing = 0
foreach ($name in $expected) {
    $path = Join-Path $outDir $name
    if (Test-Path $path) {
        "  OK       $path"
    } else {
        "  MISSING  $path"
        $missing++
    }
}
if ($missing -gt 0) { throw "$missing title asset(s) were not written." }
Write-Host "Title assets written to $outDir"
