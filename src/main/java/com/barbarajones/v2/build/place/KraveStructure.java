package com.barbarajones.v2.build.place;

import com.barbarajones.v2.build.def.PlacementContext;
import com.barbarajones.v2.build.def.StructureDef;
import com.barbarajones.v2.build.def.StructureGeometry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.UUID;

/**
 * The public face of the placement engine. Everything outside this package
 * should only ever need this class.
 *
 * <pre>{@code
 * PlacementResult result = KraveStructure.place(level, pos, Rotation.NONE, MyBuildings.STARTER_SHACK);
 * if (!result.started()) {
 *     player.displayClientMessage(result.message(), true);
 * }
 * }</pre>
 *
 * <p>Placement is a two-phase operation and that is the whole point. Phase one
 * ({@link #check}) is pure: it reads the world, works out the build plane,
 * classifies every column and every block in the way, and returns a verdict
 * without writing anything. Phase two only runs if phase one passed. There is
 * no path through this class that writes half a building and then discovers a
 * problem.
 *
 * <p>Phase two hands the work to a {@link BuildJob}, which lays blocks down
 * over a couple of seconds from the ground up. The job is registered with
 * {@link BuildScheduler}, which also force-finishes every outstanding job when
 * a level unloads or the server stops - so a mid-build shutdown still leaves a
 * complete building.
 */
public final class KraveStructure {

    /** Nothing wider than this can be placed, whatever a definition claims. */
    public static final int MAX_FOOTPRINT = 64;
    /** Nothing that touches more blocks than this can be placed. */
    public static final int MAX_VOLUME = 200_000;

    private KraveStructure() { }

    // =====================================================================
    // The API the buildings module calls
    // =====================================================================

    /**
     * Places a building. This is the signature the house module is written
     * against; the overloads below just add optional detail.
     *
     * @param level    the server level; placement is server-only
     * @param anchor   the block the player pointed at (see
     *                 {@link StructureDef.Anchor} for how the footprint sits
     *                 relative to it)
     * @param rotation quarter-turn to apply; {@link Rotation#NONE} faces the
     *                 building's front south
     * @param def      the building
     * @return whether the build started, and a player-ready message either way
     */
    public static PlacementResult place(ServerLevel level, BlockPos anchor, Rotation rotation, StructureDef def) {
        return place(level, anchor, rotation, def, null, false);
    }

    /** As {@link #place(ServerLevel, BlockPos, Rotation, StructureDef)}, recording who did it so undo can refund them. */
    public static PlacementResult place(ServerLevel level, BlockPos anchor, Rotation rotation,
                                        StructureDef def, @Nullable ServerPlayer placer) {
        return place(level, anchor, rotation, def, placer, false);
    }

    /**
     * @param instant skip the animation and finish this tick. Use for worldgen
     *                and commands, never for a player placing a schematic - the
     *                build-up is the point.
     */
    public static PlacementResult place(ServerLevel level, BlockPos anchor, Rotation rotation,
                                        StructureDef def, @Nullable ServerPlayer placer, boolean instant) {
        if (def == null) {
            return new PlacementResult(false, Component.translatable("barbarajones.build.fail.unknown"),
                    PlacementCheck.refusal(null, rotation, anchor,
                            Component.translatable("barbarajones.build.fail.unknown")), null);
        }

        PlacementCheck check = check(level, anchor, rotation, def);
        if (!check.ok()) {
            return new PlacementResult(false, check.failure(), check, null);
        }
        if (BuildScheduler.overlapsRunningJob(level, check.worldBounds())) {
            Component busy = Component.translatable("barbarajones.build.fail.busy");
            return new PlacementResult(false, busy,
                    PlacementCheck.refusal(def, rotation, anchor, busy), null);
        }

        UUID placerId = placer == null ? null : placer.getUUID();
        PlacementContext ctx = new PlacementContext(level, def, rotation, check.origin(), anchor, placerId);
        BuildJob job = new BuildJob(level, def, check, ctx, placer);
        BuildScheduler.submit(level, job);
        if (instant) {
            job.finishNow();
        }
        return new PlacementResult(true,
                Component.translatable("barbarajones.build.started", Component.translatable(def.nameKey())),
                check, job);
    }

