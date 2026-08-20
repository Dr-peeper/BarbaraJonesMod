# KRAVE VILLAGE SYSTEM

Package: `com.barbarajones.v2.village`
Status: feature-complete for the 2.0 spine. Not compiled (per hard rule #1 — the
orchestrator builds). Read the **Not finished / known gaps** section before you
rely on anything here.

---

## 1. The one wiring change the orchestrator must make

Add exactly one line to `BarbaraJonesMod`'s constructor, alongside the other
module inits:

```java
com.barbarajones.v2.village.KraveVillage.init(bus);
```

That is the whole central change. Everything else in this module is wired by
`@Mod.EventBusSubscriber` annotations inside the package:

| class | bus | what it does |
|---|---|---|
| `VillageRegistry` | MOD | entity attributes, common setup (buff table + network channel) |
| `VillageEvents` | FORGE | level tick, block place/break, damage reduction, death, HUD sync |
| `client.VillageClientSetup` | MOD (CLIENT) | renderer, model layer, keybind, menu screen |
| `client.VillageHud` | FORGE (CLIENT) | HUD overlay, disconnect cleanup |
| `client.VillageKeys` | FORGE (CLIENT) | V-key poll |

Also required centrally:

1. **Merge `assets/barbarajones/lang/village.json` into `en_us.json`.** Every
   string this module produces has a key in there. Nothing else in the module
   touches lang.
2. **Optionally add the three items to a creative tab** (`ModTabs` is off-limits
   to me). They are obtainable with `/give` without this; a tab entry is a
   convenience, not a correctness requirement.
3. **Textures are already generated and verified** by `tools/make_village.ps1`.
   Re-run it if the model ever changes. It reads every PNG back and checks
   dimensions and opaque-pixel count, and exits non-zero on any problem.

No `build.gradle`, `ModItems`, `ModBlocks`, `ModEntities`, `ModSounds`,
`ModTabs`, `ModMenus`, `ClientSetup`, `EventHandler`, `BarbaraJonesMod`,
`ModNetwork` or `en_us.json` file was edited.

---

## 2. Registered ids

All under the `barbarajones` namespace, all from this module's own
`DeferredRegister`s in `VillageRegistry`.

### Items (`VillageRegistry.ITEMS`)

| id | field | notes |
|---|---|---|
| `barbarajones:village_charter` | `VILLAGE_CHARTER` | founds a settlement where you click; consumed |
| `barbarajones:village_atlas` | `VILLAGE_ATLAS` | opens the village screen; reusable |
| `barbarajones:krave_villager_spawn_egg` | `KRAVE_VILLAGER_SPAWN_EGG` | `ForgeSpawnEggItem`, colours `0x8A5A2A` / `0xE9B23C` |

### Entities (`VillageRegistry.ENTITIES`)

| id | field | notes |
|---|---|---|
| `barbarajones:krave_villager` | `KRAVE_VILLAGER` | `MobCategory.CREATURE`, 0.62 x 1.95, tracking 10, update 3 |

One entity type for all five professions — profession is synced entity data. Its
attributes are registered by `VillageRegistry.onAttributeCreation` (a second
listener on `EntityAttributeCreationEvent`; `ModEntityAttributes` is untouched
and its self-check list does not include us, so nothing there breaks). Built from
`createMobAttributes()`. Its renderer is registered in `VillageClientSetup`.

### Menus (`VillageRegistry.MENUS`)

| id | field | screen |
|---|---|---|
| `barbarajones:krave_trade` | `KRAVE_TRADE` | `client.KraveTradeScreen` |

### Network

