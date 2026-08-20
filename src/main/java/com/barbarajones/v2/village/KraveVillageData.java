package com.barbarajones.v2.village;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The persistent home of every settlement in one dimension.
 *
 * <p>Stored as a {@code SavedData} on the {@link ServerLevel}, exactly the way
 * vanilla stores raids - file {@code data/barbarajones_villages.dat} inside the
 * dimension folder. That choice matters:
 *
 * <ul>
 *   <li>A settlement spans dozens of chunks, so chunk-attached capability data has
 *       the wrong lifetime - half the village would unload while the other half
 *       kept ticking.
 *   <li>A settlement outlives every player in it, so player-attached data is wrong
 *       too.
 *   <li>One object per dimension gives exactly one source of truth, so two loaded
 *       copies can never disagree about the tier.
 * </ul>
 *
 * <p>Villages are keyed by {@link UUID}, not by origin {@link BlockPos}. Position is
 * not identity: the origin can be moved later, and keying on it would silently
 * fork a village into two the first time someone relocated the charter.
 *
 * <p><b>setDirty.</b> {@code SavedData} only writes when it is marked dirty. Every
 * mutating method on this class calls {@link #setDirty()} itself, and callers that
 * reach a {@link Village} through {@link #get(UUID)} and change it <em>must</em>
 * call {@link #setDirty()} afterwards. This is the number one source of "it worked
 * until I reloaded" bugs in save-data code.
 */
public class KraveVillageData extends SavedData {

    /** The SavedData file name inside each dimension's data folder. */
    public static final String KEY = "barbarajones_villages";

    private final Map<UUID, Village> villages = new LinkedHashMap<>();

    /**
     * Fetches (or creates) the village table for this dimension.
     *
     * <p>Cheap - {@code computeIfAbsent} caches the instance on the level - so it
     * is fine to call this once per query rather than holding a reference.
     */
    public static KraveVillageData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(KraveVillageData::load, KraveVillageData::new, KEY);
    }

    /**
     * The village table for this dimension <em>if one has ever been written</em>,
     * otherwise null.
     *
     * <p>Use this on hot paths - the level tick, block place, block break. The
     * plain {@link #get} creates and caches an empty table for every dimension it is
     * asked about, which would put a {@code barbarajones_villages.dat} in the Nether
     * and the End of every world whether or not anyone ever founded anything.
     * {@code DimensionDataStorage} caches the miss, so calling this every tick is
     * one map lookup, not a disk read.
     */
    @Nullable
    public static KraveVillageData getExisting(ServerLevel level) {
        return level.getDataStorage().get(KraveVillageData::load, KEY);
    }

    public Collection<Village> villages() {
        return this.villages.values();
    }

    public boolean isEmpty() {
        return this.villages.isEmpty();
    }

    @Nullable
    public Village get(UUID id) {
        return id == null ? null : this.villages.get(id);
    }

    /**
     * The village whose claim box contains {@code pos}, or null.
     *
     * <p>If claims overlap - which nothing prevents, and which is a perfectly
     * reasonable thing for a player to build - the one whose origin is nearest
     * wins, so the answer is stable rather than depending on iteration order.
     */
    @Nullable
    public Village containing(BlockPos pos) {
        Village best = null;
        double bestDist = Double.MAX_VALUE;
        for (Village village : this.villages.values()) {
            if (!village.contains(pos)) {
                continue;
            }
            double dist = village.horizontalDistanceSqr(pos);
            if (dist < bestDist) {
                bestDist = dist;
                best = village;
            }
        }
        return best;
    }

    /** Nearest village by origin regardless of claim, or null if there are none. */
    @Nullable
    public Village nearest(BlockPos pos) {
        Village best = null;
        double bestDist = Double.MAX_VALUE;
        for (Village village : this.villages.values()) {
            double dist = village.horizontalDistanceSqr(pos);
            if (dist < bestDist) {
                bestDist = dist;
                best = village;
            }
        }
        return best;
    }

    /** The highest tier anywhere in this dimension. Drives the portal gate. */
    public VillageTier bestTier() {
        VillageTier best = VillageTier.WILDERNESS;
        for (Village village : this.villages.values()) {
            if (village.tier().index() > best.index()) {
                best = village.tier();
            }
        }
        return best;
    }

    /**
     * Creates a settlement. Returns the existing village instead if {@code origin}
     * already falls inside one - founding a second village on top of the first is
     * never what the player meant, and two overlapping claims fighting over the
     * same blocks is a bug factory.
     */
    public Village create(BlockPos origin, String name) {
        Village existing = containing(origin);
        if (existing != null) {
            return existing;
        }
        Village village = new Village(UUID.randomUUID(), origin, name);
        this.villages.put(village.id(), village);
        setDirty();
        return village;
    }

    public boolean remove(UUID id) {
        boolean removed = this.villages.remove(id) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    /**
     * Ticks every settlement. Called once per {@link Village#VILLAGE_TICK_INTERVAL}
     * from {@link VillageEvents}, not per game tick.
     */
    public void tickAll(ServerLevel level) {
        boolean changed = false;
        // Copied because attraction can spawn entities, and a mod listening on
        // EntityJoinLevelEvent is entitled to create a village from that handler.
        List<Village> snapshot = new ArrayList<>(this.villages.values());
        for (Village village : snapshot) {
            if (village.tick(level)) {
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    /** Removes a dead resident from whichever village claimed it. */
    public boolean forgetVillager(UUID entityId) {
        boolean changed = false;
        for (Village village : this.villages.values()) {
            if (village.unregisterVillager(entityId)) {
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
        return changed;
    }

    // ---- persistence --------------------------------------------------------

    public static KraveVillageData load(CompoundTag tag) {
        KraveVillageData data = new KraveVillageData();
        ListTag list = tag.getList("Villages", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                Village village = Village.load(list.getCompound(i));
                data.villages.put(village.id(), village);
            } catch (RuntimeException broken) {
                // One corrupt settlement must not cost the player every other one.
                com.mojang.logging.LogUtils.getLogger().error(
                        "[barbarajones] dropped an unreadable village entry", broken);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Village village : this.villages.values()) {
            list.add(village.save());
        }
        tag.put("Villages", list);
        return tag;
    }
}
