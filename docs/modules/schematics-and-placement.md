# Schematics & Structure Placement

Package: `com.barbarajones.v2.build`

The technical core of 2.0. A schematic item, a ghost preview, a terrain-aware
placement engine that builds over a few seconds, and a code-defined structure
format. No `.nbt` files anywhere - buildings are Java, so they diff and review
like Java.

---

## 1. Wiring (orchestrator)

**One line in `BarbaraJonesMod`'s constructor:**

```java
com.barbarajones.v2.build.KraveBuild.init(bus);
```

That registers three `DeferredRegister`s and the module's own network channel.
Everything else in the module registers itself:

| Thing | Where it registers itself |
|---|---|
| Build tick loop, level-unload flush | `place.BuildScheduler` (`@Mod.EventBusSubscriber`, FORGE) |
| Player-placed block ledger | `place.PlayerBuiltLedger.Hooks` (FORGE) |
| Left-click-to-rotate | `item.SchematicInput` (FORGE) |
| Ghost preview + client tick | `client.GhostPreviewRenderer` (FORGE, `Dist.CLIENT`) |
| Rotate keybind | `KraveBuild.ClientBootstrap` (MOD, `Dist.CLIENT`) |
| Creative tab contents | `KraveBuild.Tabs` (MOD) |

Nothing needs adding to `ModItems`, `ModBlocks`, `ModTabs`, `ClientSetup`,
`EventHandler` or `ModNetwork`. None of those files were touched.

**Lang:** `assets/barbarajones/lang/v2_build.json` (31 keys). Merge as usual. The key
 category reuses the mod's existing `KraveKeys.CATEGORY`, so no duplicate
 `key.categories.barbarajones` entry is contributed.

**Textures:** `tools/make_v2_build.ps1`. Writes and then pixel-verifies
`textures/item/krave_schematic.png` and `textures/block/krave_core.png`. Already
run; both files are on disk.

### Registered ids

| Registry | Id | Class |
|---|---|---|
| Item | `barbarajones:krave_schematic` | `item.KraveSchematicItem` |
| Block | `barbarajones:krave_core` | `block.KraveCoreBlock` |
| BlockEntity | `barbarajones:krave_core` | `block.KraveCoreBlockEntity` |
| Network channel | `barbarajones:v2_build` | `net.BuildNetwork` |
| SavedData | `barbarajones_player_built` (per level) | `place.PlayerBuiltLedger` |
| Block tag | `barbarajones:build_clearable` | data |
| Block tag | `barbarajones:build_protected` | data |
| Recipe | `krave_schematic.json` (2 paper + 1 krave_cereal) | data |
| Keybind | `key.barbarajones.rotate_schematic` (R) | `client.BuildKeys` |

`krave_core` has **no** BlockItem on purpose - it is not an obtainable block.
Its loot table is empty.

---

## 2. Player-facing behaviour

- **Right-click ground** with a schematic: unfolds it. A translucent footprint
  appears where you are pointing.
  - green = flat ground, ready
  - amber = the engine will shave this column down
  - blue = the engine will pack this column up
  - red = blocked, too steep, or hanging over nothing
  - a cyan lip marks the **front** (the door side)
  - a wireframe shows the building's real height
- **Left-click, or R**: rotates 90 degrees.
- **Sneak + right-click**: commits. The building goes up over ~3.5 seconds from
  the ground up, with the placement sound of whatever block just landed.
- **Break the Krave Foundation Stone within 60 seconds**: the building comes
  back down and you get the schematic back.

The preview is entirely client-side. The client owns the same `StructureDef`
objects and runs the same `KraveStructure.check` against its own copy of the
world, so it tracks the crosshair with zero latency and agrees with the server
because it is literally the same code. No packets are involved in the preview.

---

## 3. `StructureDef` - the format you write buildings in

### 3.1 Coordinate system - read this bit twice

```
        +Y  up
         |
         |     +Z  the FRONT of the building (the side the door faces)
         |    /
         |   /
         +--------- +X  right
```

