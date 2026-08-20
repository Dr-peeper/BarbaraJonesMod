# Module: Krave Automation (`com.barbarajones.v2.machines`)

The late-game production chain. Seven machines, a conveyor, an extractor, a
syrup-burning power notion, five JSON recipe types, one GUI, and a narrow bridge
to the village module.

---

## 1. The one thing the orchestrator MUST wire

Add exactly one line to the `BarbaraJonesMod` constructor, alongside the other
`*.register(bus)` calls:

```java
com.barbarajones.v2.machines.KraveMachines.init(bus);
```

Nothing else is required. The creative-tab append, the client screen, the
block-entity renderer and the recipe-cache invalidation all register themselves
through `@Mod.EventBusSubscriber`, which Forge discovers by annotation scan.

**No shared file was edited by this module.** Not `ModItems`, `ModBlocks`,
`ModTabs`, `ModMenus`, `ClientSetup`, `EventHandler`, `BarbaraJonesMod`,
`en_us.json`, `build.gradle` or `build.ps1`.

## 2. Village integration - already connected, but read this

`docs/modules/village.md` still does not exist, but
`com.barbarajones.v2.village` **appeared in the tree while this module was being
written**, so I read its source and wired it directly:

`com.barbarajones.v2.machines.village.KraveVillageBridge` implements this
module's sink against `KraveVillageData.getExisting(level).containing(depotPos)`,
and on a shipment calls `village.addKrave(8 * cases)` +
`village.adjustHappiness(2 * cases)` then `data.setDirty()`.

That is how a shipment "raises the production rate". `Village.production` is
recomputed each village tick as
`(buildingProduction + professionProduction) * tierMultiplier * mood`, where
`mood = 0.5 + happiness/100`. Happiness is the only lever an outside system can
pull, and it drifts one point per village tick back toward its building-derived
target - so a shipment is a real, decaying boost that has to be re-earned by
keeping the line running. The Krave itself lands in the stockpile immediately.

**A Depot must stand inside a village claim** (`Village.contains`, radius 56,
height +/-32). Outside one it shows "No village linked" and refuses to consume
cases.

`KraveVillageBridge` is registered from this module's own `FMLCommonSetupEvent`
subscriber, guarded on nothing else having claimed the slot - so if the village
module or the orchestrator sets its own sink first, that one wins.

**Orchestrator: nothing to do here** unless you want to override it.

### If the village API moves

`KraveVillageBridge` is the **only** file in the machines module that imports
anything from the village module; everything else goes through `VillageLink`.
If the village API changes and this stops compiling, deleting that one file plus
the `VillageWiring` subscriber in `MachinesEvents` leaves the machines fully
functional - the Depot simply reports "No village linked".

### The two generic ways in, still available

**Option A - the sink interface (explicit).** In common setup:

```java
com.barbarajones.v2.machines.village.VillageLink.setSink(
    new com.barbarajones.v2.machines.village.VillageProductionSink() {
        @Override
        public boolean acceptShipment(ServerLevel level, BlockPos depotPos, int cases) {
            // raise the nearest village's production rate by `cases`
            return true;   // false makes the Depot stall instead of shipping
        }

        @Override
        public boolean isVillageInRange(ServerLevel level, BlockPos depotPos) {
            return true;   // false makes the Depot show "No village linked"
        }
    });
```

**Option B - the Forge event (zero wiring).** The village module adds a
subscriber; nothing central changes at all:

```java
@SubscribeEvent
public static void onKraveShipped(KraveDeliveredEvent event) {
    // event.getLevel(), event.getDepotPos(), event.getCases()
    event.setHandled();   // required, or the Depot treats the shipment as refused
}
```

**If no sink and no event handler claims a shipment**, the Krave Depot accepts
Cases of Krave, runs its progress bar, finds nobody willing to take the shipment,
and **stops** - showing "No village linked" in its GUI. It never destroys the
case. That is deliberate: a silent item sink is the worst failure mode an
automation mod can have.

Interface: `com.barbarajones.v2.machines.village.VillageProductionSink`
Bridge: `com.barbarajones.v2.machines.village.VillageLink`
Event: `com.barbarajones.v2.machines.village.KraveDeliveredEvent`

---

## 3. The production chain

