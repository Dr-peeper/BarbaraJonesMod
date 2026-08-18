package com.barbarajones.quest;

import com.barbarajones.content.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The Krave questline - completely rebuilt as a BRANCHING graph.
 *
 * <p>The old system was a single {@code QuestStage} integer that marched 0..10 in
 * one straight line. This one is a directed graph: a single hub ({@link #START})
 * fans out into six parallel directions - Story, Grass, Krave, Cayden, Fame and
 * the Streets - that can be worked in any order and re-converge on the finale.
 *
 * <p>Two ways a quest completes:
 * <ul>
 *   <li><b>Collect quests</b> auto-complete the instant every listed item is in
 *       your inventory (and their prerequisites are done). Between them, the
 *       collect quests name <em>every single item in the mod</em> - finishing the
 *       questline means you have literally collected one of everything.</li>
 *   <li><b>Event quests</b> ({@link Quest#event}) only complete when the game
 *       fires them - recruiting Barbara, summoning and slaying the Krave, the
 *       $500 scam, the sewer, the revenge. These are the canon spine that laces
 *       the collect branches together.</li>
 * </ul>
 *
 * <p>Progress lives on the Quest Book ItemStack's own NBT (a string list of
 * completed quest ids), which rides normal inventory sync to the client - no
 * custom packets needed.
 */
public final class Quests {

    // ---- quest ids ----------------------------------------------------------
    // Hub
    public static final String START           = "start";

    // Story spine (event-driven canon)
    public static final String LIBRARY         = "library";
    public static final String RECRUIT_BARBARA = "recruit_barbara";
    public static final String SUMMON_KRAVE     = "summon_krave";
    public static final String SLAY_KRAVE       = "slay_krave";
    public static final String KRAVE_DANCE      = "krave_dance";
    public static final String SMASH_MOUSE      = "smash_mouse";
    public static final String THE_PLUG_DEAL    = "the_plug_deal";
    public static final String THE_SEWER        = "the_sewer";
    public static final String REVENGE          = "revenge";

    // Grass branch
    public static final String GRASS_HARVEST    = "grass_harvest";
    public static final String GRASS_PREP        = "grass_prep";
    public static final String GRASS_DRY         = "grass_dry";
    public static final String GRASS_ROLL        = "grass_roll";
    public static final String GRASS_EDIBLES     = "grass_edibles";

    // Krave branch
    public static final String COCOA_CHAIN       = "cocoa_chain";
    public static final String COOK_KRAVE        = "cook_krave";
    public static final String KRAVE_FEAST       = "krave_feast";
    public static final String KRAVE_FORGE       = "krave_forge";
    public static final String KRAVE_TAPE        = "krave_tape";

    // Cayden branch
    public static final String CAYDEN_TOOLS      = "cayden_tools";
    public static final String CAYDEN_FASTFOOD   = "cayden_fastfood";
    public static final String CAYDEN_DRINKS     = "cayden_drinks";
    public static final String CAYDEN_HYGIENE    = "cayden_hygiene";
    public static final String CAYDEN_PAPERS     = "cayden_papers";

    // Fame branch
    public static final String FAME_GEAR         = "fame_gear";
    public static final String FAME_MERCH        = "fame_merch";
    public static final String FAME_REDFIT       = "fame_redfit";
    public static final String FAME_COMPUTER     = "fame_computer";

    // Streets branch
    public static final String PLUG_KIT          = "plug_kit";
    public static final String PLUG_SCAM         = "plug_scam";
    public static final String PASTRY_AISLE      = "pastry_aisle";

    // The finale
    public static final String PEACE             = "peace";

    // ---- branch labels (drive the book UI grouping / colour) ---------------
    public static final String B_STORY  = "Story";
    public static final String B_GRASS  = "Grass";
    public static final String B_KRAVE  = "Krave";
    public static final String B_CAYDEN = "Cayden";
    public static final String B_FAME   = "Fame";
    public static final String B_STREET = "Streets";
    public static final String B_END    = "The End";

    // ---- the graph ----------------------------------------------------------

    /** One node in the quest graph. */
    public static final class Quest {
        public final String id;
        public final String branch;
        public final String title;
        public final String objective;
        public final String[] prereqs;         // every prereq must be done to unlock
        public final RegistryObject<Item>[] collect; // all present in inventory -> auto-complete
        public final boolean event;            // completes only via complete(), never auto
        public final boolean finale;           // unlocks only when everything else is done

        Quest(String id, String branch, String title, String objective, String[] prereqs,
              boolean event, boolean finale, RegistryObject<Item>[] collect) {
            this.id = id;
            this.branch = branch;
            this.title = title;
            this.objective = objective;
            this.prereqs = prereqs;
            this.collect = collect;
            this.event = event;
            this.finale = finale;
        }
    }

    /** Insertion-ordered so the book renders branches in a stable order. */
    public static final List<Quest> ALL = new ArrayList<>();
    private static final Map<String, Quest> BY_ID = new LinkedHashMap<>();
    /** Distinct branch labels in first-seen order. */
    public static final List<String> BRANCHES = new ArrayList<>();

    private static String[] pre(String... ids) {
        return ids;
    }

    @SafeVarargs
    private static RegistryObject<Item>[] items(RegistryObject<Item>... it) {
        return it;
    }

    private static Quest add(Quest q) {
        ALL.add(q);
        BY_ID.put(q.id, q);
        if (!BRANCHES.contains(q.branch)) {
            BRANCHES.add(q.branch);
        }
        return q;
    }

    @SafeVarargs
    private static void collect(String id, String branch, String title, String objective,
                                String[] prereqs, RegistryObject<Item>... it) {
        add(new Quest(id, branch, title, objective, prereqs, false, false, items(it)));
    }

    private static void event(String id, String branch, String title, String objective,
                              String... prereqs) {
        add(new Quest(id, branch, title, objective, prereqs, true, false, items()));
    }

    private static void milestone(String id, String branch, String title, String objective,
                                  boolean finale, String... prereqs) {
        add(new Quest(id, branch, title, objective, prereqs, false, finale, items()));
    }

    static {
        // ---- the hub --------------------------------------------------------
        milestone(START, B_STORY, "The Craving",
                "It starts with a bowl of cereal. Your Quest Book just branched wide open - "
                        + "chase any thread you like. Read THE KRAVE MANUAL; Rule #1 is on page one.",
                false);

        // ---- STORY spine (event-driven canon) -------------------------------
        collect(LIBRARY, B_STORY, "Required Reading",
                "Keep the three books you were handed: the KRAVE MANUAL, the QUEST BOOK and the "
                        + "green COOKBOOK. Everything you need to craft is in the cookbook.",
                pre(START),
                ModItems.KRAVE_MANUAL, ModItems.QUEST_BOOK, ModItems.RECIPE_BOOK);

        event(RECRUIT_BARBARA, B_STORY, "Therapy",
                "Find BARBARA JONES and RIGHT-CLICK HER with an EMPTY HAND to recruit her. Grass in "
                        + "hand feeds her instead; taunting her with it makes her PSYCHO.",
                GRASS_HARVEST);
        event(SUMMON_KRAVE, B_STORY, "The Summoning",
                "With Barbara recruited and a KRAVE BOX in hand, RIGHT-CLICK the box to summon THE "
                        + "KRAVE MONSTER. Clear ground recommended: boss bar, huge jumps, constant teleporting.",
                RECRUIT_BARBARA, KRAVE_FEAST);
        event(SLAY_KRAVE, B_STORY, "The Fight",
                "Kill the Krave Monster - Barbara and Cayden fight at your side. Hit him between "
                        + "blinks. A CEREAL BOWL first gives big combat buffs. His fall begins ACT II.",
                SUMMON_KRAVE);
        event(KRAVE_DANCE, B_STORY, "I KRAVE THE KRAVE",
                "ACT II. RIGHT-CLICK the BACKWARDS RED HAT to do the Krave dance-walk. "
                        + "\"I KRAVE THE KRAVE!\" If his Mom hears it... oh no.",
                SLAY_KRAVE);
        event(SMASH_MOUSE, B_STORY, "Off-Brand",
                "The download had a VIRUS. RIGHT-CLICK the COMPUTER MOUSE to hurl it out the front "
                        + "door and SMASH it.",
                KRAVE_DANCE);
        event(THE_PLUG_DEAL, B_STORY, "The Deal",
                "Find THE PLUG (black ski mask) and RIGHT-CLICK him holding MOM'S $500. "
                        + "See exactly what your $500 buys.",
                SMASH_MOUSE, PLUG_KIT);
        event(THE_SEWER, B_STORY, "The Sewer",
                "You got scammed. Go UNDERGROUND (below y=45) and SMOKE A ROLLED JOINT... and realise "
                        + "it too is just grass. Someone is about to have a very bad day.",
                THE_PLUG_DEAL);
        event(REVENGE, B_STORY, "Got That Mothafucker",
                "The Plug is HERE with the sniper that got Cayden. KILL HIM. Watch for the tracer - "
                        + "he hits hard from very far away.",
                THE_SEWER, SLAY_KRAVE);

        // ---- GRASS branch ---------------------------------------------------
        collect(GRASS_HARVEST, B_GRASS, "Touch Grass",
                "Get a HANDFUL OF GRASS and some GRASS SEEDS. It all starts in the dirt.",
                pre(START),
                ModItems.HANDFUL_OF_GRASS, ModItems.GRASS_SEEDS);
        collect(GRASS_PREP, B_GRASS, "Mise en Place",
                "Craft a GRASS KNIFE and dice the grass down to DICED GRASS.",
                pre(GRASS_HARVEST),
                ModItems.DICED_GRASS, ModItems.GRASS_KNIFE);
        collect(GRASS_DRY, B_GRASS, "Cure It",
                "Dry it out: BURNT GRASS, plus the BLOWTORCH and LIGHTER to work it.",
                pre(GRASS_PREP),
                ModItems.BURNT_GRASS, ModItems.BLOWTORCH, ModItems.LIGHTER);
        collect(GRASS_ROLL, B_GRASS, "Roll Up",
                "ROLLING PAPER, a ROLLED JOINT, and the paraphernalia: ASHTRAY and BONG.",
                pre(GRASS_DRY),
                ModItems.ROLLING_PAPER, ModItems.ROLLED_JOINT, ModItems.ASHTRAY, ModItems.BONG);
        collect(GRASS_EDIBLES, B_GRASS, "Bake Sale",
                "The refined stuff: a GRASS BROWNIE and the legendary GOLDEN JOINT.",
                pre(GRASS_ROLL),
                ModItems.GRASS_BROWNIE, ModItems.GOLDEN_JOINT);

        // ---- KRAVE branch ---------------------------------------------------
        collect(COCOA_CHAIN, B_KRAVE, "No Jungle Required",
                "The cocoa substitute chain: a ROASTED HUSK and the COCOA SUBSTITUTE it smelts into.",
                pre(START),
                ModItems.ROASTED_HUSK, ModItems.COCOA_SUBSTITUTE);
        collect(COOK_KRAVE, B_KRAVE, "The Cereal",
                "Craft KRAVE CEREAL (2 Wheat + 1 Sugar + 1 Cocoa Beans, shapeless, makes 2) and pour "
                        + "some KRAVE MILK. The green COOKBOOK lists every recipe.",
                pre(COCOA_CHAIN),
                ModItems.KRAVE_CEREAL, ModItems.KRAVE_MILK);
        collect(KRAVE_FEAST, B_KRAVE, "The Full Bowl",
                "Brew a CEREAL BOWL (bowl + Krave + Krave Milk) and box up a KRAVE BOX (2 Paper + "
                        + "Krave Cereal) ready to summon the monster.",
                pre(COOK_KRAVE),
                ModItems.CEREAL_BOWL, ModItems.KRAVE_BOX);
        collect(KRAVE_FORGE, B_KRAVE, "Krave Arsenal",
                "Forge the whole Krave tool set - PICKAXE, SWORD, AXE, SHOVEL and HOE. Better than "
                        + "diamond, each with a deliberately infuriating catch.",
                pre(KRAVE_FEAST),
                ModItems.KRAVE_PICKAXE, ModItems.KRAVE_SWORD, ModItems.KRAVE_AXE,
                ModItems.KRAVE_SHOVEL, ModItems.KRAVE_HOE);
        collect(KRAVE_TAPE, B_KRAVE, "Evidence",
                "Preserve the KRAVE VIDEO TAPE, and keep a KRAVE CLEANSE on hand in case the "
                        + "apocalypse ever needs an off-switch.",
                pre(COOK_KRAVE),
                ModItems.KRAVE_VIDEO_TAPE, ModItems.KRAVE_CLEANSE);

        // ---- CAYDEN branch --------------------------------------------------
        collect(CAYDEN_TOOLS, B_CAYDEN, "Keeping Cayden",
                "Rule #1: do not let Cayden die. Carry the CAYDEN COMPASS to find him and the "
                        + "HOUSING QUERY tool to check his room.",
                pre(START),
                ModItems.CAYDEN_COMPASS, ModItems.HOUSING_QUERY);
        collect(CAYDEN_FASTFOOD, B_CAYDEN, "Drive-Thru",
                "Cayden eats. CHICKEN NUGGETS, FRIES, a NUGGET BOX, a DONUT and a DONUT BOX.",
                pre(CAYDEN_TOOLS),
                ModItems.CHICKEN_NUGGETS, ModItems.FRIES, ModItems.NUGGET_BOX,
                ModItems.DONUT, ModItems.DONUT_BOX);
        collect(CAYDEN_DRINKS, B_CAYDEN, "The Soda Fountain",
                "Stock the fridge: MR PIBB, PIBB ZERO, a PIBB COCKTAIL, a GATORADE and a CHEPINA.",
                pre(CAYDEN_TOOLS),
                ModItems.MR_PIBB, ModItems.PIBB_ZERO, ModItems.PIBB_COCKTAIL,
                ModItems.GATORADE, ModItems.CHEPINA);
        collect(CAYDEN_HYGIENE, B_CAYDEN, "Basic Hygiene",
                "Clean the kid up: TOWEL, SOAP, TOOTHBRUSH - and the YELLOW TEETH that prove he "
                        + "never used any of it.",
                pre(CAYDEN_TOOLS),
                ModItems.TOWEL, ModItems.SOAP, ModItems.TOOTHBRUSH, ModItems.YELLOW_TEETH);
        collect(CAYDEN_PAPERS, B_CAYDEN, "The Paperwork",
                "The family file: the MANAGER'S TIE, the CHILD SUPPORT PAPERS, the ADOPTION PAPERS "
                        + "Mom keeps threatening, and MOM'S BELT (memorabilia only).",
                pre(CAYDEN_TOOLS),
                ModItems.MANAGERS_TIE, ModItems.CHILD_SUPPORT_PAPERS,
                ModItems.ADOPTION_PAPERS, ModItems.MOMS_BELT);

        // ---- FAME branch ----------------------------------------------------
        collect(FAME_GEAR, B_FAME, "Lights, Camera",
                "Every legend needs a set. Grab a MICROPHONE and a CAMERA.",
                pre(START),
                ModItems.MICROPHONE, ModItems.CAMERA);
        collect(FAME_MERCH, B_FAME, "Merch Table",
                "The FlyRich empire: the FLYRICH POSTER, the RECORD FLYRICH, and a BARBARA PLUSH.",
                pre(FAME_GEAR),
                ModItems.FLYRICH_POSTER, ModItems.RECORD_FLYRICH, ModItems.BARBARA_PLUSH);
        collect(FAME_REDFIT, B_FAME, "The Fit",
                "The uniform of the Krave: the backwards RED HAT and the RED SHIRT.",
                pre(START),
                ModItems.RED_HAT, ModItems.RED_SHIRT);
        collect(FAME_COMPUTER, B_FAME, "The Download",
                "The cursed PC parts: a COMPUTER MOUSE, the VIRUS it carried, and the MINECRAFT DISC "
                        + "it came on.",
                pre(START),
                ModItems.COMPUTER_MOUSE, ModItems.VIRUS, ModItems.MINECRAFT_DISC);

        // ---- STREETS branch -------------------------------------------------
        collect(PLUG_KIT, B_STREET, "Street Supplies",
                "Everything the deal needs: MOM'S $500, a SKI MASK, a SNIPER SCOPE, and a "
                        + "SEWER GRATE to find your way down.",
                pre(START),
                ModItems.FIVE_HUNDRED_DOLLARS, ModItems.SKI_MASK,
                ModItems.SNIPER_SCOPE, ModItems.SEWER_GRATE);
        collect(PLUG_SCAM, B_STREET, "What $500 Buys",
                "The Plug's 'product': OFF-BRAND PASTRIES, FAKE WEED and FAKE COCAINE. None of it "
                        + "is what he said it was.",
                pre(START),
                ModItems.OFF_BRAND_PASTRIES, ModItems.FAKE_WEED, ModItems.FAKE_COCAINE);
        collect(PASTRY_AISLE, B_STREET, "Breakfast of Champions",
                "The real deal for once: a box of TOASTER PASTRIES.",
                pre(START),
                ModItems.TOASTER_PASTRIES);

        // ---- the finale -----------------------------------------------------
        milestone(PEACE, B_END, "Peace at Last",
                "Every branch walked, every item collected, the Krave conquered and the scam avenged. "
                        + "It is finally over. (The death stages, of course, remain. Forever.)",
                true);
    }

    private Quests() { }

    // ---- lookups ------------------------------------------------------------

    @Nullable
    public static Quest byId(String id) {
        return BY_ID.get(id);
    }

    @Nullable
    public static ItemStack findBook(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.is(ModItems.QUEST_BOOK.get())) {
                return s;
            }
        }
        return null;
    }

    // ---- progress storage (on the book's NBT) -------------------------------

    private static ListTag doneTag(ItemStack book) {
        return book.getOrCreateTag().getList("KraveDone", Tag.TAG_STRING);
    }

    public static boolean isDone(@Nullable ItemStack book, String id) {
        if (book == null || book.isEmpty() || book.getTag() == null) {
            return false;
        }
        ListTag list = book.getTag().getList("KraveDone", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(id)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isDone(Player player, String id) {
        return isDone(findBook(player), id);
    }

    /** Whether the quest with this id exists and has all prerequisites met for this player. */
    public static boolean isUnlocked(Player player, String id) {
        Quest q = byId(id);
        return q != null && isUnlocked(findBook(player), q);
    }

    private static void markDone(ItemStack book, String id) {
        if (isDone(book, id)) {
            return;
        }
        CompoundTag tag = book.getOrCreateTag();
        ListTag list = tag.getList("KraveDone", Tag.TAG_STRING);
        list.add(StringTag.valueOf(id));
        tag.put("KraveDone", list);
    }

    public static int doneCount(@Nullable ItemStack book) {
        if (book == null || book.getTag() == null) {
            return 0;
        }
        return book.getTag().getList("KraveDone", Tag.TAG_STRING).size();
    }

    public static int total() {
        return ALL.size();
    }

    /** A quest is unlocked once all its prerequisites are done (finale: everything else). */
    public static boolean isUnlocked(@Nullable ItemStack book, Quest q) {
        if (q.finale) {
            for (Quest other : ALL) {
                if (!other.finale && !isDone(book, other.id)) {
                    return false;
                }
            }
            return true;
        }
        for (String p : q.prereqs) {
            if (!isDone(book, p)) {
                return false;
            }
        }
        return true;
    }

    // ---- driving the graph --------------------------------------------------

    /** Called once on first join: grant the hub and announce the branches. */
    public static void onFirstJoin(Player player) {
        ItemStack book = findBook(player);
        if (book == null) {
            return;
        }
        markDone(book, START);
        if (!player.level().isClientSide) {
            player.sendSystemMessage(Component.literal(head()
                    + ChatFormatting.YELLOW + "Quest opened: " + Quests.byId(START).title));
            announceNewlyAvailable(player, book);
        }
    }

    /**
     * Complete an EVENT quest by id (Barbara recruit, boss kill, etc). Only fires
     * if the quest exists, is unlocked, and is not already done. Returns true if it
     * actually advanced.
     */
    public static boolean complete(Player player, String id) {
        ItemStack book = findBook(player);
        Quest q = byId(id);
        if (book == null || q == null || isDone(book, id) || !isUnlocked(book, q)) {
            return false;
        }
        markDone(book, id);
        announceComplete(player, q);
        announceNewlyAvailable(player, book);
        settle(player, book);
        return true;
    }

    /**
     * Server-tick sweep (throttle in the caller): auto-completes every unlocked
     * collect quest whose items are all in the inventory, plus any milestone whose
     * prerequisites are now satisfied. Cascades until nothing new completes.
     */
    public static void tick(Player player) {
        ItemStack book = findBook(player);
        if (book == null || player.level().isClientSide) {
            return;
        }
        settle(player, book);
    }

    /** The cascade shared by complete() and tick(). */
    private static void settle(Player player, ItemStack book) {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Quest q : ALL) {
                if (isDone(book, q.id) || q.event || !isUnlocked(book, q)) {
                    continue;
                }
                if (q.collect.length == 0) {          // milestone (START, PEACE)
                    markDone(book, q.id);
                    announceComplete(player, q);
                    changed = true;
                } else if (hasAll(player, q)) {        // collect quest
                    markDone(book, q.id);
                    announceComplete(player, q);
                    changed = true;
                }
            }
            if (changed) {
                announceNewlyAvailable(player, book);
            }
        }
    }

    private static boolean hasAll(Player player, Quest q) {
        for (RegistryObject<Item> it : q.collect) {
            if (!player.getInventory().contains(new ItemStack(it.get()))) {
                return false;
            }
        }
        return true;
    }

    // ---- announcements ------------------------------------------------------

    private static String head() {
        return ChatFormatting.LIGHT_PURPLE + "[Krave Quest] " + ChatFormatting.WHITE;
    }

    private static void announceComplete(Player player, Quest q) {
        if (player.level().isClientSide) {
            return;
        }
        if (q.finale) {
            player.sendSystemMessage(Component.literal(head() + ChatFormatting.GOLD
                    + "" + ChatFormatting.BOLD + "QUEST COMPLETE: " + q.title + "!"));
            player.sendSystemMessage(Component.literal(ChatFormatting.GRAY + "  " + q.objective));
            return;
        }
        player.sendSystemMessage(Component.literal(head() + ChatFormatting.GREEN + "✔ "
                + q.title + ChatFormatting.DARK_GREEN + " done"));
    }

    /**
     * Announce quests that just became available (unlocked, not done) and have not
     * been announced before. The "seen" set lives on the book so we never repeat.
     */
    private static void announceNewlyAvailable(Player player, ItemStack book) {
        if (player.level().isClientSide) {
            return;
        }
        CompoundTag tag = book.getOrCreateTag();
        ListTag seen = tag.getList("KraveSeen", Tag.TAG_STRING);
        for (Quest q : ALL) {
            if (q.id.equals(START) || isDone(book, q.id) || !isUnlocked(book, q)) {
                continue;
            }
            boolean already = false;
            for (int i = 0; i < seen.size(); i++) {
                if (seen.getString(i).equals(q.id)) {
                    already = true;
                    break;
                }
            }
            if (already) {
                continue;
            }
            seen.add(StringTag.valueOf(q.id));
            player.sendSystemMessage(Component.literal(head() + ChatFormatting.AQUA
                    + "New quest – " + ChatFormatting.YELLOW + "[" + q.branch + "] " + q.title
                    + ChatFormatting.GRAY + " (see the Quest Book)"));
        }
        tag.put("KraveSeen", seen);
    }
}
