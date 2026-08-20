package com.barbarajones.v2.machines.recipe;

import com.google.gson.JsonObject;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * An {@link Ingredient} plus a required count.
 *
 * <p>Vanilla ingredients are count-free because the crafting grid gets its counts
 * from the grid shape - one item per cell. A machine has no grid, so "two cocoa
 * beans" has to be expressed somewhere, and this is that somewhere.
 *
 * <p>JSON is the ordinary ingredient object with an optional {@code count}:
 * <pre>
 *   { "item": "minecraft:cocoa_beans", "count": 2 }
 *   { "tag":  "forge:dusts/cocoa",     "count": 3 }
 *   [ { "item": "a" }, { "item": "b" } ]      // count defaults to 1
 * </pre>
 */
public record SizedIngredient(Ingredient ingredient, int count) {

    public static SizedIngredient fromJson(com.google.gson.JsonElement element) {
        int count = 1;
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            count = GsonHelper.getAsInt(obj, "count", 1);
            if (count < 1) {
                throw new com.google.gson.JsonSyntaxException("ingredient count must be at least 1, got " + count);
            }
        }
        return new SizedIngredient(Ingredient.fromJson(element), count);
    }

    public static SizedIngredient fromNetwork(FriendlyByteBuf buf) {
        Ingredient ing = Ingredient.fromNetwork(buf);
        return new SizedIngredient(ing, buf.readVarInt());
    }

    public void toNetwork(FriendlyByteBuf buf) {
        ingredient.toNetwork(buf);
        buf.writeVarInt(count);
    }

    /** True if this stack is the right item AND there is enough of it. */
    public boolean test(ItemStack stack) {
        return !stack.isEmpty() && stack.getCount() >= count && ingredient.test(stack);
    }

    /** The stacks a recipe viewer would show for this input, scaled to the real count. */
    public ItemStack[] displayStacks() {
        ItemStack[] base = ingredient.getItems();
        ItemStack[] out = new ItemStack[base.length];
        for (int i = 0; i < base.length; i++) {
            out[i] = base[i].copyWithCount(count);
        }
        return out;
    }
}
