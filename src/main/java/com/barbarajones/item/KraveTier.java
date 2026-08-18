package com.barbarajones.item;

import com.barbarajones.content.ModItems;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Krave-grade equipment: blisteringly fast, hits hard, and made of breakfast
 * cereal - so it has the durability you would expect from breakfast cereal.
 * The real cost is per-tool (see each tool's curse).
 */
public enum KraveTier implements Tier {
    INSTANCE;

    @Override
    public int getUses() {
        return 180;                     // cereal. it does not last.
    }

    @Override
    public float getSpeed() {
        return 14.0F;                   // faster than diamond
    }

    @Override
    public float getAttackDamageBonus() {
        return 4.0F;                    // between iron and diamond
    }

    @Override
    public int getLevel() {
        return Tiers.IRON.getLevel();   // mines everything iron can
    }

    @Override
    public int getEnchantmentValue() {
        return 22;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.KRAVE_CEREAL.get());
    }
}
