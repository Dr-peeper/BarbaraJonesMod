package com.barbarajones.v2.mayor;

import com.barbarajones.v2.village.Village;

/**
 * How far Barbara has got as mayor. Seven rungs, Squatter to Kingpin.
 *
 * <p>This is <em>her</em> progression, and it is deliberately not the same thing
 * as {@code VillageTier}. The two answer different questions and are allowed to
 * disagree:
 *
 * <ul>
 *   <li>{@code VillageTier} is <b>derived</b> from what is standing in the claim
 *       right now - buildings, residents, defence - and can go down if the
 *       player pulls the town apart. It gates the Krave portal.
 *   <li>{@code MayorRank} is <b>earned</b> and stored: it counts the projects
 *       Barbara has actually finished and never falls. It gates what she is
 *       willing to attempt next, and how many people she reckons she can feed.
 * </ul>
 *
 * <p>They interlock at exactly one point, and only in one direction: the mayor's
 * {@link #residentSupport()} is a <em>floor</em> taken against the village's own
 * {@code VillageTier#populationCap()}, so the mayor is always the more
 * conservative of the two and can never push a settlement past the cap the
 * village module already enforces. Two systems owning the same number and
 * quietly disagreeing about it is the failure this arrangement exists to avoid -
 * see {@code KraveMayor#residentCap}.
 *
 * <h2>The table</h2>
 * <pre>
 *  rank                clout  residents  jobs  radius  gap  road rung
 *  0 SQUATTER              0          2     1      14    8  0 track
 *  1 BLOCK_CAPTAIN         3          5     1      20    7  1 worn
 *  2 SLUMLORD              8          8     2      26    6  2 patchwork
 *  3 DEPUTY_MAYOR         16         12     2      32    5  3 potholed
 *  4 MAYOR                28         16     3      38    4  4 encroached
 *  5 MAYOR_FOR_LIFE       44         20     3      42    3  4 encroached
 *  6 KRAVE_KINGPIN        65         24     3      44    2  4 encroached
 * </pre>
 *
 * <p>Read the last two columns together, because they are the aesthetic brief
 * expressed as numbers. The gap between buildings <em>shrinks</em> as the rank
 * climbs - eight blocks of breathing room at the bottom, two at the top, which
 * is buildings close enough to share an alley - and the road rung stops
 * improving at the point where somebody builds a house into it and stays there.
 * A bigger Krave Village is a more crowded and a more broken one.
 */
public enum MayorRank {

    /** Living in it, not running it. One shack at a time and a footpath. */
    SQUATTER(0, 0, "Squatter", 2, 1, 14, 8),
    /** Somebody has to be in charge of this street and it may as well be her. */
    BLOCK_CAPTAIN(1, 3, "Block Captain", 5, 1, 20, 7),
    /** Collecting rent on buildings she has no paperwork for. */
    SLUMLORD(2, 8, "Slumlord", 8, 2, 26, 6),
    /** Deputy to nobody. The title was self-awarded and nobody argued. */
    DEPUTY_MAYOR(3, 16, "Deputy Mayor", 12, 2, 32, 5),
    /** Mayor. There was no election; there was also no other candidate. */
    MAYOR(4, 28, "Mayor", 16, 3, 38, 4),
    /** The term limits were repealed by the person the term limits applied to. */
    MAYOR_FOR_LIFE(5, 44, "Mayor For Life", 20, 3, 42, 3),
    /** Runs the town, the corner and the cereal. */
    KRAVE_KINGPIN(6, 65, "Kingpin of Krave", 24, 3, 44, 2);

    private static final MayorRank[] BY_INDEX = values();

    /**
     * Nothing may be sited further from the origin than this, whatever the rank
     * says, so that a settlement can never grow out of the claim the village
     * module is tracking. Every {@link #settlementRadius()} in the table above is
     * under it with room for the largest footprint.
     */
    public static final int MAX_RADIUS = Village.CLAIM_RADIUS - 8;

    private final int index;
    private final int cloutRequired;
    private final String title;
    private final int residentSupport;
    private final int concurrentProjects;
    private final int settlementRadius;
    private final int buildingGap;

    MayorRank(int index, int cloutRequired, String title, int residentSupport,
              int concurrentProjects, int settlementRadius, int buildingGap) {
        this.index = index;
        this.cloutRequired = cloutRequired;
        this.title = title;
        this.residentSupport = residentSupport;
        this.concurrentProjects = concurrentProjects;
        this.settlementRadius = settlementRadius;
        this.buildingGap = buildingGap;
    }

    public int index() {
        return this.index;
    }

    /** Finished-project score needed to reach this rung. */
    public int cloutRequired() {
        return this.cloutRequired;
    }

    /** What she calls herself. Shown on every line of the report. */
    public String title() {
        return this.title;
    }

    /**
     * How many residents Barbara reckons she can keep fed.
     *
     * <p>A ceiling on what the mayor will house, never a target and never an
     * authority: the real cap is the smaller of this and the village's own tier
     * cap. See the class javadoc.
     */
    public int residentSupport() {
        return this.residentSupport;
    }

    /** How many projects may be physically going up at once. */
    public int concurrentProjects() {
        return this.concurrentProjects;
    }

    /** How far from the village origin she will site anything. */
    public int settlementRadius() {
        return Math.min(this.settlementRadius, MAX_RADIUS);
    }

    /**
     * Clear space demanded between two of her buildings, beyond their own
     * footprints. Shrinks with rank on purpose - this is the number that turns a
     * scattering of huts into a warren of alleys.
     */
    public int buildingGap() {
        return this.buildingGap;
    }

    /**
     * Which rung of road decay she is laying now, capped at the worst one.
     *
     * <p>The cap is the point: the last three ranks all lay the rung where a
     * building has been put up in the carriageway. Roads in this town do not
     * come back.
     */
    public int roadStage() {
        return Math.min(com.barbarajones.v2.mayor.def.MayorPrefabs.ROAD_STAGES - 1, this.index);
    }

    public MayorRank next() {
        return this.index + 1 < BY_INDEX.length ? BY_INDEX[this.index + 1] : null;
    }

    public static MayorRank byIndex(int index) {
        if (index < 0) {
            return SQUATTER;
        }
        return index >= BY_INDEX.length ? BY_INDEX[BY_INDEX.length - 1] : BY_INDEX[index];
    }

    /** The highest rung whose clout requirement is met. Never falls below SQUATTER. */
    public static MayorRank forClout(int clout) {
        MayorRank best = SQUATTER;
        for (MayorRank rank : BY_INDEX) {
            if (clout >= rank.cloutRequired) {
                best = rank;
            }
        }
        return best;
    }
}
