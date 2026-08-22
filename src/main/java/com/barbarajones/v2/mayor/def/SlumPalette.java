package com.barbarajones.v2.mayor.def;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.v2.build.def.Palette;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

import java.util.List;
import java.util.function.Supplier;

/**
 * The character vocabulary Barbara's public works are drawn with, and the one
 * place the whole look of the Krave Village is decided.
 *
 * <h2>Why this is not {@code HousePalette}</h2>
 * The housing module's palette makes a coherent building: one plank, one stone,
 * one roof material, chosen once so that ten buildings read as one village. This
 * one is the exact opposite on purpose. Almost every structural character here
 * is a {@link Palette.Builder#weighted weighted} mixture of six or seven
 * materials that do not go together, resolved <em>per block at placement time</em>
 * - so no two walls Barbara puts up are the same wall, and the town can never
 * settle into a tidy scheme however large it gets.
 *
 * <p>That is the requirement, stated plainly: the village goes from small and
 * run-down to enormous and run-down. Growth here buys floor area, population and
 * income; it never buys neatness. If a future change makes {@link #WALL} resolve
 * to a single block, the entire aesthetic collapses into a normal Minecraft
 * village and the point of the module is gone.
 *
 * <h2>The mixtures, and what each is for</h2>
 * <ul>
 *   <li>{@link #WALL} - the patchwork. Krave planks are the plurality, but there
 *       is always somebody else's oak, somebody else's cobble, and the odd bare
 *       stripped log where the cladding came off.
 *   <li>{@link #RUBBLE} - masonry that has already lost. Cracked bricks, moss,
 *       gravel and coarse dirt: what a wall looks like after the second repair.
 *   <li>{@link #BOARD} - a window somebody nailed shut. Mismatched planks and
 *       iron bars, never glass.
 *   <li>{@link #WINDOW} - a window somebody did not nail shut, which mostly
 *       means a hole: one in seven resolves to air.
 *   <li>{@link #TARP} - wool. Roofs are flat here and they leak, so a third of
 *       every roof deck is sheeting held down with whatever was to hand.
 * </ul>
 *
 * <h2>Layout of the table</h2>
 * Letters and symbols are materials and fittings. Digits are geometry that
 * carries a facing and therefore cannot be chosen freely: {@code 1-4} are roof
 * stairs (the digit names the direction the slope faces, i.e. downhill) and
 * {@code 5-8} are ladders (the digit names the stair's {@code FACING}, so the
 * solid wall the ladder hangs on is on the <em>opposite</em> side).
 *
 * <p>Doors and beds are deliberately absent - {@code StructureDef.Builder} has
 * {@code door(...)} and {@code bed(...)} helpers that allocate their own private
 * characters, which is the only correct way to place anything with a HINGE or a
 * PART half.
 */
final class SlumPalette {

    private SlumPalette() { }

    // ---- structure ----------------------------------------------------------

    /** Patchwork wall. Never one material; that is the whole point. */
    static final char WALL = '#';
    /** Masonry that has already been repaired badly at least once. */
    static final char RUBBLE = '%';
    /** Corner post - a log, in whatever wood was going spare. */
    static final char POST = 'K';
    /** A window somebody boarded over. */
    static final char BOARD = 'B';
    /** A window somebody did not board over. One in seven is simply a hole. */
    static final char WINDOW = 'g';
    /** Roof sheeting: wool, in four shades of "was white once". */
    static final char TARP = 'T';
    /** Interior floor: half decking, half the dirt it was laid over. */
    static final char FLOOR = '_';
    /** Flat roof deck. Walk on it; the next storey goes on top of it. */
    static final char DECK = '=';
    /** Mixed slabs - kerbs, counters, half-finished second courses. */
    static final char SLAB = '9';

    // ---- openings and edges -------------------------------------------------

    static final char FENCE = 'e';
    static final char BARS = 'i';
    /** Cast concrete pipe. Reads as drainage nobody connected to anything. */
    static final char PIPE = 'u';

    // ---- fittings -----------------------------------------------------------

