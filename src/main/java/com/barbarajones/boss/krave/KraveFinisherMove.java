package com.barbarajones.boss.krave;

/**
 * The six finisher attacks, in the order a form works through them.
 *
 * <p>Form N requires the first N of these, so form one is the single aerial
 * throw that already worked and form six is all of them in sequence. That is
 * the whole rule - there is no per-form table of what happens, because the
 * sequence IS the form number, and a table would be six chances to write down
 * the same thing inconsistently.
 *
 * <p>Ordinals are persisted as the step index on the boss. Append, do not
 * reorder.
 */
public enum KraveFinisherMove {

    /** Kaiden throws the player down into him. The original, unchanged. */
    AERIAL_THROW("G", "KAIDEN HAS YOU"),

    /** The player picks him up and drives him into the ground. */
    GROUND_SLAM("K", "PICK HIM UP"),

    /** Launched into the sky and struck back down as a meteor. */
    METEOR("H", "PUT HIM IN THE SKY"),

    /** Carried through the terrain at speed, then hammered down. */
    GRINDER("J", "DRIVE HIM THROUGH IT"),

    /** Bounced between Kaiden and the player, ending in a midair collision. */
    COMBO("V", "TOGETHER"),

    /** Everything at once, and the end of the encounter. */
    FINALE("B", "END IT");

    private static final KraveFinisherMove[] ORDER = values();

    private final String key;
    private final String caption;

    KraveFinisherMove(String key, String caption) {
        this.key = key;
        this.caption = caption;
    }

    /** The letter shown in the prompt, and the key the client binds. */
    public String key() {
        return this.key;
    }

    /** The line under the prompt, so the move is named rather than anonymous. */
    public String caption() {
        return this.caption;
    }

    /**
     * How many attacks a form must survive.
     *
     * <p>Capped at the number of moves that exist: the Krave God at form seven
     * has no seventh attack to be finished by, so he faces the full six like
     * form six does. Better than inventing a move nobody asked for, and better
     * than an index that walks off the end of the array.
     */
    public static int stepsFor(int form) {
        return Math.max(1, Math.min(form, ORDER.length));
    }

    /** The move at a zero-based step, clamped so a bad index cannot crash a fight. */
    public static KraveFinisherMove atStep(int step) {
        return ORDER[Math.max(0, Math.min(step, ORDER.length - 1))];
    }

    /** Whether this is the last attack of the given form. */
    public static boolean isLastStep(int form, int step) {
        return step >= stepsFor(form) - 1;
    }
}
