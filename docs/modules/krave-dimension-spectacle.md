# Krave Dimension spectacle: chocolate waterfalls + the world reacting to ascension

Two independent pieces of work, packaged together because they were assigned
together. Neither depends on the other and either can be reverted alone.

## A) Chocolate waterfalls (Krave Kosmos worldgen)

### What I read first
- `dimension/krave_kosmos.json`, `dimension_type/krave_kosmos.json`,
  `worldgen/krave/KraveWorldFeatures.java` + `KraveMonolithFeature.java`.
- The other developer's chocolate terrain: `content/ModFluids.java` (the
  `barbarajones:chocolate` fluid, a reskinned-lava `ForgeFlowingFluid`),
  `configured_feature/chocolate_lake*.json`, `chocolate_tree*.json`,
  `worldgen/biome/krave_void.json` (the Krave Kosmos's actual biome - not
  `krave_world_cocoa_grove.json`, which is the separate "Krave world type"
  overworld reskin and unrelated to this dimension).
- `worldgen/feature/KraveMountainFeature.java`, `KraveValleyFeature.java`,
  `KravePeakFeature.java`, `KraveTerrainShape.java` - the existing Krave
  Kosmos terrain features, for the column-major/local-ground-following
  construction style and the "Feature instances are reused across
  concurrently-decorated chunks, keep them stateless" rule.
- Git history: `git log --oneline -i --grep="waterfall"` turned up
  `db8b74a`, `b629cd8`, `ab21d6d`, `b26b032`, `6686f3e`. Read `6686f3e`'s
  diff in full - the real bug across three earlier "fix the waterfall"
  attempts was that `KraveMountainFeature`'s outcrop/spring candidate list
  only got built from columns reaching 55% of the mountain's height, and on
  any mountain where that zone came up empty, the fix silently skipped
  outcrops AND the spring both - not made rarer, skipped entirely. The fix
  was a guaranteed fallback anchor (the origin's own column, already proven
  solid) so the candidate list is never empty.

I did **not** touch `KraveMountainFeature`'s existing spring, and did not
touch any chocolate lake/tree file - matched the existing conventions
instead (reuse `ModBlocks.CHOCOLATE_BLOCK`, `ModBlocks.KRAVE_DIRT`,
`ModBlocks.KRAVE_GRASS`; same column-major heightmap-driven construction
style as the other Krave Kosmos features) rather than inventing a second
chocolate system.

### What "real" means here, vs. the existing mountain spring
The existing mountain spring is one chocolate source block dropped on a
ledge, left entirely to vanilla fluid physics to turn into something that
reads as a waterfall. This module is a dedicated feature that:
1. **Picks a genuinely dramatic site.** `KraveWaterfallFeature.findDramaticCliff`
   samples a grid around the placement origin and, for every point,
   compares the height drop in the first couple of blocks past the edge
   against the drop further out in the same direction - a real cliff shows
   most of its drop immediately; a slope spreads it out. Only sites with a
   13+ block drop that is at least half realized within 2 blocks qualify,
   and only the single steepest/tallest one found is used. No qualifying
   site → the feature is a quiet no-op, by design (rarity in the placement
   JSON governs how often it even tries).
2. **Builds the whole visible shape directly**, applying the reliability
   lesson above proactively: the lip, the cascade down the face (up to 3
   parallel strands), and a real carved basin at the base are all placed as
   chocolate source blocks in one pass, not one spring hoping worldgen tick
   timing sorts out something dramatic. The center strand is always exactly
   the coordinate `findDramaticCliff` already validated, so at least one
   strand - and therefore at least one basin - is unconditionally
   guaranteed once a site is found, the same "guaranteed anchor" shape as
   the mountain fix, applied from the start instead of needing a follow-up
   commit.
3. Runs in the biome's `LOCAL_MODIFICATIONS` step, placed **after**
   `krave_mountain`/`krave_valley`/`krave_peak` in `krave_void.json`'s
   feature list, so its heightmap reads see terrain those features already
   carved - that ordering is what makes "dramatic cliff" mean anything.
4. Particles and sound are handled separately (`KraveWaterfallAmbience`) by
   scanning loaded chunks near each player for chocolate fluid with open
   space below it - "currently cascading," detected live, not from a
   marker. This also dresses every existing `KraveMountainFeature` spring
   for free, not just the new waterfalls.

### IDs registered
All in `com.barbarajones.worldgen.feature.KraveWaterfallContent`, which
self-registers off `RegisterEvent` (same pattern as
`worldgen.krave.KraveWorldFeatures` / `world.WorldFeatures`) - **no central
file needs editing or calling for this half of the task**:
- Feature `barbarajones:krave_waterfall` (`KraveWaterfallFeature`)
- Sound event `barbarajones:chocolate_flow`

Data files added:
- `worldgen/configured_feature/krave_waterfall.json`
- `worldgen/placed_feature/krave_waterfall.json` (rarity 1/6, `in_square` +
  `WORLD_SURFACE_WG` heightmap placement)
