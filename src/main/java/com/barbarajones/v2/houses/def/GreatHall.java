package com.barbarajones.v2.houses.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModBlocks;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.houses.def.HousePalette.*;

/**
 * Rung 9 of 10: not a home so much as the reason the village has a centre -
 * one huge stone-based hall lined with every workstation the settlement
 * needs in one room, a bell by the door, and a small guest wing tucked under
 * the eave at the back for whoever is tending it. This is the ladder's first
 * building whose villager capacity DROPS from the rung before it, on
 * purpose: a great hall is infrastructure, and the buff table already scores
 * its production and attraction well above anything residential - it does
 * not also need to sleep the whole village.
 *
 * <p><b>Footprint</b> 15x7 hall plus a 5x4 guest annex, single storey but
 * tall - 5-block walls under the steepest, widest gable in the ladder.
 * <b>Roofline</b> a grand gable with glazed rose windows at both ends; the
 * annex roof tucks under the hall's own eave rather than fighting it.
 * <b>Furnished</b> with six workstations in a row, four chests, a bell, and
 * four guest beds out in the annex.
 *
 * <p><b>Village contribution</b>: door (1), bell (1, defence 2/attraction 4/
 * happiness 3), six workstations at 1 building each (6), 4 chests (4), 4 beds
 * in the annex (8) = 20 building points, the highest yet before the mansion.
 * Villager capacity: 4 (the annex only - the hall itself sleeps no one).
 */
final class GreatHall {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "house_great_hall");

    private GreatHall() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.FRONT)
                .maxGroundDelta(5)
                .buildTicks(300);

        final int EAVE = 4;
        final int ROOF_Y = EAVE + 1;

        // ---- the hall: stone base, tall plank walls, glass everywhere -------
        b.walls(0, 0, 0, 14, 1, 6, STONE_BRICKS);
        b.walls(0, 2, 0, 14, EAVE, 6, PLANKS);
        b.fill(1, 0, 1, 13, 0, 5, PLANKS);
        for (int x = 0; x <= 14; x += 2) {
            b.column(x, 6, 0, EAVE, LOG_POST);
        }
        for (int x = 1; x <= 13; x += 3) {
            b.set(x, 2, 6, GLASS_BLOCK);
            b.set(x, 3, 6, GLASS_BLOCK);
        }
        b.door(6, 0, 6, ModBlocks.KRAVE_DOOR, Direction.SOUTH, false);
        b.door(7, 0, 6, ModBlocks.KRAVE_DOOR, Direction.SOUTH, true);
        b.set(7, 2, 6, BELL);

        // ---- roof: the widest, steepest gable in the ladder, ridged along X ---
        RoofKit.gableAlongX(b, 0, 14, 0, 6, ROOF_Y, ROOF_WOOD_N, ROOF_WOOD_S, ROOF_WOOD_RIDGE);
        RoofKit.gableEndWallX(b, 0, 6, 0, ROOF_Y, PLANKS);
        RoofKit.gableEndWallX(b, 0, 6, 14, ROOF_Y, PLANKS);
        // A glazed rose window at the peak of each gable end.
        b.set(0, ROOF_Y + 2, 3, GLASS_BLOCK);
        b.set(14, ROOF_Y + 2, 3, GLASS_BLOCK);
        b.set(0, ROOF_Y + 1, 3, GLASS_PANE);
        b.set(14, ROOF_Y + 1, 3, GLASS_PANE);

        // ---- guest annex, tucked under the hall's rear eave -------------------
        final int AEAVE = 2;
        final int AROOF_Y = AEAVE + 1;
        b.walls(4, 0, -4, 8, AEAVE, -1, PLANKS);
        b.fill(5, 0, -3, 7, 0, -1, PLANKS);
        b.column(4, -4, 0, AEAVE, LOG_POST);
        b.column(8, -4, 0, AEAVE, LOG_POST);
        // Two wall thicknesses stand between the annex interior and the hall
        // - the annex's own front wall at z=-1, then the hall's back wall at
        // z=0 - so the doorway needs carving through both.
        b.carve(5, 0, -1, 7, AEAVE - 1, 0);
        b.set(4, 1, -2, GLASS_PANE);
        b.set(8, 1, -2, GLASS_PANE);
        RoofKit.gableAlongZ(b, 4, 8, -4, -1, AROOF_Y, ROOF_WOOD_W, ROOF_WOOD_E, ROOF_WOOD_RIDGE);
        RoofKit.gableEndWallZ(b, 4, 8, -4, AROOF_Y, PLANKS);
        RoofKit.gableEndWallZ(b, 4, 8, -1, AROOF_Y, PLANKS);

        // ---- furnishing: a full civic workbench, then the guest beds --------
        b.set(1, 0, 1, CRAFTING_TABLE);
        b.set(2, 0, 1, FURNACE);
        b.set(3, 0, 1, SMOKER);
        b.set(4, 0, 1, LOOM);
        b.set(5, 0, 1, CARTOGRAPHY_TABLE);
        b.set(6, 0, 1, SMITHING_TABLE);
        b.set(1, 0, 5, CHEST);
        b.set(3, 0, 5, CHEST);
        b.set(11, 0, 5, CHEST);
        b.set(13, 0, 5, CHEST);
        b.set(9, 0, 1, LANTERN);
        b.set(9, 0, 5, LANTERN);
        b.set(5, 0, 5, LANTERN);

        // All four face NORTH (not SOUTH for the front pair) deliberately -
        // z=0 is the carved doorway into the hall, and a south-facing bed's
        // head would land right in it.
        b.bed(5, 0, -3, () -> Blocks.YELLOW_BED, Direction.NORTH);
        b.bed(7, 0, -3, () -> Blocks.YELLOW_BED, Direction.NORTH);
        b.bed(5, 0, -1, () -> Blocks.YELLOW_BED, Direction.NORTH);
        b.bed(7, 0, -1, () -> Blocks.YELLOW_BED, Direction.NORTH);
        b.set(6, 1, -2, LANTERN);

        b.core(7, -1, 3);
        return b.build();
    }
}
