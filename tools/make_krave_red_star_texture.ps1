# Krave Red Star: a procedural glowing starburst texture, rendered through
# RenderType.eyes() (the same always-full-bright layer vanilla uses for
# enderman/spider eyes) so it reads as genuinely emissive light regardless
# of the dimension's own (still fairly dim) ambient light - not a real point
# light source, Minecraft doesn't have one for entities, but it looks like
# one and gives the sky something to be lit BY.
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent $PSScriptRoot
$bdir = "$root\src\main\resources\assets\barbarajones\textures\entity"
New-Item -ItemType Directory -Force $bdir | Out-Null

$size = 64
$bmp = New-Object System.Drawing.Bitmap $size, $size, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$cx = $size / 2.0
$cy = $size / 2.0
$core = $size * 0.16
$glow = $size * 0.5

for ($y = 0; $y -lt $size; $y++) {
    for ($x = 0; $x -lt $size; $x++) {
        $dx = $x - $cx
        $dy = $y - $cy
        $dist = [Math]::Sqrt($dx * $dx + $dy * $dy)

        # An 8-point starburst: brighter along 8 spokes, dimmer between them.
        $angle = [Math]::Atan2($dy, $dx)
        $spoke = [Math]::Abs([Math]::Cos($angle * 4.0))
        $spokeBoost = 1.0 + $spoke * 0.9

        $falloff = [Math]::Max(0.0, 1.0 - ($dist / $glow))
        $falloff = [Math]::Pow($falloff, 1.6) * $spokeBoost
        $falloff = [Math]::Min(1.0, $falloff)

        if ($dist -lt $core) {
            # White-hot core.
            $r = 255; $g = [int](230 * (1.0 - ($dist / $core) * 0.3)); $b = [int](210 * (1.0 - ($dist / $core) * 0.5))
            $a = 255
        } elseif ($falloff -gt 0.02) {
            $r = 255
            $g = [int](60 + 60 * $falloff)
            $b = [int](20 + 20 * $falloff)
            $a = [int](255 * $falloff)
        } else {
            $r = 0; $g = 0; $b = 0; $a = 0
        }
        $bmp.SetPixel($x, $y, [System.Drawing.Color]::FromArgb($a, $r, $g, $b))
    }
}

$bmp.Save("$bdir\krave_red_star.png", [System.Drawing.Imaging.ImageFormat]::Png)
"wrote krave_red_star.png ($size x $size, procedural starburst)"
