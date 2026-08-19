# World ambience and structures: regenerates every worldgen data file the
# com.barbarajones.world package needs, then reports OK / MISSING / BAD JSON for
# each one. No textures are produced - the five overworld set pieces are built
# entirely from vanilla blocks plus the existing Krave block, on purpose, so
# they cannot break if a texture pipeline changes underneath them.
#
# Safe to re-run: every file is rewritten from the table below.

$repoRoot = Split-Path -Parent $PSScriptRoot
$cfDir = "$repoRoot\src\main\resources\data\barbarajones\worldgen\configured_feature"
$pfDir = "$repoRoot\src\main\resources\data\barbarajones\worldgen\placed_feature"
$bmDir = "$repoRoot\src\main\resources\data\barbarajones\forge\biome_modifier"
New-Item -ItemType Directory -Force $cfDir, $pfDir, $bmDir | Out-Null

# Worldgen JSON is parsed by the datapack registry loader, which is stricter
# than the model loader - write plain UTF-8 with no byte order mark.
$utf8NoBom = New-Object System.Text.UTF8Encoding $false
$written = @()
function WriteJson([string]$path, [string]$text) {
    [System.IO.File]::WriteAllText($path, $text, $utf8NoBom)
    $script:written += $path
}

# feature id -> rarity_filter chance (roughly one per N chunks)
$features = [ordered]@{
    'barbara_lawn'   = 44
    'sewer_entrance' = 96
    'burnt_patch'    = 30
    'abandoned_car'  = 72
    'cereal_shrine'  = 128
}

foreach ($name in $features.Keys) {
    $chance = $features[$name]

    WriteJson "$cfDir\$name.json" @"
{
  "type": "barbarajones:$name",
  "config": {}
}
"@

    WriteJson "$pfDir\$name.json" @"
{
  "feature": "barbarajones:$name",
  "placement": [
    { "type": "minecraft:rarity_filter", "chance": $chance },
    { "type": "minecraft:in_square" },
    { "type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG" },
    { "type": "minecraft:biome" }
  ]
}
"@
}

# The built pieces run in surface_structures; the burn scar is grass being
# replaced, so it belongs with the vegetation pass.
WriteJson "$bmDir\overworld_set_dressing.json" @'
{
  "type": "forge:add_features",
  "biomes": "#minecraft:is_overworld",
  "features": [
    "barbarajones:barbara_lawn",
    "barbarajones:sewer_entrance",
    "barbarajones:abandoned_car",
    "barbarajones:cereal_shrine"
  ],
  "step": "surface_structures"
}
'@

WriteJson "$bmDir\overworld_burn_scars.json" @'
{
  "type": "forge:add_features",
  "biomes": "#minecraft:is_overworld",
  "features": [
    "barbarajones:burnt_patch"
  ],
  "step": "vegetal_decoration"
}
'@

# ---- report -------------------------------------------------------------------
# A silent write failure here shows up much later as an empty world, so every
# file gets confirmed by reading it back and parsing it.
$bad = 0
foreach ($path in $written) {
    if (-not (Test-Path $path)) {
        "MISSING   $path"
        $bad++
        continue
    }
    try {
        Get-Content $path -Raw | ConvertFrom-Json | Out-Null
        "OK        $path"
    } catch {
        "BAD JSON  $path"
        $bad++
    }
}

if ($bad -gt 0) {
    "done with $bad problem(s)"
    exit 1
}
"done - $($written.Count) files"
