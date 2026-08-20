package com.barbarajones.v2.village;

import com.barbarajones.content.ModItems;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The trade tables: what each profession will buy and sell, at each of its five
 * levels.
 *
 * <h2>Shape</h2>
 * A profession has five tiers of offers. Levelling up <em>appends</em> the next
 * tier's offers to the villager's existing list rather than replacing it, so a
 * trade the player has come to rely on never silently disappears. That is the one
 * rule of merchant design that players notice immediately when it is broken.
 *
 * <h2>Currency</h2>
 * Dollars, because this mod is about a man who sells cereal out of a van. Emeralds
 * are accepted at the higher tiers so a player who arrives from a vanilla world has
 * something to spend. Krave Cereal is both a currency and a good - a Grocer buys it
 * and a Cerealogist consumes it - which is what makes the village economy circular
 * rather than a vending machine.
 *
 * <h2>Levelling</h2>
 * Trade XP comes from two sources: completing trades ({@link VillageOffer#xpReward})
 * and being hand-fed Krave Cereal. Feeding is the fast path and it is deliberately
 * loud about it - see {@link KraveVillagerEntity#feedKrave}. The XP curve is on
 * {@link #xpForLevel(int)}.
 */
public final class VillageTrades {

    /** Trade levels run 1..5. Level 1 is what a new arrival opens with. */
    public static final int MAX_LEVEL = 5;

    private VillageTrades() { }

    /**
     * Total XP needed to reach {@code level}. Deliberately shallow at the bottom -
     * a player who feeds a new villager three bowls of cereal should see it level
     * within the first minute, because that is the moment the mechanic teaches
     * itself.
     */
    public static int xpForLevel(int level) {
        return switch (Math.max(1, Math.min(MAX_LEVEL, level))) {
            case 1 -> 0;
            case 2 -> 12;
            case 3 -> 40;
            case 4 -> 90;
            default -> 170;
        };
    }

    /** XP still needed to advance from {@code level}. Zero at the cap. */
    public static int xpToNextLevel(int level, int currentXp) {
        if (level >= MAX_LEVEL) {
            return 0;
        }
        return Math.max(0, xpForLevel(level + 1) - currentXp);
    }

    /** The level {@code xp} total corresponds to. */
    public static int levelForXp(int xp) {
        int level = 1;
        for (int probe = 2; probe <= MAX_LEVEL; probe++) {
            if (xp >= xpForLevel(probe)) {
                level = probe;
            }
        }
        return level;
    }

    /**
     * Rolls the offers a villager of this profession unlocks at exactly this level.
     * Called once per level-up and appended to the villager's list; never called to
     * rebuild an existing list, because that would reroll prices under the player.
     */
    public static List<VillageOffer> offersFor(KraveProfession profession, int level, RandomSource random) {
        List<VillageOffer> out = new ArrayList<>();
        switch (profession) {
            case GROCER -> grocer(out, level, random);
            case CEREALOGIST -> cerealogist(out, level, random);
            case BUILDER -> builder(out, level, random);
            case GUARD -> guard(out, level, random);
            case COURIER -> courier(out, level, random);
        }
        return out;
    }

    // ---- helpers -------------------------------------------------------------

    private static ItemStack stack(net.minecraft.world.item.Item item, int count) {
        return new ItemStack(item, count);
    }

    private static int vary(RandomSource random, int base, int spread) {
        return Math.max(1, base + random.nextInt(spread * 2 + 1) - spread);
    }

    private static void add(List<VillageOffer> out, ItemStack costA, ItemStack costB,
                            ItemStack result, int maxUses, int xp, int level) {
        if (result.isEmpty()) {
            return;
        }
        out.add(new VillageOffer(costA, costB, result, maxUses, xp, level));
    }

    // ---- GROCER: the food economy -------------------------------------------

    private static void grocer(List<VillageOffer> out, int level, RandomSource random) {
        switch (level) {
            case 1 -> {
                add(out, stack(ModItems.HANDFUL_OF_GRASS.get(), vary(random, 8, 2)), ItemStack.EMPTY,
                        stack(ModItems.DOLLARS.get(), 1), 16, 2, 1);
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 3, 1)), ItemStack.EMPTY,
                        stack(ModItems.KRAVE_CEREAL.get(), 1), 12, 2, 1);
            }
            case 2 -> {
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 4, 1)), ItemStack.EMPTY,
                        stack(ModItems.KRAVE_MILK.get(), 1), 10, 3, 2);
                add(out, stack(ModItems.KRAVE_CEREAL.get(), 4), ItemStack.EMPTY,
                        stack(ModItems.DOLLARS.get(), vary(random, 10, 2)), 10, 3, 2);
            }
            case 3 -> {
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 9, 2)), ItemStack.EMPTY,
                        stack(ModItems.CEREAL_BOWL.get(), 1), 8, 5, 3);
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 6, 2)), ItemStack.EMPTY,
                        stack(ModItems.NUGGET_BOX.get(), 1), 8, 4, 3);
            }
            case 4 -> {
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 16, 3)), stack(Items.EMERALD, 1),
                        stack(ModItems.KRAVE_FAMILY_BOX.get(), 1), 5, 8, 4);
                add(out, stack(ModItems.DICED_GRASS.get(), 16), ItemStack.EMPTY,
                        stack(ModItems.FIVE_HUNDRED_DOLLARS.get(), 1), 4, 8, 4);
            }
            default -> add(out, stack(ModItems.FIVE_HUNDRED_DOLLARS.get(), 1), stack(Items.EMERALD, 6),
                    stack(ModItems.GOLDEN_KRAVE.get(), 1), 2, 14, 5);
        }
    }

    // ---- CEREALOGIST: turns Krave into stranger Krave -------------------------

    private static void cerealogist(List<VillageOffer> out, int level, RandomSource random) {
        switch (level) {
            case 1 -> {
                add(out, stack(ModItems.KRAVE_CEREAL.get(), vary(random, 3, 1)), ItemStack.EMPTY,
                        stack(ModItems.KRAVE_DUST.get(), 1), 16, 3, 1);
                add(out, stack(ModItems.ROASTED_HUSK.get(), 6), ItemStack.EMPTY,
                        stack(ModItems.COCOA_SUBSTITUTE.get(), 2), 12, 2, 1);
            }
            case 2 -> add(out, stack(ModItems.KRAVE_DUST.get(), 4), stack(ModItems.DOLLARS.get(), 4),
                    stack(ModItems.KRAVE_BLOCK_ITEM.get(), 1), 8, 5, 2);
            case 3 -> {
                add(out, stack(ModItems.KRAVE_CEREAL.get(), 8), ItemStack.EMPTY,
                        stack(ModItems.KRAVE_POD_ITEM.get(), 2), 8, 6, 3);
                add(out, stack(ModItems.STALE_KRAVE.get(), 6), ItemStack.EMPTY,
                        stack(ModItems.KRAVE_CEREAL.get(), 2), 10, 4, 3);
            }
            case 4 -> add(out, stack(ModItems.KRAVE_BLOCK_ITEM.get(), 2), stack(Items.EMERALD, 4),
                    stack(ModItems.KRAVE_CLEANSE.get(), 1), 4, 10, 4);
            default -> {
                add(out, stack(ModItems.KRAVE_BLOCK_ITEM.get(), 4), stack(ModItems.GOLDEN_KRAVE.get(), 1),
                        stack(ModItems.KRAVE_TETHER.get(), 1), 2, 16, 5);
                add(out, stack(ModItems.KRAVE_DUST.get(), 24), ItemStack.EMPTY,
                        stack(ModItems.GOLDEN_KRAVE.get(), 1), 3, 14, 5);
            }
        }
    }

    // ---- BUILDER: sells the village back to you -------------------------------

    private static void builder(List<VillageOffer> out, int level, RandomSource random) {
        switch (level) {
            case 1 -> {
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 2, 1)), ItemStack.EMPTY,
                        stack(ModItems.KRAVE_PLANKS_ITEM.get(), 8), 16, 2, 1);
                add(out, stack(ModItems.KRAVE_LOG_ITEM.get(), 6), ItemStack.EMPTY,
                        stack(ModItems.DOLLARS.get(), vary(random, 5, 1)), 12, 2, 1);
            }
            case 2 -> {
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 5, 1)), ItemStack.EMPTY,
                        stack(ModItems.KRAVE_DOOR_ITEM.get(), 1), 10, 3, 2);
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 4, 1)), ItemStack.EMPTY,
                        stack(ModItems.KRAVE_FENCE_ITEM.get(), 6), 10, 3, 2);
            }
            case 3 -> {
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 8, 2)), ItemStack.EMPTY,
                        stack(ModItems.SHAG_CARPET_ITEM.get(), 8), 8, 5, 3);
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 8, 2)), ItemStack.EMPTY,
                        stack(ModItems.WOOD_PANELING_ITEM.get(), 8), 8, 5, 3);
            }
            case 4 -> add(out, stack(ModItems.DOLLARS.get(), vary(random, 20, 4)), stack(Items.EMERALD, 2),
                    stack(ModItems.KRAFTING_BENCH_ITEM.get(), 1), 3, 10, 4);
            default -> {
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 26, 4)), stack(Items.EMERALD, 3),
                        stack(ModItems.RECLINER_ITEM.get(), 1), 3, 12, 5);
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 26, 4)), stack(Items.EMERALD, 3),
                        stack(ModItems.TELEVISION_ITEM.get(), 1), 3, 12, 5);
            }
        }
    }

    // ---- GUARD: armed, and moderately interested in money ---------------------

    private static void guard(List<VillageOffer> out, int level, RandomSource random) {
        switch (level) {
            case 1 -> add(out, stack(ModItems.DOLLARS.get(), vary(random, 6, 2)), ItemStack.EMPTY,
                    stack(Items.SHIELD, 1), 6, 3, 1);
            case 2 -> {
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 10, 2)), ItemStack.EMPTY,
                        stack(Items.IRON_CHESTPLATE, 1), 4, 5, 2);
                add(out, stack(Items.ROTTEN_FLESH, 24), ItemStack.EMPTY,
                        stack(ModItems.DOLLARS.get(), vary(random, 6, 2)), 10, 4, 2);
            }
            case 3 -> add(out, stack(ModItems.DOLLARS.get(), vary(random, 14, 3)), stack(Items.EMERALD, 1),
                    stack(ModItems.KRAVE_SWORD.get(), 1), 3, 8, 3);
            case 4 -> add(out, stack(ModItems.DOLLARS.get(), vary(random, 18, 3)), ItemStack.EMPTY,
                    stack(Items.IRON_BARS, 16), 6, 8, 4);
            default -> add(out, stack(ModItems.FIVE_HUNDRED_DOLLARS.get(), 1), stack(Items.EMERALD, 4),
                    stack(ModItems.KRAVE_MULTITOOL.get(), 1), 1, 18, 5);
        }
    }

    // ---- COURIER: everything that is somewhere else ---------------------------

    private static void courier(List<VillageOffer> out, int level, RandomSource random) {
        switch (level) {
            case 1 -> add(out, stack(ModItems.DOLLARS.get(), vary(random, 4, 1)), ItemStack.EMPTY,
                    stack(ModItems.MR_PIBB.get(), 2), 12, 2, 1);
            case 2 -> {
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 7, 2)), ItemStack.EMPTY,
                        stack(ModItems.DONUT_BOX.get(), 1), 8, 4, 2);
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 7, 2)), ItemStack.EMPTY,
                        stack(ModItems.FRIES.get(), 3), 10, 3, 2);
            }
            case 3 -> add(out, stack(ModItems.DOLLARS.get(), vary(random, 12, 3)), ItemStack.EMPTY,
                    stack(ModItems.CAYDEN_COMPASS.get(), 1), 3, 8, 3);
            case 4 -> {
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 15, 3)), ItemStack.EMPTY,
                        stack(ModItems.FLIP_PHONE.get(), 1), 3, 8, 4);
                add(out, stack(ModItems.DOLLARS.get(), vary(random, 11, 2)), ItemStack.EMPTY,
                        stack(ModItems.KRAVE_RADIO.get(), 1), 3, 8, 4);
            }
            default -> add(out, stack(ModItems.FIVE_HUNDRED_DOLLARS.get(), 1), stack(ModItems.KRAVE_CEREAL.get(), 8),
                    stack(ModItems.KRAVE_MANUAL.get(), 1), 2, 16, 5);
        }
    }
}