    static final char CHEST = 'c';
    static final char BARREL = 'd';
    static final char CRAFTING = 'a';
    static final char FURNACE = 'b';
    static final char SMOKER = 'j';
    static final char SMITHING = 'y';
    static final char LOOM = 'n';
    static final char STONECUTTER = 's';
    static final char CARTOGRAPHY = 'x';
    static final char COMPOSTER = 'o';
    static final char KRAFTING_BENCH = 'w';
    static final char TORCH = 't';
    static final char LANTERN = 'L';
    static final char CAMPFIRE = 'C';
    static final char HAY = 'h';
    static final char BELL = 'z';
    static final char CARPET = 'p';
    static final char STASH_BOX = 'S';
    static final char RECLINER = 'v';
    static final char TELEVISION = 'V';
    /** Compressed Krave. The village production system pays out on these. */
    static final char KRAVE_BLOCK = '@';

    // ---- geometry: roof stairs, by the direction the slope faces ------------
    // Complete four-way sets, the same way HousePalette declares its roofing:
    // an individual building only ever hangs an awning off one or two sides, but
    // a palette that only defines the directions today's buildings happen to use
    // is a trap for the next one, which will reach for the missing letter and
    // get a bake-time failure with no obvious cause.

    static final char ROOF_N = '1';
    static final char ROOF_S = '2';
    static final char ROOF_E = '3';
    static final char ROOF_W = '4';

    // ---- geometry: ladders, by FACING (the wall is on the opposite side) ----

    static final char LADDER_N = '5';
    static final char LADDER_S = '6';
    static final char LADDER_E = '7';
    static final char LADDER_W = '8';

    // ---- road surfaces ------------------------------------------------------

    /** Rung 0 road: a path worn into the dirt by people walking on it. */
    static final char ROAD_TRACK = 'D';
    /**
     * Rung 1 road: the same, wider, with the gravel starting to show.
     *
     * <p>'r' rather than the obvious 'e', which the fence already holds. A
     * silently colliding palette key is the kind of mistake that surfaces as
     * one wrong block in one building three weeks later.
     */
    static final char ROAD_WORN = 'r';
    /** Rung 2+ road: dirt, gravel, mud, cracked brick, and four kinds of patch. */
    static final char ROAD_PATCH = 'R';
    /** The bottom of a pothole. Mud, and standing muck. */
    static final char PUDDLE = 'Q';
    /** Muck trodden into whatever is underneath. Survives on any solid block. */
    static final char MUCK = 'm';

    /** Built once. Every structure in this module starts from a copy of this. */
    static final Palette BASE = build();

    // =====================================================================

    private static Palette build() {
        Palette.Builder b = Palette.builder();

        structure(b);
        openings(b);
        fittings(b);
        roofStairs(b);
        ladders(b);
        roads(b);

        return b.build();
    }

    private static void structure(Palette.Builder b) {
        // Krave planks lead, but never by enough to look deliberate. The
        // stripped log is the cladding that fell off; the cobble is the bit
        // somebody fixed with what was in the wheelbarrow.
        b.weighted(WALL, List.of(
                mod(7, ModBlocks.KRAVE_PLANKS),
                plain(4, Blocks.OAK_PLANKS),
                plain(3, Blocks.SPRUCE_PLANKS),
                plain(2, Blocks.DARK_OAK_PLANKS),
                mod(2, ModBlocks.CHOCOLATE_PLANKS),
                plain(3, Blocks.COBBLESTONE),
                plain(2, Blocks.MOSSY_COBBLESTONE),
                mod(2, ModBlocks.STRIPPED_KRAVE_LOG),
                mod(1, ModBlocks.WOOD_PANELING)));

        b.weighted(RUBBLE, List.of(
                plain(5, Blocks.COBBLESTONE),
                plain(4, Blocks.MOSSY_COBBLESTONE),
                plain(4, Blocks.CRACKED_STONE_BRICKS),
                plain(3, Blocks.MOSSY_STONE_BRICKS),
                plain(2, Blocks.ANDESITE),
                plain(2, Blocks.GRAVEL),
                plain(2, Blocks.COARSE_DIRT),
                mod(2, ModBlocks.KRAVE_COBBLESTONE)));

        b.weighted(POST, List.of(
                mod(5, ModBlocks.KRAVE_LOG),
                mod(2, ModBlocks.STRIPPED_KRAVE_LOG),
                plain(3, Blocks.OAK_LOG),
                plain(2, Blocks.SPRUCE_LOG),
                mod(1, ModBlocks.CHOCOLATE_LOG)));

        b.weighted(FLOOR, List.of(
                mod(4, ModBlocks.KRAVE_PLANKS),
                plain(4, Blocks.OAK_PLANKS),
                plain(3, Blocks.SPRUCE_PLANKS),
                plain(3, Blocks.COARSE_DIRT),
                plain(2, Blocks.DIRT),
                plain(2, Blocks.GRAVEL),
                mod(1, ModBlocks.SHAG_CARPET)));

        // A flat roof in this town is a deck with sheeting on it, not a roof.
        b.weighted(DECK, List.of(
                mod(5, ModBlocks.KRAVE_PLANKS),
                plain(3, Blocks.OAK_PLANKS),
                plain(3, Blocks.SPRUCE_PLANKS),
                plain(2, Blocks.STONE_BRICKS),
                plain(2, Blocks.BROWN_WOOL),
                plain(2, Blocks.GRAY_WOOL)));

        b.weighted(TARP, List.of(
                plain(5, Blocks.BROWN_WOOL),
                plain(4, Blocks.GRAY_WOOL),
                plain(3, Blocks.LIGHT_GRAY_WOOL),
                plain(2, Blocks.WHITE_WOOL),
                plain(1, Blocks.BLUE_WOOL)));

        b.weighted(SLAB, List.of(
                mod(4, ModBlocks.KRAVE_SLAB),
                plain(3, Blocks.OAK_SLAB),
                plain(2, Blocks.SPRUCE_SLAB),
                plain(3, Blocks.COBBLESTONE_SLAB),
                plain(2, Blocks.STONE_BRICK_SLAB)));
    }