    /**
     * Validates a placement without touching the world. Safe on the client -
     * the ghost preview calls this every few ticks.
     */
    public static PlacementCheck check(LevelReader level, BlockPos anchor, Rotation rotation, StructureDef def) {
        if (def == null) {
            return PlacementCheck.refusal(null, rotation, anchor,
                    Component.translatable("barbarajones.build.fail.unknown"));
        }

        final int spanX = def.spanX();
        final int spanZ = def.spanZ();
        final int rSpanX = StructureGeometry.rotatedSpanX(spanX, spanZ, rotation);
        final int rSpanZ = StructureGeometry.rotatedSpanZ(spanX, spanZ, rotation);
        if (rSpanX > MAX_FOOTPRINT || rSpanZ > MAX_FOOTPRINT) {
            return PlacementCheck.refusal(def, rotation, anchor,
                    Component.translatable("barbarajones.build.fail.too_big", MAX_FOOTPRINT));
        }

        final int ox = originX(anchor, rotation, def, rSpanX);
        final int oz = originZ(anchor, rotation, def, rSpanZ);

        // ---- 1. where is the ground in every column -----------------------
        final int scanRange = Math.max(8, def.maxGroundDelta() + 6);
        int[] surface = new int[rSpanX * rSpanZ];
        int found = 0;
        for (int dz = 0; dz < rSpanZ; dz++) {
            for (int dx = 0; dx < rSpanX; dx++) {
                int y = def.groundMode() == StructureDef.GroundMode.FLOAT
                        ? anchor.getY() - 1
                        : scanSurface(level, ox + dx, oz + dz, anchor.getY(), scanRange);
                surface[dz * rSpanX + dx] = y;
                if (y != PlacementCheck.NO_SURFACE) {
                    found++;
                }
            }
        }
        if (found == 0) {
            return PlacementCheck.refusal(def, rotation, anchor,
                    Component.translatable("barbarajones.build.fail.no_ground"));
        }

        // ---- 2. the build plane -------------------------------------------
        final int baseY;
        if (def.groundMode() == StructureDef.GroundMode.FLOAT) {
            baseY = anchor.getY();
        } else {
            int[] valid = new int[found];
            int i = 0;
            for (int y : surface) {
                if (y != PlacementCheck.NO_SURFACE) {
                    valid[i++] = y;
                }
            }
            Arrays.sort(valid);
            baseY = valid[valid.length / 2] + 1;
        }

        // The lowest block each column will actually be touched at. This is not
        // simply "the bottom of the box": for a column that only gets shaved
        // down, nothing below the build plane is ever written, and scanning
        // there anyway would refuse to build over somebody's cellar roof.
        final int planMinY = baseY + def.localBounds().minY();
        int[] columnLow = new int[rSpanX * rSpanZ];
        int minY = planMinY;
        int maxY = baseY + def.localBounds().maxY();
        for (int index = 0; index < columnLow.length; index++) {
            int y = surface[index];
            int low = (y != PlacementCheck.NO_SURFACE && y + 1 < baseY) ? y + 1 : baseY;
            low = Math.min(low, planMinY);
            columnLow[index] = low;
            minY = Math.min(minY, low);
            if (y != PlacementCheck.NO_SURFACE) {
                maxY = Math.max(maxY, y);
            }
        }
        if (minY < level.getMinBuildHeight() || maxY >= level.getMaxBuildHeight()) {
            return PlacementCheck.refusal(def, rotation, anchor,
                    Component.translatable("barbarajones.build.fail.world_height"));
        }

        long volume = (long) rSpanX * rSpanZ * (maxY - minY + 1);
        if (volume > MAX_VOLUME) {
            return PlacementCheck.refusal(def, rotation, anchor,
                    Component.translatable("barbarajones.build.fail.too_big", MAX_FOOTPRINT));
        }

        // ---- 3. per-column terrain verdict ---------------------------------
        PlacementCheck.ColumnStatus[] status = new PlacementCheck.ColumnStatus[rSpanX * rSpanZ];
        int worstDelta = 0;
        boolean uneven = false;
        boolean voidColumn = false;
        for (int index = 0; index < status.length; index++) {
            int y = surface[index];
            if (y == PlacementCheck.NO_SURFACE) {
                status[index] = PlacementCheck.ColumnStatus.NO_GROUND;
                voidColumn = true;
                continue;
            }
            int delta = (y + 1) - baseY;
            if (Math.abs(delta) > Math.abs(worstDelta)) {
                worstDelta = delta;
            }
            if (def.groundMode() == StructureDef.GroundMode.FLOAT) {
                status[index] = PlacementCheck.ColumnStatus.OK;
            } else if (def.groundMode() == StructureDef.GroundMode.STRICT && delta != 0) {
                status[index] = PlacementCheck.ColumnStatus.TOO_STEEP;
                uneven = true;
            } else if (Math.abs(delta) > def.maxGroundDelta()) {
                status[index] = PlacementCheck.ColumnStatus.TOO_STEEP;
                uneven = true;
            } else if (delta > 0) {
                status[index] = PlacementCheck.ColumnStatus.CUT;
            } else if (delta < 0) {
                status[index] = PlacementCheck.ColumnStatus.FILL;
            } else {
                status[index] = PlacementCheck.ColumnStatus.OK;
            }
        }

        // ---- 4. is anything in the way -------------------------------------
        Component blockedMessage = null;
        BlockPos blockedPos = null;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dz = 0; dz < rSpanZ; dz++) {
            for (int dx = 0; dx < rSpanX; dx++) {
                int index = dz * rSpanX + dx;
                int wx = ox + dx;
                int wz = oz + dz;
                for (int y = columnLow[index]; y <= maxY; y++) {
                    cursor.set(wx, y, wz);
                    if (!level.hasChunkAt(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir()) {
                        continue;
                    }
                    if (TerrainRules.verdict(level, cursor, state) == TerrainRules.Verdict.PROTECTED) {
                        status[index] = PlacementCheck.ColumnStatus.BLOCKED;
                        if (blockedMessage == null) {
                            blockedPos = cursor.immutable();
                            blockedMessage = state.is(Blocks.LAVA)
                                    ? Component.translatable("barbarajones.build.fail.lava")
                                    : Component.translatable("barbarajones.build.fail.blocked",
                                            state.getBlock().getName(),
                                            blockedPos.getX(), blockedPos.getY(), blockedPos.getZ());
                        }
                        break;
                    }
                }
            }
        }

        BlockPos origin = new BlockPos(ox, baseY, oz);
        BoundingBox bounds = new BoundingBox(ox, minY, oz, ox + rSpanX - 1, maxY, oz + rSpanZ - 1);

        Component failure = null;
        BlockPos failurePos = null;
        if (blockedMessage != null) {
            failure = blockedMessage;
            failurePos = blockedPos;
        } else if (voidColumn) {
            failure = Component.translatable("barbarajones.build.fail.no_ground");
        } else if (uneven) {
            failure = def.groundMode() == StructureDef.GroundMode.STRICT
                    ? Component.translatable("barbarajones.build.fail.strict")
                    : Component.translatable("barbarajones.build.fail.uneven",
                            Math.abs(worstDelta), def.maxGroundDelta());
        }

        return new PlacementCheck(def, rotation, anchor, origin, baseY, rSpanX, rSpanZ,
                surface, status, bounds, failure == null, failure, failurePos);
    }

