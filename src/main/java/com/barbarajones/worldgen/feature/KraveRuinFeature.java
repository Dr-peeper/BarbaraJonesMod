package com.barbarajones.worldgen.feature;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.content.ModItems;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * An actual explorable building now, not decoration: a cluster of hollow
 * cube rooms of varying size, stacked at staggered heights and offset
 * horizontally - modeled on Habitat 67 in Montreal (Moshe Safdie's modular
 * stacked-cube apartment complex), which is exactly the "different sized
 * cube rooms placed together" look this is going for. Every room is a real
 * space you can walk into, not a solid prop; roofs of lower-tier cubes
 * double as terraces for the ones stacked above them, the same way the
 * real building works, and a ladder on each elevated cube's outer wall
 * makes the whole thing climbable from ground level.
 *
 * <p>One shared ground reference for the whole cluster (found once, near
 * the origin) rather than KraveMountainFeature's per-column terrain-chasing
 * - this is an authored structure, not organic terrain, so it should read
 * as one building sitting on one foundation. Each cube still gets its own
 * support column down to solid ground beneath its footprint, since the
 * underlying terrain isn't flat and a shared reference height alone would
 * leave some corners floating.
 */
public class KraveRuinFeature extends Feature<NoneFeatureConfiguration> {

    private static final int GRID = 3;             // 3x3 potential cube slots
    private static final int SPACING = 8;           // center-to-center distance between slots
    private static final int TIER_HEIGHT = 5;       // vertical rise per stacking tier
    private static final int MIN_SIZE = 4;
    private static final int SIZE_RANGE = 3;        // -> 4..6
    private static final int TIERS = 3;             // 0..2
    private static final int GROUND_SEARCH = 20;
    private static final int MIN_CUBES = 4;
    private static final int SUPPORT_MAX_DEPTH = 40;

    public KraveRuinFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();

        BlockPos ground = findGround(level, origin);
        if (ground == null) {
            return false;
        }

        BlockState wall = ModBlocks.KRAVE_BLOCK.get().defaultBlockState();
        int center = GRID / 2;

        boolean[][] present = new boolean[GRID][GRID];
        int[][] tier = new int[GRID][GRID];
        int[][] size = new int[GRID][GRID];
        int count = 0;
        for (int gx = 0; gx < GRID; gx++) {
            for (int gz = 0; gz < GRID; gz++) {
                boolean here = (gx == center && gz == center) || random.nextInt(100) < 65;
                present[gx][gz] = here;
                tier[gx][gz] = random.nextInt(TIERS);
                size[gx][gz] = MIN_SIZE + random.nextInt(SIZE_RANGE);
                if (here) {
                    count++;
                }
            }
        }
        // Guarantee a real cluster, not one isolated cube - force more slots
        // on if the random pass came up sparse.
        while (count < MIN_CUBES) {
            int gx = random.nextInt(GRID);
            int gz = random.nextInt(GRID);
            if (!present[gx][gz]) {
                present[gx][gz] = true;
                count++;
            }
        }

        List<BlockPos> roofEdges = new ArrayList<>();
        boolean placedBarrel = false;

        for (int gx = 0; gx < GRID; gx++) {
            for (int gz = 0; gz < GRID; gz++) {
                if (!present[gx][gz]) {
                    continue;
                }
                int s = size[gx][gz];
                int localX = (gx - center) * SPACING;
                int localZ = (gz - center) * SPACING;
                int baseY = ground.getY() + tier[gx][gz] * TIER_HEIGHT;
                BlockPos base = new BlockPos(ground.getX() + localX, baseY, ground.getZ() + localZ);

                Direction doorDir = doorDirection(gx, gz, center, random);
                buildCube(level, base, s, wall, doorDir, tier[gx][gz] == 0, roofEdges);
                supportUnderFootprint(level, base, s, wall);

                if (tier[gx][gz] > 0) {
                    placeLadder(level, base, s, doorDir.getOpposite(), random);
                }

                if (!placedBarrel) {
                    placeLoot(level, base, s, random);
                    placedBarrel = true;
                }
            }
        }

        // A guaranteed chocolate spring somewhere on the cluster's roofline -
        // a second reliable source alongside KraveMountainFeature's, not
        // left to the same rarity_filter that gates whether this whole
        // structure spawns at all.
        if (!roofEdges.isEmpty()) {
            BlockState chocolate = ModBlocks.CHOCOLATE_BLOCK.get().defaultBlockState();
            level.setBlock(roofEdges.get(random.nextInt(roofEdges.size())), chocolate, 3);
        }

