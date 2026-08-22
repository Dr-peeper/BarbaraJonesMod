package com.barbarajones.v2.plug;

import com.barbarajones.content.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/**
 * Everything The Plug has ever come back with, as data.
 *
 * <p>Every table here is six rows deep and the rows are always in the same
 * order - JUNK, SCRAP, PARTIAL, SOLID, HEAVY, ABSURD - because
 * {@link PlugCompetence#rollTier} indexes straight into them. Nothing in this
 * file knows what a competence level is; it just supplies six increasingly
 * generous answers to the same question and lets the weight matrix choose. Each
 * row holds several options so the same tier does not read like a canned
 * response the third time you see it.
 *
 * <p>The first entry of every JUNK row is a rip-off: he came back with an empty
 * bag and an explanation. That is the point of the low end, and it stays
 * reachable - rarely - all the way up.
 */
final class PlugResults {

    private PlugResults() { }

    // ---- table helpers -------------------------------------------------------

    private static PlugPayout pay(String line, PlugPayout.Drop... drops) {
        return new PlugPayout(line, drops);
    }

    private static PlugPayout.Drop d(Supplier<Item> item, int min, int max) {
        return new PlugPayout.Drop(item, min, max);
    }

    private static PlugPayout.Drop d(Supplier<Item> item, int count) {
        return new PlugPayout.Drop(item, count, count);
    }

    private static PlugPayout.Drop d(Item item, int min, int max) {
        return new PlugPayout.Drop(() -> item, min, max);
    }

    private static PlugPayout.Drop d(Item item, int count) {
        return new PlugPayout.Drop(() -> item, count, count);
    }

    // ---- gather wood ---------------------------------------------------------

    static final PlugPayout[][] WOOD = {
            {
                    pay("wood? nah they was out. the whole outside was out of wood today."),
                    pay("wood.", d(Items.STICK, 1, 3)),
                    pay("that's wood in like six years. you gotta be patient with agriculture.",
                            d(Items.OAK_SAPLING, 1, 2)),
                    pay("it's wood adjacent. it WAS a plant.", d(Items.DEAD_BUSH, 1)),
            },
            {
                    pay("I broke em down for you already. that's a service.", d(Items.STICK, 10, 16)),
                    pay("pre-cut. you welcome.", d(Items.OAK_PLANKS, 3, 6)),
            },
            {
                    pay("that's a start. build a chair or somethin.", d(Items.OAK_LOG, 4, 6)),
                    pay("two different trees. that's variety. that's design.",
                            d(Items.BIRCH_LOG, 3, 5), d(Items.OAK_PLANKS, 4, 6)),
            },
            {
                    pay("stack of wood. no lie in it. this the one.", d(Items.OAK_LOG, 16, 22)),
                    pay("logs AND planks. I did half the work for you.",
                            d(Items.OAK_LOG, 10, 12), d(Items.OAK_PLANKS, 12, 16)),
            },
            {
                    pay("I was out there like a whole industry.", d(Items.OAK_LOG, 32, 40)),
                    pay("two species. don't mix em, that's tacky.",
                            d(Items.OAK_LOG, 20, 26), d(Items.SPRUCE_LOG, 14, 18)),
            },
            {
                    pay("somebody's forest is GONE. it's a field now. don't go over there.",
                            d(Items.OAK_LOG, 48, 64), d(Items.OAK_SAPLING, 3, 5)),
                    pay("found a weird tree out there. bark taste like a candy bar. I ate some.",
                            d(ModItems.CHOCOLATE_LOG_ITEM, 8, 12), d(Items.OAK_LOG, 12, 16)),
            },
    };

    // ---- get food ------------------------------------------------------------