- **`y = 0` is your floor** - the first block above the levelled ground.
- **`y = -1` is the ground surface block itself.** A floor slab at `y = -1` sits
  flush with the terrain; a foundation at `y = -2` is buried. Negative Y is
  normal and expected.
- **`Rotation.NONE` means the front faces SOUTH**, matching vanilla's structure
  convention. The engine picks the rotation that turns the front towards the
  player, so you never think about world directions - only about local +Z.
- **The bounding box is derived from the blocks you actually draw.** There is no
  size declaration to keep in sync. `minSize(x,y,z)` only *widens* it, for when
  you want clearance the building does not fill (a porch, a garden plot).

### 3.2 The palette

Characters map to block states. Every entry is a `Supplier`, **never a resolved
`BlockState`** - definitions are static fields constructed during mod
construction, long before this mod's own blocks exist in the registry. Calling
`ModBlocks.FOO.get()` there throws. The overloads below make the safe thing the
easy thing.

Two characters are **reserved and cannot be redefined**:

| Char | Meaning |
|---|---|
| `.` | KEEP - this position is not part of the plan. Removes it if an earlier op wrote there. |
| `' '` (space) | AIR - carve this position out. |

> Note on `.` inside the footprint: the engine levels the whole footprint from
> the build plane upwards, so a `.` position inside the building's own box ends
> up as **air**, not as preserved terrain. `.` is for punching holes in
> previously drawn boxes, not for preserving world blocks. If you need terrain
> genuinely untouched, keep it outside the footprint or use
> `ground(GroundMode.FLOAT)`.

```java
Palette.Builder b = Palette.builder();

b.block('#', Blocks.STONE_BRICKS);                  // vanilla block, direct
b.block('=', () -> ModBlocks.CHOCOLATE_PLANKS.get()); // our block, lazy - USE THIS ONE
b.state('s', () -> Blocks.OAK_STAIRS.defaultBlockState()
        .setValue(StairBlock.FACING, Direction.SOUTH));
b.state('g', Blocks.GLASS.defaultBlockState());     // vanilla state, direct
b.random('~', Blocks.BROWN_TERRACOTTA, Blocks.PACKED_MUD);   // uniform pick per block
b.weighted('r', List.of(Palette.weight(8, () -> Blocks.BROWN_TERRACOTTA.defaultBlockState()),
                        Palette.weight(1, () -> Blocks.BROWN_CONCRETE.defaultBlockState())));
b.fixed('N', () -> someState);   // will NOT be rotated with the building - rare
char oneOff = b.auto(() -> someState);  // private char for a state you don't want to name

Palette shared = b.build();
```

**Rotation:** every entry gets `BlockState.rotate(rotation)` applied at
placement. That is what makes stairs, doors, logs and anything with a facing
property come out right in all four orientations. Author everything as if the
building faces south and it will be correct in every direction.

Palettes can be shared across buildings: `.palette(SHARED)` on the def builder
starts from a copy you can then add to. Do that - ten buildings that share one
palette read as one village.

### 3.3 Drawing primitives

All coordinates are local and all box ranges are **inclusive**. Ops apply in the
order you add them; later ops overwrite earlier ones at the same position.

| Method | Draws |
|---|---|
| `set(x, y, z, char)` | one block |
| `fill(x1,y1,z1, x2,y2,z2, char)` | solid box |
| `hollow(x1,y1,z1, x2,y2,z2, char)` | box shell, all six faces |
| `walls(x1,y1,z1, x2,y2,z2, char)` | the four vertical faces only - no floor, no ceiling |
| `frame(x1,y1,z1, x2,y2,z2, char)` | the twelve edges only - corner posts and beams |
| `column(x, z, y1, y2, char)` | a vertical run at one column |
| `line(x1,y1,z1, x2,y2,z2, char)` | 3D line between two points |
| `carve(x1,y1,z1, x2,y2,z2)` | fills the box with AIR |
| `keep(x1,y1,z1, x2,y2,z2)` | removes the box from the plan |
| `layer(y, String... rows)` | one horizontal ASCII layer |
| `layers(yStart, String[]... layers)` | several stacked layers, `layers[0]` at `yStart` |
| `scatter(x1,y1,z1, x2,y2,z2, char, chance)` | probabilistic sprinkle, deterministic per definition |
| `door(x, y, z, Supplier<Block>)` | both halves, facing the front, left hinge |
| `door(x, y, z, Supplier<Block>, Direction localFacing, boolean rightHinge)` | both halves, your way |
| `bed(x, y, z, Supplier<Block>, Direction localFacing)` | foot at `(x,y,z)`, head one along `localFacing` |
| `op(StructureOp)` | escape hatch for a generated shape |

