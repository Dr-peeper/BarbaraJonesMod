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
 * A carved-down crevasse - the inverse of KraveMountainFeature, made rarer
 * and larger (fewer, bigger crevasses rather than lots of small round
 * bowls), with the same organic-lobe cross-section plus a dedicated
 * elongation term along a random axis so it reads as a crack in the terrain
 * rather than a pit. Depth is capped and validated against how much solid
 * ground actually exists below the surface first, so this can never carve
 * through a thin island into the void underneath.
 */
public class KraveValleyFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MAX_DEPTH = 14;
    private static final int MIN_DEPTH = 6;
    private static final int MIN_BASE_RADIUS = 8;
    private static final int BASE_RADIUS_RANGE = 7; // -> 8..14
    private static final int MIN_SOLID_BELOW = 7;
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
        double[][] lobes = crevasseLobes(random);
        // Same write-range fit the mountains needed. A valley carries an extra
        // elongation lobe on top of its jaggedness, so its worst-case radius
        // ran even further past the legal 16 than theirs did; it only escaped
        // the error log because valleys are rarer and need deep solid ground
        // under them, not because it was safe.
        int baseRadius = KraveTerrainShape.fitBaseRadius(
                MIN_BASE_RADIUS + random.nextInt(BASE_RADIUS_RANGE), lobes);

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState floorBlock = ModBlocks.KRAVE_DIRT.get().defaultBlockState();

        for (int y = 0; y < depth; y++) {
            double layerBase = radiusAt(y, depth, baseRadius);
            boolean isFloor = y == depth - 1;
            int scanR = Math.min(KraveTerrainShape.MAX_WRITE_OFFSET,
                    (int) Math.ceil(layerBase * 1.8) + 1);
            for (int dx = -scanR; dx <= scanR; dx++) {
                for (int dz = -scanR; dz <= scanR; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    double effectiveR = layerBase * KraveTerrainShape.lobeMultiplier(Math.atan2(dz, dx), lobes);
                    if (dist > effectiveR) {
                        continue;
                    }
                    BlockPos pos = origin.offset(dx, -y, dz);
                    level.setBlock(pos, isFloor ? floorBlock : air, 3);
                }
            }
        }

        // A hidden healing box at the bottom of the pit - reuses the same
        // BARRIER marker convention as every other hidden spot in the
        // Kosmos; KraveKosmosAmbience.scanForCaveMarkers picks it up
        // generically. The floor center is guaranteed solid (isFloor above).
        if (random.nextInt(10) < 4) {
            level.setBlock(origin.below(depth - 1), Blocks.BARRIER.defaultBlockState(), 3);
        }
        return true;
    }

    /** General jaggedness lobes plus one dedicated elongation term along a random axis. */
    private double[][] crevasseLobes(RandomSource random) {
        double[][] jagged = KraveTerrainShape.randomLobes(random, 2, 3, 0.12, 0.25);
        double axisAngle = random.nextDouble() * Math.PI * 2.0;
        double elongation = 0.35 + random.nextDouble() * 0.4;
        double[][] lobes = new double[jagged.length + 1][3];
        System.arraycopy(jagged, 0, lobes, 0, jagged.length);
        lobes[jagged.length] = new double[] { -2.0 * axisAngle, 2, elongation };
        return lobes;
    }

    /** Linear taper from baseRadius at the rim down to ~40% of that at the floor. */
    private double radiusAt(int y, int depth, int baseRadius) {
        double t = (double) y / (double) depth;
        return Math.max(1.0, baseRadius * (1.0 - t * 0.6));
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
