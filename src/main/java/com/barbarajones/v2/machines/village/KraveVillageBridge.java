package com.barbarajones.v2.machines.village;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import com.barbarajones.v2.village.KraveVillageData;
import com.barbarajones.v2.village.Village;

/**
 * The adapter from this module's {@link VillageProductionSink} to the real
 * {@code com.barbarajones.v2.village} API.
 *
 * <p><b>This is the only file in the machines module that imports anything from
 * the village module.</b> Everything else goes through {@link VillageLink}. If
 * the village API moves, exactly one file here breaks, and deleting this file
 * leaves the machines fully functional - the Depot just reports "No village
 * linked" instead of shipping.
 *
 * <p>How a shipment raises production: {@code Village.production} is recomputed
 * every village tick as
 * {@code (buildingProduction + professionProduction) * tierMultiplier * mood},
 * where {@code mood = 0.5 + happiness/100}. Happiness is the one lever an
 * outside system can pull, and it drifts back toward its building-derived target
 * one point per village tick - so a shipment gives a real, decaying boost to the
 * production <em>rate</em>, and a town that wants to stay at peak output has to
 * keep the line running. The Krave itself goes straight into the stockpile,
 * which is the immediate payoff.
 */
public class KraveVillageBridge implements VillageProductionSink {

    /** A Case of Krave is eight boxes of cereal, so it is worth eight to the stockpile. */
    public static final int KRAVE_PER_CASE = 8;
    /** Happiness added per case. Decays one point per village tick, so it must be re-earned. */
    public static final int HAPPINESS_PER_CASE = 2;

    @Override
    public boolean acceptShipment(ServerLevel level, BlockPos depotPos, int cases) {
        // getExisting, not get: this runs off a block-entity tick, and the plain
        // getter would create and persist an empty village table in every
        // dimension anyone ever put a Depot in.
        KraveVillageData data = KraveVillageData.getExisting(level);
        if (data == null) {
            return false;
        }
        Village village = data.containing(depotPos);
        if (village == null) {
            return false;
        }
        village.addKrave(cases * KRAVE_PER_CASE);
        village.adjustHappiness(cases * HAPPINESS_PER_CASE);
        // Mutating a Village reached through the table does not mark the SavedData
        // dirty by itself. Forgetting this is the "it worked until I reloaded"
        // bug the village module's own javadoc warns about.
        data.setDirty();
        return true;
    }

    @Override
    public boolean isVillageInRange(ServerLevel level, BlockPos depotPos) {
        KraveVillageData data = KraveVillageData.getExisting(level);
        return data != null && data.containing(depotPos) != null;
    }
}
