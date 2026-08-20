package com.barbarajones.v2.houses.def;

import com.barbarajones.v2.build.def.StructureDef;

/**
 * Procedural roof shapes shared by every house definition.
 *
 * <p>Hand-typing a sloped roof as ASCII layers is where schematics go wrong -
 * one mistyped row and a slope has a step in it that nobody notices until the
 * building is standing in front of them. These generate the slope from a
 * handful of integers instead, the same "generated shape" escape hatch
 * {@code StructureOp} exists for.
 *
 * <h2>The technique</h2>
 * A pitched roof is built as a stack of paired edge rows that each step one
 * block inward as they go up, left deliberately open in the middle (that gap
 * is the attic, and it is already air - nothing needs to be carved). Two rows
 * facing each other at the same height is what makes the ridge read as a
 * peak; only when a single row is left (an odd span) does it need a flat cap
 * instead. This is the standard hand-built Minecraft roof technique, just
 * driven by a loop instead of a person.
 *
 * <p>Stair {@code FACING} means "the slope faces this way" throughout - a
 * north-facing stair's sloped top faces north, which is what {@link
 * HousePalette}'s roof characters are pre-baked as.
 */
final class RoofKit {

    private RoofKit() { }

    /**
     * A symmetric gable roof whose ridge runs along local X: it slopes down
     * toward -Z (the {@code north} char) on one side and toward +Z ({@code
     * south}) on the other. Spans {@code [x1,x2]} by {@code [z1,z2]}
     * inclusive, first course at {@code y}.
     */
    static void gableAlongX(StructureDef.Builder b, int x1, int x2, int z1, int z2, int y,
                             char north, char south, char ridge) {
        int zLo = z1;
        int zHi = z2;
        int layer = y;
        while (zLo <= zHi) {
            if (zLo == zHi) {
                b.fill(x1, layer, zLo, x2, layer, zLo, ridge);
            } else {
                b.fill(x1, layer, zLo, x2, layer, zLo, north);
                b.fill(x1, layer, zHi, x2, layer, zHi, south);
            }
            zLo++;
            zHi--;
            layer++;
        }
    }

    /** Same shape, ridge along local Z: slopes toward -X ({@code west}) and +X ({@code east}). */
    static void gableAlongZ(StructureDef.Builder b, int x1, int x2, int z1, int z2, int y,
                             char west, char east, char ridge) {
        int xLo = x1;
        int xHi = x2;
        int layer = y;
        while (xLo <= xHi) {
            if (xLo == xHi) {
                b.fill(xLo, layer, z1, xLo, layer, z2, ridge);
            } else {
                b.fill(xLo, layer, z1, xLo, layer, z2, west);
                b.fill(xHi, layer, z1, xHi, layer, z2, east);
            }
            xLo++;
            xHi--;
            layer++;
        }
    }

    /**
     * The solid triangular infill a {@link #gableAlongZ} roof needs at ONE of
     * its two gable-end walls (call it once for {@code z1}, once for {@code
     * z2}) so the attic is not open to the sky sideways. Mirrors {@code
     * gableAlongZ}'s own shrink loop exactly, so pass it the same {@code x1,
     * x2, y} - the two edge columns are left alone (those are the roof
     * stairs, already placed) and only the gap between them is filled.
     */
    static void gableEndWallZ(StructureDef.Builder b, int x1, int x2, int z, int y, char wall) {
        int xLo = x1;
        int xHi = x2;
        int layer = y;
        while (xLo < xHi) {
            int innerLo = xLo + 1;
            int innerHi = xHi - 1;
            if (innerLo <= innerHi) {
                b.fill(innerLo, layer, z, innerHi, layer, z, wall);
            }
            xLo++;
            xHi--;
            layer++;
        }
    }

    /** As {@link #gableEndWallZ}, for a {@link #gableAlongX} roof's end wall at one {@code x}. */
    static void gableEndWallX(StructureDef.Builder b, int z1, int z2, int x, int y, char wall) {
        int zLo = z1;
        int zHi = z2;
        int layer = y;
        while (zLo < zHi) {
            int innerLo = zLo + 1;
            int innerHi = zHi - 1;
            if (innerLo <= innerHi) {
                b.fill(x, layer, innerLo, x, layer, innerHi, wall);
            }
            zLo++;
            zHi--;
            layer++;
        }
    }

    /**
     * A one-directional lean roof: a single slope from its high end {@code
     * (zHighEnd, yHighEnd)} down to its low end {@code (zLowEnd, yLowEnd)}.
     * The two ends' z and y distances must match so every step is a clean 45
     * degrees. {@code slope} must be a stair character whose FACING points
     * toward the low end (downhill) - that is what makes the sloped face
     * point outward and down rather than up into the building.
     */
    static void monoSlope(StructureDef.Builder b, int x1, int x2,
                          int zHighEnd, int yHighEnd, int zLowEnd, int yLowEnd, char slope) {
        int steps = Math.abs(zLowEnd - zHighEnd);
        if (Math.abs(yLowEnd - yHighEnd) != steps) {
            throw new IllegalArgumentException("monoSlope: z distance " + steps
                    + " does not match y distance " + Math.abs(yLowEnd - yHighEnd)
                    + " - a single-stair slope is always 45 degrees, fix one end or the other.");
        }
        int zStep = zLowEnd >= zHighEnd ? 1 : -1;
        int yStep = yLowEnd >= yHighEnd ? 1 : -1;
        for (int i = 0; i <= steps; i++) {
            int z = zHighEnd + i * zStep;
            int y = yHighEnd + i * yStep;
            b.fill(x1, y, z, x2, y, z, slope);
        }
    }

    /**
     * A square four-way hip roof closing to a single point - a turret cap.
     * Requires {@code x2 - x1 == z2 - z1} (an equal-sided footprint); anything
     * else and the two axes close at different heights and the loop simply
     * stops without a cap, which is a sign the call site picked the wrong
     * shape rather than something this method can paper over.
     */
    static void pyramid(StructureDef.Builder b, int x1, int x2, int z1, int z2, int y,
                        char north, char south, char east, char west, char cap) {
        int xLo = x1;
        int xHi = x2;
        int zLo = z1;
        int zHi = z2;
        int layer = y;
        while (xLo <= xHi && zLo <= zHi) {
            if (xLo == xHi && zLo == zHi) {
                b.set(xLo, layer, zLo, cap);
                return;
            }
            b.fill(xLo, layer, zLo, xHi, layer, zLo, north);
            b.fill(xLo, layer, zHi, xHi, layer, zHi, south);
            if (zLo + 1 <= zHi - 1) {
                b.fill(xLo, layer, zLo + 1, xLo, layer, zHi - 1, west);
                b.fill(xHi, layer, zLo + 1, xHi, layer, zHi - 1, east);
            }
            xLo++;
            xHi--;
            zLo++;
            zHi--;
            layer++;
        }
    }
}