    static final PlugPayout[][] FOOD = {
            {
                    pay("I got hungry on the way back. that's the risk you take hirin a man."),
                    pay("hydrate first. that's the number one thing nobody tells you.",
                            d(ModItems.SEWER_WATER, 1)),
                    pay("it's protein. protein is protein. stop cryin.", d(Items.ROTTEN_FLESH, 2, 4)),
                    pay("that's a potato. mostly. like 80% potato.", d(Items.POISONOUS_POTATO, 1, 2)),
            },
            {
                    pay("they had the good ones locked behind the counter like it's jewelry.",
                            d(ModItems.OFF_BRAND_PASTRIES, 1, 2)),
                    pay("bread. the original food. everything else is showin off.", d(Items.BREAD, 1, 2)),
            },
            {
                    pay("that'll hold you till somebody who love you cooks.", d(Items.BREAD, 4, 6)),
                    pay("I ate the big one in the car. that part's between us.",
                            d(ModItems.FRIES, 2, 3), d(ModItems.CHICKEN_NUGGETS, 1, 2)),
            },
            {
                    pay("real food. cooked. by heat. like a grown man eats.", d(Items.COOKED_BEEF, 6, 8)),
                    pay("the WHOLE meal. drink included. I don't do half meals.",
                            d(ModItems.NUGGET_BOX, 1), d(ModItems.FRIES, 3, 4), d(ModItems.MR_PIBB, 1)),
            },
            {
                    pay("feed the whole block off this.", d(Items.COOKED_BEEF, 10, 14), d(Items.BREAD, 6, 10)),
                    pay("sandwiches, donuts, drinks. this a function now.",
                            d(ModItems.CHICKEN_SANDWICH, 3, 5), d(ModItems.DONUT_BOX, 1), d(ModItems.GATORADE, 2, 3)),
            },
            {
                    pay("a apple. made of gold. I'm not answerin questions about it.",
                            d(Items.GOLDEN_APPLE, 1, 2), d(Items.GOLDEN_CARROT, 3, 5)),
                    pay("there was a whole cake sittin there. sittin there ALONE.",
                            d(Items.CAKE, 1), d(ModItems.APPLE_PIE, 2, 3), d(Items.COOKED_BEEF, 8, 10)),
            },
    };

    // ---- gather building supplies -------------------------------------------

    static final PlugPayout[][] BUILDING = {
            {
                    pay("supplies is expensive right now. that's the economy. take it up with the economy."),
                    pay("start with a foundation. that's foundation right there.", d(Items.DIRT, 2, 5)),
                    pay("for the garden out front. curb appeal. resale value.",
                            d(ModItems.FERTILIZER_BAG, 1)),
                    pay("brick.", d(Items.COBBLESTONE, 1, 2)),
            },
            {
                    pay("that's a wall if you build it thin.", d(Items.COBBLESTONE, 8, 14)),
                    pay("wood and nails. well. wood.", d(Items.OAK_PLANKS, 6, 10), d(Items.STICK, 4, 6)),
            },
            {
                    pay("that's a room. small room. a room for one person you don't like.",
                            d(Items.COBBLESTONE, 24, 32)),
                    pay("brought light too. don't say I never think ahead.",
                            d(Items.STONE, 16, 20), d(Items.TORCH, 8, 12)),
            },
            {
                    pay("that's a HOUSE. lit and everything.",
                            d(Items.COBBLESTONE, 48, 64), d(Items.TORCH, 12, 16)),
                    pay("planks and windows. neighbors gon be sick.",
                            d(Items.OAK_PLANKS, 40, 56), d(Items.GLASS, 10, 14)),
            },
            {
                    pay("build somethin your mama would come see.",
                            d(Items.STONE_BRICKS, 48, 64), d(Items.GLASS, 20, 24), d(Items.TORCH, 24, 32)),
                    pay("two floors worth. get a ladder in there.",
                            d(Items.OAK_PLANKS, 56, 64), d(Items.COBBLESTONE, 48, 64), d(Items.LADDER, 12, 16)),
            },
            {
                    pay("I furnished it too. you gon thank me when you sittin down.",
                            d(ModItems.SHAG_CARPET_ITEM, 6, 10), d(ModItems.WOOD_PANELING_ITEM, 12, 16),
                            d(ModItems.TELEVISION_ITEM, 1)),
                    pay("somebody's build is missin a whole wall. that's between them and God.",
                            d(Items.QUARTZ_BLOCK, 24, 32), d(Items.GLOWSTONE, 8, 12)),
            },
    };

    // ---- find iron -----------------------------------------------------------

