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
        // Krave Block just went to obsidian's real hardness (50, was 6) so it
        // still needs to clear fast: at speed 20 it was ~2.8s per block even
        // with the right tool, and the pickaxe read as sluggish generally.
        // 40 clears obsidian-hardness material in well under a second,
        // against a diamond pick's ~9+ seconds on real obsidian.
        return 40.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 4.0F;                    // between iron and diamond
    }

    @Override
    public int getLevel() {
        // Comfortably past Tiers.NETHERITE (4) - mines ancient debris,
        // obsidian, everything. The best tool in the game for the job means
        // never once hitting "Requires a better tool."
        return Tiers.NETHERITE.getLevel() + 5;
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
