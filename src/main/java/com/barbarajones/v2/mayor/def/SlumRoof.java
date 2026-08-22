package com.barbarajones.v2.mayor.def;

import com.barbarajones.v2.build.def.Palette;
import com.barbarajones.v2.build.def.StructureDef;

import static com.barbarajones.v2.mayor.def.SlumPalette.*;

/**
 * Roof shapes for Barbara's public works. Flat ones, mostly.
 *
 * <h2>Why flat, when the housing module went to the trouble of a gable kit</h2>
 * Three reasons, and all three are load-bearing:
 *
 * <ol>
 *   <li>A pitched roof is the single strongest "this is a proper house" signal
 *       in Minecraft. Nothing with a clean gable on it ever reads as run-down,
 *       however grubby the walls under it are.
 *   <li>A flat roof is a deck, and a deck is somewhere to put the next thing.
 *       Every stacked storey, every rooftop shack and every aerial in this
 *       module sits on one. The cramped, built-upon look at the higher rungs is
 *       only possible because the rung below it ended in a flat surface.
 *   <li>A 45-degree stair slope drops one block per block of run, so a pitched
 *       roof on a nine-deep building is five blocks tall. Stacking that twice
 *       gives a tower, not a tenement.
 * </ol>
 *
 * <p>Sloped stairs still appear here, but only as {@link #awning}: one course of
 * salvage nailed over a doorway or hung out over an alley. Buildings in this
 * module use several of them, facing different ways, on the same roof.
 *
 * <p>Every method takes the builder's local coordinates and is inclusive at both
 * ends, matching {@code StructureDef.Builder}'s own primitives.
 */
final class SlumRoof {

    private SlumRoof() { }

    /**
     * A flat roof deck: boards, then sheeting over about a third of it, then a
     * scatter of half-laid slabs where somebody started a repair and stopped.
     *
     * <p>The scatter is hash-driven inside {@code StructureDef.Builder}, so a
     * given building always bakes to the same pattern and stays diffable - the
     * variation between two of the same building in the world comes from the
     * weighted palette resolving differently per block, not from this.
     */
    static void deck(StructureDef.Builder b, int x1, int z1, int x2, int z2, int y) {
        b.fill(x1, y, z1, x2, y, z2, DECK);
        b.scatter(x1, y, z1, x2, y, z2, TARP, 0.34F);
        b.scatter(x1, y, z1, x2, y, z2, SLAB, 0.08F);
    }

    /**
     * A one-block kerb round the edge of a deck with about a fifth of it
     * missing. The gaps are {@link Palette#KEEP}, not air, so nothing is written
     * there at all and the roofline is genuinely ragged rather than uniformly
     * low.
     */
    static void parapet(StructureDef.Builder b, int x1, int z1, int x2, int z2, int y) {
        b.walls(x1, y, z1, x2, y, z2, RUBBLE);
        shellScatter(b, x1, z1, x2, z2, y, FENCE, 0.30F);
        shellScatter(b, x1, z1, x2, z2, y, Palette.KEEP, 0.22F);
    }

    /**
     * The bits people put on a flat roof: a stub chimney with a slab cap, and a
     * fence-post aerial with a torch on the end of it so the residents can find
     * the hatch at night.
     *
     * <p>Deliberately asymmetric - it is placed relative to the roof's minimum
     * corner rather than centred, because a centred chimney reads as designed.
     */
    static void clutter(StructureDef.Builder b, int x1, int z1, int y) {
        b.column(x1 + 1, z1 + 1, y, y + 1, RUBBLE);
        b.set(x1 + 1, y + 2, z1 + 1, SLAB);
        b.column(x1 + 2, z1 + 2, y, y + 2, FENCE);
        b.set(x1 + 2, y + 3, z1 + 2, TORCH);
    }

    /**
     * A single course of stairs jutting out over a doorway or a shopfront.
     * {@code slope} must be the stair character whose facing points away from
     * the building, or the overhang points its sloped face back into the wall.
     */
    static void awning(StructureDef.Builder b, int x1, int x2, int z, int y, char slope) {
        b.fill(x1, y, z, x2, y, z, slope);
    }

    /**
     * Punches window openings into a wall run and boards most of them.
     *
     * <p>Two thirds boarded, one third glazed, is what makes a terrace read as
     * half-occupied. Doing it per building by hand is how you end up with a
     * building where every window matches.
     *
     * @param y the course the windows sit in
     */
    static void windows(StructureDef.Builder b, int x1, int z1, int x2, int z2, int y) {
        shellScatter(b, x1, z1, x2, z2, y, BOARD, 0.40F);
        shellScatter(b, x1, z1, x2, z2, y, WINDOW, 0.22F);
    }

    /**
     * Lays a security grille over a wall course, on top of whatever
     * {@link #windows} already left there.
     *
     * <p>Shell-shaped for the same reason as everything else here: a box scatter
     * of iron bars over a room's footprint hangs bars in the middle of the room.
     */
    static void grille(StructureDef.Builder b, int x1, int z1, int x2, int z2, int y, float chance) {
        shellScatter(b, x1, z1, x2, z2, y, BARS, chance);
    }

    /**
     * {@link #windows} for a building that does not have four walls.
     *
     * <p>Call this per wall run rather than reaching for {@link #windows} with a
     * clipped rectangle: the rectangle form scatters along all four sides of the
     * box it is given, so on an open-fronted building it hangs boards in the air
     * across the opening.
     */
    static void windowRun(StructureDef.Builder b, int x1, int z1, int x2, int z2, int y) {
        b.scatter(x1, y, z1, x2, y, z2, BOARD, 0.40F);
        b.scatter(x1, y, z1, x2, y, z2, WINDOW, 0.22F);
    }

    /**
     * Scatters a key along the four wall lines of a rectangle and nowhere else.
     *
     * <p>{@code StructureDef.Builder#scatter} works on a solid box, so scattering
     * a fence over a roof's full footprint puts fence posts in the middle of the
     * deck. Four line-shaped scatters keep it on the edge. The corners are
     * covered by two calls, which is harmless: the scatter hash is a pure
     * function of position, so both calls agree.
     */
    private static void shellScatter(StructureDef.Builder b, int x1, int z1, int x2, int z2,
                                     int y, char key, float chance) {
        b.scatter(x1, y, z1, x2, y, z1, key, chance);
        b.scatter(x1, y, z2, x2, y, z2, key, chance);
        b.scatter(x1, y, z1, x1, y, z2, key, chance);
        b.scatter(x2, y, z1, x2, y, z2, key, chance);
    }
}
