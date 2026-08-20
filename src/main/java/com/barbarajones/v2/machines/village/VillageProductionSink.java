package com.barbarajones.v2.machines.village;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * The narrow interface the machines module needs from the village module.
 *
 * <p>At the time this module was written {@code com.barbarajones.v2.village} did
 * not exist and {@code docs/modules/village.md} had not been published, so rather
 * than guess at its real API this declares the smallest surface the Krave Depot
 * actually needs and leaves the connection to the orchestrator. Two methods, no
 * types from either module leaking into the other, nothing the village has to
 * expose that it would not have anyway.
 *
 * <p>The village module (or the orchestrator on its behalf) implements this and
 * calls {@link VillageLink#setSink}. Until then the Depot runs against a no-op
 * sink: it still accepts cases and still counts them, it just has nowhere to
 * send them. See {@code docs/modules/machines.md}.
 */
public interface VillageProductionSink {

    /**
     * A Krave Depot has just shipped finished product to the village.
     *
     * @param level level the depot is in - always a server level
     * @param depotPos position of the depot that shipped
     * @param cases how many Cases of Krave were shipped, always at least one
     * @return true if the village accepted the shipment and raised its production
     *         rate. Returning false makes the Depot stall rather than keep
     *         voiding product into a village that cannot use it.
     */
    boolean acceptShipment(ServerLevel level, BlockPos depotPos, int cases);

    /**
     * Whether a village is currently reachable from this depot.
     *
     * <p>Polled roughly once a second by an idle Depot so it can show the player
     * "no village linked" rather than silently doing nothing. Implementations
     * should keep this cheap - a cached nearest-village lookup, not a scan.
     */
    default boolean isVillageInRange(ServerLevel level, BlockPos depotPos) {
        return true;
    }
}
