package com.barbarajones.v2.houses.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModBlocks;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.houses.def.HousePalette.*;

/**
 * Rung 3 of 10: the small house with a stone plinth under it, two beds instead
 * of one, and a covered porch out front with its own little lean-to awning -
 * the first building in the ladder built from more than one roof shape at
 * once.
 *
 * <p><b>Footprint</b> 7x6 plus a 3-wide porch bay, single storey, walls 3
 * tall on a cobblestone plinth. <b>Roofline</b> the same front-to-back gable
 * as the small house, now with a mono-slope porch awning tucked under the
 * front gable. <b>Furnished</b> for two: two beds, a chest AND a barrel, a
 * crafting table and a furnace, two lanterns.
 *
 * <p><b>Village contribution</b>: 2 beds (4), door (1), crafting table (1),
 * furnace (1), chest (1), fence gate implied by the fence posts (0 - plain
 * fence scores nothing, only the gate does) = 8 building points. Villager
 * capacity: 2.
 */
final class Cottage {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "house_cottage");

    private Cottage() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.FRONT)
                .buildTicks(80);

        final int EAVE = 2;
        final int ROOF_Y = EAVE + 1;

        // Shell: cobblestone plinth, plank walls above it, log corners.
        b.walls(0, 0, 0, 6, 0, 5, COBBLESTONE);
        b.walls(0, 1, 0, 6, EAVE, 5, PLANKS);
        b.column(0, 0, 0, EAVE, LOG_POST);
        b.column(6, 0, 0, EAVE, LOG_POST);
        b.column(0, 5, 0, EAVE, LOG_POST);
        b.column(6, 5, 0, EAVE, LOG_POST);
        b.fill(1, 0, 1, 5, 0, 4, PLANKS);

        // Windows, two per side wall.
        b.set(0, 1, 1, GLASS_PANE);
        b.set(0, 1, 4, GLASS_PANE);
        b.set(6, 1, 1, GLASS_PANE);
        b.set(6, 1, 4, GLASS_PANE);

        b.door(3, 0, 5, ModBlocks.KRAVE_DOOR);

        // Main roof: gable, ridge front-to-back.
        RoofKit.gableAlongZ(b, 0, 6, 0, 5, ROOF_Y, ROOF_WOOD_W, ROOF_WOOD_E, ROOF_WOOD_RIDGE);
        RoofKit.gableEndWallZ(b, 0, 6, 0, ROOF_Y, PLANKS);
        RoofKit.gableEndWallZ(b, 0, 6, 5, ROOF_Y, GLASS_PANE);

        // Porch: a plank deck under a small lean-to awning of its own, railed
        // in fence with a gap left open for the step down.
        b.fill(2, 0, 6, 4, 0, 6, PLANKS);
        RoofKit.monoSlope(b, 2, 4, 5, ROOF_Y, 6, EAVE, ROOF_WOOD_S);
        b.column(2, 6, 0, 0, FENCE);
        b.column(4, 6, 0, 0, FENCE);

        // Furnishing: two beds, storage both kinds, a full little kitchen.
        b.bed(1, 0, 1, () -> Blocks.RED_BED, Direction.NORTH);
        b.bed(5, 0, 1, () -> Blocks.RED_BED, Direction.NORTH);
        b.set(1, 0, 4, CHEST);
        b.set(5, 0, 4, BARREL);
        b.set(3, 0, 1, CRAFTING_TABLE);
        b.set(3, 0, 2, FURNACE);
        b.set(1, 0, 3, LANTERN);
        b.set(5, 0, 3, LANTERN);

        b.core(3, -1, 2);
        return b.build();
    }
}