- `assets/barbarajones/sounds/chocolate_flow.ogg`, synthesized by
  `tools/make_krave_waterfalls.ps1` (already run - see Verification below)

### Files touched that are NOT mine (shared, not on the forbidden list, edited minimally and additively)
- `worldgen/biome/krave_void.json` - appended `"barbarajones:krave_waterfall"`
  to the end of the existing `LOCAL_MODIFICATIONS` array (index 2), after
  `krave_peak`. One line added, nothing removed or reordered.
- `assets/barbarajones/sounds.json` - appended one `"chocolate_flow"` entry
  at the end, before the closing brace.

### Lang
`assets/barbarajones/lang/krave_dimension_spectacle.json` -
`subtitles.barbarajones.chocolate_flow`.

### Verification
Ran `tools/make_krave_waterfalls.ps1` - it found `ffmpeg.exe` via the winget
package already on this machine, wrote and read back
`assets/barbarajones/sounds/chocolate_flow.ogg` (16,495 bytes, confirmed
`OggS` magic header), and the script checks the file is above a minimum
size before declaring success. I did **not** run Gradle, per the hard rule -
`KraveWaterfallFeature`'s worldgen logic and `KraveWaterfallAmbience`'s
scan logic are reviewed carefully (traced every API call against either
existing codebase usage or the mapped Forge jar directly, since no build
was run) but not runtime-tested in a real generated world.

