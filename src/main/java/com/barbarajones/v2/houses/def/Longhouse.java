package com.barbarajones.v2.houses.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModBlocks;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;

import static com.barbarajones.v2.houses.def.HousePalette.*;

/**
 * Rung 6 of 10: not bigger so much as HIGHER-CEILINGED and communal - a single
 * long timbered hall, a steep A-frame roof with its ridge beam left exposed
 * rather than capped flush, and six sleeping alcoves down one wall instead of
 * beds paired off into separate rooms. Where the ranch sprawls, this one
 * commits to a single shared room, which is the point: it is the first
 * building built for a household rather than a family.
 *
 * <p><b>Footprint</b> 13x5, single storey with taller-than-usual 4-block
 * walls under a steep gable. <b>Roofline</b> the steepest yet for its depth,
 * capped with a literal log beam instead of a flat plank ridge. <b>Furnished</b>
 * with six beds, a central hearth (campfire), a smithing table alongside the
 * usual crafting table and furnace, two chests and two barrels.
 *
 * <p><b>Village contribution</b>: 6 beds (12), door (1, doubled but a fence
 * gate/door pair only ever counts once per placed block so this is really
 * TWO door blocks = 2), crafting table (1), furnace (1), smithing table (0 -
 * not in the base buff table, it is here for the smithing itself not the
 * score), 2 chests (2) = 18 building points. Villager capacity: 6.
 */
final class Longhouse {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "house_longhouse");

    private Longhouse() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.FRONT)
                .buildTicks(170);

        b.block('H', Blocks.CAMPFIRE);
        char ridgeBeam = b.key(() -> ModBlocks.KRAVE_WOOD.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));

        final int EAVE = 3;
        final int ROOF_Y = EAVE + 1;

        // ---- the hall --------------------------------------------------------
        b.walls(0, 0, 0, 12, EAVE, 4, PLANKS);
        for (int x = 0; x <= 12; x += 4) {
            b.column(x, 0, 0, EAVE, LOG_POST);
            b.column(x, 4, 0, EAVE, LOG_POST);
        }
        b.fill(1, 0, 1, 11, 0, 3, PLANKS);

        for (int x = 2; x <= 10; x += 2) {
            b.set(x, 1, 0, GLASS_PANE);
            if (x != 6) {
                b.set(x, 1, 4, GLASS_PANE);
            }
        }
        b.door(6, 0, 4, ModBlocks.KRAVE_DOOR, Direction.SOUTH, false);
        b.door(7, 0, 4, ModBlocks.KRAVE_DOOR, Direction.SOUTH, true);

        // ---- steep roof, exposed ridge beam ----------------------------------
        RoofKit.gableAlongX(b, 0, 12, 0, 4, ROOF_Y, ROOF_WOOD_N, ROOF_WOOD_S, ROOF_WOOD_RIDGE);
        RoofKit.gableEndWallX(b, 0, 4, 0, ROOF_Y, PLANKS);
        RoofKit.gableEndWallX(b, 0, 4, 12, ROOF_Y, PLANKS);
        b.set(0, ROOF_Y + 1, 2, GLASS_PANE);
        b.set(12, ROOF_Y + 1, 2, GLASS_PANE);
        b.fill(0, ROOF_Y + 2, 2, 12, ROOF_Y + 2, 2, ridgeBeam);

        // ---- furnishing: six alcoves, a hearth, a working end ----------------
        for (int x = 1; x <= 11; x += 2) {
            b.bed(x, 0, 1, () -> Blocks.RED_BED, Direction.NORTH);
        }
        b.set(6, 0, 2, 'H');
        b.set(2, 0, 3, CHEST);
        b.set(10, 0, 3, CHEST);
        b.set(2, 0, 2, BARREL);
        b.set(10, 0, 2, BARREL);
        b.set(4, 0, 3, CRAFTING_TABLE);
        b.set(5, 0, 3, FURNACE);
        b.set(8, 0, 3, SMITHING_TABLE);
        b.set(3, 1, 0, LANTERN);
        b.set(9, 1, 0, LANTERN);

        b.core(6, -1, 2);
        return b.build();
    }
}