```
Krave Pods on a Krave tree
        |  Cocoa Plantation (radius 6, height +/-5, harvests + replants)
        v
   cocoa beans
        |  Krave Grinder      barbarajones:grinding
        v
  barbarajones:krave_dust                  (existing item, from ModItems)
        |  Krave Mixer        barbarajones:mixing     + krave_milk + sugar
        v
  barbarajones:krave_batter                (new)
        |  Krave Extruder     barbarajones:extruding
        v
  barbarajones:raw_krave_piece             (new)
        |  Krave Toaster      barbarajones:toasting
        v
  barbarajones:krave_cereal                (existing item, from ModItems)
        |  Krave Boxer        barbarajones:boxing     + krave_carton
        v
  barbarajones:boxed_krave    "Case of Krave"        (new)
        |  Krave Depot
        v
     the village
```

Every machine burns **Krave Syrup**. No energy API, no cables: a fuel slot and a
counter of remaining syrup units, spent one per tick of progress, exactly like a
furnace burns coal.

---

## 4. Registered ids

All in the `barbarajones` namespace.

### Blocks (and their BlockItems, same id)

| id | class | notes |
|---|---|---|
| `cocoa_plantation` | `MachineBlock` (kind `PLANTATION`) | radius farm |
| `krave_grinder` | `MachineBlock` (kind `GRINDER`) | 1 input |
| `krave_mixer` | `MachineBlock` (kind `MIXER`) | 3 inputs |
| `krave_extruder` | `MachineBlock` (kind `EXTRUDER`) | 1 input |
| `krave_toaster` | `MachineBlock` (kind `TOASTER`) | 1 input |
| `krave_boxer` | `MachineBlock` (kind `BOXER`) | 2 inputs |
| `krave_depot` | `MachineBlock` (kind `DEPOT`) | no fuel, no output slot |
| `krave_conveyor` | `KraveConveyorBlock` | 3px belt, 4 lanes, 20 ticks/block |
| `krave_extractor` | `KraveExtractorBlock` | 6-way pump, 8 items / 10 ticks |

### Items

| id | notes |
|---|---|
| `dense_krave_syrup` | fuel, 8000 units |
| `krave_batter` | Mixer out / Extruder in |
| `raw_krave_piece` | Extruder out / Toaster in; edible (1 nutrition) |
| `krave_carton` | Boxer input, crafted from paper |
| `boxed_krave` | "Case of Krave", the shippable end product |

**`barbarajones:krave_syrup` is NOT registered by this module.** The economy
module (`com.barbarajones.v2.economy.KraveEconomy.KRAVE_SYRUP`) owns that id,
its texture, its lang key and its crafting recipe, and `docs/modules/
krave-economy.md` explicitly names it as the id the machines module should build
on. `KraveFuels` imports and reads theirs.

I hit this the hard way: my texture script had originally drawn its own
`textures/item/krave_syrup.png` and **overwrote theirs**. That is fixed - the
drawing block is gone from `tools/make_krave_machines.ps1` and I re-ran
`tools/make_economy.ps1` to restore their bottle. If you are diffing and see that
file touched, that is why; it is back to what their script produces.

One file I did add on their behalf: `models/item/krave_syrup.json`
(`item/generated` + `layer0: barbarajones:item/krave_syrup`). Their doc does not
list it and it did not exist, so without it their syrup would render as a missing
model. Content is the standard one-liner - if the economy agent adds their own,
keep either, they are the same file.

### Block entity types

| id | class | valid blocks |
|---|---|---|
| `krave_machine` | `MachineBlockEntity` | all seven machine blocks |
| `krave_conveyor` | `KraveConveyorBlockEntity` | `krave_conveyor` |
| `krave_extractor` | `KraveExtractorBlockEntity` | `krave_extractor` |

### Menu type

`krave_machine` -> `MachineMenu` / `MachineScreen`. One menu serves all seven
machines; slot count and geometry come from the `MachineKind` the client reads
off the block entity at the position sent in the open packet.

### Recipe types and serializers

`grinding`, `mixing`, `extruding`, `toasting`, `boxing` - each registered in both
`ForgeRegistries.RECIPE_TYPES` and `ForgeRegistries.RECIPE_SERIALIZERS` under the
same name. All five build the same `MachineRecipe` class; the serializer injects
the type, which is what lets one class back five separate, separately
datapack-able recipe types.

Ingredient limits enforced at load: grinding/extruding/toasting exactly 1,
boxing 1-2, mixing 2-3.

### JSON recipe format

```json
{
  "type": "barbarajones:mixing",
  "ingredients": [
    { "item": "barbarajones:krave_dust", "count": 2 },
    { "item": "barbarajones:krave_milk" },
    { "item": "minecraft:sugar" }
  ],
  "result": { "item": "barbarajones:krave_batter", "count": 2 },
  "time": 160,
  "fuel_per_tick": 1
}
```

