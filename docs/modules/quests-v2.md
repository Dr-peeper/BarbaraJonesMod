# Quest system v2 — `com.barbarajones.v2.quests`

A ground-up replacement for `com.barbarajones.quest`. The old system is **not** patched; it
is superseded, and the orchestrator has to unwire it (see *Central wiring* below).

---

## 1. What actually went wrong in the old system

I read `quest/Quests.java` (588 lines), `quest/expansion/QuestExpansion.java`,
`quest/expansion/QuestExpansionEvents.java`, `quest/expansion/QuestProgress.java`,
`item/QuestBookItem.java`, `item/KraveManualItem.java` and `client/QuestBookScreen.java`
before writing anything. These are the real failure modes, with the code that caused them.
This history is worth keeping because most of it is *not* obvious from the outside.

### 1.1 Progress lived on an ItemStack, and the client re-derived it

`Quests.markDone()` wrote a `ListTag` called `KraveDone` into the Quest Book's
`ItemStack` NBT. `QuestBookScreen` then called `Quests.findBook(Minecraft.getInstance().player)`
and read completion out of the **client's replica** of that stack.

Three separate bugs fall out of that one decision:

* **Lose the book, lose the run.** The book *is* the save file. Lava, a creeper, a death in
  a hardcore-ish moment — the questline is gone with it.
* **Two books, two questlines.** Nothing stopped a second `quest_book` existing, and each
  carried its own independent `KraveDone` list. `findBook` returned whichever came first in
  the inventory scan, so which questline you were playing depended on slot order.
* **The book showed stale state.** `ItemStack` NBT replicates to the client on the server's
  own schedule. The screen also re-derived unlock state locally
  (`Quests.isUnlocked(book, q)` runs identically on both sides), so it was a *second
  implementation* of the server's rules reading a *lagging* copy of the server's data.
  When they disagreed, the player had no way to tell which was right.

### 1.2 Quests required items that crafting consumed — verified, not hypothetical

`Quests.hasAll()` was:

```java
for (RegistryObject<Item> it : q.collect) {
    if (!player.getInventory().contains(new ItemStack(it.get()))) return false;
}
```

i.e. **"are all of these in your inventory right now"**, re-asked every 40 ticks by
`EventHandler` calling `Quests.tick()`. Combine that with the mod's own recipes and you get
quests that cannot be completed:

* `GRASS_PREP` ("Mise en Place") required `DICED_GRASS` **and** `GRASS_KNIFE` held
  simultaneously. `recipes/diced_grass.json` is
  `[handful_of_grass, grass_knife] -> 2x diced_grass`. `grass_knife` is
  `new Item(props().stacksTo(1).durability(128))` — a plain damageable `Item` with no
  crafting remainder, so shapeless crafting **destroys it**. Craft diced grass and the knife
  is gone; the only way through was to craft a second knife and never use it, which the
  objective text never said. `GRASS_DRY`, `GRASS_ROLL` and `GRASS_EDIBLES` all sat behind
  it, so one bad `collect` list stranded an entire branch.
* The same shape recurs wherever a chain step eats its own input, and the polling meant a
  quest could *un-complete itself* the moment you used the items it had just rewarded you
  for gathering. `settle()` only ever added to `KraveDone`, but a quest that had never
  managed to complete during a 2-second window simply never did.

### 1.3 Locked quests displayed nothing useful

`QuestBookScreen` drew locked quests in `ChatFormatting.DARK_GRAY` at `0x666666`. A late
patch added a "Locked - first: …" line, but it only listed the first three prerequisite
**titles** — never their objectives, never a graph, and quests behind the finale
(`missingPrereqTitles` returns *every other quest in the mod* for `PEACE`) produced a line
that was truncated into meaninglessness.

### 1.4 The completion cascade was a fixpoint loop

```java
boolean changed = true;
while (changed) {
    changed = false;
    for (Quest q : ALL) { ... }
    if (changed) announceNewlyAvailable(player, book);
}
```

Re-tests **every quest in the file** on every pass, and calls the announcement routine from
inside the loop. It is quadratic in quest count, it re-runs on a 40-tick timer forever, and
`announceNewlyAvailable` was made idempotent only by a second NBT list (`KraveSeen`) — so a
book that lost its `KraveSeen` list re-announced everything. It also has no cycle guard;
`QuestExpansion` splices its own `Spec` list into `Quests.ALL` at static-init time, and
nothing anywhere checked that those `prereqs` strings named quests that existed.

### 1.5 Incidental findings (not mine to fix, but worth someone's time)

* `data/barbarajones/recipes/krave_tree_door.json` produces
  `barbarajones:krave_wood_door`, **which is not a registered item.** The registered ids are
  `krave_door` (the portal block) and `krave_door_block` (the wooden door). That recipe is
  dead. I deliberately kept `krave_door_block` off the village tier ladder because of it.
