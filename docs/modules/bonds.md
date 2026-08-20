# Bonds module (`com.barbarajones.v2.bonds`)

FEEDING, BREEDING, and COMPANION BOND systems for Cayden and Barbara, plus the
"exactly one of each" world registry and Barbara's throw. Package:
`com.barbarajones.v2.bonds`.

## The one orchestrator change

In `BarbaraJonesMod`'s constructor, alongside the other modules' `.init(bus)` calls:

```java
com.barbarajones.v2.bonds.BondsRegistry.init(bus);
```

That is the only required wiring. Client rendering (`BondsClient`) and entity
attributes (registered from `BondsRegistry.init` itself, via
`bus.addListener`) are already self-contained - Forge picks up the
`@Mod.EventBusSubscriber` classes on its own.

## The one optional orchestrator change (Barbara's throw)

`BarbaraJones.registerGoals()` already has an inner-class version of the
Cayden-throw (`BarbaraJones.BarbaraThrowCaydenGoal`, registered at priority 0).
This module's version (`com.barbarajones.v2.bonds.BarbaraThrowCaydenGoal`) is a
strict superset of it - grab windup, a real overhead carry (repositioned every
tick, not teleported once), the same arc/impact via `CaydenCobb.launchFrom`,
and a landing beat ("he lands on his feet, annoyed") the old one never had. To
swap to it, in `BarbaraJones.java`:

```java
// was:
this.goalSelector.addGoal(0, new BarbaraThrowCaydenGoal(this));
// change to:
this.goalSelector.addGoal(0, new com.barbarajones.v2.bonds.BarbaraThrowCaydenGoal(this));
```

...and the old inner class can be deleted. Until that one line lands, both
versions coexist harmlessly (they never both trigger on the same tick since
each has its own cooldown), so this is safe to leave unswapped if the
orchestrator would rather not touch `BarbaraJones.java` at all right now.

## Registered ids

**Name collision, caught and fixed before this landed:** the item was
originally built as `krave_family_box`. Before finishing, a check for other
in-flight uses of that name turned up that `ModItems.java` (shared, off
limits) already registers a real, different item at that exact id -
`KRAVE_FAMILY_BOX`, a bulk-cereal trade good already wired into
`VillageTrades.java`. Registering a second item at the same registry name
would either crash at load or silently clobber one of them, and my own draft
had already overwritten that item's shared texture and model files (they were
restored to the version in the working tree via `git checkout --`, and their
`en_us.json` lang entry was left untouched). Everything in this module was
renamed to `krave_cloning_box` / `KraveCloningBoxItem` before generating any
final assets, and a final repo-wide grep for both the old and new ids turned
up no further collisions. Flagging this here mainly as a reminder: worth a
quick `grep` for any id before this module's assets are merged, in case
another concurrent module picked the same name for something else in the
meantime.

- Entity type `barbarajones:bred_cayden_cobb` - `BredCaydenCobb`, a `CaydenCobb`
  subclass. Its own `DeferredRegister<EntityType<?>>` in `BondsRegistry`.
- Item `barbarajones:krave_cloning_box` - `KraveCloningBoxItem`. Its own
  `DeferredRegister<Item>` in `BondsRegistry`. Added to `ModTabs.MAIN` via
  a `BuildCreativeModeTabContentsEvent` listener (never edits `ModTabs.java`).
- No new sounds were registered. Every audio cue reuses an existing vanilla or
  mod `SoundEvent` (`SoundEvents.*`, `ModSounds.*`) - see "What was
  deliberately not built" below for why.
- Texture: `assets/barbarajones/textures/item/krave_cloning_box.png`, generated
  by `tools/make_bonds.ps1` (run and verified; System.Drawing pixel art, no
  placeholder flat colour, matches the brown/cereal-crumb Krave palette).
