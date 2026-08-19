package com.barbarajones.client.title;

import java.util.Random;

/**
 * The rotating splash line on the custom title screen.
 *
 * <p>Every line here is something a character actually says, or something said
 * about them, in the source videos. The menu is the mod introducing itself in its
 * own voice before the player has pressed a single button, so nothing generic is
 * allowed in this list.
 */
public final class KraveSplashes {

    private static final String[] LINES = {
        "I KRAVE THE KRAVE!",
        "where's my Chepina?",
        "don't let him die",
        "$500. cash. right now.",
        "that's my OG!",
        "Mr Pibb and a bag of nuggets",
        "the grass is FREE, sugar",
        "Daniel! bring the lighter!",
        "he's got a ski mask on",
        "Nugget did nothing wrong",
        "an ocean of liquid chocolate",
        "backwards red hat gang",
        "the Manager would like a word",
        "Duhl Wol pulled up outside",
        "she smokes what she pulls",
        "check the sewers",
        "one more box. one more.",
        "purple galaxy hoodie",
        "RULE #1: DON'T LET HIM DIE",
        "runaway kid, cereal problem",
        "it's not a drug, it's a cereal",
        "Barbara Jones does not miss",
        "mom's on the phone",
        "the donuts were a trap",
        "Cayden, get in the box",
        "the Krave Kosmos is open",
        "she ran out. run.",
        "fake drugs, real money",
        "no refunds, no receipts",
        "that ain't even grass",
        "put the cereal down",
        "nobody snipes my boy",
    };

    /** How long one line holds the screen before the next takes over. */
    private static final long PERIOD_MS = 7000L;

    /** Fade window at each end of a line's turn, so lines dissolve instead of popping. */
    private static final long FADE_MS = 500L;

    /**
     * Random starting point, chosen once per game launch. Without it the menu would
     * open on the same quote every single time and stop feeling alive by day two.
     */
    private static final int OFFSET = new Random().nextInt(LINES.length);

    private KraveSplashes() { }

    /** The quote showing at this moment. */
    public static String current(long millis) {
        int slot = (int) (((millis / PERIOD_MS) + OFFSET) % LINES.length);
        return LINES[slot];
    }

    /** 0 to 1 opacity for the current quote, ramping in and out of its turn. */
    public static float fade(long millis) {
        long into = millis % PERIOD_MS;
        if (into < FADE_MS) {
            return into / (float) FADE_MS;
        }
        long left = PERIOD_MS - into;
        return left < FADE_MS ? left / (float) FADE_MS : 1.0F;
    }
}
