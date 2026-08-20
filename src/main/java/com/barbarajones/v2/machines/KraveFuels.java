package com.barbarajones.v2.machines;

import net.minecraft.world.item.ItemStack;

import com.barbarajones.v2.economy.KraveEconomy;

/**
 * The whole power system: machines burn Krave Syrup.
 *
 * <p>This is deliberately not an energy API. There is no network, no cables, no
 * joules, no capability to negotiate - a machine has a fuel slot and a counter of
 * remaining syrup units, and it spends one unit per tick of progress. That is the
 * same mental model as a furnace, which means a player who has ever used a
 * furnace already understands every machine in this module, and it means the
 * module does not drag a second energy standard into a mod that has none.
 *
 * <p>Two grades, so there is a reason to keep refining:
 * <ul>
 *   <li>Krave Syrup - 1600 units, roughly thirteen Grinder runs.</li>
 *   <li>Dense Krave Syrup - 8000 units, five syrups' worth in one slot.</li>
 * </ul>
 *
 * <p>Vanilla furnace fuels are deliberately NOT accepted. Coal in a Krave Mixer
 * would be a worse joke than syrup in one, and keeping the fuel exclusive is what
 * makes the syrup line worth automating.
 */
public final class KraveFuels {

    public static final int SYRUP_UNITS = 1600;
    public static final int DENSE_SYRUP_UNITS = 8000;

    private KraveFuels() { }

    /** Syrup units this stack is worth, or 0 if it is not a machine fuel. */
    public static int burnUnits(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        // barbarajones:krave_syrup belongs to the economy module - see the note in
        // KraveMachines. This module consumes that id rather than minting a rival.
        if (stack.is(KraveEconomy.KRAVE_SYRUP.get())) {
            return SYRUP_UNITS;
        }
        if (stack.is(KraveMachines.DENSE_KRAVE_SYRUP.get())) {
            return DENSE_SYRUP_UNITS;
        }
        return 0;
    }

    public static boolean isFuel(ItemStack stack) {
        return burnUnits(stack) > 0;
    }
}
