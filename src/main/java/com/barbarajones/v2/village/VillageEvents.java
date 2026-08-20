package com.barbarajones.v2.village;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.village.net.PacketVillageStatus;
import com.barbarajones.v2.village.net.VillageNetwork;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Every hook the settlement system needs into the running game.
 *
 * <p>Registered by annotation on the Forge bus, so the module needs no central
 * wiring beyond {@link KraveVillage#init}. Every handler starts by checking whether
 * this dimension has a village table at all
 * ({@link KraveVillageData#getExisting}) and returns immediately if not, which is
 * the overwhelmingly common case - a world with no villages pays almost nothing for
 * having this module installed.
 *
 * <h2>What is hooked, and why</h2>
 * <ul>
 *   <li><b>Level tick</b> - drives {@link Village#tick}, once every
 *       {@value Village#VILLAGE_TICK_INTERVAL} ticks rather than every tick.
 *   <li><b>Block place / break</b> - keeps the tracked-building map responsive.
 *       The rolling sweep would find these eventually; these two handlers are what
 *       make placing a bed move the counter <em>now</em>.
 *   <li><b>Living hurt</b> - where the defence rating actually does something.
 *   <li><b>Living death</b> - prunes dead residents, and makes the town sad about
 *       it.
 *   <li><b>Player tick</b> - pushes HUD status, deduplicated so a settled village
 *       sends nothing at all.
 * </ul>
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID)
public final class VillageEvents {

    /** How often a player is checked against the village they are standing in. */
    private static final int STATUS_INTERVAL = 40;

    /** Resend period for an unchanged status. Must stay under the client's stale window. */
    private static final int HEARTBEAT_INTERVAL = 80;

    /**
     * Last status each player was sent, so an unchanging village costs no packets.
     * Keyed by player UUID and cleared on logout; a stale entry for a player who
     * never returns is one small object, and clearing it on logout keeps even that
     * from accumulating on a long-running server.
     */
    private static final Map<UUID, PacketVillageStatus> LAST_SENT = new HashMap<>();

    private VillageEvents() { }

    // ---- the heartbeat -------------------------------------------------------

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        if (level.getGameTime() % Village.VILLAGE_TICK_INTERVAL != 0) {
            return;
        }
        KraveVillageData data = KraveVillageData.getExisting(level);
        if (data == null || data.isEmpty()) {
            return;
        }
        data.tickAll(level);
    }

    // ---- keeping the building map honest -------------------------------------

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Block block = event.getPlacedBlock().getBlock();
        if (!KraveVillage.hasBuff(block)) {
            return;
        }
        KraveVillageData data = KraveVillageData.getExisting(level);
        if (data == null) {
            return;
        }
        Village village = data.containing(event.getPos());
        if (village != null && village.noteBlockPlaced(event.getPos(), block)) {
            data.setDirty();
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        KraveVillageData data = KraveVillageData.getExisting(level);
        if (data == null) {
            return;
        }
        Village village = data.containing(event.getPos());
        if (village != null && village.noteBlockRemoved(event.getPos())) {
            data.setDirty();
        }
    }

    // ---- defence -------------------------------------------------------------

    /**
     * Where walls become numbers.
     *
     * <p>Applies only to hostile damage - a mob or an explosion - landing on
     * something that is not itself a monster, inside a claim. Fall damage, drowning,
     * the void and friendly fire are all untouched: a village should protect you
     * from the raid, not from gravity.
     *
     * <p>Other modules should call
     * {@link KraveVillage#raidDamageMultiplier(ServerLevel, BlockPos)} rather than
     * duplicating this, and must not apply it a second time to a hit that has
     * already passed through here.
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        if (victim instanceof Monster) {
            return;
        }
        if (!isHostileDamage(event)) {
            return;
        }
        KraveVillageData data = KraveVillageData.getExisting(level);
        if (data == null) {
            return;
        }
        Village village = data.containing(victim.blockPosition());
        if (village == null || village.defence() <= 0) {
            return;
        }
        float multiplier = village.view().raidDamageMultiplier();
        if (multiplier < 0.999F) {
            event.setAmount(event.getAmount() * multiplier);
        }
    }

    private static boolean isHostileDamage(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof Monster) {
            return true;
        }
        return event.getSource().is(DamageTypeTags.IS_EXPLOSION);
    }

    // ---- population bookkeeping ---------------------------------------------

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) {
            return;
        }
        KraveVillageData data = KraveVillageData.getExisting(level);
        if (data == null) {
            return;
        }
        if (dead instanceof KraveVillagerEntity) {
            data.forgetVillager(dead.getUUID());
            // Losing a neighbour is the one thing that should visibly hurt a town.
            Village village = data.containing(dead.blockPosition());
            if (village != null) {
                village.adjustHappiness(-8);
                data.setDirty();
            }
        }
    }

    // ---- HUD sync ------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % STATUS_INTERVAL != 0) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        KraveVillageData data = KraveVillageData.getExisting(level);
        PacketVillageStatus status = PacketVillageStatus.NONE;
        if (data != null) {
            Village village = data.containing(player.blockPosition());
            if (village != null) {
                status = new PacketVillageStatus(village.view());
            }
        }
        sendIfChanged(player, status);
    }

    /**
     * Sends the status only when it changed, plus a heartbeat.
     *
     * <p>The heartbeat is not optional and its period is not arbitrary. The client
     * treats a status older than six seconds as stale and hides the HUD, so a
     * settled village - where nothing changes for minutes at a time - would
     * otherwise flicker out. {@value #HEARTBEAT_INTERVAL} ticks is four seconds,
     * comfortably inside that window, and it is a multiple of
     * {@value #STATUS_INTERVAL} so it actually lands on a tick this handler runs on.
     * Standing outside every claim needs no heartbeat at all: the HUD is already
     * hidden and staying hidden is the correct outcome.
     */
    private static void sendIfChanged(ServerPlayer player, PacketVillageStatus status) {
        PacketVillageStatus last = LAST_SENT.get(player.getUUID());
        if (status.sameAs(last)
                && (!status.inVillage || player.tickCount % HEARTBEAT_INTERVAL != 0)) {
            return;
        }
        LAST_SENT.put(player.getUUID(), status);
        VillageNetwork.sendTo(player, status);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }

    /** Exposed for tests and for a future /village command. */
    @Nullable
    static PacketVillageStatus lastStatusSentTo(UUID player) {
        return LAST_SENT.get(player);
    }
}