    private static void openings(Palette.Builder b) {
        b.weighted(BOARD, List.of(
                plain(4, Blocks.OAK_PLANKS),
                plain(3, Blocks.SPRUCE_PLANKS),
                plain(3, Blocks.DARK_OAK_PLANKS),
                mod(3, ModBlocks.KRAVE_PLANKS),
                plain(2, Blocks.IRON_BARS),
                mod(1, ModBlocks.WOOD_PANELING)));

        // One in seven is air: the glass went years ago and nobody replaced it.
        b.weighted(WINDOW, List.of(
                plain(6, Blocks.GLASS_PANE),
                plain(3, Blocks.BROWN_STAINED_GLASS_PANE),
                plain(2, Blocks.IRON_BARS),
                plain(2, Blocks.AIR)));

        b.weighted(FENCE, List.of(
                mod(4, ModBlocks.KRAVE_FENCE),
                plain(3, Blocks.OAK_FENCE),
                mod(2, ModBlocks.CHOCOLATE_FENCE),
                plain(2, Blocks.SPRUCE_FENCE)));

        b.block(BARS, Blocks.IRON_BARS);
        b.block(PIPE, ModBlocks.SEWER_PIPE);
    }

    private static void fittings(Palette.Builder b) {
        b.block(CHEST, Blocks.CHEST);
        b.block(BARREL, Blocks.BARREL);
        b.block(CRAFTING, Blocks.CRAFTING_TABLE);
        b.block(FURNACE, Blocks.FURNACE);
        b.block(SMOKER, Blocks.SMOKER);
        b.block(SMITHING, Blocks.SMITHING_TABLE);
        b.block(LOOM, Blocks.LOOM);
        b.block(STONECUTTER, Blocks.STONECUTTER);
        b.block(CARTOGRAPHY, Blocks.CARTOGRAPHY_TABLE);
        b.block(COMPOSTER, Blocks.COMPOSTER);
        b.block(KRAFTING_BENCH, ModBlocks.KRAFTING_BENCH);
        b.block(TORCH, Blocks.TORCH);
        b.block(LANTERN, Blocks.LANTERN);
        b.block(CAMPFIRE, Blocks.CAMPFIRE);
        b.block(HAY, Blocks.HAY_BLOCK);
        b.block(BELL, Blocks.BELL);
        b.block(CARPET, ModBlocks.SHAG_CARPET);
        b.block(STASH_BOX, ModBlocks.STASH_BOX);
        b.block(RECLINER, ModBlocks.RECLINER);
        b.block(TELEVISION, ModBlocks.TELEVISION);
        b.block(KRAVE_BLOCK, ModBlocks.KRAVE_BLOCK);
    }

