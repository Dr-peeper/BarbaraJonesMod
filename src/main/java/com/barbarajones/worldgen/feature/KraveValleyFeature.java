package com.barbarajones.worldgen.feature;

import com.barbarajones.content.ModBlocks;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The inverse of KraveMountainFeature - a shallow bowl-shaped depression
 * carved down from the surface, so the islands read as having real valleys
 * instead of being uniformly domed. Depth is capped and validated against
 * how much solid ground actually exists below the surface first, so this can
 * never carve through a thin island into the void underneath (same
 * risk-mitigation principle as KraveCavePocketFeature). The naturally
 * shrinking radius as it carves deeper leaves the surrounding untouched
 * terrain as sloped walls - since the ground is already layered
 * grass/dirt/blackstone via the surface_rule, those walls read as a natural
 * cliff face with no extra re-lining pass needed.
 */
public class KraveValleyFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MAX_DEPTH = 10;
    private static final int MIN_DEPTH = 5;
    private static final int MIN_BASE_RADIUS = 6;
    private static final int BASE_RADIUS_RANGE = 5; // -> 6..10
    private static final int MIN_SOLID_BELOW = 6;
    private static final int SAFETY_MARGIN = 3;

    public KraveValleyFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();

        if (!level.getBlockState(origin.below()).isSolid()) {
            return false;
        }

        int solidBelow = countSolidBelow(level, origin, MAX_DEPTH + SAFETY_MARGIN + 2);
        if (solidBelow < MIN_SOLID_BELOW) {
            return false;   // island too thin here - don't risk carving into the void
        }
        int depth = Math.min(MAX_DEPTH, solidBelow - SAFETY_MARGIN);
        if (depth < MIN_DEPTH) {
            return false;
        }
        int baseRadius = MIN_BASE_RADIUS + random.nextInt(BASE_RADIUS_RANGE);

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState floorBlock = ModBlocks.KRAVE_DIRT.get().defaultBlockState();

        for (int y = 0; y < depth; y++) {
            int radius = radiusAt(y, depth, baseRadius);
            boolean isFloor = y == depth - 1;
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius) {
                        continue;
                    }
                    BlockPos pos = origin.offset(dx, -y, dz);
                    level.setBlock(pos, isFloor ? floorBlock : air, 3);
                }
            }
        }
        return true;
    }

    /** Linear taper from baseRadius at the rim down to 1 at the floor. */
    private int radiusAt(int y, int depth, int baseRadius) {
        double t = (double) y / (double) depth;
        return Math.max(1, (int) Math.round(baseRadius * (1.0 - t * 0.6)));
    }

    private int countSolidBelow(WorldGenLevel level, BlockPos origin, int maxScan) {
        int count = 0;
        BlockPos.MutableBlockPos scan = origin.below().mutable();
        while (count < maxScan && level.getBlockState(scan).isSolid()) {
            count++;
            scan.move(0, -1, 0);
        }
        return count;
    }
}
