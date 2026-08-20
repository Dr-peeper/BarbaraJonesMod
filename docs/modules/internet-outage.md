# Internet Outage module (`com.barbarajones.v2.internet`)

The village's internet goes out, the manager gets called, and he turns up to
make it much worse. Package: `com.barbarajones.v2.internet` (+ `.client` for
the render-only classes).

## What this delivers

- **THE OUTAGE EVENT** (`OutageEvent`) - a per-`ServerLevel` `SavedData`
  scheduler/runner, in the spirit of vanilla `Raids`: `INACTIVE -> SCHEDULED
  -> ANNOUNCED -> ACTIVE -> ENDING -> INACTIVE`. Random future time is rolled
  in-game-time (4-10 days), re-rolled a day later if the gate fails, and
  persisted, so it survives a server restart without ever needing to fire
  early or skip a cycle. Cannot fire without a housed Cayden anywhere loaded
  in that level (see "the village gate" below). Fully cancellable via the
  public static `OutageEvent.cancel(ServerLevel)`.
- **THE ROTARY PHONE** (`RotaryPhoneItem`, item id `rotary_phone`) - craftable
  (iron + copper + redstone, see `data/.../recipes/rotary_phone.json`),
  consumed on a successful call, refused (and NOT consumed) if the call can't
  go through.
- **THE SERVICE CALL BOX** (`ServiceCallBoxBlock`, block id
  `service_call_box`) - a placeable, reusable, horizontally-oriented block
  (craft recipe consumes a Rotary Phone - `data/.../recipes/service_call_box.
  json`); right-click to call him, on `OutageEvent`'s own ten-minute manual
  cooldown so it can't be spammed.
