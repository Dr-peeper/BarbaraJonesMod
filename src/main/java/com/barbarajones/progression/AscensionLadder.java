package com.barbarajones.progression;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Cayden Cobb's transformation ladder, in one table both sides can read.
 *
 * <p>The entity uses it to decide what a form costs and what it does to his
 * stats; the upgrade screen uses the exact same rows to draw the shop. Keeping
 * one table means a rung can never claim one price in the GUI and charge
 * another on the server.
 *
 * <p>This class is deliberately free of any client import. It is loaded on the
 * dedicated server as part of {@code CaydenCobb}, so nothing here may touch
 * {@code net.minecraft.client}.
 *
 * <p>The tier numbers are the same integers {@code CaydenCobb.getTier()}
 * returns and the aura layer switches on:
 *
 * <pre>
 *   0  base            1  Super Saiyan       2  Super Saiyan 2
 *   3  Super Saiyan 3  4  Super Saiyan God   5  Super Saiyan Blue
 *   6  Ultra Instinct
 * </pre>
 */
public final class AscensionLadder {

    // ---- rung ids ----------------------------------------------------------

    public static final int BASE = 0;
    public static final int SSJ = 1;
    public static final int SSJ2 = 2;
    public static final int SSJ3 = 3;
    public static final int GOD = 4;
    public static final int BLUE = 5;
    public static final int ULTRA = 6;
    /** Highest rung on the ladder. */
    public static final int MAX = ULTRA;

    /** The item that opens the upgrade screen, matched by registry id. */
    public static final ResourceLocation LEDGER_ID =
            new ResourceLocation(BarbaraJonesMod.MODID, "ascension_ledger");

    // ---- ki economy ---------------------------------------------------------

    /** Ki for one box of Krave hand-fed to him. */
    public static final int KI_PER_KRAVE = 1;
    /** Ki for the one-in-a-hundred-thousand golden box. */
    public static final int KI_PER_GOLDEN_KRAVE = 30;
    /** Ki for putting down something that was not worth transforming for. */
    public static final int KI_PER_MOB = 1;

    /** Ki paid out for killing a foe that demanded the given rung. */
    private static final int[] KILL_KI = { KI_PER_MOB, 40, 90, 160, 260, 380, 560 };

    private AscensionLadder() { }

    /**
     * One rung of the ladder.
     *
     * @param tier         the integer {@code getTier()} reports at this rung
     * @param name         display name, e.g. "Super Saiyan Blue"
     * @param tagline      one line of flavour for the upgrade screen
     * @param color        packed ARGB the screen tints this rung with
     * @param kiCost       ki charged to unlock it
     * @param fedRequired  boxes of Krave he must have eaten before it will take
     * @param attackMul    multiplier on his fed-scaled attack damage
     * @param speedMul     multiplier on his BASE movement speed (not the fed one)
     * @param bonusHealth  extra maximum health while he holds this form
     * @param damageTaken  multiplier on incoming damage that actually lands
     * @param dodgePercent percent chance a hit is simply refused outright
     * @param edge         the one mechanical sentence that sells the rung
     */
    public record Rung(int tier, String name, String tagline, int color, int kiCost,
                       int fedRequired, double attackMul, double speedMul,
                       double bonusHealth, double damageTaken, int dodgePercent,
                       String edge) { }

