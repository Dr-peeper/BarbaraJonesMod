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
 * radius by angle. Rocky outcrops and the guaranteed chocolate waterfall
 * spring are anchored to actual placed surface blocks collected during the
 * main pass, not independently recomputed coordinates - that mismatch was
 * why waterfalls kept ending up in empty air instead of on the mound. A
 * second bug on top of that one: the "peak zone" collected for outcrops/the
 * spring required a column to reach 55% of the mountain's height, and on a
 * mountain where that zone came up empty (peak column's ground-search
 * failed, e.g.) BOTH outcrops and the spring were silently skipped
 * entirely - not rare, just quiet. origin's own column is now a guaranteed
 * fallback anchor, so every mountain that builds at all gets a spring.
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
        double[][] lobes = KraveTerrainShape.randomLobes(random, 2, 3, 0.18, 0.38);
        // Rolled, then fitted to what a feature is actually allowed to write
        // (see KraveTerrainShape.MAX_WRITE_OFFSET). Before this, a mountain
        // could reach 27 blocks out, past the legal 16, and every block beyond
        // that was silently thrown away by WorldGenRegion - so any mountain
        // that happened to sit near a chunk edge generated with slices of
        // itself simply absent. Height is untouched: vertical writes have no
        // such limit, so these are the same mountains, just narrower and
        // correspondingly steeper.
        int baseRadius = KraveTerrainShape.fitBaseRadius(
                MIN_BASE_RADIUS + random.nextInt(BASE_RADIUS_RANGE), lobes);

        BlockState dirt = ModBlocks.KRAVE_DIRT.get().defaultBlockState();
        BlockState grass = ModBlocks.KRAVE_GRASS.get().defaultBlockState();
        BlockState frame = ModBlocks.KRAVE_BLOCK.get().defaultBlockState();

        int maxRadius = Math.min(KraveTerrainShape.MAX_WRITE_OFFSET,
                (int) Math.ceil(baseRadius * 1.6) + 1);
        List<BlockPos> upperSurface = new ArrayList<>();
        // The origin's own column always exists if we got this far (the
        // isSolid() check above guarantees it), so it's a reliable fallback
        // anchor even on the rare mountain whose actual peak zone comes up
        // empty (peakRadius below missed every column, or every candidate's
        // ground-search failed) - that silent, total skip of BOTH outcrops
        // and the waterfall was the real bug behind "still not spawning"
        // reports across several earlier attempts at this.
        BlockPos originTop = null;

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
                if (dx == 0 && dz == 0) {
                    originTop = top;
                }
                if (columnHeight >= height * 0.55) {
                    upperSurface.add(top);
                }
            }
        }

        if (upperSurface.isEmpty() && originTop != null) {
            upperSurface.add(originTop);
        }

        if (!upperSurface.isEmpty()) {
            int outcrops = Math.min(upperSurface.size(), 5 + random.nextInt(6));
            for (int i = 0; i < outcrops; i++) {
                BlockPos anchor = upperSurface.get(random.nextInt(upperSurface.size()));
                level.setBlock(anchor.above(), frame, 3);
            }

            // Prefer points with an open horizontal neighbor (an actual ledge
            // the mound tapers away from) so the spring has somewhere to
            // cascade down into, rather than a random upper point that might
            // sit on a locally flat patch near the peak.
            List<BlockPos> edges = new ArrayList<>();
            for (BlockPos p : upperSurface) {
                if (!level.getBlockState(p.north()).isSolid() || !level.getBlockState(p.south()).isSolid()
                        || !level.getBlockState(p.east()).isSolid() || !level.getBlockState(p.west()).isSolid()) {
                    edges.add(p);
                }
            }
            List<BlockPos> springCandidates = edges.isEmpty() ? upperSurface : edges;

            // Guaranteed now, not a chance - repeated "still not spawning"
            // reports outweigh the earlier "not every mountain should have
            // one" note. Every mountain that reaches here (which upperSurface's
            // originTop fallback above now guarantees, not just the ones with
            // a big enough peak zone) gets at least one; a second is common
            // but not universal.
            BlockState chocolate = ModBlocks.CHOCOLATE_BLOCK.get().defaultBlockState();
            level.setBlock(springCandidates.get(random.nextInt(springCandidates.size())).above(), chocolate, 3);
            if (springCandidates.size() > 1 && random.nextInt(10) < 7) {
                level.setBlock(springCandidates.get(random.nextInt(springCandidates.size())).above(), chocolate, 3);
            }
        }

        // A hidden chamber inside the mountain's own body - "caves on
        // mountains", distinct from the general island cave pockets
        // (KraveCavePocketFeature) and the surface ruins/den. Leaves a
        // BARRIER marker like every other hidden-healing-box spot in the
        // Kosmos; KraveKosmosAmbience.scanForCaveMarkers picks it up
        // generically regardless of which Feature placed it.
        if (random.nextInt(10) < 4) {
            carveMountainCave(level, origin, random, height);
        }

        return true;
    }

    private void carveMountainCave(WorldGenLevel level, BlockPos origin, RandomSource random, int height) {
        int dx = random.nextInt(5) - 2;
        int dz = random.nextInt(5) - 2;
        BlockPos.MutableBlockPos ground = origin.offset(dx, GROUND_SEARCH, dz).mutable();
        int minY = origin.getY() - GROUND_SEARCH;
        while (ground.getY() > minY && !level.getBlockState(ground).isSolid()) {
            ground.move(0, -1, 0);
        }
        if (!level.getBlockState(ground).isSolid()) {
            return;
        }

        int chamberHeight = Math.max(3, (int) (height * (0.25 + random.nextDouble() * 0.25)));
        BlockPos floor = ground.above(chamberHeight);
        if (!level.getBlockState(floor).isSolid() || !level.getBlockState(floor.above(2)).isSolid()) {
            return;   // not solidly inside the mound's body here - skip, safe no-op
        }

        for (int cx = -1; cx <= 1; cx++) {
            for (int cz = -1; cz <= 1; cz++) {
                for (int cy = 0; cy <= 2; cy++) {
                    level.setBlock(floor.offset(cx, cy, cz), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(floor, Blocks.BARRIER.defaultBlockState(), 3);
    }
}
