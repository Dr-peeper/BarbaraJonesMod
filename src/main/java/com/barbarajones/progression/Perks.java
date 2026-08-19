package com.barbarajones.progression;

import com.barbarajones.entity.CaydenCobb;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * What levelling up actually buys you.
 *
 * <p>Perks are not stored anywhere: they are a pure function of the player's
 * Krave level, so there is no perk state to save, migrate or lose. Query them
 * from wherever they matter - {@link #caydenBonusHealth(Player)} in Cayden's
 * stat maths, {@link #plugShortchangeChance(Player)} where The Plug decides
 * whether to short you, and so on.
 *
 * <p>Only one perk is applied from inside this class ({@link #applyToCayden},
 * which the progression tick sweep calls). The rest are queries the systems
 * that own that behaviour read when they need them - that keeps the perk table
 * in one place instead of scattering level checks through the whole mod.
 */
public final class Perks {

    /** Fixed id so the max-health bonus is re-found and replaced, never stacked. */
    private static final UUID CAYDEN_HEALTH_ID =
            UUID.fromString("6b1f9c2e-5a83-4b7d-9e14-0c3f7a2d8b51");
    private static final String CAYDEN_HEALTH_NAME = "Krave level perk";

    /** The Plug's base odds of shorting you on a grass deal, before any perk. */
    public static final float BASE_SHORTCHANGE = 0.25F;
    /** Barbara's base grudge, in ticks: 20 seconds of pure indignation. */
    public static final int BASE_GRUDGE_TICKS = 400;

    /** Every perk, in unlock order. The GUI can render this table straight out. */
    public enum Perk {
        STREET_SMART(2, "Street Smart",
                "The Plug thinks twice before shorting your bag."),
        BIG_BROTHER(4, "Big Brother",
                "Cayden carries 4 more hearts' worth of health."),
        CALM_HANDS(6, "Calm Hands",
                "Barbara gets over it faster when you touch her stash."),
        SWEET_TOOTH(8, "Sweet Tooth",
                "Krave tools misbehave a little less often."),
        GUARDIAN(10, "Guardian",
                "Cayden is noticeably harder to kill."),
        HAGGLER(12, "Haggler",
                "The Plug rarely gets away with weighing it his way."),
        THERAPIST(14, "Therapist",
                "Barbara's grudges burn out in twelve seconds."),
        SUGAR_TOLERANCE(16, "Sugar Tolerance",
                "The Krave curses hit you a third less often."),
        BODYGUARD(18, "Bodyguard",
                "Cayden is built like the front door now."),
        KOSMONAUT(20, "Kosmonaut",
                "Surviving an apocalypse pays 50% more Krave XP."),
        NO_SCAMS(22, "No Scams",
                "The Plug almost never shorts you."),
        SAINT(24, "Saint",
                "Barbara forgives you in eight seconds flat."),
        IRON_STOMACH(26, "Iron Stomach",
                "Half the Krave curses simply do not fire."),
        KRAVE_KING(28, "Krave King",
                "Cayden has more health than most bosses."),
        I_KRAVE_THE_KRAVE(30, "I KRAVE THE KRAVE",
                "The Plug will not short you at all, Barbara lets it go, "
                        + "and the curses are mostly quiet.");

        /** Krave level at which this perk turns on. */
        public final int level;
        public final String title;
        public final String blurb;

        Perk(int level, String title, String blurb) {
            this.level = level;
            this.title = title;
            this.blurb = blurb;
        }
    }

    private Perks() { }

    // ---- perk lookups -------------------------------------------------------

    /** The perk unlocked exactly at this level, or null if that level grants none. */
    @Nullable
    public static Perk unlockedAt(int level) {
        for (Perk perk : Perk.values()) {
            if (perk.level == level) {
                return perk;
            }
        }
        return null;
    }

    public static boolean has(Player player, Perk perk) {
        return KraveLevel.getLevel(player) >= perk.level;
    }

    /** Everything this player currently has, in unlock order. For the GUI. */
    public static List<Perk> unlocked(Player player) {
        int level = KraveLevel.getLevel(player);
        List<Perk> out = new ArrayList<>();
        for (Perk perk : Perk.values()) {
            if (level >= perk.level) {
                out.add(perk);
            }
        }
        return out;
    }

    /** The next perk this player has not earned yet, or null once they hold them all. */
    @Nullable
    public static Perk next(Player player) {
        int level = KraveLevel.getLevel(player);
        for (Perk perk : Perk.values()) {
            if (level < perk.level) {
                return perk;
            }
        }
        return null;
    }

    // ---- the actual effects (query these from the systems that own them) ----

    /**
     * Extra max health to hand Cayden on top of his fed-based stats. Applied
     * automatically by {@link #applyToCayden}; exposed so anything else that
     * reasons about how tanky he is agrees with the HUD.
     */
    public static double caydenBonusHealth(Player player) {
        int level = KraveLevel.getLevel(player);
        if (level >= Perk.KRAVE_KING.level) {
            return 18.0D;
        } else if (level >= Perk.BODYGUARD.level) {
            return 12.0D;
        } else if (level >= Perk.GUARDIAN.level) {
            return 8.0D;
        } else if (level >= Perk.BIG_BROTHER.level) {
            return 4.0D;
        }
        return 0.0D;
    }

    /**
     * Odds (0..1) that The Plug halves a grass order. Read this instead of the
     * hard-coded 1-in-4 so a levelled player actually gets what they paid for.
     */
    public static float plugShortchangeChance(Player player) {
        int level = KraveLevel.getLevel(player);
        if (level >= Perk.I_KRAVE_THE_KRAVE.level) {
            return 0.0F;
        } else if (level >= Perk.NO_SCAMS.level) {
            return 0.05F;
        } else if (level >= Perk.HAGGLER.level) {
            return 0.10F;
        } else if (level >= Perk.STREET_SMART.level) {
            return 0.18F;
        }
        return BASE_SHORTCHANGE;
    }

    /** Convenience roll for {@link #plugShortchangeChance}. */
    public static boolean rollShortchange(Player player, net.minecraft.util.RandomSource random) {
        return random.nextFloat() < plugShortchangeChance(player);
    }

    /**
     * How long Barbara stays furious after you smoke her stash, in ticks.
     * Shorter at higher levels: she has come to expect this from you.
     */
    public static int barbaraGrudgeTicks(Player player) {
        int level = KraveLevel.getLevel(player);
        if (level >= Perk.I_KRAVE_THE_KRAVE.level) {
            return 120;
        } else if (level >= Perk.SAINT.level) {
            return 160;
        } else if (level >= Perk.THERAPIST.level) {
            return 240;
        } else if (level >= Perk.CALM_HANDS.level) {
            return 320;
        }
        return BASE_GRUDGE_TICKS;
    }

    /**
     * Multiplier (0..1) on how often a Krave tool's curse fires. The tools stay
     * cursed forever - the point of the joke - but a level 26 player is not
     * eating cereal every eight blocks any more.
     */
    public static float kraveCurseChance(Player player) {
        int level = KraveLevel.getLevel(player);
        if (level >= Perk.I_KRAVE_THE_KRAVE.level) {
            return 0.35F;
        } else if (level >= Perk.IRON_STOMACH.level) {
            return 0.50F;
        } else if (level >= Perk.SUGAR_TOLERANCE.level) {
            return 0.70F;
        } else if (level >= Perk.SWEET_TOOTH.level) {
            return 0.85F;
        }
        return 1.0F;
    }

    /** Whether a Krave curse should fire this time, given the perk softening. */
    public static boolean rollKraveCurse(Player player, net.minecraft.util.RandomSource random) {
        float chance = kraveCurseChance(player);
        return chance >= 1.0F || random.nextFloat() < chance;
    }

    /** XP multiplier applied to apocalypse survival payouts. */
    public static float apocalypseXpMultiplier(Player player) {
        return has(player, Perk.KOSMONAUT) ? 1.5F : 1.0F;
    }

    // ---- application --------------------------------------------------------

    /**
     * Push the max-health perk onto one of this player's Caydens.
     *
     * <p>Uses a TRANSIENT modifier on purpose: a permanent one would be written
     * into Cayden's save data, and re-derived every session on top of whatever
     * was already stored. Transient means the value is rebuilt from the player's
     * current level each time the world loads and can never double up.
     */
    public static void applyToCayden(ServerPlayer owner, CaydenCobb cayden) {
        AttributeInstance health = cayden.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        double bonus = caydenBonusHealth(owner);
        AttributeModifier existing = health.getModifier(CAYDEN_HEALTH_ID);
        if (existing != null && existing.getAmount() == bonus) {
            return;                                    // already correct - nothing to do
        }
        if (existing != null) {
            health.removeModifier(CAYDEN_HEALTH_ID);
        }
        if (bonus > 0.0D) {
            health.addTransientModifier(new AttributeModifier(CAYDEN_HEALTH_ID, CAYDEN_HEALTH_NAME,
                    bonus, AttributeModifier.Operation.ADDITION));
            // fill the hearts the perk just added, so the gain is visible immediately
            cayden.heal((float) bonus);
        }
        // changing max health does not itself clamp current health, and a shrinking
        // bonus (an operator resetting XP) would otherwise leave him over-full
        cayden.setHealth(Math.min(cayden.getHealth(), cayden.getMaxHealth()));
    }
}
