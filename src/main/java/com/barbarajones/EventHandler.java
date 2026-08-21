package com.barbarajones;

import com.barbarajones.apocalypse.KraveApocalypse;
import com.barbarajones.apocalypse.KraveKosmosAmbience;
import com.barbarajones.apocalypse.KraveKosmosBattle;
import com.barbarajones.content.ModDamageTypes;
import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModSounds;
import com.barbarajones.content.ModFluids;
import com.barbarajones.content.ModItems;
import com.barbarajones.dimension.KraveDimensions;
import com.barbarajones.dimension.KraveKosmosData;
import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.KraveMonster;
import com.barbarajones.entity.KraveMouthBeam;
import com.barbarajones.entity.ThePlug;
import com.barbarajones.net.ModNetwork;
import com.barbarajones.net.PacketKraveHit;
import com.barbarajones.quest.Quests;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.Nullable;

/** Drives the questline, Cayden's arrival, and the Krave Apocalypse. */
public class EventHandler {

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final String PERSIST = Player.PERSISTED_NBT_TAG;

    /** Hand out the books and spawn Cayden the first time you join. */
    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) {
            return;
        }
        boolean firstJoin = Quests.findBook(player) == null;

        // Hand out each starter item independently. This used to be one
        // all-or-nothing block gated on "do you have a Quest Book?", which
        // meant anyone whose world predated a newly added book never received
        // it - and the Required Reading quest, which wants all three at once,
        // became impossible to finish without crafting the missing one.
        ensureHas(player, ModItems.KRAVE_MANUAL.get());
        ensureHas(player, ModItems.QUEST_BOOK.get());
        ensureHas(player, ModItems.RECIPE_BOOK.get());
        ensureHas(player, ModItems.HOUSING_QUERY.get());

        if (firstJoin) {
            player.sendSystemMessage(Component.literal(ChatFormatting.GOLD
                    + "Read THE KRAVE MANUAL first. Rule #1 is on page one."));
            Quests.onFirstJoin(player);
        }

        CompoundTag persist = persisted(player);
        if (!persist.getBoolean("KraveCaydenSpawned") && player.level() instanceof ServerLevel level) {
            // Never let a cosmetic companion spawn stop a player joining the world:
            // if anything here throws, log it and let the login continue.
            try {
                CaydenCobb cayden = ModEntities.CAYDEN.get().create(level);
                if (cayden != null) {
                    cayden.setPos(player.getX() + 1.0D, player.getY(), player.getZ() + 1.0D);
                    cayden.tame(player);
                    level.addFreshEntity(cayden);
                    persist.putBoolean("KraveCaydenSpawned", true);
                    player.sendSystemMessage(Component.literal(ChatFormatting.AQUA
                            + "Cayden Cobb tagged along. Feed him Krave - and build him a house."));
                }
            } catch (Throwable err) {
                LOGGER.error("Failed to spawn Cayden on login; continuing without him", err);
            }
        }
    }

    /**
     * Give the player this item if they do not already have one. Replaces a
     * lost or never-granted starter book without duplicating it every login.
     */
    private void ensureHas(Player player, net.minecraft.world.item.Item item) {
        if (player.getInventory().contains(new ItemStack(item))) {
            return;
        }
        if (!player.getInventory().add(new ItemStack(item))) {
            player.drop(new ItemStack(item), false);
        }
    }

    /**
     * How many Krave Monster forms this player has put down, so the next one
     * arrives one step further along. Stored per player rather than per world:
     * the escalation is that player's story.
     */
    public static int kraveFormsBeaten(Player player) {
        return persistedOf(player).getInt("KraveFormsBeaten");
    }

    /** Which form should spawn for this player next. */
    public static int nextKraveForm(@Nullable Player player) {
        if (player == null) {
            return 1;
        }
        // Always the first incarnation. The escalation happens WITHIN the fight
        // now - he stands back up as the next form each time he falls - so a
        // summon that started at form 3 would skip most of the gauntlet.
        return 1;
    }

    /** Static twin of persisted(), for the spawn sites that have no handler instance. */
    private static CompoundTag persistedOf(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(PERSIST)) {
            data.put(PERSIST, new CompoundTag());
        }
        return data.getCompound(PERSIST);
    }

    /**
     * Stands the Krave Monster back up one incarnation stronger, where he fell.
     *
     * <p>Spawned on the spot rather than after a delay: a gap long enough to
     * walk away from would let the player treat each form as a separate errand,
     * and the escalation only lands if it is one continuous fight that keeps
     * getting worse under them.
     */
    /**
     * Puts the Kosmos boss back at his den after an unscripted death.
     *
     * <p>He is only supposed to die at the end of his last finisher. Anything
     * else - a command, falling out of the world - would otherwise leave the
     * dimension permanently bossless, because the den build is behind a
     * one-time flag that will never run again.
     *
     * <p>Comes back DORMANT at the form he had reached, so the confrontation
     * runs again and progress is not lost.
     */
    private void reseatKosmosBoss(ServerLevel level, KraveMonster fallen, int form) {
        ServerLevel kosmos = level.getServer().getLevel(KraveDimensions.KRAVE_KOSMOS);
        if (kosmos == null) {
            return;
        }
        KraveMonster next = ModEntities.KRAVE_MONSTER.get().create(kosmos);
        if (next == null) {
            return;
        }
        net.minecraft.world.phys.Vec3 den = com.barbarajones.dimension.KraveDimensions.BOSS_ISLAND;
        next.setPos(den.x, den.y + com.barbarajones.dimension.KraveDenBuilder.DEN_HEIGHT_OFFSET, den.z);
        next.setForm(Math.max(1, form));
        next.markScriptedEncounter();
        kosmos.addFreshEntity(next);
        KraveKosmosData.get(kosmos).setBossId(next.getUUID());
        LOGGER.info("Kosmos boss died outside its finisher; reseated at the den, dormant, form {}.",
                next.getForm());
    }

    private void reviveNextForm(ServerLevel level, KraveMonster fallen, int nextForm) {
        if (fallen.isScriptedEncounter()) {
            // The scripted encounter advances through its finisher, never through
            // death. Reaching here at all means he died some other way - a
            // command, the void - so put him back at the den, dormant, and let
            // the confrontation be attempted again. Silently doing nothing would
            // leave the Kosmos with no boss and no way to get one back.
            reseatKosmosBoss(level, fallen, nextForm);
            return;
        }
        KraveMonster next = ModEntities.KRAVE_MONSTER.get().create(level);
        if (next == null) {
            return;
        }
        next.moveTo(fallen.getX(), fallen.getY(), fallen.getZ(), fallen.getYRot(), 0.0F);
        next.setForm(nextForm);
        // Straight back into the fight. This is the death-driven gauntlet, which
        // is still how the independent summons escalate - the Kosmos resident
        // uses the scripted finisher instead and never reaches this path.
        next.spawnHostile();
        next.setTarget(fallen.getTarget());
        level.addFreshEntity(next);

        // Hand the Kosmos boss identity to the new body. Reviving spawns a NEW
        // entity with a NEW uuid, but KraveKosmosData still held the ORIGINAL
        // one - so when the final form finally fell, the uuid check in onDeath
        // did not match and setBossEverDefeated never ran. Killing the Kosmos
        // boss simply never registered, and anything gated behind having beaten
        // him stayed locked forever.
        ServerLevel kosmosLevel = level.getServer().getLevel(KraveDimensions.KRAVE_KOSMOS);
        if (kosmosLevel != null) {
            KraveKosmosData bossData = KraveKosmosData.get(kosmosLevel);
            if (fallen.getUUID().equals(bossData.getBossId())) {
                bossData.setBossId(next.getUUID());
            }
        }

        // Vanilla's ~1s death animation (plus his own ghost-trail afterimage
        // effect) used to leave the collapsing old body on screen at the same
        // moment the new form spawned in the same spot - two Krave Monsters
        // visible at once, which reads as "he duplicated" rather than "he got
        // back up." Discarding him immediately instead of letting death play
        // out makes the hand-off read as one boss, not two.
        fallen.discard();

        level.playSound(null, fallen.blockPosition(),
                nextForm >= 4 ? ModSounds.MONSTER_ROAR_2.get() : ModSounds.KRAVE_ROAR.get(),
                SoundSource.HOSTILE, 2.4F, 0.45F);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                fallen.getX(), fallen.getY() + 1.0D, fallen.getZ(), 2, 1.5D, 1.0D, 1.5D, 0.0D);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                fallen.getX(), fallen.getY() + 1.0D, fallen.getZ(), 120, 2.0D, 2.0D, 2.0D, 0.25D);

        // One incarnation per rung now (SSJ through Ultra Instinct) instead
        // of four - each line escalates the same way the Cayden fight does.
        String line = switch (nextForm) {
            case 2 -> "It gets back up. SECOND FORM.";
            case 3 -> "That was not all of it either. THIRD FORM.";
            case 4 -> "It stops being an animal about it. GOD FORM.";
            case 5 -> "Cold, and perfectly still. BLUE FORM.";
            default -> "FINAL FORM. There is nothing after this one.";
        };
        for (Player p : level.getEntitiesOfClass(Player.class,
                fallen.getBoundingBox().inflate(72.0D))) {
            p.sendSystemMessage(Component.literal(ChatFormatting.DARK_PURPLE + ""
                    + ChatFormatting.BOLD + line));
        }
    }

    @SubscribeEvent
    public void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        com.barbarajones.diag.ServerStallWatchdog.start(event);
    }

    @SubscribeEvent
    public void onServerStopping(net.minecraftforge.event.server.ServerStoppingEvent event) {
        com.barbarajones.diag.ServerStallWatchdog.stop(event);
    }

    /** Run the apocalypse cutscene. */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        // Stamp first: if something below never returns, the watchdog needs the
        // timestamp from BEFORE the stall, not after it.
        com.barbarajones.diag.ServerStallWatchdog.heartbeat();
        if (event.phase == TickEvent.Phase.END) {
            KraveApocalypse.tickAll();
            KraveKosmosBattle.tickAll();
            KraveKosmosAmbience.tick();
            com.barbarajones.dimension.KraveChocolateWeather.tick();
        }
    }

    /**
     * Liquid chocolate hurts like lava but deliberately doesn't set anything
     * ablaze - no setSecondsOnFire() call here. It's hot, not literally fire;
     * the visible burning overlay would look wrong on something brown. Cayden
     * reacts differently (see CaydenCobb.tick(), which checks the same fluid
     * type) so he's excluded here rather than taking damage on top of
     * transforming.
     *
     * <p>Also drags movement down the same way lava does - but that part
     * does NOT come for free just from the fluid's density/viscosity/
     * motionScale being set to lava's own values. Vanilla's actual "wading
     * through lava is slow" physics in LivingEntity.travel() is gated behind
     * isInLava(), which reads the vanilla lava fluid TAG specifically - a
     * custom FluidType never satisfies that check no matter how it's
     * configured, so without this, chocolate swam exactly like water with a
     * brown tint. Tagging chocolate into #minecraft:lava instead was
     * considered and rejected: that tag also drives vanilla's own lavaHurt(),
     * which calls setSecondsOnFire() - exactly the ignite behavior the
     * paragraph above deliberately avoids. Scaling velocity down by hand
     * here gets the sluggish feel without the side effect.
     *
     * <p>Visibility (see ModFluids.CHOCOLATE_TYPE's client extensions) is
     * fog-only now, matching real lava exactly - the earlier version also
     * had a water-style getOverlayTexture() HUD blit, which lava has never
     * had (only water does), plus a vanilla Darkness effect bolted on here
     * as a guaranteed-but-wrong-looking stand-in while the fog hook's actual
     * behavior was unverified. Both removed - trust the fog hook, since it
     * is the real, documented Forge extension point for exactly this.
     */
    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = (LivingEntity) event.getEntity();
        if (entity.level().isClientSide || entity instanceof CaydenCobb) {
            return;
        }
        if (entity.getFluidTypeHeight(ModFluids.CHOCOLATE_TYPE.get()) > 0.0D) {
            entity.hurt(ModDamageTypes.of(entity.level(), ModDamageTypes.CHOCOLATE), 2.0F);
            // Same per-tick momentum decay vanilla applies to an entity it
            // already recognizes as lava-slowed.
            Vec3 slowed = entity.getDeltaMovement().scale(0.5D);
            entity.setDeltaMovement(slowed);
        }
    }

    /** Watch for the player's first box of Krave. */
    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player player = event.player;
        if (player.tickCount % 40 != 0) {
            return;
        }
        // Sweep the whole graph: any collect quest whose items are now all in the
        // inventory auto-completes, and milestones cascade behind them.
        Quests.tick(player);

        // Duhl Wol: daily spawn and tracking
        tickDuhlWol(player);
    }

    private void tickDuhlWol(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        CompoundTag persist = persisted(player);
        long gameDay = player.level().getDayTime() / 24000L;

        // First sighting: a fresh player has no DuhlLastDay, and an unset int
        // reads back as 0 - the same value gameDay holds on day one. Comparing
        // them meant he could never turn up in a brand new world, which is
        // exactly where you would go looking for him. Seed the marker a day
        // behind so the very first day still counts as a new one.
        if (!persist.contains("DuhlLastDay")) {
            persist.putInt("DuhlLastDay", (int) gameDay - 1);
        }
        int lastDay = persist.getInt("DuhlLastDay");

        if (gameDay > lastDay) {
            // Clear the debt BEFORE deciding whether he shows up - the old
            // order let yesterday paid flag suppress today visit.
            persist.putInt("DuhlLastDay", (int) gameDay);
            persist.putBoolean("DuhlPaidToday", false);
            spawnDuhlWol(player);
        }
    }

    private void spawnDuhlWol(Player player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        try {
            com.barbarajones.entity.DuhlWol duhl = ModEntities.DUHL_WOL.get().create(level);
            if (duhl != null) {
                // Spawn near the player
                duhl.setPos(player.getX() + 10.0D, player.getY(), player.getZ() + 10.0D);
                int stage = player.getRandom().nextInt(3);   // random stage 0-2 (dirt, stone, andesite)
                duhl.setOweStage(stage);
                duhl.setTimer(6000);   // 5 minutes (300 ticks * 20 = 6000)
                level.addFreshEntity(duhl);

                // Spawn the car
                com.barbarajones.entity.DuhlWolCar car = ModEntities.DUHL_WOL_CAR.get().create(level);
                if (car != null) {
                    double startX = player.getX() + 20.0D;
                    double startZ = player.getZ() + 20.0D;
                    car.setPos(startX, player.getY(), startZ);
                    car.setState(0);   // arriving
                    car.setTarget(player.getX() + 10.0D, player.getZ() + 10.0D);
                    level.addFreshEntity(car);
                    level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE,
                            SoundSource.MASTER, 1.2F, 0.5F);   // car horn sound
                    // "rap music playing" - loop one of the raw extracted clips from the
                    // car itself while it rolls up, since we can't stream licensed music
                    level.playSound(null, car.blockPosition(), com.barbarajones.content.ModSounds.DIALOGUE[15].get(),
                            SoundSource.RECORDS, 1.0F, 1.0F);   // dialogue_16
                }

                player.sendSystemMessage(Component.literal(ChatFormatting.DARK_RED + "" + ChatFormatting.BOLD
                        + "Duhl Wol pulls up in his car, jumps out, and looks you dead in the eyes."));
                duhl.startArrivalIntro(duhl.getWantedItem().getHoverName().getString());
            }
        } catch (Throwable err) {
            LOGGER.error("Failed to spawn Duhl Wol", err);
        }
    }

    /**
     * The Krave Kosmos's actual tension cue: tell the victim's client to
     * flash a hit vignette when Krave Monster (melee or his mouth beam)
     * lands a hit - replaces the generic ambient Dread vignette that used to
     * run there permanently maxed-out (see DreadClient.tick()).
     */
    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof KraveMonster || attacker instanceof KraveMouthBeam) {
            ModNetwork.sendTo(player, new PacketKraveHit());
        }
    }

    /** Boss and Plug deaths advance the quest; pet deaths trigger the apocalypse. */
    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }

        if (dead instanceof KraveMonster fallen) {
            int form = fallen.getForm();
            for (Player p : level.getEntitiesOfClass(Player.class,
                    dead.getBoundingBox().inflate(64.0D))) {
                CompoundTag persist = persistedOf(p);
                persist.putInt("KraveFormsBeaten",
                        Math.max(persist.getInt("KraveFormsBeaten"), form));
            }

            if (form < KraveMonster.FINAL_FORM) {
                // One summon is the whole gauntlet: he gets straight back up as
                // the next incarnation, in the same spot, without the player
                // having to go and fetch another Krave Box. Killing him is a
                // four-round fight, not four separate errands.
                reviveNextForm(level, fallen, form + 1);
            } else {
                for (Player p : level.getEntitiesOfClass(Player.class,
                        dead.getBoundingBox().inflate(64.0D))) {
                    p.sendSystemMessage(Component.literal(ChatFormatting.GOLD + ""
                            + ChatFormatting.BOLD + "THE KRAVE IS FINALLY DEAD."));
                }
                // Only marks the Kosmos-resident boss as defeated, not any
                // Krave Monster - the Krave Box and the 10th Cayden death
                // both spawn their own separate, fully independent encounter
                // (see KraveBoxItem), so a summoned one dying here must not
                // satisfy this. Compared by UUID against whichever instance
                // KraveDoorBlock.ensureBossExists actually spawned.
                ServerLevel kosmos = level.getServer().getLevel(KraveDimensions.KRAVE_KOSMOS);
                if (kosmos != null) {
                    KraveKosmosData data = KraveKosmosData.get(kosmos);
                    if (fallen.getUUID().equals(data.getBossId())) {
                        data.setBossEverDefeated(true);
                    }
                }
            }
            for (Player player : level.getEntitiesOfClass(Player.class,
                    dead.getBoundingBox().inflate(48.0D, 32.0D, 48.0D))) {
                Quests.complete(player, Quests.SLAY_KRAVE);   // the boss falls; ACT II begins
                Quests.complete(player, Quests.REVENGE);      // may already be avenged if Plug fell first
            }
            return;
        }

        if (dead instanceof ThePlug) {
            if (event.getSource().getEntity() instanceof Player player) {
                Quests.complete(player, Quests.REVENGE);
            }
            return;
        }

        LivingEntity killer = event.getSource().getEntity() instanceof LivingEntity le ? le : null;

        // Only a player's OWN pet dying NEAR them triggers the apocalypse.
        if (dead instanceof CaydenCobb cayden) {
            Player owner = cayden.getOwner() instanceof Player p ? p : null;
            if (cayden.isTame()
                    && (ownerIsNear(owner, cayden) || isUnrecoverableDeath(event.getSource()))
                    && !KraveApocalypse.isActiveNear(level, cayden.position())) {
                int stage = nextDeathStage(owner);
                KraveApocalypse.start(level, cayden.position(), killer, owner,
                        true, cayden.getKraveFed(), cayden.isRageUnlocked(), stage, isEndless(owner));
            }
        } else if (dead instanceof BarbaraJones barbara) {
            Player owner = barbara.getPetOwner();
            if (barbara.isPet()
                    && (ownerIsNear(owner, barbara) || isUnrecoverableDeath(event.getSource()))
                    && !KraveApocalypse.isActiveNear(level, barbara.position())) {
                int stage = nextDeathStage(owner);
                KraveApocalypse.start(level, barbara.position(), killer, owner,
                        false, 0, false, stage, isEndless(owner));
            }
        }
    }

    /** Whether THIS player has crossed the 11-death threshold (see nextDeathStage). */
    private boolean isEndless(@Nullable Player owner) {
        return owner != null && persisted(owner).getBoolean("KraveEndless");
    }

    /**
     * Increment the death count. Deaths 1-10 walk the stages in order. From the
     * ELEVENTH death on, the world simply never stops ending: the stage locks at
     * 10 and the apocalypse re-arms itself forever (see ENDLESS below).
     */
    private int nextDeathStage(@Nullable Player owner) {
        if (owner == null) {
            return 1;
        }
        CompoundTag persist = persisted(owner);
        int count = persist.getInt("KraveDeathStage") + 1;
        persist.putInt("KraveDeathStage", count);

        if (count >= 11) {
            if (!persist.getBoolean("KraveEndless")) {
                persist.putBoolean("KraveEndless", true);
                owner.sendSystemMessage(Component.literal(ChatFormatting.DARK_RED + ""
                        + ChatFormatting.BOLD + "ELEVEN. THERE IS NO STAGE ELEVEN."));
                owner.sendSystemMessage(Component.literal(ChatFormatting.DARK_RED
                        + "It does not stop now. It never stops now."));
            }
            return 10;
        }
        return count;
    }

    private boolean ownerIsNear(@Nullable Player owner, Entity pet) {
        return owner != null && owner.isAlive() && owner.distanceToSqr(pet) < 48.0D * 48.0D;
    }

    /**
     * Whether this death should start the apocalypse even though the owner is
     * nowhere near it.
     *
     * <p>The proximity rule exists so a pet dying in an unloaded corner of the
     * world does not set off a cutscene nobody sees. An out-of-world death is
     * the exception that rule cannot survive: it happens hundreds of blocks
     * below the player by definition, so it always fails the check. Cayden fell
     * off a Kosmos island, died at y=-337, and never came back - no cutscene, no
     * body, nothing. CaydenCobb.onBelowWorld now catches him before this can
     * happen at all; this is the second line of defence for anything that still
     * gets through, such as a death in a dimension without a floor.
     */
    private boolean isUnrecoverableDeath(net.minecraft.world.damagesource.DamageSource source) {
        return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)
                || source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD);
    }

    private CompoundTag persisted(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(PERSIST)) {
            data.put(PERSIST, new CompoundTag());
        }
        return data.getCompound(PERSIST);
    }
}
