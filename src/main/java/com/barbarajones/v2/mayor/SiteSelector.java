package com.barbarajones.v2.mayor;

import com.barbarajones.v2.build.def.StructureDef;
import com.barbarajones.v2.build.place.KraveStructure;
import com.barbarajones.v2.build.place.PlacementCheck;
import com.barbarajones.v2.mayor.def.MayorPrefabs;
import com.barbarajones.v2.village.Village;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.Nullable;

/**
 * Where the mayor puts things.
 *
 * <h2>Buildings: a sunflower that tightens as she rises</h2>
 * Candidate centres come off a golden-angle spiral seeded from a cursor that is
 * persisted on the settlement, so successive projects march round the town
 * instead of re-rolling the same handful of spots. Radius grows with the square
 * root of the cursor, which is the standard even-area distribution - the town
 * fills outward from the middle rather than forming a ring.
 *
 * <p>The number that shapes the result is {@link MayorRank#buildingGap}, and it
 * <b>shrinks</b> as Barbara's rank climbs: eight blocks of clear ground between
 * buildings at Squatter, two at Kingpin. Two blocks is an alley. That single
 * descending number is what turns a scattering of huts into a warren, and it is
 * the mechanical half of the "small and run-down becomes huge and run-down"
 * brief - the other half is the palette, which never improves either.
 *
 * <p><b>The gap rule is an aesthetic control, not a safety guarantee.</b> What
 * actually stops two buildings occupying the same blocks is
 * {@code KraveStructure.check}, which refuses any footprint containing a
 * manufactured or player-placed block. Tightening the gap to zero would produce
 * a lot of rejected candidates, never an overlap. Say that out loud because the
 * two rules look interchangeable and are not.
 *
 * <h2>Roads: four spurs, extended shortest-first</h2>
 * The town has at most four roads, one per cardinal direction, and a Road
 * Expansion adds one segment to whichever is currently shortest. A spur stops
 * when its next segment would leave the rank's settlement radius, which is what
 * keeps roads from being an unbounded way to spend permits.
 */
public final class SiteSelector {

    /**
     * Clear ground kept round the origin so the village charter is not built
     * over. Eight blocks: the widest footprint in the module is eleven, so a
     * building centred on the inner edge of the ring still stops two and a half
     * blocks short of the origin.
     */
    public static final int MIN_PLAZA = 8;

    /**
     * How far out a road spur starts.
     *
     * <p>Closer in than the buildings, because a road that begins outside the
     * plaza is a road that does not reach the middle of the town. Roads and
     * buildings are allowed to meet: a building sited later may clip the edge of
     * a road, and that is on purpose - "structures encroaching" is part of what
     * the roads are for. The reverse cannot happen, because a road's placement
     * check sees the building's planks and cobble as manufactured and refuses.
     */
    private static final int ROAD_START = 4;

    /** Candidate positions considered in one attempt. */
    private static final int MAX_CANDIDATES = 16;

    /**
     * Full placement checks allowed per attempt.
     *
     * <p>A check reads every block in a footprint's column range - low thousands
     * for the tenement - so this is the real cost of an attempt and the reason
     * the cheap tests run first. The cursor persists, so an attempt that runs out
     * of budget simply carries on from where it stopped ten seconds later rather
     * than starting again.
     */
    private static final int MAX_CHECKS = 6;

    /** Golden angle in radians. Successive candidates never line up. */
    private static final double GOLDEN_ANGLE = 2.399963229728653D;

    /** How many candidates the radius sweep takes to reach the outer edge. */
    private static final int SPIRAL_PERIOD = 48;

    private SiteSelector() { }

