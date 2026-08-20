package com.barbarajones.v2.machines.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * Reads and syncs {@link MachineRecipe}. One instance is registered per machine
 * recipe type; each instance knows which {@link RecipeType} the recipes it builds
 * belong to, which is how five separate recipe types share one recipe class.
 *
 * <p>The type is handed in as a {@link Supplier} rather than a direct reference
 * because serializers and recipe types are separate Forge registries and the
 * order in which their {@code RegisterEvent}s fire is not something this class
 * should depend on. Resolving lazily makes the ordering irrelevant.
 *
 * <p>JSON shape:
 * <pre>
 * {
 *   "type": "barbarajones:mixing",
 *   "ingredients": [
 *     { "item": "barbarajones:krave_dust", "count": 2 },
 *     { "item": "barbarajones:krave_milk" },
 *     { "item": "minecraft:sugar" }
 *   ],
 *   "result": { "item": "barbarajones:krave_batter", "count": 2 },
 *   "time": 160,
 *   "fuel_per_tick": 1
 * }
 * </pre>
 */
public class MachineRecipeSerializer implements RecipeSerializer<MachineRecipe> {

    /** Hard cap: a machine only ever has three input slots, so more can never match. */
    public static final int MAX_INPUTS = 3;

    private final Supplier<RecipeType<MachineRecipe>> type;
    private final int minInputs;
    private final int maxInputs;

    public MachineRecipeSerializer(Supplier<RecipeType<MachineRecipe>> type, int minInputs, int maxInputs) {
        this.type = type;
        this.minInputs = minInputs;
        this.maxInputs = Math.min(maxInputs, MAX_INPUTS);
    }

    @Override
    public MachineRecipe fromJson(ResourceLocation id, JsonObject json) {
        JsonArray array = GsonHelper.getAsJsonArray(json, "ingredients");
        if (array.size() < minInputs || array.size() > maxInputs) {
            throw new JsonSyntaxException("recipe " + id + " has " + array.size()
                    + " ingredients; this machine accepts " + minInputs + ".." + maxInputs);
        }
        List<SizedIngredient> inputs = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            inputs.add(SizedIngredient.fromJson(array.get(i)));
        }

        ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
        if (result.isEmpty()) {
            throw new JsonSyntaxException("recipe " + id + " produces nothing");
        }

        int time = GsonHelper.getAsInt(json, "time", 120);
        int fuel = GsonHelper.getAsInt(json, "fuel_per_tick", 1);
        return new MachineRecipe(id, type.get(), this, inputs, result, time, fuel);
    }

    @Override
    public MachineRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<SizedIngredient> inputs = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            inputs.add(SizedIngredient.fromNetwork(buf));
        }
        ItemStack result = buf.readItem();
        int time = buf.readVarInt();
        int fuel = buf.readVarInt();
        return new MachineRecipe(id, type.get(), this, inputs, result, time, fuel);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, MachineRecipe recipe) {
        buf.writeVarInt(recipe.inputs().size());
        for (SizedIngredient input : recipe.inputs()) {
            input.toNetwork(buf);
        }
        buf.writeItem(recipe.output());
        buf.writeVarInt(recipe.time());
        buf.writeVarInt(recipe.fuelPerTick());
    }
}
