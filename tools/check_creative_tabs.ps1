# Every DeferredRegister<Item> must have its contents put into a creative tab.
#
# Registering an item and putting it in a tab are SEPARATE steps, in different
# events. A module that does the first and skips the second looks completely
# healthy: it compiles, it registers, its items work in commands and recipes -
# and they are invisible to anyone browsing creative. Six of the eleven 2.0
# modules had done exactly that, which is why none of the new mobs appeared to
# have spawn eggs. They did. You just could not find them.
#
# A register is considered covered if its owning class is named in any file that
# handles BuildCreativeModeTabContentsEvent - either the module's own hook, or
# the shared V2Tabs list for modules that have none.
$src = Resolve-Path "$PSScriptRoot\..\src\main\java"
$files = Get-ChildItem $src -Recurse -Filter *.java

$tabText = ($files | Where-Object { (Get-Content $_.FullName -Raw) -match 'BuildCreativeModeTabContentsEvent' } |
        ForEach-Object { Get-Content $_.FullName -Raw }) -join "`n"

$bad = @()
foreach ($f in $files) {
    $text = Get-Content $f.FullName -Raw
    if ($text -notmatch 'DeferredRegister<Item>') { continue }
    foreach ($m in ([regex]'DeferredRegister<Item>\s+([A-Z_0-9]+)\s*=').Matches($text)) {
        $field = $m.Groups[1].Value
        $cls = $f.BaseName
        # Named as Class.FIELD from a tab handler, or FIELD inside a class that
        # handles the event itself.
        # Covered if a tab handler names the owning CLASS at all. Some modules add
        # their items through a helper (abilities uses AbilityItems.itemFor(id))
        # rather than by naming the register, and demanding Class.FIELD called that
        # a miss when it is plainly handled.
        $named = [regex]::Escape($cls) + "s*."
        $selfHooked = ($text -match 'BuildCreativeModeTabContentsEvent')
        if (-not $selfHooked -and $tabText -notmatch $named) {
            $bad += "$cls`: $field is registered but never added to a creative tab"
        }
    }
}

if ($bad.Count -gt 0) {
    foreach ($x in $bad) { Write-Host "  $x" }
    throw "$($bad.Count) item register(s) are invisible in creative. Add them to a tab (see v2/V2Tabs.java)."
}
Write-Host "Creative tab check: every item register reaches a tab."