    /**
     * A chosen, validated placement. Handed straight to
     * {@code KraveStructure.place} on the same tick, so nothing can change under
     * it between the check and the build.
     *
     * <p>The definition travels <em>in</em> the site rather than being worked out
     * again by the caller. A road's definition depends on the rank and on how
     * many segments that spur already has, and the caller's next act is to
     * increment exactly that counter - so recomputing it there would be two
     * pieces of code deriving one value, with the second one deriving it from
     * state the first had already changed.
     *
     * @param roadDirection index into the settlement's road spurs, or -1 for a
     *                      building
     */
    public record Site(StructureDef def, BlockPos anchor, Rotation rotation,
                       PlacementCheck check, int roadDirection) {

        public BoundingBox bounds() {
            return this.check.worldBounds();
        }

        public BlockPos centre() {
            BoundingBox box = bounds();
            return new BlockPos(box.minX() + box.getXSpan() / 2, this.check.baseY(),
                    box.minZ() + box.getZSpan() / 2);
        }
    }

    // =====================================================================

    /**
     * Finds somewhere to put a building, or returns null if nothing in this
     * attempt's budget worked out.
     *
     * <p>Null is a normal answer, not an error: the town may genuinely be full at
     * this rank, and the caller reports it as such and tries again next tick.
     */
    @Nullable
    public static Site pickBuilding(ServerLevel level, Village village, MayorSettlement settlement,
                                    StructureDef def) {
        MayorRank rank = settlement.rank();
        int radius = rank.settlementRadius();
        int footprint = Math.max(def.spanX(), def.spanZ());
        int checksLeft = MAX_CHECKS;

        for (int attempt = 0; attempt < MAX_CANDIDATES && checksLeft > 0; attempt++) {
            int cursor = settlement.nextSiteCursor();
            BlockPos probe = spiralPoint(village.origin(), cursor, radius);

            if (!level.hasChunkAt(probe)) {
                continue;
            }
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe);
            if (!MayorSafety.groundIsSolid(level, ground)) {
                continue;
            }
            if (!village.contains(ground)) {
                continue;
            }
            if (tooCloseToNeighbour(settlement, ground, rank.buildingGap(), footprint)) {
                continue;
            }

            // Doors face the middle of town, so a building put up on the far
            // side of the settlement still fronts onto it.
            Direction front = towards(village.origin().getX() - ground.getX(),
                    village.origin().getZ() - ground.getZ());
            Rotation rotation = rotationForFront(front);

            checksLeft--;
            PlacementCheck check = KraveStructure.check(level, ground, rotation, def);
            if (!check.ok()) {
                continue;
            }
            if (!MayorSafety.siteAllowed(level, check.worldBounds())) {
                continue;
            }
            if (!footprintInsideClaim(village, check)) {
                continue;
            }
            return new Site(def, ground, rotation, check, -1);
        }
        return null;
    }

    /**
     * Finds the next segment of road: one step further along whichever of the
     * four spurs is currently shortest and still has room inside the rank's
     * settlement radius.
     *
     * <p>Which definition gets laid is {@link MayorRank#roadStage}'s business,
     * not this method's - the site is chosen the same way whatever state the
     * road is in.
     */
    @Nullable
    public static Site pickRoad(ServerLevel level, Village village, MayorSettlement settlement) {
        MayorRank rank = settlement.rank();
        int radius = rank.settlementRadius();
        int segment = MayorPrefabs.ROAD_SEGMENT_LENGTH;

        // Shortest first, so the four roads grow together rather than one of
        // them running to the edge of the claim while the others are stubs.
        int[] order = spursByLength(settlement);
        for (int direction : order) {
            int laid = settlement.roadSpur(direction);
            if (ROAD_START + (laid + 1) * segment > radius) {
                continue;
            }
            int distance = ROAD_START + laid * segment + segment / 2;
            Direction heading = CARDINALS[direction];
            BlockPos probe = village.origin().offset(
                    heading.getStepX() * distance, 0, heading.getStepZ() * distance);
            if (!level.hasChunkAt(probe)) {
                continue;
            }
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, probe);
            if (!MayorSafety.groundIsSolid(level, ground) || !village.contains(ground)) {
                continue;
            }

            StructureDef def = MayorPrefabs.get(MayorPrefabs.road(rank.roadStage(),
                    laid + direction));
            if (def == null) {
                continue;
            }
            // A road runs along its own local +Z, so the rotation is the one
            // that points the definition's front down the spur.
            Rotation rotation = rotationForFront(heading);
            PlacementCheck check = KraveStructure.check(level, ground, rotation, def);
            if (!check.ok() || !MayorSafety.siteAllowed(level, check.worldBounds())
                    || !footprintInsideClaim(village, check)) {
                continue;
            }
            return new Site(def, ground, rotation, check, direction);
        }
        return null;
    }

    // =====================================================================

    private static final Direction[] CARDINALS = {
            Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private static BlockPos spiralPoint(BlockPos origin, int cursor, int radius) {
        double angle = cursor * GOLDEN_ANGLE;
        double fraction = (double) (cursor % SPIRAL_PERIOD) / SPIRAL_PERIOD;
        double distance = MIN_PLAZA + (radius - MIN_PLAZA) * Math.sqrt(fraction);
        int x = origin.getX() + (int) Math.round(Math.cos(angle) * distance);
        int z = origin.getZ() + (int) Math.round(Math.sin(angle) * distance);
        return new BlockPos(x, origin.getY(), z);
    }

    /**
     * The spacing rule. Compares centre to centre, allowing for the new
     * building's own half-width plus a fixed allowance for whatever is already
     * standing there - the settlement remembers centres, not footprints, so a
     * precise answer is not available and is not needed.
     */
    private static boolean tooCloseToNeighbour(MayorSettlement settlement, BlockPos candidate,
                                               int gap, int footprint) {
        int minimum = gap + footprint / 2 + 4;
        long minimumSq = (long) minimum * minimum;
        for (BlockPos site : settlement.sites()) {
            long dx = site.getX() - candidate.getX();
            long dz = site.getZ() - candidate.getZ();
            if (dx * dx + dz * dz < minimumSq) {
                return true;
            }
        }
        return false;
    }

    /**
     * All four corners of the footprint must be inside the village's claim.
     *
     * <p>This is the bounded-settlement guarantee: {@code Village#contains} tests
     * the claim radius and the claim height together, so a building cannot creep
     * out of the volume the village module is sweeping, and a hillside site far
     * above the origin is refused rather than producing a settlement that is
     * tracked at one end and invisible at the other.
     */
    private static boolean footprintInsideClaim(Village village, PlacementCheck check) {
        BoundingBox box = check.worldBounds();
        int y = check.baseY();
        return village.contains(new BlockPos(box.minX(), y, box.minZ()))
                && village.contains(new BlockPos(box.maxX(), y, box.minZ()))
                && village.contains(new BlockPos(box.minX(), y, box.maxZ()))
                && village.contains(new BlockPos(box.maxX(), y, box.maxZ()));
    }

    /** Spur indices sorted shortest first. Four elements; an insertion sort is plenty. */
    private static int[] spursByLength(MayorSettlement settlement) {
        int count = settlement.roadSpurCount();
        int[] order = new int[count];
        for (int i = 0; i < count; i++) {
            order[i] = i;
        }
        for (int i = 1; i < count; i++) {
            int value = order[i];
            int j = i - 1;
            while (j >= 0 && settlement.roadSpur(order[j]) > settlement.roadSpur(value)) {
                order[j + 1] = order[j];
                j--;
            }
            order[j + 1] = value;
        }
        return order;
    }

    /** The cardinal direction a horizontal offset points in. */
    private static Direction towards(int dx, int dz) {
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    /**
     * The rotation that points a definition's front (its local +Z) at a world
     * direction.
     *
     * <p>Inverse of {@code StructureGeometry.front}, which is
     * {@code rotation.rotate(SOUTH)}. Written out rather than searched for, so a
     * wrong answer is a wrong line here rather than a silent fallback.
     */
    private static Rotation rotationForFront(Direction front) {
        switch (front) {
            case WEST:
                return Rotation.CLOCKWISE_90;
            case NORTH:
                return Rotation.CLOCKWISE_180;
            case EAST:
                return Rotation.COUNTERCLOCKWISE_90;
            case SOUTH:
            default:
                return Rotation.NONE;
        }
    }
}