`count` on an ingredient defaults to 1; `time` defaults to 120; `fuel_per_tick`
defaults to 1. Ingredients may be tags or arrays - anything vanilla
`Ingredient.fromJson` accepts. Matching is order-independent (backtracking
assignment), so the three Mixer slots can be filled in any order.

---

## 5. Files added

```
src/main/java/com/barbarajones/v2/machines/
    KraveMachines.java            registries + init(bus)
    MachineKind.java              the seven variants as data
    MachineSlots.java             the shared 5-slot layout
    KraveFuels.java               syrup burn values
    MachinesEvents.java           creative tab + server recipe-cache invalidation
    block/MachineBlock.java
    block/KraveConveyorBlock.java
    block/KraveExtractorBlock.java
    blockentity/MachineBlockEntity.java
    blockentity/MachineProcess.java
    blockentity/MachineProcesses.java   recipe / plantation / depot behaviour
    blockentity/KraveConveyorBlockEntity.java
    blockentity/KraveExtractorBlockEntity.java
    inventory/MachineItemHandler.java
    inventory/SidedItemView.java
    menu/MachineMenu.java
    recipe/SizedIngredient.java
    recipe/MachineContainer.java
    recipe/MachineRecipe.java
    recipe/MachineRecipeSerializer.java
    recipe/RecipeLookup.java
    client/MachineScreen.java
    client/KraveConveyorRenderer.java
    client/MachinesClient.java
    village/VillageProductionSink.java
    village/VillageLink.java
    village/KraveDeliveredEvent.java
    village/KraveVillageBridge.java   the ONLY import of com.barbarajones.v2.village

tools/make_krave_machines.ps1     42 textures, all read back and verified

assets/barbarajones/lang/machines.json          <- orchestrator merges into en_us.json
assets/barbarajones/blockstates/                9 files
assets/barbarajones/models/block/               16 files (7 machines x2, conveyor, extractor)
assets/barbarajones/models/item/                15 files (incl. krave_syrup.json, see note above)
assets/barbarajones/textures/block/             22 png + 8 mcmeta
assets/barbarajones/textures/item/              5 png
assets/barbarajones/textures/gui/container/     7 png (256x256)
data/barbarajones/loot_tables/blocks/           9 files
data/barbarajones/recipes/machines/             17 files (6 machine recipes, 11 crafting)
data/minecraft/tags/blocks/mineable/pickaxe.json  <- SEE MERGE NOTE BELOW
```

### MERGE NOTE - `data/minecraft/tags/blocks/mineable/pickaxe.json`

This file **did not exist** in the tree and I created it, listing the nine blocks
this module adds. Only `shovel.json` was there before. If another agent also
creates it, the two must be merged by hand - a tag file path can only exist once
per namespace per mod. `"replace": false` is set, so it merges cleanly with any
other mod's copy, just not with a second copy of ours.

---

## 6. Automation behaviour worth knowing

- **Capabilities.** Every machine exposes `ForgeCapabilities.ITEM_HANDLER`.
  `Direction.DOWN` gives an extract-only view of the output slot; every other
  face gives insert on the live inputs and the fuel slot plus extract on the
  output. Fuel is deliberately insert-only, so a hopper on a machine's side
  cannot drain back out the syrup it just fed in. Vanilla hoppers therefore work
  exactly as players expect: on top feeds, underneath collects.
- **Auto-eject.** A machine pushes its output into whatever it *faces*, every 8
  ticks, only into a real `IItemHandler`. It never drops items on the ground. The
  front is the face with the mechanism on it, and machines are placed facing the
  player, exactly like a furnace.
- **Conveyor.** Four lanes, one second per block. Items physically travel and are
  rendered sliding by `KraveConveyorRenderer`; a stack is only offered to the
  next block once it has finished crossing. Belts back up rather than spilling.
  Right-click with an empty hand rotates one.
- **Extractor.** Pulls from the block behind, pushes into the block in front,
  8 items every 10 ticks. Holds nothing itself. Simulates the insert before
  committing the extract, and puts items back if a downstream handler lies about
  its capacity.
- **Comparators.** Every machine emits an analog signal from its output slot's
  fill level (the Depot from its input slot).
- **Recipe cache.** `RecipeLookup` indexes recipes per type and rebuilds only on
  `AddReloadListenerEvent` (server) / `RecipesUpdatedEvent` (client). Block
  entities cache the matched recipe next to the generation they matched it in and
  re-scan only when their inputs change. No `getRecipeFor` call per machine per
  tick.
