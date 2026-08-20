package com.barbarajones.v2.houses.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.houses.def.HousePalette.*;

/**
 * Rung 1 of 10: three walls, a dirt floor and a roof that leans on the tallest
 * one. This is what somebody builds the night they arrive with nothing -
 * krave-plank offcuts and whatever logs were already lying around.
 *
 * <p><b>Footprint</b> 3x3, single storey, no door - the front is just open.
 * <b>Roofline</b> a single 45-degree lean (see {@link RoofKit#monoSlope}), the
 * one roof shape every later building in the ladder is NOT. <b>Furnished</b>
 * with one bed, a barrel, and a lantern on the ground - the minimum that
 * still counts as "somewhere someone lives" rather than a windbreak.
 *
 * <p><b>Village contribution</b> (from {@code VillageBuffs}): the bed alone is
 * building 2 / attraction 6 / happiness 2. Nothing else here is in the buff
 * table (a plain barrel scores, torches/lanterns do not add building score).
 * Villager capacity: 1.
 */
final class LeanTo {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "house_lean_to");

    private LeanTo() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.FRONT)
                .buildTicks(20);

        // Back wall (z=0), full height, log corner posts.
        b.fill(0, 0, 0, 2, 2, 0, PLANKS);
        b.column(0, 0, 0, 2, LOG_POST);
        b.column(2, 0, 0, 2, LOG_POST);

        // Side walls follow the roof down: two courses at z=1, none at z=2.
        b.column(0, 1, 0, 1, PLANKS);
        b.column(2, 1, 0, 1, PLANKS);

        // The lean roof itself: high at the back (z=0, y=3), low over the open
        // front (z=2, y=1). Overhangs the walls by nothing - this is a shack,
        // not an eave detail.
        RoofKit.monoSlope(b, 0, 2, 0, 3, 2, 1, ROOF_WOOD_S);

        // Furnishing: a bed against the back wall, a barrel for what little
        // there is to store, a lantern to see it by.
        b.bed(1, 0, 1, () -> Blocks.BROWN_BED, Direction.NORTH);
        b.set(0, 0, 2, BARREL);
        b.set(1, 0, 2, LANTERN);

        b.core(1, -1, 1);
        return b.build();
    }
}
