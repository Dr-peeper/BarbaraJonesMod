package com.barbarajones.worldgen.feature;

import com.barbarajones.content.ModBlocks;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * A real chocolate waterfall: chocolate poured down an actual cliff face and
 * pooled in a carved basin at the bottom, at a site chosen because the local
 * terrain is genuinely dramatic there - not a single spring block dropped at
 * a random surface point and left to vanilla fluid physics.
 *
 * <p>Site selection ({@link #findDramaticCliff}) samples a grid of points
 * around the placement origin and, for each, measures how sharply the ground
 * actually drops nearby versus further out in the same direction: a real
 * cliff shows most of its total drop within the first couple of blocks past
 * the edge, where a gentle slope spreads it out evenly. Only the single best
 * (steepest, tallest) candidate found is used - this feature either builds
 * one convincing waterfall or, on a chunk with nothing dramatic enough,
 * builds nothing at all. It is placed in the LOCAL_MODIFICATIONS step after
 * krave_mountain/krave_valley/krave_peak (see the biome json), so it is
 * reading terrain those features already carved, which is exactly where the
 * real cliffs in this dimension are.
 *
 * <p>Reliability lesson carried over from KraveMountainFeature's own
 * documented waterfall history (three "fix the waterfall" commits before the
 * real bug - a silent skip when the candidate zone came up empty - was
 * found): this feature does not place one source block and hope vanilla
 * fluid propagation, worldgen tick timing and levelDecreasePerBlock=2 sort
 * out something that reads as a waterfall on its own. The whole visible
 * shape - the lip, the cascade down the face, and the pool at the base - is
 * placed directly as chocolate source blocks, so a freshly generated chunk
 * shows a finished waterfall immediately, with vanilla fluid physics only
 * responsible for any further spread at the edges.
 */
public class KraveWaterfallFeature extends Feature<NoneFeatureConfiguration> {

    // The search radius is a write-range budget, not a taste call. Everything
    // this feature places is measured from the cliff it picks, so the furthest
    // block it can touch is SEARCH_RADIUS + one strand offset + one fall
    // offset + the basin radius. That has to stay inside
    // KraveTerrainShape.MAX_WRITE_OFFSET (15) or the far side of a cascade is
    // silently dropped during generation: 8 + 1 + 1 + 5 = 15 exactly.
    //
    // It used to search 22 out, which could put a whole waterfall up to 29
    // blocks from origin - roughly half of one would survive. Sampling is
    // twice as dense now to claw back some of the candidate columns the
    // smaller radius costs; mountains also each place a guaranteed spring of
    // their own (see KraveMountainFeature), so this feature was never the
    // only source of them.
    private static final int SEARCH_RADIUS = 8;
    private static final int SAMPLE_STEP = 2;
    private static final int EDGE_LOOKAHEAD = 2;
    private static final int FAR_LOOKAHEAD = 6;
    private static final int MIN_DROP = 13;
    private static final int WIDTH_HALF = 1;          // -> up to 3 strands wide
    private static final int MAX_FACE_SCAN = 60;       // safety cap filling the cascade column
    private static final int[][] DIRS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

    public KraveWaterfallFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        BlockPos origin = ctx.origin();

        Site site = findDramaticCliff(level, origin);
        if (site == null) {
            return false;   // nothing dramatic enough nearby - a quiet, deliberate no-op
        }
        buildWaterfall(level, site);
        return true;
    }

    /** One candidate cliff: the lip position, the direction the ground falls away, and how far it drops. */
    private record Site(int topX, int topY, int topZ, int dirX, int dirZ, int drop) { }

    private Site findDramaticCliff(WorldGenLevel level, BlockPos origin) {
        Site best = null;
        for (int gx = -SEARCH_RADIUS; gx <= SEARCH_RADIUS; gx += SAMPLE_STEP) {
            for (int gz = -SEARCH_RADIUS; gz <= SEARCH_RADIUS; gz += SAMPLE_STEP) {
                int x = origin.getX() + gx;
                int z = origin.getZ() + gz;
                int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                if (!level.getBlockState(new BlockPos(x, topY - 1, z)).isSolid()) {
                    continue;   // no floor to stand the lip on
                }

                for (int[] dir : DIRS) {
                    int nearY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,
                            x + dir[0] * EDGE_LOOKAHEAD, z + dir[1] * EDGE_LOOKAHEAD);
                    int farX = x + dir[0] * FAR_LOOKAHEAD;
                    int farZ = z + dir[1] * FAR_LOOKAHEAD;
                    int farY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, farX, farZ);

                    int drop = topY - farY;
                    int near = topY - nearY;
                    if (drop < MIN_DROP || near < drop * 0.5D) {
                        continue;   // not steep, or not tall enough to be dramatic
                    }
                    // there has to be an actual floor to land on, not a bottomless gap
                    if (!level.getBlockState(new BlockPos(farX, farY - 1, farZ)).isSolid()
                            && level.getFluidState(new BlockPos(farX, farY - 1, farZ)).isEmpty()) {
                        continue;
                    }

                    if (best == null || drop > best.drop()) {
                        best = new Site(x, topY, z, dir[0], dir[1], drop);
                    }
                }
            }
        }
        return best;
    }

    private void buildWaterfall(WorldGenLevel level, Site site) {
        BlockState chocolate = ModBlocks.CHOCOLATE_BLOCK.get().defaultBlockState();
        int perpX = -site.dirZ();
        int perpZ = site.dirX();

        List<BlockPos> basins = new ArrayList<>();
        for (int w = -WIDTH_HALF; w <= WIDTH_HALF; w++) {
            int colTopX = site.topX() + perpX * w;
            int colTopZ = site.topZ() + perpZ * w;
            int colTopY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, colTopX, colTopZ);
            if (Math.abs(colTopY - site.topY()) > 3) {
                continue;   // this strand has drifted off the same lip - skip it, don't gouge fresh terrain
            }
            BlockPos colGround = new BlockPos(colTopX, colTopY - 1, colTopZ);
            if (!level.getBlockState(colGround).isSolid()) {
                continue;
            }

            // the lip: a source block sitting right on the edge
            level.setBlock(new BlockPos(colTopX, colTopY, colTopZ), chocolate, 3);

            int fallX = colTopX + site.dirX();
            int fallZ = colTopZ + site.dirZ();
            int fallBaseY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, fallX, fallZ);
            if (colTopY - fallBaseY < MIN_DROP / 2) {
                continue;   // this strand isn't actually over the drop
            }

            int filled = 0;
            for (int y = colTopY - 1; y > fallBaseY && filled < MAX_FACE_SCAN; y--) {
                BlockPos face = new BlockPos(fallX, y, fallZ);
                if (level.getBlockState(face).isAir()) {
                    level.setBlock(face, chocolate, 3);
                    filled++;
                } else if (!level.getFluidState(face).isEmpty()) {
                    break;      // already met another liquid on the way down - stop, don't overwrite it
                } else {
                    break;      // a solid ledge mid-face - the cascade already placed reads fine stopping here
                }
            }
            if (filled > 0) {
                basins.add(new BlockPos(fallX, fallBaseY, fallZ));
            }
        }

        for (BlockPos basin : dedupe(basins)) {
            buildBasin(level, basin, site.drop());
        }
    }

    /** Two strands landing on the same column happen often enough (width 3, narrow cliffs) to be worth skipping twice. */
    private List<BlockPos> dedupe(List<BlockPos> basins) {
        List<BlockPos> unique = new ArrayList<>();
        for (BlockPos p : basins) {
            if (!unique.contains(p)) {
                unique.add(p);
            }
        }
        return unique;
    }

    /** A shallow round pool carved into the landing spot, deeper toward the center - real, not a hopeful puddle. */
    private void buildBasin(WorldGenLevel level, BlockPos center, int dropHeight) {
        BlockState chocolate = ModBlocks.CHOCOLATE_BLOCK.get().defaultBlockState();
        int radius = Math.max(2, Math.min(5, 2 + dropHeight / 12));

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > radius) {
                    continue;
                }
                int bx = center.getX() + dx;
                int bz = center.getZ() + dz;
                int groundY = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, bx, bz);
                double t = 1.0D - dist / radius;
                int depth = 1 + (int) Math.round(t * 2.0D);   // 1..3, deepest at the center

                if (!level.getBlockState(new BlockPos(bx, groundY - depth - 1, bz)).isSolid()) {
                    continue;   // not enough ground here to carve into safely - leave it alone
                }
                for (int y = groundY - depth; y < groundY; y++) {
                    level.setBlock(new BlockPos(bx, y, bz), chocolate, 3);
                }
            }
        }
    }
}
