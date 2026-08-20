package com.barbarajones.v2.machines.recipe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Per-type recipe index for the machines.
 *
 * <p>The naive thing is to call {@code RecipeManager.getRecipeFor(type, ...)} from
 * every machine's tick. That is what a vanilla furnace does and it is fine at
 * vanilla scale, but {@code getAllRecipesFor} allocates a fresh list on every
 * call and {@code getRecipeFor} walks it linearly - at a hundred machines ticking
 * twenty times a second that is two thousand list allocations and scans per
 * second for something that changes only on datapack reload.
 *
 * <p>So: the list is cached per recipe type and rebuilt only when
 * {@link #invalidate()} bumps the generation, which happens on datapack sync
 * (server) and on {@code RecipesUpdatedEvent} (client). Block entities cache the
 * *matched* recipe alongside the generation they matched it in and only re-scan
 * when their inputs change or the generation moves.
 *
 * <p>Client and server keep separate maps. They hold identical content after a
 * datapack sync, but they are separate {@code RecipeManager}s owned by separate
 * threads and sharing one map between them is a data race waiting to happen.
 */
public final class RecipeLookup {

    private static volatile int generation = 1;

    private static final Map<RecipeType<?>, Entry> SERVER = new HashMap<>();
    private static final Map<RecipeType<?>, Entry> CLIENT = new HashMap<>();

    private record Entry(int generation, List<MachineRecipe> recipes) { }

    private RecipeLookup() { }

    /** Current index generation. Block entities store this next to their cached recipe. */
    public static int generation() {
        return generation;
    }

    /** Drops every cached list and invalidates every block entity's cached match. */
    public static void invalidate() {
        generation++;
        synchronized (SERVER) {
            SERVER.clear();
        }
        synchronized (CLIENT) {
            CLIENT.clear();
        }
    }

    /** Every loaded recipe of one type, cached until the next {@link #invalidate()}. */
    public static List<MachineRecipe> all(Level level, RecipeType<MachineRecipe> type) {
        Map<RecipeType<?>, Entry> map = level.isClientSide() ? CLIENT : SERVER;
        synchronized (map) {
            Entry entry = map.get(type);
            if (entry == null || entry.generation() != generation) {
                entry = new Entry(generation, List.copyOf(level.getRecipeManager().getAllRecipesFor(type)));
                map.put(type, entry);
            }
            return entry.recipes();
        }
    }

    /** First recipe of {@code type} whose ingredients can all be satisfied, or null. */
    @Nullable
    public static MachineRecipe find(Level level, RecipeType<MachineRecipe> type, MachineContainer container) {
        for (MachineRecipe recipe : all(level, type)) {
            if (recipe.match(container) != null) {
                return recipe;
            }
        }
        return null;
    }
}
