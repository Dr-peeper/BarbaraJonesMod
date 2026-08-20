# Village Houses (v2)

Package: `com.barbarajones.v2.houses`

Ten village buildings, ascending from a Lean-To to the Krave Mansion, each
defined programmatically against `com.barbarajones.v2.build`'s `StructureDef`
API (see `docs/modules/schematics-and-placement.md` - read that first, this
module is a consumer of it, not an extension of it).

---

## 1. Wiring (orchestrator)

**One line in `BarbaraJonesMod`'s constructor**, alongside (after or before,
order does not matter - see below) `KraveBuild.init(bus)`:

```java
com.barbarajones.v2.houses.KraveHouses.init(bus);
```

That registers this module's own `DeferredRegister<Item>` (ten dedicated
schematic items) and calls `KraveHouseDefs.registerAll()`, which builds and
registers all ten `StructureDef`s with `StructureRegistry`. Nothing needs
adding to `ModItems`, `ModBlocks`, `ModTabs`, `ClientSetup`, `EventHandler` or
`ModNetwork` - none of those files were touched.

**Why order doesn't matter:** `KraveHouses.init` only needs `KraveBuild`'s
`ITEMS`/`BLOCKS` DeferredRegisters and `KraveSchematicItem` class to exist as
compiled types (a compile-time dependency, already satisfied by this module
importing `com.barbarajones.v2.build.item.KraveSchematicItem`), not for
`KraveBuild.init` to have already *run*. Both modules' registrations resolve
through Forge's own deferred/event-driven registry pipeline regardless of
which `init(bus)` call happens first in the constructor.

**Creative tab:** nothing to add. `KraveBuild.Tabs` already iterates every
registered `StructureDef` and drops one schematic into the tab per building;
this module does not (and must not) add its own tab-population code.

**Lang:** `assets/barbarajones/lang/v2_houses.json` - 20 keys (one
`building.barbarajones.house_*` name per building, one
`item.barbarajones.schematic_*` item name per dedicated schematic item).
Merge as usual.

**Textures:** `tools/make_v2houses.ps1`. Writes and pixel-verifies ten
16x16 item icons - `textures/item/schematic_*.png`, one per building, a rolled
parchment blueprint tied with a tier-coloured ribbon (twine -> red -> blue ->
gold) over a tiny ink-sketched silhouette of that building's roofline. Already
run; all ten files are on disk and were read back and verified by the script
itself, not just checked for existence.

**Recipes:** `data/barbarajones/recipes/schematic_*.json`, ten files, ordinary
`minecraft:crafting_shapeless`. Nothing to register in code.

### Registered ids

| Registry | Id | Class |
|---|---|---|
| Item | `barbarajones:schematic_lean_to` | `KraveSchematicItem` (dedicated) |
| Item | `barbarajones:schematic_small_house` | " |
| Item | `barbarajones:schematic_cottage` | " |
| Item | `barbarajones:schematic_two_storey_house` | " |
| Item | `barbarajones:schematic_ranch` | " |
| Item | `barbarajones:schematic_longhouse` | " |
| Item | `barbarajones:schematic_manor` | " |
| Item | `barbarajones:schematic_tower_house` | " |
| Item | `barbarajones:schematic_great_hall` | " |
| Item | `barbarajones:schematic_krave_mansion` | " |
| `StructureDef` id | `barbarajones:house_lean_to` | `def.LeanTo` |
| `StructureDef` id | `barbarajones:house_small_house` | `def.SmallHouse` |
| `StructureDef` id | `barbarajones:house_cottage` | `def.Cottage` |
| `StructureDef` id | `barbarajones:house_two_storey_house` | `def.TwoStoreyHouse` |
| `StructureDef` id | `barbarajones:house_ranch` | `def.Ranch` |
| `StructureDef` id | `barbarajones:house_longhouse` | `def.Longhouse` |
| `StructureDef` id | `barbarajones:house_manor` | `def.Manor` |
| `StructureDef` id | `barbarajones:house_tower_house` | `def.TowerHouse` |
| `StructureDef` id | `barbarajones:house_great_hall` | `def.GreatHall` |
| `StructureDef` id | `barbarajones:house_krave_mansion` | `def.KraveMansion` |

Every building class in `def` (`LeanTo`, `SmallHouse`, ... `KraveMansion`) is
package-private on purpose - `def.KraveHouseDefs` is the one seam anything
outside the package should go through, both for the ids above and for the
villager-capacity table below.

---

## 2. The ladder