### Known gaps
- Not visually verified in a running game (rule #1 of the task: no Gradle).
  The site-selection thresholds (13 block minimum drop, `chance: 6` rarity)
  are a reasonable starting guess, not a tuned-in-game value.
- No new block or texture work - deliberately reuses the other developer's
  `CHOCOLATE_BLOCK`/`KRAVE_DIRT`/`KRAVE_GRASS`. If they change those
  registry names, this feature breaks with them (same exposure every other
  Krave Kosmos terrain feature already has).
- `KraveWaterfallAmbience` scans every server level every 12 ticks, 8
  samples per online player, radius 16 horizontal / 10 vertical. Cheap in
  isolation; not load-tested against many simultaneous players.

---

## B) The world reacts when Cayden transforms

### What I read first
- `entity/CaydenCobb.java` - specifically `transformationSpectacle(int)`,
  `tickSpectacle()`, `announceTier(int)`, `becomeSuperSaiyan()`,
  `onEnterKosmos()`, `updateTier()`. **Not edited** (per the task).
- `client/render/SsjAuraLayer.java` - read for where/how the ascended aura
  actually renders. **Not edited**.
- `apocalypse/KraveApocalypse.java`, `apocalypse/KraveKosmosBattle.java`,
  `apocalypse/KraveKosmosAmbience.java`, `client/ApocalypseClient.java` -
  the tornado (`entity/KraveTornado.java` via `ModEntities.TORNADO`),
  meteor, and screen-shake machinery named in the task.

### The gap this fills
`CaydenCobb.transformationSpectacle(tier)` already exists and is genuinely
good work - ground-crack **particles**, an expanding lightning-bolt ring,
and hostile-mob flinch, all in a tight radius around Cayden himself. Its own
comment already calls it cosmetic-only ("nothing is ever broken") and
scoped to a few dozen blocks. That is the "local effect" the task describes
- this module is the missing *world* half: it does not replace or duplicate
`transformationSpectacle`, it runs alongside it and reaches further out with things that method deliberately doesn't do:
real (not particle-only) ground disturbance at higher tiers, entities and
loose terrain decoration physically tossed, `KraveTornado` funnels touching
down away from Cayden, a fog-based sky darkening, a real camera shake (not
just the local mob flinch), and - the specific ask - a rumble that reaches
distant players *after* the flash instead of on top of it.

### The one call the orchestrator must add
Inside `CaydenCobb.announceTier(int tier)`, immediately after its existing
call to `transformationSpectacle(tier)` (that method's own comment already
calls this moment "the ACTUAL transformation moment"):

```java
transformationSpectacle(tier);
com.barbarajones.apocalypse.KraveQuake.onAscend((ServerLevel) level(), position(), tier);
```

That one line is the entire integration surface. `KraveQuake` never reads
or writes anything on `CaydenCobb` itself - it only takes the level,
position, and tier it's handed, so it cannot affect his health, AI, or
survival in any way (rule #1 stays untouched by construction, not by
promise).

**Known, deliberate gap:** `CaydenCobb.onEnterKosmos()` calls
`becomeSuperSaiyan()` directly and never goes through `announceTier`, so
arriving in the Kosmos does not currently trigger a world reaction. That
path reads as a quieter "gate" moment in the existing code (no
`transformationSpectacle` call there either), so I left it as the one
un-wired case rather than guessing it should be louder. The identical
one-line call works there too if that turns out to be wanted.

### What it does (`apocalypse/KraveQuake.java`, server; `client/KraveQuakeClient.java`, client)
Everything is self-ticking and self-registered
(`@Mod.EventBusSubscriber`) - **no `EventHandler.java` edit**, matching how
`apocalypse.KraveKosmosAmbience`/`KraveApocalypse` are ticked centrally but
this package isn't allowed to touch that central tick call, so it
subscribes to `TickEvent.ServerTickEvent`/`ClientTickEvent` directly
instead.

Scaled by tier (`progression.AscensionLadder`, 1 SSJ .. 6 ULTRA):
- **Outward shockwave**: a ring expands from the epicenter at
  `0.6 + tier*0.18` blocks/tick out to `10 + tier*9` blocks (SSJ ~19 blocks,
  ULTRA ~64). Living entities caught in the ring the moment it crosses them
  get a gentle upward/outward toss (`0.10 + tier*0.045` strength, well
  under fall-damage territory); creative-mode players are skipped.
- **Loose blocks lifted**: flowers, saplings, leaves, short/tall grass,
  ferns, mushrooms, sweet berry bush - an explicit allow-list, never
  anything a player could have built - get kicked into the air as real
  vanilla `FallingBlockEntity` instances (`FallingBlockEntity.fall`, which
  is the same vanilla mechanism sand/gravel/anvils use; `setHurtsEntities`
  is deliberately never called, so these never deal fall damage on
  landing).
- **Real ground scarring, SSJ2 and up only**: a sampled grass surface block
  has a small chance to flip to its bare-dirt equivalent -
  dimension-aware (`barbarajones:krave_grass` → `krave_dirt` in the Kosmos,
  vanilla `grass_block` → `dirt` anywhere else, since `demandFor()` can
  require an ascension outside the Kosmos too). SSJ1 never does this - it
  stays the "tremor" the brief asks for.
- **Tornadoes, SSJ2 and up**: `tier-1` `KraveTornado` entities
  (`ModEntities.TORNADO`) touch down at staggered random points inside the
  disturbed radius over the quake's lifespan.
- **Distant delayed rumble**: every player within 400 blocks is scheduled a
  `ModSounds.KRAVE_RUMBLE` (plus `KRAVE_BOOM` at ULTRA) at a delay of
  roughly `distance / 7 blocks-per-tick` - the flash from
  `transformationSpectacle` has already fired by the time `onAscend` runs,
  so this reliably lands after it, arriving later the further away the
  listener is, the way real thunder trails real lightning.
- **Sky darkening + camera shake (client)**: `KraveQuakeClient` watches
  every loaded `CaydenCobb`'s public, already-synced `getTier()` each
  client tick; a tier that just went *up* starts a short decaying pulse
  (sharp attack, curved decay, tier-scaled duration and strength). While a
  pulse is live it darkens `ViewportEvent.ComputeFogColor` toward black and
  perturbs `ViewportEvent.ComputeCameraAngles`' yaw/pitch/roll - a real
  camera shake, not the GUI-overlay-only trick `ApocalypseClient` uses
  (there's no overlay content here to shake against). Both fall off with
  distance from Cayden's live position (out to 220 blocks) and take the
  strongest pulse if several Caydens ascend at once, rather than stacking.
  No network packet - this reuses state the game already syncs.
- A close-quake suppression (10 blocks) stops a fight that escalates
  several tiers in a few ticks from stacking overlapping shockwaves;
  `onServerTick` catches and swallows any exception from a single quake's
  tick so a cosmetic bug can never wedge or crash the server.

### IDs / files
No new items, blocks, entities, or registries - this module only *reads*
`ModEntities.TORNADO`, `ModSounds.KRAVE_RUMBLE`/`KRAVE_BOOM`/`KRAVE_TORNADO`,
`ModBlocks.KRAVE_GRASS`/`KRAVE_DIRT` (all pre-existing, none edited). No
lang keys needed - it sends no chat messages and shows no GUI text.

### Known gaps
- Not runtime-tested (no Gradle, per the hard rule). `ViewportEvent.ComputeCameraAngles`
  is a new-to-this-codebase hook - I confirmed its class, and its
  `getYaw/setYaw/getPitch/setPitch/getRoll/setRoll` methods, directly
  against the mapped Forge jar in the Gradle cache (no source jar was
  available to read), since nothing in this codebase used it yet to copy
  from; the actual in-game feel of the shake amplitude is untuned.
- The SSJ2+ ground-scarring (`grass` → `dirt`) is a real, permanent world
  edit with no revert-after-fight logic. It's deliberately sparse (roughly
  1-in-6 of an already-small per-tick sample count) so it reads as light
  scarring rather than a crater, but a player who wants their grass back
  has to replant it.
- The distant-rumble scheduler assumes a stable player list captured at
  `onAscend` time; a player who joins mid-quake gets no rumble from that
  quake, and one who logs out before their scheduled delay is simply
  skipped (checked via `isAlive()`) rather than erroring.
