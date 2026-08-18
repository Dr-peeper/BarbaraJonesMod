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
 * Carves a small cave pocket inside a big island's interior and leaves a
 * BARRIER block as an inert "spawn site" marker on its floor - never
 * legitimately placed anywhere else in this mod, so it's an unambiguous
 * signal. KraveKosmosAmbience scans for these at ordinary tick time and
 * replaces them with a real KraveHealingBox spawn; this Feature itself only
 * ever touches blocks, deliberately staying out of the
 * entity-spawning-from-worldgen risk zone. No cave carvers exist for this
 * dimension (End-style noise has no carver plumbing), so this is the only
 * way to get underground pockets at all.
 */
public class KraveCavePocketFeature extends Feature<NoneFeatureConfiguration> {

    private static final int RADIUS = 3;

    public KraveCavePocketFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();
        RandomSource random = ctx.random();

        if (!level.getBlockState(origin).isSolid()) {
            return false;   // needs to start inside solid rock, not open air/void
        }

        BlockPos center = origin.below(6 + random.nextInt(7));
        if (!level.getBlockState(center).isSolid()) {
            return false;
        }

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState floor = ModBlocks.KRAVE_DIRT.get().defaultBlockState();

        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    if (Math.sqrt(dx * dx + dy * dy + dz * dz) > RADIUS) {
                        continue;
                    }
                    BlockPos pos = center.offset(dx, dy, dz);
                    // force a solid floor layer regardless of what the sphere
                    // carve would otherwise do there, so the marker (and later
                    // the healing box) always has real ground under it
                    level.setBlock(pos, dy == -RADIUS ? floor : air, 3);
                }
            }
        }

        BlockPos marker = center.offset(0, -RADIUS + 1, 0);
        level.setBlock(marker, Blocks.BARRIER.defaultBlockState(), 3);
        return true;
    }
}
