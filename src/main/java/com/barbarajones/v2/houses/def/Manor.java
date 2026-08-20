package com.barbarajones.v2.houses.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModBlocks;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.houses.def.HousePalette.*;

/**
 * Rung 7 of 10: the first building that is unmistakably somebody's estate
 * rather than somebody's home. A stone-founded, two-storey main block with a
 * proper pedimented front gable, split into a hall and a bedroom wing on each
 * floor, flanked by two square corner turrets - the ladder's first use of a
 * true pyramid roof, and taller than the house it is attached to.
 *
 * <p><b>Footprint</b> 9x7 main block plus two 3x3 turrets, two storeys.
 * <b>Roofline</b> a front-gabled main roof over an interior-partitioned
 * house, two pyramid-capped towers standing a full level taller. <b>Furnished</b>
 * with five beds across two rooms on two floors, a proper kitchen, storage in
 * both wings, and windows practically everywhere.
 *
 * <p><b>Village contribution</b>: 5 beds (10), door x2 (2), crafting table
 * (1), furnace (1), smoker (1), 3 chests (3), krave block trim (3 per block
 * used) = 21+ building points before the krave-block trim is even counted.
 * Villager capacity: 5.
 */
final class Manor {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "house_manor");

    private Manor() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.FRONT)
                .maxGroundDelta(5)
                .buildTicks(220);

        final int EAVE1 = 2;
        final int FLOOR2 = 3;
        final int EAVE2 = 5;
        final int ROOF_Y = EAVE2 + 1;

        // ---- main block, ground floor -----------------------------------------
        b.walls(0, 0, 0, 8, 0, 6, STONE_BRICKS);
        b.walls(0, 1, 0, 8, EAVE1, 6, PLANKS);
        b.fill(1, 0, 1, 7, 0, 5, PLANKS);
        // Partition: a hall at the front (near the door) and a bedroom wing at
        // the back, joined by a two-wide gap rather than a door.
        b.fill(1, 0, 3, 7, EAVE1, 3, PLANKS);
        b.carve(3, 0, 3, 5, EAVE1 - 1, 3);
        for (int z = 1; z <= 5; z += 2) {
            b.set(0, 1, z, GLASS_PANE);
            b.set(8, 1, z, GLASS_PANE);
        }
        b.door(3, 0, 6, ModBlocks.KRAVE_DOOR, Direction.SOUTH, false);
        b.door(4, 0, 6, ModBlocks.KRAVE_DOOR, Direction.SOUTH, true);

        // ---- second floor ---------------------------------------------------
        b.walls(0, FLOOR2, 0, 8, EAVE2, 6, PLANKS);
        b.fill(1, FLOOR2, 1, 7, FLOOR2, 5, PLANKS);
        b.fill(1, FLOOR2, 3, 7, EAVE2, 3, PLANKS);
        b.carve(3, FLOOR2, 3, 5, EAVE2 - 1, 3);
        for (int z = 1; z <= 5; z += 2) {
            b.set(0, FLOOR2 + 1, z, GLASS_PANE);
            b.set(8, FLOOR2 + 1, z, GLASS_PANE);
        }
        b.set(4, FLOOR2 + 1, 6, GLASS_BLOCK);

        // Krave-block pilasters, full height. All five along the back wall;
        // only the two outer corners at the front, clear of the doorway.
        for (int x = 0; x <= 8; x += 2) {
            b.column(x, 0, 0, EAVE2, KRAVE_BLOCK);
        }
        b.column(0, 6, 0, EAVE2, KRAVE_BLOCK);
        b.column(8, 6, 0, EAVE2, KRAVE_BLOCK);

        // ---- roof: pedimented front gable over the whole main block -----------
        RoofKit.gableAlongZ(b, 0, 8, 0, 6, ROOF_Y, ROOF_WOOD_W, ROOF_WOOD_E, ROOF_WOOD_RIDGE);
        RoofKit.gableEndWallZ(b, 0, 8, 0, ROOF_Y, PLANKS);
        RoofKit.gableEndWallZ(b, 0, 8, 6, ROOF_Y, GLASS_PANE);

        // ---- two corner turrets, a full level taller than the main roof -------
        // Flush against the main block's west/east walls at the front corners
        // (sharing that wall plane, not floating beside it), 3x3 inside.
        buildTurret(b, -2, 0, 4, 6, EAVE2 + 3);
        buildTurret(b, 8, 10, 4, 6, EAVE2 + 3);

        // ---- furnishing ----------------------------------------------------
        b.bed(2, 0, 4, () -> Blocks.PURPLE_BED, Direction.SOUTH);
        b.bed(6, 0, 4, () -> Blocks.PURPLE_BED, Direction.SOUTH);
        b.set(2, 0, 1, CRAFTING_TABLE);
        b.set(3, 0, 1, FURNACE);
        b.set(5, 0, 1, SMOKER);
        b.set(6, 0, 1, CHEST);
        b.set(1, 0, 4, CHEST);

        b.bed(2, FLOOR2, 4, () -> Blocks.PURPLE_BED, Direction.SOUTH);
        b.bed(6, FLOOR2, 4, () -> Blocks.PURPLE_BED, Direction.SOUTH);
        b.bed(4, FLOOR2, 1, () -> Blocks.PURPLE_BED, Direction.NORTH);
        b.set(7, FLOOR2, 4, CHEST);
        b.set(1, FLOOR2, 1, LANTERN);
        b.set(7, FLOOR2, 1, LANTERN);

        b.core(4, -1, 3);
        return b.build();
    }

    /** A hollow square turret, capped with a pyramid roof one level taller than the main house. */
    private static void buildTurret(StructureDef.Builder b, int x1, int x2, int z1, int z2, int eave) {
        b.walls(x1, 0, z1, x2, eave, z2, STONE_BRICKS);
        b.walls(x1, eave - 2, z1, x2, eave - 2, z2, PLANKS);
        int midZ = (z1 + z2) / 2;
        b.set(x1, eave - 1, midZ, IRON_BARS);
        b.set(x2, eave - 1, midZ, IRON_BARS);
        RoofKit.pyramid(b, x1, x2, z1, z2, eave + 1,
                ROOF_STONE_N, ROOF_STONE_S, ROOF_STONE_E, ROOF_STONE_W, ROOF_STONE_RIDGE);
    }
}
