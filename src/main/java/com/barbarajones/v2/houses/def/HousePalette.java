package com.barbarajones.v2.houses.def;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.v2.build.def.Palette;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.Half;

/**
 * The character vocabulary every building in this module draws with.
 *
 * <p>One shared {@link Palette} so the ten buildings read as one village's worth
 * of material, not ten unrelated experiments, and so a change to (say) which
 * lantern reads as "light" only has to happen once. Every building starts from
 * {@link #BASE} via {@code StructureDef.Builder#palette(Palette)} and adds only
 * the handful of characters that particular building actually needs beyond this
 * - a bed, a special corner block, whatever makes it that building.
 *
 * <h2>Layout of the table</h2>
 * <ul>
 *   <li>Letters are materials - walls, storage, workstations, comfort.</li>
 *   <li>Digits are roof geometry - always the same four compass stair facings in
 *       whichever of the two roofing materials the building calls for, fed to
 *       {@link RoofKit}. Keeping these on digits means they can never collide
 *       with a material letter a building author reaches for later.</li>
 * </ul>
 *
 * <p>Doors and beds are deliberately NOT in here - {@code StructureDef.Builder}
 * has dedicated {@code door(...)} and {@code bed(...)} helpers that allocate
 * their own private characters per call, which is the correct way to place
 * anything with a HINGE or a PART half.
 */
final class HousePalette {

    private HousePalette() { }

    // ---- roof geometry, krave-plank stairs -------------------------------
    // Read RoofKit before changing these: north/south/east/west here means the
    // direction the SLOPE faces (downhill), which is also the stair's FACING.
    static final char ROOF_WOOD_N = '1';
    static final char ROOF_WOOD_S = '2';
    static final char ROOF_WOOD_E = '3';
    static final char ROOF_WOOD_W = '4';

    // ---- roof geometry, stone-brick stairs (grander / stone-built roofs) --
    static final char ROOF_STONE_N = '5';
    static final char ROOF_STONE_S = '6';
    static final char ROOF_STONE_E = '7';
    static final char ROOF_STONE_W = '8';

    /** Ridge cap for a krave-plank roof: reuse the wall plank character. */
    static final char ROOF_WOOD_RIDGE = '#';
    /** Ridge cap for a stone-brick roof: reuse the stone-brick wall character. */
    static final char ROOF_STONE_RIDGE = 'O';

    // ---- walls / structure --------------------------------------------------
    static final char PLANKS = '#';
    static final char KRAVE_BLOCK = '@';
    static final char LOG_POST = 'K';
    static final char COBBLESTONE = 'o';
    static final char STONE_BRICKS = 'O';
    static final char CHOCOLATE_PLANKS = 'q';
    static final char CHOCOLATE_FENCE = 'Q';

    // ---- openings -------------------------------------------------------
    static final char GLASS_PANE = 'g';
    static final char GLASS_BLOCK = 'G';
    static final char IRON_BARS = 'i';

    // ---- fittings --------------------------------------------------------
    static final char FENCE = 'e';
    static final char FENCE_GATE = 'f';
    static final char SLAB_WOOD = 'l';
    static final char SLAB_STONE = 'm';
    static final char HAY_BALE = 'h';

    // ---- furnishing: beds are placed via Builder#bed, not this table -----
    static final char CHEST = 'c';
    static final char BARREL = 'd';
    static final char CRAFTING_TABLE = 'a';
    static final char FURNACE = 'b';
    static final char SMOKER = 'j';
    static final char LOOM = 'n';
    static final char CARTOGRAPHY_TABLE = 'x';
    static final char SMITHING_TABLE = 'y';
    static final char TORCH = 't';
    static final char LANTERN = 'T';
    static final char SHAG_CARPET = 'p';
    static final char WOOD_PANELING = 'P';
    static final char BELL = 'z';

    /** Built once; every building extends a fresh {@code Builder} copy of this. */
    static final Palette BASE = build();

    private static Palette build() {
        Palette.Builder b = Palette.builder();

        b.block(PLANKS, ModBlocks.KRAVE_PLANKS);
        b.block(KRAVE_BLOCK, ModBlocks.KRAVE_BLOCK);
        b.block(LOG_POST, ModBlocks.KRAVE_LOG);
        b.block(COBBLESTONE, Blocks.COBBLESTONE);
        b.block(STONE_BRICKS, Blocks.STONE_BRICKS);
        b.block(CHOCOLATE_PLANKS, ModBlocks.CHOCOLATE_PLANKS);
        b.block(CHOCOLATE_FENCE, ModBlocks.CHOCOLATE_FENCE);

        b.block(GLASS_PANE, Blocks.GLASS_PANE);
        b.block(GLASS_BLOCK, Blocks.GLASS);
        b.block(IRON_BARS, Blocks.IRON_BARS);

        b.block(FENCE, ModBlocks.KRAVE_FENCE);
        b.block(FENCE_GATE, ModBlocks.KRAVE_FENCE_GATE);
        b.block(SLAB_WOOD, ModBlocks.KRAVE_SLAB);
        b.block(SLAB_STONE, Blocks.STONE_BRICK_SLAB);
        b.block(HAY_BALE, Blocks.HAY_BLOCK);

        b.block(CHEST, Blocks.CHEST);
        b.block(BARREL, Blocks.BARREL);
        b.block(CRAFTING_TABLE, Blocks.CRAFTING_TABLE);
        b.block(FURNACE, Blocks.FURNACE);
        b.block(SMOKER, Blocks.SMOKER);
        b.block(LOOM, Blocks.LOOM);
        b.block(CARTOGRAPHY_TABLE, Blocks.CARTOGRAPHY_TABLE);
        b.block(SMITHING_TABLE, Blocks.SMITHING_TABLE);
        b.block(TORCH, Blocks.TORCH);
        b.block(LANTERN, Blocks.LANTERN);
        b.block(SHAG_CARPET, ModBlocks.SHAG_CARPET);
        b.block(WOOD_PANELING, ModBlocks.WOOD_PANELING);
        b.block(BELL, Blocks.BELL);

        stairState(b, ROOF_WOOD_N, ModBlocks.KRAVE_STAIRS, Direction.NORTH);
        stairState(b, ROOF_WOOD_S, ModBlocks.KRAVE_STAIRS, Direction.SOUTH);
        stairState(b, ROOF_WOOD_E, ModBlocks.KRAVE_STAIRS, Direction.EAST);
        stairState(b, ROOF_WOOD_W, ModBlocks.KRAVE_STAIRS, Direction.WEST);

        stairState(b, ROOF_STONE_N, () -> Blocks.STONE_BRICK_STAIRS, Direction.NORTH);
        stairState(b, ROOF_STONE_S, () -> Blocks.STONE_BRICK_STAIRS, Direction.SOUTH);
        stairState(b, ROOF_STONE_E, () -> Blocks.STONE_BRICK_STAIRS, Direction.EAST);
        stairState(b, ROOF_STONE_W, () -> Blocks.STONE_BRICK_STAIRS, Direction.WEST);

        return b.build();
    }

    private static void stairState(Palette.Builder b, char key,
                                    java.util.function.Supplier<net.minecraft.world.level.block.Block> block,
                                    Direction facing) {
        b.state(key, () -> block.get().defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, Half.BOTTOM));
    }
}
