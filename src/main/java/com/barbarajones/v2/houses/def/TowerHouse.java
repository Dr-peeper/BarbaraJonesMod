package com.barbarajones.v2.houses.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModBlocks;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;

import static com.barbarajones.v2.houses.def.HousePalette.*;

/**
 * Rung 8 of 10: the ladder's first building that grows by going STRAIGHT UP -
 * four floors in a single 5x5 stone shaft, climbed by a ladder through a
 * carved hole in each floor slab rather than a staircase, and finished flat
 * with a crenellated parapet instead of any kind of pitched roof. It is built
 * for defence, not comfort - a small garrison keep, not a home for a large
 * family, which is why its villager capacity is lower than the ranch's
 * despite costing more and standing taller.
 *
 * <p><b>Footprint</b> 5x5, four storeys, stone throughout. <b>Roofline</b> a
 * flat battlemented top - no stairs, no slope, the first building in the
 * ladder with no roof shape from {@link RoofKit} at all. <b>Furnished</b> as
 * a garrison: an armoury floor at the base, one bed per floor above it, ending
 * in a windowed lookout at the top.
 *
 * <p><b>Village contribution</b>: 3 beds (6), door (1), furnace (1), smithing
 * table (0, not scored), chest x3 (3), iron bars are not scored either - this
 * building's real contribution is defence, which the buff table does not
 * derive from raw block count the way building score does. Villager
 * capacity: 3.
 */
final class TowerHouse {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "house_tower_house");

    private TowerHouse() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.FRONT)
                .maxGroundDelta(2)
                .buildTicks(260);

        final int F1 = 0;
        final int F2 = 3;
        final int F3 = 6;
        final int F4 = 9;
        final int TOP = 11;

        // ---- four floors, one shell -----------------------------------------
        b.walls(0, F1, 0, 4, TOP, 4, STONE_BRICKS);
        b.fill(1, F1, 1, 3, F1, 3, COBBLESTONE);
        b.fill(1, F2, 1, 3, F2, 3, COBBLESTONE);
        b.fill(1, F3, 1, 3, F3, 3, COBBLESTONE);
        b.fill(1, F4, 1, 3, F4, 3, COBBLESTONE);

        // Ladder shaft: one interior cell, open through every floor slab,
        // climbed continuously from the ground to the lookout.
        b.carve(2, F2, 1, 2, F2, 1);
        b.carve(2, F3, 1, 2, F3, 1);
        b.carve(2, F4, 1, 2, F4, 1);
        // A continuous run of ladder, floor to parapet hatch - including the
        // carved cells themselves, so climbing through each hole is unbroken.
        char ladder = b.key(() -> Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        for (int y = 1; y <= TOP + 1; y++) {
            b.set(2, y, 1, ladder);
        }

        // Arrow-slit windows, staggered so no two floors line up the same way.
        b.set(2, F1 + 1, 0, IRON_BARS);
        b.set(0, F2 + 1, 2, IRON_BARS);
        b.set(4, F2 + 1, 2, IRON_BARS);
        b.set(2, F3 + 1, 0, IRON_BARS);
        b.set(0, F3 + 1, 2, IRON_BARS);
        b.set(4, F3 + 1, 2, IRON_BARS);
        // The lookout floor gets real windows on all four sides, not slits.
        b.set(2, F4 + 1, 0, GLASS_PANE);
        b.set(0, F4 + 1, 2, GLASS_PANE);
        b.set(4, F4 + 1, 2, GLASS_PANE);
        b.set(2, F4 + 1, 4, GLASS_PANE);

        b.door(2, F1, 4, ModBlocks.KRAVE_DOOR);

        // ---- flat battlemented top, no pitched roof at all -------------------
        int parapetY = TOP + 1;
        b.fill(0, parapetY, 0, 4, parapetY, 4, STONE_BRICKS);
        b.carve(2, parapetY, 1, 2, parapetY, 1);
        for (int x = 0; x <= 4; x += 2) {
            b.set(x, parapetY + 1, 0, STONE_BRICKS);
            b.set(x, parapetY + 1, 4, STONE_BRICKS);
        }
        for (int z = 0; z <= 4; z += 2) {
            b.set(0, parapetY + 1, z, STONE_BRICKS);
            b.set(4, parapetY + 1, z, STONE_BRICKS);
        }
        b.set(2, parapetY, 2, LANTERN);

        // ---- furnishing --------------------------------------------------
        b.set(0, F1, 1, CHEST);
        b.set(0, F1, 3, BARREL);
        b.set(4, F1, 1, FURNACE);
        b.set(4, F1, 3, SMITHING_TABLE);
        b.set(2, F1 + 1, 2, LANTERN);

        b.bed(3, F2, 3, () -> Blocks.GRAY_BED, Direction.WEST);
        b.set(0, F2, 3, CHEST);

        b.bed(3, F3, 3, () -> Blocks.GRAY_BED, Direction.WEST);
        b.set(0, F3, 3, CHEST);

        b.bed(3, F4, 3, () -> Blocks.GRAY_BED, Direction.WEST);
        b.set(0, F4, 3, CRAFTING_TABLE);

        b.core(2, -1, 2);
        return b.build();
    }
}
