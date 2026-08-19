package com.barbarajones.worldgen.feature;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.block.krave.KraveWood;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * One Krave tree: a chocolate trunk, a blocky cereal canopy, and one to three
 * Krave Pods hanging off the bark.
 *
 * <p>Written by hand rather than configured through {@code minecraft:tree} because
 * the pods are the point. A vanilla {@code TreeConfiguration} has a decorator slot
 * for cocoa, but it is wired to jungle logs and vanilla cocoa; placing our own pod
 * against our own trunk needs the trunk positions, which only the trunk-placing
 * code has.
 *
 * <p>Both entry points come through here: the placed feature scatters these across
 * the overworld, and a Krave Sapling grows one via {@code KraveTreeGrower}.
 */
public class KraveTreeFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_TRUNK = 5;
    private static final int TRUNK_VARIANCE = 4;
    /** Leaves at distance 7 decay immediately, so nothing generated may reach it. */
    private static final int MAX_LEAF_DISTANCE = 6;

    public KraveTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();

        if (!canRootHere(level, origin)) {
            return false;
        }

        int trunk = MIN_TRUNK + random.nextInt(TRUNK_VARIANCE);
        // The crown reaches two blocks past the last log, so the whole column has
        // to be clear before a single block is placed - a half-built tree left
        // behind by an early bail-out is worse than no tree.
        for (int y = 0; y <= trunk + 1; y++) {
            if (!isOpen(level, origin.above(y))) {
                return false;
            }
        }

        BlockState log = KraveWood.LOG.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        for (int y = 0; y < trunk; y++) {
            level.setBlock(origin.above(y), log, 3);
        }

        placeCanopy(level, random, origin.above(trunk - 1));
        placePods(level, random, origin, trunk);
        return true;
    }

    /**
     * A squat two-layer blob with a tapered crown, centred on the topmost log.
     * Cereal does not droop, so the silhouette is deliberately boxier than an oak.
     */
    private void placeCanopy(WorldGenLevel level, RandomSource random, BlockPos top) {
        for (int dy = -1; dy <= 2; dy++) {
            int radius = dy <= 0 ? 2 : 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int spread = Math.abs(dx) + Math.abs(dz);
                    if (spread == 0 && dy <= 0) {
                        continue;                                   // the trunk lives here
                    }
                    if (dy == 2 && spread > 1) {
                        continue;                                   // taper the crown to a plus
                    }
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2 && random.nextInt(3) != 0) {
                        continue;                                   // chew the corners off
                    }
                    int distance = Math.min(MAX_LEAF_DISTANCE,
                            Math.max(1, spread + Math.max(0, dy)));
                    placeLeaf(level, top.offset(dx, dy, dz), distance);
                }
            }
        }
    }

    /**
     * Leaves have to be written with a real DISTANCE. Default state is 7, which
     * {@link LeavesBlock#randomTick} treats as orphaned and drops on the first tick
     * after the chunk loads - the tree would generate and then quietly go bald.
     */
    private void placeLeaf(WorldGenLevel level, BlockPos pos, int distance) {
        if (!isOpen(level, pos)) {
            return;
        }
        // The other branch registered its own krave_leaves, which won the id clash,

        // so KraveWood.LEAVES is no longer registered and must not be placed here.

        level.setBlock(pos, ModBlocks.KRAVE_LEAVES.get().defaultBlockState()
                .setValue(LeavesBlock.DISTANCE, distance)
                .setValue(LeavesBlock.PERSISTENT, Boolean.FALSE)
                .setValue(LeavesBlock.WATERLOGGED, Boolean.FALSE), 3);
    }

    private void placePods(WorldGenLevel level, RandomSource random, BlockPos origin, int trunk) {
        int pods = 1 + random.nextInt(3);
        for (int i = 0; i < pods; i++) {
            // Keep them below the canopy and off the ground so they are reachable
            // and visible from outside the leaves.
            int y = 1 + random.nextInt(Math.max(1, trunk - 2));
            Direction face = Direction.from2DDataValue(random.nextInt(4));
            BlockPos podPos = origin.above(y).relative(face);
            if (!isOpen(level, podPos)) {
                continue;
            }
            level.setBlock(podPos, KraveWood.POD.defaultBlockState()
                    // FACING points back at the trunk that holds the pod up.
                    .setValue(HorizontalDirectionalBlock.FACING, face.getOpposite())
                    .setValue(CocoaBlock.AGE, 1 + random.nextInt(2)), 3);
        }
    }

    /**
     * Air, or anything the world treats as scenery. Fluids are excluded by hand:
     * {@code BlockTags.REPLACEABLE} lists water and lava, and a trunk rising out of
     * an ocean looks like a bug.
     */
    private static boolean isOpen(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        return state.isAir() || state.is(BlockTags.REPLACEABLE) || state.is(BlockTags.LEAVES);
    }

    private static boolean canRootHere(WorldGenLevel level, BlockPos origin) {
        BlockState ground = level.getBlockState(origin.below());
        boolean soil = ground.is(BlockTags.DIRT) || ground.is(Blocks.FARMLAND);
        return soil && isOpen(level, origin);
    }
}
