package com.barbarajones.v2.mayor.def;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.resources.ResourceLocation;

import static com.barbarajones.v2.mayor.def.SlumPalette.*;

/**
 * The roads, and the one rule they obey: they only ever get worse.
 *
 * <h2>The progression</h2>
 * Five rungs, laid in this order as Barbara's rank climbs. Read them as a
 * sentence, because that is the design:
 *
 * <ol start="0">
 *   <li><b>TRACK</b> - dirt. Three blocks wide, worn into the ground by people
 *       walking the same line twice a day. Nobody built it.
 *   <li><b>WORN</b> - more dirt. Five wide now, because more people walk it, and
 *       the gravel underneath has started to come through where they do.
 *   <li><b>PATCHWORK</b> - somebody has begun filling holes, with whatever was in
 *       the barrow: cobble, cracked brick, andesite, packed mud, more gravel.
 *       No two patches match and none of them are flush.
 *   <li><b>POTHOLED</b> - the patches have failed. About one square in six is now
 *       a hole with mud in the bottom of it, and there is muck trodden across
 *       everything.
 *   <li><b>ENCROACHED</b> - somebody has built half a house into the carriageway.
 *       The road is three wide again, not because it was narrowed on purpose but
 *       because a wall is standing in it.
 * </ol>
 *
 * <p>There is deliberately no cobblestone-path rung, no stone-brick rung and no
 * paved-avenue rung. The usual settlement-mod progression - dirt, then gravel,
 * then stone, then a lit boulevard - is the exact shape this module exists to
 * refuse. A bigger Krave Village has <em>more</em> road in <em>worse</em>
 * condition, and the rung it ends on has a building growing out of it.
 *
 * <h2>How one segment is built</h2>
 * A segment is 5 wide by 8 long and is drawn entirely at local {@code y = -1} -
 * the ground surface course - so it replaces the topsoil rather than sitting on
 * it. Potholes reach down to {@code y = -2}. Everything is decided by a
 * position hash seeded from the definition id, so a given segment always bakes
 * to the same holes and the same ragged verge; the variation you see between two
 * segments in the world comes from two things, both intentional:
 *
 * <ul>
 *   <li>two lettered variants per rung, whose different ids give different
 *       hashes and therefore genuinely different hole patterns, and
 *   <li>the weighted {@link SlumPalette} surfaces resolving per block at
 *       placement time, so no two stretches of the same variant are made of the
 *       same mud.
 * </ul>
 */
final class RoadKit {

    /** Carriageway width. Never grows; rung 4 takes two of these back. */
    static final int SPAN_X = 5;
    /** How far one Road Expansion project carries the road. */
    static final int SPAN_Z = 8;

    /** Lettered variants per rung. Two is enough to break the repeat. */
    static final String[] VARIANTS = {"a", "b"};
    /** How many rungs of decay there are. */
    static final int STAGES = 5;

    private RoadKit() { }

    static ResourceLocation id(int stage, int variant) {
        int s = Math.max(0, Math.min(STAGES - 1, stage));
        String v = VARIANTS[Math.floorMod(variant, VARIANTS.length)];
        return new ResourceLocation(BarbaraJonesMod.MODID, "mayor_road_" + s + "_" + v);
    }

    // =====================================================================

    static StructureDef build(int stage, int variant) {
        ResourceLocation id = id(stage, variant);
        StructureDef.Builder b = StructureDef.builder(id)
                .palette(BASE)
                .anchor(StructureDef.Anchor.CENTER)
                .ground(StructureDef.GroundMode.LEVEL)
                .maxGroundDelta(4)
                .buildTicks(40);

        // The footprint is declared rather than derived. Ragged verges are drawn
        // with KEEP, which REMOVES positions from the plan - so without this the
        // bounds would shrink to whatever survived the ragging and every segment
        // would be a different width and land in a different place.
        b.minSize(SPAN_X, 1, SPAN_Z);

        int seed = id.hashCode();

        switch (stage) {
            case 0:
                // Three wide, inside a five-wide footprint: the outer columns are
                // levelled and stripped but never surfaced, so the track sits in
                // a band of trampled ground rather than ending at a drawn edge.
                carriageway(b, 1, 3, ROAD_TRACK);
                ragVerge(b, seed, 1, 3, 20);
                break;
            case 1:
                carriageway(b, 0, 4, ROAD_WORN);
                ragVerge(b, seed, 0, 4, 18);
                break;
            case 2:
                carriageway(b, 0, 4, ROAD_PATCH);
                muck(b, seed, 0, 4, 12);
                ragVerge(b, seed, 0, 4, 22);
                break;
            case 3:
                carriageway(b, 0, 4, ROAD_PATCH);
                muck(b, seed, 0, 4, 16);
                potholes(b, seed, 0, 4, 17);
                ragVerge(b, seed, 0, 4, 24);
                break;
            case 4:
            default:
                // What is left of the road, pushed over to one side.
                carriageway(b, 2, 4, ROAD_PATCH);
                muck(b, seed, 2, 4, 18);
                potholes(b, seed, 2, 4, 20);
                ragVerge(b, seed, 2, 4, 26);
                encroachment(b, seed);
                break;
        }

        // Buried under the near end of the segment: a foundation stone in the
        // middle of a road surface would be the one clean block in it.
        b.core(SPAN_X / 2, -2, 0);
        return b.build();
    }

