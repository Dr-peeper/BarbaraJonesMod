package com.barbarajones.progression;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.apocalypse.KraveApocalypse;
import com.barbarajones.content.ModItems;
import com.barbarajones.dimension.KraveDimensions;
import com.barbarajones.entity.BarbaraJones;
import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.entity.KraveMinion;
import com.barbarajones.entity.KraveMonster;
import com.barbarajones.entity.ThePlug;
import com.barbarajones.quest.Quests;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Every way a player earns Krave XP, in one place.
 *
 * <p>This class deliberately owns no state: it reads and writes the persistent
 * counters in {@link KraveLevel} / {@link PlayerStats} and pushes a sync packet
 * when they move. Registered by annotation on the Forge bus, so nothing needs
 * to be added to the mod's constructor.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID)
public final class ProgressionEvents {

    // ---- payouts ------------------------------------------------------------
    private static final int XP_QUEST          = 45;
    private static final int XP_FEED_CAYDEN    = 12;
    private static final int XP_SMOKE          = 10;
    private static final int XP_PLUG_TRADE     = 30;
    private static final int XP_PLUG_SCAM      = 250;
    private static final int XP_KOSMOS_FIRST   = 300;
    private static final int XP_KOSMOS_RETURN  = 25;
    private static final int XP_KRAVE_MONSTER  = 500;
    private static final int XP_THE_PLUG       = 300;
    private static final int XP_KRAVE_MINION   = 10;
    /** Base payout for living through an apocalypse, before the per-stage bonus. */
    private static final int XP_APOCALYPSE     = 120;
    private static final int XP_APOCALYPSE_PER_STAGE = 40;

    /**
     * How long after a pet's death the owner must still be breathing to count
     * as having survived, in ticks. The longest staged cinematic runs 578 ticks
     * (see KraveApocalypse's totalDur), so 900 covers it plus the fall at the end.
     */
    private static final int SURVIVAL_WINDOW = 900;

    /** Game time at which a pending survival check pays out; 0 when nothing is pending. */
    private static final String KEY_SURVIVE_AT = "KraveSurviveAt";
    /** Quest count already converted to XP, so the sweep never double-pays. */
    private static final String KEY_QUESTS_PAID = "KraveQuestsPaid";
    /** Written by EventHandler.nextDeathStage - read only, never touched here. */
    private static final String KEY_DEATH_STAGE = "KraveDeathStage";

    private ProgressionEvents() { }

    // ---- the sweep ----------------------------------------------------------

    /**
     * Polls the things that have no event of their own: quest completions (which
     * live on the Quest Book's NBT), the apocalypse survival timer, and Cayden's
     * perk health.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        int tick = player.tickCount;
        if (tick % 20 == 0) {
            payForQuests(player);
            checkSurvival(player);
        }
        if (tick % 40 == 0) {
            refreshCaydenPerk(player);
        }
        if (tick % 100 == 0) {
            // cheap heartbeat: keeps the HUD honest if a client reconnects or
            // an award happened while the player was in another dimension
            KraveLevel.sync(player);
        }
    }

    /**
     * The questline stores progress as a string list on the book, with no event
     * when it grows, so diff the completed count against what we have already
     * paid for. Only ever pays for an increase - losing the book (or crafting a
     * fresh empty one) can never claw XP back or re-pay old quests.
     */
    private static void payForQuests(ServerPlayer player) {
        ItemStack book = Quests.findBook(player);
        if (book == null) {
            return;
        }
        int done = Quests.doneCount(book);
        CompoundTag tag = KraveLevel.data(player);
        int paid = tag.getInt(KEY_QUESTS_PAID);
        if (done <= paid) {
            return;
        }
        int gained = done - paid;
        tag.putInt(KEY_QUESTS_PAID, done);
        PlayerStats.add(player, PlayerStats.Stat.QUESTS_DONE, gained);
        KraveLevel.award(player, XP_QUEST * gained,
                gained == 1 ? "quest complete" : gained + " quests complete");
    }

    private static void checkSurvival(ServerPlayer player) {
        CompoundTag tag = KraveLevel.data(player);
        long due = tag.getLong(KEY_SURVIVE_AT);
        if (due == 0L || player.level().getGameTime() < due) {
            return;
        }
        tag.putLong(KEY_SURVIVE_AT, 0L);
        if (!player.isAlive()) {
            return;                                   // you did not survive it
        }
        int stage = Math.max(1, Math.min(10, tag.getInt(KEY_DEATH_STAGE)));
        int payout = Math.round((XP_APOCALYPSE + XP_APOCALYPSE_PER_STAGE * stage)
                * Perks.apocalypseXpMultiplier(player));

        PlayerStats.add(player, PlayerStats.Stat.APOCALYPSES_SURVIVED, 1);
        KraveLevel.award(player, payout, "you walked out of stage " + stage);
        KraveAdvancements.grant(player, "apocalypse_survivor");
    }

    private static void refreshCaydenPerk(ServerPlayer player) {
        double bonus = Perks.caydenBonusHealth(player);
        if (bonus <= 0.0D) {
            return;                                   // nothing to apply yet - skip the entity scan
        }
        for (CaydenCobb cayden : player.level().getEntitiesOfClass(CaydenCobb.class,
                player.getBoundingBox().inflate(48.0D))) {
            if (cayden.isOwnedBy(player)) {
                Perks.applyToCayden(player, cayden);
            }
        }
    }