- **THE INTERNET MANAGER BOSS** (`InternetManagerBoss`, entity id
  `internet_manager`) - three-phase fight (CONNECTING -> BUFFERING -> 503
  SERVICE UNAVAILABLE) with its own `ServerBossEvent`. Signature kit, all
  built the same telegraph-then-fire way `TheManager` does:
  - **cable-whip melee** - forward-cone reach attack (further than his fists)
    that reels the target in, on top of ordinary `MeleeAttackGoal` swings.
  - **LATENCY** - rubber-bands the player's position for a run of ticks out
    of every ten (`LatencyTracker`); a safe, non-network-layer stand-in for
    real packet delay - see that class's doc for why.
  - **PACKET LOSS** - opens a window where a chance of the player's hits on
    him simply do not register (`hurt()` returns false with feedback,
    doesn't touch the Forge event bus at all).
  - **BUFFERING** - he is genuinely invulnerable while a loading ring fills;
    the ring IS the windup timer, rendered as a halo over his head that
    fills clockwise. Enough cumulative damage during the fill breaks it
    early (and stuns him, a real punish window); let it complete and he
    unloads a THROTTLE pulse the instant it finishes.
  - **THROTTLE** - an AoE slow/mining-fatigue pulse on everything in range
    (players AND mobs), himself exempted.
- **Loot**: `static_ip` (the ability-gate item, see `InternetLoot.
  hasStaticIp`), `fiber_optic_coil` (crafting material), `managers_headset`
  (trophy), plus vanilla redstone/copper.

## Registered ids (all via this module's own registries, `InternetContent`)

| kind   | id                              | class                    |
|--------|----------------------------------|---------------------------|
| entity | `barbarajones:internet_manager`  | `InternetManagerBoss`     |
| block  | `barbarajones:service_call_box`  | `ServiceCallBoxBlock`     |
| item   | `barbarajones:service_call_box`  | `BlockItem`               |
| item   | `barbarajones:rotary_phone`      | `RotaryPhoneItem`         |
| item   | `barbarajones:static_ip`         | `Item`                    |
| item   | `barbarajones:fiber_optic_coil`  | `Item`                    |
| item   | `barbarajones:managers_headset`  | `Item`                    |
| item   | `barbarajones:internet_manager_spawn_egg` | `ForgeSpawnEggItem` |

No custom sounds, no custom `MobEffect`, no loot table JSON (death loot is
`dropCustomDeathLoot`, same as `TheManager`/`MomCobbBoss`), no menu/GUI, no
custom network packets - everything rides vanilla `SoundEvents`, vanilla
title/boss-bar packets, and vanilla `MobEffectInstance`s.

## The one entry point

`InternetContent.init(IEventBus bus)` exists as the stable hook the rule
asks for, but it currently has nothing to do at mod-construction time -
**everything that actually needs wiring is the three `DeferredRegister`s
themselves.** Everything else (attribute creation, entity renderer/layer
registration, the tick scheduler, the packet-loss/latency logic) is a
self-registering `@Mod.EventBusSubscriber` class in this package or
`.client`, exactly like `ExtraRegistry`/`ExtraClientSetup` - **zero
additional wiring needed for any of that.**

## What the orchestrator must change centrally

1. **`BarbaraJonesMod.java`** - add these three lines alongside the existing
   `ModXxx.Xxx.register(bus)` calls in the constructor:
   ```java
   InternetContent.ITEMS.register(bus);
   InternetContent.BLOCKS.register(bus);
   InternetContent.ENTITIES.register(bus);
   ```
   (`InternetContent.init(bus)` may also be called for forward-compatibility
   but does nothing today.)

2. **`ModTabs.java`** (optional, cosmetic) - `MAIN`'s `displayItems` only
   iterates `ModItems.ITEMS.getEntries()`, so none of this module's items
   show up by browsing the creative tab until a second `forEach` is added
   for `InternetContent.ITEMS.getEntries()`. Everything is still obtainable
   by crafting, by `/give`, and the entity by spawn egg or `/summon` in the
   meantime - this is a visibility gap, not a functional one.

3. **`entity/CaydenCobb.java`** (I did **not** touch this file - another
   agent may be in it). `demandFor(LivingEntity foe)` (around line 1159-1162
   as of this writing) needs exactly one new branch, inserted before the
   final `else { return 0; }`:
   ```java
   } else if (foe instanceof com.barbarajones.v2.internet.InternetManagerBoss) {
       base = AscensionLadder.GOD;
   } else {
       return 0;
   }
   ```
   That is the entire change. `scanForBoss()` needs **no** change at all -
   it already scans every `LivingEntity` within 160 blocks
   (`BOSS_SCAN_RANGE`) and calls `demandFor` on each one; since
   `InternetManagerBoss` is a normal `Monster` (a `LivingEntity`), it is
   picked up automatically the instant the branch above exists. Fighting
   this boss will then push Cayden to tier `AscensionLadder.GOD` (4) exactly
   the way fighting `TheManager`/a `Warden` pushes him to SSJ2 today - one
   rung higher, since this fight is meant to be the harder of the two.

4. **`client/render/SsjAuraLayer.java`** - **verified, no change needed.**
   I read this file end to end rather than assuming: its `render()` method
   already special-cases `tier == 4` (`AscensionLadder.GOD`) with a red tint
   (`tr=1.0F, tg=0.15F, tb=0.18F`, the `case 4 ->` arm of the per-rung
   `switch`, currently around line 70), applied through the existing
   `TintedVertexConsumer` wrapper whenever the tint isn't pure white. That
   path is real and already exercised today (the Krave Monster's fourth
   incarnation already demands GOD), so once step 3 above lands, fighting
   this boss will render Cayden's Super Saiyan God aura red with **zero**
   changes to this file. If it ever renders gold/red-orange instead of true
   red during this fight specifically, the bug is that `entity.getTier()`
   isn't actually reaching 4 (a `CaydenCobb`/`AscensionLadder` problem, most
   likely step 3 not having landed yet or the player not having GOD
   unlocked on the ascension ladder) - it is not a rendering bug, and the
   fix is not in this file.

## The village gate

