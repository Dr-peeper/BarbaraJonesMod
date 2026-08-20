# Craveling Mob Family (`com.barbarajones.v2.mobs`)

The mod's new common overworld hostile family: CRAVELING (baseline),
KRISPBONE (ranged skirmisher), LOOMWEAVER (web-trapper), SOGGY (tanky/AoE on
death), and THE MASCOT (rare, non-hostile buffer/fleer). Everything lives
under its own package with its own `DeferredRegister`s, per the module rules.

## The one entry point

```java
com.barbarajones.v2.mobs.Mobs.init(bus);
```

Call this once from `BarbaraJonesMod`'s constructor, alongside the other
`ModX.register(bus)` calls. It registers this module's four own
`DeferredRegister`s (entities, items, sounds, blocks) and schedules
`MobSpawnPlacements.register()` for common setup. Nothing else needs to be
called - attribute suppliers (`MobAttributes`) and the client renderer/layer
bindings (`client.MobsClientSetup`) register themselves via
`@Mod.EventBusSubscriber` and fire automatically once the mod loads.

**I did NOT add this call to `BarbaraJonesMod.java` myself** - that file is on
the forbidden-edit list. This is the one line the orchestrator needs to add.

## What the orchestrator must wire centrally

1. **`BarbaraJonesMod.java`**: add `com.barbarajones.v2.mobs.Mobs.init(bus);`
   in the constructor, next to the other registrations.
2. **`content/ModTabs.java`**: the five spawn eggs and two flavor items
   (`krave_shard`, `cereal_mascot_head`) live in this module's OWN
   `ModMobItems.ITEMS`, not `ModItems.ITEMS`, so they will NOT appear in the
   "Barbara Jones" creative tab until someone adds a second `forEach` there:
   ```java
   .displayItems((params, output) -> {
       ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
       com.barbarajones.v2.mobs.ModMobItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
   })
   ```
   Until that's added, the items still exist and are still fully obtainable
   via `/give`, they just won't show up when browsing the tab.

## Registered IDs

**Entities** (`ModMobEntities`, `barbarajones:` namespace):
`craveling`, `krispbone`, `loomweaver`, `soggy`, `the_mascot` (all spawn-egg
eligible, `MobCategory.MONSTER`/`CREATURE`), plus `krave_shard`
(`MobCategory.MISC`, Krispbone's projectile - no spawn egg, no attributes,
not a `LivingEntity`).

