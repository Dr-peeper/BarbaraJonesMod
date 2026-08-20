package com.barbarajones.v2.village;

import net.minecraft.network.chat.Component;

/**
 * How developed a settlement is. Six rungs, WILDERNESS through KRAVE_CAPITAL.
 *
 * <p>A village's tier is <em>derived</em>, never stored as the truth - it is
 * recomputed from buildings, population and defence every village tick and
 * cached on {@link Village}. That means a tier can go <em>down</em> if the
 * player tears their own town apart, which is deliberate: the Krave dimension
 * gate has to mean "you currently have a real settlement", not "you once did".
 *
 * <p>Requirements are ALL-of, not any-of. A village with forty buildings and
 * one resident is still a CAMP, because the buildings are empty.
 *
 * <p>The quest module gates the Krave dimension portal on {@link #VILLAGE}; the
 * constant lives on {@link KraveVillage#PORTAL_TIER} so nothing has to hard-code
 * the ordinal.
 */
public enum VillageTier {

    /** No settlement worth the name. Nothing is gated behind this. */
    WILDERNESS(0,  0,  0,  0,  2, 0.5F, "wilderness"),
    /** A few crates and a bedroll. Attraction has started. */
    CAMP(1,        3,  1,  0,  4, 1.0F, "camp"),
    /** Enough roofs that people stay. First real Krave production. */
    HAMLET(2,      8,  3,  4,  7, 1.4F, "hamlet"),
    /** The gate tier - a working village. The Krave portal opens here. */
    VILLAGE(3,    16,  6, 12, 11, 1.9F, "village"),
    /** Walls, guards, a market. Raids start bouncing off. */
    TOWN(4,       28, 10, 26, 16, 2.5F, "town"),
    /** The end state. Cayden has somewhere to live that is not a cardboard box. */
    KRAVE_CAPITAL(5, 44, 15, 45, 22, 3.2F, "capital");

    private static final VillageTier[] BY_INDEX = values();

    private final int index;
    private final int requiredBuildings;
    private final int requiredPopulation;
    private final int requiredDefence;
    private final int populationCap;
    private final float productionMultiplier;
    private final String key;

    VillageTier(int index, int requiredBuildings, int requiredPopulation, int requiredDefence,
                int populationCap, float productionMultiplier, String key) {
        this.index = index;
        this.requiredBuildings = requiredBuildings;
        this.requiredPopulation = requiredPopulation;
        this.requiredDefence = requiredDefence;
        this.populationCap = populationCap;
        this.productionMultiplier = productionMultiplier;
        this.key = key;
    }

    /** 0..5. This is what {@link KraveVillage#tierOf} returns. */
    public int index() {
        return this.index;
    }

    public int requiredBuildings() {
        return this.requiredBuildings;
    }

    public int requiredPopulation() {
        return this.requiredPopulation;
    }

    public int requiredDefence() {
        return this.requiredDefence;
    }

    /** How many Krave Villagers this tier will let attraction spawn. */
    public int populationCap() {
        return this.populationCap;
    }

    /** Multiplies raw Krave production. A capital makes triple what a camp does. */
    public float productionMultiplier() {
        return this.productionMultiplier;
    }

    /** Translation key, e.g. {@code village.barbarajones.tier.village}. */
    public String translationKey() {
        return "village.barbarajones.tier." + this.key;
    }

    public Component displayName() {
        return Component.translatable(translationKey());
    }

    public static VillageTier byIndex(int index) {
        if (index < 0) {
            return WILDERNESS;
        }
        return index >= BY_INDEX.length ? BY_INDEX[BY_INDEX.length - 1] : BY_INDEX[index];
    }

    /** The tier immediately above this one, or {@code null} at the top. */
    public VillageTier next() {
        return this.index + 1 < BY_INDEX.length ? BY_INDEX[this.index + 1] : null;
    }

    /**
     * The highest tier whose requirements are all met.
     *
     * <p>Walks down from the top so a village that overshoots one requirement
     * but not another lands on the correct rung rather than the first match.
     */
    public static VillageTier evaluate(int buildings, int population, int defence) {
        for (int i = BY_INDEX.length - 1; i >= 0; i--) {
            VillageTier t = BY_INDEX[i];
            if (buildings >= t.requiredBuildings
                    && population >= t.requiredPopulation
                    && defence >= t.requiredDefence) {
                return t;
            }
        }
        return WILDERNESS;
    }
}