    // =====================================================================
    // Geometry helpers, shared with the preview
    // =====================================================================

    /** World X of the rotated footprint's minimum corner. */
    public static int originX(BlockPos anchor, Rotation rotation, StructureDef def, int rSpanX) {
        switch (def.anchorMode()) {
            case CORNER:
                return anchor.getX();
            case FRONT: {
                Direction front = StructureGeometry.front(rotation);
                if (front == Direction.EAST) {
                    return anchor.getX() - rSpanX + 1;
                }
                if (front == Direction.WEST) {
                    return anchor.getX();
                }
                return anchor.getX() - rSpanX / 2;
            }
            case CENTER:
            default:
                return anchor.getX() - rSpanX / 2;
        }
    }

    /** World Z of the rotated footprint's minimum corner. */
    public static int originZ(BlockPos anchor, Rotation rotation, StructureDef def, int rSpanZ) {
        switch (def.anchorMode()) {
            case CORNER:
                return anchor.getZ();
            case FRONT: {
                Direction front = StructureGeometry.front(rotation);
                if (front == Direction.SOUTH) {
                    return anchor.getZ() - rSpanZ + 1;
                }
                if (front == Direction.NORTH) {
                    return anchor.getZ();
                }
                return anchor.getZ() - rSpanZ / 2;
            }
            case CENTER:
            default:
                return anchor.getZ() - rSpanZ / 2;
        }
    }

    /**
     * Turns whatever the player is pointing at into the anchor block. Shared by
     * the item and the ghost preview so the preview never lies about where the
     * building will land.
     */
    public static BlockPos anchorFor(BlockHitResult hit) {
        return anchorFor(hit.getBlockPos(), hit.getDirection());
    }

    /** Same rule, from a position and the face that was hit. */
    public static BlockPos anchorFor(BlockPos hitPos, Direction face) {
        return face == Direction.UP ? hitPos.above() : hitPos.relative(face);
    }

    /**
     * Finds the top of the ground in one column: the highest solid, non-plant,
     * non-tree block within {@code range} of {@code fromY}.
     *
     * <p>Trees are skipped deliberately. A building placed in a forest should
     * measure the dirt, not the canopy - the trunk inside the footprint is
     * cleared like any other natural block.
     */
    public static int scanSurface(LevelReader level, int x, int z, int fromY, int range) {
        int top = Math.min(fromY + range, level.getMaxBuildHeight() - 1);
        int bottom = Math.max(fromY - range, level.getMinBuildHeight());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = top; y >= bottom; y--) {
            cursor.set(x, y, z);
            if (!level.hasChunkAt(cursor)) {
                return PlacementCheck.NO_SURFACE;
            }
            BlockState state = level.getBlockState(cursor);
            if (state.isAir() || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
                    || state.is(Blocks.SNOW) || state.is(Blocks.BAMBOO)) {
                continue;
            }
            if (TerrainRules.isGround(level, cursor, state)) {
                return y;
            }
        }
        return PlacementCheck.NO_SURFACE;
    }
}