        return true;
    }

    @Nullable
    private BlockPos findGround(WorldGenLevel level, BlockPos origin) {
        BlockPos.MutableBlockPos pos = origin.offset(0, GROUND_SEARCH, 0).mutable();
        int minY = origin.getY() - GROUND_SEARCH;
        while (pos.getY() > minY && !level.getBlockState(pos).isSolid()) {
            pos.move(0, -1, 0);
        }
        return level.getBlockState(pos).isSolid() ? pos.immutable() : null;
    }

    /** Faces toward the grid's center cell, so most rooms open into the shared core of the cluster. */
    private Direction doorDirection(int gx, int gz, int center, RandomSource random) {
        int dx = Integer.signum(center - gx);
        int dz = Integer.signum(center - gz);
        if (dx == 0 && dz == 0) {
            return Direction.Plane.HORIZONTAL.getRandomDirection(random);
        }
        if (dx != 0 && (dz == 0 || random.nextBoolean())) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        }
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /**
     * A hollow room: solid floor and roof, solid walls on all 4 sides except
     * a 2-wide, 2-tall doorway on doorDir - and, for ground-tier cubes, a
     * second doorway on the opposite (outward-facing) wall so there's
     * always an obvious way in from outside the whole cluster, not just
     * from its core.
     */
    private void buildCube(WorldGenLevel level, BlockPos base, int size, BlockState wall,
                           Direction doorDir, boolean groundTier, List<BlockPos> roofEdges) {
        int mid = size / 2;
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                for (int y = 0; y < size; y++) {
                    BlockPos p = base.offset(x, y, z);
                    boolean isFloor = y == 0;
                    boolean isRoof = y == size - 1;
                    boolean isWall = x == 0 || x == size - 1 || z == 0 || z == size - 1;
                    if (isFloor || isRoof) {
                        level.setBlock(p, wall, 3);
                    } else if (isWall) {
                        if (isDoorGap(x, z, y, size, mid, doorDir)
                                || (groundTier && isDoorGap(x, z, y, size, mid, doorDir.getOpposite()))) {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                        } else {
                            level.setBlock(p, wall, 3);
                        }
                    } else {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
        }
        roofEdges.add(base.offset(mid, size - 1, 0));
        roofEdges.add(base.offset(mid, size - 1, size - 1));
        roofEdges.add(base.offset(0, size - 1, mid));
        roofEdges.add(base.offset(size - 1, size - 1, mid));
    }

    private boolean isDoorGap(int x, int z, int y, int size, int mid, Direction dir) {
        int lo = mid - 1;
        boolean heightOk = y == 1 || y == 2;
        if (!heightOk || size <= 3) {
            return false;
        }
        return switch (dir) {
            case WEST -> x == 0 && (z == lo || z == lo + 1);
            case EAST -> x == size - 1 && (z == lo || z == lo + 1);
            case NORTH -> z == 0 && (x == lo || x == lo + 1);
            case SOUTH -> z == size - 1 && (x == lo || x == lo + 1);
            default -> false;
        };
    }

    /** Fills straight down from each floor corner to solid ground, so no cube floats over uneven terrain. */
    private void supportUnderFootprint(WorldGenLevel level, BlockPos base, int size, BlockState wall) {
        int[][] corners = { {0, 0}, {size - 1, 0}, {0, size - 1}, {size - 1, size - 1} };
        for (int[] c : corners) {
            BlockPos.MutableBlockPos pos = base.offset(c[0], -1, c[1]).mutable();
            int depth = 0;
            while (depth < SUPPORT_MAX_DEPTH && !level.getBlockState(pos).isSolid()) {
                level.setBlock(pos, wall, 3);
                pos.move(0, -1, 0);
                depth++;
            }
        }
    }

    /** A climbable strip on the wall facing away from the door, from ground up to this cube's floor. */
    private void placeLadder(WorldGenLevel level, BlockPos base, int size, Direction facing, RandomSource random) {
        int mid = size / 2;
        BlockPos wallPos = switch (facing) {
            case WEST -> base.offset(0, 0, mid);
            case EAST -> base.offset(size - 1, 0, mid);
            case NORTH -> base.offset(mid, 0, 0);
            default -> base.offset(mid, 0, size - 1);
        };
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, facing);
        BlockPos.MutableBlockPos pos = wallPos.mutable();
        int guard = 0;
        while (guard < TIER_HEIGHT * TIERS + 4 && !level.getBlockState(pos).isSolid()) {
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, ladder, 3);
            }
            pos.move(0, -1, 0);
            guard++;
        }
    }

    private void placeLoot(WorldGenLevel level, BlockPos base, int size, RandomSource random) {
        BlockPos spot = base.offset(size / 2, 1, size / 2);
        level.setBlock(spot, Blocks.BARREL.defaultBlockState(), 3);
        if (level.getBlockEntity(spot) instanceof BarrelBlockEntity barrel) {
            barrel.setItem(4, new ItemStack(ModItems.KRAVE_CEREAL.get(), 4 + random.nextInt(5)));
            barrel.setItem(11, new ItemStack(ModItems.KRAVE_TETHER.get(), 1 + random.nextInt(2)));
            if (random.nextBoolean()) {
                barrel.setItem(13, new ItemStack(ModItems.KRAVE_SWORD.get()));
            }
        }
    }
}
