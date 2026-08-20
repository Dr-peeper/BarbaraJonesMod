package com.barbarajones.v2.quests.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds the crafting recipe that produces a given item, laid out as a 3x3 grid
 * the quest panel can draw.
 *
 * <p>A quest that says "craft a Grass Knife" and then declines to say how is the
 * single most common way a quest log wastes someone's time. The recipe is right
 * there in the recipe manager, so the atlas shows it.
 *
 * <p>Resolved from the live {@code RecipeManager} rather than from a hand-written
 * table. A table would be a second copy of the truth, and the copy that nobody
 * updates is the one that lies - change a recipe JSON and a hardcoded atlas keeps
 * showing the old ingredients forever, with nothing to catch it.
 *
 * <p>Results are cached per item, because the panel redraws every frame and
 * scanning every recipe in the game sixty times a second to answer a question
 * whose answer cannot change mid-session would be absurd. The cache is dropped
 * on resource reload, which is when recipes can actually change.
 */
public final class QuestRecipes {

    /** A recipe flattened into the shape the panel draws: nine cells and a result. */
    public record Grid(ItemStack[] cells, ItemStack result, boolean shapeless) {
        public static final int W = 3;
    }

    private static final Map<ResourceLocation, Grid> CACHE = new HashMap<>();

    /** Sentinel so a miss is cached too - a fruitless scan is worth not repeating. */
    private static final Grid NONE = new Grid(new ItemStack[9], ItemStack.EMPTY, false);

    private QuestRecipes() { }

    /** Called on resource reload; recipes are the one thing that invalidates this. */
    public static void clear() {
        CACHE.clear();
    }

    /**
     * The crafting recipe producing {@code item}, or null if nothing crafts it -
     * plenty of quest items are mob drops or rewards, and those have no grid.
     */
    @Nullable
    public static Grid forItem(ResourceLocation item) {
        Grid hit = CACHE.get(item);
        if (hit != null) {
            return hit == NONE ? null : hit;
        }
        Grid built = resolve(item);
        CACHE.put(item, built == null ? NONE : built);
        return built;
    }

    @Nullable
    private static Grid resolve(ResourceLocation item) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;   // no recipe manager before a world is joined
        }
        var access = mc.level.registryAccess();

        for (Recipe<?> recipe : mc.level.getRecipeManager().getRecipes()) {
            if (recipe.getType() != RecipeType.CRAFTING || !(recipe instanceof CraftingRecipe crafting)) {
                continue;
            }
            ItemStack out;
            try {
                out = crafting.getResultItem(access);
            } catch (Exception ignored) {
                continue;   // a broken third-party recipe must not take the screen down
            }
            if (out.isEmpty()) {
                continue;
            }
            ResourceLocation outId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(out.getItem());
            if (outId == null || !outId.equals(item)) {
                continue;
            }
            return flatten(crafting, out);
        }
        return null;
    }

    /**
     * Lay the ingredients into a 3x3. A shaped recipe keeps its own width, so a
     * 2x1 recipe stays 2x1 in the corner rather than being smeared across the
     * grid; a shapeless one is filled left to right, which is how every recipe
     * viewer shows them and is close enough to true.
     */
    private static Grid flatten(CraftingRecipe recipe, ItemStack result) {
        ItemStack[] cells = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            cells[i] = ItemStack.EMPTY;
        }
        List<Ingredient> ingredients = recipe.getIngredients();

        if (recipe instanceof ShapedRecipe shaped) {
            int w = shaped.getWidth();
            for (int i = 0; i < ingredients.size(); i++) {
                int col = i % w;
                int row = i / w;
                if (col < Grid.W && row < Grid.W) {
                    cells[row * Grid.W + col] = first(ingredients.get(i));
                }
            }
            return new Grid(cells, result, false);
        }

        for (int i = 0; i < ingredients.size() && i < 9; i++) {
            cells[i] = first(ingredients.get(i));
        }
        return new Grid(cells, result, true);
    }

    /**
     * One representative stack for an ingredient. Tag ingredients accept many
     * items; the panel has room for one, and showing the first is both stable
     * across frames and honest enough - the tooltip names what it is.
     */
    private static ItemStack first(Ingredient ingredient) {
        ItemStack[] options = ingredient.getItems();
        return options.length == 0 ? ItemStack.EMPTY : options[0];
    }

    /** Every distinct item a quest's tasks ask the player to make or hold. */
    public static List<ResourceLocation> craftables(com.barbarajones.v2.quests.Quest quest) {
        List<ResourceLocation> out = new ArrayList<>();
        for (var task : quest.tasks) {
            for (var hold : task.supplies()) {
                if (!out.contains(hold.item)) {
                    out.add(hold.item);
                }
            }
        }
        return out;
    }
}
