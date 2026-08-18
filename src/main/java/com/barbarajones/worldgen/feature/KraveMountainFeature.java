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
 * A large, rare mound built up from the island surface - deliberately fewer
 * and bigger than the original version, with an organic (non-circular)
 * cross-section instead of a perfectly round stepped cone, which is what
 * actually read as "artificial" in practice. The irregular silhouette comes
 * from summing a few random cosine "lobes" against the angle around the
 * mountain's center - a classic way to turn a circle into a natural-looking
 * rocky outline without needing real noise infrastructure. Built as an
 * imperative block-placement pass (like KraveRuinFeature/KraveDenBuilder)
 * rather than by editing the dimension's density function - see
 * KraveValleyFeature's javadoc for why.
 *
 * <p>All per-placement randomness (lobe phase/frequency/amplitude) lives in
 * local variables inside {@link #place}, never on instance fields - Feature
 * instances are shared/reused across concurrent chunk decoration, so any
 * per-placement state has to stay on the call stack.
 */
public class KraveMountainFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_HEIGHT = 28;
    private static final int HEIGHT_RANGE = 28;   // -> 28..55
    private static final int MIN_BASE_RADIUS = 10;
    private static final int BASE_RADIUS_RANGE = 7; // -> 10..16

    public KraveMountainFeature(Codec<NoneFeatureConfiguration> codec) {
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
        double[][] lobes = KraveTerrainShape.randomLobes(random, 2, 3, 0.18, 0.38);

        BlockState dirt = ModBlocks.KRAVE_DIRT.get().defaultBlockState();
        BlockState grass = ModBlocks.KRAVE_GRASS.get().defaultBlockState();
        BlockState frame = ModBlocks.KRAVE_BLOCK.get().defaultBlockState();

        for (int y = 0; y < height; y++) {
            int layerBase = radiusAt(y, height, baseRadius);
            int scanR = (int) Math.ceil(layerBase * 1.6) + 1;
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

        // Cap whatever ends up exposed on top of each column with grass -
        // cheaper and more robust than tracking the organic surface analytically.
        int capScan = (int) Math.ceil(baseRadius * 1.6) + 1;
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

        // A handful of rocky outcrops near the peak for texture.
        int outcrops = 5 + random.nextInt(6);
        for (int i = 0; i < outcrops; i++) {
            int y = (int) (height * (0.55 + random.nextDouble() * 0.4));
            double angle = random.nextDouble() * Math.PI * 2.0;
            int layerBase = Math.max(1, radiusAt(y, height, baseRadius));
            double r = layerBase * KraveTerrainShape.lobeMultiplier(angle, lobes);
            int dx = (int) Math.round(Math.cos(angle) * r);
            int dz = (int) Math.round(Math.sin(angle) * r);
            level.setBlock(origin.offset(dx, y, dz), frame, 3);
        }

        if (random.nextBoolean()) {
            placeWaterfallSpring(level, origin, random, height, baseRadius, lobes);
        }
        return true;
    }

    /** Linear taper from baseRadius at y=0 down to 1 at the peak. */
    private int radiusAt(int y, int height, int baseRadius) {
        double t = (double) y / (double) height;
        return Math.max(1, (int) Math.round(baseRadius * (1.0 - t)));
    }

    private void placeWaterfallSpring(WorldGenLevel level, BlockPos origin, RandomSource random,
                                      int height, int baseRadius, double[][] lobes) {
        int y = (int) (height * (0.6 + random.nextDouble() * 0.25));
        double angle = random.nextDouble() * Math.PI * 2.0;
        int layerBase = Math.max(2, radiusAt(y, height, baseRadius));
        double r = layerBase * KraveTerrainShape.lobeMultiplier(angle, lobes);
        int dx = (int) Math.round(Math.cos(angle) * r);
        int dz = (int) Math.round(Math.sin(angle) * r);
        BlockPos springPos = origin.offset(dx, y, dz);
        level.setBlock(springPos, ModBlocks.CHOCOLATE_BLOCK.get().defaultBlockState(), 3);
    }
}
