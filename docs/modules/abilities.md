# Player Abilities (`com.barbarajones.v2.abilities`)

Lets the player earn and use the same powers Cayden's ascension ladder
(`progression/AscensionLadder.java`) unlocks for himself: the knockback
flash, the red Krave laser, flight bursts, the apocalypse's meteor, Ultra
Instinct's total dodge, and the Super Saiyan God aura. Read
`AscensionLadder.java` and `entity/CaydenCobb.java` before touching this
module - every number and every reused entity in here is keyed off what
those two files already do. Neither file was edited.

Package: `com.barbarajones.v2.abilities` (+ `.item`, `.net`, `.client`).

## The one wiring line the orchestrator needs to add

In `BarbaraJonesMod`'s constructor, next to every other module's
`init(modBus)` call:

```java
com.barbarajones.v2.abilities.PlayerAbilities.init(modEventBus);
```

That is the entire integration surface. Everything else in this module is a
`@Mod.EventBusSubscriber` and registers itself the moment Forge scans the
classpath - `AbilityEvents` (world hooks), `AbilityEvents.TabInjector`
(creative tab), `client.AbilityKeys` (keybinds, client-only), and
`client.AbilityHud` (the HUD, client-only) all need nothing further.

**Cross-module fix already applied**: `v2/manual/book/ManualBook.java`
(another agent's in-progress file, not owned by this module) already had a
whole "Player Abilities" chapter written against this exact set of six
items and field names - convincing evidence the item ids/names below are
the actually-expected ones - but imported them from the wrong package
(`com.barbarajones.abilities` instead of `com.barbarajones.v2.abilities`).
That one import line was fixed in place; nothing else in that file was
touched. Worth the orchestrator double-checking that file still owns its own
content otherwise.

**Lang**: `assets/barbarajones/lang/abilities.json` is written and needs to
be merged into `en_us.json` like every other module's lang file.

**Assets**: `tools/make_abilities.ps1` has already been run - the six item
textures exist under `textures/item/` and were verified read-back after
writing (16x16, non-blank, PNG dimensions checked). Re-run it any time the
art needs regenerating.

## What was registered

Own `DeferredRegister<Item>` in `AbilityItems` (registry ids, all under the
`barbarajones` namespace):

| Item id | Class | Ability |
|---|---|---|
| `krave_gauntlet` | `item.KraveGauntletItem` | `AbilityId.GAUNTLET` |
| `laser_lens` | `item.LaserLensItem` | `AbilityId.LASER` |
| `ascension_charm` | `item.AscensionCharmItem` | `AbilityId.CHARM` |
| `meteor_totem` | `item.MeteorTotemItem` | `AbilityId.TOTEM` |
| `instinct_band` | `item.InstinctBandItem` | `AbilityId.BAND` |
| `god_core` | `item.GodCoreItem` | `AbilityId.GODCORE` |

All six: `stacksTo(1)`, `Rarity.EPIC`, never damageable, never consumed on
use - they are cooldown-gated tools, not charges. They are injected into
`ModTabs.MAIN`'s creative tab at runtime via `BuildCreativeModeTabContentsEvent`
(`AbilityEvents.TabInjector`) rather than by editing `ModTabs.java`.

Own network channel in `net.AbilityNetwork` (`barbarajones:abilities`,
completely separate from any other module's on purpose - see that class's
doc comment): `PacketActivateAbility` (C2S, keybind press) and
`PacketAbilitySync` (S2C, full unlock/cooldown/active snapshot).

No blocks, no entities, no menus, no new sounds - every sound used
(`COMBAT_HEAVY_HIT`, `KRAVE_BEAM_FIRE`, `TRANSFORM_CHARGE`, `KRAVE_ROAR`,
`TRANSFORM_ULTRA_HUM`, `KRAVE_SCREECH`, `TRANSFORM_GODPULSE`) already exists
in `content.ModSounds` and was chosen to match the ability's flavor, per the
brief's instruction to reuse rather than duplicate.

## What each ability actually does

| Ability | Effect | Cooldown | Active window |
|---|---|---|---|
| Krave Gauntlet | Cone sweep in front of the player: 6 damage + a 3.4/0.85 launch to everything it catches, matching `CaydenCobb`'s `FLASH_ODDS` shove exactly. | 3s | instant |
| Laser Lens | Fires 3 real `entity.KraveLaser` bolts (the *same* class Cayden fires, constructed directly - not reimplemented) at whatever is under the crosshair. | 2s | instant |
| Ascension Charm | Opens a 3s window of `CaydenCobb.combatFlight()`-style burst lift (per-tick, in `AbilityEvents#tickCharm`), fall distance held at zero throughout. | 10s | 3s |
| Meteor Totem | Spawns 3 real `entity.KraveMeteor` (TYPE_METEOR) above wherever the player is looking, **without** `saiyanStrike()` - so it lands as the apocalypse's own indiscriminate blast, not a safe boss-only strike. Will hurt the caller if they stand where they aimed. That is the intended cost. | 30s | instant |
| Instinct Band | 3s window of total damage refusal (`LivingHurtEvent` cancelled outright in `AbilityEvents#onHurt`) - Ultra Instinct's dodge at 100% instead of `CaydenCobb`'s per-hit `dodgePercent` roll. | 60s | 3s |
| God Core | Real `AttributeModifier`s for 30s: +150% attack, +60% movement speed, +12 max health (topped up immediately, not regenerated), plus 35% of incoming damage refused via a straight multiplier (`GodCoreItem.DAMAGE_TAKEN_MULTIPLIER`). Numbers are `AscensionLadder.GOD`'s own stat sheet, scaled down from "final boss" to "player can survive holding this". | 120s | 30s |

## Unlocks - the clean check the quest module can grant against

`AbilityData.grant(ServerPlayer, AbilityId)` is the entry point: idempotent,
announces itself in chat, plays a sound, and re-syncs the HUD. A quest
reward, a command, anything can call it directly and does not need to know
anything else about this module. `AbilityData.isUnlocked(Player, AbilityId)`
is the matching read side - both live in this module's own persistent-NBT
storage (`AbilityData`, same `PERSISTED_NBT_TAG` sub-tag pattern as
`KraveLevel`/`PlayerStats`), synced to the client in `PacketAbilitySync` and
read by both `AbilityItem`'s tooltip and `AbilityHud`.

Nothing else has to call `grant` by hand, though - every ability already
grants itself automatically, matched deliberately against what the in-game
manual's own "Player Abilities" chapter already documents (see the
cross-module note above) rather than invented independently:

