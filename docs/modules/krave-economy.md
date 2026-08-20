# Krave Crafting Economy

Package: `com.barbarajones.v2.economy`
Java entry point (call exactly once, alongside the other `*.init(bus)`/`*.register(bus)` calls in `BarbaraJonesMod`'s constructor):

```java
com.barbarajones.v2.economy.KraveEconomy.init(bus);
```

That one call registers everything this module owns: its `Item`s, its `Block`,
and its own creative tab. There is nothing else the orchestrator needs to call.

## The loop this module builds

> kill Cravelings -> pick up Cocoa Beans (+ maybe a Krave box) -> craft Cocoa
> Beans + Wheat/Sugar into **Krave** (Krave Cereal) -> grind spare Cocoa Beans
> into **Krave Dust** by hand at a **Krave Mortar** -> upgrade Krave Dust +
> Krave + Krave Milk into **Rich Krave** -> spend Krave/Rich Krave/Golden
> Krave on Cayden, and Krave Dust/Krave Syrup on whatever machines, armour,
> and abilities modules build next.

Nothing above requires a wiki: every new item this module adds carries a
plain-English tooltip or in-world chat/actionbar line explaining what to do
with it (see "UX, no wiki required" below). No advancements are registered
here on purpose - the quest module owns progression tracking.

**No mob module exists yet.** This module assumes a "Craveling" (or whatever
the mob module ends up calling it) will drop `minecraft:cocoa_beans` (and,
rarely, `barbarajones:krave_cereal` or `barbarajones:krave_dust`) on death.
No such mob or loot table exists anywhere in the repo as of this writing -
that is someone else's module, not built here.

## What already existed and was deliberately left alone

Read, never edited, from this module:

| id | where it lives | role in this economy |
|---|---|---|
| `barbarajones:krave_cereal` | `content.ModItems.KRAVE_CEREAL` | **"plain Krave."** The base food/Cayden-feed tier. Already craftable (`recipes/krave_cereal.json`: 2 wheat + 1 sugar + 1 cocoa bean -> 2 Krave Cereal) and already wired into `CaydenCobb.mobInteract`. |
| `barbarajones:krave_dust` | `content.ModItems.KRAVE_DUST` | **Krave Dust**, the intermediate this task asked this module to "register, texture and document." It was already registered (plain item, stack 64) with **no texture and no cocoa-bean source** - see "Bugs found and fixed" below. |
| `barbarajones:krave_milk` | `content.ModItems.KRAVE_MILK` | Minor food, already craftable from a milk bucket + Krave Cereal. Used here as a Rich Krave ingredient. |
| `barbarajones:golden_krave` | `content.ModItems.GOLDEN_KRAVE` (+ `GoldenKraveItem`) | **"Golden Krave," the top tier.** Already craftable (8 gold ingots + 1 Krave Cereal) and already wired into `CaydenCobb.mobInteract` as the strongest feed (full heal, 30 Ki). This module does not touch it or its recipe.
| `barbarajones:krave_ore` / `deepslate_krave_ore` | `block.krave.KraveOre` | Mines to Krave Dust by design (see its own javadoc) - see "Bugs found and fixed."

Because plain Krave and Golden Krave already existed, fully wired to Cayden,
this module's job narrowed to exactly what was missing: the **Rich Krave**
mid-tier, **Krave Syrup** as a new intermediate, and the **Krave Mortar**.

## What this module registers

All in `com.barbarajones.v2.economy.KraveEconomy` unless noted.

| id | java field | type | notes |
|---|---|---|---|
| `barbarajones:rich_krave` | `KraveEconomy.RICH_KRAVE` | `Item` (food) | New mid-tier Krave. Nutrition 6 / saturation 0.6, Speed (20s) + Regeneration (10s), no `alwaysEat`. No Hunger-effect gag unlike plain Krave - "richer" reads as "less junk food." |
| `barbarajones:krave_syrup` | `KraveEconomy.KRAVE_SYRUP` | `Item` (`KraveSyrupItem`, `v2.economy.item`) | **New intermediate material.** Drinks like a Honey Bottle (32-tick drink, hands back `minecraft:glass_bottle`). Minor food (nutrition 3 / saturation 0.5 + short Regen) so it isn't a dead end even before another module consumes it. **This is the id the machines/armour/abilities modules should build on.** |
| `barbarajones:krave_mortar` (block) | `KraveEconomy.KRAVE_MORTAR` | `Block` (`KraveMortarBlock`, `v2.economy.block`) | The manual grinder. See below. |
| `barbarajones:krave_mortar` (item) | `KraveEconomy.KRAVE_MORTAR_ITEM` | `BlockItem` (`KraveMortarBlockItem`, `v2.economy.item`) | Carries its own tooltip explaining how to use the block. |
| `barbarajones:krave_economy` (creative tab) | `KraveEconomy.TAB` | `CreativeModeTab` | New tab so the three items above (plus Krave Dust/Cereal/Golden Krave for convenience) are reachable in creative without waiting on `ModTabs.java` to be updated centrally. The orchestrator may fold these into the main tab instead - either is fine, this was just to keep the module self-contained. |

**`barbarajones:krave_dust` is *not* re-registered here** - it already exists
in `content.ModItems.KRAVE_DUST` and this module only adds a texture, a model,
new recipes/loot sources for it, and documents it as the other canonical
intermediate id alongside Krave Syrup.

### Ids other modules should build on

- **`barbarajones:krave_dust`** (`com.barbarajones.content.ModItems.KRAVE_DUST`) - ground cocoa, cheap and stackable.
- **`barbarajones:krave_syrup`** (`com.barbarajones.v2.economy.KraveEconomy.KRAVE_SYRUP`) - refined/concentrated, one craft per unit, the "worth automating" material.

## The Krave Mortar

`com.barbarajones.v2.economy.block.KraveMortarBlock`. A plain `Block`, no
block entity, no GUI - deliberately the "by hand, before automation" step the
task asked for.

**Interaction:** right-click it while holding Cocoa Beans. The whole stack is
ground in pairs: `COCOA_PER_DUST = 2` Cocoa Beans -> 1 Krave Dust, all at
once (so a stack of 11 cocoa beans grinds 5 Krave Dust and leaves 1 cocoa bean
in hand). Plays two layered vanilla sounds (`GRAVEL_HIT` + `CROP_BREAK`),
spawns a few `CRIT` particles, and prints an actionbar line (`+N Krave Dust`,
or a "needs at least 2" hint if you don't have enough) every time - that
actionbar line **is** the tutorial; no wiki, no advancement needed.

The 2:1 ratio is deliberately worse than a straight 1:1 - it should always
feel worth building whatever automated grinder the machines module adds
later. Crafted from 6 cobblestone in a mortar-and-pestle shape
(`recipes/krave_mortar.json`), so it's reachable in the first few minutes of
a new world.

Model: five-element hand-built block model (`models/block/krave_mortar.json`
+ `blockstates/krave_mortar.json`) - a stone bowl on a base, not a flat cube,
so it visually reads as a mortar rather than a mystery block.

## Recipes added (`data/barbarajones/recipes/`)

All new files; none of them touch or redefine an existing recipe JSON.

| file | recipe | rationale |
|---|---|---|
| `rich_krave.json` | Krave Cereal + Krave Dust + Krave Milk -> **2** Rich Krave | The upgrade path once you have any dust and have bothered to make milk. Costs roughly 2 Krave Cereal worth of inputs (1 direct + 1 via the milk recipe) plus 1 dust, for 2 Rich Krave back - a fair trade for a strictly-better food/feed item. |
| `krave_syrup.json` | Glass Bottle + 2 Krave Dust + Sugar -> 1 Krave Syrup | Mirrors the existing `mr_pibb.json` shape (bottle + sugar + a Krave-line ingredient). Deliberately 1-for-1 (no bulk output) since this is meant to be the "valuable, worth automating" material for later modules. |
| `rich_krave_from_syrup.json` | Krave Cereal + Krave Syrup -> **2** Rich Krave | A second path to Rich Krave once you have Syrup surplus, so Syrup isn't a complete dead-end for a player who never automates. |
| `krave_mortar.json` | 6 Cobblestone (mortar-and-pestle shape) -> 1 Krave Mortar | Cheap, stone-tier, reachable well inside the first ten minutes. |

**Intentionally left untouched:** `krave_cereal.json` (2 wheat + 1 sugar + 1
cocoa -> 2 Krave Cereal - already a good "leftover cocoa is useful" rate),
`krave_dust.json` (Krave Cereal -> 2 Krave Dust, an existing storage-style
recipe), `krave_milk.json`, `golden_krave.json`, `off_brand_krave.json`,
`krave_ore_cereal_from_dust.json`. Re-defining any of these would be editing
another agent's/an earlier pass's work for no reason.

## Cayden feed tiers (edit to a **non-forbidden** shared file)

`content/ModItems.java`, `content/ModBlocks.java` etc. are off-limits, but
`src/main/java/com/barbarajones/entity/CaydenCobb.java` is not on that list,
and the task explicitly asked for the tiered line to work "as Cayden feed."
**This module made one small, additive edit to `CaydenCobb.java`:**

- Added one more `mobInteract` branch, ordered between the existing Golden
  Krave and Krave Cereal checks, that fires on
  `barbarajones.v2.economy.KraveEconomy.RICH_KRAVE`.
- Added one new private method, `feedRichKrave(Player)`, mirroring the
  existing `feedGoldenKrave`/`feedKrave` methods: `+7` HP heal, `+5` Ki
  (a new local constant `KI_PER_RICH_KRAVE`, not touching
  `AscensionLadder.java`), a happy-villager particle burst, and a chat line.
- Nothing else in that file was touched. Full tier order in
  `mobInteract`: Golden Krave (existing) -> **Rich Krave (new)** -> Krave
  Cereal (existing) -> empty hand / claim-home (existing).

If whoever owns Cayden's AI wants a different balance for `KI_PER_RICH_KRAVE`
or the heal amount, both are single literals right next to `feedRichKrave`.

## Bugs found and fixed along the way

Being honest per the module-doc rule - these weren't asked for, but they were
directly blocking "leftover cocoa is genuinely useful" and "Krave Dust is a
real material," so they were fixed as pure data (no shared Java files
touched):

1. **`barbarajones:krave_dust` texture/model - checked, not a bug.** It
   already had both (`textures/item/krave_dust.png`,
   `models/item/krave_dust.json`) from an earlier pass. Noted here only
   because the task asked this module to confirm Krave Dust is textured, and
   it was verified rather than assumed.
2. **Krave Ore had no loot table.** `block.krave.KraveOre`'s own javadoc says
   "a crystallised sugar seam that drops Krave Dust," but neither
   `krave_ore.json` nor `deepslate_krave_ore.json` existed under
   `data/barbarajones/loot_tables/blocks/` - so mining it dropped **nothing**.
   Added both, standard vanilla-ore-with-fortune shape (silk touch drops the
   ore block itself; otherwise 2-3 Krave Dust, scaling with Fortune, one more
   on the deepslate variant). This is squarely inside "Krave Dust is a real
   crafting material," so it was fixed rather than just reported.
3. **`barbarajones:krave_mortar` needed its own loot table too**, added
   alongside it (`loot_tables/blocks/krave_mortar.json`) - a `Block` with no
   loot table drops nothing when broken, which is the exact trap #2 above
   fell into; didn't want to ship the same bug twice.

## Textures

`tools/make_economy.ps1` (System.Drawing, same idiom as the other
`make_*.ps1` scripts). Writes and verifies (re-reads each PNG and checks its
decoded width/height):

- `textures/item/rich_krave.png` - a carton in the same visual language as
  `golden_krave`/`stale_krave`/`off_brand_krave` (see `make_extra_content.ps1`'s
  `Carton()`), dark chocolate + bronze trim, two sparkles instead of golden's
  thirty so the tier reads at a glance.
- `textures/item/krave_syrup.png` - a Honey-Bottle-shaped silhouette filled
  with dark amber syrup instead of gold, so it reads as "Krave's own," not a
  Honey reskin.
- `textures/block/krave_mortar.png` - speckled stone with a darker carved-bowl
  vignette and scattered cocoa-dust stains, tiled across all faces of the
  hand-built block model.

Run once already; all three files verified present and 16x16 (script output
captured below, re-running is safe/idempotent - fixed RNG seed):

```
OK  textures/item/rich_krave.png   (16x16)
OK  textures/item/krave_syrup.png  (16x16)
OK  textures/block/krave_mortar.png (16x16)
```

## Lang

`assets/barbarajones/lang/krave_economy.json` (own file, flat, same shape as
`en_us.json`, for the orchestrator to merge):

```json
{
  "item.barbarajones.rich_krave": "Rich Krave",
  "item.barbarajones.krave_syrup": "Krave Syrup",
  "block.barbarajones.krave_mortar": "Krave Mortar"
}
```

(The mortar's `BlockItem` inherits its display name from the block's
`block.barbarajones.krave_mortar` key - vanilla `BlockItem` behaviour - so no
separate `item.barbarajones.krave_mortar` key is needed.)

## Balance reasoning: a house inside ~20 minutes

Starting from nothing:

1. Punch/gather wood, get a crafting table + basic tools - unchanged, vanilla pace.
2. Kill a couple of Cravelings (once that mob exists) or find wild cocoa pods:
   a handful of Cocoa Beans is enough for several Krave Cereal
   (`krave_cereal.json` is already 1 cocoa bean per 2 cereal - cheap).
3. Eat Krave Cereal to stay fed while building - it is a real food (nutrition
   4) with a Speed buff, so early building/gathering is faster, not slower.
4. Any cocoa beans beyond what's needed for cereal are not wasted: 6
   cobblestone -> Krave Mortar (a five-second craft once you have stone
   tools), then grind the surplus into Krave Dust on the spot - no recipe
   lookup, no wiki, the tooltip and the actionbar feedback say exactly what
   to do.
5. Krave Dust either goes straight into Off-Brand Krave / Krave Syrup, or
   gets banked - it never sits as a dead-end item the way it did before this
   module (no texture, no cocoa source).

None of this is gated behind a grind: the whole chain above is 2-4 short
crafts plus normal vanilla wood/stone gathering, which comfortably fits
inside a 20-minute "first house" window alongside actually placing blocks.
Rich Krave and Krave Syrup are intentionally NOT on the critical path to a
first house - they are the next step up, for once the player has spare cocoa
and a little milk/sugar to work with.

## Honest gaps - what this module did NOT finish

- **No Craveling mob exists yet.** The "kill Cravelings -> get cocoa and
  Krave" half of the loop has nothing to kill. This module cannot create that
  mob (out of scope) and only assumes a `minecraft:cocoa_beans` drop.
- **Schematics and village upgrades are not built here.** The task's loop
  description mentions spending Krave on schematics and village upgrades;
  those are other modules' systems. This module only makes sure Krave/Rich
  Krave/Krave Dust/Krave Syrup exist as clean, documented ids for those
  systems to spend.
- **Cross-module id collision risk, flagged but not fixed here:**
  `src/main/java/com/barbarajones/v2/machines/KraveFuels.java` already
  references a `com.barbarajones.v2.machines.KraveMachines.KRAVE_SYRUP`
  field. **`KraveMachines.java` does not exist anywhere in the repo yet**
  (checked via repo-wide search). If/when it's created and registers its own
  `krave_syrup` item, that is a duplicate-registration crash waiting to
  happen against this module's `barbarajones:krave_syrup`. A background task
  was spawned (via `spawn_task`, title "Fix KraveFuels dangling ref to
  undefined KraveMachines.KRAVE_SYRUP") flagging that `KraveFuels`/
  `KraveMachines` should reference `KraveEconomy.KRAVE_SYRUP` instead of
  registering their own. Whoever picks up the machines module should read
  that flag (or this section) before writing `KraveMachines.java`.
- **No custom sounds were registered** (`content/ModSounds.java` is
  forbidden) - the mortar reuses two vanilla `SoundEvents`
  (`GRAVEL_HIT` + `CROP_BREAK`) already used elsewhere in this codebase.
  A dedicated "grind" sound would read better; that's a nice-to-have for
  whoever next has access to `ModSounds.java`.
- **`ModTabs.java` was not updated** (forbidden). This module's items are
  reachable via its own new "Krave Economy" creative tab instead. If the
  orchestrator would rather have them in the single main tab, that's a
  one-line addition to `ModTabs.MAIN`'s `displayItems` once someone with
  write access to that file is available.
- **No JEI/recipe-book integration checked** - this module only wrote data
  JSON; it did not verify how `RecipeBookItem`/`KraveManualItem` surface these
  new recipes, if at all.