- **Input filtering.** Recipe machines reject any item no loaded recipe of their
  type uses, so a hopper cannot jam a Grinder full of cobblestone.

## 7. Animation

- Each machine has a two-frame `*_front_on.png` (16x32 strip + `.mcmeta`,
  frametime 6) selected by the `running` blockstate property, so a working
  machine visibly pulses and an idle one does not - and it still looks right to a
  player who logs in next to it, which block-entity-only state would not.
- The conveyor belt texture is a four-frame scrolling strip (16x64, frametime 2),
  and rotates with the block so the tread always runs the way items travel.
- `KraveConveyorRenderer` draws the cargo sliding along, interpolated with the
  partial tick.

---

## 8. What I did NOT finish - read this

1. **Nothing was compiled.** Per the hard rule I did not run Gradle. Every API
   used was verified against the actual mapped Forge 1.20.1 jar
   (`forge-1.20.1-47.2.0_mapped_official_1.20.1.jar`) by reading class constant
   pools - `CocoaBlock.MAX_AGE`, `ItemStack.copyWithCount`,
   `ShapedRecipe.itemStackFromJson`, `RecipeType.simple`,
   `ItemRenderer.renderStatic`'s exact descriptor, `BlockBehaviour.Properties
   .pushReaction`, `ContainerHelper.saveAllItems`, the Forge event class
   locations, and so on. That is not the same as a green build. **First compile
   is likely to surface something.**

2. **The village link is wired but never run.** `KraveVillageBridge` was written
   against `com.barbarajones.v2.village` source that appeared mid-task and is
   presumably still being edited by another agent. I read `Village.java` and
   `KraveVillageData.java` and used only `getExisting`, `containing`, `addKrave`,
   `adjustHappiness` and `setDirty`, all of which are public and documented in
   their own javadoc - but that module has no `docs/modules/village.md` yet, its
   API is not frozen, and **nobody has ever executed this path**. If the build
   breaks on an import from `com.barbarajones.v2.village`, it is this one file,
   and section 2 says exactly how to cut it loose.

   The balance numbers (8 stockpile and 2 happiness per case) are a guess. I had
   no design brief for what a case should be worth.

3. **No JEI / recipe-viewer integration.** The five recipe types are invisible to
   JEI; a player learns the chain from the GUIs and this document. `MachineRecipe
   .isSpecial()` returns true so they also stay out of the vanilla recipe book,
   which is correct (they are not hand-craftable) but means there is no in-game
   discovery path for the chain at all. A Krave Manual page or a JEI plugin is
   the obvious follow-up.

4. **No custom sounds.** Machines use vanilla `SoundEvents` (`CROP_BREAK` on
   harvest, `ITEM_PICKUP` on shipment, `ITEM_FRAME_ROTATE_ITEM` on belt rotation)
   and are otherwise silent. I did not touch `sounds.json` or `ModSounds` because
   both are shared. A running-machine hum would help a lot and is a clean
   follow-up: add the entries, then call `level.playSound` from
   `MachineBlockEntity.serverTick` when `running` is true and the tick counter is
   a multiple of the loop length.

5. **The Plantation's radius scan is not configurable.** `RADIUS = 6`,
   `HEIGHT = 5`, `RESCAN_INTERVAL = 60` are constants in
   `MachineProcesses.PlantationProcess`. That is a 13x11x13 box, about 1,850
   block lookups, at most once every three seconds per plantation. Fine for a
   handful; a player who builds forty plantations will feel it. If this needs
   tuning it should move into `Config.java`, which I did not touch.

6. **Conveyors do not merge partial stacks in a lane.** Each lane holds one stack
   and inserting into an occupied lane fails, so a belt fed single items one at a
   time carries at most four items at once rather than four full stacks. That is
   intentional (it is what makes the belt look loaded), but it does mean belt
   throughput is items-per-second, not stacks-per-second, and a fast Extruder can
   outrun a single belt.

7. **No blockstate for "belt connects to next belt".** Belts are visually
   independent blocks; there is no seam-joining model. They line up because they
   are the same height, not because they know about each other.

8. **Depot shipping is one case per 40 ticks, hard-coded**
   (`MachineProcesses.DepotProcess.SHIP_TIME`) and the lifetime shipped counter is
   per-depot NBT shown in its GUI. There is no global "total shipped" statistic
   and no advancement hooked to it.

9. **`raw_krave_piece` is edible but `krave_batter` and `boxed_krave` are not.**
   If the design wants Cayden to be able to eat a whole case, that is a one-line
   `.food(...)` change in `KraveMachines`.
