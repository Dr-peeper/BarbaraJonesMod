# Every living entity we register must have an attribute supplier bound to it.
#
# A LivingEntity type with no AttributeSupplier is legal to compile and legal
# to register. It fails later, quietly, in one of two ways depending on how it
# is spawned:
#
#   /summon           -> "Unable to summon entity"
#   create() in code  -> returns null, and callers null-check and move on
#
# The second one is the dangerous one. MomKraveStash was in exactly that state:
# MomCobbBoss.spawnStash() calls create(), gets null, returns, and her whole
# phase-three stash mechanic simply never happened - no crash, no log line,
# nothing to notice. The Manager and his minions were in the first state and
# were unspawnable by any means, with finished createAttributes() builders
# sitting right there that nothing ever called.
#
# This deliberately does NOT filter on MobCategory. Vanilla's own
# DefaultAttributes.validate() skips MISC, and so did the first version of this
# check - which is precisely why MomKraveStash (a Monster registered as MISC,
# because it wants a health pool but no spawn-group behaviour) slipped past
# both. What decides whether attributes are needed is the entity's SUPERCLASS,
# not the category it was filed under, so that is what this resolves - walking
# the extends chain across the whole codebase so a mod class that extends a mod
# class that extends Monster is still caught.
$src = Resolve-Path "$PSScriptRoot\..\src\main\java"
$files = Get-ChildItem $src -Recurse -Filter *.java

# --- class -> superclass, for every class in the mod --------------------------
$super = @{}
foreach ($f in $files) {
    $text = Get-Content $f.FullName -Raw
    foreach ($m in ([regex]'(?m)^\s*(?:public\s+|final\s+|abstract\s+)*class\s+(\w+)(?:<[^>]*>)?\s+extends\s+([\w.]+)').Matches($text)) {
        $super[$m.Groups[1].Value] = ($m.Groups[2].Value -split '\.')[-1]
    }
}

# Vanilla roots that carry an attribute map. Anything reaching one of these
# needs a supplier; anything that does not (plain Entity, projectiles, props)
# must not have one and is ignored.
$livingRoots = @(
    'LivingEntity', 'Mob', 'PathfinderMob', 'Monster', 'Animal', 'AgeableMob',
    'TamableAnimal', 'AbstractVillager', 'Villager', 'AbstractGolem',
    'FlyingMob', 'AbstractFish', 'WaterAnimal', 'Raider', 'AbstractIllager',
    'PatrollingMonster', 'AbstractSkeleton', 'Zombie', 'AbstractHorse'
)

function Test-IsLiving([string]$cls) {
    $seen = @{}
    $cur = $cls
    while ($cur -and -not $seen.ContainsKey($cur)) {
        if ($livingRoots -contains $cur) { return $true }
        $seen[$cur] = $true
        $cur = $super[$cur]
    }
    return $false
}

# --- gather every attribute binding, from every module -----------------------
# Comments are stripped first. Without that, prose is indistinguishable from
# code to a regex, and a comment merely *discussing* a binding - like the one
# in ModEntityAttributes explaining why MOM_STASH needs one - is enough to
# convince the check that the binding exists. That is a check that reports
# success because someone wrote about the bug.
function Remove-JavaComments([string]$t) {
    $t = [regex]::Replace($t, '/\*(?s:.*?)\*/', ' ')
    return [regex]::Replace($t, '(?m)//.*$', ' ')
}

$attrText = ($files | Where-Object {
        (Get-Content $_.FullName -Raw) -match 'EntityAttributeCreationEvent'
    } | ForEach-Object { Remove-JavaComments (Get-Content $_.FullName -Raw) }) -join "`n"

$missing = @()
foreach ($f in $files) {
    $text = Remove-JavaComments (Get-Content $f.FullName -Raw)
    if ($text -notmatch 'DeferredRegister<EntityType') { continue }

    # Field name plus the builder body it is assigned, so the entity class can
    # be read out of the `.of(Foo::new, ...)` factory reference.
    $pattern = 'RegistryObject<EntityType<[^>]+>>\s+([A-Z_0-9]+)\s*=(?<body>(?s).*?);'
    foreach ($m in ([regex]$pattern).Matches($text)) {
        $field = $m.Groups[1].Value
        $body  = $m.Groups['body'].Value
        if ($body -notmatch '(?:\.|<)?([\w.]+)::new') { continue }
        $cls = ($Matches[1] -split '\.')[-1]
        if (-not (Test-IsLiving $cls)) { continue }
        if ($attrText -notmatch ([regex]::Escape($field) + '\s*\.\s*get\s*\(\s*\)')) {
            $missing += "$($f.BaseName).$field  ($cls)"
        }
    }
}

if ($missing.Count -gt 0) {
    foreach ($x in $missing) { Write-Host "  NO ATTRIBUTES: $x" }
    throw "$($missing.Count) living entity type(s) have no attribute supplier. Bind one in an EntityAttributeCreationEvent handler, or create() returns null and they silently never spawn."
}
Write-Host "Attribute check: every living entity has an attribute supplier."
