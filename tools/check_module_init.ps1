# Every DeferredRegister declared anywhere must actually be handed to the mod bus.
#
# The internet module declared ITEMS, BLOCKS and ENTITIES, then shipped an
# init(IEventBus) whose body was a comment saying the orchestrator would call the
# registers itself. The orchestrator called init(). Nothing registered, and the
# game NPE'd on "Registry Object not present: barbarajones:internet_manager"
# before the title screen. It compiled perfectly - an empty method always does.
#
# Three things this has to get right, each of which fooled an earlier version:
#  - Two overloads share the name. register("some_id", () -> ..) registers
#    CONTENT and proves nothing; register(bus) is the handoff. Only the second
#    counts, and it is told apart by passing a variable rather than a string.
#  - Javadoc showing the call is not the call. Comments are stripped first.
#  - The handoff is often in a DIFFERENT file: BarbaraJonesMod calls
#    ModBlocks.BLOCKS.register(bus). So the whole tree is the haystack, and a
#    qualified Class.FIELD.register(x) counts from anywhere.
$src = Resolve-Path "$PSScriptRoot\..\src\main\java"
$files = Get-ChildItem $src -Recurse -Filter *.java

$strip = {
    param($t)
    $t = [regex]::Replace($t, '(?s)/\*.*?\*/', '')
    return [regex]::Replace($t, '(?m)//.*$', '')
}

$clean = @{}
foreach ($f in $files) { $clean[$f.FullName] = & $strip (Get-Content $f.FullName -Raw) }
$all = ($clean.Values) -join "`n"

$bad = @()
foreach ($f in $files) {
    $text = $clean[$f.FullName]
    $cls = $f.BaseName
    $declared = ([regex]'DeferredRegister<[^>]*(?:<[^>]*>)?>\s+([A-Z_0-9]+)\s*=').Matches($text) |
            ForEach-Object { $_.Groups[1].Value } | Select-Object -Unique
    foreach ($d in $declared) {
        $bare      = [regex]::Escape($d) + '\s*\.\s*register\s*\(\s*[A-Za-z_$]'
        $qualified = [regex]::Escape("$cls.$d") + '\s*\.\s*register\s*\(\s*[A-Za-z_$]'
        # Same-file handoff, or anyone in the tree calling it by its full name.
        if (($text -notmatch $bare) -and ($all -notmatch $qualified)) {
            $bad += "$cls`: $d is declared but never handed to the bus"
        }
    }
}

if ($bad.Count -gt 0) {
    foreach ($x in $bad) { Write-Host "  $x" }
    throw "$($bad.Count) DeferredRegister(s) are never registered. Add them to the module's init(IEventBus)."
}

# ---------------------------------------------------------------------------
# And the other half: an init that nobody calls.
#
# The check above proves the HANDOFF STATEMENT exists. It does not prove the
# method containing it ever runs, and those are different facts. The mayor
# module declared its register, wrote ITEMS.register(modEventBus) inside
# init(IEventBus), and was never added to BarbaraJonesMod - so this file
# reported success while ten items stayed unbound.
#
# What made it worse than silence: the module's creative-tab handler is
# annotation-scanned, so it fired anyway and called .get() on an unbound
# RegistryObject, NPE-ing while building the mod's own tab. A module that is
# half-wired crashes harder than one that is not wired at all.
$orphans = @()
foreach ($f in $files) {
    $text = $clean[$f.FullName]
    $cls = $f.BaseName
    if ($text -notmatch 'static\s+void\s+init\s*\(\s*IEventBus') { continue }
    # A call from anywhere in the tree, by bare or qualified name. The
    # declaration itself is excluded by requiring an argument that is not a
    # type - init(bus) counts, init(IEventBus bus) does not.
    $called = [regex]::Escape($cls) + '\s*\.\s*init\s*\(\s*[A-Za-z_$][A-Za-z_$0-9.]*\s*\)'
    if ($all -notmatch $called) {
        $orphans += "$cls`: init(IEventBus) is declared but nothing calls it"
    }
}

if ($orphans.Count -gt 0) {
    foreach ($x in $orphans) { Write-Host "  $x" -ForegroundColor Red }
    throw "$($orphans.Count) module init(IEventBus) method(s) are never called. Add them to BarbaraJonesMod."
}
Write-Host "Module init check: every DeferredRegister is handed to the bus, and every init is called."
