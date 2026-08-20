package com.barbarajones.v2.machines.village;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import net.minecraftforge.common.MinecraftForge;

/**
 * The single point where the machines module talks to the village.
 *
 * <p>Everything the Krave Depot knows about the village goes through here, which
 * means connecting the two modules is one call in one place and disconnecting
 * them is deleting it. No other class in this module imports anything from the
 * village.
 *
 * <p>Two channels, tried in order:
 * <ol>
 *   <li>A registered {@link VillageProductionSink}, if the village module (or the
 *       orchestrator) set one.</li>
 *   <li>{@link KraveDeliveredEvent} on the Forge bus, which any
 *       {@code @SubscribeEvent} method can claim with no wiring at all.</li>
 * </ol>
 *
 * <p>If neither claims a shipment, {@link #deliver} returns false and the Depot
 * stalls with "no village linked" rather than deleting the player's product. A
 * silent item sink is the single worst failure mode an automation mod can have -
 * the player loses hours of production and nothing tells them why.
 */
public final class VillageLink {

    @Nullable
    private static volatile VillageProductionSink sink;

    private VillageLink() { }

    /**
     * Connects the village module. Call once, during common setup.
     *
     * <p>Passing null disconnects, which exists for tests and for the case where
     * the village module is compiled out.
     */
    public static void setSink(@Nullable VillageProductionSink newSink) {
        sink = newSink;
    }

    @Nullable
    public static VillageProductionSink sink() {
        return sink;
    }

    /** Whether a village is reachable. Depots poll this while idle to show status. */
    public static boolean isVillageInRange(ServerLevel level, BlockPos pos) {
        VillageProductionSink current = sink;
        return current == null || current.isVillageInRange(level, pos);
    }

    /**
     * Ships finished Krave to the village.
     *
     * @return true if something actually took the shipment and raised production.
     *         False means the caller must NOT consume the items.
     */
    public static boolean deliver(ServerLevel level, BlockPos depotPos, int cases) {
        if (cases <= 0) {
            return false;
        }
        VillageProductionSink current = sink;
        if (current != null && current.acceptShipment(level, depotPos, cases)) {
            return true;
        }
        KraveDeliveredEvent event = new KraveDeliveredEvent(level, depotPos, cases);
        MinecraftForge.EVENT_BUS.post(event);
        return event.isHandled();
    }
}
