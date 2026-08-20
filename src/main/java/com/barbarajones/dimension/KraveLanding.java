package com.barbarajones.dimension;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/**
 * Finds solid ground to actually land a player on in the Krave Kosmos, instead
 * of trusting a fixed coordinate to happen to have terrain under it. Used both
 * by the portal (KraveDoorBlock) and the ambient Kosmonaut spawner.
 */
public final class KraveLanding {

    /** Hard cap on chunk loads per search - bounds worst-case tick stall. */
    private static final int MAX_CHUNKS = 40;

    private KraveLanding() { }

    /**
     * Spirals outward from {@code center} in chunk-sized rings (capped at
     * {@link #MAX_CHUNKS} chunk loads total), force-loading/generating each
     * candidate chunk and column-scanning its center for solid ground.
     * Returns the first solid hit (one block above the surface), or empty if
     * the whole bounded search comes up dry.
     */
    public static Optional<Vec3> findLanding(ServerLevel kosmos, Vec3 center, int rings) {
        int cx = Mth.floor(center.x) >> 4;
        int cz = Mth.floor(center.z) >> 4;
        int tried = 0;

        for (int r = 0; r <= rings && tried < MAX_CHUNKS; r++) {
            for (int dx = -r; dx <= r && tried < MAX_CHUNKS; dx++) {
                for (int dz = -r; dz <= r && tried < MAX_CHUNKS; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;   // ring only - inner chunks already tried at smaller r
                    }
                    tried++;
                    int chunkX = cx + dx;
                    int chunkZ = cz + dz;
                    kosmos.getChunk(chunkX, chunkZ);   // force load/generate

                    double x = (chunkX << 4) + 8.0D;
                    double z = (chunkZ << 4) + 8.0D;
                    Optional<Vec3> found = scanColumn(kosmos, x, z);
                    if (found.isPresent()) {
                        return found;
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Same bounded spiral search as {@link #findLanding}, but rejects two
     * kinds of bad candidate: anywhere too close to a spot already in
     * {@code avoid} (so a batch of placements spreads out instead of
     * bunching together near whichever chunk the search happens to hit
     * first), and anywhere boxed in against a wall or cliff face on most
     * sides (so nothing ends up half-buried in a mountain). Used for
     * scattering healing boxes rather than the single-spot portal landing.
     */
    public static Optional<Vec3> findOpenLanding(ServerLevel kosmos, Vec3 center, int rings,
                                                 List<Vec3> avoid, double minSeparation) {
        int cx = Mth.floor(center.x) >> 4;
        int cz = Mth.floor(center.z) >> 4;
        int tried = 0;

        for (int r = 0; r <= rings && tried < MAX_CHUNKS; r++) {
            for (int dx = -r; dx <= r && tried < MAX_CHUNKS; dx++) {
                for (int dz = -r; dz <= r && tried < MAX_CHUNKS; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    tried++;
                    int chunkX = cx + dx;
                    int chunkZ = cz + dz;
                    kosmos.getChunk(chunkX, chunkZ);

                    double x = (chunkX << 4) + 8.0D;
                    double z = (chunkZ << 4) + 8.0D;
                    Optional<Vec3> found = scanColumn(kosmos, x, z);
                    if (found.isEmpty()) {
                        continue;
                    }
                    Vec3 spot = found.get();
                    if (tooCloseToAny(spot, avoid, minSeparation) || !hasOpenSurroundings(kosmos, spot)) {
                        continue;
                    }
                    return Optional.of(spot);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Same bounded spiral search as {@link #findLanding}, but only accepts a
     * spot with a wide, flat, clear pocket around it - checked across a
     * {@code radius}-block square centered on the candidate, not just the
     * candidate column itself. {@link #findLanding} alone happily returns
     * the peak of a mountain or the floor of a crevice (both are, after all,
     * "solid ground with air above"); the portal built right behind wherever
     * the player lands ({@code KraveDoorBlock#buildReturnPortal}) assumes a
     * level, open area to work with and previously ended up half-buried or
     * floating whenever that assumption didn't hold.
     */
    public static Optional<Vec3> findClearLanding(ServerLevel kosmos, Vec3 center, int rings,
                                                   int radius, int maxHeightVariance, int clearance) {
        int cx = Mth.floor(center.x) >> 4;
        int cz = Mth.floor(center.z) >> 4;
        int tried = 0;

        for (int r = 0; r <= rings && tried < MAX_CHUNKS; r++) {
            for (int dx = -r; dx <= r && tried < MAX_CHUNKS; dx++) {
                for (int dz = -r; dz <= r && tried < MAX_CHUNKS; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    tried++;
                    int chunkX = cx + dx;
                    int chunkZ = cz + dz;
                    kosmos.getChunk(chunkX, chunkZ);

                    double x = (chunkX << 4) + 8.0D;
                    double z = (chunkZ << 4) + 8.0D;
                    Optional<Vec3> found = scanColumn(kosmos, x, z);
                    if (found.isEmpty()) {
                        continue;
                    }
                    Vec3 spot = found.get();
                    if (isFlatAndClear(kosmos, spot, radius, maxHeightVariance, clearance)) {
                        return Optional.of(spot);
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Every ground column in a {@code radius}-block square around {@code
     * spot} must exist (no void edges - a crevice or the map's edge), have
     * at least {@code clearance} blocks of open air above it (no cave
     * ceiling or overhang), and the whole square's surface height must vary
     * by no more than {@code maxHeightVariance} (rejects a mountain peak or
     * slope, which {@link #scanColumn} alone can't tell apart from flat
     * ground - both are "solid, with air above").
     */
    private static boolean isFlatAndClear(ServerLevel level, Vec3 spot, int radius,
                                          int maxHeightVariance, int clearance) {
        int cx = Mth.floor(spot.x);
        int cz = Mth.floor(spot.z);
        int minSurface = Integer.MAX_VALUE;
        int maxSurface = Integer.MIN_VALUE;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                Optional<Vec3> column = scanColumn(level, cx + dx + 0.5D, cz + dz + 0.5D);
                if (column.isEmpty()) {
                    return false;
                }
                int surfaceY = Mth.floor(column.get().y) - 1;
                minSurface = Math.min(minSurface, surfaceY);
                maxSurface = Math.max(maxSurface, surfaceY);

                BlockPos.MutableBlockPos check = new BlockPos.MutableBlockPos(cx + dx, surfaceY + 1, cz + dz);
                for (int dy = 0; dy < clearance; dy++) {
                    if (level.getBlockState(check).blocksMotion()) {
                        return false;
                    }
                    check.move(0, 1, 0);
                }
            }
        }
        return (maxSurface - minSurface) <= maxHeightVariance;
    }

    private static boolean tooCloseToAny(Vec3 spot, List<Vec3> avoid, double minSeparation) {
        for (Vec3 other : avoid) {
            if (spot.distanceTo(other) < minSeparation) {
                return true;
            }
        }
        return false;
    }

    /** At least 3 of the 4 cardinal neighbors at ground level must be open - tolerates one adjacent wall, rejects a corner/crevice. */
    private static boolean hasOpenSurroundings(ServerLevel level, Vec3 spot) {
        BlockPos pos = BlockPos.containing(spot.x, spot.y, spot.z);
        int open = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (!level.getBlockState(pos.relative(dir)).blocksMotion()) {
                open++;
            }
        }
        return open >= 3;
    }

    private static Optional<Vec3> scanColumn(ServerLevel level, double x, double z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(x), 128, Mth.floor(z));
        while (pos.getY() > level.getMinBuildHeight() && !level.getBlockState(pos).blocksMotion()) {
            pos.move(0, -1, 0);
        }
        if (!level.getBlockState(pos).blocksMotion()) {
            return Optional.empty();
        }
        return Optional.of(new Vec3(x, pos.getY() + 1.0D, z));
    }
}