    static final PlugPayout[][] IRON = {
            {
                    pay("iron is a controlled substance now. new rules. I don't make em."),
                    pay("iron.", d(Items.IRON_NUGGET, 1, 2)),
                    pay("that's made of iron. that's a iron item. you asked for iron.",
                            d(Items.BUCKET, 1)),
                    pay("that came off somethin iron. counts.", d(Items.STRING, 2, 3), d(Items.ROTTEN_FLESH, 1, 2)),
            },
            {
                    pay("count em. that's iron. that's iron numbers.", d(Items.IRON_NUGGET, 5, 9)),
                    pay("raw. you gotta cook it. I'm not a chef.", d(Items.RAW_IRON, 1, 2)),
            },
            {
                    pay("smelted and everything. I stood by a furnace for you.", d(Items.IRON_INGOT, 2, 3)),
                    pay("raw plus the coal to cook it. full service.",
                            d(Items.RAW_IRON, 3, 5), d(Items.COAL, 2, 4)),
            },
            {
                    pay("that's a set of tools and change.", d(Items.IRON_INGOT, 6, 9)),
                    pay("bring your own furnace, I brought the rest.",
                            d(Items.RAW_IRON, 8, 11), d(Items.COAL, 4, 6)),
            },
            {
                    pay("I found the vein. the WHOLE vein.", d(Items.IRON_INGOT, 14, 20)),
                    pay("blocked up for storage. I think about your inventory space.",
                            d(Items.IRON_BLOCK, 1), d(Items.IRON_INGOT, 5, 8)),
            },
            {
                    pay("a man was haulin this. was.", d(Items.IRON_BLOCK, 3, 4)),
                    pay("brought the anvil too. don't ask how I carried it. I got a bad back now.",
                            d(Items.ANVIL, 1), d(Items.IRON_INGOT, 8, 12)),
            },
    };

    // ---- collect Krave resources --------------------------------------------

    static final PlugPayout[][] KRAVE = {
            {
                    pay("Cayden got there first. that boy is FAST. I seen a blur and then a empty shelf."),
                    pay("it was in the back. way in the back. behind the other back.",
                            d(ModItems.STALE_KRAVE, 1, 2)),
                    pay("same box. different letters. nobody readin the box anyway.",
                            d(ModItems.OFF_BRAND_KRAVE, 1)),
                    pay("that's the ingredient. make it yourself. be a man.",
                            d(ModItems.COCOA_SUBSTITUTE, 1, 3)),
            },
            {
                    pay("bag broke. I scooped what I could off the ground. it's fine.",
                            d(ModItems.KRAVE_DUST, 2, 4)),
                    pay("husks. that's the outside. the outside got value too.",
                            d(ModItems.ROASTED_HUSK, 3, 6)),
            },
            {
                    pay("a few boxes. hide em before the kid smell it.", d(ModItems.KRAVE_CEREAL, 3, 5)),
                    pay("dust and milk. that's a breakfast if you brave.",
                            d(ModItems.KRAVE_DUST, 6, 9), d(ModItems.KRAVE_MILK, 1, 2)),
            },
            {
                    pay("that's a real run. that's what you pay me for.", d(ModItems.KRAVE_CEREAL, 10, 14)),
                    pay("boxes and milk. set the whole table.",
                            d(ModItems.KRAVE_BOX, 2), d(ModItems.KRAVE_MILK, 2, 3)),
            },
            {
                    pay("family size. plural. I emptied a aisle.",
                            d(ModItems.KRAVE_FAMILY_BOX, 2, 3), d(ModItems.KRAVE_CEREAL, 6, 10)),
                    pay("boxes and a bag of dust for the road.",
                            d(ModItems.KRAVE_BOX, 4, 6), d(ModItems.KRAVE_DUST, 10, 14)),
            },
            {
                    pay("don't ever ask me where the gold one came from. don't bring it up again.",
                            d(ModItems.GOLDEN_KRAVE, 1, 2), d(ModItems.KRAVE_FAMILY_BOX, 1)),
                    pay("they had it in a EVIDENCE bag. was. it was in a evidence bag.",
                            d(ModItems.KRAVE_BLOCK_ITEM, 4, 6), d(ModItems.CONFISCATED_KRAVE, 1, 2)),
            },
    };

    // ---- find emeralds -------------------------------------------------------