- Lang: `assets/barbarajones/lang/bonds.json` (item + entity display names -
  every chat/UI string in this module is `Component.literal`, matching this
  codebase's own convention, so it needs no translation keys).

## What each file does

- `BondsRegistry` - the registries and the one `init(bus)`.
- `BondLevel` - the five-rung "Stranger &rarr; Krave Soulmate" ladder, one
  table for both companions (Cayden off `getKraveFed()`, Barbara off her own
  shadow lifetime-gifts counter - see `BondState`).
- `BondState` - every bit of extra state, stored in Forge's per-entity
  `getPersistentData()` rather than in either base entity: Barbara's lifetime
  gift count, an assigned house position, search/happiness cooldowns, the
  Family Box cooldown, and the last bond level announced.
- `BondBuffs` - the escalating passive buffs (on top of `CaydenCobb`'s own
  fed-scaled attack/speed, which was already real before this module).
- `FeedingBondEvents` - feeding feedback (action-bar meter) and bond tracking
  for Cayden and Barbara, plus the tick hook that reapplies buffs, updates the
  nameplate, and drives house-settling.
- `KraveCloningBoxItem` / `BredCaydenCobb` - CAYDEN BREEDING (see below).
- `CanonicalRegistry` / `CanonicalGuardEvents` - ONE OF EACH, ENFORCED (see
  below).
- `VillageHouseFinder` - HOUSE ASSIGNMENT (see below).
- `BarbaraThrowCaydenGoal` - THE THROW (see below).
- `BondsClient` - registers `CaydenRenderer` for `BredCaydenCobb` (he reuses
  Cayden's renderer wholesale - he IS one).

## FEEDING CAYDEN

`CaydenCobb.feedKrave`/`applyKraveStats` (untouched, off limits) already made
Krave feeding raise his raw attack/speed. This module adds the rest of what
"feeding makes him stronger" needs to feel like a relationship rather than a
stat creeping up:

- **Feeding UI feedback**: `FeedingBondEvents.onInteract` observes the same
  `PlayerInteractEvent.EntityInteract` that precedes his own `mobInteract`,
  and posts an action-bar meter (`Player.displayClientMessage(component,
  true)` - plain server-side vanilla API, no client overlay code needed) with
  his live bond level, star meter, and boxes-to-next-rung. Since his feed
  always succeeds for any player with either item (there is no failure branch
  to predict), `getKraveFed() + 1` is a real number, not a guess.
- **Visible bond level**: his custom name (`entity.setCustomName` /
  `setCustomNameVisible`) is turned on and updated to `"Cayden Cobb ★★☆☆"`
  the moment his bond rung changes - readable in play, not buried in chat.
- **Escalating buffs**: `BondBuffs.applyToCayden`, reapplied on a timer,
  layers `MobEffectInstance`s (damage resistance, regen, then damage boost +
  speed, then fire resistance + more resistance at the top rung) on top of his
  existing stat scaling.

### Change CaydenCobb.java should NOT get (kept as documentation only)

Per the brief, `CaydenCobb.java` was read but not edited. Nothing here
requires editing it for feeding itself to work - the counter
(`getKraveFed()`), the rage threshold (`RAGE_THRESHOLD = 25`), and the
ascension ladder gates are all already public and were used as-is. The one
place a small, additive change to `CaydenCobb.java` WOULD help (house
assignment) is called out below, in its own section, rather than here.

## CAYDEN BREEDING: the Krave Cloning Box

`CaydenCobb.getBreedOffspring()` already returns `null` - he was never meant
to breed the normal way. Instead: `KraveCloningBoxItem`, right-clicked on any
`CaydenCobb` (canonical or already-bred) that has eaten at least 15 boxes,
consumes itself and spawns a `BredCaydenCobb` next to him - a full second
Cayden, tamed to the same player. The box does not survive the encounter; one
of five random punchlines announces it to everyone nearby. A 5-minute
per-parent cooldown and an 8-clones-per-owner cap (best-effort - it only
counts currently loaded clones) keep it from becoming a lag machine.

**Inheritance, not duplication**: the offspring gets `~40%` of the parent's
fed count and roughly half of the parent's taught ascension forms - each
re-taught for real through the public `CaydenCobb.tryUnlock`, funded with
exactly the Ki those specific rungs cost (`AscensionLadder.rung(t).kiCost()`,
public), so the transformation spectacle fires honestly and the child ends up
with zero leftover Ki rather than a windfall. Everything is done through
`CaydenCobb`'s existing public API (`restoreKrave`, `addKi`, `tryUnlock`,
`getKraveFed`, `getUnlockMask`, `highestUnlockedTier`, `isRageUnlocked`) -
nothing private was touched, nothing was reimplemented.

### Known, unfixed collision (please read before anyone is surprised by it)

`CaydenCobb.die()` banks ascension progress into a **static**, owner-UUID-keyed
map (`ASCENSION_LEGACY`) for the next respawn to consume, and that map is
shared by every `CaydenCobb` instance, canonical or bred, because it is a
static field on the base class. If a bred Cayden and the canonical Cayden
share an owner and are BOTH dead at the same moment, whichever dies second
overwrites whichever legacy entry was already banked for the first. This was
not worked around because `CaydenCobb.java` was explicitly off limits. The
clean fix, for whoever next has permission to edit it, is either:

1. Key `ASCENSION_LEGACY` by entity UUID instead of owner UUID, or
2. Add a `protected` hook (e.g. `protected void onBankLegacy(UUID owner)`)
   that `die()` calls, which `BredCaydenCobb` could override to no-op.

This is a real, narrow-window bug, not a hypothetical - flagging it clearly
rather than quietly shipping it.

## ONE OF EACH, ENFORCED

`CanonicalRegistry` (a `SavedData`, keyed `barbarajones_bonds_canonical`,
using this codebase's actual `computeIfAbsent(loader, constructor, key)`
three-arg form - confirmed against `KraveKosmosData`'s own usage, not assumed)
tracks the canonical Barbara's and Cayden's UUIDs plus a periodically
refreshed snapshot (position, fed count, rage flag, Ki, unlock mask) for
last-resort rebuilding.

`CanonicalGuardEvents`:

- **Prevents duplicates**: `EntityJoinLevelEvent`, checked with
  `entity.getClass() == CaydenCobb.class` / `== BarbaraJones.class` (exact
  type, so `BredCaydenCobb` - a distinct subclass - is exempt by
  construction, not by a flag). If a live canonical one is already
  registered and this is a different entity, the join is cancelled. This
  also narrows `EventHandler.onLogin`'s existing per-player Cayden spawn to
  "only the first player in the world gets the canonical one" - see the gap
  below about the message that player sees regardless.
- **Restores if lost**: every `SNAPSHOT_INTERVAL` (5s) it snapshots the pair
  while alive; if one is missing for a full 2 minutes it rebuilds it from the
  snapshot via `ModEntities.CAYDEN`/`BARBARA` + the same public API the
  breeding item uses. The long grace period is deliberate - `KraveApocalypse`
  already has its own, much faster death/respawn cycle for the common case,
  and this is a safety net under it, not a replacement.

### Known gaps in the enforcement

- **Chunk-unload blind spot**: `ServerLevel#getEntity(UUID)` only finds
  currently-loaded entities. If the recorded canonical Cayden's chunk happens
  to be unloaded at the exact moment a duplicate tries to join, the duplicate
  will be wrongly admitted (and, since `onJoin` unconditionally re-records
  whichever entity was just admitted, it will silently become the new
  "canonical" one). This is a narrow, rare race, not a design I found a clean
  fix for without a forced chunk-load that felt riskier than the bug.
- **`EventHandler.onLogin`'s message is unconditional.** For the second and
  later players in a world, this registry cancels their personal Cayden's
  join, but `EventHandler.java` (off limits to edit) still sets their
  `KraveCaydenSpawned` flag and tells them "Cayden Cobb tagged along" even
  though he is not actually there. The clean fix is a small change to that
  handler - check `CanonicalRegistry` before spawning at all, and if a
  canonical Cayden already exists, either skip the message or (nicer) walk
  the existing one over instead of trying to create a second - but
  `EventHandler.java` is on the explicit do-not-edit list, so this is
  documented rather than patched.
- Barbara's rebuild path does not try to re-pet her to a specific owner
  (there is no public way to read `petOwner`'s UUID off a live instance, only
  `getPetOwner()`, which falls back to "nearest player" when unset) - a
  rebuilt Barbara comes back un-pet, recruitable the normal way.

## FEEDING VILLAGERS

The brief says to read `docs/modules/village.md` and call that module's API.
**That file did not exist** when this module was written - there was no
documented API to read. Rather than block on that, I read the module's source
directly (`com.barbarajones.v2.village`) and found it was not a stub: it is a
complete, self-contained implementation of exactly this requirement, already
built by whichever concurrent agent owns that package.
`KraveVillagerEntity.feedKrave()` (called from that entity's own
`mobInteract` for both Krave Cereal and Golden Krave) already grants trade
XP, particles, a chime, a growing glow, and an action-bar progress message,
and `VillageOffer.restock()` / its discount-on-level-up already deliver
"better trades, faster restock." There was nothing left to wire, and building
a second feeding path aimed at vanilla `Villager` (which this mod's own
economy does not otherwise use - it spawns `KraveVillagerEntity` instead)
would have meant a parallel system doing the same job worse.

What this module DOES call into that API for is real, additive integration
that did not exist before: `VillageHouseFinder`

- prefers a bed inside a chartered `KraveVillage` claim (via
  `KraveVillage.nearest(level, pos)` / `VillageView.contains(pos)`) over one
  outside any claim, when picking where Cayden and Barbara settle, and
- calls `KraveVillage.adjustHappiness(level, home, 1)` on a slow drip (every
  2 minutes) while a companion is home, so a village is mechanically, not just
  narratively, happier for having them living in it.

**Not done / not verified**: the exact `PoiManager.findAll` overload used in
`VillageHouseFinder` (predicate over `Holder<PoiType>`, `BlockPos` origin,
radius, `PoiManager.Occupancy`) was written from memory of the 1.20.1 Mojang
mappings and was not checked against this repo's actual mapped sources or
compiled - if the central build flags a signature mismatch here, this is the
first place to look.

## FEEDING BARBARA + the throw

Feeding (grass/joints) already existed on `BarbaraJones` (stash, high,
psycho/calm scale). This module adds:

- The same feedback/bond-level/nameplate/escalating-buffs treatment as
  Cayden, off a new lifetime "times fed grass" shadow counter (`BondState`,
  since her stash decays and was never meant to survive as a lifetime total).
- **The throw that never worked**: see `BarbaraThrowCaydenGoal` above. Grab
  (he freezes mid-whatever-he-was-doing via `setNoAi(true)`, matching how
  `launchFrom` itself already works) &rarr; carry (repositioned overhead
  every tick, not teleported once) &rarr; `CaydenCobb.launchFrom` for the
  actual arc/impact (real damage on landing, already implemented there,
  reused rather than duplicated) &rarr; a landing beat once `isNoAi()` flips
  back to `false` - the exact signal `CaydenCobb.tickThrow()` gives when he
  lands, read without touching any of its private timer state. "He lands on
  his feet, annoyed" is a guaranteed chat line every time, not a coin flip.

## HOUSE ASSIGNMENT

Both companions settle in a real village house - a bed inside a room that
passes every `HousingValidator` check `CaydenCobb`'s own manual claim already
trusts (enclosed, lit, doored, floored, `MIN_VOLUME`/`MIN_HEIGHT` met) -
found via `PoiManager` beds, preferring one inside a chartered `KraveVillage`
claim (see above). `VillageHouseFinder.settleTick`, called from
`FeedingBondEvents`'s tick hook, walks them home when the owner is not
nearby and steps back (does nothing) when the owner is close, so normal
follow/combat AI drives instead - the same shape as `CaydenCobb`'s own
`CaydenFollowOrHomeGoal`.

### The one CaydenCobb.java addition this would benefit from

`CaydenCobb.home` has no public setter - only `tryClaimHome(Player player)`,
which validates at the PLAYER's position, because it was built for "stand in
the room and right-click him." There is no way to programmatically hand him a
discovered house without either teleporting a real player into it or editing
the class, so this module drives him to his auto-assigned house independently
(direct navigation, not his own `home` field) - which makes "settle in a
village house and follow the player from there" true in play, but means his
own `isHoused()` will not reflect an auto-assigned house, only a manually
claimed one. The clean fix, whenever `CaydenCobb.java` is open for edits
again, is a small additive method:

```java
/** Programmatic variant of tryClaimHome, for systems that have already
 *  validated a location without a player standing in it. */
public void assignHome(BlockPos pos, String dimensionId) {
    this.home = pos;
    this.homeDim = dimensionId;
    this.entityData.set(HOUSED, true);
}
```

Barbara has no housing system of her own at all (nothing to conflict with),
so her side of this is fully native to this module already.

## Deliberately not built

- **No new sound assets.** Every cue in this module reuses an existing
  `SoundEvents.*` or `ModSounds.*` entry. Given the size of the rest of the
  brief, generating and wiring a fresh sound bank (`tools/make_bonds_audio.ps1`
  + `sounds.json` + ffmpeg) was judged lower value than the mechanical systems
  above, and the existing sound bank already covers every beat used here
  (villager yes/no, generic eat, explosions, trident throw, etc.).
- **No custom HUD overlay.** "Feeding UI feedback" is delivered through the
  vanilla action-bar channel (`Player.displayClientMessage(component, true)`)
  called from plain server-side code, which needed no `Dist.CLIENT` /
  `DistExecutor` work at all and was judged the more robust choice over a
  bespoke `RegisterGuiOverlaysEvent` + a second network channel just to
  duplicate what the action bar already does well.
- **Vanilla-`Villager` feeding** - see "FEEDING VILLAGERS" above; not built
  because `KraveVillagerEntity` already does this job completely.
