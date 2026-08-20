package com.barbarajones.v2.manual.book;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.content.ModItems;
import com.barbarajones.v2.abilities.AbilityItems;
import com.barbarajones.v2.economy.KraveEconomy;
import com.barbarajones.v2.internet.InternetContent;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * The whole Krave Manual, 2.0 edition: eleven chapters, written against the
 * actual source of every module listed in the task brief, not against what
 * the old manual guessed those modules might eventually do.
 *
 * <p><b>How this file is put together.</b> Each chapter is one private static
 * method returning a {@link ManualChapter} - a flat {@code List<PageElement>}
 * that {@code client.Paginator} slices into screen-sized pages at render
 * time (see {@link PageElement}'s own javadoc for why nothing here hand-picks
 * page breaks). {@link #CHAPTERS} is the fixed reading order; nothing else in
 * the client should build a chapter list any other way.
 *
 * <p><b>Where the facts came from.</b> Every mechanical claim in this file was
 * checked against the current source, not carried over from memory of the 1.x
 * manual: {@code CaydenCobb.java} and {@code AscensionLadder.java} for the
 * ascension ladder and feeding tiers, {@code v2.bonds} for bond levels and
 * villager trading, {@code v2.village.VillageBuffs}/{@code VillageTier} for
 * how a settlement actually grows, {@code v2.machines} for the production
 * chain, {@code boss.manager}/{@code boss.mom}/{@code v2.internet} for the
 * three bosses, {@code abilities} for the six player powers, and
 * {@code dimension}/{@code block.KraveDoorBlock} for the Krave Kosmos. See
 * {@code docs/modules/krave-manual.md} for the handful of things that are
 * documented here as designed-but-not-yet-reachable in play, and exactly why.
 *
 * <p><b>Illustrations.</b> Every {@link PageElement.Gallery} and recipe
 * element resolves real {@link ItemStack}s through {@link Icon#of}, which
 * degrades to a barrier icon rather than crashing if something is not
 * registered on a given build (see {@link Icon}'s own javadoc). A few
 * entities with no held item of their own (Cayden, Barbara, the bosses, the
 * Kraveling family) are illustrated with their spawn eggs - still a real
 * item render, just not a held one.
 */
public final class ManualBook {

    private ManualBook() { }

    // ---- palette (mirrors client.ui.KraveTheme's values without depending on
    // a client-only class from this data package) ----------------------------
    private static final int GOLD   = 0xFFE9B23C;
    private static final int RED    = 0xFFC81E24;
    private static final int PURPLE = 0xFF6B3FA0;
    private static final int GRASS  = 0xFF57B03A;
    private static final int DANGER = 0xFFE23C3C;
    private static final int MILK   = 0xFFF4EDDD;
    private static final int DIM    = 0xFFA2968B;
    private static final int BLUE   = 0xFF4FA8FF;

    // ---- tiny content-authoring helpers --------------------------------------

    private static PageElement.Heading head(String s) { return new PageElement.Heading(s); }
    private static PageElement.Sub sub(String s) { return new PageElement.Sub(s); }
    private static PageElement.Para p(String s) { return new PageElement.Para(s); }
    private static PageElement.Para p(String s, int c) { return new PageElement.Para(s, c); }
    private static PageElement.Bullets bul(String... s) { return new PageElement.Bullets(List.of(s)); }
    private static PageElement.Steps steps(String... s) { return new PageElement.Steps(List.of(s)); }
    private static PageElement.Divider div() { return new PageElement.Divider(); }
    private static PageElement.Spacer space(int px) { return new PageElement.Spacer(px); }
    private static PageElement.HardBreak brk() { return new PageElement.HardBreak(); }
    private static PageElement.Gallery gal(Icon... i) { return new PageElement.Gallery(List.of(i)); }
    private static PageElement.Callout note(String label, String text, int accent) {
        return new PageElement.Callout(label, text, accent);
    }
    private static PageElement.Table table(List<String> headers, List<Integer> widths, List<List<String>> rows) {
        return new PageElement.Table(headers, rows, widths);
    }
    private static List<String> row(String... s) { return List.of(s); }

    private static Icon icon(net.minecraft.world.item.Item item, String label) {
        return Icon.of(() -> new ItemStack(item), label);
    }
    private static Icon icon(net.minecraft.world.level.block.Block block, String label) {
        return Icon.of(() -> new ItemStack(block), label);
    }
    private static Icon reg(java.util.function.Supplier<net.minecraft.world.item.Item> item, String label) {
        return Icon.of(() -> new ItemStack(item.get()), label);
    }
    private static Icon regBlock(java.util.function.Supplier<net.minecraft.world.level.block.Block> b, String label) {
        return Icon.of(() -> new ItemStack(b.get()), label);
    }

    private static final Icon BLANK = Icon.of(() -> ItemStack.EMPTY, "");

    private static PageElement.CraftGrid shapeless3x3(String note, Icon output, int count, Icon... ingredients) {
        List<Icon> cells = new ArrayList<>();
        for (Icon in : ingredients) {
            cells.add(in);
        }
        while (cells.size() < 9) {
            cells.add(BLANK);
        }
        return new PageElement.CraftGrid(cells, true, output, count, note);
    }

    /** A 3x3 shape read top-to-bottom as three 3-character rows, {@code '.'} for empty. */
    private static PageElement.CraftGrid shaped3x3(String pattern, java.util.Map<Character, Icon> key,
                                                    Icon output, int count, String note) {
        List<Icon> cells = new ArrayList<>();
        String flat = pattern.replace("\n", "").replace(" ", "");
        for (int i = 0; i < 9; i++) {
            char c = i < flat.length() ? flat.charAt(i) : '.';
            cells.add(c == '.' ? BLANK : key.getOrDefault(c, BLANK));
        }
        return new PageElement.CraftGrid(cells, false, output, count, note);
    }

    private static PageElement.FlowRecipe flow(String verb, Icon output, int count, String note, Icon... inputs) {
        return new PageElement.FlowRecipe(verb, List.of(inputs), output, count, note);
    }

    // ==========================================================================
    // THE BOOK ITSELF
    // ==========================================================================

    public static final List<ManualChapter> CHAPTERS = List.of(
            chapterRule1(),
            chapterKravelings(),
            chapterKrave(),
            chapterVillage(),
            chapterVillagers(),
            chapterAutomation(),
            chapterCayden(),
            chapterBarbara(),
            chapterBosses(),
            chapterAbilities(),
            chapterDimension(),
            chapterRecipeIndex()
    );

    /**
     * The recipe index. Its body is EMPTY here on purpose: the contents are
     * generated from the live recipe manager when the chapter is opened, because
     * this class is built at class-init, long before any recipe exists to read.
     * ManualScreen swaps in the generated pages by chapter id.
     */
    private static ManualChapter chapterRecipeIndex() {
        return new ManualChapter("recipe_index", 12, "Every Recipe",
                "Every craftable thing in the mod, as a grid.",
                icon(net.minecraft.world.item.Items.CRAFTING_TABLE, "Recipes"), GOLD, List.of());
    }

    public static ManualChapter byId(String id) {
        for (ManualChapter c : CHAPTERS) {
            if (c.id().equals(id)) {
                return c;
            }
        }
        return CHAPTERS.get(0);
    }

    // ==========================================================================
    // 1. RULE #1
    // ==========================================================================

    private static ManualChapter chapterRule1() {
        List<PageElement> e = new ArrayList<>();

        e.add(head("RULE #1: DO NOT LET HIM DIE"));
        e.add(note("THE ONLY RULE THAT MATTERS", "Everything else in this book is optional. This is not.", DANGER));
        e.add(p("You will lose Krave. You will lose Barbara's grass. You will lose whole "
                + "afternoons to a village that refuses to grow. None of that ends the run. "
                + "A tamed Cayden Cobb (or a pet Barbara Jones) dying does - permanently, "
                + "loudly, and on purpose."));
        e.add(gal(icon(ModItems.EGG_CAYDEN.get(), "Cayden"), icon(ModItems.EGG_BARBARA.get(), "Barbara")));

        e.add(sub("WHAT HAPPENS IF HE DIES"));
        e.add(p("The KRAVE APOCALYPSE fires. You cannot stop it, cancel it, or outrun it - it "
                + "is a ten-stage cinematic escalation that owns the sky above wherever you are "
                + "standing. Barbara blows smoke rings across it. Mr. Pibb rains. Knives fall "
                + "with the cereal boxes. The Blowtorch sets the field alight. The gates of "
                + "Hell open overhead. The Manager arrives with a meteor shower behind him. A "
                + "cleaver the size of a house drops out of nowhere. Every actor in the cast "
                + "shows up at once. And at stage ten, the Krave Monster himself climbs bodily "
                + "out of the crater."));
        e.add(note("SMALL MERCY", "You take no damage from any of it. That is the only kindness in the whole sequence.", GOLD));
        e.add(p("Death eleven and beyond does not stop. There is no in-game cure for the "
                + "apocalypse loop once it starts repeating - the only fix is to never trigger "
                + "it in the first place. Read the rest of this page like your world depends "
                + "on it, because it does."));

        e.add(brk());
        e.add(head("HOW TO KEEP HIM ALIVE"));
        e.add(steps(
                "HOUSE HIM. A housed Cayden stays leashed within 22 blocks of his own bed "
                        + "instead of following you into caves, lava, and open sky. This is the "
                        + "single biggest thing you can do - see the housing rules later in this "
                        + "chapter.",
                "FEED HIM. Krave raises his fed-scaled attack damage, and a stronger Cayden "
                        + "wins fights he would otherwise lose. See the Cayden chapter for exactly "
                        + "how the feeding math works.",
                "TEACH HIM A FORM. As of 2.0 he has a whole Super Saiyan ascension ladder. An "
                        + "unascended Cayden dies to things an ascended one shrugs off - his "
                        + "Ascension Ledger progress (bought with the Cayden Compass) is not "
                        + "cosmetic, it is armour.",
                "WATCH THE FAT. Every 5 boxes eaten makes him slightly slower, up to a hard "
                        + "cap. A fat, unascended Cayden cannot run away from anything.",
                "CARRY THE COMPASS. Know where he is before you hear the siren, not after. "
                        + "The Cayden Compass gives bearing, distance, exact coordinates, Krave "
                        + "count, and housing status.",
                "LIGHT HIS ROOM. Hostile mobs spawn in the dark ones, and they spawn right "
                        + "next to him."
        ));

        e.add(sub("WHAT ACTUALLY KILLS HIM"));
        e.add(table(
                List.of("THREAT", "WHY"),
                List.of(70, 210),
                List.of(
                        row("Fall damage", "He follows you off ledges without checking the drop."),
                        row("Hostile mobs", "He attacks every hostile on sight within 16 blocks, and "
                                + "every hostile within 12 blocks targets him right back. He does not "
                                + "know when to disengage."),
                        row("The Internet Manager", "The 503 phase throws everything at once, on short "
                                + "cooldowns, to everyone standing near the fight."),
                        row("Your own weapon", "Watch your swing near him in a crowd."),
                        row("Lava, cactus, drowning", "He is, underneath the ascension ladder, still a kid.")
                )
        ));
        e.add(p("After any death he respawns with 30 seconds of total invulnerability - green "
                + "happy-villager sparkles mean untouchable. Use that half minute to walk him "
                + "somewhere safe, not to relax.", DIM));

        e.add(brk());
        e.add(head("THE TEN DEATH STAGES"));
        e.add(p("Each stage is strictly worse than the last, and the cinematic escalates the "
                + "instant the previous stage's beat finishes - there is no pausing it."));
        e.add(table(
                List.of("#", "WHAT ARRIVES"),
                List.of(14, 270),
                List.of(
                        row("1", "The face fills your screen. The siren drags itself down an octave. A box falls."),
                        row("2", "Barbara blows the O's she invented across the whole sky."),
                        row("3", "Mr. Pibb rains. The Pourer appears."),
                        row("4", "Knives come down with the boxes."),
                        row("5", "The Blowtorch. She lights the entire field."),
                        row("6", "The gates of Hell open overhead."),
                        row("7", "The Manager arrives. Meteors follow him in."),
                        row("8", "The Cleaver falls out of the sky."),
                        row("9", "Every actor in the cast, at once. Full wrath."),
                        row("10", "The Krave Monster climbs out of the crater himself.")
                )
        ));
        e.add(note("STAGE 11 AND BEYOND", "It never stops. Endless mode. No in-game cure exists - "
                + "prevention is the entire strategy.", DANGER));

        e.add(brk());
        e.add(head("HOUSING HIM (TERRARIA RULES)"));
        e.add(p("Craft the HOUSING QUERY (a Stick, a pane of Glass, and an Iron Ingot) and "
                + "right-click it inside a candidate room. It lists every failing condition at "
                + "once, not just the first one it finds - so you fix the room in one pass "
                + "instead of six."));
        e.add(gal(icon(ModItems.HOUSING_QUERY.get(), "Housing Query")));
        e.add(table(
                List.of("REQUIREMENT", "THRESHOLD"),
                List.of(120, 160),
                List.of(
                        row("Sealed", "No gap to the sky or open air."),
                        row("Interior air", "At least 30 blocks of it."),
                        row("Headroom", "3 or more blocks, somewhere in the room."),
                        row("Light level", "8 or brighter."),
                        row("An entrance", "A door or a trapdoor."),
                        row("A bed", "With a solid floor under at least 70% of it.")
                )
        ));
        e.add(p("Then right-click Cayden EMPTY-HANDED while standing inside the qualifying "
                + "room. He re-checks it every ten seconds after that - break the door, steal "
                + "the light, or tear off the roof and he moves out and tells you exactly why."));
        e.add(note("TOO BIG COUNTS AS OUTDOORS", "Rooms over roughly 900 blocks of interior air stop "
                + "reading as a house and start reading as a field. Build him a room, not a warehouse.", GOLD));

        return new ManualChapter("rule1", 1, "Rule #1: Cayden Must Not Die",
                "The only rule in this book that is not optional.",
                icon(ModItems.EGG_CAYDEN.get(), "Cayden Cobb"), DANGER, e);
    }

    // ==========================================================================
    // 2. THE KRAVELINGS
    // ==========================================================================

    private static ManualChapter chapterKravelings() {
        List<PageElement> e = new ArrayList<>();

        e.add(head("THE KRAVELINGS"));
        e.add(p("Five mobs, one family: hostile cereal gone feral, plus one that is not "
                + "hostile at all. This is the mod's own hostile roster - not reskinned "
                + "zombies, each with one genuinely distinct signature move."));
        e.add(note("A NOTE ON THIS CHAPTER", "The Kraveling family was still mid-build elsewhere in the "
                + "codebase as this manual went to print - see the gap list in this module's own "
                + "docs page. What follows is accurate to the design as written; exact stats and "
                + "drop tables may still move before 2.0 ships.", GOLD));

        e.add(brk());
        e.add(sub("KRAVELING"));
        e.add(p("The baseline. An ordinary melee brawler with one twist to its bite: a "
                + "connecting hit sprays a burst of dry cereal crumbs and has a real chance to "
                + "leave crumbs underfoot as a short Slowness - as if you are now standing in "
                + "spilled cereal. Everything else about its kit is the overworld baseline; the "
                + "crumb is the whole reason it is not just a reskinned zombie."));

        e.add(sub("KRISPBONE"));
        e.add(p("A skirmisher, not a melee brawler - it has no melee-attack goal at all. It "
                + "kites backward when you close the distance and advances when you're too far "
                + "to hit, holding an ideal range, and flicks hardened Krave Shards at you in "
                + "short bursts from that range. Fight it like a skeleton with legs, not like a "
                + "zombie."));
        e.add(gal(icon(Items.ROTTEN_FLESH, "Krispbone (placeholder render)")));

        e.add(sub("LOOMWEAVER"));
        e.add(p("Never fights for movement control - it lays traps instead. Every few seconds, "
                + "if you're in range and it has line of sight, it drops a patch of sticky Milk "
                + "Webbing near your feet rather than closing to melee. The webbing is placed by "
                + "Loomweaver only; it is not a block you can mine and keep."));

        e.add(sub("SOGGY"));
        e.add(p("Doesn't throw a normal punch - it belly-flops. A connecting hit shoves you "
                + "hard, well past ordinary zombie knockback, and being waterlogged and "
                + "top-heavy, Soggy stumbles for a moment afterward with a short "
                + "self-inflicted Slowness. Slow to close in, but a landed hit costs you real "
                + "positioning."));

        e.add(sub("THE MASCOT"));
        e.add(note("NOT HOSTILE", "The Mascot is registered as a plain creature, not a monster - the odd one out "
                + "in the family on purpose.", GRASS));
        e.add(p("Every other member of the family checks for nearby Kravelings and buffs "
                + "itself off them (any hostile Kraveling implements the same shared marker "
                + "interface), and The Mascot is the one that hands that buff out. Kill it last "
                + "if you're clearing a group - or leave it, since it never swings at you "
                + "itself."));

        e.add(brk());
        e.add(sub("WHAT THEY DROP"));
        e.add(p("Two items are confirmed and registered: Krave Shard, the ammunition Krispbone "
                + "throws at you (and what you can pick back up off the ground), and the "
                + "Cereal Mascot Head - an Epic-rarity trophy drop, not equipment. Full drop "
                + "tables for the rest of the family were not finalised in the source at the "
                + "time of writing; treat anything else as a bonus.", DIM));
        e.add(gal(icon(Items.PRISMARINE_SHARD, "Krave Shard (placeholder render)"),
                icon(Items.PLAYER_HEAD, "Cereal Mascot Head (placeholder render)")));

        return new ManualChapter("kravelings", 2, "The Kravelings",
                "Five mobs, one hostile family, and the one that isn't.",
                icon(Items.ZOMBIE_SPAWN_EGG, "Kraveling"), 0xFF8A5A2A, e);
    }

    // ==========================================================================
    // 3. KRAVE: CRAFTING, TIERS, AND WHAT EACH IS FOR
    // ==========================================================================

    private static ManualChapter chapterKrave() {
        List<PageElement> e = new ArrayList<>();

        e.add(head("KRAVE: THE TIERS"));
        e.add(p("Every recipe below is real, checked against the actual JSON on disk - not a "
                + "guess at what a chocolate cereal mod probably crafts. \"Krave\" the game "
                + "object is Krave Cereal; everything else in this chapter is a tier above it, "
                + "an ingredient toward it, or a tool made out of it."));

        e.add(sub("TIER 1 - KRAVE CEREAL"));
        e.add(p("The base tier. Feeds Cayden +1 Ki and raises his fed-scaled attack every box; "
                + "every 5th box announces his new attack number. Also a real food in your own "
                + "hand."));
        e.add(shapeless3x3("SHAPELESS", icon(ModItems.KRAVE_CEREAL.get(), "Krave Cereal"), 2,
                icon(Items.WHEAT, "Wheat"), icon(Items.WHEAT, "Wheat"),
                icon(Items.SUGAR, "Sugar"), icon(Items.COCOA_BEANS, "Cocoa Beans")));

        e.add(sub("TIER 2 - RICH KRAVE"));
        e.add(p("The Krave Crafting Economy module's mid-tier: strictly better food (Speed + "
                + "Regeneration, no Hunger downside) and worth 5 Ki per box to Cayden - between "
                + "a plain box and the golden one. Two paths to it:"));
        e.add(shapeless3x3("SHAPELESS", icon(KraveEconomy.RICH_KRAVE.get(), "Rich Krave"), 2,
                icon(ModItems.KRAVE_CEREAL.get(), "Krave Cereal"),
                icon(ModItems.KRAVE_DUST.get(), "Krave Dust"),
                icon(ModItems.KRAVE_MILK.get(), "Krave Milk")));
        e.add(shapeless3x3("SHAPELESS - alternate path, once you have Syrup surplus",
                icon(KraveEconomy.RICH_KRAVE.get(), "Rich Krave"), 2,
                icon(ModItems.KRAVE_CEREAL.get(), "Krave Cereal"),
                icon(KraveEconomy.KRAVE_SYRUP.get(), "Krave Syrup")));

        e.add(sub("TIER 3 - GOLDEN KRAVE"));
        e.add(p("The top tier. A full heal and 30 Ki in one box - the only realistic way to "
                + "buy Cayden's first ascension rung without weeks of hand-feeding plain "
                + "cereal. Expensive in gold on purpose."));
        e.add(shaped3x3("GGG\nGKG\nGGG", java.util.Map.of(
                        'G', icon(Items.GOLD_INGOT, "Gold Ingot"),
                        'K', icon(ModItems.KRAVE_CEREAL.get(), "Krave Cereal")),
                icon(ModItems.GOLDEN_KRAVE.get(), "Golden Krave"), 1, "SHAPED - a ring of gold around one box"));

        e.add(brk());
        e.add(sub("THE INTERMEDIATES: DUST AND SYRUP"));
        e.add(p("Krave Dust is ground cocoa - cheap, stackable, the thing surplus cocoa turns "
                + "into instead of sitting dead in a chest. Grind it by hand at a Krave Mortar, "
                + "no fuel and no GUI required:"));
        e.add(new PageElement.FlowRecipe("RIGHT-CLICK the mortar holding", List.of(
                icon(Items.COCOA_BEANS, "Cocoa Beans (x2 per grind)")),
                icon(ModItems.KRAVE_DUST.get(), "Krave Dust"), 1,
                "Deliberately a worse-than-1:1 ratio, so an automated grinder is always worth building later."));
        e.add(shaped3x3(" S \nS S\nSSS", java.util.Map.of('S', icon(Items.COBBLESTONE, "Cobblestone")),
                icon(KraveEconomy.KRAVE_MORTAR_ITEM.get(), "Krave Mortar"), 1,
                "SHAPED - mortar-and-pestle pattern"));
        e.add(p("Krave Syrup is the refined, concentrated material - one craft per unit, "
                + "deliberately not a bulk output, because it is what the automation chapter's "
                + "machines are meant to run on."));
        e.add(shapeless3x3("SHAPELESS", icon(KraveEconomy.KRAVE_SYRUP.get(), "Krave Syrup"), 1,
                icon(Items.GLASS_BOTTLE, "Glass Bottle"),
                icon(ModItems.KRAVE_DUST.get(), "Krave Dust"),
                icon(ModItems.KRAVE_DUST.get(), "Krave Dust"),
                icon(Items.SUGAR, "Sugar")));

        e.add(brk());
        e.add(sub("COCOA WITHOUT THE JUNGLE"));
        e.add(p("Krave Cereal needs cocoa, and jungles are far. Three steps and two furnace "
                + "burns gets you cocoa beans at home instead:"));
        e.add(new PageElement.FlowRecipe("SMELT",
                List.of(icon(Items.BROWN_MUSHROOM, "Brown Mushroom")),
                icon(ModItems.ROASTED_HUSK.get(), "Roasted Husk"), 1,
                "A Brown Mushroom smelts into a Roasted Husk."));
        e.add(shapeless3x3("SHAPELESS", icon(ModItems.COCOA_SUBSTITUTE.get(), "Cocoa Substitute"), 1,
                icon(ModItems.ROASTED_HUSK.get(), "Roasted Husk"), icon(ModItems.ROASTED_HUSK.get(), "Roasted Husk"),
                icon(Items.SUGAR, "Sugar"), icon(Items.COAL, "Coal")));
        e.add(new PageElement.FlowRecipe("SMELT",
                List.of(icon(ModItems.COCOA_SUBSTITUTE.get(), "Cocoa Substitute")),
                icon(Items.COCOA_BEANS, "Cocoa Beans"), 1,
                "The Cocoa Substitute itself smelts into real Cocoa Beans. A process on purpose, "
                        + "but a process you can run entirely at home."));

        e.add(brk());
        e.add(sub("EVERYDAY KRAVE ITEMS"));
        e.add(gal(icon(ModItems.KRAVE_MILK.get(), "Krave Milk"),
                icon(ModItems.CEREAL_BOWL.get(), "Cereal Bowl"),
                icon(ModItems.KRAVE_BOX.get(), "Krave Box"),
                icon(ModItems.CAYDEN_COMPASS.get(), "Cayden Compass")));
        e.add(bul(
                "Krave Milk - Milk Bucket + Krave Cereal. The Rich Krave ingredient, and a "
                        + "minor food on its own.",
                "Cereal Bowl - Bowl + Krave Cereal + Krave Milk. The full breakfast: real "
                        + "combat buffs when you drink it before a fight.",
                "Krave Box - 2 Paper + Krave Cereal. Right-clicking one on open flat ground "
                        + "summons the Krave Monster - see the Bosses chapter before you do this.",
                "Cayden Compass - Compass + Krave Cereal + Lapis Lazuli. Right-click for his "
                        + "bearing, distance, exact coordinates, Krave count, and housing status, "
                        + "searching out to 512 blocks. Also opens his Ascension screen."
        ));

        e.add(brk());
        e.add(sub("KRAVE TOOLS (CURSED, ON PURPOSE)"));
        e.add(p("All five tools: Krave Cereal in the ordinary tool shape, 180 uses, iron "
                + "mining level, +4 damage, and a speed of 14 - faster than diamond. That is "
                + "the deal. Here is the price on each one."));
        e.add(table(
                List.of("TOOL", "THE CATCH"),
                List.of(70, 210),
                List.of(
                        row("Pickaxe", "Every 10-20 blocks Cayden takes a bite out of it - Slowness and Hunger."),
                        row("Sword", "Cayden teleports directly in front of your eyes to watch. Blocks everything."),
                        row("Axe", "Drops cereal crumbs, and he shoves you aside to get at them."),
                        row("Shovel", "Pure sugar. You bounce, and you do not choose where."),
                        row("Hoe", "The crunch is LOUD. It pulls every nearby hostile toward you.")
                )
        ));

        return new ManualChapter("krave", 3, "Krave: Crafting & Tiers",
                "Cereal, Rich Krave, Golden Krave - and what each one buys you.",
                icon(ModItems.KRAVE_CEREAL.get(), "Krave Cereal"), RED, e);
    }

    // ==========================================================================
    // 4. BUILDING THE VILLAGE
    // ==========================================================================

    private static ManualChapter chapterVillage() {
        List<PageElement> e = new ArrayList<>();

        e.add(head("BUILDING THE VILLAGE"));
        e.add(note("HOW THIS ACTUALLY WORKS", "There is no blueprint you place and watch complete itself. "
                + "A Krave village grows because you build a real settlement - beds, doors, "
                + "workstations, lights, walls - the same way a vanilla village grows a "
                + "population, just with Krave's own furniture and industry weighted in.", GOLD));
        e.add(p("The mod tracks every settlement's BUILDING, POPULATION, and DEFENCE scores "
                + "off the blocks placed inside it. Six tiers, each strictly ALL of its "
                + "requirements, not any one of them - forty buildings and one resident is "
                + "still a camp, because the buildings are standing empty."));

        e.add(table(
                List.of("TIER", "BUILDINGS", "POP", "DEFENCE", "POP CAP"),
                List.of(90, 60, 40, 60, 60),
                List.of(
                        row("Wilderness", "0", "0", "0", "2"),
                        row("Camp", "3", "1", "0", "4"),
                        row("Hamlet", "8", "3", "4", "7"),
                        row("Village *", "16", "6", "12", "11"),
                        row("Town", "28", "10", "26", "16"),
                        row("Krave Capital", "44", "15", "45", "22")
                )
        ));
        e.add(note("* THE GATE TIER", "The Krave dimension's portal only opens once your settlement reaches "
                + "VILLAGE. This is the whole reason to build one at all, beyond just wanting a "
                + "nice home for Cayden.", PURPLE));
        e.add(p("A tier is recomputed continuously, not stored - tear your own town apart and "
                + "it can genuinely fall back down a rung. \"Currently a real settlement\" is "
                + "what gates the portal, not \"once was.\"", DIM));

        e.add(brk());
        e.add(sub("WHAT ACTUALLY COUNTS"));
        e.add(p("Only tracked blocks contribute - this is deliberate, so the sweep stays cheap "
                + "and a village grows because you built a place worth living in, not because "
                + "you placed one magic totem."));
        e.add(table(
                List.of("BLOCK", "WHAT IT GIVES"),
                List.of(140, 200),
                List.of(
                        row("Any bed", "The single strongest attraction signal - a bed is a resident."),
                        row("Any door (incl. the Krave Door)", "Turns four walls into a real building."),
                        row("Workstations", "Crafting table, furnace, smithing table, loom, brewing stand, and more."),
                        row("Bell", "Attraction, happiness, and real defence."),
                        row("Krafting Bench", "The mod's own bench - the best workstation in the game, obviously."),
                        row("Krave Block", "The centre of Krave industry. Worth more production than anything else in the base table."),
                        row("Krave Pod / Log / Leaves / Grass, Chocolate Log", "Steady production, the visible sign of a working Krave farm."),
                        row("Torches, lanterns, campfires, glowstone", "Defence and happiness - a dark village is a raid waiting to happen."),
                        row("Walls, iron bars, the Chocolate/Krave Fence line", "Fortification."),
                        row("Recliner, Television, Boombox, Stash Box", "Pure happiness and attraction - comfort is what makes people stay.")
                )
        ));
        e.add(gal(regBlock(ModBlocks.KRAVE_BLOCK, "Krave Block"),
                regBlock(ModBlocks.KRAFTING_BENCH, "Krafting Bench"),
                regBlock(ModBlocks.KRAVE_DOOR, "Krave Door"),
                regBlock(ModBlocks.STASH_BOX, "Stash Box"),
                regBlock(ModBlocks.RECLINER, "Recliner"),
                regBlock(ModBlocks.TELEVISION, "Television")));
        e.add(p("One block on that list is a joke with a real mechanical cost: the Stash Box is "
                + "worth as much happiness as almost anything in the table, and it quietly costs "
                + "the settlement a point of defence. Exactly as good for morale as it sounds, "
                + "exactly as bad for the neighbourhood.", DIM));

        e.add(brk());
        e.add(sub("SCHEMATICS - THE PART STILL BEING BUILT"));
        e.add(note("HONEST GAP", "A framework for unlockable building blueprints exists in the code "
                + "(a structure registry, and a quest-reward type that specifically hands out "
                + "\"schematic\" unlocks) - but as of this build, no actual building schematics "
                + "are registered on it yet, and no quest hands one out. There is no list of \"the "
                + "ten buildings\" to print here honestly, because it has not been written yet.", DANGER));
        e.add(p("Until that lands, build your village the organic way described above: beds, "
                + "doors, workstations, lights, walls, and Krave industry. It works today, it "
                + "gates the same portal, and nothing about it will change out from under you "
                + "when the schematic system does arrive."));

        return new ManualChapter("village", 4, "Building the Village",
                "No blueprints yet - just beds, doors, lights, and Krave.",
                regBlock(ModBlocks.KRAVE_DOOR, "Krave Door"), 0xFF8A5A2A, e);
    }

    // ==========================================================================
    // 5. VILLAGERS AND TRADING
    // ==========================================================================

    private static ManualChapter chapterVillagers() {
        List<PageElement> e = new ArrayList<>();

        e.add(head("VILLAGERS AND TRADING"));
        e.add(p("Krave makes villagers smarter. Feed an ordinary vanilla villager Krave Cereal "
                + "or Golden Krave and they climb a five-rung bond, entirely on top of vanilla "
                + "trading - no new profession required to get the benefit."));
        e.add(gal(icon(ModItems.KRAVE_CEREAL.get(), "Krave Cereal"), icon(ModItems.GOLDEN_KRAVE.get(), "Golden Krave")));

        e.add(sub("HOW THE BOND WORKS"));
        e.add(p("Every feed banks toward the next rung - an ordinary box counts once, a Golden "
                + "Krave counts as three. Five rungs, three feeds per rung, so a fully bonded "
                + "villager costs roughly fifteen boxes' worth of feeding, fewer if you spend "
                + "golden ones."));
        e.add(table(
                List.of("EFFECT", "WHAT IT DOES"),
                List.of(110, 230),
                List.of(
                        row("Instant restock", "Every offer's uses are reset the moment you feed them - a real, "
                                + "immediate restock, not a cosmetic one."),
                        row("Better prices", "A discount is knocked off every offer per rung, the same mechanism "
                                + "Hero of the Village uses, aimed at that one villager specifically."),
                        row("Faster auto-restock", "Base cadence is three minutes between free restocks; a fully "
                                + "bonded villager restocks itself every thirty seconds.")
                )
        ));
        e.add(note("PRACTICAL ADVICE", "Feed your best trades first. A bonded farmer or librarian pays that "
                + "investment back the fastest, simply by having offers worth restocking often.", GOLD));

        e.add(brk());
        e.add(sub("THE SAME SYSTEM, THREE WAYS"));
        e.add(p("Feeding a villager Krave, feeding Cayden Krave, and feeding Barbara grass all "
                + "run through the same underlying bond system under the hood - which is why "
                + "all three show the same kind of feedback: an action-bar line with a star "
                + "meter (★★★☆) the moment you feed them, and their name tag lighting up "
                + "with the same stars once a rung is reached. If you understand one, you "
                + "understand all three."));
        e.add(bul(
                "Cayden's bond runs off his lifetime Krave-fed counter - the same number his "
                        + "ascension feeding uses, so bonding him is never wasted progress.",
                "Barbara has no lifetime counter of her own (her stash decays), so her bond "
                        + "tracks a separate lifetime tally of every gift of grass she has ever been "
                        + "handed.",
                "Villagers track lifetime feeds the same way, per villager, so moving a bonded "
                        + "villager to a new trade hall keeps their rung."
        ));

        e.add(brk());
        e.add(sub("SETTLING IN A REAL HOUSE"));
        e.add(p("Cayden and Barbara can also settle into an actual village house - a real bed, "
                + "in a real sealed, lit, doored room, found by the same housing rules Cayden's "
                + "own manual room-claim already trusts. When the owner is far away, a settled "
                + "companion stays near their assigned bed instead of wandering; get within "
                + "about ten blocks and normal follow/combat behaviour takes back over."));
        e.add(note("KNOWN LIMITATION", "An auto-assigned house is tracked separately from Cayden's own manual "
                + "housing claim (see the Rule #1 chapter) - his own isHoused() indicator only "
                + "reflects a room you claimed by hand with an empty-handed right-click. If you "
                + "want the Rule #1 housing feedback to show green, claim his home yourself.", DIM));

        return new ManualChapter("villagers", 5, "Villagers & Trading",
                "Krave makes them smarter - literally. A five-rung bond, on top of vanilla trading.",
                icon(Items.EMERALD, "Trading"), GRASS, e);
    }

    // ==========================================================================
    // 6. AUTOMATION: THE PRODUCTION CHAIN
    // ==========================================================================

    private static ManualChapter chapterAutomation() {
        List<PageElement> e = new ArrayList<>();

        e.add(head("AUTOMATION: THE PRODUCTION CHAIN"));
        e.add(note("A NOTE ON THIS CHAPTER", "The machines module was still being wired to its own item "
                + "registry elsewhere in the codebase as this manual went to print, so exact block "
                + "textures and menu screens may not be reachable in every build yet. The chain "
                + "design below is accurate to the finished source; see this module's own docs "
                + "page for the exact compile status.", GOLD));
        e.add(p("Seven machine kinds, one shared shape: up to three inputs, a fuel slot, one "
                + "output, a progress bar - laid out exactly like a vanilla furnace, on purpose, "
                + "so if you have ever used a furnace you already know how to read every Krave "
                + "machine."));

        e.add(sub("THE CHAIN, START TO FINISH"));
        e.add(steps(
                "PLANTATION - farms Krave Pods and cocoa in a radius around itself, no fuel "
                        + "needed. Feed it cocoa beans to keep it replanting.",
                "GRINDER - the automated cousin of the hand-cranked Krave Mortar. Cocoa in, "
                        + "Krave Dust out.",
                "MIXER - the only three-input machine. Krave Dust, milk, and sugar in; Krave "
                        + "Batter out.",
                "EXTRUDER - Krave Batter in, raw Krave pieces out.",
                "TOASTER - raw pieces in, finished Krave out.",
                "BOXER - Krave plus a carton in, a Case of Krave out. Two inputs.",
                "DEPOT - no fuel slot, no output slot at all. What goes in leaves the world "
                        + "entirely and turns straight into village production."
        ));
        e.add(note("THE DEPOT IS THE WHOLE POINT", "Everything upstream of it exists to keep the Depot fed. "
                + "That is what turns a personal Krave production line into your village's own "
                + "economy - see the Building the Village chapter for what that production "
                + "actually buys your settlement.", PURPLE));

        e.add(brk());
        e.add(sub("POWER: KRAVE SYRUP, NOT AN ENERGY GRID"));
        e.add(p("Deliberately not an energy API - no network, no cables, no invented unit of "
                + "power. A machine has a fuel slot and spends syrup units per tick of "
                + "progress, the exact same mental model as a vanilla furnace."));
        e.add(table(
                List.of("FUEL", "UNITS", "ROUGHLY"),
                List.of(140, 80, 130),
                List.of(
                        row("Krave Syrup", "1,600", "About 13 Grinder runs."),
                        row("Dense Krave Syrup", "8,000", "Five syrups' worth, one slot.")
                )
        ));
        e.add(note("VANILLA FUEL DOES NOT WORK HERE", "Coal in a Krave Mixer would be a worse joke than "
                + "syrup in one. The exclusivity is what makes refining syrup worth automating "
                + "instead of just burning coal forever.", GOLD));
        e.add(p("Every recipe a Grinder, Mixer, Extruder, Toaster, or Boxer runs is a plain "
                + "JSON recipe of that machine's own recipe type - there is no hard-coded "
                + "knowledge of cocoa or dust anywhere in the machine itself. A datapack could "
                + "add a grinding recipe for something else entirely without touching a line of "
                + "Java.", DIM));

        return new ManualChapter("automation", 6, "Automation",
                "Plantation to Depot: the whole production chain, one furnace-shaped machine at a time.",
                icon(Items.HOPPER, "Automation"), 0xFF9AA0AA, e);
    }

    // ==========================================================================
    // 7. CAYDEN: FEEDING, ASCENSION, BREEDING
    // ==========================================================================

    private static ManualChapter chapterCayden() {
        List<PageElement> e = new ArrayList<>();

        e.add(head("CAYDEN COBB"));
        e.add(p("Your Krave-addicted companion. He fights every hostile mob on sight - and "
                + "every hostile within range targets him right back - gets stronger and "
                + "fatter the more Krave you feed him, and as of 2.0, climbs an entire Super "
                + "Saiyan-style ascension ladder you unlock and pay for by hand."));
        e.add(gal(icon(ModItems.EGG_CAYDEN.get(), "Cayden Cobb")));

        e.add(sub("FEEDING TIERS"));
        e.add(table(
                List.of("FEED", "HEAL", "KI"),
                List.of(150, 60, 60),
                List.of(
                        row("Krave Cereal", "+4 HP", "+1"),
                        row("Rich Krave", "+7 HP", "+5"),
                        row("Golden Krave", "Full heal", "+30")
                )
        ));
        e.add(p("Right-click him holding any of the three. Every 5th plain box announces his "
                + "new attack number out loud; at 25 lifetime boxes he unlocks KRAVE RAGE - "
                + "Barbara picks him up bodily and throws him at your enemies. It does exactly "
                + "what you'd expect a thrown child to do."));
        e.add(note("FAT IS A REAL STAT", "Every 5 boxes eaten (of any kind) makes him slightly slower, "
                + "capped well short of useless. Feeding him is not a free lunch - watch it near "
                + "a fight you might need to run from.", DIM));

        e.add(brk());
        e.add(head("THE ASCENSION LADDER"));
        e.add(p("Seven rungs, base included. Buy a rung with the Cayden Compass (right-click "
                + "him with it to open his Ascension screen) once he has both the Ki and the "
                + "lifetime feeding the rung demands - and once the rung below it is already "
                + "his. Costs climb roughly geometrically on purpose: Ultra Instinct should "
                + "feel like the end of a long climb, not the next purchase."));
        e.add(table(
                List.of("FORM", "KI COST", "BOXES FED", "THE EDGE"),
                List.of(90, 55, 60, 175),
                List.of(
                        row("Super Saiyan", "24", "8", "Quadruple damage, burst flight, Krave lasers."),
                        row("Super Saiyan 2", "70", "20", "A standing shockwave every two seconds."),
                        row("Super Saiyan 3", "150", "36", "Fast enough to run down something that flies."),
                        row("Super Saiyan God", "280", "55", "38% of all incoming damage simply doesn't apply."),
                        row("Super Saiyan Blue", "460", "80", "Halves what lands, and dodges 1 hit in 3 outright."),
                        row("Ultra Instinct", "700", "110", "The body answers before he does - 7 hits in 10 never arrive.")
                )
        ));
        e.add(p("Ki comes from feeding him (see the table above) and from things dying in "
                + "front of him - a bigger kill pays more, scaled to whichever rung the fight "
                + "demanded. Nothing here is spent automatically; every rung is a deliberate "
                + "purchase you make with the Compass, and a locked form he reaches for in a "
                + "real fight simply fails, with a chat line telling you exactly which rung he "
                + "wanted."));
        e.add(note("WHERE HE ASCENDS FOR FREE", "Liquid chocolate and the Krave Kosmos both force Super "
                + "Saiyan the instant he touches/enters them - but only if he has unlocked at "
                + "least the first rung already. A completely un-ascended Cayden gets none of "
                + "this; the ladder gates everything.", PURPLE));

        e.add(brk());
        e.add(sub("BREEDING"));
        e.add(note("HONEST GAP - NOT WORKING YET", "There is a real, craftable Krave Family Size box (3x3 "
                + "Krave Cereal), a distinct BredCaydenCobb entity class built to be an exempt "
                + "copy of Cayden so \"exactly one original\" enforcement leaves it alone, and "
                + "save-data plumbing for a feeding cooldown on it. What is missing: nothing "
                + "actually spawns one yet, and it has no registered entity type at all in this "
                + "build. Right-clicking the Family Size box does nothing beyond what it says on "
                + "the tin - do not expect a second Cayden from it in this version.", DANGER));
        e.add(gal(icon(ModItems.KRAVE_FAMILY_BOX.get(), "Krave Family Size")));
        e.add(shaped3x3("KKK\nKKK\nKKK", java.util.Map.of('K', icon(ModItems.KRAVE_CEREAL.get(), "Krave Cereal")),
                icon(ModItems.KRAVE_FAMILY_BOX.get(), "Krave Family Size"), 1, "SHAPED"));

        e.add(brk());
        e.add(sub("BONDING (SEE ALSO: THE VILLAGERS CHAPTER)"));
        e.add(p("Separate from the ascension ladder entirely: every box you have ever fed him "
                + "also climbs a five-rung relationship track - Stranger, Regular, Ride-or-Die, "
                + "Bonded for Life, Krave Soulmate - that never goes down and hands out its own "
                + "passive buffs (resistance, regen, and at the top rung, fire immunity). Rung "
                + "two lands deliberately on the same 25-box mark that unlocks Krave Rage."));

        return new ManualChapter("cayden", 7, "Cayden: Feeding & Ascension",
                "Feed him, teach him, and understand exactly why breeding doesn't work yet.",
                icon(ModItems.EGG_CAYDEN.get(), "Cayden Cobb"), GOLD, e);
    }

    // ==========================================================================
    // 8. BARBARA JONES
    // ==========================================================================

    private static ManualChapter chapterBarbara() {
        List<PageElement> e = new ArrayList<>();

        e.add(head("BARBARA JONES"));
        e.add(p("She smokes grass. Actual grass, pulled out of the ground. Her stash drains "
                + "constantly, and everything she does in a fight is paid for out of that same "
                + "pool - the bag you keep filling is, literally, her ammunition."));
        e.add(gal(icon(ModItems.EGG_BARBARA.get(), "Barbara Jones")));

        e.add(sub("KEEPING HER CALM"));
        e.add(bul(
                "RECRUIT - right-click her with an empty hand.",
                "FEED - right-click her holding grass. Feeding is what refills the stash that "
                        + "everything below spends.",
                "PSYCHO - stash empty, or offended, and she runs, jumps, grows, and comes for "
                        + "you with low-damage endless shoving. Feed her to calm her back down.",
                "DO NOT TAUNT HER - holding grass near her without handing it over sets her "
                        + "off exactly as hard as an empty stash does. Give it to her or put it away.",
                "SHE HATES WATCHING YOU GET HIGH - smoking a Rolled Joint in front of her "
                        + "offends her for a full 20 seconds, and an offended Barbara goes psycho "
                        + "whether her own stash is full or not. Smoke out of her sight."
        ));
        e.add(note("AS A PET", "She fights alongside you, gets the same 30-second post-respawn immunity "
                + "Cayden does - and her death fires the exact same Krave Apocalypse his does. See "
                + "Rule #1. There are two of them to protect, not one.", DANGER));

        e.add(brk());
        e.add(head("HER MOVES"));
        e.add(p("Eight abilities, every one of them something she does with a lit blunt, every "
                + "one paid for out of the same grass stash that keeps her from going psycho - "
                + "fighting and staying calm draw on one pool, so keeping her fed is not "
                + "optional even in combat."));
        e.add(table(
                List.of("MOVE", "WHAT IT DOES"),
                List.of(90, 220),
                List.of(
                        row("The O's", "Blown smoke rings that sail through a line of enemies and stagger every one."),
                        row("Lit Cherry", "She flicks the cherry off the end. Cheap chip damage that sets things alight."),
                        row("Smoke Screen", "A standing cloud that blinds and bogs down whatever walks into it."),
                        row("Second-Hand", "Passive ambient haze, metered by the second rather than cast on demand."),
                        row("Ash Cloud", "She taps the cherry hard: burning ash rings out and shoves everything back."),
                        row("Blowback", "Retaliation. Whoever just hit her gets the whole lungful, point blank."),
                        row("Contact High", "She passes it around - the whole crew gets faster and tougher, and a little dizzy."),
                        row("Burnout", "The entire bag in one pull. Costs everything she has left.")
                )
        ));
        e.add(note("HIGH BARBARA IS A GENEROUS BARBARA", "While high, every move above costs 40% less "
                + "stash and comes back roughly 35% sooner - the entire trade for how badly she "
                + "aims in that state.", GOLD));
        e.add(p("Her whole role next to Cayden is deliberate: she does almost no direct damage "
                + "and an enormous amount of crowd control. Half her kit exists purely to drag "
                + "aggro off him - keep her fed and she is doing more for his survival than "
                + "your own sword is."));

        e.add(brk());
        e.add(sub("HER FUEL: THE GRASS PIPELINE"));
        e.add(p("What you're actually handing her, from the ground up:"));
        e.add(steps(
                "Handful of Grass - 3x any grass or fern block.",
                "Diced Grass (x2) - a Handful, cut with a Grass Knife (Iron + Stick).",
                "Burnt Grass - Diced Grass smelted (a furnace works fine, no Blowtorch required).",
                "Rolling Paper - a single sheet of Paper.",
                "Rolled Joint - Rolling Paper + 2x Burnt Grass. This is what she smokes for the O's.",
                "Golden Joint - a Rolled Joint plus a Gold Ingot, for when regular grass isn't enough of a statement."
        ));
        e.add(gal(icon(Items.FERN, "Handful of Grass (source)"), icon(Items.SUGAR, "→"),
                icon(Items.PAPER, "Rolling Paper")));
        e.add(p("Can't find her? Right-click THE PLUG holding Emeralds: two handfuls per "
                + "emerald, up to eight at a time - and he shorts you roughly one time in four. "
                + "See the Bosses chapter for the rest of what he's about.", DIM));

        return new ManualChapter("barbara", 8, "Barbara & Her Moves",
                "Grass in, smoke out - and eight ways to spend it.",
                icon(ModItems.EGG_BARBARA.get(), "Barbara Jones"), GRASS, e);
    }

    // ==========================================================================
    // 9. THE BOSSES
    // ==========================================================================

    private static ManualChapter chapterBosses() {
        List<PageElement> e = new ArrayList<>();

        e.add(head("THE BOSSES"));
        e.add(p("Four real fights, three of them boss-bar-and-phases encounters built entirely "
                + "for 2.0. Two of the three new ones have no summon item wired up yet - see the "
                + "honest note under each."));

        e.add(brk());
        e.add(sub("THE KRAVE MONSTER"));
        e.add(gal(icon(ModItems.EGG_KRAVE_MONSTER.get(), "Krave Monster")));
        e.add(p("Summoned by right-clicking a Krave Box on open flat ground. 100 HP, iron-golem "
                + "toughness so he dies fast once you land hits - the trick is landing them. He "
                + "jumps absurdly high and teleports like an enderman on a sugar rush, trailing "
                + "afterimages."));
        e.add(note("STRATEGY", "Do not chase the afterimages. Stand still, watch for the real one to land, "
                + "and swing then.", GOLD));
        e.add(p("At Rule #1's death stage 10 he climbs out of the crater on his own - nobody "
                + "summons that one.", DIM));

        e.add(brk());
        e.add(sub("THE MANAGER"));
        e.add(gal(icon(ModItems.EGG_THE_MANAGER.get(), "The Manager")));
        e.add(p("Three acts, gated by health: RESERVATION, PERFORMANCE REVIEW, then FIRED. He "
                + "opens a file on you in Act Two that stays open until the fight ends, and by "
                + "Act Three he's calling in Facilities to bring the filing cabinets back in "
                + "around you."));
        e.add(note("DROPS", "Pink Slip, a Severance Check, an Employee of the Month plaque, his own tie, "
                + "and Mom's $500 back.", DIM));
        e.add(note("HONEST GAP", "No quest, item, or structure currently summons him in a normal playthrough - "
                + "he exists only via his spawn egg in this build. Killing him is also what unlocks "
                + "the Laser Lens ability (see the next chapter), so this is worth flagging loudly "
                + "for a second pass rather than burying it.", DANGER));

        e.add(brk());
        e.add(sub("MOM COBB"));
        e.add(gal(icon(ModItems.EGG_MOM_BOSS.get(), "Mom Cobb")));
        e.add(p("Three acts of escalating parenting, gated by health: \"Where have you "
                + "been?\", \"GET OFF THAT GAME\", and \"I'M TAKING THE KRAVE.\" In the last act "
                + "she goes for your stash directly and eats it herself to stay standing - hit "
                + "her hard enough and she gives up on her own terms (\"...fine. Keep it.\")."));
        e.add(note("DROPS", "Her TV remote, a pile of Confiscated Krave, a spare Krave Box, Child Support "
                + "Papers, and - one time in four - Adoption Papers. She was not joking.", DIM));
        e.add(note("HONEST GAP", "Same gap as The Manager: spawn egg only in this build, no natural trigger yet. "
                + "Defeating her is what unlocks the Ascension Charm ability.", DANGER));

        e.add(brk());
        e.add(sub("THE INTERNET OUTAGE"));
        e.add(gal(icon(InternetContent.SERVICE_CALL_BOX_ITEM.get(), "Service Call Box"),
                icon(InternetContent.ROTARY_PHONE.get(), "Rotary Phone")));
        e.add(p("The one fight in this book you can actually trigger without a spawn egg: "
                + "place a Service Call Box, then use the Rotary Phone on it, and The Internet "
                + "Manager shows up - hard hat, hi-vis, headset, a length of cable he is "
                + "entirely willing to use on you."));
        e.add(table(
                List.of("PHASE", "WHAT'S NEW"),
                List.of(60, 250),
                List.of(
                        row("CONNECTING...", "Cable-whip melee plus PACKET LOSS windows, where some of your "
                                + "hits on him simply do not register."),
                        row("BUFFERING...", "He goes fully invulnerable behind a filling loading ring and "
                                + "adds LATENCY, which rubber-bands you. Deal 42+ damage while the ring "
                                + "fills to shatter it early and stun him hard - let it finish instead "
                                + "and he unloads a THROTTLE pulse the instant it completes."),
                        row("503 SERVICE UNAVAILABLE", "Everything in rotation, shorter cooldowns, THROTTLE "
                                + "joins the mix on a much shorter fuse.")
                )
        ));
        e.add(note("THE ONE REAL TELL", "Any hit landing for 10+ damage cancels an ordinary telegraphed "
                + "attack outright (\"Rude.\") - except the buffering ring, which only breaks under "
                + "sustained damage. Interrupt everything else; save your burst for the ring.", GOLD));
        e.add(p("Drops: a Static IP (a gate item for whatever needs one), a handful of Fiber "
                + "Optic Coil, and his own headset - plus redstone and copper for the trouble.", DIM));

        return new ManualChapter("bosses", 9, "The Bosses",
                "Krave Monster, The Manager, Mom Cobb, and the guy who fixes your Wi-Fi.",
                icon(ModItems.EGG_KRAVE_MONSTER.get(), "Boss"), PURPLE, e);
    }

    // ==========================================================================
    // 10. PLAYER ABILITIES
    // ==========================================================================

    private static ManualChapter chapterAbilities() {
        List<PageElement> e = new ArrayList<>();

        e.add(head("PLAYER ABILITIES"));
        e.add(p("Six items, six borrowed pieces of Cayden's own ascension ladder, handed to "
                + "you directly instead of grown into. Every one is Epic rarity, unstackable, "
                + "and completely inert until you've actually earned it - equip-and-use, no "
                + "crafting table involved."));

        e.add(table(
                List.of("ITEM", "WHAT IT DOES", "COOLDOWN", "HOW YOU EARN IT"),
                List.of(95, 140, 55, 90),
                List.of(
                        row("Krave Gauntlet", "A charged knockback punch - the same shove Cayden's "
                                + "own flash uses.", "3s", "Slay the Krave Monster."),
                        row("Laser Lens", "Fires three Krave laser bolts from wherever you're "
                                + "looking - the exact projectile Super Saiyan Cayden fires.", "2s", "Put The Manager in the ground."),
                        row("Ascension Charm", "Brief flight, in bursts - lights the fuse, then physics "
                                + "does the rest.", "10s", "Defeat Mom Cobb."),
                        row("Meteor Totem", "Calls the apocalypse's own falling meteor down on wherever "
                                + "you're looking.", "30s", "Finish the Revenge quest."),
                        row("Instinct Band", "Ultra Instinct's \"the body answers before he does\" - total "
                                + "dodge, for a few seconds.", "60s", "Reach the Kosmos Ascension (Kosmos Master)."),
                        row("God Core", "The Super Saiyan God aura, worn instead of grown into: massive "
                                + "attack and speed, 38% damage reduction.", "120s", "Finish the entire base questline (Peace at Last).")
                )
        ));
        e.add(gal(icon(AbilityItems.KRAVE_GAUNTLET.get(), "Krave Gauntlet"),
                icon(AbilityItems.LASER_LENS.get(), "Laser Lens"),
                icon(AbilityItems.ASCENSION_CHARM.get(), "Ascension Charm"),
                icon(AbilityItems.METEOR_TOTEM.get(), "Meteor Totem"),
                icon(AbilityItems.INSTINCT_BAND.get(), "Instinct Band"),
                icon(AbilityItems.GOD_CORE.get(), "God Core")));

        e.add(note("READ THIS BEFORE YOU HUNT THEM", "Two of the six (Laser Lens and Ascension Charm) are "
                + "gated on bosses that have no natural spawn trigger in this build - see the "
                + "Bosses chapter's honest notes on The Manager and Mom Cobb. Use their spawn "
                + "eggs in creative if you need them for testing right now.", DANGER));

        e.add(brk());
        e.add(sub("WHY THESE SIX, IN THIS ORDER"));
        e.add(p("Every unlock mirrors an actual rung or move on Cayden's own ascension ladder "
                + "and combat kit - nothing here is invented separately from what he already "
                + "does. That is deliberate: these items exist so a fully-geared player can "
                + "stand next to a fully-ascended Cayden and genuinely keep up, using powers "
                + "that read as \"the same trick he does\" rather than a second, unrelated "
                + "progression system bolted on beside his."));

        return new ManualChapter("abilities", 10, "Player Abilities",
                "Six of Cayden's own tricks, earned and worn instead of ascended into.",
                icon(AbilityItems.GOD_CORE.get(), "God Core"), 0xFFF4EDDD, e);
    }

    // ==========================================================================
    // 11. THE KRAVE DIMENSION
    // ==========================================================================

    private static ManualChapter chapterDimension() {
        List<PageElement> e = new ArrayList<>();

        e.add(head("THE KRAVE DIMENSION"));
        e.add(p("The Krave Kosmos. Its gate is a real building requirement, not a Nether-style "
                + "portal you can throw together on day one - see Building the Village for what "
                + "it takes to unlock it."));

        e.add(sub("BUILDING THE PORTAL"));
        e.add(p("A 3-wide, 3-tall frame of solid Krave Block, with the bottom-middle two cells "
                + "replaced by the Krave Door. Open the door with the frame complete around it "
                + "and you step straight through."));
        e.add(shaped3x3("KKK\nK.K\nK.K", java.util.Map.of('K', regBlock(ModBlocks.KRAVE_BLOCK, "Krave Block")),
                regBlock(ModBlocks.KRAVE_DOOR, "Krave Door goes in the two empty cells"), 1,
                "FRAME SHAPE - not a crafting recipe, a build layout"));
        e.add(note("YOUR VILLAGE HAS TO EARN THIS FIRST", "The door only actually opens once your nearest "
                + "settlement reads as VILLAGE tier or higher. Building the frame early just gets "
                + "you a door that doesn't work yet.", PURPLE));

        e.add(brk());
        e.add(sub("WHAT HAPPENS WHEN YOU STEP THROUGH"));
        e.add(bul(
                "The game finds real solid ground for you near the landing point instead of "
                        + "trusting a fixed coordinate - you will not fall into the void on arrival.",
                "If you're carrying fewer than one Krave Tether, you're handed one for free. "
                        + "Nobody should be able to walk in without a way back out.",
                "Cayden and Barbara come with you, if they're nearby when you step through - "
                        + "and Cayden ascends to Super Saiyan the instant he arrives, whether or "
                        + "not that costs him anything (arrival is one of the free-ascension "
                        + "triggers from the Cayden chapter).",
                "The Krave Monster's den generates once, near the dimension's centre: a "
                        + "guaranteed-solid platform ringed with pillars, with hidden healing boxes "
                        + "protecting him."
        ));
        e.add(gal(icon(ModItems.KRAVE_TETHER.get(), "Krave Tether")));
        e.add(p("Use the Tether to head straight back to the exact doorway you stepped in "
                + "from - the game remembers your entry position and dimension the moment you "
                + "cross over.", DIM));

        e.add(brk());
        e.add(sub("WHAT LIVES OUT THERE"));
        e.add(p("Kosmonauts (Krave Minions) spawn ambiently around every player exploring the "
                + "islands, capped at three per player within a wide radius - there to give you "
                + "something to run into between the portal and the boss's den, not to overwhelm "
                + "you. Cayden ignores them entirely; they're yours to handle."));
        e.add(p("Cave pockets scattered through the terrain sometimes hide a Krave Healing Box "
                + "and a scattering of Krave Cereal - explore, don't just beeline for the "
                + "island.", DIM));

        return new ManualChapter("dimension", 11, "The Krave Dimension",
                "A real portal, a real gate condition, and a boss's den waiting on the other side.",
                regBlock(ModBlocks.KRAVE_DOOR, "Krave Door"), 0xFF6B3FA0, e);
    }
}