* `KraveMinion` is only ever spawned by `apocalypse/KraveKosmosAmbience` and
  `apocalypse/KraveKosmosBattle`, both of which are Krave Kosmos only. There is no
  overworld spawn. A "kill Cravelings" quest placed early in the spine would have been
  unreachable, so it lives in the Kosmos chapter instead (see §5).

---

## 2. Architecture

FTB Quests is the reference, and the two structural ideas taken from it are:

1. **Definitions and progress are separate things with separate lifetimes.** Definitions are
   datapack JSON, shared by everyone, sent once. Progress is per-player, changes constantly,
   sent as small deltas.
2. **Layout rides on the quest object** (`x`/`y` fields), so the tree screen renders by
   iterating quests, and there is no second layout table to drift out of sync.

```
data/<ns>/quests/**.json
        │  (SimpleJsonResourceReloadListener)
        ▼
   QuestLoader ──► QuestValidator ──► QuestFile  (immutable; reverse edges + roots prebuilt)
                        │ throws                      │
                        ▼                             ├──► S2CQuestDefs ──► ClientQuests
                  world fails to load                 │                          │
                                                      ▼                          ▼
world events ──► QuestEvents ──► QuestEngine ──► PlayerQuests            QuestTreeScreen
(kill/craft/place/                  │           (server-side NBT)
 dimension/tick)                    └──► S2CQuestState (delta) ──► ClientQuests
                                    ◄──── C2SQuestAction (claim / deliver)
```

### The three properties the engine was built to have

* **Progress never decreases.** Every write is either `addProgress` (cumulative, driven by an
  event that fires exactly once per occurrence) or `raiseProgress` (a `max`, safe to sample
  on a timer). §1.2 cannot recur: completion latches, and spending the items afterwards is
  irrelevant.
* **Settling is a bounded walk, not a fixpoint loop.** `QuestFile` precomputes reverse edges.
  When a quest completes the engine enqueues *only its dependants*. A quest completes at most
  once, so the queue is bounded by the edge count. There is an iteration guard that logs
  loudly if it is ever hit (it should be unreachable — the validator rejects cycles).
* **The client is told, never asked.** `ClientQuests.isComplete()` is a set lookup on data the
  server sent. There is no second opinion available to be wrong.

---

## 3. Task types

