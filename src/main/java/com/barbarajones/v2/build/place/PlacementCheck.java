package com.barbarajones.v2.build.place;

import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.Nullable;

/**
 * The answer to "can this building go here, and what would it look like".
 *
 * <p>Produced by {@link KraveStructure#check} and used by three different
 * callers: the server before it commits, the client to colour the ghost
 * preview, and the failure path to tell the player what is actually wrong. It
 * is pure data - computing it never touches the world.
 *
 * <p>Column data is stored in flat arrays indexed {@code dz * spanX + dx},
 * because the preview walks it every frame.
 */
public final class PlacementCheck {

    /** Per-column verdict, which is what the ghost preview colours by. */
    public enum ColumnStatus {
        /** Ground is already at the build plane. */
        OK,
        /** Ground is above the plane and will be shaved down. */
        CUT,
        /** Ground is below the plane and will be packed up. */
        FILL,
        /** Something somebody built is in the way. */
        BLOCKED,
        /** Ground is further from the plane than the definition allows. */
        TOO_STEEP,
        /** No ground found at all - a cliff edge, deep water, the void. */
        NO_GROUND
    }

    /** Marker for "the surface scan found nothing in this column". */
    public static final int NO_SURFACE = Integer.MIN_VALUE;

    private final StructureDef def;
    private final Rotation rotation;
    private final BlockPos anchor;
    private final BlockPos origin;
    private final int baseY;
    private final int spanX;
    private final int spanZ;
    private final int[] surfaceY;
    private final ColumnStatus[] status;
    private final BoundingBox worldBounds;
    private final boolean ok;
    private final Component failure;
    private final BlockPos failurePos;

    PlacementCheck(StructureDef def, Rotation rotation, BlockPos anchor, BlockPos origin, int baseY,
                   int spanX, int spanZ, int[] surfaceY, ColumnStatus[] status,
                   BoundingBox worldBounds, boolean ok,
                   @Nullable Component failure, @Nullable BlockPos failurePos) {
        this.def = def;
        this.rotation = rotation;
        this.anchor = anchor;
        this.origin = origin;
        this.baseY = baseY;
        this.spanX = spanX;
        this.spanZ = spanZ;
        this.surfaceY = surfaceY;
        this.status = status;
        this.worldBounds = worldBounds;
        this.ok = ok;
        this.failure = failure;
        this.failurePos = failurePos;
    }

    /** A refusal with no geometry - unknown structure, wrong dimension, that kind of thing. */
    static PlacementCheck refusal(StructureDef def, Rotation rotation, BlockPos anchor, Component reason) {
        return new PlacementCheck(def, rotation, anchor, anchor, anchor.getY(), 0, 0,
                new int[0], new ColumnStatus[0],
                new BoundingBox(anchor.getX(), anchor.getY(), anchor.getZ(),
                        anchor.getX(), anchor.getY(), anchor.getZ()),
                false, reason, null);
    }

    public boolean ok() {
        return ok;
    }

    public StructureDef def() {
        return def;
    }

    public Rotation rotation() {
        return rotation;
    }

    public BlockPos anchor() {
        return anchor;
    }

    /** World position of the rotated footprint's minimum X/Z corner, at the build plane. */
    public BlockPos origin() {
        return origin;
    }

    /** World Y of the building's floor (local y = 0). */
    public int baseY() {
        return baseY;
    }

    /** Footprint width along world X, after rotation. */
    public int spanX() {
        return spanX;
    }

    /** Footprint width along world Z, after rotation. */
    public int spanZ() {
        return spanZ;
    }

    /** Every block position the placement will read or write, including the terrain it levels. */
    public BoundingBox worldBounds() {
        return worldBounds;
    }

    /** Why it failed, ready to send to the player. Null when {@link #ok()}. */
    @Nullable
    public Component failure() {
        return failure;
    }

    /** The block that caused the failure, when there was a single culprit. */
    @Nullable
    public BlockPos failurePos() {
        return failurePos;
    }

    public ColumnStatus status(int dx, int dz) {
        if (dx < 0 || dz < 0 || dx >= spanX || dz >= spanZ) {
            return ColumnStatus.NO_GROUND;
        }
        return status[dz * spanX + dx];
    }

    /** Existing ground height in this column, or {@link #NO_SURFACE}. */
    public int surfaceY(int dx, int dz) {
        if (dx < 0 || dz < 0 || dx >= spanX || dz >= spanZ) {
            return NO_SURFACE;
        }
        return surfaceY[dz * spanX + dx];
    }

    /** True if this column is fine to build on. */
    public boolean columnOk(int dx, int dz) {
        ColumnStatus s = status(dx, dz);
        return s == ColumnStatus.OK || s == ColumnStatus.CUT || s == ColumnStatus.FILL;
    }

    /** The Y the preview should draw this column's tile at, so it is never buried in a hill. */
    public int previewY(int dx, int dz) {
        int surface = surfaceY(dx, dz);
        if (surface == NO_SURFACE) {
            return baseY;
        }
        return Math.max(baseY, surface + 1);
    }
}
