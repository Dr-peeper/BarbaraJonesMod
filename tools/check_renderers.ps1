# Every entity we register must have a renderer registered for it.
#
# An entity with no renderer is invisible until something spawns one, and then
# the render dispatcher dereferences null and takes the game down. The gap
# between those two moments can be months: smoke_ring and ember_cherry were both
# registered, both had fully written renderer classes sitting beside them, and
# nothing ever connected the two - so the crash waited for the first Torching.
#
# Neither the compiler nor the registry can catch that: both halves compile
# perfectly alone, and an entity type with no renderer is legal right up until
# it is drawn. So it is checked here, before the jar is built.
$src = "$PSScriptRoot\..\src\main\java\com\barbarajones"
$client = Get-Content "$src\client\ClientSetup.java" -Raw

$missing = @()
foreach ($f in @("$src\content\ModEntities.java", "$src\content\extra\ExtraRegistry.java")) {
    if (-not (Test-Path $f)) { continue }
    $text = Get-Content $f -Raw
    foreach ($m in ([regex]'RegistryObject<EntityType<[^>]+>>\s+([A-Z_0-9]+)').Matches($text)) {
        $field = $m.Groups[1].Value
        if ($client -notmatch [regex]::Escape($field) + '\s*\.\s*get\s*\(\s*\)') {
            $missing += $field
        }
    }
}

if ($missing.Count -gt 0) {
    foreach ($f in $missing) { Write-Host "  NO RENDERER: $f" }
    throw "$($missing.Count) entity type(s) have no renderer. Register one in ClientSetup (NoopRenderer::new if it draws nothing)."
}
Write-Host "Renderer check: every registered entity has a renderer."
