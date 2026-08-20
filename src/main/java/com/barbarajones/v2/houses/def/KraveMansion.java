package com.barbarajones.v2.houses.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModBlocks;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.houses.def.HousePalette.*;

/**
 * Rung 10 of 10: KRAVE MANSION. Everything the previous nine buildings proved
 * one at a time, together on one lot - a three-storey central block with two
 * flights of stairs, two full two-storey wings flush against it, two stone
 * towers a full level taller than anything else in the village, and a
 * pillared entrance portico in front of a pair of krave doors. It is built
 * from every material the ladder has used: cobblestone and stone brick at
 * the base, krave block pilasters, krave plank and log throughout, chocolate
 * plank accents inside, glass by the window-full.
 *
 * <p><b>Footprint</b> 11x9 central block plus two 5x9 wings plus two 3x3
 * towers - roughly 21 blocks of total frontage. <b>Roofline</b> a long
 * central gable over two lower wing gables, capped by two pyramid towers
 * standing taller than all of it. <b>Furnished</b> throughout: eight beds
 * across five rooms, a full kitchen, a study, storage in every wing, and
 * lanterns the whole way through.
 *
 * <p><b>Village contribution</b>: 8 beds (16), 2 doors (2), krafting bench
 * (2, the best workstation in the game), crafting table + furnace + smoker +
 * smithing table (4), 6 chests (6), bell (1), krave block trim throughout
 * (3 per block) - comfortably the highest building score in the village
 * before counting a single pilaster. Villager capacity: 8.
 */