**Items** (`ModMobItems`): `craveling_spawn_egg`, `krispbone_spawn_egg`,
`loomweaver_spawn_egg`, `soggy_spawn_egg`, `the_mascot_spawn_egg`,
`krave_shard` (also Krispbone's projectile visual/ammo), `cereal_mascot_head`
(The Mascot's guaranteed trophy drop, `Rarity.EPIC`).

**Blocks** (`ModMobBlocks`): `milk_webbing` - Loomweaver's web trap, built on
vanilla `WebBlock`. Deliberately has **no BlockItem and no creative-tab
entry** - it's placed by the mob only, never obtained by a player. Not a gap.

**Sounds** (`ModMobSounds`): four per mob (`_ambient`, `_hurt`, `_death`, plus
one signature cue - `_step`/`_shoot`/`_web`/`_splash`/`_flee`), 20 total, all
newly synthesized (see `tools/make_craveling_mobs.ps1`). Entries were added
to the shared `assets/barbarajones/sounds.json` **by hand, append-only** -
that file isn't on the forbidden list but is a real shared resource; flagging
it here in case another agent touched it at the same time.

## Distinct AI per mob (not five `MeleeAttackGoal`s)

- **Craveling** - `CravelingCrumbleAttackGoal` (a `MeleeAttackGoal` override):
  connecting hits spray crumb particles and have a 35% chance to apply a
  short Slowness, "crumbs underfoot." Never overrides sun-sensitivity (unlike
  Zombie/Skeleton), so unlike a zombie it does not burn in daylight - a real,
  not just cosmetic, difference from the mob it's replacing in spawn weight.
- **Krispbone** - `KrispboneCombatGoal`, its ONLY combat goal (no
  `MeleeAttackGoal` at all): kites backward inside 5 blocks, advances beyond
  14, and fires `fireShardBurst()` - three `KraveShardEntity` projectiles in
  an 8-degree spread - on a distance-scaled cooldown.
- **Loomweaver** - `LoomweaverWebTrapGoal` (flagged with no `Goal.Flag`, so it
  layers under a normal `MeleeAttackGoal` bite) drops `milk_webbing` at a
  target's feet on a 5s cooldown. Also copies vanilla `Spider`'s
  `onClimbable()`/`isClimbing()`/`setClimbing()` pattern so it visually
  clings to walls the instant it bumps into one.
- **Soggy** - `SoggySlamAttackGoal` (a `MeleeAttackGoal` override): a hit
  shoves the target hard and gives Soggy itself a brief self-inflicted
  Slowness (stumbling from its own weight). `die()` is overridden to spawn a
  vanilla `AreaEffectCloud` (Slowness, 3-block radius, 5s) - the milk splash.
- **The Mascot** - NO attack goal, NO target selector, ever.
  `MascotBuffAuraGoal` (no movement/look flags) pulses Speed + Strength every
  3s onto every `CravelingKin` within 10 blocks; `AvoidEntityGoal<Player>`
  (vanilla, radius 12) makes it flee the moment a player closes in.
  `CravelingKin` is a marker interface implemented by the other four mobs
  specifically so this generalizes instead of hard-coding one class.

## Spawn placement + biome modifier

`MobSpawnPlacements` (called once from `Mobs`'s `FMLCommonSetupEvent`
listener) registers all five via `SpawnPlacements.register`: the four hostile
mobs use `Monster.checkMonsterSpawnRules` (the exact rule zombies/skeletons
use), The Mascot uses the more permissive `Mob.checkMobSpawnRules` since it's
a fleeing, non-hostile creature that can appear in daylight.

Five `forge:add_spawns` biome modifiers under
`data/barbarajones/forge/biome_modifier/`, all targeting
`#minecraft:is_overworld` (matching this repo's existing biome-modifier
convention):

| mob | weight | pack size |
|---|---|---|
| craveling | 95 | 1-4 |
| krispbone | 20 | 1-3 |
| loomweaver | 30 | 1-2 |
| soggy | 15 | 1-2 |
| the_mascot | 2 | 1 |

Craveling's weight/pack-size is a direct zombie-parity match (vanilla zombie
is weight 95, 1-4, in most overworld biomes). **Known simplification**:
vanilla's actual per-biome weights vary (desert has husk instead, swamp
differs, etc.); this applies one flat weight across the whole
`#minecraft:is_overworld` tag rather than replicating every biome's exact
table. Documented, not hidden.

## Loot

Standard `minecraft:entity` loot tables under
`data/barbarajones/loot_tables/entities/` (default lookup by registry name -
no `getDefaultLootTable()` override needed in Java):

- **craveling**: `minecraft:cocoa_beans` (1-3) + `barbarajones:krave_cereal` (0-2)
- **krispbone**: `barbarajones:krave_shard` (1-3) + cocoa beans (0-1)
- **loomweaver**: `minecraft:string` (0-2, "hardened milk-strands") + krave_cereal (0-1)
- **soggy**: cocoa beans (0-2) + krave_cereal (0-1) + 15% chance `minecraft:milk_bucket`
- **the_mascot**: guaranteed `cereal_mascot_head` + guaranteed `barbarajones:golden_krave` + krave_cereal (3-6) + cocoa beans (2-4) - "real drops" for a rare kill.

## Models/renderers/textures

All five have bespoke geometry (`client/*Model.java` + `*Renderer.java`), not
reused vanilla layers:

- **Craveling / Krispbone / Soggy / The Mascot**: custom `HumanoidModel`
  meshes (same pattern as the existing `ManagerModel`) - Craveling adds four
  "chunk" cubes for a blocky cereal-square silhouette, Krispbone replaces
  every box with a genuinely thinner one (hollow ribcage read), Soggy
  enlarges the torso and adds a belly-sag cube, The Mascot replaces the head
  with an oversized box (the costume head).
- **Loomweaver**: a fully bespoke `HierarchicalModel` - abdomen + thorax +
  head + eight independently-animated legs in an alternating tripod gait, not
  a modified biped.

