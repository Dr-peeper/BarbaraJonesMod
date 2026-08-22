package com.barbarajones.v2.plug;

import com.barbarajones.content.ModItems;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What The Plug accepts as money, and what it is worth to him.
 *
 * <p>Nothing new is registered for this. He already takes emeralds off you for
 * grass and already takes Mom's $500 for a bag of nothing, so the job board
 * runs on the same three things the mod already had: an emerald, a loose
 * dollar, and a $500 note. Prices are quoted in "bands" purely so one number
 * can cover all three.
 *
 * <p>He does not make change. Ever. Paying a two-band errand with a $500 note
 * hands him six bands of tip, which is exactly how {@link PlugBusiness} decides
 * you paid well - see the tip handling there. That is not a bug to be fixed
 * with a change-giving routine; it is the entire reason paying him properly
 * feels like a decision.
 */
public final class PlugCurrency {

    private PlugCurrency() { }

    /** What one of this item is worth, or 0 if he does not take it. */
    public static int perItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.is(Items.EMERALD) || stack.is(ModItems.DOLLARS.get())) {
            return 1;
        }
        if (stack.is(ModItems.FIVE_HUNDRED_DOLLARS.get())) {
            return 8;
        }
        return 0;
    }

    public static boolean isCurrency(ItemStack stack) {
        return perItem(stack) > 0;
    }

    /**
     * Takes payment for a job out of {@code held}.
     *
     * <p>Returns the bands actually handed over - never less than {@code price}
     * when it succeeds, often more - or 0 if the stack could not cover it, in
     * which case nothing is taken. Creative mode pays without losing the items,
     * matching how the $500 deal already behaves.
     */
    public static int take(Player player, ItemStack held, int price) {
        int unit = perItem(held);
        if (unit <= 0) {
            return 0;
        }
        int needed = (price + unit - 1) / unit;
        if (held.getCount() < needed) {
            return 0;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(needed);
        }
        return needed * unit;
    }

    /** The line he uses when quoting a price. */
    public static String quote(int price) {
        return price + " bands up front";
    }
}