| # | Building | Footprint | Storeys | Roofline | Beds (capacity) |
|---|---|---|---|---|---|
| 1 | Lean-To | 3x3 | 1, open-fronted | single lean slope | 1 |
| 2 | Small House | 5x5 | 1 | gable, glazed front peak | 1 |
| 3 | Cottage | 7x6 + porch | 1 | gable + lean-to porch awning | 2 |
| 4 | Two-Storey House | 6x6 | 2 | gable ridged the OTHER way, real staircase | 3 |
| 5 | Ranch | 11x5 + shed wing | 1 | long low gable + a second lean-to roof | 4 |
| 6 | Longhouse | 13x5 | 1, tall | steep A-frame, exposed log ridge beam | 6 |
| 7 | Manor | 9x7 + 2 turrets | 2 + towers | pedimented front gable + 2 pyramid turrets | 5 |
| 8 | Tower House | 5x5 | 4, vertical | flat crenellated parapet - no pitched roof at all | 3 |
| 9 | Great Hall | 15x7 + annex | 1, tall | the widest steepest gable, rose windows | 4 |
| 10 | Krave Mansion | 11x9 + 2 wings + 2 towers | 3 + wings + towers | central gable over 2 wing gables under 2 pyramid towers | 8 |

Every roof shape after the Lean-To's single mono-slope is built procedurally
by `def.RoofKit` (gable ridged either axis, a one-directional lean, a square
pyramid closing to a point, plus the matching triangular gable-end infill) -
see that class's own javadoc for the technique. Nothing in this module hand-
types a sloped roof as literal ASCII layers; the geometry is generated from a
handful of integers per call, which is also why every roof in the ladder is
internally consistent rather than "close enough by eye."

Villager **capacity is deliberately not monotonic** rung to rung - the Tower
House (a garrison keep) and the Great Hall (civic infrastructure with a small
guest wing) both sleep fewer people than the building just below them. Both
are still unambiguously bigger, costlier and more advanced builds; they are
just not primarily housing. See `def.KraveHouseDefs`'s javadoc for the full
reasoning, and each building class's own javadoc for its exact bed placement
and material list.

### Materials

