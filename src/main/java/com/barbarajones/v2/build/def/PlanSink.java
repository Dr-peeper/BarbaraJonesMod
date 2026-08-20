package com.barbarajones.v2.build.def;

/**
 * Where a {@link StructureOp} writes its blocks while a {@link StructureDef} is
 * being baked. Coordinates are LOCAL: x/z within the footprint, y relative to
 * the ground plane (y=0 is the first block above the levelled ground, y=-1 is
 * the ground surface block itself).
 *
 * <p>The key is a palette character. Two characters are reserved:
 * {@link Palette#KEEP} ('.') removes the position from the plan entirely (the
 * world block there is left alone), and {@link Palette#AIR} (' ') writes air
 * (the world block there is carved out).
 */
@FunctionalInterface
public interface PlanSink {

    void put(int x, int y, int z, char key);
}
