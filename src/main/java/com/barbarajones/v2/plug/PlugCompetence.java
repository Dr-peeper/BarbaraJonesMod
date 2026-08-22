package com.barbarajones.v2.plug;

import net.minecraft.util.RandomSource;

/**
 * How good he actually is, expressed as one number and three tables.
 *
 * <p>The whole progression is: reputation points -&gt; a competence level 0-5
 * -&gt; a row of {@code TIER_WEIGHTS}. Every job in {@link PlugJob} supplies six
 * rows of results, one per outcome tier, and the competence level only decides
 * <em>which row gets rolled</em>. That is why there is no per-job difficulty
 * code anywhere: adding a job is adding data, not branches.
 *
 * <p>Note what the top row does <b>not</b> do. Level 5 still rolls JUNK six
 * times in a hundred, and the ABSURD column only ever gets to six either. He
 * gets better, he never gets professional - a Plug who reliably returns exactly
 * what you ordered is a vending machine, and nobody tells stories about a
 * vending machine.
 */
public final class PlugCompetence {

    /** Number of outcome tiers every job must supply. Indexes into a job's result table. */
    public static final int TIERS = 6;

    /** Highest competence level. Reputation past the last threshold buys nothing. */
    public static final int MAX_LEVEL = 5;

    /**
     * Reputation needed for each level. Deliberately steepening: the first
     * promotion is a couple of jobs, the last one is a relationship.
     */
    private static final int[] THRESHOLDS = { 0, 12, 30, 55, 90, 140 };

    /**
     * Outcome odds per competence level, in percent. Row = level, column = tier,
     * every row sums to 100 so the roll below can use a flat 0-99.
     */
    private static final int[][] TIER_WEIGHTS = {
            //  JUNK SCRAP PART SOLID HEAVY ABSURD
            {     46,   30,  16,    6,    2,     0 },   // 0 - a stranger with your money
            {     32,   30,  22,   12,    3,     1 },   // 1 - he remembers your face
            {     22,   26,  26,   18,    6,     2 },   // 2 - he answers the phone
            {     14,   20,  26,   26,   11,     3 },   // 3 - he shows up
            {      9,   15,  22,   30,   20,     4 },   // 4 - he shows up early
            {      6,   10,  18,   30,   30,     6 },   // 5 - family, allegedly
    };

    /**
     * Percentage applied to a job's base duration. A bad Plug is slow because he
     * gets lost, gets distracted, and stops for food; a good one already knows
     * where the thing is.
     */
    private static final int[] DURATION_PERCENT = { 150, 132, 114, 96, 80, 66 };

    /** Chance in percent of an unasked-for extra on top of the haul. Nothing below level 2. */
    private static final int[] BONUS_PERCENT = { 0, 0, 4, 10, 18, 28 };

    /** No job ever finishes faster than this, whatever the maths says. He has to at least walk there. */
    private static final int MIN_JOB_TICKS = 300;

    private PlugCompetence() { }

    /** The competence level this much reputation buys. */
    public static int levelFor(int reputation) {
        int level = 0;
        for (int i = 0; i < THRESHOLDS.length; i++) {
            if (reputation >= THRESHOLDS[i]) {
                level = i;
            }
        }
        return level;
    }

    public static int clampLevel(int level) {
        return Math.max(0, Math.min(MAX_LEVEL, level));
    }

    /** Rolls which of a job's six result rows this delivery comes out of. */
    public static int rollTier(int level, RandomSource random) {
        int[] row = TIER_WEIGHTS[clampLevel(level)];
        int roll = random.nextInt(100);
        for (int tier = 0; tier < row.length; tier++) {
            roll -= row[tier];
            if (roll < 0) {
                return tier;
            }
        }
        // Only reachable if a row is edited to sum under 100; the last tier is a
        // saner answer than an index blowing up in front of the player.
        return row.length - 1;
    }

    /**
     * How long this job takes him, in ticks: base duration, scaled by competence,
     * then jittered by up to 15% either way so two identical jobs never feel like
     * a countdown the player can set a clock by.
     */
    public static int jobTicks(PlugJob job, int level, RandomSource random) {
        int scaled = job.baseSeconds() * 20 * DURATION_PERCENT[clampLevel(level)] / 100;
        int jittered = scaled * (85 + random.nextInt(31)) / 100;
        return Math.max(MIN_JOB_TICKS, jittered);
    }

    public static boolean rollBonus(int level, RandomSource random) {
        return random.nextInt(100) < BONUS_PERCENT[clampLevel(level)];
    }

    /** What he calls himself at this level. Shown when the player asks for a status. */
    public static String title(int level) {
        return switch (clampLevel(level)) {
            case 1 -> "somebody you know";
            case 2 -> "your guy";
            case 3 -> "YOUR guy";
            case 4 -> "the connect";
            case 5 -> "family";
            default -> "some dude in a ski mask";
        };
    }
}