    // ---- the pieces ---------------------------------------------------------

    /** The surface course itself, inclusive in X, the full length in Z. */
    private static void carriageway(StructureDef.Builder b, int x1, int x2, char surface) {
        b.fill(x1, -1, 0, x2, -1, SPAN_Z - 1, surface);
    }

    /**
     * Muck trodden over the top of the surface.
     *
     * <p>Carpets rather than plants: a tall grass on gravel fails its own
     * {@code canSurvive} on the placement engine's settle pass and pops off as a
     * dropped item, so a road built that way would be followed by a trail of
     * item entities. A carpet survives on anything solid.
     */
    private static void muck(StructureDef.Builder b, int seed, int x1, int x2, int percent) {
        for (int z = 0; z < SPAN_Z; z++) {
            for (int x = x1; x <= x2; x++) {
                if (roll(seed, 1, x, z) < percent) {
                    b.set(x, 0, z, MUCK);
                }
            }
        }
    }

    /**
     * Holes. A pothole is air at the surface course with mud in the bottom of
     * it, so it is a real depression you can trip into rather than a differently
     * coloured square. Anything trodden into the surface above the hole is
     * removed with KEEP, because a carpet floating over a hole would have popped
     * off anyway.
     */
    private static void potholes(StructureDef.Builder b, int seed, int x1, int x2, int percent) {
        for (int z = 0; z < SPAN_Z; z++) {
            for (int x = x1; x <= x2; x++) {
                if (roll(seed, 2, x, z) < percent) {
                    b.set(x, -2, z, PUDDLE);
                    b.carve(x, -1, z, x, -1, z);
                    b.keep(x, 0, z, x, 0, z);
                }
            }
        }
    }

    /**
     * Eats bites out of the edge of the carriageway. The bitten columns are
     * KEEP, not air and not dirt - the world simply keeps whatever grass was
     * there, so the road's edge is where the road stopped being maintained
     * rather than a drawn boundary.
     */
    private static void ragVerge(StructureDef.Builder b, int seed, int x1, int x2, int percent) {
        for (int z = 0; z < SPAN_Z; z++) {
            for (int x : new int[]{x1, x2}) {
                if (roll(seed, 3, x, z) < percent) {
                    b.keep(x, -2, z, x, 0, z);
                }
            }
        }
    }

    /**
     * Rung 4: the half-house in the road.
     *
     * <p>Two columns of the old carriageway (local x 0 and 1) are now the end of
     * somebody's house: two blocks thick, so it is all wall and no room, with
     * boarded windows in it and a flat sheeted deck on top. A further wall stands
     * one block out into what is left of the lane, propped with a fence, so the
     * road pinches to two blocks wide as it goes past. Nobody asked, and the road
     * went round it.
     */
    private static void encroachment(StructureDef.Builder b, int seed) {
        final int x1 = 0;
        final int x2 = 1;
        final int z1 = 1;
        final int z2 = 6;

        b.fill(x1, -1, z1, x2, -1, z2, FLOOR);
        b.walls(x1, 0, z1, x2, 2, z2, WALL);
        b.column(x1, z1, 0, 2, POST);
        b.column(x1, z2, 0, 2, POST);
        SlumRoof.windows(b, x1, z1, x2, z2, 1);

        // Flat, like everything else in this town. A pitched roof here would be
        // the one piece of proper building work in the whole road.
        SlumRoof.deck(b, x1, z1, x2, z2, 3);
        b.set(x1, 4, z1 + 1, FENCE);

        // The wall that ended up in the lane, plus the props holding it there.
        int spurZ = z1 + 2 + (roll(seed, 4, 0, 0) % 2);
        b.column(x2 + 1, spurZ, 0, 2, WALL);
        b.set(x2 + 1, -1, spurZ, FLOOR);
        b.set(x2 + 1, 3, spurZ, SLAB);
        b.set(x2 + 1, 0, spurZ + 1, FENCE);
    }

    // ---- the hash -----------------------------------------------------------

    /**
     * A stable 0..99 roll for one position and one purpose.
     *
     * <p>Deliberately not a {@code RandomSource}: definitions are baked once at
     * mod construction and shared by every placement, so a random source here
     * would mean the same road segment had a different hole pattern in two
     * different worlds and no way to reproduce a report about it. The
     * {@code channel} argument keeps the muck, the holes and the verge from
     * correlating with each other.
     */
    private static int roll(int seed, int channel, int x, int z) {
        int h = seed * 31 + channel;
        h = h * 31 + x;
        h = h * 31 + z;
        h ^= h >>> 15;
        h *= 0x2c1b3c6d;
        h ^= h >>> 12;
        return (h >>> 8 & 0xFFFF) % 100;
    }
}
