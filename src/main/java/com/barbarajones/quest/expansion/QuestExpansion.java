package com.barbarajones.quest.expansion;

import com.barbarajones.content.ModItems;
import com.barbarajones.quest.Quests;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * THE EXPANSION BOARD - six more branches hanging off the original Krave quest graph.
 *
 * <p>{@link Quests} owns the graph; this class only owns <em>data</em>. Every quest here
 * is a {@link Spec}, which is a {@code Quests.Quest} in all but name, and the registry
 * splices the whole list in with a single loop (see the class comment on {@link #ALL}).
 * Nothing in the original file has to change for these to appear in the book.
 *
 * <p>The six new branches, each hanging off an existing quest so the tree stays connected:
 * <ul>
 *   <li><b>The Stash</b> (from Touch Grass) - the grass economy at scale: bulk harvest,
 *       curing, rolling, and keeping Barbara topped up so she never goes psycho.</li>
 *   <li><b>Provider</b> (from Keeping Cayden) - Rule #1 as a progression: feed him,
 *       house him, upgrade the house, keep him breathing for five straight days.</li>
 *   <li><b>The Kosmos</b> (from The Cereal) - build the door, cross over, survive the
 *       liquid chocolate, cross the floating islands, watch the ascension.</li>
 *   <li><b>Infamy</b> (from Street Supplies) - the money, the fake product, the sewer,
 *       and putting The Plug in the ground more than once.</li>
 *   <li><b>Domestic</b> (from the hub) - the supporting cast: Daniel and his lighter,
 *       Nugget the cat, Mom Cobb, and Duhl Wol's daily tribute.</li>
 *   <li><b>The Grind</b> (from Level 10) - pure gamification: XP levels, distance walked,
 *       damage eaten, boxes of Krave demolished.</li>
 * </ul>
 *
 * <p>Three kinds of node, exactly as in the base graph:
 * <ul>
 *   <li><b>collect</b> - auto-completes once every listed item is in the inventory.</li>
 *   <li><b>goal</b> - an event quest. {@link QuestExpansionEvents} watches the world and
 *       calls {@code Quests.complete()} when the condition is actually met; the objective
 *       text always says exactly what that condition is.</li>
 *   <li><b>milestone</b> - a branch capstone with no items of its own: it completes the
 *       moment its prerequisites do.</li>
 * </ul>
 */
public final class QuestExpansion {

    private QuestExpansion() { }

    // ---- branch labels ------------------------------------------------------

    public static final String B_STASH    = "The Stash";
    public static final String B_PROVIDER = "Provider";
    public static final String B_KOSMOS   = "The Kosmos";
    public static final String B_INFAMY   = "Infamy";
    public static final String B_DOMESTIC = "Domestic";
    public static final String B_GRIND    = "The Grind";

    // ---- quest ids ----------------------------------------------------------
    // Every id is branch-prefixed so it can never collide with a base-game quest id.

    // The Stash
    public static final String STASH_HANDFULS   = "stash_handfuls";
    public static final String STASH_DICED      = "stash_diced";
    public static final String STASH_CURED      = "stash_cured";
    public static final String STASH_PAPERS     = "stash_papers";
    public static final String STASH_JARRED     = "stash_jarred";
    public static final String STASH_SMOKE_FIVE = "stash_smoke_five";
    public static final String STASH_CHAIN      = "stash_chain";
    public static final String STASH_SUPPLIER   = "stash_supplier";
    public static final String STASH_WITHDRAWAL = "stash_withdrawal";
    public static final String STASH_RESUPPLY   = "stash_resupply";
    public static final String STASH_BROWNIES   = "stash_brownies";
    public static final String STASH_TOP_SHELF  = "stash_top_shelf";
    public static final String STASH_EMPIRE     = "stash_empire";

    // Provider
    public static final String PROVIDER_FIRST_BOWL = "provider_first_bowl";
    public static final String PROVIDER_TEN        = "provider_ten";
    public static final String PROVIDER_RAGE       = "provider_rage";
    public static final String PROVIDER_PANTRY     = "provider_pantry";
    public static final String PROVIDER_ROOF       = "provider_roof";
    public static final String PROVIDER_UPGRADE    = "provider_upgrade";
    public static final String PROVIDER_MANSION    = "provider_mansion";
    public static final String PROVIDER_MEDIC      = "provider_medic";
    public static final String PROVIDER_FIVE_DAYS  = "provider_five_days";
    public static final String PROVIDER_SPOTLESS   = "provider_spotless";
    public static final String PROVIDER_CARE       = "provider_care";
    public static final String PROVIDER_GUARDIAN   = "provider_guardian";

    // The Kosmos
    public static final String KOSMOS_KIT       = "kosmos_kit";
    public static final String KOSMOS_ARRIVAL   = "kosmos_arrival";
    public static final String KOSMOS_CHOCOLATE = "kosmos_chocolate";
    public static final String KOSMOS_BUCKET    = "kosmos_bucket";
    public static final String KOSMOS_BAR       = "kosmos_bar";
    public static final String KOSMOS_SOIL      = "kosmos_soil";
    public static final String KOSMOS_HIGH      = "kosmos_high";
    public static final String KOSMOS_HOP       = "kosmos_hop";
    public static final String KOSMOS_RESIDENT  = "kosmos_resident";
    public static final String KOSMOS_ISLAND    = "kosmos_island";
    public static final String KOSMOS_ASCENSION = "kosmos_ascension";
    public static final String KOSMOS_MASTER    = "kosmos_master";

    // Infamy
    public static final String INFAMY_BANKROLL = "infamy_bankroll";
    public static final String INFAMY_RICH     = "infamy_rich";
    public static final String INFAMY_REPEAT   = "infamy_repeat";
    public static final String INFAMY_CASE     = "infamy_case";
    public static final String INFAMY_TASTE    = "infamy_taste";
    public static final String INFAMY_BURN     = "infamy_burn";
    public static final String INFAMY_SEWER    = "infamy_sewer";
    public static final String INFAMY_GRATE    = "infamy_grate";
    public static final String INFAMY_SCOPE    = "infamy_scope";
    public static final String INFAMY_THREE    = "infamy_three";
    public static final String INFAMY_KINGPIN  = "infamy_kingpin";

    // Domestic
    public static final String DOMESTIC_DANIEL      = "domestic_daniel";
    public static final String DOMESTIC_DANIEL_FIVE = "domestic_daniel_five";
    public static final String DOMESTIC_NUGGET_MEET = "domestic_nugget_meet";
    public static final String DOMESTIC_NUGGET_FED  = "domestic_nugget_fed";
    public static final String DOMESTIC_COLLAR      = "domestic_collar";
    public static final String DOMESTIC_NUGGET_TAME = "domestic_nugget_tame";
    public static final String DOMESTIC_MOM_MEET    = "domestic_mom_meet";
    public static final String DOMESTIC_MOM_GIFT    = "domestic_mom_gift";
    public static final String DOMESTIC_DUHL_MEET   = "domestic_duhl_meet";
    public static final String DOMESTIC_DUHL_PAY    = "domestic_duhl_pay";
    public static final String DOMESTIC_DUHL_CLEAR  = "domestic_duhl_clear";
    public static final String DOMESTIC_DUHL_FIGHT  = "domestic_duhl_fight";
    public static final String DOMESTIC_DUHL_REG    = "domestic_duhl_regular";
    public static final String DOMESTIC_FAMILY      = "domestic_family";

    // The Grind
    public static final String GRIND_LEVEL_TEN    = "grind_level_ten";
    public static final String GRIND_LEVEL_THIRTY = "grind_level_thirty";
    public static final String GRIND_LEVEL_FIFTY  = "grind_level_fifty";
    public static final String GRIND_CLEAN        = "grind_clean";
    public static final String GRIND_WALK         = "grind_walk";
    public static final String GRIND_JUMP         = "grind_jump";
    public static final String GRIND_PAIN         = "grind_pain";
    public static final String GRIND_KILLS        = "grind_kills";
    public static final String GRIND_TIME         = "grind_time";
    public static final String GRIND_CEREAL       = "grind_cereal";
    public static final String GRIND_PIBB         = "grind_pibb";
    public static final String GRIND_NUGGETS      = "grind_nuggets";
    public static final String GRIND_HOARDER      = "grind_hoarder";
    public static final String GRIND_LEGEND       = "grind_legend";

    // ---- thresholds ---------------------------------------------------------
    // Shared with QuestExpansionEvents so the objective text and the check that
    // grants the quest can never drift apart.

    public static final int STASH_HANDFUL_COUNT = 16;
    public static final int STASH_DICED_COUNT   = 32;
    public static final int STASH_BURNT_COUNT   = 32;
    public static final int STASH_PAPER_COUNT   = 16;
    public static final int STASH_JOINT_COUNT   = 8;
    public static final int STASH_BROWNIE_COUNT = 4;
    public static final int STASH_GOLDEN_COUNT  = 2;
    public static final int SMOKE_FIRST         = 5;
    public static final int SMOKE_CHAIN         = 25;
    public static final int BARBARA_FEEDS       = 10;

    public static final int CAYDEN_FED_TEN      = 10;
    public static final int CAYDEN_FED_RAGE     = 25;    // mirrors CaydenCobb.RAGE_THRESHOLD
    public static final int PANTRY_CEREAL       = 16;
    public static final int PANTRY_MILK         = 4;
    public static final int HOUSE_UPGRADE_AIR   = 120;
    public static final int HOUSE_MANSION_AIR   = 300;
    public static final int MEDIC_FEEDS         = 3;
    public static final int PROVIDER_DAYS       = 5;

    public static final int KOSMOS_TRAVEL       = 600;   // blocks moved inside the dimension
    public static final int KOSMOS_STAY_TICKS   = 12000; // 10 minutes
    public static final int KOSMOS_CEILING      = 140;   // y to climb to
    public static final int KOSMOS_ISLAND_RANGE = 48;    // how close to the boss island counts

    public static final int INFAMY_DOLLARS      = 64;
    public static final int INFAMY_PLUG_DEALS   = 2;     // $500 handed over, twice
    public static final int INFAMY_PASTRY_CASE  = 8;
    public static final int INFAMY_BURN_COUNT   = 3;
    public static final int INFAMY_SEWER_TICKS  = 12000; // 10 minutes below y=40
    public static final int INFAMY_GRATE_DEPTH  = 30;
    public static final int INFAMY_SNIPE_RANGE  = 32;    // blocks
    public static final int INFAMY_PLUG_KILLS   = 3;

    public static final int DANIEL_LIGHTERS     = 5;
    public static final int NUGGET_TREATS       = 3;
    public static final int DUHL_TRIBUTE_DAYS   = 3;

    public static final int GRIND_LVL_ONE       = 10;
    public static final int GRIND_LVL_TWO       = 30;
    public static final int GRIND_LVL_THREE     = 50;
    public static final int GRIND_WALK_CM       = 1_000_000;   // 10 km
    public static final int GRIND_JUMPS         = 2500;
    public static final int GRIND_DAMAGE_TENTHS = 5000;        // 500 half-hearts of pain
    public static final int GRIND_MOB_KILLS     = 150;
    public static final int GRIND_PLAY_TICKS    = 360_000;     // 5 hours
    public static final int GRIND_CEREAL_EATEN  = 50;
    public static final int GRIND_PIBB_DRUNK    = 20;
    public static final int GRIND_NUGGETS_EATEN = 40;
    public static final int GRIND_VARIETY       = 20;    // distinct mod items carried at once

    // ---- the spec -----------------------------------------------------------

    /**
     * One expansion quest. Deliberately field-for-field identical to
     * {@code Quests.Quest} minus {@code finale}, which no expansion quest uses -
     * the base game's PEACE is still the one and only finale.
     */
    public static final class Spec {
        public final String id;
        public final String branch;
        public final String title;
        public final String objective;
        public final String[] prereqs;
        public final RegistryObject<Item>[] collect;
        public final boolean event;

        Spec(String id, String branch, String title, String objective, String[] prereqs,
             boolean event, RegistryObject<Item>[] collect) {
            this.id = id;
            this.branch = branch;
            this.title = title;
            this.objective = objective;
            this.prereqs = prereqs;
            this.collect = collect;
            this.event = event;
        }
    }

    /**
     * Every new quest, in book order.
     *
     * <p>THE SPLICE: one statement at the very end of the {@code static} block in
     * {@code Quests.java} pulls the whole board in -
     * <pre>
     * for (QuestExpansion.Spec s : QuestExpansion.ALL) {
     *     add(new Quest(s.id, s.branch, s.title, s.objective, s.prereqs, s.event, false, s.collect));
     * }
     * </pre>
     * It must run inside {@code Quests} because {@code Quest}'s constructor and
     * {@code add()} are package-private.
     */
    public static final List<Spec> ALL = new ArrayList<>();

    private static final Map<String, Spec> BY_ID = new LinkedHashMap<>();

    /** All six new branch labels, in book order. */
    public static final List<String> BRANCHES = List.of(
            B_STASH, B_PROVIDER, B_KOSMOS, B_INFAMY, B_DOMESTIC, B_GRIND);

    public static Spec byId(String id) {
        return BY_ID.get(id);
    }

    // ---- builders -----------------------------------------------------------

    private static String[] pre(String... ids) {
        return ids;
    }

    @SafeVarargs
    private static RegistryObject<Item>[] items(RegistryObject<Item>... it) {
        return it;
    }

    private static void add(Spec s) {
        ALL.add(s);
        BY_ID.put(s.id, s);
    }

    /** Auto-completes the moment every listed item is in the player's inventory. */
    @SafeVarargs
    private static void collect(String id, String branch, String title, String objective,
                                String[] prereqs, RegistryObject<Item>... it) {
        add(new Spec(id, branch, title, objective, prereqs, false, items(it)));
    }

    /** Completes only when {@link QuestExpansionEvents} sees the condition met. */
    private static void goal(String id, String branch, String title, String objective,
                             String... prereqs) {
        add(new Spec(id, branch, title, objective, prereqs, true, items()));
    }

    /** A capstone: completes the instant its prerequisites are all done. */
    private static void milestone(String id, String branch, String title, String objective,
                                  String... prereqs) {
        add(new Spec(id, branch, title, objective, prereqs, false, items()));
    }

    static {

        // ================= THE STASH =========================================
        // Barbara's whole economy: rip up a lawn, cut it, cure it, roll it, and
        // above all keep HER supplied, because an empty stash is a psycho Barbara.

        goal(STASH_HANDFULS, B_STASH, "Rip Up the Lawn",
                "Punch grass blocks until you are carrying " + STASH_HANDFUL_COUNT
                        + " HANDFULS OF GRASS at once. She does not smoke seeds. She smokes the "
                        + "lawn, and the lawn is free.",
                Quests.GRASS_HARVEST);
        goal(STASH_DICED, B_STASH, "Bulk Prep",
                "Run the lawn through the GRASS KNIFE until you hold " + STASH_DICED_COUNT
                        + " DICED GRASS at once. This is the boring part of every business.",
                STASH_HANDFULS);
        goal(STASH_CURED, B_STASH, "The Curing Room",
                "Smelt diced grass into BURNT GRASS until you have " + STASH_BURNT_COUNT
                        + " on you. A furnace is a curing room if you believe hard enough.",
                STASH_DICED);
        goal(STASH_PAPERS, B_STASH, "Papers, Please",
                "Carry " + STASH_PAPER_COUNT + " ROLLING PAPER and " + STASH_JOINT_COUNT
                        + " ROLLED JOINTS at the same time. Sugar cane makes the paper, burnt "
                        + "grass fills it. Inventory, not chest.",
                STASH_CURED);
        collect(STASH_JARRED, B_STASH, "Weighed and Jarred",
                "Go semi-legitimate: craft a MASON JAR OF STASH (glass panes ringing burnt grass) "
                        + "and a POCKET SCALE. If you are not weighing it, you are just gardening.",
                pre(STASH_CURED),
                ModItems.STASH_JAR, ModItems.POCKET_SCALE);
        goal(STASH_SMOKE_FIVE, B_STASH, "Personal Use",
                "Hold right-click with a ROLLED JOINT until it finishes - FIVE times total. "
                        + "Quality control, technically. Do it away from Barbara.",
                STASH_PAPERS);
        goal(STASH_CHAIN, B_STASH, "Chain Smoker",
                "Smoke " + SMOKE_CHAIN + " joints, total, across your whole save. Barbara is "
                        + "counting and Barbara is judging.",
                STASH_SMOKE_FIVE);
        goal(STASH_SUPPLIER, B_STASH, "Her Supplier",
                "RIGHT-CLICK BARBARA holding a HANDFUL OF GRASS to top her stash up. Do it "
                        + BARBARA_FEEDS + " times. A fed Barbara is a calm Barbara. Give her a "
                        + "minute and she will find you on her own.",
                Quests.GRASS_HARVEST);
        goal(STASH_WITHDRAWAL, B_STASH, "Out of Stash",
                "Let her stash run all the way down and be standing there when it does. She goes "
                        + "PSYCHO - bigger, faster, screaming. Watch from inside a doorway.",
                STASH_SUPPLIER);
        goal(STASH_RESUPPLY, B_STASH, "Emergency Resupply",
                "Walk up to a RAGING Barbara and hand her grass anyway: right-click her with a "
                        + "HANDFUL OF GRASS while she is mid-episode. Wear the armour.",
                STASH_WITHDRAWAL);
        goal(STASH_BROWNIES, B_STASH, "Bake Sale, For Real",
                "Carry " + STASH_BROWNIE_COUNT + " GRASS BROWNIES at once. Do not label the tray. "
                        + "Do not leave the tray out. Do not let Cayden near the tray.",
                Quests.GRASS_EDIBLES);
        goal(STASH_TOP_SHELF, B_STASH, "Top Shelf Only",
                "Hold " + STASH_GOLDEN_COUNT + " GOLDEN JOINTS at the same time. Gold, paper and "
                        + "grass: it is a felony and a flex.",
                STASH_BROWNIES);
        milestone(STASH_EMPIRE, B_STASH, "The Whole Operation",
                "Grown, cut, cured, rolled, weighed, jarred, smoked and survived. Barbara Jones "
                        + "has a supply chain now.",
                STASH_PAPERS, STASH_JARRED, STASH_CHAIN, STASH_RESUPPLY, STASH_TOP_SHELF);

        // ================= PROVIDER ==========================================
        // Rule #1 turned into a progression bar. Everything here is about the kid
        // still being alive at the end of it.

        goal(PROVIDER_FIRST_BOWL, B_PROVIDER, "First Bowl",
                "Right-click Cayden with KRAVE CEREAL in hand. He tames on the first bowl and he "
                        + "will tell the entire server about it.",
                Quests.CAYDEN_TOOLS);
        goal(PROVIDER_TEN, B_PROVIDER, "Growing Boy",
                "Feed him " + CAYDEN_FED_TEN + " boxes of Krave. His attack climbs every bowl. So "
                        + "does his waistline - every 5 boxes he gets slower.",
                PROVIDER_FIRST_BOWL);
        goal(PROVIDER_RAGE, B_PROVIDER, "Unlock the Rage",
                "Feed him " + CAYDEN_FED_RAGE + " boxes total to unlock KRAVE RAGE. Twenty-five "
                        + "boxes of cereal for one angry child. Worth it.",
                PROVIDER_TEN);
        goal(PROVIDER_PANTRY, B_PROVIDER, "Stocked Pantry",
                "Keep " + PANTRY_CEREAL + " KRAVE CEREAL and " + PANTRY_MILK + " KRAVE MILK on you "
                        + "at once. Running out mid-cave is how Rule #1 gets broken.",
                PROVIDER_FIRST_BOWL);
        goal(PROVIDER_ROOF, B_PROVIDER, "A Roof Over His Head",
                "Build a sealed room with a door, a bed, a floor and a light, then right-click him "
                        + "EMPTY-HANDED inside it. The HOUSING QUERY tool tells you what is missing.",
                Quests.CAYDEN_TOOLS);
        goal(PROVIDER_UPGRADE, B_PROVIDER, "Room Upgrade",
                "Get his claimed room up to " + HOUSE_UPGRADE_AIR + " blocks of interior air. "
                        + "Knock a wall out. He is not a hamster.",
                PROVIDER_ROOF);
        goal(PROVIDER_MANSION, B_PROVIDER, "The Cobb Mansion",
                "Push the same room past " + HOUSE_MANSION_AIR + " blocks of interior air, still "
                        + "sealed, still lit, still with a bed. A runaway with an estate.",
                PROVIDER_UPGRADE);
        goal(PROVIDER_MEDIC, B_PROVIDER, "Field Medic",
                "Krave heals him 2 hearts a box. Feed him " + MEDIC_FEEDS + " times while he is "
                        + "under HALF health - that is the difference between a scare and a siren.",
                PROVIDER_FIRST_BOWL);
        goal(PROVIDER_FIVE_DAYS, B_PROVIDER, "Five Days, Still Breathing",
                "Survive " + PROVIDER_DAYS + " in-game days with a living Cayden nearby. Sleep, "
                        + "light the place, keep the compass on you.",
                PROVIDER_ROOF);
        goal(PROVIDER_SPOTLESS, B_PROVIDER, "Rule #1, Unbroken",
                "Reach " + PROVIDER_DAYS + " days together having never once triggered the Krave "
                        + "Apocalypse. Zero death stages on the record. Most players cannot.",
                PROVIDER_FIVE_DAYS);
        collect(PROVIDER_CARE, B_PROVIDER, "Care Package",
                "Assemble the survival kit he actually wants: a NUGGET BOX, FRIES, a DONUT BOX, a "
                        + "MR PIBB and a pack of TOASTER PASTRIES. All at once.",
                pre(Quests.CAYDEN_FASTFOOD),
                ModItems.NUGGET_BOX, ModItems.FRIES, ModItems.DONUT_BOX,
                ModItems.MR_PIBB, ModItems.TOASTER_PASTRIES);
        milestone(PROVIDER_GUARDIAN, B_PROVIDER, "Cayden's Guardian",
                "Fed, raged, housed, upgraded, patched up and five days clean. Nobody has ever "
                        + "looked after that kid this well, including his mother.",
                PROVIDER_RAGE, PROVIDER_PANTRY, PROVIDER_MANSION, PROVIDER_MEDIC,
                PROVIDER_SPOTLESS, PROVIDER_CARE);

        // ================= THE KOSMOS ========================================
        // The dimension: build the door, cross, survive the chocolate, cross the
        // islands, and be there for the ascension.

        collect(KOSMOS_KIT, B_KOSMOS, "Build the Door",
                "Craft KRAVE BLOCKS (8 obsidian around an amethyst shard) and a KRAVE DOOR "
                        + "(6 krave planks). Then wall the door in - a whole room, floor and roof, "
                        + "all krave block, not just a frame standing in open air. Only one column "
                        + "stays hollow: the one behind the door.",
                pre(Quests.COOK_KRAVE),
                ModItems.KRAVE_BLOCK_ITEM, ModItems.KRAVE_DOOR_ITEM);
        goal(KOSMOS_ARRIVAL, B_KOSMOS, "Krave Kosmos",
                "Walk into the finished room and shut the door behind you. Purple sky, floating "
                        + "islands, an ocean of liquid chocolate. Bring blocks. Bring a lot of blocks. "
                        + "There is no tether, no backup, no second door. Remember exactly where "
                        + "yours is - it is the only way home you get.",
                KOSMOS_KIT);
        goal(KOSMOS_CHOCOLATE, B_KOSMOS, "It's Hot Chocolate",
                "Touch the liquid chocolate and live. It burns exactly like lava, because it is "
                        + "chocolate the temperature of lava. One tap. Then get out.",
                KOSMOS_ARRIVAL);
        collect(KOSMOS_BUCKET, B_KOSMOS, "Take Some Home",
                "Scoop a BUCKET OF LIQUID CHOCOLATE straight out of the sea. Empty bucket, "
                        + "right-click the surface, do not fall in.",
                pre(KOSMOS_CHOCOLATE),
                ModItems.CHOCOLATE_BUCKET);
        collect(KOSMOS_BAR, B_KOSMOS, "Solid Form",
                "Craft a KOSMIC CHOCOLATE BAR from 2 COCOA SUBSTITUTE and a SUGAR. Same sea, "
                        + "portable, and it does not melt your boots.",
                pre(KOSMOS_BUCKET),
                ModItems.CHOCOLATE_BAR);
        collect(KOSMOS_SOIL, B_KOSMOS, "Kosmic Farming",
                "Dig up KRAVE GRASS and KRAVE DIRT and carry both home. Yes it grows in the "
                        + "Overworld. Yes Barbara will try to smoke it.",
                pre(KOSMOS_ARRIVAL),
                ModItems.KRAVE_GRASS_ITEM, ModItems.KRAVE_DIRT_ITEM);
        goal(KOSMOS_HIGH, B_KOSMOS, "Above the Chocolate",
                "Climb to y=" + KOSMOS_CEILING + " or higher inside the Kosmos. Pillar up an "
                        + "island if you have to. The view is the whole point.",
                KOSMOS_ARRIVAL);
        goal(KOSMOS_HOP, B_KOSMOS, "Island Hopping",
                "Cover " + KOSMOS_TRAVEL + " blocks of ground inside the Kosmos. Bridge between "
                        + "islands - falling in the chocolate does not count as travel.",
                KOSMOS_ARRIVAL);
        goal(KOSMOS_RESIDENT, B_KOSMOS, "Kosmic Resident",
                "Spend 10 minutes total in the dimension. It does not have to be in one trip. It "
                        + "will not feel like 10 minutes.",
                KOSMOS_ARRIVAL);
        goal(KOSMOS_ISLAND, B_KOSMOS, "The Boss Island",
                "Reach the main island at the middle of the Kosmos - coordinates near x=0, z=0. "
                        + "That is his house. Knock.",
                KOSMOS_HOP);
        goal(KOSMOS_ASCENSION, B_KOSMOS, "The Ascension",
                "Bring Cayden with you and watch him go SUPER SAIYAN in the Kosmos - golden aura, "
                        + "flight, the works. Keep him alive through it anyway.",
                KOSMOS_ARRIVAL);
        milestone(KOSMOS_MASTER, B_KOSMOS, "Kosmonaut",
                "Door built, chocolate survived, islands crossed, ceiling reached, ascension "
                        + "witnessed. You live there now, basically.",
                KOSMOS_BAR, KOSMOS_SOIL, KOSMOS_HIGH, KOSMOS_RESIDENT,
                KOSMOS_ISLAND, KOSMOS_ASCENSION);

        // ================= INFAMY ============================================
        // The money, the fake product, the sewer and the man who sold it to you.

        collect(INFAMY_BANKROLL, B_INFAMY, "Bankroll",
                "Hold MOM'S $500 and a WAD OF CASH at the same time. One is for The Plug. One is "
                        + "for Duhl Wol. Neither is yours.",
                pre(Quests.PLUG_KIT),
                ModItems.FIVE_HUNDRED_DOLLARS, ModItems.DOLLARS);
        goal(INFAMY_RICH, B_INFAMY, "Getting Paid",
                "Stack " + INFAMY_DOLLARS + " WAD OF CASH in your inventory at once. Duhl Wol "
                        + "counts it faster than you can.",
                INFAMY_BANKROLL);
        goal(INFAMY_REPEAT, B_INFAMY, "Fool Me Twice",
                "Hand The Plug MOM'S $500 a second time. He drops the roll about half the time "
                        + "when he goes down, so the money keeps coming back. You knew. You did it anyway.",
                Quests.THE_PLUG_DEAL);
        goal(INFAMY_CASE, B_INFAMY, "The Whole Case",
                "End up holding " + INFAMY_PASTRY_CASE + " OFF-BRAND PASTRIES. He does not sell "
                        + "singles, he sells regret in bulk.",
                Quests.PLUG_SCAM);
        goal(INFAMY_TASTE, B_INFAMY, "Tastes Like Cardboard",
                "Actually eat the product: finish an OFF-BRAND PASTRY, some FAKE WEED or the FAKE "
                        + "COCAINE. Now you know. Now you are angry.",
                Quests.PLUG_SCAM);
        goal(INFAMY_BURN, B_INFAMY, "Burn the Evidence",
                "Hold the LIGHTER and right-click while fake product is in your inventory - it "
                        + "goes up in flames. Torch " + INFAMY_BURN_COUNT + " of them.",
                INFAMY_TASTE);
        goal(INFAMY_SEWER, B_INFAMY, "Sewer Dweller",
                "Spend 10 minutes total below y=40. Down there nobody asks where the $500 went.",
                Quests.PLUG_KIT);
        goal(INFAMY_GRATE, B_INFAMY, "Down the Grate",
                "Be holding a SEWER GRATE below y=" + INFAMY_GRATE_DEPTH + ". Standing in your own "
                        + "sewer with the lid in your hand: that is the whole aesthetic.",
                INFAMY_SEWER);
        goal(INFAMY_SCOPE, B_INFAMY, "Through the Scope",
                "Kill something from " + INFAMY_SNIPE_RANGE + "+ blocks away with a SNIPER SCOPE "
                        + "in your inventory. Bow, crossbow, gravity, his own trick.",
                Quests.PLUG_KIT);
        goal(INFAMY_THREE, B_INFAMY, "Three Times",
                "Put The Plug in the ground " + INFAMY_PLUG_KILLS + " times. He keeps coming back "
                        + "and so does the $500 he drops.",
                Quests.REVENGE);
        milestone(INFAMY_KINGPIN, B_INFAMY, "Street Legend",
                "The money, the burner, the sewer, the scope and three funerals for one man in a "
                        + "ski mask. The block knows your name.",
                INFAMY_RICH, INFAMY_REPEAT, INFAMY_CASE, INFAMY_BURN, INFAMY_GRATE,
                INFAMY_SCOPE, INFAMY_THREE);

        // ================= DOMESTIC ==========================================
        // The supporting cast. They turn up on their own once these unlock.

        goal(DOMESTIC_DANIEL, B_DOMESTIC, "The Lighter Guy",
                "Daniel wanders by eventually - tall, green jacket, terrified of you. Right-click "
                        + "him EMPTY-HANDED and he hands over a LIGHTER rather than talk.",
                Quests.START);
        goal(DOMESTIC_DANIEL_FIVE, B_DOMESTIC, "Lighter Collection",
                "Get " + DANIEL_LIGHTERS + " lighters out of Daniel. He never says no. He never "
                        + "says anything.",
                DOMESTIC_DANIEL);
        goal(DOMESTIC_NUGGET_MEET, B_DOMESTIC, "NUGGET! NUGGET, GET IN HERE!",
                "Barbara's ginger cat gets out constantly, and now that you know Barbara you will "
                        + "hear about it. Find Nugget and get within a few blocks of her.",
                Quests.RECRUIT_BARBARA);
        goal(DOMESTIC_NUGGET_FED, B_DOMESTIC, "The Cat Tax",
                "Right-click Nugget with CHICKEN NUGGETS or a DONUT, " + NUGGET_TREATS + " times. "
                        + "She is named after one and she knows it.",
                DOMESTIC_NUGGET_MEET);
        collect(DOMESTIC_COLLAR, B_DOMESTIC, "Name Tag",
                "Craft NUGGET'S COLLAR (leather + orange dye + string). Barbara lost the original "
                        + "one and blamed the cat.",
                pre(DOMESTIC_NUGGET_FED),
                ModItems.NUGGET_COLLAR);
        goal(DOMESTIC_NUGGET_TAME, B_DOMESTIC, "Officially Yours",
                "Right-click Nugget with the COLLAR. It goes on, she takes your name, and Barbara "
                        + "never has to be told.",
                DOMESTIC_COLLAR);
        goal(DOMESTIC_MOM_MEET, B_DOMESTIC, "Mom Cobb",
                "Word gets around that a stranger is housing her son. Cayden's mother comes "
                        + "looking - get within a few blocks and let her look at you.",
                Quests.CAYDEN_TOOLS);
        goal(DOMESTIC_MOM_GIFT, B_DOMESTIC, "Peace Offering",
                "Right-click Mom Cobb holding a DONUT BOX. It is not an apology. It is a box of "
                        + "donuts, which is close enough for this family.",
                DOMESTIC_MOM_MEET);
        goal(DOMESTIC_DUHL_MEET, B_DOMESTIC, "He Pulled Up",
                "Duhl Wol comes for tribute once a day, in the car, at speed. Be within sight when "
                        + "he gets out. You have 5 minutes from that moment.",
                Quests.START);
        goal(DOMESTIC_DUHL_PAY, B_DOMESTIC, "Pay The Man",
                "He names an item - dirt, stone, andesite, cash. Right-click him holding it. "
                        + "Paying is always cheaper than the alternative.",
                DOMESTIC_DUHL_MEET);
        goal(DOMESTIC_DUHL_CLEAR, B_DOMESTIC, "Debt Cleared",
                "Pay through all three of his demands in one visit until he says \"appreciate it, "
                        + "homie\" and drives off. Settled. Until tomorrow.",
                DOMESTIC_DUHL_PAY);
        goal(DOMESTIC_DUHL_FIGHT, B_DOMESTIC, "NOW We Fight",
                "Hit him instead of paying him and finish what you started. He hits back hard and "
                        + "he does not forget. Neither does the next visit.",
                DOMESTIC_DUHL_MEET);
        goal(DOMESTIC_DUHL_REG, B_DOMESTIC, "Regular Customer",
                "Pay tribute on " + DUHL_TRIBUTE_DAYS + " different days. It is not extortion, it "
                        + "is a subscription.",
                DOMESTIC_DUHL_PAY);
        milestone(DOMESTIC_FAMILY, B_DOMESTIC, "Family Business",
                "Daniel supplies you, Nugget tolerates you, Mom Cobb has been bribed and Duhl Wol "
                        + "has been paid. This is what stability looks like around here.",
                DOMESTIC_DANIEL_FIVE, DOMESTIC_NUGGET_TAME, DOMESTIC_MOM_GIFT,
                DOMESTIC_DUHL_CLEAR, DOMESTIC_DUHL_REG);

        // ================= THE GRIND =========================================
        // Numbers go up. That is the entire branch and it is unashamed of itself.

        goal(GRIND_LEVEL_TEN, B_GRIND, "Level 10",
                "Hit experience level 10. Mine, breed, smelt, whatever - the Krave economy runs on "
                        + "XP like everything else.",
                Quests.START);
        goal(GRIND_LEVEL_THIRTY, B_GRIND, "Level 30",
                "Hit experience level 30. That is one full enchant and a lot of nights spent not "
                        + "watching Cayden.",
                GRIND_LEVEL_TEN);
        goal(GRIND_LEVEL_FIFTY, B_GRIND, "Level 50",
                "Hit experience level 50. At this point you are just showing off in front of a "
                        + "middle-aged woman who smokes lawn.",
                GRIND_LEVEL_THIRTY);
        goal(GRIND_CLEAN, B_GRIND, "Untouchable",
                "Reach level " + GRIND_LVL_TWO + " having never died. Not once. The counter is on "
                        + "your stats page and it is watching.",
                GRIND_LEVEL_TEN);
        goal(GRIND_WALK, B_GRIND, "Touch Grass, Literally",
                "Walk 10 kilometres. Ten thousand blocks of touching grass, which is what everyone "
                        + "keeps telling Barbara to do.",
                GRIND_LEVEL_TEN);
        goal(GRIND_JUMP, B_GRIND, "Krave Hops",
                "Jump " + GRIND_JUMPS + " times. The Krave Monster does it better and he does it "
                        + "at you.",
                GRIND_LEVEL_TEN);
        goal(GRIND_PAIN, B_GRIND, "Take a Beating",
                "Absorb 250 hearts of damage across your save. Fall damage counts. Cayden's "
                        + "friendly fire counts. It all counts.",
                GRIND_LEVEL_TEN);
        goal(GRIND_KILLS, B_GRIND, "Body Count",
                "Kill " + GRIND_MOB_KILLS + " mobs. Half of them were going for Cayden anyway, so "
                        + "call it self-defence by proxy.",
                GRIND_LEVEL_TEN);
        goal(GRIND_TIME, B_GRIND, "Chronically Online",
                "Log 5 hours of play time in this world. Barbara has been out there smoking the "
                        + "lawn that entire time.",
                GRIND_LEVEL_TEN);
        goal(GRIND_CEREAL, B_GRIND, "Cereal Killer",
                "Eat " + GRIND_CEREAL_EATEN + " KRAVE CEREAL yourself, not counting anything you "
                        + "fed the kid. I KRAVE THE KRAVE.",
                Quests.COOK_KRAVE);
        goal(GRIND_PIBB, B_GRIND, "Pibb Addict",
                "Drink " + GRIND_PIBB_DRUNK + " MR PIBB. It is not sponsorship if nobody is paying "
                        + "you, it is just a problem.",
                Quests.CAYDEN_DRINKS);
        goal(GRIND_NUGGETS, B_GRIND, "Nugget Economy",
                "Eat " + GRIND_NUGGETS_EATEN + " CHICKEN NUGGETS. Do not do it in front of the cat "
                        + "of the same name.",
                Quests.CAYDEN_FASTFOOD);
        goal(GRIND_HOARDER, B_GRIND, "Hoarder",
                "Carry " + GRIND_VARIETY + " DIFFERENT Barbara Jones items at the same time - any "
                        + "twenty of them. Duhl Wol is going to have opinions about that inventory.",
                GRIND_LEVEL_TEN);
        milestone(GRIND_LEGEND, B_GRIND, "Certified Legend",
                "Levelled, walked, jumped, bled, fed and hoarded. The numbers all went up. That "
                        + "was the whole assignment.",
                GRIND_LEVEL_FIFTY, GRIND_CLEAN, GRIND_WALK, GRIND_JUMP, GRIND_PAIN,
                GRIND_KILLS, GRIND_TIME, GRIND_CEREAL, GRIND_PIBB, GRIND_NUGGETS,
                GRIND_HOARDER);
    }
}