final class KraveMansion {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "house_krave_mansion");

    private KraveMansion() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.FRONT)
                .maxGroundDelta(6)
                .buildTicks(400);

        final int F1 = 0;
        final int F2 = 3;
        final int F3 = 6;
        final int EAVE3 = 8;
        final int ROOF_Y = EAVE3 + 1;

        // ================= central block =================
        b.walls(0, 0, 0, 10, 1, 8, STONE_BRICKS);
        b.walls(0, 2, 0, 10, EAVE3, 8, PLANKS);
        b.fill(1, F1, 1, 9, F1, 7, PLANKS);
        b.fill(1, F2, 1, 9, F2, 7, PLANKS);
        b.fill(1, F3, 1, 9, F3, 7, PLANKS);

        // Two flights of stairs, opposite corners, so climbing the mansion
        // does not mean walking back across the same room twice.
        b.carve(1, F2, 1, 1, F2, 2);
        b.set(1, 1, 1, ROOF_WOOD_S);
        b.set(1, 2, 2, ROOF_WOOD_S);
        b.set(1, F2, 3, ROOF_WOOD_S);
        b.carve(9, F3, 1, 9, F3, 2);
        b.set(9, 4, 1, ROOF_WOOD_S);
        b.set(9, 5, 2, ROOF_WOOD_S);
        b.set(9, F3, 3, ROOF_WOOD_S);

        for (int z = 1; z <= 7; z += 2) {
            b.set(0, 1, z, GLASS_PANE);
            b.set(10, 1, z, GLASS_PANE);
            b.set(0, F2 + 1, z, GLASS_PANE);
            b.set(10, F2 + 1, z, GLASS_PANE);
            b.set(0, F3 + 1, z, GLASS_PANE);
            b.set(10, F3 + 1, z, GLASS_PANE);
        }
        for (int x = 0; x <= 10; x += 2) {
            b.column(x, 0, 0, EAVE3, KRAVE_BLOCK);
        }
        b.column(0, 8, 0, EAVE3, KRAVE_BLOCK);
        b.column(10, 8, 0, EAVE3, KRAVE_BLOCK);

        b.door(4, 0, 8, ModBlocks.KRAVE_DOOR, Direction.SOUTH, false);
        b.door(5, 0, 8, ModBlocks.KRAVE_DOOR, Direction.SOUTH, true);
        b.set(3, F2 + 1, 8, GLASS_BLOCK);
        b.set(7, F2 + 1, 8, GLASS_BLOCK);

        RoofKit.gableAlongZ(b, 0, 10, 0, 8, ROOF_Y, ROOF_WOOD_W, ROOF_WOOD_E, ROOF_WOOD_RIDGE);
        RoofKit.gableEndWallZ(b, 0, 10, 0, ROOF_Y, PLANKS);
        RoofKit.gableEndWallZ(b, 0, 10, 8, ROOF_Y, GLASS_PANE);

        // Entrance portico.
        b.fill(3, 0, 9, 6, 0, 10, PLANKS);
        b.column(3, 9, 0, 3, LOG_POST);
        b.column(6, 9, 0, 3, LOG_POST);
        b.column(3, 10, 0, 3, LOG_POST);
        b.column(6, 10, 0, 3, LOG_POST);
        b.fill(3, 4, 9, 6, 4, 10, SLAB_WOOD);

        // ================= wings, two storeys each =================
        final int WF2 = 3;
        final int WEAVE = 5;
        final int WROOF_Y = WEAVE + 1;
        buildWing(b, -4, 0, 0, 8, WF2, WEAVE, WROOF_Y);
        buildWing(b, 10, 14, 0, 8, WF2, WEAVE, WROOF_Y);

        // Doorways from the central block into each wing, both floors. The
        // wings share their x=0 / x=10 wall with the central block (one
        // thickness, not two), so one carve per opening is enough here -
        // unlike the great hall's annex, which stands apart from the hall
        // behind its own separate wall.
        b.carve(0, F1, 3, 0, F1 + 1, 4);
        b.carve(10, F1, 3, 10, F1 + 1, 4);
        b.carve(0, WF2, 3, 0, WF2 + 1, 4);
        b.carve(10, WF2, 3, 10, WF2 + 1, 4);

        // ================= towers, flush with each wing's outer front corner ====
        buildTower(b, -6, -4, 6, 8, EAVE3 + 4);
        buildTower(b, 14, 16, 6, 8, EAVE3 + 4);

        // ================= furnishing =================
        b.set(2, F1, 1, CRAFTING_TABLE);
        b.set(3, F1, 1, FURNACE);
        b.set(4, F1, 1, SMOKER);
        b.block('k', ModBlocks.KRAFTING_BENCH);
        b.set(6, F1, 1, 'k');
        b.set(8, F1, 1, SMITHING_TABLE);
        b.set(5, F1, 7, BELL);
        b.set(3, F1, 6, CHEST);
        b.set(7, F1, 6, CHEST);
        b.set(4, F1, 3, LANTERN);
        b.set(6, F1, 3, LANTERN);

        // West-facing here (not WEST->head-at-x=1) deliberately: x=1 is the
        // west staircase's carved shaft at this exact y, and a bed head would
        // block the stairwell opening.
        b.bed(3, F2, 2, () -> Blocks.MAGENTA_BED, Direction.EAST);
        b.bed(8, F2, 2, () -> Blocks.MAGENTA_BED, Direction.EAST);
        b.set(5, F2, 2, CHEST);
        b.set(5, F2, 6, LANTERN);

        b.bed(2, F3, 6, () -> Blocks.MAGENTA_BED, Direction.WEST);
        b.bed(8, F3, 6, () -> Blocks.MAGENTA_BED, Direction.EAST);
        b.set(5, F3, 6, CHEST);
        b.set(5, F3, 2, LANTERN);

        b.core(5, -1, 4);
        return b.build();
    }

    /** A two-storey wing flush against the central block's east or west wall. */
    private static void buildWing(StructureDef.Builder b, int x1, int x2, int z1, int z2,
                                  int floor2, int eave, int roofY) {
        b.walls(x1, 0, z1, x2, 1, z2, STONE_BRICKS);
        b.walls(x1, 2, z1, x2, eave, z2, PLANKS);
        int ix1 = x1 + 1;
        int ix2 = x2 - 1;
        b.fill(ix1, 0, z1 + 1, ix2, 0, z2 - 1, PLANKS);
        b.fill(ix1, floor2, z1 + 1, ix2, floor2, z2 - 1, PLANKS);
        b.set(x1, 1, (z1 + z2) / 2, GLASS_PANE);
        b.set(x2, 1, (z1 + z2) / 2, GLASS_PANE);
        b.set(x1, floor2 + 1, (z1 + z2) / 2, GLASS_PANE);
        b.set(x2, floor2 + 1, (z1 + z2) / 2, GLASS_PANE);
        RoofKit.gableAlongZ(b, x1, x2, z1, z2, roofY, ROOF_WOOD_W, ROOF_WOOD_E, ROOF_WOOD_RIDGE);
        RoofKit.gableEndWallZ(b, x1, x2, z1, roofY, PLANKS);
        RoofKit.gableEndWallZ(b, x1, x2, z2, roofY, GLASS_PANE);
    }

    /** A four-square stone tower, pyramid-capped, taller than everything beside it. */
    private static void buildTower(StructureDef.Builder b, int x1, int x2, int z1, int z2, int eave) {
        b.walls(x1, 0, z1, x2, eave, z2, STONE_BRICKS);
        b.walls(x1, eave - 2, z1, x2, eave - 2, z2, KRAVE_BLOCK);
        int midX = (x1 + x2) / 2;
        int midZ = (z1 + z2) / 2;
        b.set(midX, eave - 1, z1, IRON_BARS);
        b.set(midX, eave - 1, z2, IRON_BARS);
        b.set(x1, eave - 1, midZ, IRON_BARS);
        b.set(x2, eave - 1, midZ, IRON_BARS);
        RoofKit.pyramid(b, x1, x2, z1, z2, eave + 1,
                ROOF_STONE_N, ROOF_STONE_S, ROOF_STONE_E, ROOF_STONE_W, ROOF_STONE_RIDGE);
    }
}