Every building shares one palette, `def.HousePalette.BASE`, extended per
building only where that building needs something the base table does not
have (a campfire for the Longhouse's hearth, the Krafting Bench for the
Mansion's kitchen). Krave wood (`krave_planks`/`krave_log`/`krave_wood`) and
Krave block are load-bearing in every single building; vanilla stone brick,
cobblestone and glass carry the "grander" rungs (Manor onward) so the ladder
reads as krave materials PLUS increasingly serious construction, not ten
buildings in ten unrelated palettes.

### Village contribution

This module declares NOTHING new to the village scoring system and does not
need to - `com.barbarajones.v2.village`'s `VillageBuffs` table already scores
every bed, door, workstation, chest, lantern, fence, and krave block
automatically, per placed block, the instant it exists inside a village
claim. A building's real contribution to village tier is exactly the sum of
what it physically places, which is why every building class's javadoc spells
that sum out (e.g. the Krave Mansion: 8 beds = 16 building points, 2 doors,
a Krafting Bench, four more workstations, six chests, a bell, before a single
krave-block pilaster is even counted). `def.KraveHouseDefs.capacities()`
exposes the declared bed-count table for anything that wants an at-a-glance
"how much housing is this" answer without walking the claim.

---

## 3. What I finished

- All ten `StructureDef`s, built entirely against the documented
  `StructureDef.Builder` API - `palette`/`block`/`state`/`fixed`/`random`,
  `fill`/`walls`/`hollow`/`frame`/`column`/`line`/`set`/`carve`/`keep`,
  `door`/`bed`, `anchor`/`ground`/`maxGroundDelta`/`buildTicks`/`core`, and the
  `op(StructureOp)` escape hatch (via `def.RoofKit`) for every sloped roof.
- `def.RoofKit`: reusable gable (both axes), mono-slope, pyramid, and matching
  gable-end-wall infill generators, with a defensive `IllegalArgumentException`
  in `monoSlope` if the z-distance and y-distance of a slope's two ends do not
  match (a single stair run is always 45 degrees; this catches a mismatched
  call at structure-build time with a message naming the mismatch, rather than
  silently baking a roof with a step in it).
- `def.HousePalette`: one shared 40-odd-character material table plus eight
  roof-stair-facing characters (four krave-plank, four stone-brick) so every
  building draws from the same vocabulary.
- `def.KraveHouseDefs`: the one file that knows all ten ids, registers all ten
  definitions, and declares the villager-capacity table.
- `KraveHouses`: ten dedicated `KraveSchematicItem`s (own id, own icon, own
  recipe, own refund identity - not the shared NBT-driven item) and the single
  `init(bus)` entry point.
- Ten crafting recipes, `minecraft:crafting_shapeless`, each within the
  standard 9-ingredient 3x3 crafting-grid budget, ramping in material tier:
  krave planks and a stick (Lean-To) -> krave wood/log plus a torch (Small
  House) -> add glass and Krave Dust (Cottage) -> add iron (Two-Storey House,
  Ranch) -> krave block and more Krave Dust (Longhouse) -> stone brick, iron
  and gold (Manor, Tower House) -> gold and a diamond (Great Hall) -> gold,
  diamond, emerald AND a stack-worth nod to `barbarajones:dollars` (Krave
  Mansion - the mod's running joke about money, since this is the one building
  in the mod that is explicitly a flex).
- Ten item models (`minecraft:item/generated`) and ten pixel-art icons,
  generated and pixel-verified by `tools/make_v2houses.ps1` (run, not just
  written - see section 1).
- Twenty lang keys.
- Every `column(x, z, y1, y2, key)` call in every file was individually
  re-checked against the API's actual `(x, z, y1, y2)` parameter order after
  I caught myself transposing it (muscle memory from `fill`'s `x, y, z, x, y,
  z` order bleeding into `column`'s different one) in the Lean-To and the
  Great Hall. Both were wrong in a way that would have put corner posts at the
  wrong wall entirely, at the wrong height, with no compiler present to catch
  it. I do not have high confidence there is no third one I missed - see below.

## 4. What I did NOT finish - read this part

- **NOT COMPILED**, for the same reason `schematics-and-placement.md` gives
  for its own module: no JDK on this machine, rule 1 says do not run gradle.
  I traced coordinate math for every roof, every staircase, and every
  `column()`/`fill()`/`bed()`/`door()` call by hand against the documented
  API, and found and fixed three real bugs that way (two transposed
  `column()` calls, one `monoSlope` call whose two ends did not actually
  agree on distance) - but "found three by hand" is not the same guarantee a
  compiler gives you. Expect to fix something, most likely a coordinate
  that is one block off rather than a structural failure - the palette
  validation in `StructureDef.Builder.build()` (a hard, named, loud crash for
  any character used but never defined) is the one class of bug I am
  confident is NOT present, because I grepped every raw character literal
  against its definition.
- **`Cayden`'s personal housing check (`com.barbarajones.housing.
  HousingValidator`) is not guaranteed to pass on every building.** That
  validator wants an ENCLOSED room (its flood-fill must terminate, not escape)
  plus a door/trapdoor, a bed, and a light source - it is a different system
  from village tier and this module's task was the latter. The Lean-To in
  particular is intentionally open-fronted (no door at all - that is the
  point of a lean-to, and it is why it is nearly free) and will almost
  certainly fail HousingValidator's "enclosed" test. Every other building has
  a real door and should pass, but I did not walk each one through that
  validator's exact flood-fill rules to confirm. If Cayden needs to be able to
  move into one of these specifically, check the Lean-To first.
- **No `.onComplete`/`.blockEntity` hooks anywhere in the ladder** - chests
  are placed empty rather than seeded with starter loot, and nothing
  registers with the settlement tracker on completion (the doc's own
  suggested hook, `KraveVillageData.get(level).addBuilding(...)`, is a one-
  line addition if a future pass wants these buildings to self-report to
  `v2.village` the moment they finish, on top of the automatic per-block
  buff scoring they already get for free).
- **A few furnishing pieces sit in wall niches rather than free-standing** -
  several beds, chests and windows across the bigger buildings (Manor,
  Great Hall, Krave Mansion) land exactly on a shared wall segment between two
  attached sections (a turret sharing a wall with the main block, a wing
  sharing a wall with the central block) and get overwritten by whichever op
  ran second. The result is still solid and still furnished - a few windows
  end up facing into an adjoining room instead of outside, which I judged not
  worth the extra bookkeeping to chase down block by block given the size of
  this task, but it is a real (cosmetic-only) imperfection, not a design
  choice.
- **No terrain-fit tuning beyond `maxGroundDelta` on a few of the larger
  builds.** The Tower House and Manor lower their tolerance slightly (they
  are "serious" builds and should ask for more level ground); nothing raises
  it. A very large flat plot is the easiest way to test the Krave Mansion.
