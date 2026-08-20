package com.barbarajones.v2.village;

import net.minecraft.core.BlockPos;

import java.util.UUID;

/**
 * An immutable snapshot of one settlement, handed to callers outside this module.
 *
 * <p>{@link KraveVillage} never returns the live {@link Village} object. Four other
 * modules read this state and none of them should be able to write it by accident,
 * and a live object read from another thread (a render thread drawing the HUD, say)
 * is a data race waiting to happen. Everything here is a value; the snapshot is
 * cheap and is taken fresh on every query, so hold onto it for the length of a
 * method and no longer.
 *
 * @param id          stable identity of the village; survives moving the origin
 * @param name        display name, player-set or generated
 * @param origin      the block the charter was planted on
 * @param radius      claim radius in blocks, horizontally, from {@code origin}
 * @param tier        derived development tier
 * @param population  living Krave Villagers registered to this village
 * @param buildings   summed {@link VillageBuff#building()} of every tracked block
 * @param defence     summed defence rating, blocks plus Guards
 * @param happiness   0..100
 * @param production  Krave produced per real-world hour at the current rate
 * @param stockpile   Krave sitting in the village store right now
 * @param memberCount how many players have joined this village
 */
public record VillageView(
        UUID id,
        String name,
        BlockPos origin,
        int radius,
        VillageTier tier,
        int population,
        int buildings,
        int defence,
        int happiness,
        int production,
        int stockpile,
        int memberCount) {

    /** Convenience for callers that only care about the number. */
    public int tierIndex() {
        return this.tier.index();
    }

    /** True once this settlement is developed enough to open the Krave portal. */
    public boolean unlocksPortal() {
        return this.tier.index() >= KraveVillage.PORTAL_TIER;
    }

    /**
     * The fraction of incoming raid damage that still lands, 0.4 (a fortress) to
     * 1.0 (an unwalled camp). See {@link KraveVillage#raidDamageMultiplier}.
     */
    public float raidDamageMultiplier() {
        float reduction = Math.min(KraveVillage.MAX_DAMAGE_REDUCTION,
                this.defence * KraveVillage.DEFENCE_TO_REDUCTION);
        return 1.0F - reduction;
    }

    /** Whether {@code pos} falls inside the claim box. */
    public boolean contains(BlockPos pos) {
        return Math.abs(pos.getX() - this.origin.getX()) <= this.radius
                && Math.abs(pos.getZ() - this.origin.getZ()) <= this.radius
                && Math.abs(pos.getY() - this.origin.getY()) <= Village.CLAIM_HEIGHT;
    }
}
