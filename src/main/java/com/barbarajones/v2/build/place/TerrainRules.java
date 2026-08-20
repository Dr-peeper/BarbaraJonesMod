package com.barbarajones.v2.build.place;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Decides what the placement engine is allowed to bulldoze.
 *
 * <p>Two signals, in this order:
 * <ol>
 *   <li><b>The ledger.</b> {@link PlayerBuiltLedger} records every block a
 *       player physically placed. A ledger hit is absolute - if a player put a
 *       dirt block there, a building will not eat it, even though dirt is
 *       otherwise the most clearable thing in the game.</li>
 *   <li><b>The block itself.</b> Terrain, plants and ores clear. Anything
 *       manufactured - planks, glass, wool, a chest, a rail - does not. This is
 *       the fallback that also protects villages, mineshafts and any building
 *       placed before the ledger existed.</li>
 * </ol>
 *
 * <p>Both lists are overridable from data without touching this file, via the
 * block tags {@code barbarajones:build_clearable} and
 * {@code barbarajones:build_protected}. The tags win over everything else, so
 * a wrong call here is a one-line datapack fix rather than a code change.
 *
 * <p>The ledger is server-only, which means the client's ghost preview runs on
 * the block heuristic alone and can be very slightly optimistic (a player-placed
 * dirt block reads green on the client and red on the server). The server is
 * authoritative and says exactly which block stopped it, so the failure mode is
 * an explanatory refusal, never a wrong build.
 */
public final class TerrainRules {

    public static final TagKey<Block> CLEARABLE = TagKey.create(Registries.BLOCK,
            new ResourceLocation(BarbaraJonesMod.MODID, "build_clearable"));
    public static final TagKey<Block> PROTECTED = TagKey.create(Registries.BLOCK,
            new ResourceLocation(BarbaraJonesMod.MODID, "build_protected"));

    /** Tags whose contents are natural world material and may be cleared. */
    private static final List<TagKey<Block>> NATURAL_TAGS = List.of(
            BlockTags.DIRT,
            BlockTags.BASE_STONE_OVERWORLD,
            BlockTags.BASE_STONE_NETHER,
            BlockTags.SAND,
            BlockTags.TERRACOTTA,
            BlockTags.LOGS,
            BlockTags.LEAVES,
            BlockTags.SAPLINGS,
            BlockTags.FLOWERS,
            BlockTags.SNOW,
            BlockTags.ICE,
            BlockTags.NYLIUM,
            BlockTags.WART_BLOCKS,
            BlockTags.CORAL_BLOCKS,
            BlockTags.CAVE_VINES,
            BlockTags.MOSS_REPLACEABLE,
            BlockTags.LUSH_GROUND_REPLACEABLE,
            BlockTags.AZALEA_ROOT_REPLACEABLE,
            BlockTags.SCULK_REPLACEABLE,
            BlockTags.MUSHROOM_GROW_BLOCK);

    /** Loose natural blocks that no convenient tag covers. */
    private static final List<Block> NATURAL_BLOCKS = List.of(
            Blocks.GRAVEL, Blocks.CLAY, Blocks.MUD, Blocks.MUDDY_MANGROVE_ROOTS, Blocks.MANGROVE_ROOTS,
            Blocks.CALCITE, Blocks.TUFF, Blocks.DRIPSTONE_BLOCK, Blocks.POINTED_DRIPSTONE,
            Blocks.MOSS_BLOCK, Blocks.MOSS_CARPET, Blocks.ROOTED_DIRT, Blocks.PODZOL,
            Blocks.MYCELIUM, Blocks.GRASS_BLOCK, Blocks.DIRT_PATH, Blocks.FARMLAND,
            Blocks.POWDER_SNOW, Blocks.MAGMA_BLOCK, Blocks.SOUL_SAND, Blocks.SOUL_SOIL,
            Blocks.BASALT, Blocks.SMOOTH_BASALT, Blocks.BLACKSTONE, Blocks.GLOWSTONE,
            Blocks.SEAGRASS, Blocks.TALL_SEAGRASS, Blocks.KELP, Blocks.KELP_PLANT,
            Blocks.SUGAR_CANE, Blocks.BAMBOO, Blocks.VINE, Blocks.GLOW_LICHEN,
            Blocks.SCULK, Blocks.SCULK_VEIN, Blocks.AMETHYST_BLOCK, Blocks.BUDDING_AMETHYST,
            Blocks.WATER, Blocks.SNOW, Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.BLUE_ICE);

    /** Mod terrain, matched by registry path so a rename in another agent's file cannot silently break this. */
    private static final List<String> NATURAL_MOD_PATHS = List.of(
            "krave_dirt", "krave_grass", "krave_ore", "deepslate_krave_ore",
            "krave_log", "krave_wood", "krave_leaves", "krave_sapling",
            "chocolate_log", "krave_pod", "grass_crop");

    private TerrainRules() { }

    /** What the engine may do with a block that is standing where a building wants to go. */
    public enum Verdict {
        /** Free to remove. */
        CLEARABLE,
        /** Somebody built this. Refuse, and say which block it was. */
        PROTECTED
    }

    public static Verdict verdict(LevelReader level, BlockPos pos, BlockState state) {
        if (state.is(PROTECTED)) {
            return Verdict.PROTECTED;
        }
        if (PlayerBuiltLedger.isPlayerBuilt(level, pos)) {
            return Verdict.PROTECTED;
        }
        if (state.isAir() || state.is(CLEARABLE)) {
            return Verdict.CLEARABLE;
        }
        // Lava is not "protected" in the ownership sense, but building into it is
        // never what anyone meant, so it stops the placement and gets its own message.
        if (state.is(Blocks.LAVA)) {
            return Verdict.PROTECTED;
        }
        // A block entity is a container, a spawner or one of our own core blocks.
        // Never bulldoze one, whatever it is made of.
        if (state.hasBlockEntity()) {
            return Verdict.PROTECTED;
        }
        for (TagKey<Block> tag : NATURAL_TAGS) {
            if (state.is(tag)) {
                return Verdict.CLEARABLE;
            }
        }
        for (Block block : NATURAL_BLOCKS) {
            if (state.is(block)) {
                return Verdict.CLEARABLE;
            }
        }
        if (isModTerrain(state)) {
            return Verdict.CLEARABLE;
        }
        // Anything left with no collision box is vegetation, cobwebs, torches and
        // the like. Growing things clear; a player-placed torch is in the ledger.
        if (state.getCollisionShape(level, pos).isEmpty()) {
            return Verdict.CLEARABLE;
        }
        return Verdict.PROTECTED;
    }

    private static boolean isModTerrain(BlockState state) {
        ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (key == null || !BarbaraJonesMod.MODID.equals(key.getNamespace())) {
            return false;
        }
        return NATURAL_MOD_PATHS.contains(key.getPath());
    }

    /**
     * True if this block counts as ground you can stand a building on: solid
     * enough to hold a floor up, not a plant, not a fluid.
     */
    public static boolean isGround(BlockGetter level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        // A pure fluid block is not ground. A waterlogged stair still is, which
        // is why this tests the block rather than just the fluid state.
        if (state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }
}