**`layer` row order, precisely:**

- `rows[0]` is `z = 0`, which is the **back** of the building.
- The **last** row is the front - so the door appears on the bottom row, and you
  draw the layer the way you would read a floor plan.
- Character index within a row is `x`, left to right.
- Rows may be ragged; any position past the end of a row is treated as `.`.

`scatter` hashes the position with the structure id rather than rolling a
random source, so a definition always bakes to the same plan. That keeps
buildings diffable and keeps the ghost preview honest.

### 3.4 Placement behaviour

| Method | Default | What it does |
|---|---|---|
| `anchor(Anchor)` | `CENTER` | Where the footprint sits relative to the block the player pointed at |
| `ground(GroundMode)` | `LEVEL` | How the terrain is treated |
| `maxGroundDelta(int)` | `4` | How much height spread is still levellable |
| `foundation(Supplier<BlockState>)` | match the terrain | Block packed under low columns |
| `buildTicks(int)` | `70` | Roughly how long the animation runs (clamped 10-400) |
| `minSize(x, y, z)` | derived | Widens the footprint beyond what you drew |
| `core(x, y, z)` | centre of footprint at `y = -1` | Where the Foundation Stone goes |
| `marker(name, x, y, z)` | - | A named local position you can look up later |
| `name(String)` | `building.<namespace>.<path>` | Translation key for the display name |

**`Anchor`**
- `CENTER` - footprint centred on the anchor. Least surprising, but the player
  may end up inside it.
- `CORNER` - the anchor is the footprint's minimum X/Z corner after rotation.
- `FRONT` - **use this for houses.** The front edge sits on the anchor and the
  building extends away from the player, so the door lands in front of them.

**`GroundMode`**
- `LEVEL` - cut high columns down, pack low ones up, refuse if the spread
  exceeds `maxGroundDelta`.
- `STRICT` - refuse unless the ground is already dead flat.
- `FLOAT` - do not touch the terrain at all; the anchor Y is the floor. For
  bridges, towers, sky platforms.

### 3.5 Completion hooks

Run on the server on the tick the last block lands. A hook that throws is logged
and skipped - it cannot leave a building half-initialised.

```java
.onComplete((ServerLevel level, PlacementContext ctx) -> { ... })

.spawn(x, y, z, ModEntities.CAYDEN::get)
.spawn(x, y, z, ModEntities.CAYDEN::get, (entity, ctx) -> { /* configure it */ })

.blockEntity(x, y, z, (be, ctx) -> { /* fill a chest, name a sign */ })
```

`PlacementContext` gives you:

| Method | Returns |
|---|---|
| `world(int lx, int ly, int lz)` / `world(BlockPos local)` | local -> world |
| `marker(String name)` | world position of a marker, or null |
| `core()` | world position of the Foundation Stone |
| `origin()` | min X/Z corner of the rotated footprint, at the build plane |
| `anchor()` | the block the player pointed at |
| `rotation()`, `front()` | orientation |
| `placer()` | UUID of whoever placed it, or null |
| `level()`, `def()` | the obvious |
| `trackSpawned(Entity)` | register an entity so an undo removes it too |

