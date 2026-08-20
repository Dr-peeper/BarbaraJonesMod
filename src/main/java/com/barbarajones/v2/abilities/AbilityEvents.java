package com.barbarajones.v2.abilities;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.abilities.item.GodCoreItem;
import com.barbarajones.boss.manager.TheManager;
import com.barbarajones.boss.mom.MomCobbBoss;
import com.barbarajones.content.ModSounds;
import com.barbarajones.quest.Quests;
import com.barbarajones.v2.quests.QuestApi;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Every piece of "what does the world do to make this system real" lives
 * here: granting unlocks off boss kills and quest completions, ticking the
 * three abilities that run for a window instead of firing once, and the one
 * damage-event hook Instinct Band and God Core both need.
 *
 * <p>Default bus (FORGE) for everything except the nested {@link TabInjector},
 * which needs the MOD bus - same split {@code KraveKeys} uses for its own
 * nested {@code Registrar}.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID)
public final class AbilityEvents {

    /** How often (in player ticks) the quest-completion watcher re-checks each player. Cheap, so no need to rush it. */
    private static final int WATCH_INTERVAL = 30;
    /** The newer quest module's own key for this same power - see {@code AbilityUnlocks}'s class doc. */
    private static final String GODCORE_ALT_KEY = "i_krave_the_krave";

    private AbilityEvents() { }

    // ---- boss-kill unlocks ---------------------------------------------------

    /**
     * Same call as {@code ProgressionEvents} makes for the Krave Monster:
     * credit everyone standing in the fight, not just whoever's hit landed
     * last. Cayden and Barbara do plenty of the actual damage in both of
     * these fights.
     */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) {
            return;
        }
        if (dead instanceof TheManager) {
            grantNearby(dead, AbilityId.LASER);
        } else if (dead instanceof MomCobbBoss) {
            grantNearby(dead, AbilityId.CHARM);
        }
    }

    private static void grantNearby(LivingEntity boss, AbilityId id) {
        for (Player player : boss.level().getEntitiesOfClass(Player.class,
                boss.getBoundingBox().inflate(48.0D, 32.0D, 48.0D))) {
            if (player instanceof ServerPlayer sp) {
                AbilityData.grant(sp, id);
            }
        }
    }

    // ---- per-tick: quest watcher, Charm's flight, God Core's window --------

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        try {
            if (player.tickCount % WATCH_INTERVAL == 0) {
                watchUnlocks(player);
            }
            long now = player.level().getGameTime();
            if (AbilityData.isActive(player, AbilityId.CHARM, now)) {
                tickCharm(player);
            }
            tickGodCore(player, now);
        } catch (Throwable ignored) {
            // an ability desync must never take a player's tick down with it
        }
    }

    /**
     * Quest-gated abilities, polled off the base questline
     * ({@code Quests.isDone}, which also answers for the expansion branches
     * that splice into the same book). {@link AbilityId#GODCORE} accepts a
     * second, alternate path here too: the newer quest module's
     * {@code QuestApi.hasAbility} under its own key for the same power - see
     * {@code AbilityUnlocks}'s class doc for why that costs nothing to check.
     */
    private static void watchUnlocks(ServerPlayer player) {
        for (AbilityId id : AbilityId.VALUES) {
            if (AbilityData.isUnlocked(player, id)) {
                continue;
            }
            String quest = AbilityUnlocks.questFor(id);
            boolean earned = quest != null && Quests.isDone(player, quest);
            if (!earned && id == AbilityId.GODCORE) {
                earned = safeHasAltAbility(player);
            }
            if (earned) {
                AbilityData.grant(player, id);
            }
        }
    }

    /** {@code v2.quests} is a separate, actively-developed module - never let a hiccup in it break this watcher. */
    private static boolean safeHasAltAbility(ServerPlayer player) {
        try {
            return QuestApi.hasAbility(player, GODCORE_ALT_KEY);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** The lift and trail for an open Ascension Charm window - see the item's own doc for why it is split this way. */
    private static void tickCharm(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 move = player.getDeltaMovement();
        double lift = 0.10D + Math.max(0.0D, look.y) * 0.08D;
        double forward = 0.22D;
        player.setDeltaMovement(move.scale(0.86D)
                .add(look.x * forward, lift, look.z * forward));
        player.fallDistance = 0.0F;
        if (player.tickCount % 3 == 0 && player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 0.2D, player.getZ(), 2, 0.15D, 0.05D, 0.15D, 0.01D);
        }
    }

    /**
     * God Core's buffs are applied the instant the item fires (see
     * {@code GodCoreItem#activate}) so there is no one-tick delay on the
     * power spike. This only has to notice the window closing and take them
     * back off, plus keep the aura visibly running while it lasts.
     */
    private static void tickGodCore(ServerPlayer player, long now) {
        boolean active = AbilityData.isActive(player, AbilityId.GODCORE, now);
        boolean buffed = GodCoreItem.isBuffed(player);
        if (active && !buffed) {
            GodCoreItem.applyBuffs(player);   // safety net - covers a state that predates a server restart
        } else if (!active && buffed) {
            GodCoreItem.removeBuffs(player);
        }
        if (active && player.tickCount % 5 == 0 && player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + player.getBbHeight() * 0.5D, player.getZ(),
                    4, 0.35D, 0.5D, 0.35D, 0.01D);
        }
    }

    // ---- damage: Instinct Band's total dodge, God Core's reduction ---------

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }
        long now = player.level().getGameTime();
        if (AbilityData.isActive(player, AbilityId.BAND, now)) {
            event.setCanceled(true);
            if (player.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + player.getBbHeight() * 0.6D, player.getZ(),
                        10, 0.3D, 0.4D, 0.3D, 0.12D);
                level.playSound(null, player.blockPosition(), ModSounds.KRAVE_SCREECH.get(),
                        player.getSoundSource(), 0.7F, 2.0F);
            }
            return;
        }
        if (AbilityData.isActive(player, AbilityId.GODCORE, now)) {
            event.setAmount(event.getAmount() * GodCoreItem.DAMAGE_TAKEN_MULTIPLIER);
        }
    }

    // ---- sync on the moments a client's world state actually changes -------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            AbilityData.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            AbilityData.sync(sp);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            AbilityData.sync(sp);
        }
    }

    // ---- creative tab: MOD bus, so it is a nested subscriber -----------------

    /**
     * Injects the six ability items into the mod's single creative tab
     * ({@code ModTabs.MAIN}) without editing {@code ModTabs.java} - that file
     * is off limits, and this event exists specifically so other mods (and,
     * here, other modules of this one) can add to a tab they do not own.
     */
    @Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class TabInjector {

        private TabInjector() { }

        @SubscribeEvent
        public static void onBuildTab(BuildCreativeModeTabContentsEvent event) {
            if (!event.getTabKey().equals(com.barbarajones.content.ModTabs.MAIN.getKey())) {
                return;
            }
            for (AbilityId id : AbilityId.VALUES) {
                event.accept(AbilityItems.itemFor(id));
            }
        }
    }
}
