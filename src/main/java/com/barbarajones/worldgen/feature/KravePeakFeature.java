package com.barbarajones.worldgen.feature;

import com.barbarajones.content.ModBlocks;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * A small, common spire - the scaled-down, frequent counterpart to
 * KraveMountainFeature. Same column-major, local-ground-following
 * construction (see KraveMountainFeature's javadoc for why) so small peaks
 * don't float above the natural terrain either.
 */
public class KravePeakFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_HEIGHT = 8;
    private static final int HEIGHT_RANGE = 9;    // -> 8..16
    private static final int MIN_BASE_RADIUS = 2;
    private static final int BASE_RADIUS_RANGE = 3; // -> 2..4
    private static final int GROUND_SEARCH = 6;

    public KravePeakFeature(Codec<NoneFeatureConfiguration> codec) {
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

        int height = MIN_HEIGHT + random.nextInt(HEIGHT_RANGE);
        int baseRadius = MIN_BASE_RADIUS + random.nextInt(BASE_RADIUS_RANGE);
        double[][] lobes = KraveTerrainShape.randomLobes(random, 1, 1, 0.12, 0.22);

        BlockState dirt = ModBlocks.KRAVE_DIRT.get().defaultBlockState();
        BlockState grass = ModBlocks.KRAVE_GRASS.get().defaultBlockState();

        int maxRadius = (int) Math.ceil(baseRadius * 1.4) + 1;

        for (int dx = -maxRadius; dx <= maxRadius; dx++) {
            for (int dz = -maxRadius; dz <= maxRadius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                double mult = KraveTerrainShape.lobeMultiplier(Math.atan2(dz, dx), lobes);
                double maxYf = height * (1.0 - dist / (baseRadius * mult));
                int columnHeight = (int) Math.min(height, Math.round(maxYf));
                if (columnHeight <= 0) {
                    continue;
                }

                BlockPos.MutableBlockPos ground = origin.offset(dx, GROUND_SEARCH, dz).mutable();
                int minY = origin.getY() - GROUND_SEARCH;
                while (ground.getY() > minY && !level.getBlockState(ground).isSolid()) {
                    ground.move(0, -1, 0);
                }
                if (!level.getBlockState(ground).isSolid()) {
                    continue;
                }

                BlockPos top = null;
                for (int y = 0; y < columnHeight; y++) {
                    top = ground.above(1 + y);
                    level.setBlock(top, dirt, 3);
                }
                level.setBlock(top, grass, 3);
            }
        }
        return true;
    }
}
