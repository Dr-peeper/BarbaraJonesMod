# The Krave Manual, 2.0

Package: `com.barbarajones.v2.manual`
Java entry point:

```java
com.barbarajones.v2.manual.ManualModule.init(bus);
```

**That call is a documented no-op.** This module registers zero new items,
blocks, or entities - see `ManualModule`'s own javadoc. The manual is still
the exact same `barbarajones:krave_manual` item it always was. Calling
`init(bus)` is harmless and free; not calling it changes nothing. It exists
purely so this module has the same one-entry-point shape the task's rules ask
every module to expose.

## What this task actually was

Rewrite the Krave Manual - "both its content and how it looks" - to cover all
of 2.0, in an in-game book UI with chapters, a contents page, navigation,
search, item/block-render illustrations, and real crafting-grid recipe
displays, instead of the old plain button-list `Screen` with hand-typed prose.
Nothing here adds a new mechanic to the mod; it documents the mechanics eleven
other modules already built.

## The two files this module does not own, and left alone

Two call sites are fixed by name and could not move:

- `content.ModItems` (forbidden to edit) constructs
  `new com.barbarajones.item.KraveManualItem(props().stacksTo(1))` by exact
  type. `item/KraveManualItem.java` therefore keeps its exact class name,
  package, and constructor signature - only its tooltip text changed (two
  lines instead of one, mentioning search).
- `client.ClientPacketHandler#openManual()` (not forbidden, but not this
  module's file to rename) does
  `Minecraft.getInstance().setScreen(new com.barbarajones.client.KraveManualScreen())`.
  `client/KraveManualScreen.java` is now a two-line subclass of this module's
  real screen: `public class KraveManualScreen extends
  com.barbarajones.v2.manual.client.ManualScreen`. Every actual line of 2.0
  manual content and UI lives in `v2.manual`, as the task asked.

Nothing else needed touching. `tools/make_manual.ps1` (the icon/model/recipe/
lang generator for the `krave_manual` item itself) was **not** re-run or
edited - it already did its job correctly and this module does not duplicate
its output.

## What this module built

All under `com.barbarajones.v2.manual`:

| package | what's there |
|---|---|
| `book` | The content model and the book itself. `PageElement` is a sealed interface of pure-data element types (heading, paragraph, bullets, numbered steps, divider, gallery, callout, table, a real 3x3 crafting grid, and a non-grid "flow" recipe for interactions like feeding/smelting/machines). `ManualChapter` is a flat `List<PageElement>` - deliberately **not** pre-split into pages; see `PageElement`'s javadoc for why. `ManualBook` holds all eleven chapters as data. `SearchIndex` is a plain substring search over every chapter's text. `Icon` resolves an `ItemStack` lazily and defensively (never throws, falls back to a barrier icon) - see its own javadoc for why that mattered while sibling modules were still mid-build. |
| `client` | `ManualRenderer` measures and draws one `PageElement` at a time (shared by both passes, so measurement and drawing can never disagree). `Paginator` greedily slices a chapter's flat element list into screen-sized pages at the *current* window size - resize the game and it re-flows correctly instead of clipping. `ManualScreen` is the actual `Screen`: title bar, a persistent chapter rail (contents/11 chapters/search), the reading pane (paginated chapter content, or the contents overview, or live search results), page-turn navigation with a real slide animation and the vanilla book-page-turn sound, and item-tooltip-on-hover for every illustration and every recipe-grid slot. |

Visual language is deliberately borrowed wholesale from
`client.ui.KraveTheme` (the same cereal-box look Cayden's Ascension screen,
the quest tree, and the village trade screen already use) rather than
inventing a second style - the manual reads as part of the mod, not a wiki
bolted on beside it. Two new textures back the book-specific parts:
`textures/gui/manual/manual_paper.png` (a tileable 32x32 parchment
background for the reading pane) and `textures/gui/manual/manual_cover.png`
(the 200x40 open-book banner on the contents page), both generated and
verified by `tools/make_manual_book.ps1` (System.Drawing, same idiom as every
other `make_*.ps1`, fixed RNG seed, re-reads and checks decoded pixel size of
every file it writes):

```
OK    .../manual_cover.png  (200x40)
OK    .../manual_paper.png  (32x32)
```

Chapter icons and every illustration/recipe-grid slot in the book are real
`ItemStack` renders (spawn eggs stand in for the handful of entities with no
held item of their own - Cayden, Barbara, the three bosses) - there is no
hand-painted art anywhere in the book, per the task brief.

## Lang

**No new lang file.** This module registers no new item/block/entity/GUI-
string ids that need one - every string in the book is drawn as a plain
literal, matching the existing convention `client.ui.KraveTheme`,
`client.ui.KraveScreen`, and the *original* `KraveManualScreen` all already
used for their own GUI chrome and body text (translation keys are used in
this codebase for registered content's display names, not for hand-authored
screen prose). The one real translation key involved,
`item.barbarajones.krave_manual`, already exists in `en_us.json` from
`tools/make_manual.ps1`'s earlier run and was not touched.

## Content sourcing - what was actually read, not assumed

Every mechanical claim in `ManualBook.java` was checked against current
source immediately before being written, listed per chapter:

1. **Rule #1** - `entity/CaydenCobb.java` (death/respawn/grace, housing claim,
   ascension gate on death), `apocalypse/KraveApocalypse.java` (the ten
   stages), `housing/HousingValidator.java` (the six housing constants,
   confirmed unchanged: `MIN_VOLUME=30`, `MIN_LIGHT=8`).
