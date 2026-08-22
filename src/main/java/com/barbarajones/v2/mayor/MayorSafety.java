package com.barbarajones.v2.mayor;

import com.barbarajones.dimension.KraveArena;
import com.barbarajones.dimension.KraveDimensions;
import com.barbarajones.dimension.KraveKosmosData;
import com.barbarajones.v2.build.place.TerrainRules;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * The list of places Barbara is not allowed to build, and the only place that
 * list lives.
 *
 * <h2>What this class does NOT do, on purpose</h2>
 * It does not re-check player-built blocks. That protection already exists and
 * is already wired: {@code KraveStructure.check} runs
 * {@link TerrainRules#verdict} over every block in the footprint, and that
 * consults {@code PlayerBuiltLedger} before it consults anything else. The mayor
 * honours it by treating any failed check as "this site is unusable, try the
 * next candidate" rather than by reporting an error - see
 * {@code SiteSelector#pick}. Adding a second ledger sweep here would read like a
 * belt-and-braces guard and would in fact be dead code that can never fire,
 * which is worse than no guard at all because it stops anybody looking for the
 * real one.
 *
 * <h2>What it does do</h2>
 * The four things the placement engine has no way to know about:
 *
 * <ol>
 *   <li><b>The boss arena.</b> Everything that happens in the Kraved Castle
 *       courtyard destroys terrain - the Monster carves discs two dozen blocks
 *       across, {@code KraveKosmosBattle} drops meteors at explosion power ten
 *       with block damage on. A settlement inside that is a settlement that gets
 *       demolished, so the whole island is off limits by radius.
 *   <li><b>The castle itself.</b> Read from the bounds
 *       {@code KraveKosmosData} recorded when the den was placed, not from
 *       constants, so the protected volume cannot drift away from the building.
 *       {@link KraveArena#isProtected} is called first because that is the code
 *       path which recovers those bounds for worlds whose den predates the
 *       recording.
 *   <li><b>The void.</b> A candidate needs solid, non-liquid ground under it and
 *       has to sit clear of both build limits with room for its own height.
 *   <li><b>Unloaded chunks.</b> Nothing is sited into a chunk that is not
 *       already loaded, so the mayor can never trigger world generation.
 * </ol>
 */
public final class MayorSafety {

    /**
     * Horizontal exclusion around the Kosmos boss island, in blocks.
     *
     * <p>Deliberately much larger than the castle: the fight's blast radius is
     * what matters, not the building's footprint, and the meteors do not aim.
     */
    public static final int ARENA_RADIUS = 96;

    /** Clearance demanded from both build limits. */
    private static final int HEIGHT_MARGIN = 6;

    private MayorSafety() { }

    /**
     * The whole verdict for one candidate footprint.
     *
     * @param box the full volume the placement will read or write, i.e.
     *            {@code PlacementCheck#worldBounds}
     */
    public static boolean siteAllowed(ServerLevel level, BoundingBox box) {
        if (!withinBuildLimits(level, box)) {
            return false;
        }
        if (!chunksLoaded(level, box)) {
            return false;
        }
        return !insideProtectedArena(level, box);
    }

    private static boolean withinBuildLimits(ServerLevel level, BoundingBox box) {
        return box.minY() >= level.getMinBuildHeight() + HEIGHT_MARGIN
                && box.maxY() < level.getMaxBuildHeight() - HEIGHT_MARGIN;
    }

    /**
     * Every corner chunk of the footprint must already be loaded.
     *
     * <p>Corners rather than every chunk: a footprint is at most sixty-four
     * blocks across and the four corners bound it, so a gap in the middle is not
     * reachable without one of the corners also being outside the loaded region.
     */
    private static boolean chunksLoaded(ServerLevel level, BoundingBox box) {
        return level.hasChunkAt(box.minX(), box.minZ())
                && level.hasChunkAt(box.maxX(), box.minZ())
                && level.hasChunkAt(box.minX(), box.maxZ())
                && level.hasChunkAt(box.maxX(), box.maxZ());
    }

    /**
     * True if any part of this footprint is in the boss arena or on the castle.
     *
     * <p>Only ever true in the Krave Kosmos, which is the only dimension either
     * of those exists in - and the {@code KraveKosmosData} lookup is behind that
     * dimension test on purpose, because asking for it elsewhere would write an
     * empty Kosmos save file into the Overworld's data folder.
     */
    private static boolean insideProtectedArena(ServerLevel level, BoundingBox box) {
        if (!level.dimension().equals(KraveDimensions.KRAVE_KOSMOS)) {
            return false;
        }

        Vec3i centre = box.getCenter();
        double islandX = KraveDimensions.BOSS_ISLAND.x;
        double islandZ = KraveDimensions.BOSS_ISLAND.z;
        int reach = ARENA_RADIUS + Math.max(box.getXSpan(), box.getZSpan());
        double dx = centre.getX() - islandX;
        double dz = centre.getZ() - islandZ;
        if (dx * dx + dz * dz <= (double) reach * reach) {
            return true;
        }

        // Asking the arena about any position in the Kosmos is what recovers the
        // recorded castle bounds for a world whose den was built before they
        // started being recorded - the recovery lives behind this call, not
        // behind the position. Only then is it worth reading the box.
        KraveArena.isProtected(level, BlockPos.containing(KraveDimensions.BOSS_ISLAND));
        BoundingBox castle = KraveKosmosData.get(level).getCastleBounds();
        return castle != null && castle.intersects(box);
    }

    /**
     * True if this column is something a building can stand on: solid, not a
     * fluid, and not thin air over a chasm.
     *
     * <p>Cheap, and meant to be run before the expensive placement check so that
     * obviously hopeless candidates never reach it.
     */
    public static boolean groundIsSolid(ServerLevel level, BlockPos ground) {
        if (!level.hasChunkAt(ground)) {
            return false;
        }
        if (ground.getY() <= level.getMinBuildHeight() + HEIGHT_MARGIN
                || ground.getY() >= level.getMaxBuildHeight() - HEIGHT_MARGIN) {
            return false;
        }
        BlockState below = level.getBlockState(ground.below());
        if (below.isAir() || below.is(Blocks.WATER) || below.is(Blocks.LAVA)) {
            return false;
        }
        return TerrainRules.isGround(level, ground.below(), below);
    }
}
