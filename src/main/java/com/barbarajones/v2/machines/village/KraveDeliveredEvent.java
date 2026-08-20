package com.barbarajones.v2.machines.village;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import net.minecraftforge.eventbus.api.Event;

/**
 * Fired on the Forge event bus whenever a Krave Depot ships finished product.
 *
 * <p>This exists alongside {@link VillageProductionSink} so the village module has
 * a zero-wiring option: a plain {@code @SubscribeEvent} method needs nothing
 * registered in {@code BarbaraJonesMod}, no static setter call, and no load-order
 * agreement between two modules that are being written in parallel. The sink
 * interface is the explicit contract; this event is the one that works even if
 * nobody remembers to connect it.
 *
 * <p>A handler that actually raises village production should call
 * {@link #setHandled()}. If neither a sink nor an event handler claims a
 * shipment, the Depot stops shipping and reports it in its GUI instead of
 * quietly destroying the player's product.
 */
public class KraveDeliveredEvent extends Event {

    private final ServerLevel level;
    private final BlockPos depotPos;
    private final int cases;
    private boolean handled;

    public KraveDeliveredEvent(ServerLevel level, BlockPos depotPos, int cases) {
        this.level = level;
        this.depotPos = depotPos;
        this.cases = cases;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos getDepotPos() {
        return depotPos;
    }

    /** Number of Cases of Krave in this shipment. Always at least one. */
    public int getCases() {
        return cases;
    }

    public boolean isHandled() {
        return handled;
    }

    /** Call this if you raised village production because of this shipment. */
    public void setHandled() {
        this.handled = true;
    }
}