Entities spawned through `.spawn(...)` are tracked automatically, so an undo
removes them again rather than leaving an orphan standing in a field. **Cayden
and players are never removed by an undo** - rule one of this mod.

---

## 4. Worked example - a complete building

This compiles against the API exactly as written (modulo your own `ModBlocks`
/ `ModItems` / `ModEntities` names). A 7x7 shack with a door, two windows, a
bed, a stocked chest, a two-step roof and a resident.

```java
package com.barbarajones.v2.house;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModItems;
import com.barbarajones.v2.build.def.Palette;
import com.barbarajones.v2.build.def.StructureDef;
import com.barbarajones.v2.build.def.StructureRegistry;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

public final class HouseStructures {

    private HouseStructures() { }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(BarbaraJonesMod.MODID, path);
    }

    /** Shared by every building so the whole settlement reads as one place. */
    public static final Palette VILLAGE_PALETTE = Palette.builder()
            .block('=', Blocks.SPRUCE_PLANKS)          // walls
            .block('|', Blocks.SPRUCE_LOG)             // corner posts
            .block('_', Blocks.SPRUCE_PLANKS)          // floor
            .block('o', Blocks.GLASS_PANE)             // windows
            .block('L', Blocks.LANTERN)                // light
            .block('C', Blocks.CHEST)                  // storage
            .random('~', Blocks.BROWN_TERRACOTTA,      // crumbly cereal roof
                         Blocks.PACKED_MUD,
                         Blocks.BROWN_GLAZED_TERRACOTTA)
            .build();

    public static final StructureDef STARTER_SHACK = StructureRegistry.register(
            StructureDef.builder(id("starter_shack"))
                    .palette(VILLAGE_PALETTE)
                    .anchor(StructureDef.Anchor.FRONT)   // door lands facing the player
                    .maxGroundDelta(3)
                    .buildTicks(70)

                    // ---- floor, sunk one into the ground so it never floats ----
                    .fill(0, -1, 0, 6, -1, 6, '_')

                    // ---- walls three high, logs on the corners ----
                    .walls(0, 0, 0, 6, 2, 6, '=')
                    .column(0, 0, 0, 2, '|')
                    .column(6, 0, 0, 2, '|')
                    .column(0, 6, 0, 2, '|')
                    .column(6, 6, 0, 2, '|')

                    // ---- windows: two on the front, one each side ----
                    .set(1, 1, 6, 'o')
                    .set(5, 1, 6, 'o')
                    .set(0, 1, 3, 'o')
                    .set(6, 1, 3, 'o')

                    // ---- door in the middle of the front wall ----
                    // added AFTER walls(), so it overwrites the planks there
                    .door(3, 0, 6, () -> Blocks.SPRUCE_DOOR)

                    // ---- roof, two stepped layers. rows[0] is the BACK (z=0) ----
                    .layer(3, "~~~~~~~",
                              "~~~~~~~",
                              "~~~~~~~",
                              "~~~~~~~",
                              "~~~~~~~",
                              "~~~~~~~",
                              "~~~~~~~")
                    .layer(4, ".......",
                              ".~~~~~.",
                              ".~~~~~.",
                              ".~~~~~.",
                              ".~~~~~.",
                              ".~~~~~.",
                              ".......")
                    .layer(5, ".......",
                              ".......",
                              "..~~~..",
                              "..~~~..",
                              "..~~~..",
                              ".......",
                              ".......")

                    // ---- furniture ----
                    .bed(1, 0, 1, () -> Blocks.RED_BED, Direction.SOUTH)  // foot (1,0,1), head (1,0,2)
                    .set(5, 0, 5, 'L')
                    .set(5, 0, 1, 'C')

                    // ---- the undo block, under the middle of the floor ----
                    .core(3, -1, 3)

                    // ---- named positions other systems can find again ----
                    .marker("bed", 1, 0, 1)
                    .marker("door", 3, 0, 6)
                    .marker("hearth", 3, 0, 3)

                    // ---- completion hooks ----
                    .blockEntity(5, 0, 1, (be, ctx) -> {
                        if (be instanceof ChestBlockEntity chest) {
                            chest.setItem(0, new ItemStack(ModItems.KRAVE_CEREAL.get(), 8));
                        }
                    })
                    .spawn(3, 0, 3, ModEntities.CAYDEN::get, (entity, ctx) -> {
                        entity.setCustomName(net.minecraft.network.chat.Component.literal("Cayden"));
                    })
                    .onComplete((level, ctx) -> {
                        // e.g. tell the settlement tracker a house now exists here
                        // KraveVillageData.get(level).addBuilding(ctx.def().id(), ctx.core(), ctx.rotation());
                    })

                    .build());
}
```