    /**
     * Roof stairs. Each direction is itself a mixture, so even a single sloped
     * awning is three materials wide - which is the only way an overhang in this
     * town reads as "salvaged" rather than "built".
     */
    private static void roofStairs(Palette.Builder b) {
        stairs(b, ROOF_N, Direction.NORTH);
        stairs(b, ROOF_S, Direction.SOUTH);
        stairs(b, ROOF_E, Direction.EAST);
        stairs(b, ROOF_W, Direction.WEST);
    }

    private static void stairs(Palette.Builder b, char key, Direction facing) {
        b.weighted(key, List.of(
                Palette.weight(5, () -> stair(ModBlocks.KRAVE_STAIRS.get(), facing)),
                Palette.weight(3, () -> stair(Blocks.OAK_STAIRS, facing)),
                Palette.weight(2, () -> stair(Blocks.SPRUCE_STAIRS, facing)),
                Palette.weight(3, () -> stair(Blocks.COBBLESTONE_STAIRS, facing)),
                Palette.weight(1, () -> stair(Blocks.STONE_BRICK_STAIRS, facing))));
    }

    private static BlockState stair(Block block, Direction facing) {
        return block.defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, Half.BOTTOM);
    }

    private static void ladders(Palette.Builder b) {
        ladder(b, LADDER_N, Direction.NORTH);
        ladder(b, LADDER_S, Direction.SOUTH);
        ladder(b, LADDER_E, Direction.EAST);
        ladder(b, LADDER_W, Direction.WEST);
    }

    private static void ladder(Palette.Builder b, char key, Direction facing) {
        b.state(key, () -> Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing));
    }

    /**
     * Road surfaces, worst to worst. There is no paved option and there is never
     * going to be one: see {@link RoadKit} for the progression these four feed.
     */
    private static void roads(Palette.Builder b) {
        b.weighted(ROAD_TRACK, List.of(
                plain(6, Blocks.DIRT),
                plain(4, Blocks.COARSE_DIRT),
                plain(3, Blocks.DIRT_PATH),
                plain(1, Blocks.ROOTED_DIRT)));

        b.weighted(ROAD_WORN, List.of(
                plain(5, Blocks.COARSE_DIRT),
                plain(4, Blocks.DIRT),
                plain(3, Blocks.GRAVEL),
                plain(2, Blocks.DIRT_PATH),
                plain(1, Blocks.ROOTED_DIRT)));

        // The "patched garbage" rung. Everything anybody ever filled a hole with.
        b.weighted(ROAD_PATCH, List.of(
                plain(5, Blocks.COARSE_DIRT),
                plain(5, Blocks.GRAVEL),
                plain(3, Blocks.DIRT),
                plain(3, Blocks.COBBLESTONE),
                plain(2, Blocks.CRACKED_STONE_BRICKS),
                plain(2, Blocks.ANDESITE),
                plain(2, Blocks.PACKED_MUD),
                plain(2, Blocks.MUD),
                plain(1, Blocks.MOSSY_COBBLESTONE)));

        b.weighted(PUDDLE, List.of(
                plain(5, Blocks.MUD),
                plain(3, Blocks.MUDDY_MANGROVE_ROOTS),
                plain(2, Blocks.CLAY),
                plain(2, Blocks.GRAVEL)));

        // Carpets survive on top of anything solid, which is exactly why they
        // are the muck: a plant here would pop off the gravel on the settle pass
        // and leave the road littered with dropped items.
        b.weighted(MUCK, List.of(
                plain(4, Blocks.MOSS_CARPET),
                plain(3, Blocks.BROWN_CARPET),
                plain(2, Blocks.GRAY_CARPET)));
    }

    // ---- weight helpers ------------------------------------------------------
    // Two names rather than two overloads: a lambda argument makes an overload
    // pair on Block/Supplier<Block> ambiguous at the call site, and the resulting
    // compile error is nowhere near the line that caused it.

    private static Palette.Weighted plain(int weight, Block block) {
        return Palette.weight(weight, block::defaultBlockState);
    }

    private static Palette.Weighted mod(int weight, Supplier<Block> block) {
        return Palette.weight(weight, () -> block.get().defaultBlockState());
    }
}
