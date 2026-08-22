package com.barbarajones.v2.mayor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The persistent mayor's office for one dimension.
 *
 * <p>A {@code SavedData} on the {@link ServerLevel}, file
 * {@code data/barbarajones_mayor.dat} inside the dimension folder, sitting
 * beside the village module's own {@code barbarajones_villages.dat} and keyed by
 * the same village {@link UUID}s.
 *
 * <h2>Why a second file rather than fields on Village</h2>
 * Two reasons, and the first one is the important one:
 *
 * <ul>
 *   <li><b>Ownership.</b> {@code Village} is another module's file. Adding
 *       mayoral state to it would put two modules' code in one class and one
 *       save format, and the next change to either would be a merge conflict in
 *       somebody else's work. Keying a separate SavedData on the village id gets
 *       the same association with none of that.
 *   <li><b>Lifetime.</b> A settlement can exist perfectly well with nobody
 *       running it. Most villages in most worlds will never have a mayor, and
 *       they should not be paying for an empty project queue in their save
 *       entry.
 * </ul>
 *
 * <p>Entries whose village has since been dissolved are pruned by the pipeline
 * on a mayor tick, not on load: {@code SavedData} is constructed from a tag with
 * no level to hand, so this class has no way to ask the village table anything
 * at that point. {@link #villageIds} and {@link #forget} are the two halves of
 * that cross-check.
 *
 * <p><b>None of this is static state.</b> Everything a mayor knows lives here
 * and is written with the world. Barbara herself stores nothing authoritative -
 * she asks this object every tick whether she still has the job.
 */
public final class MayorData extends SavedData {

    /** File name under {@code <dimension>/data/}. */
    public static final String KEY = "barbarajones_mayor";

    private final Map<UUID, MayorSettlement> settlements = new LinkedHashMap<>();

    /**
     * The mayor's office for this dimension, creating an empty one if this is
     * the first time anybody has asked.
     */
    public static MayorData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(MayorData::load, MayorData::new, KEY);
    }

    /**
     * The office <em>if it has ever been written</em>, otherwise null.
     *
     * <p>Use this on any path that runs whether or not a mayor exists. The plain
     * {@link #get} creates and caches an empty file for every dimension it is
     * asked about, which would drop a {@code barbarajones_mayor.dat} into the
     * Nether and the End of every world in which nobody ever met Barbara.
     */
    @Nullable
    public static MayorData getExisting(ServerLevel level) {
        return level.getDataStorage().get(MayorData::load, KEY);
    }

    /** The office's record for a village, creating it on first use. */
    public MayorSettlement settlementFor(UUID villageId) {
        MayorSettlement existing = this.settlements.get(villageId);
        if (existing != null) {
            return existing;
        }
        MayorSettlement created = new MayorSettlement(villageId);
        this.settlements.put(villageId, created);
        setDirty();
        return created;
    }

    /**
     * Every village this office has a record for, as a copy.
     *
     * <p>A copy because the caller's whole reason for asking is to cross-check
     * the ids against the village table and {@link #forget} the ones that have
     * gone, which would otherwise be a modification during iteration.
     */
    public Set<UUID> villageIds() {
        return new HashSet<>(this.settlements.keySet());
    }

    /** Drops the record for a village that no longer exists. */
    public boolean forget(UUID villageId) {
        boolean removed = this.settlements.remove(villageId) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    // ---- persistence --------------------------------------------------------

    public static MayorData load(CompoundTag tag) {
        MayorData data = new MayorData();
        ListTag list = tag.getList("Settlements", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            try {
                MayorSettlement settlement = MayorSettlement.load(list.getCompound(i));
                if (settlement != null) {
                    data.settlements.put(settlement.villageId(), settlement);
                }
            } catch (RuntimeException broken) {
                // One unreadable settlement must not cost the player the rest of
                // the dimension's mayoral history.
                com.mojang.logging.LogUtils.getLogger().error(
                        "[barbarajones] dropped an unreadable mayor entry", broken);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (MayorSettlement settlement : this.settlements.values()) {
            list.add(settlement.save());
        }
        tag.put("Settlements", list);
        return tag;
    }
}
