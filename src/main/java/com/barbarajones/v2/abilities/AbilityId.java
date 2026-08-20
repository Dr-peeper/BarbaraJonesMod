package com.barbarajones.v2.abilities;

/**
 * The six powers Cayden's ascension ladder has always kept to himself, now on
 * a table the player's side of the mod can read too.
 *
 * <p>Mirrors how {@code AscensionLadder} works: one enum both the items and
 * the HUD read, so a cooldown can never say one number on the icon and charge
 * another on the server. {@code index} is also the wire order for every
 * ability packet - append only, never reorder.
 *
 * <p>Unlock gating itself lives in {@code AbilityUnlocks} and
 * {@code AbilityEvents} - which quest id or boss kill grants which ability,
 * including {@link #GODCORE}'s extra path through the newer quest module's
 * own {@code i_krave_the_krave} key. Nothing about that needs storing per
 * entry here.
 *
 * <ul>
 *   <li><b>cooldownTicks</b> - server ticks between activations.</li>
 *   <li><b>activeTicks</b> - how long the effect keeps running once triggered,
 *       for the three abilities that open a window rather than firing once.
 *       Zero means "resolves instantly, nothing to keep ticking".</li>
 * </ul>
 */
public enum AbilityId {

    /** The knockback flash, on demand: a charged punch that launches whatever it hits. */
    GAUNTLET(0, "krave_gauntlet", "Krave Gauntlet", 60, 0),
    /** The red Krave laser Cayden fires at Super Saiyan and up. */
    LASER(1, "laser_lens", "Laser Lens", 40, 0),
    /** Cayden's short bursts of flight, borrowed for a few seconds at a time. */
    CHARM(2, "ascension_charm", "Ascension Charm", 200, 60),
    /** Calls the apocalypse's own meteor down on wherever you are looking. */
    TOTEM(3, "meteor_totem", "Meteor Totem", 600, 0),
    /** Ultra Instinct's "the body answers before he does" - total, for a few seconds. */
    BAND(4, "instinct_band", "Instinct Band", 1200, 60),
    /** The Super Saiyan God aura, worn instead of transformed into. */
    GODCORE(5, "god_core", "God Core", 2400, 600);

    /** Wire order for every ability packet. Do not renumber existing entries. */
    public final int index;
    /** Registry name (lower snake case) - matches the item id exactly. */
    public final String id;
    /** Player-facing name, for chat lines and the HUD. */
    public final String displayName;
    /** Ticks between activations. */
    public final int cooldownTicks;
    /** Ticks the effect stays active once triggered; 0 for an instant effect. */
    public final int activeTicks;

    AbilityId(int index, String id, String displayName, int cooldownTicks, int activeTicks) {
        this.index = index;
        this.id = id;
        this.displayName = displayName;
        this.cooldownTicks = cooldownTicks;
        this.activeTicks = activeTicks;
    }

    public static final AbilityId[] VALUES = values();
    public static final int COUNT = VALUES.length;

    public static AbilityId byIndex(int i) {
        return i >= 0 && i < VALUES.length ? VALUES[i] : null;
    }

    public boolean hasActiveWindow() {
        return this.activeTicks > 0;
    }
}