Every texOffs/box-size pair in each `*Model.java` is mirrored exactly (same
numbers) in `tools/make_craveling_mobs.ps1`'s `Paint-Box` calls - the script
comment documents the shared UV formula. All 8 textures (5 entity skins + 2
item icons + 1 block texture) were generated and reloaded to verify actual
pixel dimensions, not just file existence (see the script's `Save-Verify`).

## Known gaps - read before assuming this is finished

- **Loomweaver's wall-climb is cosmetic, not pathfinding-aware.** It copies
  vanilla `Spider`'s synced `isClimbing`/`onClimbable` flag (set from
  `horizontalCollision`), so it tilts/clings correctly once it's already
  pressed against a wall - but I did **not** override `createNavigation()`
  the way vanilla `Spider` does internally to actively path up sheer walls in
  pursuit. It uses plain `Mob`/`Monster` ground pathfinding. I could not
  verify vanilla `Spider`'s exact `createNavigation()` override body against
  decompiled source (bytecode-only access), and guessing at it risked a worse
  bug than the honest gap. Net effect: Loomweaver chases normally on the
  ground and will visually climb if it happens to bump a wall, but won't
  deliberately scale one to flank.
- **No custom block model beyond the vanilla `cross` parent** for
  `milk_webbing` - it reuses `minecraft:block/cross` (same base as cobweb),
  just with a new texture. That's a deliberate, low-risk choice, not an
  oversight.
- **Creative tab + `BarbaraJonesMod.java` wiring** are explicitly left to the
  orchestrator (see above) - both are on the forbidden-edit list for this
  agent.
- **No advancements/recipes** were added for any of the five mobs or their
  drops - out of scope per the brief, not attempted.
- I could not run Gradle to confirm this actually compiles (rule #1). Every
  API call in every file was individually checked against the real
  `javap`-decompiled `forge-1.20.1-47.2.0_mapped_official_1.20.1.jar` in
  `.tools/jdk17` rather than against memory alone, specifically to catch
  signature mistakes before they became build failures for whoever runs
  Gradle next - but that is not a substitute for an actual compile.

## Files

```
src/main/java/com/barbarajones/v2/mobs/
  Mobs.java                        - the one init(bus) entry point
  ModMobEntities.java, ModMobItems.java, ModMobSounds.java, ModMobBlocks.java
  MobAttributes.java                - EntityAttributeCreationEvent, own subscriber
  MobSpawnPlacements.java           - SpawnPlacements.register calls
  entity/CravelingEntity.java, KrispboneEntity.java, LoomweaverEntity.java,
         SoggyEntity.java, MascotEntity.java, CravelingKin.java
  entity/ai/CravelingCrumbleAttackGoal.java, KrispboneCombatGoal.java,
            LoomweaverWebTrapGoal.java, SoggySlamAttackGoal.java,
            MascotBuffAuraGoal.java
  entity/projectile/KraveShardEntity.java
  block/MilkWebbingBlock.java
  client/CravelingModel.java, CravelingRenderer.java (+ same x4 more),
         KraveShardRenderer.java, MobsClientSetup.java (own @EventBusSubscriber)
src/main/resources/assets/barbarajones/
  textures/entity/{craveling,krispbone,loomweaver,soggy,the_mascot}.png
  textures/item/{krave_shard,cereal_mascot_head}.png
  textures/block/milk_webbing.png
  sounds/{20 files - see ModMobSounds.java for the exact list}.ogg
  blockstates/milk_webbing.json, models/block/milk_webbing.json
  models/item/{krave_shard,cereal_mascot_head,*_spawn_egg (x5)}.json
  lang/craveling_mobs.json
data/barbarajones/
  loot_tables/entities/{craveling,krispbone,loomweaver,soggy,the_mascot}.json
  forge/biome_modifier/{craveling,krispbone,loomweaver,soggy,the_mascot}_overworld_spawns.json
tools/make_craveling_mobs.ps1       - textures + sounds, idempotent, self-verifying
```

Also touched (shared, not forbidden, append-only):
`assets/barbarajones/sounds.json` (20 new entries appended at the end).
