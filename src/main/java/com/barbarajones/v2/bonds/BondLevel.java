package com.barbarajones.v2.bonds;

import net.minecraft.ChatFormatting;

/**
 * How close a fed companion is to the player, on a five-rung ladder that both
 * Cayden and Barbara climb (off two different counters - his lifetime Krave
 * boxes, her lifetime gifts of grass - see {@link BondState}).
 *
 * <p>This is deliberately separate from Cayden's own ascension ladder
 * ({@code AscensionLadder} in {@code com.barbarajones.progression}): that one
 * is about combat forms he has to be taught and can lose (power down). Bond
 * level never goes down - it is a running total of how much the player has
 * put into the relationship, and it is what the escalating passive buffs in
 * {@link BondBuffs} scale off.
 */
public enum BondLevel {

    STRANGER(0, "Stranger", ChatFormatting.GRAY, 0),
    REGULAR(1, "Regular", ChatFormatting.WHITE, 1),
    RIDE_OR_DIE(2, "Ride-or-Die", ChatFormatting.YELLOW, 2),
    BONDED_FOR_LIFE(3, "Bonded for Life", ChatFormatting.GOLD, 3),
    KRAVE_SOULMATE(4, "Krave Soulmate", ChatFormatting.LIGHT_PURPLE, 4);

    private final int index;
    private final String displayName;
    private final ChatFormatting color;
    /** How many filled stars this level shows out of {@link #MAX_STARS}. */
    private final int stars;

    public static final int MAX_STARS = 4;

    BondLevel(int index, String displayName, ChatFormatting color, int stars) {
        this.index = index;
        this.displayName = displayName;
        this.color = color;
        this.stars = stars;
    }

    public int index() {
        return this.index;
    }

    public String displayName() {
        return this.displayName;
    }

    public ChatFormatting color() {
        return this.color;
    }

    /** A compact "current/next" thermometer, e.g. "[***.]" - used in nameplates and toasts. */
    public String stars() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < MAX_STARS; i++) {
            sb.append(i < this.stars ? '★' : '☆');   // ★ / ☆
        }
        return sb.toString();
    }

    public boolean isMax() {
        return this == KRAVE_SOULMATE;
    }

    public BondLevel next() {
        BondLevel[] all = values();
        return this.index + 1 < all.length ? all[this.index + 1] : this;
    }

    /**
     * Cayden's thresholds run off {@code getKraveFed()} - a lifetime counter
     * that never resets, even across his apocalypse respawns (see
     * {@code CaydenCobb.ASCENSION_LEGACY} / {@code restoreKrave}). Rung 2 lines
     * up deliberately with {@code CaydenCobb.RAGE_THRESHOLD} (25): the same
     * feeding milestone that unlocks Krave Rage is the one that makes the bond
     * official.
     */
    private static final int[] CAYDEN_THRESHOLDS = { 0, 8, 25, 50, 90 };

    /**
     * Barbara has no lifetime counter of her own - her stash decays - so
     * {@link BondState} keeps a shadow counter of every gift of grass she has
     * ever been handed, and these thresholds run off that.
     */
    private static final int[] BARBARA_THRESHOLDS = { 0, 4, 10, 20, 35 };

    public static BondLevel forCaydenFed(int lifetimeFed) {
        return forCount(lifetimeFed, CAYDEN_THRESHOLDS);
    }

    public static BondLevel forBarbaraGifts(int lifetimeGifts) {
        return forCount(lifetimeGifts, BARBARA_THRESHOLDS);
    }

    /** How many more (Krave boxes / grass gifts) until the next rung, or -1 at max. */
    public static int untilNextCayden(int lifetimeFed) {
        return untilNext(lifetimeFed, CAYDEN_THRESHOLDS);
    }

    public static int untilNextBarbara(int lifetimeGifts) {
        return untilNext(lifetimeGifts, BARBARA_THRESHOLDS);
    }

    private static BondLevel forCount(int count, int[] thresholds) {
        BondLevel level = STRANGER;
        for (BondLevel l : values()) {
            if (count >= thresholds[l.index]) {
                level = l;
            }
        }
        return level;
    }

    private static int untilNext(int count, int[] thresholds) {
        BondLevel level = forCount(count, thresholds);
        if (level.isMax()) {
            return -1;
        }
        return thresholds[level.index + 1] - count;
    }
}