2. **Cravelings** - `v2/mobs/ModMobEntities.java`, `ModMobItems.java`,
   `ModMobBlocks.java`, and all four `entity/ai/*Goal.java` files for each
   mob's signature move. **Honest gap, flagged in the chapter itself:** the
   five entity classes those files reference
   (`CravelingEntity`/`KrispboneEntity`/`LoomweaverEntity`/`SoggyEntity`/
   `MascotEntity`) did not exist anywhere in the repo as Java source at the
   time this was written, and no loot table for any of them exists under
   `data/barbarajones/loot_tables/`. The chapter is accurate to the goal
   files' own documented design and to the two items (`Krave Shard`,
   `Cereal Mascot Head`) that *are* registered, and says so plainly instead
   of inventing stats or drop rates. **Needs a second pass once
   `v2.mobs` compiles and ships real loot tables** - illustrations currently
   use vanilla placeholder item renders, explicitly labelled as such.
3. **Krave tiers** - `data/barbarajones/recipes/*.json` read directly for
   every crafting grid shown (`krave_cereal`, `krave_box`, `cereal_bowl`,
   `krave_milk`, `golden_krave`, `rich_krave`, `krave_syrup`,
   `krave_mortar`, `cayden_compass`), not guessed. `v2/economy/KraveEconomy.java`
   and its own module doc (`docs/modules/krave-economy.md`) for what each
   tier is *for*.
4. **Building the village** - `v2/village/VillageTier.java` (the six tiers
   and their real thresholds) and `v2/village/VillageBuffs.java` (every
   block that actually contributes, read directly rather than guessed).
   **Honest gap:** the task asked for "schematics and the ten buildings."
   `v2/build/def/StructureRegistry.java` is a real, working framework for
   registering building blueprints, and `quest.expansion.QuestReward` has a
   `SCHEMATIC` reward kind wired up to grant them - but a repo-wide search
   turned up **zero calls to `StructureRegistry.register(...)`** and **zero**
   files under `data/barbarajones/quests/`. There is no list of ten
   buildings to print, because nothing has registered any yet. The chapter
   says this plainly and documents the organic block-based growth system
   that works today instead. **Needs a second pass** the moment real
   schematics land - it will likely deserve its own dedicated section rather
   than the honest-gap callout it has now.
5. **Villagers and trading** - `v2/bonds/VillagerKraveBond.java`,
   `BondLevel.java`, `FeedingBondEvents.java`, `VillageHouseFinder.java` -
   all read directly for the exact discount/restock numbers and thresholds.
6. **Automation** - `v2/machines/MachineKind.java` (the seven machine kinds
   and slot layout), `blockentity/MachineProcesses.java` (Plantation/Depot/
   recipe-driven behaviour), `KraveFuels.java` (syrup unit economy).
   **Honest gap:** `v2.machines.KraveMachines` - the class
   `MachineProcesses.java` and `KraveFuels.java` both import and call
   (`KraveMachines.recipeTypeFor(...)`, `KraveMachines.KRAVE_SYRUP`) - **does
   not exist anywhere in the repo**, exactly as `docs/modules/krave-economy.md`
   already flagged from an earlier pass. This means the whole machines
   package currently fails to compile on its own. The chapter is written
   against the finished design in the code that *does* exist (it is
   internally coherent and clearly intentional design, not a guess) and says
   so at the top rather than pretending the machines are reachable in the
   current build. **Needs a second pass the moment `KraveMachines.java`
   lands** - re-verify the chain against the real screens/block textures once
   it compiles.
