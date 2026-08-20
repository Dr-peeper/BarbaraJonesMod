package com.barbarajones.v2.village;

import net.minecraft.network.chat.Component;

/**
 * What one placed block contributes to the settlement around it.
 *
 * <p>This is the extension point behind "every item matters to the village".
 * Any module can teach the village system that its block is worth something:
 *
 * <pre>{@code
 * KraveVillage.registerVillageBuff(MyBlocks.CEREAL_SILO.get(),
 *         VillageBuff.builder()
 *                 .building(2)      // counts as two buildings toward tier
 *                 .production(6)    // +6 Krave/hour
 *                 .happiness(3)
 *                 .description("block.mymod.cereal_silo.village")
 *                 .build());
 * }</pre>
 *
 * <p>Buffs are per-<em>block</em>, not per-blockstate, and they stack per
 * placed block: ten silos give ten times the production. There is a per-village
 * tracking cap ({@link Village#MAX_TRACKED_BUILDINGS}) so that paving a chunk in
 * one buffed block cannot make the save file unbounded.
 *
 * <p>All five channels are independent and all may be zero or negative. A
 * negative {@code happiness} is a legitimate design tool - a Krave Waste Vat
 * should make the neighbours miserable.
 *
 * <ul>
 *   <li>{@code building}   - how many "buildings" this counts as for tier maths.
 *   <li>{@code defence}    - added to the village defence rating, which is what
 *                            reduces raid damage.
 *   <li>{@code production} - Krave produced per real-world hour, before the tier
 *                            and happiness multipliers.
 *   <li>{@code attraction} - how strongly this pulls new Krave Villagers in.
 *   <li>{@code happiness}  - shifts the village's happiness target, 0..100.
 * </ul>
 */
public final class VillageBuff {

    /** The do-nothing buff. Returned instead of null for unregistered blocks. */
    public static final VillageBuff NONE = new VillageBuff(0, 0, 0, 0, 0, null);

    private final int building;
    private final int defence;
    private final int production;
    private final int attraction;
    private final int happiness;
    private final String descriptionKey;

    private VillageBuff(int building, int defence, int production, int attraction,
                        int happiness, String descriptionKey) {
        this.building = building;
        this.defence = defence;
        this.production = production;
        this.attraction = attraction;
        this.happiness = happiness;
        this.descriptionKey = descriptionKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Shorthand for the common "this is a plain house/shed" case:
     * one building, a little attraction, a little cheer, nothing else.
     */
    public static VillageBuff house() {
        return builder().building(1).attraction(2).happiness(1).build();
    }

    /** Shorthand for a defensive fixture. */
    public static VillageBuff fortification(int defence) {
        return builder().building(1).defence(defence).build();
    }

    public int building() {
        return this.building;
    }

    public int defence() {
        return this.defence;
    }

    public int production() {
        return this.production;
    }

    public int attraction() {
        return this.attraction;
    }

    public int happiness() {
        return this.happiness;
    }

    /** May be null. A lang key describing what this block does for a village. */
    public String descriptionKey() {
        return this.descriptionKey;
    }

    public boolean isNothing() {
        return this.building == 0 && this.defence == 0 && this.production == 0
                && this.attraction == 0 && this.happiness == 0;
    }

    public Component describe() {
        return this.descriptionKey == null
                ? Component.translatable("village.barbarajones.buff.generic")
                : Component.translatable(this.descriptionKey);
    }

    @Override
    public String toString() {
        return "VillageBuff[building=" + building + ", defence=" + defence
                + ", production=" + production + ", attraction=" + attraction
                + ", happiness=" + happiness + "]";
    }

    /** Fluent builder; every channel defaults to zero. */
    public static final class Builder {
        private int building;
        private int defence;
        private int production;
        private int attraction;
        private int happiness;
        private String descriptionKey;

        private Builder() { }

        public Builder building(int value) {
            this.building = value;
            return this;
        }

        public Builder defence(int value) {
            this.defence = value;
            return this;
        }

        public Builder production(int value) {
            this.production = value;
            return this;
        }

        public Builder attraction(int value) {
            this.attraction = value;
            return this;
        }

        public Builder happiness(int value) {
            this.happiness = value;
            return this;
        }

        public Builder description(String langKey) {
            this.descriptionKey = langKey;
            return this;
        }

        public VillageBuff build() {
            return new VillageBuff(building, defence, production, attraction, happiness, descriptionKey);
        }
    }
}
