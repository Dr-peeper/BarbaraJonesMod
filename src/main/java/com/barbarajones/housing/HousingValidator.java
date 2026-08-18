package com.barbarajones.housing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Terraria-style housing validation.
 *
 * Starting from a candidate position we flood-fill the open air. If the fill
 * escapes (hits the cap) the room is not enclosed. Otherwise we check the room
 * against the residency requirements:
 *
 *   - enclosed on all sides (the flood fill terminates)
 *   - at least {@link #MIN_VOLUME} blocks of interior air
 *   - at least {@link #MIN_HEIGHT} blocks tall somewhere
 *   - a light source (block light in the room)
 *   - a door or trapdoor (a way in)
 *   - a bed (somewhere to sleep)
 *   - a solid floor under the room
 *
 * Cayden refuses to live anywhere that fails. The Housing Query item reports
 * every failing condition at once so you know exactly what to build.
 */
public final class HousingValidator {

    public static final int MIN_VOLUME = 30;    // interior air blocks
    public static final int MIN_HEIGHT = 3;
    public static final int MAX_FILL   = 900;   // fill cap: beyond this it's "outdoors"
    public static final int MIN_LIGHT  = 8;

    private HousingValidator() { }

    public static HousingResult validate(Level level, BlockPos start) {
        BlockPos origin = findOpenSpace(level, start);
        if (origin == null) {
            return HousingResult.fail("No open space here to call a room.");
        }

        Set<BlockPos> air = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        air.add(origin);

        boolean escaped = false;
        int minY = origin.getY(), maxY = origin.getY();

        while (!queue.isEmpty()) {
            if (air.size() > MAX_FILL) {
                escaped = true;
                break;
            }
            BlockPos cur = queue.poll();
            minY = Math.min(minY, cur.getY());
            maxY = Math.max(maxY, cur.getY());

            for (Direction dir : Direction.values()) {
                BlockPos next = cur.relative(dir);
                if (air.contains(next)) {
                    continue;
                }
                if (!level.isLoaded(next)) {
                    escaped = true;
                    continue;
                }
                if (isWall(level, next)) {
                    continue;   // wall: the fill stops here, as it should
                }
                air.add(next);
                queue.add(next);
            }
        }

        HousingResult result = new HousingResult();
        result.volume = air.size();
        result.anchor = origin;

        if (escaped || air.size() > MAX_FILL) {
            result.valid = false;
            result.problems.add("Not enclosed - seal the walls, floor and roof.");
            return result;
        }
        if (air.size() < MIN_VOLUME) {
            result.problems.add("Too cramped (" + air.size() + "/" + MIN_VOLUME + " blocks of space).");
        }
        if ((maxY - minY + 1) < MIN_HEIGHT) {
            result.problems.add("Ceiling too low (needs " + MIN_HEIGHT + " blocks of headroom).");
        }
        if (!hasLight(level, air)) {
            result.problems.add("Too dark - add a light source.");
        }
        if (!hasDoor(level, air)) {
            result.problems.add("No door - he needs a way in.");
        }
        if (!hasBed(level, air)) {
            result.problems.add("No bed - he is not sleeping on the floor.");
        }
        if (!hasFloor(level, air, minY)) {
            result.problems.add("No solid floor.");
        }

        result.valid = result.problems.isEmpty();
        if (result.valid) {
            result.anchor = centre(air, origin);
        }
        return result;
    }

    // ---- checks ------------------------------------------------------------

    /** A block that blocks the flood fill: anything solid, plus doors/trapdoors. */
    private static boolean isWall(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock) {
            return true;   // a closed doorway still counts as sealing the room
        }
        if (state.getBlock() instanceof BedBlock) {
            return false;  // furniture doesn't wall the room off
        }
        return state.isSolidRender(level, pos) || state.blocksMotion();
    }

    private static BlockPos findOpenSpace(Level level, BlockPos start) {
        if (level.getBlockState(start).isAir()) {
            return start;
        }
        for (int dy = 0; dy <= 3; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = start.offset(dx, dy, dz);
                    if (level.getBlockState(p).isAir()) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    private static boolean hasLight(Level level, Set<BlockPos> air) {
        for (BlockPos p : air) {
            if (level.getBrightness(LightLayer.BLOCK, p) >= MIN_LIGHT) {
                return true;
            }
        }
        return false;
    }

    /** Doors bound the room, so scan the shell around the air cells. */
    private static boolean hasDoor(Level level, Set<BlockPos> air) {
        for (BlockPos p : air) {
            for (Direction dir : Direction.values()) {
                BlockState s = level.getBlockState(p.relative(dir));
                if (s.getBlock() instanceof DoorBlock || s.getBlock() instanceof TrapDoorBlock) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasBed(Level level, Set<BlockPos> air) {
        for (BlockPos p : air) {
            if (level.getBlockState(p).getBlock() instanceof BedBlock) {
                return true;
            }
            for (Direction dir : Direction.values()) {
                if (level.getBlockState(p.relative(dir)).getBlock() instanceof BedBlock) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasFloor(Level level, Set<BlockPos> air, int minY) {
        int solid = 0, checked = 0;
        for (BlockPos p : air) {
            if (p.getY() != minY) {
                continue;
            }
            checked++;
            BlockState below = level.getBlockState(p.below());
            if (!below.isAir() && below.blocksMotion()) {
                solid++;
            }
        }
        return checked > 0 && solid >= checked * 0.7;
    }

    private static BlockPos centre(Set<BlockPos> air, BlockPos fallback) {
        long sx = 0, sy = 0, sz = 0;
        for (BlockPos p : air) {
            sx += p.getX(); sy += p.getY(); sz += p.getZ();
        }
        int n = air.size();
        BlockPos avg = new BlockPos((int) (sx / n), (int) (sy / n), (int) (sz / n));
        return air.contains(avg) ? avg : fallback;
    }
}