    // ---- deaths -------------------------------------------------------------

    /**
     * Runs at LOWEST priority on purpose: EventHandler.onDeath starts the
     * apocalypse at normal priority, so by the time we get here
     * {@link KraveApocalypse#isActiveNear} is a reliable answer to "did that
     * pet's death actually kick one off?".
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) {
            return;
        }

        if (dead instanceof ServerPlayer player) {
            KraveLevel.data(player).putLong(KEY_SURVIVE_AT, 0L);   // no credit for dying
            return;
        }

        Entity source = event.getSource().getEntity();
        ServerPlayer killer = source instanceof ServerPlayer sp ? sp : null;

        if (dead instanceof KraveMonster) {
            // credit everyone who was in the arena: Barbara and Cayden do a lot
            // of the damage, and the last hit is often not the player's
            for (Player nearby : dead.level().getEntitiesOfClass(Player.class,
                    dead.getBoundingBox().inflate(48.0D, 32.0D, 48.0D))) {
                if (nearby instanceof ServerPlayer sp) {
                    PlayerStats.add(sp, PlayerStats.Stat.BOSSES_KILLED, 1);
                    KraveLevel.award(sp, XP_KRAVE_MONSTER, "THE KRAVE MONSTER FELL");
                }
            }
            return;
        }
        if (dead instanceof ThePlug && killer != null) {
            PlayerStats.add(killer, PlayerStats.Stat.BOSSES_KILLED, 1);
            KraveLevel.award(killer, XP_THE_PLUG, "got that mothafucker");
            return;
        }
        if (dead instanceof KraveMinion && killer != null) {
            KraveLevel.award(killer, XP_KRAVE_MINION, "minion down");
            return;
        }

        if (dead instanceof CaydenCobb cayden) {
            if (cayden.isTame() && cayden.getOwner() instanceof ServerPlayer owner) {
                PlayerStats.add(owner, PlayerStats.Stat.CAYDEN_DEATHS, 1);
                KraveLevel.sync(owner);
                armSurvival(owner, dead);
            }
        } else if (dead instanceof BarbaraJones barbara) {
            if (barbara.isPet() && barbara.getPetOwner() instanceof ServerPlayer owner) {
                armSurvival(owner, dead);
            }
        }
    }

    /** Start the survival clock if that death really did open the sky. */
    private static void armSurvival(ServerPlayer owner, LivingEntity dead) {
        if (!KraveApocalypse.isActiveNear(dead.level(), dead.position())) {
            return;
        }
        CompoundTag tag = KraveLevel.data(owner);
        long due = owner.level().getGameTime() + SURVIVAL_WINDOW;
        // a second pet dying mid-cinematic extends the sentence, never shortens it
        tag.putLong(KEY_SURVIVE_AT, Math.max(tag.getLong(KEY_SURVIVE_AT), due));
    }

    // ---- interactions -------------------------------------------------------

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack held = event.getItemStack();
        Entity target = event.getTarget();

        if (target instanceof CaydenCobb && held.is(ModItems.KRAVE_CEREAL.get())) {
            PlayerStats.add(player, PlayerStats.Stat.KRAVE_FED, 1);
            KraveLevel.award(player, XP_FEED_CAYDEN, "Cayden ate");
            return;
        }

        if (!(target instanceof ThePlug)) {
            return;
        }
        if (held.is(ModItems.FIVE_HUNDRED_DOLLARS.get())) {
            PlayerStats.add(player, PlayerStats.Stat.DOLLARS_SCAMMED, 500);
            PlayerStats.add(player, PlayerStats.Stat.PLUG_DEALS, 1);
            KraveLevel.award(player, XP_PLUG_SCAM, "expensive lesson");
        } else if (held.is(Items.EMERALD)) {
            PlayerStats.add(player, PlayerStats.Stat.PLUG_DEALS, 1);
            KraveLevel.award(player, XP_PLUG_TRADE, "street business");
        }
    }

    @SubscribeEvent
    public static void onFinishUsingItem(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack used = event.getItem();
        if (used.is(ModItems.ROLLED_JOINT.get())
                || used.is(ModItems.GOLDEN_JOINT.get())
                || used.is(ModItems.GRASS_BROWNIE.get())) {
            PlayerStats.add(player, PlayerStats.Stat.GRASS_SMOKED, 1);
            KraveLevel.award(player, XP_SMOKE, "Barbara's whole thing");
        }
    }

    // ---- travel and session -------------------------------------------------

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getTo().equals(KraveDimensions.KRAVE_KOSMOS)) {
            boolean first = PlayerStats.isFirst(player, PlayerStats.Stat.KOSMOS_TRIPS);
            PlayerStats.add(player, PlayerStats.Stat.KOSMOS_TRIPS, 1);
            KraveLevel.award(player, first ? XP_KOSMOS_FIRST : XP_KOSMOS_RETURN,
                    first ? "you found the KRAVE KOSMOS" : "back in the Kosmos");
        }
        KraveLevel.sync(player);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            KraveLevel.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        // the new ServerPlayer instance has a fresh client connection state
        if (event.getEntity() instanceof ServerPlayer player) {
            KraveLevel.sync(player);
        }
    }
}
