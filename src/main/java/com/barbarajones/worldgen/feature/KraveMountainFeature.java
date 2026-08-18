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
 * A randomly-sized mound built up from the island surface - the terrain
 * variation the flat sloped-cheese island shape doesn't have on its own.
 * Deliberately built as an imperative block-placement pass (like
 * KraveRuinFeature/KraveDenBuilder) rather than by editing the dimension's
 * density function - that file already caused one launch-blocking crash from
 * a single wrong codec field, and this way the shape is ordinary compiled
 * Java instead of more unverifiable JSON.
 *
 * <p>About half the time, a single Krave Chocolate source block is placed on
 * the mound's upper flank, open to air on its outer/upper side - the fluid's
 * own flow behavior (already shipped) does the rest, cascading down the
 * slope exactly like a vanilla lava/water spring off a cliff.
 */
public class KraveMountainFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_HEIGHT = 12;
    private static final int HEIGHT_RANGE = 17;   // -> 12..28
    private static final int MIN_BASE_RADIUS = 5;
    private static final int BASE_RADIUS_RANGE = 4; // -> 5..8

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

        BlockState dirt = ModBlocks.KRAVE_DIRT.get().defaultBlockState();
        BlockState grass = ModBlocks.KRAVE_GRASS.get().defaultBlockState();
        BlockState frame = ModBlocks.KRAVE_BLOCK.get().defaultBlockState();

        // Solid tapering cone, dirt body.
        for (int y = 0; y < height; y++) {
            int radius = radiusAt(y, height, baseRadius);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dz * dz > radius * radius) {
                        continue;
                    }
                    level.setBlock(origin.offset(dx, y, dz), dirt, 3);
                }
            }
        }

        // Cap whatever ends up exposed on top of each column with grass -
        // cheaper and more robust than tracking the cone's surface analytically.
        int maxRadius = baseRadius;
        for (int dx = -maxRadius; dx <= maxRadius; dx++) {
            for (int dz = -maxRadius; dz <= maxRadius; dz++) {
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
        int outcrops = 4 + random.nextInt(5);
        for (int i = 0; i < outcrops; i++) {
            int y = (int) (height * (0.55 + random.nextDouble() * 0.4));
            int radius = Math.max(1, radiusAt(y, height, baseRadius));
            double angle = random.nextDouble() * Math.PI * 2.0;
            int dx = (int) Math.round(Math.cos(angle) * radius);
            int dz = (int) Math.round(Math.sin(angle) * radius);
            level.setBlock(origin.offset(dx, y, dz), frame, 3);
        }

        if (random.nextBoolean()) {
            placeWaterfallSpring(level, origin, random, height, baseRadius);
        }
        return true;
    }

    /** Linear taper from baseRadius at y=0 down to 1 at the peak. */
    private int radiusAt(int y, int height, int baseRadius) {
        double t = (double) y / (double) height;
        return Math.max(1, (int) Math.round(baseRadius * (1.0 - t)));
    }

    private void placeWaterfallSpring(WorldGenLevel level, BlockPos origin, RandomSource random,
                                      int height, int baseRadius) {
        int y = (int) (height * (0.6 + random.nextDouble() * 0.25));
        int radius = Math.max(2, radiusAt(y, height, baseRadius));
        double angle = random.nextDouble() * Math.PI * 2.0;
        int dx = (int) Math.round(Math.cos(angle) * radius);
        int dz = (int) Math.round(Math.sin(angle) * radius);
        BlockPos springPos = origin.offset(dx, y, dz);
        level.setBlock(springPos, ModBlocks.CHOCOLATE_BLOCK.get().defaultBlockState(), 3);
    }
}