7. **Cayden: feeding, ascension, breeding** - `progression/AscensionLadder.java`
   read in full for every rung's exact cost/effect numbers,
   `entity/CaydenCobb.java` for feeding tiers and the free-ascension
   triggers. **Honest gap, and the single most important one in this whole
   book:** `CaydenCobb.getBreedOffspring(...)` returns `null` - there is no
   working breeding. A real, craftable Krave Family Size box exists, a real
   `BredCaydenCobb` entity *class* exists and is explicitly written to be
   exempt from the "exactly one Cayden" enforcement in `v2.bonds.CanonicalGuardEvents`
   - but `BredCaydenCobb` **has no registered `EntityType` anywhere**, and
   nothing anywhere calls its constructor. The chapter documents this
   loudly rather than promising a feature that does not fire in this build.
   `docs/modules/krave-economy.md`'s existing flag about `KI_PER_RICH_KRAVE`
   balance is unrelated and still stands separately.
8. **Barbara and her moves** - `entity/barbara/SmokeAbility.java` (all eight
   moves, exact costs/cooldowns and the high-Barbara discount),
   `BarbaraCombat.java` for the stash-as-ammunition framing.
9. **The bosses** - `entity/KraveMonster.java` (100 HP, jump/teleport,
   confirmed unchanged from the 1.x manual), `boss/manager/TheManager.java`
   and `boss/mom/MomCobbBoss.java`/`MomPhase.java` (all three phases per
   boss, drop tables read from `dropCustomDeathLoot`),
   `v2/internet/InternetManagerBoss.java` (all three phases, the buffering-
   ring break threshold, the interrupt-damage threshold). **Honest gap:**
   both `TheManager` and `MomCobbBoss` are registered `EntityType`s with
   spawn eggs in `content.ModItems`, but a repo-wide search found **no
   quest, item, structure, or apocalypse-stage code that spawns either one**
   as a real, fightable entity - only `cinematic.actor.ManagerActor`, a
   *visual-only* stand-in used by the apocalypse cutscene at stage 7, which
   is not the same class. Since killing each one is what unlocks the Laser
   Lens and Ascension Charm abilities respectively (chapter 10), this gap is
   flagged in **three** places in the book (the boss chapter twice, the
   abilities chapter once) rather than buried once. The Internet Manager, by
   contrast, is fully reachable today (Service Call Box + Rotary Phone) and
   is documented as the one boss in the chapter you can actually start
   without creative mode.
10. **Player abilities** - `abilities/AbilityId.java` (exact cooldowns),
    `abilities/AbilityUnlocks.java` (exact unlock gate per ability, quest or
    boss kill), the six `item/*Item.java` class javadocs for what each one
    actually does mechanically.
11. **The Krave dimension** - `block/KraveDoorBlock.java` read in full for
    the portal frame shape, the free-Tether guarantee, companion escort, and
    the landing search; `dimension/KraveDenBuilder.java` and
    `apocalypse/KraveKosmosAmbience.java` for the den and ambient Kosmonaut
    spawning. `v2/village/VillageTier.java`'s `PORTAL_TIER` gate is cited
    accurately (Village tier, index 3) via `VillageTier`, not guessed.

## Honest gaps - what this module did NOT finish

Being straightforward per the module-doc rule:

- **The Craveling family (chapter 2) is written against goal-file design,
  not a working build** - the entity classes and loot tables do not exist
  in the repo yet. See item 2 above. Illustrations for that chapter use
  vanilla placeholder items and say so on the page.
- **Village schematics and "the ten buildings" (chapter 4) do not exist as
  registered content** - the framework is real, nothing is registered on it.
  See item 4 above.
- **The automation chapter (6) is accurate to unreachable code** - the
  machines package has a compile-breaking missing class
  (`v2.machines.KraveMachines`) that predates this module and is already
  flagged in `docs/modules/krave-economy.md`. See item 6 above.
- **Cayden breeding (chapter 7) does not work** - loudly documented rather
  than glossed over. See item 7 above.
- **The Manager and Mom Cobb (chapters 9 and 10) have no natural summon
  trigger** - spawn-egg only in this build. See item 9 above.
- **No JEI/recipe-book cross-check was done** - this module only reads
  `data/barbarajones/recipes/*.json` directly for the grids it draws; it did
  not verify how any other recipe-viewing UI in the mod surfaces the same
  recipes, if at all.
- **No automated screenshot/visual test exists for the new screen** - it was
  authored and pagination-checked by inspection (measure/render share one
  code path per element on purpose, specifically so they cannot silently
  disagree - see `ManualRenderer`'s javadoc) but was not run inside a live
  client as part of this pass, per the "do not run Gradle" rule. Whoever
  next has a running client should open the manual once and confirm the
  rail, contents page, chapter pagination, search, and recipe-grid tooltips
  all render as designed.
- **Small window sizes are not scissored on the chapter rail** - at a very
  small screen height the rail's thirteen rows could in principle run past
  the panel's bottom border before the footer. Not expected to matter at any
  realistic play resolution; flagged rather than silently accepted.
