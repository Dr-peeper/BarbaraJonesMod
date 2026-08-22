package com.barbarajones.v2.mayor.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.mayor.def.SlumPalette.*;

/**
 * Rung 3: the Workshop. Every workstation in the game against the walls of one
 * shed, plus the mod's own Krafting Bench in the middle of the floor where it is
 * in everybody's way.
 *
 * <p>This is the building that makes the village <em>useful</em> rather than
 * merely populated. Nothing else Barbara puts up gives the player somewhere to
 * smelt, cut, smith, weave and craft without carrying it all out there
 * themselves - and the Krafting Bench is what the whole Krave recipe tree needs.
 *
 * <p><b>Footprint</b> 9 x 10 - the shed is 9 x 8 and there is a course of
 * salvage awning off each end, one sloping south over the door and one sloping
 * north over the timber stack. <b>Roofline</b> deliberately uneven: the forge
 * chimney is off-centre, sat over the two furnaces rather than over the middle
 * of the building, because it was put where the smoke was and not where it
 * would look right.
 *
 * <p><b>Village contribution</b>: eight separate workstations at 1 building /
 * 1 production / 2 attraction each, the Krafting Bench at 2 / 4 / 3, and two
 * Builders whose profession raises the settlement's effective building count
 * just by living here.
 */
final class Workshop {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "mayor_workshop");

    private Workshop() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.CENTER)
                .maxGroundDelta(3)
                .buildTicks(80);

        final int LAST_X = 8;
        final int FRONT = 7;
        final int EAVE = 3;
        final int ROOF = EAVE + 1;

        b.fill(0, -1, 0, LAST_X, -1, FRONT, FLOOR);
        b.walls(0, 0, 0, LAST_X, EAVE, FRONT, WALL);
        b.column(0, 0, 0, EAVE, POST);
        b.column(LAST_X, 0, 0, EAVE, POST);
        b.column(0, FRONT, 0, EAVE, POST);
        b.column(LAST_X, FRONT, 0, EAVE, POST);
        SlumRoof.windows(b, 0, 0, LAST_X, FRONT, 1);
        SlumRoof.windows(b, 0, 0, LAST_X, FRONT, 2);

        // The ladder hangs off the left wall rather than the back one, because
        // the back wall is where the furnaces went. Restored here after the
        // scatter, which is allowed to knock holes in it.
        b.column(0, 6, 0, EAVE, WALL);
        b.door(4, 0, FRONT, () -> Blocks.DARK_OAK_DOOR);
        // A second, wider opening: the doors you get a cart through. No door in
        // it, because there has not been a door in it for years.
        b.carve(6, 0, FRONT, 7, 1, FRONT);

        // ---- the benches, round the walls ----------------------------------
        b.set(1, 0, 1, FURNACE);
        b.set(2, 0, 1, FURNACE);
        b.set(3, 0, 1, SMOKER);
        b.set(5, 0, 1, SMITHING);
        b.set(6, 0, 1, STONECUTTER);
        b.set(7, 0, 1, CRAFTING);
        b.set(1, 0, 3, LOOM);
        b.set(1, 0, 4, CARTOGRAPHY);
        b.set(7, 0, 3, BARREL);
        b.set(7, 0, 4, CHEST);
        b.set(7, 0, 5, BARREL);
        b.set(1, 0, 5, COMPOSTER);

        // The bench everything in this mod is actually made on, dumped in the
        // middle of the floor where there is no room for it.
        b.set(4, 0, 3, KRAFTING_BENCH);
        b.set(4, 0, 4, HAY);
        b.set(3, -1, 5, CARPET);
        b.set(2, 0, 2, TORCH);
        b.set(6, 0, 5, LANTERN);
        b.set(5, 0, 5, PIPE);

        // ---- roofline ------------------------------------------------------
        SlumRoof.deck(b, 0, 0, LAST_X, FRONT, ROOF);
        SlumRoof.parapet(b, 0, 0, LAST_X, FRONT, ROOF + 1);
        SlumRoof.awning(b, 3, 5, FRONT + 1, ROOF - 1, ROOF_S);
        // And one over the back, where the timber is stacked. It slopes the
        // other way, which is the point: nothing on this building matches.
        b.fill(0, ROOF - 1, -1, LAST_X, ROOF - 1, -1, ROOF_N);

        // The forge chimney: a proper stack, off to one side, sat over the two
        // furnaces rather than over the middle of the building. The campfire on
        // top is the smoke - it is eight blocks up a masonry flue, so it is not
        // in anybody's way.
        b.column(2, 1, ROOF, ROOF + 3, RUBBLE);
        b.set(2, ROOF + 4, 1, CAMPFIRE);

        // FACING east means the wall it hangs on is the course to the west,
        // which here is the left wall at x = 0.
        b.column(1, 6, 0, EAVE, LADDER_E);
        b.carve(1, ROOF, 6, 1, ROOF, 6);

        b.marker("staff0", 4, 0, 2);
        b.marker("staff1", 3, 0, 4);

        b.core(4, -1, FRONT);
        return b.build();
    }
}
