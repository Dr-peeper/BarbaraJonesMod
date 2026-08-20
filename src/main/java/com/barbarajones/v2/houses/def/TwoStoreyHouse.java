package com.barbarajones.v2.houses.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModBlocks;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.houses.def.HousePalette.*;

/**
 * Rung 4 of 10: the ladder's first upper floor. A real staircase (not a
 * ladder) climbs from the ground floor to a full second storey through a
 * carved stairwell, and the roof ridge now runs the other way - side to side
 * instead of front to back - so it reads as a different silhouette from the
 * cottage even from a distance, not the same box with an extra layer.
 *
 * <p><b>Footprint</b> 6x6, two full storeys of 3 blocks each. <b>Roofline</b>
 * a gable ridged along X, gable-end triangles on the SIDE walls this time
 * (one glazed) rather than the front. <b>Furnished</b> across two floors: a
 * ground-floor guest bed, kitchen (crafting table + furnace) and storage, two
 * more beds and a second chest upstairs.
 *
 * <p><b>Village contribution</b>: 3 beds (6), door (1), crafting table (1),
 * furnace (1), 2 chests (2) = 11 building points. Villager capacity: 3.
 */
final class TwoStoreyHouse {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "house_two_storey_house");

    private TwoStoreyHouse() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.FRONT)
                .buildTicks(110);

        final int EAVE1 = 2;
        final int FLOOR2 = 3;
        final int EAVE2 = 5;
        final int ROOF_Y = EAVE2 + 1;

        // ---- ground floor ---------------------------------------------------
        b.walls(0, 0, 0, 5, EAVE1, 5, PLANKS);
        b.column(0, 0, 0, EAVE1, LOG_POST);
        b.column(5, 0, 0, EAVE1, LOG_POST);
        b.column(0, 5, 0, EAVE1, LOG_POST);
        b.column(5, 5, 0, EAVE1, LOG_POST);
        b.fill(1, 0, 1, 4, 0, 4, PLANKS);
        b.set(0, 1, 3, GLASS_PANE);
        b.set(5, 1, 3, GLASS_PANE);
        b.door(3, 0, 5, ModBlocks.KRAVE_DOOR);

        // ---- second floor slab, walls, stairwell -----------------------------
        b.walls(0, FLOOR2, 0, 5, EAVE2, 5, PLANKS);
        b.fill(1, FLOOR2, 1, 4, FLOOR2, 4, PLANKS);
        b.column(0, 0, FLOOR2, EAVE2, LOG_POST);
        b.column(5, 0, FLOOR2, EAVE2, LOG_POST);
        b.column(0, 5, FLOOR2, EAVE2, LOG_POST);
        b.column(5, 5, FLOOR2, EAVE2, LOG_POST);
        b.set(0, 4, 2, GLASS_PANE);
        b.set(5, 4, 2, GLASS_PANE);

        // Stairwell: a carved 1x3 shaft against the west wall, three stairs
        // climbing it. The floor-slab cells above the first two treads are
        // carved for headroom; the third tread lands flush with the upper
        // floor and needs no carving - it simply replaces that floor plank.
        b.carve(1, FLOOR2, 1, 1, FLOOR2, 2);
        b.set(1, 1, 1, ROOF_WOOD_S);
        b.set(1, 2, 2, ROOF_WOOD_S);
        b.set(1, FLOOR2, 3, ROOF_WOOD_S);

        // ---- roof: gable ridged along X, glazed gable end on the east side ---
        RoofKit.gableAlongX(b, 0, 5, 0, 5, ROOF_Y, ROOF_WOOD_N, ROOF_WOOD_S, ROOF_WOOD_RIDGE);
        RoofKit.gableEndWallX(b, 0, 5, 0, ROOF_Y, PLANKS);
        RoofKit.gableEndWallX(b, 0, 5, 5, ROOF_Y, GLASS_PANE);

        // ---- furnishing, kept clear of the stairwell column (x=1, z=1..3) ----
        b.bed(4, 0, 1, () -> Blocks.RED_BED, Direction.NORTH);
        b.set(2, 0, 1, CRAFTING_TABLE);
        b.set(3, 0, 1, FURNACE);
        b.set(4, 0, 4, CHEST);
        b.set(2, 0, 4, LANTERN);

        b.bed(3, FLOOR2, 1, () -> Blocks.BLUE_BED, Direction.NORTH);
        b.bed(3, FLOOR2, 4, () -> Blocks.BLUE_BED, Direction.SOUTH);
        b.set(4, FLOOR2, 2, CHEST);
        b.set(2, FLOOR2, 4, LANTERN);

        b.core(2, -1, 2);
        return b.build();
    }
}