**Register it during your module's `init(bus)`** - touching the class is enough,
since the fields are static. Anything later and the creative tab will have been
built without your schematics in it.

```java
public static void init(IEventBus bus) {
    ITEMS.register(bus);
    HouseStructures.class.getName();   // or just reference STARTER_SHACK somewhere
}
```

### 4.1 Giving it its own schematic item

Two ways, both supported.

**Dedicated item** (recommended - own texture, own name, own recipe):

```java
public static final RegistryObject<Item> SHACK_SCHEMATIC = ITEMS.register("schematic_starter_shack",
        () -> new KraveSchematicItem(new Item.Properties().stacksTo(16),
                new ResourceLocation(BarbaraJonesMod.MODID, "starter_shack")));
```

The constructor only takes the id, not the `StructureDef`, so item registration
and structure registration can happen in either order. A dedicated item is
remembered by id, so a refund hands back **your** item, not the generic one.

Then the recipe, `data/barbarajones/recipes/schematic_starter_shack.json` -
build every building's schematic out of the blank one so the blank has a job:

```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    { "item": "barbarajones:krave_schematic" },
    { "item": "minecraft:spruce_planks" },
    { "item": "barbarajones:krave_cereal" }
  ],
  "result": { "item": "barbarajones:schematic_starter_shack", "count": 1 }
}
```

**Shared item with NBT** (good for `/give`, loot tables, quest rewards):

```java
ItemStack stack = KraveSchematicItem.forStructure(HouseStructures.STARTER_SHACK);
```

### 4.2 Lang keys you must add

Per building, one key, auto-derived as `building.<namespace>.<path>` unless you
override it with `.name(...)`:

```json
"building.barbarajones.starter_shack": "Starter Shack"
```

Plus one per dedicated schematic item:

```json
"item.barbarajones.schematic_starter_shack": "Starter Shack Schematic"
```

(The shared item shows `"<Building> Schematic"` automatically via
`item.barbarajones.krave_schematic.named`.)

---

## 5. `KraveStructure` - placing from code

```java
PlacementResult result = KraveStructure.place(level, anchor, rotation, def);
```

| Signature | Use |
|---|---|
| `place(ServerLevel, BlockPos, Rotation, StructureDef)` | the one you asked for |
| `place(..., @Nullable ServerPlayer placer)` | records who did it, so undo refunds them |
| `place(..., @Nullable ServerPlayer, boolean instant)` | `instant` skips the animation - worldgen and commands only |
| `check(LevelReader, BlockPos, Rotation, StructureDef)` | validate without touching the world; safe on the client |
| `anchorFor(BlockHitResult)` / `anchorFor(BlockPos, Direction)` | crosshair -> anchor block |

`PlacementResult`:
- `started()` - validation passed and the build job is running. It does **not**
  mean the last block has landed; hook completion with `.onComplete(...)`.
- `message()` - always populated, always safe to hand straight to a player. On
  failure it names the block that is in the way and its coordinates.
- `check()` - the full validation, including per-column detail.

### What happens on a successful placement

1. **Validate (pure).** Scan every footprint column for its ground height, take
   the median as the build plane, classify each column (OK / CUT / FILL /
   TOO_STEEP / NO_GROUND), then scan every block the placement would actually
   write for anything protected.