Own `SimpleChannel` at `barbarajones:village` (separate from `ModNetwork`'s
`barbarajones:main`, so packet ids never collide with another module's).

| id | message | direction |
|---|---|---|
| 0 | `PacketVillageOffers` | S→C |
| 1 | `PacketSelectOffer` | C→S |
| 2 | `PacketVillageStatus` | S→C |

### Assets written

```
assets/barbarajones/lang/village.json
assets/barbarajones/models/item/village_charter.json
assets/barbarajones/models/item/village_atlas.json
assets/barbarajones/models/item/krave_villager_spawn_egg.json
assets/barbarajones/textures/item/village_charter.png          16x16
assets/barbarajones/textures/item/village_atlas.png            16x16
assets/barbarajones/textures/entity/krave_villager/grocer.png       64x64
assets/barbarajones/textures/entity/krave_villager/cerealogist.png  64x64
assets/barbarajones/textures/entity/krave_villager/builder.png      64x64
assets/barbarajones/textures/entity/krave_villager/guard.png        64x64
assets/barbarajones/textures/entity/krave_villager/courier.png      64x64
tools/make_village.ps1
```

No blocks, no block models, no blockstates, no loot tables, no sounds.json entry,
no configured features. Nothing that can fail a registry load.

Keybind: `key.barbarajones.village`, default **V** (vanilla-unbound; the codex
already owns K).

---

## 3. Public API — `KraveVillage`

This is the only class outside code should touch. Everything on it is static.
`Village`, `KraveVillageData` and the goals are package-internal in spirit;
reading them from outside will work but is not a supported contract.

### Progression gate (this is what the quest module wants)

```java
// the dimension-wide answer
int tier = KraveVillage.tierOf(serverLevel);            // 0..5
boolean open = KraveVillage.isPortalUnlocked(serverLevel);

// the constant to compare against - never hard-code 3
KraveVillage.PORTAL_TIER          // == VillageTier.VILLAGE.index() == 3
```

`tierOf(ServerLevel)` returns the **highest** tier of any settlement in that
dimension. There is also `tierOf(ServerLevel, BlockPos)` for "the tier of the
village at this spot", and `tierOf(Level)` which returns 0 on the client.

Tiers are **derived every village tick, not banked** — they can go down if the
player demolishes their own town. If the quest module wants a monotone,
never-decreasing tier (and its existing `VillageState` is explicitly built that
way), it should keep its own high-water mark and feed it from this query rather
than expecting this number to only ever rise.

### Reading a settlement

```java
Optional<VillageView> here    = KraveVillage.containing(serverLevel, pos);
Optional<VillageView> closest = KraveVillage.nearest(serverLevel, pos);
Optional<VillageView> byId    = KraveVillage.byId(serverLevel, uuid);
List<VillageView>     all     = KraveVillage.all(serverLevel);

int pop  = KraveVillage.populationOf(serverLevel, pos);
int def  = KraveVillage.defenceOf(serverLevel, pos);
int prod = KraveVillage.productionOf(serverLevel, pos);   // Krave per real hour
int mood = KraveVillage.happinessOf(serverLevel, pos);    // 0..100, 50 outside
```

`VillageView` is an **immutable record snapshot**: id, name, origin, radius,
tier, population, buildings, defence, happiness, production, stockpile,
memberCount, plus the helpers `tierIndex()`, `unlocksPortal()`,
`raidDamageMultiplier()` and `contains(BlockPos)`. Take it fresh per method call;
do not cache it across ticks.

Every read is null-safe: a null level or a position outside every claim gives the
neutral answer, never an exception.

### Defence

```java
float multiplier = KraveVillage.raidDamageMultiplier(serverLevel, pos); // 0.40..1.00
event.setAmount(event.getAmount() * multiplier);
```

Curve: `reduction = min(0.60, defence * 0.006)`, so 100 defence is the 60 % cap.
Both constants are public (`DEFENCE_TO_REDUCTION`, `MAX_DAMAGE_REDUCTION`).

**Do not double-apply.** This module already applies the same multiplier in
`VillageEvents.onLivingHurt` to any hostile-source damage landing on a
non-monster inside a claim. If the raid module has its own damage path that does
*not* go through `LivingHurtEvent`, call this. If it goes through
`LivingHurtEvent`, it is already covered.

### Mutating

```java
UUID id = KraveVillage.found(serverLevel, origin, founderOrNull, "Name");
boolean joined = KraveVillage.addMember(serverLevel, villageId, playerUuid);
KraveVillage.addKrave(serverLevel, pos, 8);
int got = KraveVillage.withdrawKrave(serverLevel, villageId, 8);
KraveVillage.adjustHappiness(serverLevel, pos, -5);
```

`found` returns the existing village if the position is already claimed rather
than creating an overlapping second one.

### The extension point — "every item matters to the village"

```java
KraveVillage.registerVillageBuff(MyBlocks.WATCHTOWER.get(),
        VillageBuff.builder()
                .building(3)      // counts as 3 buildings for tier maths
                .defence(9)       // adds to the defence rating
                .production(0)    // Krave per hour, before tier + mood multipliers
                .attraction(2)    // how hard it pulls new residents in
                .happiness(1)     // shifts the happiness target
                .description("block.mymod.watchtower.village")
                .build());

// RegistryObject form - resolution is deferred until registries thaw, so this is
// safe from a static initialiser
KraveVillage.registerVillageBuff(MyBlocks.CEREAL_SILO, VillageBuff.house());
```

Helpers: `VillageBuff.house()`, `VillageBuff.fortification(int)`,
`VillageBuff.NONE` (registering NONE removes a block from the system).
Read back with `KraveVillage.buffOf(Block)` (never null) and
`KraveVillage.hasBuff(Block)`.

Registrations are thread-safe (Forge constructs mods in parallel). Register in a
constructor or common setup. Registering later works, but blocks already placed
in the world are only picked up when that village's rolling sweep next reaches
them — up to about five minutes.

The base table lives in `VillageBuffs` and covers beds, doors, workstations,
storage, lights, walls, the mod's Krave blocks/logs/pods, and the comfort
furniture. It is installed during common setup and is a **default** — a later
registration for the same block wins.

---

## 4. How the system actually works

### Persistence

`KraveVillageData extends SavedData`, one per dimension, file
`<dimension>/data/barbarajones_villages.dat`. Villages are keyed by `UUID`, not
by origin `BlockPos` — position is not identity, and the origin is movable.
Every mutating method calls `setDirty()`; callers reaching a `Village` directly
must too.

Survives reload and world reopen: origin, name, members, resident UUIDs, the full
tracked-building map (position + block id), tier, happiness, stockpile, both
progress accumulators, and the cached profession tallies. A block whose mod has
since been removed silently drops out of the map on load rather than aborting the
whole SavedData read; one corrupt village entry is logged and skipped rather than
costing the player every other village.

Multiple villages per dimension are fully supported. Overlapping claims resolve
to the nearest origin, so answers are stable rather than iteration-order
dependent.

### Building tracking

Two directions, on purpose:

* **Instant** — `BlockEvent.EntityPlaceEvent` / `BreakEvent` update the map
  immediately, so placing a bed moves the counter within a second.
* **Rolling sweep** — 3 cells of 16×16×16 per village tick, 196 cells to a full
  pass over the 113×65×113 claim: roughly 5 minutes per pass, ~12k block reads
  every 5 seconds. This is what corrects the drift that never fires a break
  event — explosions, pistons, fire, world edits, restored backups.

The sweep only deletes entries in cells whose chunks were **loaded** when it
looked, which is the single condition that stops a village on the edge of render
distance from slowly deleting itself. Tracking is capped at 4096 blocks per
village so the save file cannot grow without bound.

### Tier

`VillageTier.evaluate(buildings, population, defence)` walks down from the top and
returns the highest tier meeting **all three** requirements.

| tier | idx | buildings | population | defence | pop cap | production x |
|---|---|---|---|---|---|---|
| Wilderness | 0 | 0 | 0 | 0 | 2 | 0.5 |
| Krave Camp | 1 | 3 | 1 | 0 | 4 | 1.0 |
| Krave Hamlet | 2 | 8 | 3 | 4 | 7 | 1.4 |
| **Krave Village** | **3** | 16 | 6 | 12 | 11 | 1.9 |
| Krave Town | 4 | 28 | 10 | 26 | 16 | 2.5 |
| Krave Capital | 5 | 44 | 15 | 45 | 22 | 3.2 |

Tier 3 is `PORTAL_TIER`. Tier changes are announced in chat to members and to
anyone standing in the claim, with a sound.

### Attraction, production, happiness

Every 100 ticks a village banks `attraction` points; at 600 it spawns a resident,
choosing the profession the village is *short of* rather than at random. It stops
banking at the tier's population cap, so emptying a full village does not vomit
five villagers out at once. If no safe ground is loaded the points are kept and
retried five seconds later rather than dropping a villager into a wall.

Production accumulates the same way — `production` points per village tick, 720
of them to one Krave into the stockpile (720 village ticks = one real hour). The
stockpile caps at `64 * (tier + 1)`.

Happiness drifts one point per village tick toward a structural target built from
buildings, defence, the sum of every buff's happiness, and an overcrowding
penalty. Happiness feeds production, attraction, and trade prices.

### Villagers

`KraveVillagerEntity extends PathfinderMob implements Npc`. Shares nothing with
vanilla `Villager` — no `Brain`, no POI, no gossip, no `MerchantOffer`, and it
deliberately does **not** implement `Merchant` (this module ships its own menu, so
implementing that interface would buy nothing and add a large API surface to get
wrong).

Five professions in `KraveProfession`, each with its own texture, trade table and
settlement contribution (defence / production / attraction, per trade level):

| profession | defence | production | attraction |
|---|---|---|---|
| Grocer | 0 | 4 | 3 |
| Cerealogist | 0 | 9 | 1 |
| Builder | 1 | 2 | 2 |
| Guard | 5 | 0 | 0 |
| Courier | 1 | 1 | 6 |

**Ordinals are persisted and sent over the wire — append new professions, never
reorder.**

AI: `FloatGoal` → `VillagerSleepGoal` → melee → `VillagerUseBuildingGoal` →
`VillagerWanderHomeGoal` → look goals. Guards additionally target monsters;
everyone retaliates when hit.

* **Wander** is homeward — it picks points near the pathfinding restriction
  (which the entity sets to the village origin every 5 seconds), so residents
  drift around the village instead of out of it.
* **Use buildings** walks to a position from the settlement's own tracked-block
  map and works there with profession-coloured particles and a work sound. It
  reads the same map the tier maths reads, so a villager standing at a
  workstation is standing at something that is genuinely counting.
* **Sleep** finds a free bed from the tracked map (fast path — no world scan),
  falling back to a small 17×9×17 box only for a villager with no settlement.
  Bed claims are deduplicated in one entity query, not one per candidate.
  An `OCCUPIED` bed with nothing lying in it is accepted, so a crash mid-night
  does not permanently lock every bed in town.

### Making defence mean something

Vanilla hostiles only know how to hunt `Villager`, and a Krave Villager is not
one — so out of the box nothing would ever attack a settlement and the whole
defence rating would be a number on a screen. `Village.aggravateMonsters` fixes
that: once per village tick it points up to 4 idle hostiles inside the claim at
the nearest resident within 16 blocks. One entity query per five seconds, only
monsters with no target already, and range-gated so it reads as "the zombie found
them" rather than as the village magnetising every mob in the chunk.

Guards then engage (they are the only profession that seeks combat), everyone
retaliates via `HurtByTargetGoal` with `setAlertOthers()`, and the defence rating
reduces the damage that lands. Losing a resident costs the village 8 happiness.

I did **not** reach into vanilla mobs' `goalSelector`/`targetSelector` from
outside to do this — that depends on a Forge access transformer I could not
verify without building, and `Mob.setTarget` is public and sufficient.

### Trading

Custom `KraveTradeMenu` + `KraveTradeScreen`, not vanilla's merchant pair,
because the screen has to show trade level, Krave eaten, and what the next level
unlocks — the feeding loop is invisible otherwise.

Slots: 0 payment A, 1 payment B, 2 result (read-only; taking from it is the one
and only code path that performs a trade), 3–38 player inventory.

Offers are `VillageOffer` — our own type, our own wire format. Order-insensitive
payment matching, a price multiplier driven by village happiness (recomputed when
the screen opens, applied at read time so the base cost survives), and partial
daily restocking (`uses` drops by half `maxUses` per day rather than zeroing, so
a heavily farmed trade takes several days to recover).

Levelling: 5 levels, XP thresholds 0 / 12 / 40 / 90 / 170. A level-up **appends**
the next tier of offers; nothing the player already relies on ever disappears.
XP comes from completed trades and from being hand-fed Krave Cereal
(4 XP each; Golden Krave is worth 24).

Feedback on feeding is deliberately loud: particles, a rising eat sound, an
actionbar line naming exactly how much more Krave is needed, a renderer flash and
swell (`getGlow()`), a permanent size increase with `getKraveFed()`, a bigger
size step per trade level, and on level-up a particle burst, a level-up chime and
a chat line. The screen shows level pips, an XP bar with the exact remaining
count, the Krave-eaten counter and a locked teaser row.

Selection is client-initiated, **server-authoritative**: the client sends only an
index, validated against the server's own list.

### HUD and screens

* `client.VillageHud` — compact panel top **right** (the quest tracker owns top
  left), showing name, tier, tier pips, population/cap, Krave per hour and
  defence with its damage-reduction percentage. Drawn only from the last synced
  status; goes stale and hides after 6 seconds of silence.
* `client.VillageScreen` — the full read-out, on **V** or the Village Atlas.
  Shows state, and every unmet requirement for the next tier as
  current/required in red, plus the Krave portal gate state. A player who cannot
  see *why* their village will not advance assumes the system is broken.

Sync is a small fixed-shape delta (`PacketVillageStatus`, nine numbers and a
name) sent only on change plus a 4-second heartbeat. A capital costs the same
bandwidth as a camp.

---

## 5. Finished

- Village state as `SavedData` on the `ServerLevel`, surviving reload and world
  reopen; multiple villages supported, one is the common case.
- Origin, tier, member list, resident list, tracked buildings, defence rating,
  happiness and Krave production rate, all persisted.
- Attraction over time from placed buildings; better buildings raise the tier;
  tier gates the portal through `KraveVillage.tierOf(level)` / `PORTAL_TIER`.
- Five custom NPC professions on a bespoke model and rig, with per-profession
  textures, that wander the village, use the tracked buildings, and sleep in beds.
- Custom trading menu + screen, restocking offers, five trade levels that append
  offers, and Krave-feeding that raises them with visible feedback everywhere.
- Defence rating from village blocks and Guards, reducing raid damage, with the
  `registerVillageBuff(Block, VillageBuff)` extension point and a base table.
- Hostiles actually attack the settlement, so the defence rating is exercised
  rather than merely displayed.
- HUD overlay and a full village screen.
- Texture generation script that verifies what it wrote by reading it back.
- Lang file with an entry for every string the module can produce.

## 6. Not finished / known gaps — read this

1. **Nothing here has been compiled.** Hard rule #1. The code is written against
   1.20.1 Mojang mappings from the conventions in this repo, but a first build
   may well surface a signature I got wrong. The likeliest candidates, in order:
   `SoundEvents` fields that are `Holder.Reference<SoundEvent>` rather than plain
   `SoundEvent` in 1.20.1 (I avoided every one I was unsure of, but
   `SoundEvents.WOOD_PLACE`, `ITEM_PICKUP`, `SHIELD_BLOCK`, `BREWING_STAND_BREW`
   and `VILLAGER_*` in `ai/VillagerUseBuildingGoal` and `KraveVillagerEntity` are
   the places to look first); and `KraveTheme` helper signatures, which I read
   from source but did not exercise.
2. **The Courier profession has no unique behaviour beyond its stat
   contribution and trade table.** The design note says "delivers the stockpile"
   — the stockpile exists and `withdrawKrave` is exposed, but no courier AI
   actually carries anything to a player yet. Attraction and trades are real; the
   delivery fantasy is not implemented.
3. **The village stockpile has no consumer inside this module.** It accumulates
   and is displayed and can be withdrawn through the API, but nothing spends it.
   That is intentional headroom for the economy module; do not describe it to a
   player as doing something until something spends it.
4. **No `/village` debug command.** `VillageEvents.lastStatusSentTo` exists as a
   hook for one. Diagnosing a village currently means reading the HUD.
5. **Retraining is unimplemented.** `setProfession` exists and correctly rerolls
   the trade list, but nothing in game calls it — there is no job-block or item
   that changes a villager's profession after it arrives.
6. **Sleeping villagers use vanilla's `startSleeping`, which sets the bed's
   `OCCUPIED` blockstate.** A server killed mid-night leaves that flag set. The
   sleep goal works around it (an occupied bed with nothing in it is accepted),
   but the blockstate itself is not cleaned up on world load, so a player may see
   a bed rendered as occupied with nobody in it until a villager next uses it.
7. **Claim geometry is fixed** at radius 56, height ±32. It is not configurable
   and the charter cannot resize it.
8. **The attraction spawner does not check light or hostility** at the chosen
   position beyond needing two air blocks over non-air, non-liquid ground. A
   villager can spawn into a dark corner of the claim at night.
9. **`Village.census` adopts any Krave Villager standing inside a claim**,
   including one a player spawn-egged in from another village. That is deliberate
   (it makes summoned and relocated villagers work), but it means a villager
   cannot be "visiting" — it joins.
10. **Not tested against the quest module.** I deliberately did not call into
    `com.barbarajones.v2.quests` — `QuestApi` does not exist in the tree yet, and
    compiling against a class another agent is still writing would break both of
    us. The coupling is one-directional by design: the quest module calls
    `KraveVillage.tierOf(level)`. Someone has to write that call.
