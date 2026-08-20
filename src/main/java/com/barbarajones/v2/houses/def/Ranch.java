package com.barbarajones.v2.houses.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModBlocks;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.houses.def.HousePalette.*;

/**
 * Rung 5 of 10: the ladder's first building that grows OUT instead of UP -
 * one long single-storey hall with a low gable roof, plus an open equipment
 * shed tacked onto one end under its own lean-to. Where the two-storey house
 * proved height, this proves footprint: eleven blocks of frontage is a
 * building nobody could mistake for a cottage, even at one storey.
 *
 * <p><b>Footprint</b> 11x5 main hall plus a 3x3 open shed wing, single
 * storey. <b>Roofline</b> a long low gable over the hall, a separate lean-to
 * over the shed meeting it at a different height - two roof shapes on one
 * building, on purpose. <b>Furnished</b> for four: four beds down the back
 * wall, two chests, a kitchen, and a loom out in the shed for the wool.
 *
 * <p><b>Village contribution</b>: 4 beds (8), door (1), crafting table (1),
 * furnace (1), loom (1), 2 chests (2) = 14 building points. Villager
 * capacity: 4.
 */
final class Ranch {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "house_ranch");

    private Ranch() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.FRONT)
                .buildTicks(150);

        final int EAVE = 2;
        final int ROOF_Y = EAVE + 1;

        // ---- the hall: long, low, wide -----------------------------------
        b.walls(0, 0, 0, 10, EAVE, 4, PLANKS);
        b.column(0, 0, 0, EAVE, LOG_POST);
        b.column(10, 0, 0, EAVE, LOG_POST);
        b.column(0, 4, 0, EAVE, LOG_POST);
        b.column(10, 4, 0, EAVE, LOG_POST);
        b.fill(1, 0, 1, 9, 0, 3, PLANKS);

        for (int x = 1; x <= 9; x += 2) {
            b.set(x, 1, 0, GLASS_PANE);
            if (x != 5) {
                b.set(x, 1, 4, GLASS_PANE);
            }
        }
        b.door(5, 0, 4, ModBlocks.KRAVE_DOOR);

        RoofKit.gableAlongX(b, 0, 10, 0, 4, ROOF_Y, ROOF_WOOD_N, ROOF_WOOD_S, ROOF_WOOD_RIDGE);
        RoofKit.gableEndWallX(b, 0, 4, 0, ROOF_Y, PLANKS);
        RoofKit.gableEndWallX(b, 0, 4, 10, ROOF_Y, PLANKS);

        // ---- the shed: an open equipment wing off the east end -----------
        b.fill(8, 0, 5, 10, 0, 7, PLANKS);
        b.column(8, 5, 0, EAVE, LOG_POST);
        b.column(10, 5, 0, EAVE, LOG_POST);
        b.column(8, 7, 0, EAVE - 1, LOG_POST);
        b.column(10, 7, 0, EAVE - 1, LOG_POST);
        b.set(9, 0, 6, HAY_BALE);
        b.set(9, 1, 6, HAY_BALE);
        RoofKit.monoSlope(b, 8, 10, 5, ROOF_Y, 7, ROOF_Y - 2, ROOF_WOOD_S);

        // ---- furnishing ----------------------------------------------------
        b.bed(1, 0, 1, () -> Blocks.LIME_BED, Direction.NORTH);
        b.bed(3, 0, 1, () -> Blocks.LIME_BED, Direction.NORTH);
        b.bed(6, 0, 1, () -> Blocks.LIME_BED, Direction.NORTH);
        b.bed(8, 0, 1, () -> Blocks.LIME_BED, Direction.NORTH);
        b.set(1, 0, 3, CHEST);
        b.set(8, 0, 3, CHEST);
        b.set(4, 0, 3, CRAFTING_TABLE);
        b.set(5, 0, 3, FURNACE);
        b.set(3, 0, 2, LANTERN);
        b.set(7, 0, 2, LANTERN);
        b.set(8, 0, 5, LOOM);
        b.set(10, 0, 6, BARREL);

        b.core(5, -1, 2);
        return b.build();
    }
}