2. **Refuse or commit.** If anything failed, nothing is written at all. There is
   no code path that writes half a building and then discovers a problem.
3. **Clear the footprint of living things** - players and mobs inside are nudged
   to the nearest outside edge before walls appear around them.
4. **Build**, a few blocks per tick over `buildTicks`: terrain scraped top-down,
   hollows packed bottom-up, then the building itself layer by layer from the
   ground, each layer spreading outwards from the middle. Every block plays its
   own placement sound and puffs its own break particles.
5. **Settle.** One pass of shape and neighbour updates over everything written.
   Blocks are laid with `UPDATE_CLIENTS` only during the build - with neighbour
   updates on, a door's lower half spends a tick alone and `DoorBlock.updateShape`
   deletes it before the upper half arrives. Same order vanilla's own structure
   placer uses.
6. **Place the Foundation Stone**, storing the undo snapshot, and run the hooks.

If the server stops or the level unloads mid-build, `BuildScheduler` force-runs
every outstanding job to completion first. A killed server still saves a
finished building.

### What counts as "in the way"

`place.TerrainRules`, in order:

1. Block tag `barbarajones:build_protected` - always refuse.
2. `PlayerBuiltLedger` hit - a player physically placed this block. Always
   refuse, even for dirt.
3. Air, or block tag `barbarajones:build_clearable` - clear.
4. Lava - refuse, with its own message.
5. Anything with a block entity - refuse. Chests, spawners, other buildings.
6. Natural tags (`DIRT`, `BASE_STONE_OVERWORLD`, `SAND`, `LOGS`, `LEAVES`,
   `TERRACOTTA`, `SNOW`, `ICE`, `NYLIUM`, `MOSS_REPLACEABLE`, ...) and a list of
   loose natural blocks - clear.
7. The mod's own terrain blocks (`krave_dirt`, `krave_grass`, `krave_ore`,
   `chocolate_log`, ...) - clear.
8. No collision box (plants, vines, cobwebs) - clear.
9. Everything else - refuse.

Both tags are the escape hatch: a wrong call here is a one-line datapack fix,
not a code change.

---

## 6. Refund / undo

The Krave Foundation Stone (`barbarajones:krave_core`) carries the building's
identity and an undo snapshot: every block the placer overwrote, as block-state
ids over the building's bounding box, run-length encoded. Terrain is repetitive
and untouched runs collapse to one pair, so a house that modified four thousand
positions typically stores a couple of hundred integers.

Breaking it **within 60 seconds** (`KraveBuild.REFUND_WINDOW_TICKS`), **as the
player who placed it**, restores the snapshot top-down over about a second,
removes any entities the hooks spawned (never Cayden, never players), and hands
back the schematic. Right-clicking the stone reports how long is left.

Breaking it after the window is just breaking a block; the building stays.

**Stated caveat:** block-state ids are assigned at runtime from the loaded block
registry. They are stable for the life of a world whose mod list does not
change, which comfortably covers a 60-second window. If the block count changes
between placement and undo, the snapshot is treated as stale and the refund is
refused rather than restoring wrong blocks.

---

## 7. Rules for building authors

1. **Never resolve a `BlockState` eagerly for one of this mod's blocks.**
   `.block('=', () -> ModBlocks.X.get())`, not `.block('=', ModBlocks.X.get())`.
   Vanilla blocks are fine either way.
2. **A palette character you never defined is a hard crash at load**, naming the
   character and the exact local coordinate. That is deliberate - a typo that
   silently leaves a hole in a wall costs far more than a loud startup failure.
3. **Draw the door after the wall.** Ops are painted in order.
4. **Put the core somewhere deliberate.** The default (buried under the middle of
   the footprint) works but is hard for a player to find on purpose. Under the
   doormat or in the hearth makes the undo a feature instead of a secret.
5. **Use `Anchor.FRONT` for anything a player walks into.**
6. **Nothing wider than 64 blocks, nothing over 60,000 blocks in the plan.**
   Both are hard limits with clear errors.
