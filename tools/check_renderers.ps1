# Every entity we register must have a renderer registered for it.
#
# An entity with no renderer is invisible until something spawns one, and then
# the render dispatcher dereferences null and takes the game down. Neither the
# compiler nor the registry catches it: both halves compile perfectly alone, and
# an entity type with no renderer is legal right up until it is drawn.
#
# This scans EVERY source file, not a hand-listed pair of registry classes. The
# first version only looked at ModEntities and ExtraRegistry, so when 2.0 added
# eleven modules with their own DeferredRegisters it happily reported "all
# clear" while never looking at forty new entities. A check with a hardcoded
# search path silently stops covering the code that grows around it.
$src = Resolve-Path "$PSScriptRoot\..\src\main\java"
$files = Get-ChildItem $src -Recurse -Filter *.java

# Renderer registrations can live in any client setup class, so gather them all.
$clientText = ($files | Where-Object { (Get-Content $_.FullName -Raw) -match 'registerEntityRenderer' } |
        ForEach-Object { Get-Content $_.FullName -Raw }) -join "`n"

$missing = @()
foreach ($f in $files) {
    $text = Get-Content $f.FullName -Raw
    if ($text -notmatch 'DeferredRegister<EntityType') { continue }
    foreach ($m in ([regex]'RegistryObject<EntityType<[^>]+>>\s+([A-Z_0-9]+)').Matches($text)) {
        $field = $m.Groups[1].Value
        if ($clientText -notmatch ([regex]::Escape($field) + '\s*\.\s*get\s*\(\s*\)')) {
            $missing += "$($f.BaseName).$field"
        }
    }
}

if ($missing.Count -gt 0) {
    foreach ($x in $missing) { Write-Host "  NO RENDERER: $x" }
    throw "$($missing.Count) entity type(s) have no renderer. Register one (NoopRenderer::new if it draws nothing)."
}
Write-Host "Renderer check: every registered entity has a renderer."
