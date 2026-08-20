package com.barbarajones.v2.houses.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModBlocks;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.barbarajones.v2.houses.def.HousePalette.*;

/**
 * Rung 2 of 10: four real walls, a real door, and a proper gable roof instead
 * of one lean-to slope. Still one room, still one bed, but it is a HOUSE now,
 * not a windbreak - the first building in the ladder someone would be
 * embarrassed to still be living in a year later.
 *
 * <p><b>Footprint</b> 5x5, single storey, walls 3 tall. <b>Roofline</b> a
 * proper two-slope gable, ridge running front-to-back, with a triangular
 * glass gable window over the door - the first building in the ladder with a
 * window at all. <b>Furnished</b> with a bed, a chest, and - new this rung - a
 * crafting table, plus a lantern.
 *
 * <p><b>Village contribution</b>: bed (2 building), door (1), crafting table
 * (1), chest (1) = 5 building points, plus attraction/happiness from all
 * four. Villager capacity: 1.
 */
final class SmallHouse {

    static final ResourceLocation ID = new ResourceLocation(BarbaraJonesMod.MODID, "house_small_house");

    private SmallHouse() { }

    static StructureDef build() {
        StructureDef.Builder b = StructureDef.builder(ID)
                .palette(BASE)
                .anchor(StructureDef.Anchor.FRONT)
                .buildTicks(50);

        final int EAVE = 2;
        final int ROOF_Y = EAVE + 1;

        // Shell: four walls, corner posts, plank floor.
        b.walls(0, 0, 0, 4, EAVE, 4, PLANKS);
        b.column(0, 0, 0, EAVE, LOG_POST);
        b.column(4, 0, 0, EAVE, LOG_POST);
        b.column(0, 4, 0, EAVE, LOG_POST);
        b.column(4, 4, 0, EAVE, LOG_POST);
        b.fill(1, 0, 1, 3, 0, 3, PLANKS);

        // Windows on both side walls, door centred on the front (z=4).
        b.set(0, 1, 2, GLASS_PANE);
        b.set(4, 1, 2, GLASS_PANE);
        b.door(2, 0, 4, ModBlocks.KRAVE_DOOR);

        // Gable roof, ridge along Z, glazed triangle over the door.
        RoofKit.gableAlongZ(b, 0, 4, 0, 4, ROOF_Y, ROOF_WOOD_W, ROOF_WOOD_E, ROOF_WOOD_RIDGE);
        RoofKit.gableEndWallZ(b, 0, 4, 0, ROOF_Y, PLANKS);
        RoofKit.gableEndWallZ(b, 0, 4, 4, ROOF_Y, GLASS_PANE);

        // Furnishing.
        b.bed(1, 0, 1, () -> Blocks.RED_BED, Direction.NORTH);
        b.set(3, 0, 1, CHEST);
        b.set(3, 0, 3, CRAFTING_TABLE);
        b.set(1, 0, 3, LANTERN);

        b.core(2, -1, 2);
        return b.build();
    }
}
