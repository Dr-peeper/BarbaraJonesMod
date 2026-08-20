package com.barbarajones.v2.bonds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

/**
 * Every bit of extra state this module hangs off an entity, stored in Forge's
 * per-entity {@link Entity#getPersistentData()} bag rather than by touching
 * {@code CaydenCobb}/{@code BarbaraJones} - that NBT is saved and loaded
 * automatically by Forge, so this survives chunk unload, server restart and
 * (for Cayden) his apocalypse respawn cycle exactly like the entity's own
 * fields do, with zero changes to either entity class.
 *
 * <p>All keys live under one nested "BJBonds" compound so nothing here can
 * collide with a tag name the base entity (or another mod) happens to use.
 */
final class BondState {

    private static final String ROOT = "BJBonds";

    /**
     * The cadence {@code FeedingBondEvents.onTick} actually calls the tick-driven
     * methods in this file at (once every this many real ticks, not every tick -
     * see its {@code TICK_STRIDE}). Every cooldown stored here is decremented by
     * this much per call rather than by 1, so a constant named e.g.
     * {@code RETRY_TICKS = 20 * 30} really does mean 30 seconds instead of
     * silently meaning 20x that.
     */
    static final int TICK_STRIDE = 20;

    private BondState() { }

    private static CompoundTag root(Entity e) {
        CompoundTag data = e.getPersistentData();
        CompoundTag root = data.getCompound(ROOT);
        if (!data.contains(ROOT)) {
            data.put(ROOT, root);
        }
        return root;
    }

    // ---- Barbara's lifetime "times fed grass" shadow counter ----------------

    static int barbaraLifetimeGifts(Entity barbara) {
        return root(barbara).getInt("Gifts");
    }

    static void addBarbaraGift(Entity barbara) {
        CompoundTag r = root(barbara);
        r.putInt("Gifts", r.getInt("Gifts") + 1);
    }

    // ---- Krave Family Box cooldown, per parent Cayden ------------------------
    // Decremented from FeedingBondEvents.onTick - see tryDecrementFamilyBoxCooldown.

    static int familyBoxCooldown(Entity cayden) {
        return root(cayden).getInt("FamilyBoxCooldown");
    }

    static void setFamilyBoxCooldown(Entity cayden, int ticks) {
        root(cayden).putInt("FamilyBoxCooldown", ticks);
    }

    /** Call once per {@link #TICK_STRIDE}; keeps the cooldown honest without a second stored timestamp. */
    static void tryDecrementFamilyBoxCooldown(Entity cayden) {
        int t = familyBoxCooldown(cayden);
        if (t > 0) {
            setFamilyBoxCooldown(cayden, Math.max(0, t - TICK_STRIDE));
        }
    }

    // ---- assigned village house (Cayden and Barbara both use this) ----------

    static void setAssignedHouse(Entity resident, BlockPos pos, String dimensionId) {
        CompoundTag r = root(resident);
        CompoundTag house = new CompoundTag();
        house.putInt("X", pos.getX());
        house.putInt("Y", pos.getY());
        house.putInt("Z", pos.getZ());
        house.putString("Dim", dimensionId);
        r.put("House", house);
    }

    @Nullable
    static BlockPos assignedHousePos(Entity resident) {
        CompoundTag r = root(resident);
        if (!r.contains("House")) {
            return null;
        }
        CompoundTag house = r.getCompound("House");
        return new BlockPos(house.getInt("X"), house.getInt("Y"), house.getInt("Z"));
    }

    @Nullable
    static String assignedHouseDim(Entity resident) {
        CompoundTag r = root(resident);
        if (!r.contains("House")) {
            return null;
        }
        return r.getCompound("House").getString("Dim");
    }

    static boolean hasAssignedHouse(Entity resident) {
        return root(resident).contains("House");
    }

    /** Throttles how often a resident without a house re-scans for one. */
    static int houseSearchCooldown(Entity resident) {
        return root(resident).getInt("HouseSearchCooldown");
    }

    static void setHouseSearchCooldown(Entity resident, int ticks) {
        root(resident).putInt("HouseSearchCooldown", ticks);
    }

    /** Throttles how often a housed resident nudges their village's happiness. */
    static int happinessCooldown(Entity resident) {
        return root(resident).getInt("HappinessCooldown");
    }

    static void setHappinessCooldown(Entity resident, int ticks) {
        root(resident).putInt("HappinessCooldown", ticks);
    }

    // ---- last bond level announced, so the nameplate/toast only fires once --

    static int lastAnnouncedBondLevel(Entity e) {
        CompoundTag r = root(e);
        return r.contains("LastBondLevel") ? r.getInt("LastBondLevel") : -1;
    }

    static void setLastAnnouncedBondLevel(Entity e, int level) {
        root(e).putInt("LastBondLevel", level);
    }
}
