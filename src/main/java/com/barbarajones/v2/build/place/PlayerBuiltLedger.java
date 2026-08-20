package com.barbarajones.v2.build.place;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * Remembers which blocks a player put there with their own hands.
 *
 * <p>The placement engine needs to answer "is it OK to flatten this?", and the
 * only honest answer for a stone block is "depends who put it there". Vanilla
 * keeps no such record, so this does: one {@link SavedData} per dimension
 * holding packed {@link BlockPos} longs, written on
 * {@link BlockEvent.EntityPlaceEvent} and cleared on
 * {@link BlockEvent.BreakEvent}.
 *
 * <p>Bounded on purpose. Above {@link #MAX_ENTRIES} the ledger stops recording
 * and logs once; from that point the engine falls back to
 * {@link TerrainRules}'s block heuristic alone, which still protects anything
 * manufactured. A degraded ledger is a slightly blunter refusal, never a
 * bulldozed base.
 *
 * <p>Positions are also validated lazily: a recorded position whose block has
 * since become air is dropped on the next lookup, so pistons, explosions and
 * anything else that removes blocks without a BreakEvent do not leak entries
 * forever.
 */
public final class PlayerBuiltLedger extends SavedData {

    private static final Logger LOG = LoggerFactory.getLogger("BarbaraJones/BuildLedger");

    /** Storage name under {@code <world>/data/}. */
    public static final String NAME = "barbarajones_player_built";

    /** Roughly 2 MB of longs. Past this the heuristic carries the load alone. */
    public static final int MAX_ENTRIES = 250_000;

    private final LongOpenHashSet positions = new LongOpenHashSet();
    private boolean warnedFull;

    private PlayerBuiltLedger() { }

    private PlayerBuiltLedger(CompoundTag tag) {
        for (long packed : tag.getLongArray("Positions")) {
            positions.add(packed);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("Positions", positions.toLongArray());
        return tag;
    }

    public static PlayerBuiltLedger get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                PlayerBuiltLedger::new, PlayerBuiltLedger::new, NAME);
    }

    // ---- queries -----------------------------------------------------------

    /**
     * True if a player is known to have placed the block at this position.
     * Returns false on the client, where the ledger does not exist.
     */
    public static boolean isPlayerBuilt(LevelReader level, BlockPos pos) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        PlayerBuiltLedger ledger = get(server);
        long packed = pos.asLong();
        if (!ledger.positions.contains(packed)) {
            return false;
        }
        // Lazy prune: the block is gone, so the record is stale.
        if (server.getBlockState(pos).isAir()) {
            ledger.positions.remove(packed);
            ledger.setDirty();
            return false;
        }
        return true;
    }

    // ---- mutation ----------------------------------------------------------

    public void record(BlockPos pos) {
        if (positions.size() >= MAX_ENTRIES) {
            if (!warnedFull) {
                warnedFull = true;
                LOG.warn("Player-built ledger hit {} entries; no longer recording. Structure placement"
                        + " falls back to the block heuristic in TerrainRules.", MAX_ENTRIES);
            }
            return;
        }
        if (positions.add(pos.asLong())) {
            setDirty();
        }
    }

    public void forget(BlockPos pos) {
        if (positions.remove(pos.asLong())) {
            setDirty();
        }
    }

    public int size() {
        return positions.size();
    }

    // ---- hooks -------------------------------------------------------------

    /**
     * Its own Forge subscriber inside this module, so nothing has to be added
     * to the shared EventHandler.
     */
    @Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Hooks {

        private Hooks() { }

        @SubscribeEvent
        public static void onPlace(BlockEvent.EntityPlaceEvent event) {
            if (!(event.getEntity() instanceof Player)) {
                return;
            }
            LevelAccessor level = event.getLevel();
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            PlayerBuiltLedger ledger = get(server);
            if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
                // Doors, beds and tall flowers arrive as several snapshots at once.
                for (BlockSnapshot snapshot : multi.getReplacedBlockSnapshots()) {
                    ledger.record(snapshot.getPos());
                }
            } else {
                ledger.record(event.getPos());
            }
        }

        @SubscribeEvent
        public static void onBreak(BlockEvent.BreakEvent event) {
            if (event.getLevel() instanceof ServerLevel server) {
                get(server).forget(event.getPos());
            }
        }
    }
}
