# Krave Tuff, Krave Cobblestone, and the Chocolate Door: vanilla tuff/
# cobblestone/spruce_door art, recolored - not original designs. Same
# technique as make_krave_terrain_textures.ps1 (luminance * fixed tint).
# Source PNGs live in tools/vanilla_src/, extracted from the vanilla client
# jar (assets/minecraft/textures/block/...).
#
# These three exist specifically to reskin an imported castle schematic
# (fortress.litematic) into krave materials: the castle used three separate
# stone tones (stone bricks, tuff, cobblestone) for masonry contrast, and
# krave_block already covers stone bricks - these two cover the other two
# so that contrast survives instead of flattening into one material.
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
$srcDir = "$PSScriptRoot\vanilla_src"
$bdir = "$root\src\main\resources\assets\barbarajones\textures\block"

function Load($name){ [System.Drawing.Bitmap]::new("$srcDir\$name") }
function NewImg($w,$h){ New-Object System.Drawing.Bitmap $w,$h,([System.Drawing.Imaging.PixelFormat]::Format32bppArgb) }
function ToRgb([string]$h){ [Convert]::ToInt32($h.Substring(0,2),16),[Convert]::ToInt32($h.Substring(2,2),16),[Convert]::ToInt32($h.Substring(4,2),16) }

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

# Lighter milk-chocolate tone - distinct from krave_block's dark 4A2C18, so
# tuff-trimmed walls still read as two-tone masonry instead of flattening.
$tuffTint = '6B4226'
(Recolor (Load 'tuff.png') $tuffTint).Save("$bdir\krave_tuff.png", [System.Drawing.Imaging.ImageFormat]::Png)

# Rougher caramel tone, lighter still - cobblestone was the roughest/lightest
# of the castle's three stones, so it gets the lightest krave tint too.
$cobbleTint = '8B5A2B'
(Recolor (Load 'cobblestone.png') $cobbleTint).Save("$bdir\krave_cobblestone.png", [System.Drawing.Imaging.ImageFormat]::Png)

# Chocolate Door: a genuinely different door from the Krave Door (that one's
# a lighter "chocolate bar" pattern, portal-linked) - dark spruce planking,
# recolored, so the two never read as the same block at a glance.
$doorTint = '5C3A21'
(Recolor (Load 'spruce_door_top.png') $doorTint).Save("$bdir\chocolate_door_top.png", [System.Drawing.Imaging.ImageFormat]::Png)
(Recolor (Load 'spruce_door_bottom.png') $doorTint).Save("$bdir\chocolate_door_bottom.png", [System.Drawing.Imaging.ImageFormat]::Png)

Write-Output "wrote krave_tuff, krave_cobblestone, chocolate_door_top(+bottom) (recolored from vanilla art)"
