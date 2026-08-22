package com.barbarajones.v2.plug;

import net.minecraft.util.RandomSource;

/**
 * The seven things you can pay The Plug to go and do.
 *
 * <p>A job is four numbers and a table: what he calls it, what it costs up
 * front, roughly how long he is gone, and the six rows of results in
 * {@link PlugResults}. Nothing here knows anything about how good he is - that
 * lives entirely in {@link PlugCompetence}, which picks which row gets rolled.
 *
 * <p>Order matters: the constants run cheapest-first because sneaking with an
 * empty hand walks this list in declaration order, and a new player should meet
 * the two-band errand before the eight-band one.
 */
public enum PlugJob {

    GATHER_WOOD("gather wood", "get you some wood", 2, 60, PlugResults.WOOD),
    GET_FOOD("get food", "find you some food", 3, 75, PlugResults.FOOD),
    BUILDING_SUPPLIES("gather building supplies", "come back with buildin supplies",
            3, 90, PlugResults.BUILDING),
    FIND_IRON("find iron", "find you some iron", 4, 105, PlugResults.IRON),
    CRAVE_RUN("collect Krave", "do a Krave run", 5, 120, PlugResults.KRAVE),
    FIND_EMERALDS("find emeralds", "find you emeralds", 6, 150, PlugResults.EMERALDS),
    MINE_DIAMONDS("mine diamonds", "go get diamonds", 8, 180, PlugResults.DIAMONDS);

    private final String label;
    private final String pitch;
    private final int price;
    private final int baseSeconds;
    private final PlugPayout[][] outcomes;

    PlugJob(String label, String pitch, int price, int baseSeconds, PlugPayout[][] outcomes) {
        if (outcomes.length != PlugCompetence.TIERS) {
            // Fail at class load rather than in front of a player: a short table
            // means rollTier can index off the end on a lucky roll, which would
            // only ever show up as a rare, unreproducible crash mid-job.
            throw new IllegalStateException("PlugJob " + label + " needs exactly "
                    + PlugCompetence.TIERS + " result tiers, has " + outcomes.length);
        }
        this.label = label;
        this.pitch = pitch;
        this.price = price;
        this.baseSeconds = baseSeconds;
        this.outcomes = outcomes;
    }

    /** How the job appears on the menu. */
    public String label() {
        return this.label;
    }

    /** How he describes taking the job, in his own words. */
    public String pitch() {
        return this.pitch;
    }

    /** Up-front cost in bands. See {@link PlugCurrency} for what a band is worth. */
    public int price() {
        return this.price;
    }

    /** Duration before competence scaling. {@link PlugCompetence#jobTicks} does the scaling. */
    public int baseSeconds() {
        return this.baseSeconds;
    }

    /** One randomly chosen result from the given tier. */
    public PlugPayout payout(int tier, RandomSource random) {
        PlugPayout[] row = this.outcomes[Math.max(0, Math.min(PlugCompetence.TIERS - 1, tier))];
        return row[random.nextInt(row.length)];
    }

    /** The next job on the menu, wrapping round. */
    public PlugJob next() {
        PlugJob[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    /**
     * Reads a job back out of save data by name.
     *
     * <p>Falls back to the cheapest job rather than throwing, because the one
     * thing that must never happen is a renamed constant making an old save
     * unloadable - the player would lose the whole table, not one job.
     */
    public static PlugJob byName(String name) {
        for (PlugJob job : values()) {
            if (job.name().equals(name)) {
                return job;
            }
        }
        return GATHER_WOOD;
    }
}
