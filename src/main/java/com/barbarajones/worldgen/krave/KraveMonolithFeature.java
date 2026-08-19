package com.barbarajones.worldgen.krave;

import com.barbarajones.content.ModBlocks;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The Krave Monolith - a cereal box the size of a house, standing open-topped
 * on the skyline of the Krave world type.
 *
 * <p>This is the landmark that answers "why does this world feel different?"
 * from a distance, before the player has read a single tooltip. It is
 * deliberately a silhouette rather than a dungeon: a hollow Krave-block shell
 * with a torn-open top and a doorway at ground level, lit from inside by
 * shroomlight so it glows out of the tear after dark.
 *
 * <p>Blocks only - no entities and no block entities. Spawning from worldgen is
 * a known crash source in this project, and a landmark that generates during
 * chunk fill must not depend on anything that is not loaded yet.
 */
public class KraveMonolithFeature extends Feature<NoneFeatureConfiguration> {

    /** Long side of the footprint, in blocks. */
    private static final int LONG_SIDE = 5;

    /** Short side of the footprint, in blocks. */
    private static final int SHORT_SIDE = 3;

    /** How far the box is driven into the ground so it never looks perched. */
    private static final int FOOTING = 3;

    /** Reject the site if the footprint columns differ by more than this. */
    private static final int MAX_SLOPE = 3;

    public KraveMonolithFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();

        // half-extents, randomly rotated a quarter turn so a run of monoliths
        // does not read as a row of identical props
        boolean turned = random.nextBoolean();
        int halfX = (turned ? SHORT_SIDE : LONG_SIDE) / 2;
        int halfZ = (turned ? LONG_SIDE : SHORT_SIDE) / 2;

        int minSurface = Integer.MAX_VALUE;
        int maxSurface = Integer.MIN_VALUE;
        for (int dx = -halfX; dx <= halfX; dx++) {
            for (int dz = -halfZ; dz <= halfZ; dz++) {
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,
                        origin.getX() + dx, origin.getZ() + dz);
                minSurface = Math.min(minSurface, surface);
                maxSurface = Math.max(maxSurface, surface);
            }
        }
        if (maxSurface - minSurface > MAX_SLOPE) {
            return false;   // a cliff edge - the box would hang in mid air
        }

        BlockPos footing = new BlockPos(origin.getX(), minSurface - 1, origin.getZ());
        if (!level.getBlockState(footing).isSolid()
                || !level.getFluidState(footing).isEmpty()) {
            return false;   // water, lava or a cave roof - nothing to stand on
        }

        BlockState shell = ModBlocks.KRAVE_BLOCK.get().defaultBlockState();
        BlockState glow = Blocks.SHROOMLIGHT.defaultBlockState();
        BlockState spill = Blocks.PACKED_MUD.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        int bottom = minSurface - FOOTING;
        int height = 13 + random.nextInt(9);
        int top = minSurface + height;

        for (int y = bottom; y <= top; y++) {
            for (int dx = -halfX; dx <= halfX; dx++) {
                for (int dz = -halfZ; dz <= halfZ; dz++) {
                    boolean wall = Math.abs(dx) == halfX || Math.abs(dz) == halfZ;
                    BlockPos pos = new BlockPos(origin.getX() + dx, y, origin.getZ() + dz);
                    if (wall) {
                        level.setBlock(pos, shell, 3);
                    } else if (y > bottom) {
                        // hollow interior, lit every few courses so the glow
                        // climbs the inside and escapes through the torn top
                        level.setBlock(pos, (y - bottom) % 6 == 0 ? glow : air, 3);
                    } else {
                        level.setBlock(pos, shell, 3);
                    }
                }
            }
        }

        tearTop(level, random, origin, halfX, halfZ, top, air);
        cutDoorway(level, origin, halfX, halfZ, minSurface, air);
        scatterSpill(level, random, origin, halfX, halfZ, minSurface, spill, shell);
        return true;
    }

    /**
     * Chews a few blocks out of the top ring. A perfectly square rim reads as a
     * player build; a ragged one reads as a box somebody ripped open.
     */
    private void tearTop(WorldGenLevel level, RandomSource random, BlockPos origin,
                         int halfX, int halfZ, int top, BlockState air) {
        int bites = 3 + random.nextInt(4);
        for (int i = 0; i < bites; i++) {
            int dx = random.nextInt(halfX * 2 + 1) - halfX;
            int dz = random.nextInt(halfZ * 2 + 1) - halfZ;
            if (Math.abs(dx) != halfX && Math.abs(dz) != halfZ) {
                continue;   // interior is already open
            }
            int depth = 1 + random.nextInt(2);
            for (int y = 0; y < depth; y++) {
                level.setBlock(new BlockPos(origin.getX() + dx, top - y, origin.getZ() + dz),
                        air, 3);
            }
        }
    }

    /** A one-wide, two-high way in on the long face, at standing height. */
    private void cutDoorway(WorldGenLevel level, BlockPos origin, int halfX, int halfZ,
                            int surface, BlockState air) {
        boolean onXFace = halfX >= halfZ;
        for (int y = 0; y < 2; y++) {
            BlockPos pos = onXFace
                    ? new BlockPos(origin.getX(), surface + y, origin.getZ() - halfZ)
                    : new BlockPos(origin.getX() - halfX, surface + y, origin.getZ());
            level.setBlock(pos, air, 3);
        }
    }

    /** Cereal that fell out when the box was torn open. */
    private void scatterSpill(WorldGenLevel level, RandomSource random, BlockPos origin,
                              int halfX, int halfZ, int surface,
                              BlockState spill, BlockState shell) {
        for (int i = 0; i < 14; i++) {
            int dx = random.nextInt(halfX * 2 + 7) - (halfX + 3);
            int dz = random.nextInt(halfZ * 2 + 7) - (halfZ + 3);
            if (Math.abs(dx) <= halfX && Math.abs(dz) <= halfZ) {
                continue;   // that is the box itself
            }
            int x = origin.getX() + dx;
            int z = origin.getZ() + dz;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (Math.abs(y - surface) > 2 || !level.getBlockState(pos).isAir()) {
                continue;
            }
            if (!level.getBlockState(pos.below()).isSolid()) {
                continue;
            }
            level.setBlock(pos, random.nextInt(4) == 0 ? shell : spill, 3);
        }
    }
}