| type | counting | driven by |
|---|---|---|
| `kill` | cumulative | `LivingDeathEvent` (killer, or `getKillCredit()` so a summon's kill still counts) |
| `obtain` | high-water | inventory sample; nudged instantly by `ItemPickupEvent` / craft |
| `craft` | cumulative | `PlayerEvent.ItemCraftedEvent` + `ItemSmeltedEvent` |
| `deliver` | cumulative | player presses **Hand over the items** → `C2SQuestAction.DELIVER` |
| `place_building` | high-water | `BlockEvent.EntityPlaceEvent`, filtered to blocks something asked about |
| `village_tier` | high-water | `VillageState`, recomputed on place + on the sample tick |
| `feed_cayden` | high-water | `CaydenCobb.getKraveFed()` sampled from the owner's nearby Cayden |
| `defeat_boss` | latch | `LivingDeathEvent` |
| `visit_dimension` | latch | `PlayerChangedDimensionEvent` |
| `unlock_ability` | latch | quest-granted abilities + the level-gated `Perks` table |
| `krave_level` | high-water | `KraveLevel.getLevel()` |

`obtain` deliberately means **"hold N at once, ever"**, not "acquire N in total". A
high-water mark is the only sampled measure that is idempotent, and mixing a sampled measure
with event-driven increments is how you double-count. Anything that needs true lifetime
totals uses `craft`.

Sampling runs on `TickEvent.PlayerTickEvent` at `tickCount % 20 == 7` — a 1s cadence,
deliberately phase-offset so it never lands on the same tick as the other 20/40-tick sweeps
already in this mod.

## 4. Rewards

`item` (overflow drops at your feet, never voided) · `krave_xp` (through the existing
`KraveLevel.award` curve, so the HUD updates itself) · `ability` (a flag other modules read
via `QuestApi.hasAbility`) · `schematic` (a flag, not a piece of paper — read via
`QuestApi.hasSchematic`).

Claiming is idempotent: `grantRewards` bails unless `markClaimed` returns true, so a
double-click or a replayed packet cannot pay out twice.

## 5. The validator

`QuestValidator.validate()` runs inside the datapack reload, **before** the new graph is
published, so a broken pack leaves the previous good graph in place and fails the world load
with a boxed banner listing every problem. Fatal checks:

1. missing dependency  2. missing chapter  3. self-dependency  4. dependency cycle
(iterative three-colour DFS — recursion would blow the stack on a deep pack)
5. unreachable quest (not derivable from any root by forward edges, honouring
`min_dependencies`)  6. `min_dependencies` out of range  7. unknown registry ids for items,
entities and buildings — a `place_building` block must have an **item form** or the player
has nothing to place  8. **consumed prerequisite**: for every `deliver` task, the sum of what
the ancestor chain tells the player to obtain/craft, plus item rewards, minus what the chain
already consumed, must cover what is being asked for.

Check 8 is §1.2 expressed as arithmetic, and it is the reason `bosses/the_deal` depends on
`village/the_stash` — that is where the $500 it hands over comes from. Remove that dependency
and the world will refuse to load with an explicit message saying so. Non-fatal warnings
cover milestones that have rewards but no `auto_claim`.

I re-ran the same nine checks offline against the real `ModItems`/`ModBlocks`/`ModEntities`
registration names before shipping: **22 quests, 4 chapters, all reachable, no cycles, no
unregistered ids.** The scratch script lives at
`%TEMP%\claude\…\scratchpad\check_quests.ps1` if it is wanted in-repo.

## 6. The shipped questline

22 quests in 4 chapters. Every item in it was checked against the actual recipe pack; the
whole spine is craftable from vanilla ingredients.

* **The Craving** (`spine`) — wake up → touch grass → the grass knife → Krave cereal →
  the first bowl → four walls.
* **The Village** (`village`) — a roof over Rule #1 (Cayden validated-housed) → feed him ten
  times → panelling → living room → the stash (which is where the $500 comes from).
* **The Krave Kosmos** (`kosmos`) — build and place a Krave Door → step through → kill 8
  Cravelings (they only exist on the far side; see §1.5) → grind Krave dust and hit level 8
  → certified Kosmonaut.
* **The Bosses** (`bosses`) — summon and kill the Krave Monster (unlockable from *either*
  the Kosmos or the village branch — `min_dependencies: 1`) → hand over the $500 → Mom Cobb
  and The Plug → The Manager → Peace at Last.

`spine/wake_up` is a zero-task milestone, so a new player's book is never empty: it completes
on login and immediately opens Touch Grass with its objective printed in chat.

## 7. Registered ids

| kind | id | notes |
|---|---|---|
| item | `barbarajones:quest_atlas` | the quest book. `QuestRegistry.QUEST_ATLAS` |

Named *Atlas*, not Codex or Quest Book, to collide with neither the still-registered
`barbarajones:quest_book` nor the existing `client/ui/KraveCodexScreen` (a different
module's UI). Granted once on first login; also craftable
(`book + paper + krave_cereal`). It stores **nothing** — it is a key that opens a screen.

Also added: `assets/.../models/item/quest_atlas.json`,
`assets/.../textures/item/quest_atlas.png` (generated + pixel-verified by
`tools/make_questsv2.ps1`), `data/.../recipes/quest_atlas.json`,
`assets/.../lang/questsv2.json` (123 keys, every one verified present). It deliberately does
NOT redefine `key.categories.barbarajones`, which `en_us.json` already carries.

Keybind `key.barbarajones.open_quests`, default **K**, registered from the module's own
client subscriber. Does not touch `ClientSetup.java`.

Creative tab: the Atlas adds itself to vanilla `TOOLS_AND_UTILITIES` via
`BuildCreativeModeTabContentsEvent` in `QuestRegistry`, because `ModTabs.java` is off limits.
Moving it into the mod's own tab is a one-line change there whenever that file is free.

## 8. Entry point — the one thing the orchestrator must wire

```java
// BarbaraJonesMod constructor, next to the other *.register(bus) lines:
com.barbarajones.v2.quests.QuestModule.init(bus);
```

That is the **only** required edit. Everything else self-registers:
`QuestEvents` (`@Mod.EventBusSubscriber`) installs the datapack listener and all world hooks;
`QuestRegistry` and `QuestClientSetup.ModBus` are MOD-bus subscribers;
`QuestNetwork` registers its packets from `QuestModule`'s own common-setup listener on its
own channel (`barbarajones:quests_v2`), so it shares no packet-id table with `ModNetwork`.

### Removing the old system (orchestrator, please)

The old engine still runs and will fight this one for chat space. To retire it:

* `EventHandler.java` — delete the import at **line 18** (`com.barbarajones.quest.Quests`)
  and the calls at **line 53** (`Quests.findBook` used as the first-join probe — replace with
  a plain persistent-NBT flag), **line 68** (`Quests.onFirstJoin`), **line 237**
  (`Quests.tick`), **lines 358–359** and **line 366** (`Quests.complete(...)`). The v2 engine
  already observes every one of those deaths through `LivingDeathEvent`.
* Then `com.barbarajones.quest` (`Quests`, `expansion/*`), `item/QuestBookItem`,
  `client/QuestBookScreen` and `client/QuestBoardScreen` can be deleted. Leave the
  `quest_book` **item** registered if you want old worlds to keep the object; it becomes
  inert.
* `client/KraveManualScreen` and `item/KraveManualItem` are untouched and still work — the
  manual is a separate thing and v2 hands one out as the first quest reward.

## 9. For other modules

Call `QuestApi`, never `QuestEngine`/`PlayerQuests`. Everything is null-safe and a no-op on
the client side.

```java
QuestApi.reportBuildingPlaced(player, blockId);  // structure stamped into the world
QuestApi.reportVillageTier(player, tier);        // your settlement tier; stored value is max()
QuestApi.reportCaydenFed(player);                // optional — the engine samples him anyway
QuestApi.reportAbilityUnlocked(player, "name");
QuestApi.reportBossDefeated(player, bossTypeId); // optional — LivingDeathEvent covers it

QuestApi.isComplete(player, id);   QuestApi.isUnlocked(player, id);
QuestApi.hasAbility(player, "kosmic_tether");
QuestApi.hasSchematic(player, new ResourceLocation("barbarajones", "starter_shack"));
QuestApi.villageTier(player);      QuestApi.completedCount(player);
```

**Village module, read this:** there is no settlement system in the repo, so
`VillageState` derives a 0–5 tier from blocks the player has personally placed plus
`CaydenCobb.isHoused()`. It is monotone — knocking a wall down never un-earns a quest. If you
land a real settlement system, do not rip this out: just call `reportVillageTier` and the
higher number wins.

---

## 10. Done / not done

**Done and I am confident in it**

* Data-driven quests + chapters, hot-reloadable with `/reload`, overridable by any datapack.
* All eleven task types, each on a real hook or an idempotent sample.
* Server-authoritative per-player progress in `PERSISTED_NBT_TAG` (survives death and
  dimension change, same mechanism `KraveLevel` already relies on), synced with a
  definitions-once + progress-delta packet pair.
* Bounded settle walk, latched completion, idempotent reward claiming.
* Validator with nine checks, fatal at load, re-run offline against the real registries.
* Pannable/zoomable tree screen with drawn dependency edges, cross-chapter edge stubs, and a
  detail panel that shows objective + tasks + rewards + an explicit "Unlocked by:" list
  **for locked quests too**. No grey box anywhere.
* 22-quest spine from spawn to the last boss. Texture, model, recipe, lang all present.

**Not done — be honest about these**

* **Not compiled.** Rule #1 of this job is no Gradle. Everything was written against 1.20.1
  Mojang-mapped signatures from memory and cross-checked against existing code in this repo,
  and braces/parens balance in all 24 files, but the first central build may still turn up a
  typo. The highest-risk spots are the `GuiGraphics` overloads in `QuestTreeScreen`
  (`drawString(Font, Component, int, int, int, boolean)`, `renderTooltip(Font, List, Optional,
  int, int)`, `enableScissor/disableScissor`) and `SimpleJsonResourceReloadListener`'s
  `apply` signature in `QuestLoader`.
* **Not play-tested.** No world was launched. The graph is validated, but "does feeding
  Cayden ten times feel right" is a balance question nobody has answered.
* **The old system is still wired in.** I could not touch `EventHandler.java` or
  `BarbaraJonesMod.java`. Until §8 is done, the v2 engine is inert (never initialised) and
  the old one is still running. Do not judge either from a build where both are live.
* **`deliver` has no partial-submit UI.** Pressing the button takes whatever it can find and
  advances by that much; there is no slot-based submission screen. Adequate for the one
  delivery task shipped, thin if the pack grows a lot of them.
* **The tree screen has no search, no bookmarks, no zoom-to-quest.** It pans and zooms and
  that is all. Cross-chapter dependencies render as a labelled stub, not as a clickable jump.
* **`feed_cayden` needs Cayden within 96 blocks** of the player when the sample runs. Since
  he follows you, that is nearly always true — but feed him and then walk 200 blocks away
  within the same second and the tick is missed. It is a high-water read of his own lifetime
  counter, so the next time you are near him it catches up. No progress is permanently lost.
* **`VillageState` counts blocks the player placed, not blocks standing in the world.** Place
  a carpet, mine it, place it again: that counts twice. I chose the monotone/forgiving side
  deliberately, but it is exploitable if anyone cares.
* **No advancement integration.** `KraveAdvancements.grant` exists and quests do not call it.
* **No `/quests` command** for debugging or for granting a quest to a stuck player.