    static final PlugPayout[][] EMERALDS = {
            {
                    pay("the market crashed. today. while I was out there. terrible timing on your part."),
                    pay("here go one back. we even. we good. we solid.", d(Items.EMERALD, 1)),
                    pay("couldn't find emeralds. found this instead. this is better.",
                            d(ModItems.FAKE_COCAINE, 1, 2)),
                    pay("green. you asked for green.", d(Items.GREEN_DYE, 2, 3)),
            },
            {
                    pay("that's a couple. the villagers is stingy right now.", d(Items.EMERALD, 2, 3)),
                    pay("cashed em out. same value. trust the exchange rate.", d(ModItems.DOLLARS, 6, 12)),
            },
            {
                    pay("that's a trade or two. don't blow it on a bucket.", d(Items.EMERALD, 5, 7)),
                    pay("emeralds and a little paper.", d(Items.EMERALD, 4, 5), d(ModItems.DOLLARS, 4, 8)),
            },
            {
                    pay("that's a librarian's whole year right there.", d(Items.EMERALD, 10, 14)),
                    pay("green and green. I diversified for you.",
                            d(Items.EMERALD, 7, 9), d(ModItems.FIVE_HUNDRED_DOLLARS, 1)),
            },
            {
                    pay("I was in that village like I had a lease.", d(Items.EMERALD, 18, 26)),
                    pay("blocked up. count it later, count it in private.", d(Items.EMERALD_BLOCK, 2, 3)),
            },
            {
                    pay("a villager is missin. a WHOLE villager. we not doing a search party.",
                            d(Items.EMERALD_BLOCK, 4, 5)),
                    pay("I don't wanna talk about what today was.",
                            d(Items.EMERALD, 28, 36), d(ModItems.FIVE_HUNDRED_DOLLARS, 2)),
            },
    };

    // ---- mine diamonds -------------------------------------------------------

    static final PlugPayout[][] DIAMONDS = {
            {
                    pay("what money? ... oh THAT money. nah that was gas. that was gas money bro."),
                    pay("that's diamond money right there. same thing. basically the same thing.",
                            d(Items.EMERALD, 1)),
                    pay("they had a form you gotta fill out first. that's the form.", d(Items.PAPER, 3)),
                    pay("it was in the hole. I ain't gon lie to you about what was in the hole.",
                            d(Items.ROTTEN_FLESH, 1, 2)),
                    pay("gravel. shiny gravel. squint at it.", d(Items.GRAVEL, 2, 4)),
                    pay("that's a baby one. keep it warm. it grow.", d(Items.IRON_NUGGET, 1)),
            },
            {
                    pay("black diamond. same rock, different mood.", d(Items.COAL, 3, 6)),
                    pay("sharp though. a diamond is just a sharp rock with a agent.",
                            d(Items.FLINT, 2, 3), d(Items.GRAVEL, 3, 5)),
            },
            {
                    pay("one. ONE. and I almost died for that one.", d(Items.DIAMOND, 1)),
                    pay("had to bring the wall with it. it was stuck.",
                            d(Items.DIAMOND, 1), d(Items.DEEPSLATE, 3, 6)),
            },
            {
                    pay("clean. no notes. don't say nothin, just take it.", d(Items.DIAMOND, 2, 3)),
                    pay("lapis was on the way. I don't leave money on the floor.",
                            d(Items.DIAMOND, 2, 3), d(Items.LAPIS_LAZULI, 4, 8)),
            },
            {
                    pay("I hit a POCKET. I hit a whole pocket down there.", d(Items.DIAMOND, 4, 6)),
                    pay("diamonds and iron. I was down there workin like I got a badge.",
                            d(Items.DIAMOND, 3, 5), d(Items.IRON_INGOT, 6, 10)),
            },
            {
                    pay("don't ask whose house it was in. we not doing that today.",
                            d(Items.DIAMOND_BLOCK, 1), d(Items.DIAMOND, 2, 3)),
                    pay("the lava said no. I said yes. we argued. I won.",
                            d(Items.DIAMOND, 6, 9), d(Items.NETHERITE_SCRAP, 1)),
            },
    };

    // ---- the tip he sometimes throws in on top -------------------------------

    /**
     * Extras, rolled separately from the haul at higher competence. Kept small
     * on purpose: a bonus is a wink, not a second payday, and if it were big
     * enough to farm the whole progression would be about re-rolling bonuses
     * instead of about him.
     */
    static final PlugPayout[] BONUSES = {
            pay("threw somethin in the bag for you. don't smoke it all at once.",
                    d(ModItems.ROLLED_JOINT, 1)),
            pay("grabbed you a drink. it was hot out there.", d(ModItems.MR_PIBB, 1, 2)),
            pay("keep the change. I'm not a animal.", d(Items.EMERALD, 2, 4)),
            pay("that's for the kid. don't eat it yourself.", d(ModItems.KRAVE_CEREAL, 2, 3)),
            pay("found this on the way. it followed me home.", d(Items.DIAMOND, 1)),
            pay("CHEPINA. say less. say absolutely nothing.", d(ModItems.CHEPINA, 1)),
            pay("consider that a rebate. I'm a business.", d(ModItems.FIVE_HUNDRED_DOLLARS, 1)),
    };
}