7. **Structure ids must be unique.** `StructureRegistry.register` throws on a
   duplicate - the schematic item resolves buildings by id.

---

## 8. Integration points

- **Quests** (`v2.quests`) already tracks schematic unlocks by `ResourceLocation`
  via `QuestApi.hasSchematic(player, id)`. Those ids are meant to be
  `StructureDef.id()`s. Gate your schematic recipes or your shop stock on it;
  this module deliberately does not hard-depend on the quest module.
- **Settlement/village** (`v2.village`): the natural hook is
  `.onComplete((level, ctx) -> ...)`, registering `ctx.def().id()`,
  `ctx.core()` and `ctx.rotation()` - a `BuildingRecord` shape that survives
  chunk unload without needing the building's chunks loaded.
- **Housing validation** (`com.barbarajones.housing.HousingValidator`) wants a
  real bed, a door and a light source. Use `.bed(...)` and `.door(...)` rather
  than hand-placing halves, and put a lantern or torch inside.

---

## 9. Status

### Done

- `StructureDef` / `Palette` / `StructureRegistry` with the full primitive set,
  markers, hooks, three anchor modes and three ground modes.
- `KraveStructure.place` / `.check` with the exact requested signature.
- Terrain levelling (cut and fill), refusal with per-column detail, and a hard
  guarantee that a refused placement writes nothing.
- Progressive build over `buildTicks` with per-block sounds and particles;
  deferred settle pass so doors and beds survive.
- Force-completion on level unload and server stop.
- Ghost preview: per-column colour-coded footprint, height wireframe, front
  marker, rotation by left-click or R, all client-local.
- Krave Schematic item (shared NBT-driven, plus a constructor for per-building
  items) with tooltips and a cheap recipe.
- Krave Foundation Stone with RLE undo snapshot, 60-second refund, entity
  cleanup with a Cayden exemption.
- `PlayerBuiltLedger` recording player-placed blocks, capped at 250k entries with
  graceful degradation.
- Textures generated and pixel-verified; 31 lang keys; two block tags; loot
  table; models; blockstate.

### Not done / known gaps - read these

- **NOT COMPILED.** Per the hard rules I did not run gradle, and no JDK is
  installed on this machine to even parse-check with. The code is written
  carefully against 1.20.1 Mojang mappings and cross-checked against existing
  files in this repo for every API I was unsure of (`computeIfAbsent`'s 3-arg
  form, `BuildCreativeModeTabContentsEvent`'s package, `javax.annotation.Nullable`,
  fastutil's presence in the Forge cache) - but it has never seen a compiler.
  Expect to fix something.
- **No `.place()` overload taking a `Mirror`.** Rotation only. Nothing asked for
  mirroring and it doubles the geometry surface area.
- **`.` inside the footprint becomes air, not preserved terrain.** Documented in
  3.2, but it is a real limitation: there is no per-region "do not level here"
  op. `GroundMode.FLOAT` is the only way to leave terrain alone entirely.
- **The client preview can be very slightly optimistic.** The player-built ledger
  is server-only, so a player-placed *natural* block (dirt, stone) reads green on
  the client and is refused by the server. Anything manufactured is caught by the
  block heuristic on both sides, so this is rare. The server's refusal names the
  exact block and position.
- **Rotation is player-relative.** The stack stores quarter turns applied on top
  of "face the player". If a player spins 90 degrees in the same tick they
  commit, the server's view of their facing could differ from the client's and
  the building lands a quarter turn off. Not observed, but the race exists.
- **No `/krave build` debug command.** Placing via code or the item only.
- **The undo restores blocks, not block entity contents.** A chest that was in
  the footprint blocks the placement outright (block entities are protected), so
  this only matters for contents the hooks themselves created - which the undo
  removes with the blocks anyway.
- **`BuildScheduler` holds jobs in a `WeakHashMap` keyed by level.** Correct, but
  it means an in-progress build is not persisted across a crash that skips the
  stopping event. The unload/stop flush covers every clean path.
