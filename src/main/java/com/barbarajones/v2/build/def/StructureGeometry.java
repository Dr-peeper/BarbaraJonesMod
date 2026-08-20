package com.barbarajones.v2.build.def;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;

/**
 * The rotation maths, in one place, so the server placer, the client ghost
 * preview and the undo pass can never disagree about where a block goes.
 *
 * <p>Convention, and this is the whole contract:
 * <ul>
 *   <li>Local +X is right, local +Z is the building's FRONT (the side the door
 *       faces), local +Y is up.</li>
 *   <li>{@link Rotation#NONE} therefore means the front faces SOUTH, matching
 *       vanilla's structure convention.</li>
 *   <li>Local coordinates are normalised into {@code [0, span)} before rotating,
 *       so a definition that uses negative Y (a foundation) or an off-origin X
 *       still rotates around its own footprint rather than around world zero.</li>
 * </ul>
 */
public final class StructureGeometry {

    private StructureGeometry() { }

    /** True when the rotation swaps the X and Z spans. */
    public static boolean swapsAxes(Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90;
    }

    public static int rotatedSpanX(int spanX, int spanZ, Rotation rotation) {
        return swapsAxes(rotation) ? spanZ : spanX;
    }

    public static int rotatedSpanZ(int spanX, int spanZ, Rotation rotation) {
        return swapsAxes(rotation) ? spanX : spanZ;
    }

    /**
     * Rotates a normalised local X.
     *
     * @param nx normalised local x, in {@code [0, spanX)}
     * @param nz normalised local z, in {@code [0, spanZ)}
     */
    public static int rotateX(int nx, int nz, int spanX, int spanZ, Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_90:
                return spanZ - 1 - nz;
            case CLOCKWISE_180:
                return spanX - 1 - nx;
            case COUNTERCLOCKWISE_90:
                return nz;
            case NONE:
            default:
                return nx;
        }
    }

    /** Rotates a normalised local Z. See {@link #rotateX}. */
    public static int rotateZ(int nx, int nz, int spanX, int spanZ, Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_90:
                return nx;
            case CLOCKWISE_180:
                return spanZ - 1 - nz;
            case COUNTERCLOCKWISE_90:
                return spanX - 1 - nx;
            case NONE:
            default:
                return nz;
        }
    }

    /** Which way the building's front faces once rotated. */
    public static Direction front(Rotation rotation) {
        return rotation.rotate(Direction.SOUTH);
    }

    /**
     * The rotation that turns the building's front towards a player facing
     * {@code playerFacing}. A player looking north sees a building whose front
     * faces south, which is {@link Rotation#NONE}.
     */
    public static Rotation facingPlayer(Direction playerFacing) {
        switch (playerFacing) {
            case EAST:
                return Rotation.CLOCKWISE_90;
            case SOUTH:
                return Rotation.CLOCKWISE_180;
            case WEST:
                return Rotation.COUNTERCLOCKWISE_90;
            case NORTH:
            default:
                return Rotation.NONE;
        }
    }

    /** Steps a rotation one quarter turn clockwise. */
    public static Rotation next(Rotation rotation) {
        switch (rotation) {
            case NONE:
                return Rotation.CLOCKWISE_90;
            case CLOCKWISE_90:
                return Rotation.CLOCKWISE_180;
            case CLOCKWISE_180:
                return Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90:
            default:
                return Rotation.NONE;
        }
    }

    /** Steps a rotation one quarter turn anticlockwise. */
    public static Rotation previous(Rotation rotation) {
        return next(next(next(rotation)));
    }

    /** Stable index 0..3 for NBT storage. */
    public static int index(Rotation rotation) {
        switch (rotation) {
            case CLOCKWISE_90:
                return 1;
            case CLOCKWISE_180:
                return 2;
            case COUNTERCLOCKWISE_90:
                return 3;
            case NONE:
            default:
                return 0;
        }
    }

    /** Inverse of {@link #index(Rotation)}; tolerates junk input. */
    public static Rotation byIndex(int index) {
        switch (Math.floorMod(index, 4)) {
            case 1:
                return Rotation.CLOCKWISE_90;
            case 2:
                return Rotation.CLOCKWISE_180;
            case 3:
                return Rotation.COUNTERCLOCKWISE_90;
            case 0:
            default:
                return Rotation.NONE;
        }
    }
}
