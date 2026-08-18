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

import java.util.ArrayList;
import java.util.List;

/**
 * A large, rare mound built up from the island surface. Column-major: for
 * every (dx, dz) in the footprint, it finds THAT column's own local ground
 * height (searching a modest range around the placement origin) and grows
 * the mound up from there, instead of filling flat disc layers at a height
 * shared by the whole footprint. The underlying island terrain is not flat
 * (it's the reused vanilla End "cheese" shape), so a shared-height version
 * left gaps under the mound's edges wherever the natural ground dipped below
 * the origin's height - this is what actually fixes that "floating" look.
 *
 * <p>Cross-section is organic rather than circular via
 * {@link KraveTerrainShape} - a few random cosine "lobes" perturbing the
 * radius by angle. Rocky outcrops and the optional chocolate waterfall
 * spring are anchored to actual placed surface blocks collected during the
 * main pass, not independently recomputed coordinates - that mismatch was
 * why waterfalls kept ending up in empty air instead of on the mound.
 */
public class KraveMountainFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_HEIGHT = 28;
    private static final int HEIGHT_RANGE = 28;   // -> 28..55
    private static final int MIN_BASE_RADIUS = 10;
    private static final int BASE_RADIUS_RANGE = 7; // -> 10..16
    private static final int GROUND_SEARCH = 14;

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

        int maxRadius = (int) Math.ceil(baseRadius * 1.6) + 1;
        List<BlockPos> upperSurface = new ArrayList<>();

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
                    continue;   // no nearby ground under this column - leave a natural gap
                }

                BlockPos top = null;
                for (int y = 0; y < columnHeight; y++) {
                    top = ground.above(1 + y);
                    level.setBlock(top, dirt, 3);
                }
                level.setBlock(top, grass, 3);
                if (columnHeight >= height * 0.55) {
                    upperSurface.add(top);
                }
            }
        }

        if (!upperSurface.isEmpty()) {
            int outcrops = Math.min(upperSurface.size(), 5 + random.nextInt(6));
            for (int i = 0; i < outcrops; i++) {
                BlockPos anchor = upperSurface.get(random.nextInt(upperSurface.size()));
                level.setBlock(anchor.above(), frame, 3);
            }
            if (random.nextBoolean()) {
                BlockPos spring = upperSurface.get(random.nextInt(upperSurface.size()));
                level.setBlock(spring.above(), ModBlocks.CHOCOLATE_BLOCK.get().defaultBlockState(), 3);
            }
        }

        return true;
    }
}
