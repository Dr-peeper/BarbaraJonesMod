package com.barbarajones.v2.machines.recipe;

import java.util.List;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * One JSON-defined machine operation: N counted inputs in, one stack out, over a
 * fixed number of ticks.
 *
 * <p>A single class backs all five machine recipe types (grinding, mixing,
 * extruding, toasting, boxing). The type is injected by the serializer rather
 * than hard-coded, which is what lets the Grinder and the Toaster share every
 * line of matching and consumption code while still being separate, separately
 * datapack-able {@link RecipeType}s that cannot cross-contaminate.
 *
 * <p>Matching is order-independent. {@link #match} runs a small backtracking
 * assignment of ingredients to slots rather than a greedy left-to-right pass,
 * because greedy is wrong the moment two ingredients can accept the same item -
 * a Mixer recipe of (dust, dust, sugar) fed (dust, sugar, dust) must still match,
 * and greedy would bind the first dust ingredient to slot 0, the second to slot 1
 * which holds sugar, and wrongly report no match.
 */
public class MachineRecipe implements Recipe<MachineContainer> {

    private final ResourceLocation id;
    private final RecipeType<MachineRecipe> type;
    private final RecipeSerializer<MachineRecipe> serializer;
    private final List<SizedIngredient> inputs;
    private final ItemStack result;
    private final int time;
    private final int fuelPerTick;

    public MachineRecipe(ResourceLocation id,
                         RecipeType<MachineRecipe> type,
                         RecipeSerializer<MachineRecipe> serializer,
                         List<SizedIngredient> inputs,
                         ItemStack result,
                         int time,
                         int fuelPerTick) {
        this.id = id;
        this.type = type;
        this.serializer = serializer;
        this.inputs = List.copyOf(inputs);
        this.result = result;
        this.time = Math.max(1, time);
        this.fuelPerTick = Math.max(0, fuelPerTick);
    }

    public List<SizedIngredient> inputs() {
        return inputs;
    }

    /** Ticks of work at one fuel unit per tick (times {@link #fuelPerTick}). */
    public int time() {
        return time;
    }

    /** Syrup units burned per tick of progress. 0 means this recipe runs cold. */
    public int fuelPerTick() {
        return fuelPerTick;
    }

    /** The literal output stack. Callers must copy before inserting. */
    public ItemStack output() {
        return result;
    }

    /**
     * Assigns each ingredient to a distinct container slot.
     *
     * @return an array where {@code result[i]} is the container index feeding
     *         ingredient {@code i}, or {@code null} if no complete assignment exists.
     */
    public int[] match(MachineContainer container) {
        int n = inputs.size();
        if (n == 0) {
            return new int[0];
        }
        int[] assignment = new int[n];
        boolean[] used = new boolean[container.getContainerSize()];
        return assign(container, 0, assignment, used) ? assignment : null;
    }

    private boolean assign(MachineContainer container, int ingredientIndex, int[] assignment, boolean[] used) {
        if (ingredientIndex == inputs.size()) {
            return true;
        }
        SizedIngredient want = inputs.get(ingredientIndex);
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (used[slot] || !want.test(container.getItem(slot))) {
                continue;
            }
            used[slot] = true;
            assignment[ingredientIndex] = slot;
            if (assign(container, ingredientIndex + 1, assignment, used)) {
                return true;
            }
            used[slot] = false;
        }
        return false;
    }

    @Override
    public boolean matches(MachineContainer container, Level level) {
        return match(container) != null;
    }

    @Override
    public ItemStack assemble(MachineContainer container, RegistryAccess access) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess access) {
        return result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (SizedIngredient input : inputs) {
            list.add(input.ingredient());
        }
        return list;
    }

    /**
     * Keeps these out of the vanilla recipe book. They are not craftable by hand
     * and a recipe-book entry the player can click but never fulfil is worse than
     * no entry at all.
     */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeType<?> getType() {
        return type;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }
}