`OutageEvent` cannot leave `SCHEDULED` (nor accept a manual call) unless
`CaydenCobb.isHoused()` is true for at least one loaded, tamed Cayden in that
level (`villageExists`, backed by the existing `HousingValidator`). This
module does **not** invent a settlement/village system - there isn't one in
the codebase yet (confirmed: no `Village`/`Settlement` class anywhere,
`housing/HousingValidator` is per-room, not per-settlement) - it piggybacks
on the one real, working "the player has built somewhere to live" signal that
already exists. If a proper settlement system lands later, swapping
`villageExists()`'s implementation is a one-method change contained entirely
in `OutageEvent`.

## Honest gaps / what is NOT finished

- **No custom audio.** `.tools/ffmpeg/ffmpeg.exe` is not present in this
  environment (checked before starting), so I did not attempt the WAV-synth
  + ffmpeg-encode pipeline `make_alarm.ps1`/`make_krave_audio.ps1` use.
  Every sound cue in this module (siren, dread hum, dial tone, buffering
  hum, packet-loss glitch, throttle pulse, death line) is a vanilla
  `SoundEvents` constant chosen for the closest thematic fit, the same way
  `TheManager` leans on vanilla sounds almost entirely. A bespoke sound set
  would be a strict improvement if ffmpeg becomes available.
- **No creative-tab entry** until `ModTabs.java` gets the second
  `displayItems` line (see above) - a known, deliberate gap, not an
  oversight.
- **Cancellation has no player-facing command.** `OutageEvent.cancel
  (ServerLevel)` is a public static API, fully functional, but nothing
  currently calls it - it's there for a future `/` command or admin tool to
  hook into.
- **Numbers are untested.** I cannot run Gradle in this environment (rule
  1), so none of the fight's tuning (`MAX_HEALTH 320`, damage values,
  windup lengths, the 42-damage buffer-break threshold, the 4-10 day random
  schedule window) has been played. Treat every constant in
  `InternetManagerBoss`/`OutageEvent` as a first-pass estimate the
  orchestrator or a playtest pass should retune, not as balanced.
- **Pre-existing bug found, not mine to fix**: neither `TheManager` nor
  `ManagerMinion` (in `boss/manager/`) have an `EntityAttributeCreationEvent`
  registration anywhere in the codebase - `ModEntityAttributes.REQUIRED`
  doesn't list them and no `createAttributes()` call for either exists
  outside their own classes. Per that file's own documented crash mode, both
  mobs should NPE the instant they're constructed (null attribute map). This
  module's own boss avoids the same fate via its own self-registered
  `InternetOutageEvents.Registrar` (`EntityAttributeCreationEvent`,
  MOD-bus) rather than by touching `ModEntityAttributes.java`. Flagged
  separately rather than fixed here - it's a different module's mob.

## Files

```
src/main/java/com/barbarajones/v2/internet/
  InternetContent.java          registries + init(bus)
  InternetLoot.java              STATIC_IP_ID / hasStaticIp() ability gate
  InternetManagerBoss.java       the boss
  InternetOutageEvents.java      FORGE-bus ticking + MOD-bus attribute registrar
  LatencyTracker.java            the LATENCY rubber-band, process-lifetime only
  OutageEvent.java                SavedData scheduler/runner
  RotaryPhoneItem.java
  ServiceCallBoxBlock.java
  client/
    InternetClientSetup.java     renderer/layer registration (self-registered)
    InternetManagerModel.java
    InternetManagerRenderer.java

src/main/resources/assets/barbarajones/
  lang/internet_outage.json
  textures/entity/internet_manager.png
  textures/item/{rotary_phone,static_ip,fiber_optic_coil,managers_headset}.png
  textures/block/service_call_box_{top,side,front}.png
  models/item/{rotary_phone,static_ip,fiber_optic_coil,managers_headset,
               service_call_box,internet_manager_spawn_egg}.json
  models/block/service_call_box.json
  blockstates/service_call_box.json

src/main/resources/data/barbarajones/recipes/
  rotary_phone.json
  service_call_box.json

tools/make_internet_manager.ps1   textures for all of the above (run, verified)
```
