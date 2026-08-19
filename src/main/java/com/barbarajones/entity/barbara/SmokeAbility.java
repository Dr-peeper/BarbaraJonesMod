package com.barbarajones.entity.barbara;

/**
 * Barbara's kit. Every move on this list is something she does with a lit
 * blunt, and every move is paid for out of the same grass stash that keeps her
 * from going PSYCHO - so fighting and staying calm draw on one pool and the
 * player has to keep her fed.
 *
 * <p>Costs are in stash ticks, the same unit {@code BarbaraJones} drains at one
 * per tick. A handful of grass is worth 2400, so a full bag buys roughly nine
 * pokes or two of the heavy plays.
 */
public enum SmokeAbility {

    /** Blown rings that sail through a line of mobs and stagger every one. */
    THE_OS("The O's", 350, 60),

    /** She flicks the cherry off the end. Cheap chip damage that sets things alight. */
    LIT_CHERRY("Lit Cherry", 250, 45),

    /** A standing cloud that blinds and bogs down whatever walks into it. */
    SMOKE_SCREEN("Smoke Screen", 900, 220),

    /** Passive haze. Metered per second rather than per cast - see BarbaraCombat. */
    SECOND_HAND("Second-Hand", 40, 20),

    /** She taps the cherry hard: burning ash rings out and shoves everything back. */
    ASH_CLOUD("Ash Cloud", 700, 260),

    /** Retaliation. Whoever just hit her gets the whole lungful, point blank. */
    BLOWBACK("Blowback", 500, 120),

    /** She passes it around. The crew gets faster and tougher, and a little dizzy. */
    CONTACT_HIGH("Contact High", 1200, 700),

    /** The whole bag in one pull. Costs everything she has left. */
    BURNOUT("Burnout", 2000, 900);

    private final String label;
    private final int cost;
    private final int cooldown;

    SmokeAbility(String label, int cost, int cooldown) {
        this.label = label;
        this.cost = cost;
        this.cooldown = cooldown;
    }

    public String label() {
        return this.label;
    }

    /**
     * High Barbara is a generous Barbara - she is not counting what she burns,
     * so everything gets cheaper and comes back sooner. That is the whole trade
     * for how badly she aims in that state.
     */
    public int cost(boolean high) {
        return high ? this.cost * 3 / 5 : this.cost;
    }

    public int cooldown(boolean high) {
        return high ? this.cooldown * 13 / 20 : this.cooldown;
    }
}