| Ability | Gate | How it is checked |
|---|---|---|
| `GAUNTLET` | Quest `slay_krave` (`quest.Quests.SLAY_KRAVE`) done | Polled every 30 ticks per player in `AbilityEvents#watchUnlocks` via `Quests.isDone(player, id)` |
| `LASER` | Boss kill: **The Manager** (`boss.manager.TheManager`) | `LivingDeathEvent` in `AbilityEvents#onDeath`, grants everyone within 48 blocks of the kill (Cayden/Barbara land plenty of the actual hits, same pattern `ProgressionEvents` uses for the Krave Monster) |
| `CHARM` | Boss kill: **Mom Cobb** (`boss.mom.MomCobbBoss`) | Same `LivingDeathEvent` path |
| `TOTEM` | Quest `revenge` (`quest.Quests.REVENGE`) done | Quest watcher |
| `BAND` | Quest `kosmos_master` (`quest.expansion.QuestExpansion.KOSMOS_MASTER`) done | Quest watcher |
| `GODCORE` | Quest `peace` (`quest.Quests.PEACE`) - the entire base questline finished - **or** the newer quest module's own `i_krave_the_krave` ability key | Quest watcher checks both; see below |

All four `quest.Quests`/`quest.expansion.QuestExpansion` ids already exist
on the base graph - nothing needed there. Full reasoning for each pick lives
in `AbilityUnlocks.java`'s class doc.