    /**
     * The ladder itself, index == tier.
     *
     * <p>Costs climb roughly geometrically because the rungs do too: Ultra
     * Instinct is thirty times his base punch and it should read as the end of a
     * long climb, not as the next purchase.
     */
    private static final Rung[] RUNGS = {
        new Rung(BASE, "Cayden Cobb", "A runaway kid who eats too much cereal.",
                0xFFA2968B, 0, 0, 1.0D, 1.0D, 0.0D, 1.0D, 0,
                "No aura. No flight. Just a kid with a spoon."),
        new Rung(SSJ, "Super Saiyan", "Gold, loud, and the first time it happens you remember it.",
                0xFFE9B23C, 24, 8, 4.0D, 1.6D, 0.0D, 1.0D, 0,
                "Quadruple damage, flight in bursts, and Krave lasers."),
        new Rung(SSJ2, "Super Saiyan 2", "The crackle around him never stops now.",
                0xFFFFD75E, 70, 20, 7.0D, 1.9D, 4.0D, 1.0D, 0,
                "Adds the standing shockwave that detonates the air every two seconds."),
        new Rung(SSJ3, "Super Saiyan 3", "Taller hair. Thinner patience.",
                0xFFF0C04A, 150, 36, 11.0D, 2.2D, 8.0D, 1.0D, 0,
                "Fast enough to run down something that flies away from him."),
        new Rung(GOD, "Super Saiyan God", "Red, and very quiet about it.",
                0xFFC81E24, 280, 55, 15.5D, 2.5D, 16.0D, 0.62D, 0,
                "Divine body: 38% of all incoming damage simply does not apply."),
        new Rung(BLUE, "Super Saiyan Blue", "God power, held perfectly still.",
                0xFF4FA8FF, 460, 80, 22.0D, 2.9D, 24.0D, 0.48D, 30,
                "Halves what lands and slips one hit in three entirely."),
        new Rung(ULTRA, "Ultra Instinct", "He stops trying.",
                0xFFF4EDDD, 700, 110, 30.0D, 3.4D, 32.0D, 0.55D, 70,
                "The body answers before he does: seven hits in ten never arrive."),
    };

    // ---- lookups ------------------------------------------------------------

    /** The rung for a tier, clamped so a corrupt save can never index out. */
    public static Rung rung(int tier) {
        return RUNGS[Math.max(0, Math.min(MAX, tier))];
    }

    public static String nameOf(int tier) {
        return rung(tier).name();
    }

    /** Ki paid for killing something that demanded {@code demandTier}. */
    public static int kiForKill(int demandTier) {
        return KILL_KI[Math.max(0, Math.min(MAX, demandTier))];
    }

    // ---- unlock bookkeeping -------------------------------------------------
    // Unlocks live in one int as a bitmask, bit N == rung N. That is what gets
    // synched to the client and written to his NBT, so the screen and the save
    // agree without a second field to keep in step.

    public static boolean unlocked(int mask, int tier) {
        return tier <= BASE || (mask & (1 << tier)) != 0;
    }

    public static int withUnlocked(int mask, int tier) {
        return tier <= BASE ? mask : mask | (1 << tier);
    }

    /** Highest contiguous rung the mask allows him to reach. */
    public static int highest(int mask) {
        int best = BASE;
        for (int t = SSJ; t <= MAX; t++) {
            if (!unlocked(mask, t)) {
                break;
            }
            best = t;
        }
        return best;
    }

    public static int countUnlocked(int mask) {
        int n = 0;
        for (int t = SSJ; t <= MAX; t++) {
            if (unlocked(mask, t)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Why a rung cannot be bought right now, or null when it can be.
     *
     * <p>Returned as prose because both the screen and the server chat line want
     * to say the same thing, and there is exactly one place to change it.
     */
    public static String blocker(int tier, int mask, int ki, int kraveFed) {
        if (tier < SSJ || tier > MAX) {
            return "That is not a form.";
        }
        if (unlocked(mask, tier)) {
            return "He already knows it.";
        }
        if (!unlocked(mask, tier - 1)) {
            return "Unlock " + nameOf(tier - 1) + " first.";
        }
        Rung r = rung(tier);
        if (kraveFed < r.fedRequired()) {
            return "He has eaten " + kraveFed + " boxes. This needs " + r.fedRequired() + ".";
        }
        if (ki < r.kiCost()) {
            return "Needs " + (r.kiCost() - ki) + " more Ki.";
        }
        return null;
    }

    // ---- the ledger item ----------------------------------------------------

    /**
     * True for the Ascension Ledger.
     *
     * <p>Matched on registry id rather than a {@code ModItems} constant on
     * purpose: the item's registration line lives in a file this system does not
     * own, so the check has to compile and behave sanely whether or not that line
     * has landed yet.
     */
    public static boolean isLedger(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return LEDGER_ID.equals(id);
    }
}
