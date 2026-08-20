package com.barbarajones.v2.economy.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A bottle of thick, syrupy melted Krave Dust - the sweetener other modules
 * (machines, potions, abilities) can build on top of. Drinking one behaves
 * exactly like a Honey Bottle: a 32-tick drink animation that hands back an
 * empty glass bottle instead of consuming the container.
 *
 * <p>Registry id: {@code barbarajones:krave_syrup}. Registered in
 * {@link com.barbarajones.v2.economy.KraveEconomy}.
 */
public class KraveSyrupItem extends Item {

    private static final int DRINK_DURATION = 32;

    public KraveSyrupItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof Player player) {
            return player.getAbilities().instabuild
                    ? result
                    : ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE));
        }
        return result;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return DRINK_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }
}