**God Core's second path, explained.** While reading `v2/quests/` (a newer,
separate quest engine another agent is building in the same batch) it turned
out `data/barbarajones/quests/bosses/peace.json` - that engine's own actual
finale quest - already rewards an ability keyed `i_krave_the_krave`, clearly
intended for exactly this slot. Rather than pick one questline over the
other, `AbilityEvents#watchUnlocks` accepts a grant from either:
`Quests.isDone(player, "peace")` (this module's primary, documented path) or
`v2.quests.QuestApi.hasAbility(player, "i_krave_the_krave")` (a zero-cost
bonus check, wrapped in a try/catch so a hiccup in that still-being-built
module can never break this one). A player who finishes either questline's
ending gets God Core.

## Crafting - hard recipes, late-game materials

All six are `crafting_shapeless` (kept to at most 9 total ingredient
entries - the 3x3 grid's actual cap; a shapeless recipe above that count
silently fails to load). Every recipe uses `barbarajones:krave_dust`
(economy module, already registered) as its common thread, plus:

- **Krave Gauntlet**: iron, gold, `krave_cereal` (a Krave Monster boss drop).
- **Laser Lens**: `managers_tie` + `employee_of_the_month` - both drop from
  **The Manager**, the same boss that gates using the item.
- **Ascension Charm**: `moms_tv_remote` + `confiscated_krave` (Mom Cobb
  drops) + feathers.
- **Meteor Totem**: `krave_family_box`, magma cream, a diamond.
- **Instinct Band**: `barbarajones:krave_syrup` x3 + phantom membrane +
  `krave_tether` (a Kosmos-branch item, echoing Instinct Band's own Kosmos
  gate).
- **God Core**: `severance_check` + `five_hundred_dollars` (Manager drops)
  + `adoption_papers` (Mom Cobb drop) + `krave_syrup` x3 + netherite ingot -
  deliberately the hardest recipe, gated behind drops from **both** bosses.

`barbarajones:krave_syrup` was confirmed registered by
`v2.economy.KraveEconomy.KRAVE_SYRUP` while reading that module's own docs
(`docs/modules/krave-economy.md` explicitly names it as *"the id the
machines/armour/abilities modules should build on"*) - not present in
`content.ModItems`, which is why an earlier pass of this doc flagged it as a
possible gap. It is not one; every id in every recipe above was confirmed
registered somewhere in the tree as of this writing.

## The keybind system

`client.AbilityKeys` registers six `KeyMapping`s (`key.barbarajones.ability_<id>`),
all unbound by default under the existing `key.categories.barbarajones`
category (`KraveKeys.OPEN_CODEX` already lives there) - a player binds
whichever keys they want in Controls. A press sends `PacketActivateAbility`
carrying only the `AbilityId` index, never an item or a slot; the server
searches the player's inventory + offhand for a matching `AbilityItem` and
activates it. That indirection is the entire "equipped" model this system
uses - equipped means *carried somewhere in your inventory*, not placed in
a dedicated gear slot (this codebase has no Curios-style equipment system to
hook into). Right-click also works on every ability item directly, for
testing one without touching the keybind menu at all.

## The HUD

`client.AbilityHud`, a `RenderGuiOverlayEvent.Post` pinned to
`VanillaGuiOverlay.HOTBAR` (same anchor `KraveHud` uses, so the two never
double-draw). Shows one 20x20 slot per **unlocked** ability, centered above
the hotbar: the real item icon, a top-down dark wipe for remaining cooldown,
a gold pulsing frame plus a countdown while an active window is running.
Fed entirely by `client.AbilityClientState`, which `PacketAbilitySync`
populates on login, respawn, dimension change, and every activation -
`AbilityClientState` itself has zero `net.minecraft.client` imports, which
is what lets the common-side `AbilityItem.appendHoverText` read it directly
for the tooltip's LOCKED/Unlocked line without a `DistExecutor` hop.

## What is finished

- All six items: real effects, real cooldowns, real (where applicable)
  active-duration windows, sounds, particles.
- Persistent per-player storage (`AbilityData`, same `PERSISTED_NBT_TAG`
  pattern as `KraveLevel`/`PlayerStats` - survives death and respawn).
- Automatic unlock granting off both boss kills and quest completions, plus
  a clean idempotent `grant()` anything else (a quest reward, a command) can
  call directly - including, as it turned out, a real second questline's own
  reward, recognized without needing to duplicate anything.
- Full client sync + HUD + keybinds + tooltips.
- Reuses `entity.KraveLaser` and `entity.KraveMeteor` outright, as instructed
  - neither file was touched.
- Injects into the shared creative tab without editing `ModTabs.java`.
- Textures generated, written, and read back to confirm they are not blank.
- Every registered id has a lang entry in `assets/barbarajones/lang/abilities.json`.
- Found and fixed a one-line cross-module import bug in another agent's
  in-progress file (`v2/manual/book/ManualBook.java`) that would otherwise
  have failed the whole build - see the cross-module note above.

## What is NOT finished / known gaps

- **No armor-slot or Curios integration.** "Equipped" means "carried in
  inventory" everywhere in this module, including the docstrings. If the
  mod later adds a real equipment-slot system, the inventory scan in
  `net.PacketActivateAbility#findCarried` is the one place that would need
  to change.
- **Balance is a first pass**, not a tuned pass. Every number (damage,
  cooldown seconds, buff percentages) is a reasoned estimate anchored to
  `AscensionLadder`'s own rows, not something played and iterated on.
- **No verification build was run**, per the hard rule against running
  Gradle. Every Forge/Minecraft method signature this module calls was
  cross-checked against the actual mapped jar
  (`forge-1.20.1-47.2.0_mapped_official_1.20.1.jar`) with `javap` rather
  than left to guesswork, but a real `BUILD SUCCESSFUL` has not been seen.
- **The quest watcher polls, it does not push.** A player who finishes the
  gating quest while genuinely offline sees the unlock the moment they next
  load in (their `PlayerTickEvent` starts ticking again); nothing is missed,
  it just is not instantaneous across a session boundary that doesn't exist
  anyway.
- **Two parallel quest systems exist in this codebase right now**
  (`quest.Quests`/`quest.expansion.QuestExpansion`, the established one this
  module gates against, and a newer `v2.quests` engine another agent was
  actively building alongside this task). This module bets on the
  established one as primary and only reaches into the newer one for God
  Core's one already-matching reward key. If the newer engine ends up
  replacing the old one entirely, `AbilityUnlocks`/`AbilityEvents#onDeath`
  and `#watchUnlocks` are the only places that would need new quest/boss
  ids - the item, HUD, keybind and crafting layers are untouched by which
  quest system is authoritative.
