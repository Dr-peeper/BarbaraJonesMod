# Krave Grass/Dirt AND chocolate tree textures: literally vanilla's dirt/
# grass/oak_log/oak_leaves art, recolored - not original designs. Source
# PNGs are extracted from the vanilla client jar
# (assets/minecraft/textures/block/...) into tools/vanilla_src/ before
# running this script - grass_block_top.png and grass_block_side_overlay.png
# are the grayscale "tintable" masks vanilla itself recolors per-biome at
# runtime; dirt.png/oak_log*.png/oak_leaves.png are already-colored art.
# Every source is recolored the same way regardless of which kind it is:
# convert to luminance, multiply by a fixed Krave tint - the same math
# vanilla's own biome tint applies, just baked into the file once instead of
# computed every frame, since none of these blocks use the tint system.
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
$srcDir = "$PSScriptRoot\vanilla_src"
$bdir = "$root\src\main\resources\assets\barbarajones\textures\block"
$miscdir = "$root\src\main\resources\assets\barbarajones\textures\misc"
New-Item -ItemType Directory -Force $miscdir | Out-Null

function Load($name){ [System.Drawing.Bitmap]::new("$srcDir\$name") }
function NewImg($w,$h){ New-Object System.Drawing.Bitmap $w,$h,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function ToRgb([string]$h){ [Convert]::ToInt32($h.Substring(0,2),16),[Convert]::ToInt32($h.Substring(2,2),16),[Convert]::ToInt32($h.Substring(4,2),16) }

# Recolor by luminance*tint - works whether the source is already grayscale
# (the tint masks) or fully colored (dirt.png) since it discards the
# original hue either way and imposes the new one uniformly.
function Recolor($src, [string]$tintHex){
    $tint = ToRgb $tintHex
    $out = NewImg $src.Width $src.Height
    for($x=0; $x -lt $src.Width; $x++){ for($y=0; $y -lt $src.Height; $y++){
        $p = $src.GetPixel($x,$y)
        $lum = (0.299*$p.R + 0.587*$p.G + 0.114*$p.B) / 255.0
        $r = [Math]::Min(255,[int]($lum*$tint[0]))
        $g = [Math]::Min(255,[int]($lum*$tint[1]))
        $b = [Math]::Min(255,[int]($lum*$tint[2]))
        $out.SetPixel($x,$y,[System.Drawing.Color]::FromArgb($p.A,$r,$g,$b))
    }}
    return $out
}

function Composite($base,$overlay){
    $out = NewImg $base.Width $base.Height
    for($x=0; $x -lt $base.Width; $x++){ for($y=0; $y -lt $base.Height; $y++){
        $ov = $overlay.GetPixel($x,$y)
        if($ov.A -gt 0){ $out.SetPixel($x,$y,$ov) } else { $out.SetPixel($x,$y,$base.GetPixel($x,$y)) }
    }}
    return $out
}

$dirtTint = 'B85C3C'    # rusty terracotta - same warm-red family as the box, reads as "dirt"
$grassTint = 'C62828'   # the Krave Box's own red, exactly - ties the whole Kosmos to one palette

$dirtSrc = Load 'dirt.png'
$dirtOut = Recolor $dirtSrc $dirtTint
$dirtOut.Save("$bdir\krave_dirt.png", [System.Drawing.Imaging.ImageFormat]::Png)

$topSrc = Load 'grass_block_top.png'
$topOut = Recolor $topSrc $grassTint
$topOut.Save("$bdir\krave_grass_top.png", [System.Drawing.Imaging.ImageFormat]::Png)

$overlaySrc = Load 'grass_block_side_overlay.png'
$overlayTinted = Recolor $overlaySrc $grassTint
# vanilla's overlay mask is fully opaque where it draws (it's the grayscale
# fringe art itself, not an alpha gradient) - use its own luminance as an
# alpha mask so it blends into the dirt body instead of stamping a hard edge.
for($x=0; $x -lt $overlayTinted.Width; $x++){ for($y=0; $y -lt $overlayTinted.Height; $y++){
    $p = $overlayTinted.GetPixel($x,$y)
    $srcP = $overlaySrc.GetPixel($x,$y)
    $lum = [int]((0.299*$srcP.R + 0.587*$srcP.G + 0.114*$srcP.B))
    $overlayTinted.SetPixel($x,$y,[System.Drawing.Color]::FromArgb($lum,$p.R,$p.G,$p.B))
}}
$dirtSideBase = Recolor (Load 'grass_block_side.png') $dirtTint
$sideOut = Composite $dirtSideBase $overlayTinted
$sideOut.Save("$bdir\krave_grass_side.png", [System.Drawing.Imaging.ImageFormat]::Png)

# ---- chocolate trees: vanilla oak log/leaves art, recolored -------------------
$logTint = '6B4226'     # chocolate brown - matches the mod's existing $choc/$chocL tones
$leafTint = 'C62828'    # the same Krave Box red as the grass - "red leaves"

$logSideOut = Recolor (Load 'oak_log.png') $logTint
$logSideOut.Save("$bdir\chocolate_log.png", [System.Drawing.Imaging.ImageFormat]::Png)

$logTopOut = Recolor (Load 'oak_log_top.png') $logTint
$logTopOut.Save("$bdir\chocolate_log_top.png", [System.Drawing.Imaging.ImageFormat]::Png)

$leavesOut = Recolor (Load 'oak_leaves.png') $leafTint
$leavesOut.Save("$bdir\krave_leaves.png", [System.Drawing.Imaging.ImageFormat]::Png)

$planksOut = Recolor (Load 'oak_planks.png') $logTint
$planksOut.Save("$bdir\chocolate_planks.png", [System.Drawing.Imaging.ImageFormat]::Png)

# Chocolate Block (formerly "Krave Block", the portal-frame/structure material) -
# vanilla stone bricks, recolored dark chocolate. A different, darker tint
# than the planks/log - it's meant to read as a dense, crunchy building
# material, not the same wood-brown as the trees.
$blockTint = '4A2C18'
$krBlockOut = Recolor (Load 'stone_bricks.png') $blockTint
$krBlockOut.Save("$bdir\krave_block.png", [System.Drawing.Imaging.ImageFormat]::Png)

# In-fluid screen overlay - vanilla's own underwater.png is just a flat
# translucent tinted tile (not a wave pattern), so recoloring it brown
# reproduces the same "tinted screen while submerged" mechanic water uses,
# for chocolate. This is the effect that actually reads as "hard to see" -
# lava itself only uses short fog distance (no overlay texture exists for
# it), which is a separate, harder-to-verify-from-outside-a-running-game
# mechanism.
$overlayOut = Recolor (Load 'underwater.png') 'D6A86A'
$overlayOut.Save("$miscdir\chocolate_overlay.png", [System.Drawing.Imaging.ImageFormat]::Png)

Write-Output "wrote krave_dirt, krave_grass_top, krave_grass_side, chocolate_log(+top), krave_leaves, chocolate_planks, krave_block, chocolate_overlay (recolored from vanilla art)"
