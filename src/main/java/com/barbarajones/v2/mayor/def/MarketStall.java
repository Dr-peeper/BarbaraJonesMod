package com.barbarajones.v2.mayor.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.resources.ResourceLocation;

import static com.barbarajones.v2.mayor.def.SlumPalette.*;

/**
 * Rung 1: the Market Stall. Three walls, a counter, and a tarpaulin on poles
 * over the front of it. The first thing Barbara builds that the player can
 * actually buy from.
 *
 * <p><b>Footprint</b> 7 x 7, of which the front two courses are open air under
 * the awning - so the stall is a 7 x 4 hut with a queue standing in front of it.
 * <b>Staffed</b> by one Grocer, spawned by the mayor when the build finishes and
 * enrolled in the settlement like any other resident, which means the stall is
 * also a trading post the moment it exists.
 *
 * <p><b>Village contribution</b>: three barrels, a chest, a composter and a
 * lantern, plus whatever the Grocer's own profession adds to production and
 * attraction once she has a trade level.
 */
final class MarketStall {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "mayor_market_stall");

    private MarketStall() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.CENTER)
                .maxGroundDelta(3)
                .buildTicks(45);

        final int LAST_X = 6;
        final int BACK_DEPTH = 3;   // the enclosed part runs z = 0..3
        final int FRONT = 6;        // the awning reaches z = 6
        final int EAVE = 2;
        final int ROOF = EAVE + 1;

        // The whole footprint is surfaced, including the standing area, so the
        // queue is not queueing on grass.
        b.fill(0, -1, 0, LAST_X, -1, FRONT, FLOOR);

        // Three walls. The fourth side is the counter.
        b.fill(0, 0, 0, LAST_X, EAVE, 0, WALL);
        b.fill(0, 0, 0, 0, EAVE, BACK_DEPTH, WALL);
        b.fill(LAST_X, 0, 0, LAST_X, EAVE, BACK_DEPTH, WALL);
        b.column(0, 0, 0, EAVE, POST);
        b.column(LAST_X, 0, 0, EAVE, POST);
        // Per wall run, because the fourth side is an opening: the rectangle
        // form of this would hang boards in mid-air across the shopfront.
        SlumRoof.windowRun(b, 0, 0, LAST_X, 0, 1);
        SlumRoof.windowRun(b, 0, 0, 0, BACK_DEPTH, 1);
        SlumRoof.windowRun(b, LAST_X, 0, LAST_X, BACK_DEPTH, 1);

        // The counter: a run of mismatched slabs with a gap the shopkeeper gets
        // in and out through.
        b.fill(0, 0, BACK_DEPTH, LAST_X, 0, BACK_DEPTH, SLAB);
        b.carve(3, 0, BACK_DEPTH, 3, 0, BACK_DEPTH);

        // Roof over the shop, then the awning on poles over the standing area,
        // one course lower so the rain runs off it onto the customers.
        SlumRoof.deck(b, 0, 0, LAST_X, BACK_DEPTH, ROOF);
        b.fill(0, ROOF - 1, BACK_DEPTH + 1, LAST_X, ROOF - 1, FRONT - 1, TARP);
        SlumRoof.awning(b, 0, LAST_X, FRONT, ROOF - 1, ROOF_S);
        b.column(0, FRONT - 1, 0, ROOF - 2, FENCE);
        b.column(LAST_X, FRONT - 1, 0, ROOF - 2, FENCE);

        // Stock.
        b.set(1, 0, 1, BARREL);
        b.set(2, 0, 1, BARREL);
        b.set(5, 0, 1, BARREL);
        b.set(4, 0, 1, CHEST);
        b.set(5, 0, 2, COMPOSTER);
        b.set(1, 0, 2, HAY);
        b.set(3, 0, 1, KRAVE_BLOCK);
        // On the crate, not floating: a lantern is not a hanging lantern and
        // fails its own canSurvive check with nothing under it.
        b.set(2, 1, 1, LANTERN);
        b.set(1, 0, BACK_DEPTH + 2, PIPE);

        b.marker("staff0", 3, 0, 2);

        b.core(3, -1, BACK_DEPTH);
        return b.build();
    }
}
