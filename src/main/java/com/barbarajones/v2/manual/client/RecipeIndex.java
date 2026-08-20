package com.barbarajones.v2.manual.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.manual.book.Icon;
import com.barbarajones.v2.manual.book.PageElement;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Builds the manual's recipe index: every crafting recipe this mod adds, drawn
 * as a real grid.
 *
 * <p>Generated from the live {@code RecipeManager} every time the chapter is
 * opened, never from a hand-written list. A hand-written index is a second copy
 * of the truth, and the copy nobody updates is the one that lies - add a recipe
 * and a static index silently omits it, change one and it silently misleads.
 * This cannot drift: if the recipe exists, it is in the book.
 *
 * <p>Ordered so it is navigable rather than in registry order, which is
 * effectively random: recipes are grouped by what they produce, and within a
 * group sorted by name.
 *
 * <p>Only crafting-table recipes get a grid, because a grid is what a 3x3 is.
 * Smelting and the mod's machine recipes are counted and named at the end
 * instead of being drawn as a crafting square they do not use - a furnace recipe
 * rendered as a 3x3 with one ingredient in the corner teaches the wrong thing.
 */
public final class RecipeIndex {

    private RecipeIndex() { }

    /** Chapter id the screen swaps for freshly generated content. */
    public static final String CHAPTER_ID = "recipe_index";

    private record Entry(ResourceLocation id, CraftingRecipe recipe, ItemStack result) { }

    public static List<PageElement> build() {
        List<PageElement> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();

        out.add(new PageElement.Heading("EVERY RECIPE"));
        if (mc.level == null) {
            out.add(new PageElement.Para(
                    "The recipe list is read from the world you are in, so it is only "
                            + "available once you have loaded one.", 0));
            return out;
        }
        var access = mc.level.registryAccess();

        List<Entry> ours = new ArrayList<>();
        List<String> otherKinds = new ArrayList<>();

        for (Recipe<?> recipe : mc.level.getRecipeManager().getRecipes()) {
            ItemStack result;
            try {
                result = recipe.getResultItem(access);
            } catch (Exception ignored) {
                continue;   // a broken third-party recipe must not empty the chapter
            }
            if (result.isEmpty()) {
                continue;
            }
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(result.getItem());
            boolean oursByResult = itemId != null && BarbaraJonesMod.MODID.equals(itemId.getNamespace());
            boolean oursByRecipe = BarbaraJonesMod.MODID.equals(recipe.getId().getNamespace());
            if (!oursByResult && !oursByRecipe) {
                continue;
            }
            if (recipe.getType() == RecipeType.CRAFTING && recipe instanceof CraftingRecipe crafting) {
                ours.add(new Entry(recipe.getId(), crafting, result));
            } else {
                otherKinds.add(result.getHoverName().getString());
            }
        }

        ours.sort(Comparator.comparing(e -> e.result().getHoverName().getString()));

        out.add(new PageElement.Para(
                "Every one of the " + ours.size() + " crafting recipes this mod adds, "
                        + "read straight out of the game rather than typed up by hand - so "
                        + "if it is craftable, it is on these pages.", 0));

        for (Entry e : ours) {
            out.add(new PageElement.CraftGrid(
                    cells(e.recipe()),
                    !(e.recipe() instanceof ShapedRecipe),
                    Icon.of(() -> e.result().copy()),
                    e.result().getCount(),
                    e.result().getHoverName().getString()));
        }

        if (!otherKinds.isEmpty()) {
            otherKinds.sort(String::compareTo);
            out.add(new PageElement.Heading("NOT MADE ON A BENCH"));
            out.add(new PageElement.Para(
                    "These are smelted, or made in one of the machines, so there is no "
                            + "grid to show: " + String.join(", ", dedupe(otherKinds)) + ".", 0));
        }
        return out;
    }

    /**
     * Nine cells. A shaped recipe keeps its own width so a 2x1 recipe stays a
     * 2x1 in the corner rather than being smeared across the whole square;
     * shapeless fills left to right, which is how every recipe viewer shows them.
     */
    private static List<Icon> cells(CraftingRecipe recipe) {
        ItemStack[] grid = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            grid[i] = ItemStack.EMPTY;
        }
        List<Ingredient> ingredients = recipe.getIngredients();

        if (recipe instanceof ShapedRecipe shaped) {
            int w = shaped.getWidth();
            for (int i = 0; i < ingredients.size(); i++) {
                int col = i % w;
                int row = i / w;
                if (col < 3 && row < 3) {
                    grid[row * 3 + col] = firstOf(ingredients.get(i));
                }
            }
        } else {
            for (int i = 0; i < ingredients.size() && i < 9; i++) {
                grid[i] = firstOf(ingredients.get(i));
            }
        }

        List<Icon> icons = new ArrayList<>(9);
        for (ItemStack cell : grid) {
            final ItemStack stack = cell;
            icons.add(Icon.of(() -> stack));
        }
        return icons;
    }

    /**
     * One representative stack per ingredient. A tag ingredient accepts many
     * items and the cell has room for one; the tooltip names whichever is shown.
     */
    private static ItemStack firstOf(Ingredient ingredient) {
        ItemStack[] options = ingredient.getItems();
        return options.length == 0 ? ItemStack.EMPTY : options[0].copy();
    }

    private static List<String> dedupe(List<String> names) {
        List<String> out = new ArrayList<>();
        for (String n : names) {
            if (!out.contains(n)) {
                out.add(n);
            }
        }
        return out;
    }
}
