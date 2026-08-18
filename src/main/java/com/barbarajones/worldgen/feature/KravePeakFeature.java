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
 * KraveMountainFeature (which is now deliberately large and rare). Keeps
 * the islands feeling scattered with texture even between the big
 * mountains, without the cost of a full mound (no rocky outcrops, no
 * waterfall, a single mild lobe rather than several).
 */
public class KravePeakFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_HEIGHT = 8;
    private static final int HEIGHT_RANGE = 9;    // -> 8..16
    private static final int MIN_BASE_RADIUS = 2;
    private static final int BASE_RADIUS_RANGE = 3; // -> 2..4

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

        for (int y = 0; y < height; y++) {
            double layerBase = Math.max(1.0, baseRadius * (1.0 - (double) y / height));
            int scanR = (int) Math.ceil(layerBase * 1.4) + 1;
            for (int dx = -scanR; dx <= scanR; dx++) {
                for (int dz = -scanR; dz <= scanR; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    double effectiveR = layerBase * KraveTerrainShape.lobeMultiplier(Math.atan2(dz, dx), lobes);
                    if (dist > effectiveR) {
                        continue;
                    }
                    level.setBlock(origin.offset(dx, y, dz), dirt, 3);
                }
            }
        }

        int capScan = (int) Math.ceil(baseRadius * 1.4) + 1;
        for (int dx = -capScan; dx <= capScan; dx++) {
            for (int dz = -capScan; dz <= capScan; dz++) {
                BlockPos.MutableBlockPos scan = origin.offset(dx, height, dz).mutable();
                while (scan.getY() > origin.getY() && level.getBlockState(scan).isAir()) {
                    scan.move(0, -1, 0);
                }
                if (level.getBlockState(scan).is(ModBlocks.KRAVE_DIRT.get())) {
                    level.setBlock(scan, grass, 3);
                }
            }
        }
        return true;
    }
}
