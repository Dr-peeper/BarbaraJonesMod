package com.barbarajones.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * THE KRAVE MANUAL - the whole mod, written down.
 *
 * A contents page plus chapters of fixed pages. Page 0 is always the contents,
 * and every chapter button jumps straight to that chapter's first page.
 */
public class KraveManualScreen extends Screen {

    private static final String R  = ChatFormatting.RED.toString();
    private static final String DR = ChatFormatting.DARK_RED.toString();
    private static final String G  = ChatFormatting.GREEN.toString();
    private static final String Y  = ChatFormatting.YELLOW.toString();
    private static final String W  = ChatFormatting.WHITE.toString();
    private static final String GR = ChatFormatting.GRAY.toString();
    private static final String A  = ChatFormatting.AQUA.toString();
    private static final String P  = ChatFormatting.LIGHT_PURPLE.toString();
    private static final String GO = ChatFormatting.GOLD.toString();
    private static final String B  = ChatFormatting.BOLD.toString();

    /** One chapter: a name for the contents page and its pages of text. */
    private record Chapter(String name, String[][] pages) { }

    private static final Chapter[] CHAPTERS = {

        // ==================================================================
        new Chapter("RULE #1: DON'T LET HIM DIE", new String[][] {
            { DR + B + "RULE #1 - DO NOT LET CAYDEN COBB DIE.",
              W + "Everything else in this book is optional.",
              W + "This is not.",
              GR + " ",
              Y + "When a tamed Cayden (or a pet Barbara) dies,",
              Y + "the KRAVE APOCALYPSE fires. You cannot stop",
              Y + "it, cancel it, or outrun it.",
              GR + " ",
              W + "The sky turns blood red. A face fills your",
              W + "screen. A siren drags itself down an octave.",
              W + "Something enormous falls. Then it gets worse.",
              GR + " ",
              A + "You take no damage from any of it.",
              A + "That is the only mercy in the whole sequence." },

            { DR + B + "HOW TO KEEP HIM ALIVE",
              GR + " ",
              G + "1. HOUSE HIM." + W + " A housed Cayden stays near",
              W + "   his bed instead of following you into",
              W + "   caves, lava and creepers. This is the",
              W + "   single biggest thing you can do.",
              G + "2. FEED HIM." + W + " Krave raises his attack, and",
              W + "   a stronger Cayden wins fights he would",
              W + "   otherwise lose.",
              G + "3. WATCH THE FAT." + W + " Every 5 boxes makes him",
              W + "   slower. A fat Cayden cannot run away.",
              G + "4. CARRY THE COMPASS." + W + " Know where he is",
              W + "   before you hear the siren, not after.",
              G + "5. LIGHT HIS ROOM." + W + " Mobs spawn in the dark",
              W + "   ones, and they spawn next to him." },

            { DR + B + "WHAT ACTUALLY KILLS HIM",
              GR + " ",
              R + "Fall damage." + W + " He follows you off ledges.",
              R + "Hostile mobs." + W + " He attacks every hostile on",
              W + "  sight, and they all hunt him back. He",
              W + "  does not know when to quit.",
              R + "The Plug." + W + " He snipes Cayden from 40 blocks",
              W + "  for 8 damage, unprovoked, forever.",
              R + "Your own sword." + W + " Watch your swing.",
              R + "Lava, cactus, drowning." + W + " He is a child.",
              GR + " ",
              A + "After a death he respawns with 30 SECONDS",
              A + "of total immunity - green sparkles mean",
              A + "untouchable. Use that half minute to walk",
              A + "him somewhere safe." },

            { DR + B + "THE TEN DEATH STAGES",
              GR + "Each death is worse than the last.",
              W + "1  The face. The siren. The box falls.",
              W + "2  Barbara blows the O's across the sky.",
              W + "3  Mr. Pibb rains. The Pourer appears.",
              W + "4  The knives come down with the boxes.",
              W + "5  The Blowtorch. She lights the whole field.",
              W + "6  " + R + "THE GATES OF HELL OPEN OVERHEAD.",
              W + "7  The Manager arrives. Meteors follow.",
              W + "8  The Cleaver falls out of the sky.",
              W + "9  Every actor at once. Full wrath.",
              W + "10 " + DR + "THE KRAVE MONSTER CLIMBS OUT OF IT.",
              GR + " ",
              DR + B + "DEATH 11 AND BEYOND: IT NEVER STOPS.",
              GR + "Endless mode. No in-game cure exists." }
        }),

        // ==================================================================
        new Chapter("QUEST WALKTHROUGH", new String[][] {
            { GO + B + "THE QUEST BOOK",
              GR + "Book + Krave Cereal. Right-click to read your",
              GR + "current objective. Progress is stored on the",
              GR + "book itself - do not throw it away.",
              GR + " ",
              Y + "0 - THE CRAVING",
              W + "Craft KRAVE CEREAL: 2 Wheat + Sugar + Cocoa",
              W + "Beans (shapeless, makes 2).",
              GR + " ",
              Y + "1 - RUNAWAY",
              W + "Find Cayden Cobb. Feed him Krave by right-",
              W + "clicking with cereal in hand. Right-click him",
              W + "EMPTY-HANDED inside a valid room to house him.",
              GR + "See the HOUSING chapter before you try." },

            { Y + "2 - THERAPY",
              W + "Find BARBARA JONES and right-click her with an",
              W + "EMPTY HAND to recruit her.",
              GR + "Grass in hand feeds her instead. Waving it at",
              GR + "her without giving it makes her PSYCHO.",
              GR + " ",
              Y + "3 - THE SUMMONING",
              W + "Craft a KRAVE BOX (2 Paper + Krave Cereal) and",
              W + "right-click it. Do this on open flat ground.",
              GR + " ",
              Y + "4 - THE FIGHT",
              W + "Kill the Krave Monster. Drink a CEREAL BOWL",
              W + "first. Barbara and Cayden fight beside you.",
              GR + "He teleports constantly - hit him between",
              GR + "blinks, and do not chase him." },

            { P + B + "ACT II - THE LORE",
              Y + "5 - I KRAVE THE KRAVE",
              W + "Craft the BACKWARDS RED HAT and right-click it",
              W + "to do the dance-walk.",
              GR + " ",
              Y + "6 - THE PREQUEL",
              W + "Craft a COMPUTER MOUSE. The download had a",
              W + "virus. Right-click to hurl it out the door.",
              GR + " ",
              Y + "7 - OFF-BRAND",
              W + "Craft MOM'S $500, find THE PLUG (black ski",
              W + "mask) and right-click him holding the money.",
              GR + "You will not enjoy what you get." },

            { Y + "8 - THE SEWER",
              W + "Go below y=45 and smoke a ROLLED JOINT. Down",
              W + "there in the dark, you work out that it was",
              W + "grass all along.",
              GR + " ",
              Y + "9 - GOT THAT MOTHAFUCKER",
              W + "The Plug is here with the sniper. Kill him.",
              GR + "Watch for the particle tracer - he fires from",
              GR + "far outside your render of caring.",
              GR + "Drops: Ski Mask, Sniper Scope, and sometimes",
              GR + "your $500 back.",
              GR + " ",
              GO + "10 - PEACE AT LAST",
              W + "The scam is avenged. The Krave is conquered.",
              DR + "The death stages, of course, remain." }
        }),

        // ==================================================================
        new Chapter("CAYDEN COBB", new String[][] {
            { A + B + "FEEDING HIM",
              W + "Right-click Cayden holding KRAVE CEREAL.",
              GR + " ",
              G + "+1 attack damage per 5 boxes eaten.",
              R + "Fatter and slower with every 5 boxes.",
              P + "25 boxes unlocks KRAVE RAGE.",
              GR + " ",
              Y + "KRAVE RAGE: " + W + "Barbara picks him up and throws",
              W + "him bodily into your enemies. It does what",
              W + "you would expect a thrown child to do.",
              GR + " ",
              A + B + "THE CAYDEN COMPASS",
              GR + "Compass + Krave Cereal + Lapis.",
              W + "Right-click for his bearing, distance, exact",
              W + "coordinates, Krave count and housing status.",
              W + "Searches 512 blocks." },

            { A + B + "HIS BEHAVIOUR",
              W + "He auto-tames to the nearest player within 12",
              W + "blocks. However he arrived, he is yours.",
              GR + " ",
              W + "He attacks any hostile within 16 blocks on",
              W + "sight - and every hostile within 12 blocks",
              W + "targets him back. He is always in a fight.",
              GR + " ",
              W + "UNHOUSED: he follows you everywhere.",
              W + "HOUSED: he stays within 22 blocks of his bed",
              W + "and walks himself home.",
              GR + " ",
              DR + "He re-checks his house every 10 seconds. Break",
              DR + "the room and he moves out and tells you so." }
        }),

        // ==================================================================
        new Chapter("HOUSING (TERRARIA RULES)", new String[][] {
            { GO + B + "WILL HE LIVE HERE?",
              GR + "Craft the HOUSING QUERY (Stick + Glass + Iron)",
              GR + "and right-click inside a room. It lists every",
              GR + "failing condition at once - not just the first.",
              GR + " ",
              Y + "ALL SIX MUST BE TRUE:",
              G + "1." + W + " SEALED. No gap to the sky or outdoors.",
              G + "2." + W + " 30+ blocks of interior air.",
              G + "3." + W + " 3+ blocks of headroom somewhere.",
              G + "4." + W + " Light level 8 or brighter.",
              G + "5." + W + " A door or trapdoor.",
              G + "6." + W + " A bed, and a solid floor under 70% of it.",
              GR + " ",
              A + "Then right-click him EMPTY-HANDED inside it.",
              DR + "Too big counts as outdoors. Rooms over 900",
              DR + "blocks of air are not houses, they are fields." }
        }),

        // ==================================================================
        new Chapter("BARBARA JONES", new String[][] {
            { G + B + "THE GRASS",
              W + "She smokes grass. Actual grass, out of the",
              W + "ground. Her stash drains constantly.",
              GR + " ",
              R + "STASH EMPTY = PSYCHO." + W + " She runs, jumps,",
              W + "grows, and comes for you. Low damage, endless",
              W + "shoving. Feed her to calm her down.",
              GR + " ",
              R + "TAUNTING HER." + W + " Holding grass near her",
              R + "without giving it" + W + " sets her off just as",
              W + "hard. Hand it over or put it away.",
              GR + " ",
              A + "RECRUIT: " + W + "right-click, EMPTY HAND.",
              A + "FEED: " + W + "right-click holding grass." },

            { G + B + "GETTING HIGH - AND HER OPINION OF IT",
              W + "Smoke a ROLLED JOINT and she blows the O's",
              W + "she invented.",
              GR + " ",
              DR + "She hates watching YOU get high.",
              W + "Smoke in front of her and she takes offence",
              W + "for a full 20 seconds - and an offended",
              W + "Barbara goes psycho whether her stash is",
              W + "full or not.",
              GR + " ",
              GR + "Smoke out of her sight, or do not smoke.",
              GR + " ",
              Y + "As a pet she fights alongside you, and her",
              Y + "death fires the apocalypse exactly like his.",
              A + "She also gets the 30-second respawn immunity." }
        }),

        // ==================================================================
        new Chapter("RECIPES: THE GRASS PIPELINE", new String[][] {
            { Y + B + "FROM THE GROUND TO THE JOINT",
              G + "Handful of Grass" + W + " = 3x any grass or fern",
              G + "Grass Knife" + W + " = Iron + Stick",
              G + "Diced Grass x2" + W + " = Handful + Grass Knife",
              G + "Burnt Grass" + W + " = Diced + Blowtorch",
              GR + "  (or just smelt Diced Grass in a furnace)",
              G + "Rolling Paper" + W + " = 1x Paper",
              G + "Rolled Joint" + W + " = Rolling Paper + 2x Burnt",
              G + "Golden Joint" + W + " = Rolled Joint + Gold Ingot",
              G + "Grass Brownie" + W + " = Diced + Cocoa + Wheat",
              G + "Grass Seeds x2" + W + " = Handful + Wheat Seeds",
              G + "Bong" + W + " = Glass Bottle + Iron + Diced Grass",
              GR + " ",
              A + "BUYING IT: " + W + "right-click THE PLUG holding",
              A + "EMERALDS. 2 handfuls each, up to 8 at a time -",
              A + "and a 1-in-4 chance he shorts you anyway." },

            { Y + B + "COCOA WITHOUT THE JUNGLE",
              GR + "Krave Cereal needs cocoa. Jungles are far.",
              GR + " ",
              G + "Roasted Husk" + W + " = smelt a Brown Mushroom",
              G + "Cocoa Substitute" + W + " = 2x Roasted Husk +",
              W + "   Sugar + Coal",
              G + "Cocoa Beans" + W + " = smelt Cocoa Substitute",
              GR + " ",
              W + "Three steps and two furnace burns instead of",
              W + "an expedition. It is a process on purpose -",
              W + "but it is a process you can do at home." }
        }),

        // ==================================================================
        new Chapter("RECIPES: KRAVE & FOOD", new String[][] {
            { P + B + "THE KRAVE LINE",
              G + "Krave Cereal x2" + W + " = 2x Wheat + Sugar + Cocoa",
              G + "Krave Box" + W + " = 2x Paper + Krave Cereal",
              DR + "  (right-click: SUMMONS THE KRAVE MONSTER)",
              G + "Krave Milk" + W + " = Milk Bucket + Krave Cereal",
              G + "Cereal Bowl" + W + " = Bowl + Krave + Krave Milk",
              A + "  (the full breakfast - big combat buffs)",
              G + "Quest Book" + W + " = Book + Krave Cereal",
              G + "Cookbook" + W + " = Book + Paper",
              G + "This Manual" + W + " = Book + Krave Box",
              G + "Cayden Compass" + W + " = Compass + Krave + Lapis",
              G + "Krave Video 1" + W + " = Paper + Krave + Redstone" },

            { Y + B + "FOOD & DRINK",
              G + "Chicken Nuggets x4" + W + " = Cooked Chicken",
              G + "10-Piece Box" + W + " = 2x Nuggets + Paper",
              G + "Fries x2" + W + " = 2x Potato",
              G + "Donut" + W + " = Wheat + Sugar + Egg",
              G + "Donut Box" + W + " = 3x Donut + Paper",
              G + "Toaster Pastries" + W + " = Bread + Sugar",
              G + "Pibb Cocktail" + W + " = Mr. Pibb + Chepina",
              G + "Pibb ZERO" + W + " = Mr. Pibb + Glass Bottle",
              GR + "  (zero sugar. zero joy.)",
              GR + " ",
              A + "Nuggets and Donuts are what NUGGET the cat",
              A + "follows. Barbara is forever hollering for her." }
        }),

        // ==================================================================
        new Chapter("RECIPES: TOOLS & LORE", new String[][] {
            { Y + B + "TOOLS OF THE TRADE",
              G + "Blowtorch" + W + " = Iron + Flint&Steel + Iron",
              G + "Daniel's Lighter" + W + " = Flint + Iron",
              G + "Microphone" + W + " = 2x Iron + Coal",
              G + "Camera" + W + " = Iron + Glass + Redstone",
              G + "Housing Query" + W + " = Stick + Glass + Iron",
              G + "Ashtray" + W + " = Iron + 2x Cobblestone",
              G + "Toothbrush" + W + " = Stick + Bone",
              G + "Towel" + W + " = 2x Wool + String",
              G + "Soap" + W + " = Slimeball + Sugar",
              G + "Sewer Grate" + W + " = 2x Iron + Iron Bars",
              G + "Yellow Teeth" + W + " = Bone + Yellow Dye" },

            { P + B + "ACT II ARTEFACTS",
              G + "Backwards Red Hat" + W + " = Red Wool + String +",
              W + "   Red Dye  " + GR + "(right-click: I KRAVE THE KRAVE!)",
              G + "All-Red Fit" + W + " = 2x Red Wool",
              G + "Computer Mouse" + W + " = Iron + Redstone + String",
              GR + "   (right-click: throw it. SMASH.)",
              G + "Pirated MC Download" + W + " = Paper + Redstone",
              G + "Computer Virus" + W + " = Download + Rotten Flesh",
              G + "Mom's $500" + W + " = 2x Gold + Paper",
              G + "Ski Mask" + W + " = Black Wool + String",
              G + "\"Cocaine\" x3" + W + " = Snowball + Paper",
              G + "Fake Weed" + GR + " - only from The Plug. Obviously." },

            { Y + B + "MEMORABILIA",
              G + "Manager's Tie" + W + " = String + Red Dye",
              G + "Child Support Papers" + W + " = 3x Paper",
              G + "Adoption Papers" + W + " = 2x Paper + Ink Sac",
              G + "Fly Rich Poster" + W + " = 2x Paper + Purple Dye",
              G + "Barbara Plush" + W + " = 2x Wool + Handful of Grass",
              G + "Mom's Belt" + W + " = Leather + Iron",
              GR + "  (it hangs on a wall. that is all it does.)",
              GR + " ",
              GR + "None of these do anything mechanical. They",
              GR + "are here because they were in the videos." }
        }),

        // ==================================================================
        new Chapter("KRAVE TOOLS (CURSED)", new String[][] {
            { P + B + "GOOD TOOLS. TERRIBLE TOOLS.",
              GR + "All five: Krave Cereal in the normal tool",
              GR + "shape. 180 uses, iron mining level,",
              GR + "+4 damage, and a speed of 14 - faster than",
              GR + "diamond. That is the deal. Here is the price:",
              GR + " ",
              Y + "PICKAXE" + W + " - every 10-20 blocks he takes a",
              W + "  bite out of it. Slowness and Hunger.",
              Y + "SWORD" + W + " - Cayden teleports directly in",
              W + "  front of your eyes to watch. Blocking",
              W + "  absolutely everything.",
              Y + "AXE" + W + " - drops cereal crumbs, and he shoves",
              W + "  you aside to get at them.",
              Y + "SHOVEL" + W + " - pure sugar. You bounce, and you",
              W + "  do not choose where.",
              Y + "HOE" + W + " - the crunch is LOUD. It pulls every",
              W + "  hostile in earshot toward you." }
        }),

        // ==================================================================
        new Chapter("EVERYONE ELSE", new String[][] {
            { DR + B + "THE KRAVE MONSTER",
              W + "Summoned by right-clicking a Krave Box.",
              W + "100 HP and a boss bar. An overweight kid in",
              W + "a purple galaxy hoodie.",
              GR + " ",
              W + "Easy to hurt, almost impossible to hit: he",
              W + "jumps enormously high and teleports like an",
              W + "enderman, trailing ten afterimages.",
              A + "Strategy: do not chase. Stand still, watch",
              A + "for the real one to land, and swing then.",
              GR + " ",
              DR + "At death stage 10 he climbs out of the crater",
              DR + "on his own. Nobody summons him." },

            { DR + B + "THE PLUG",
              W + "Black ski mask. 40 HP. Owns a sniper with a",
              W + "particle tracer that hits for 8 from 40",
              W + "blocks away, through anything.",
              R + "He shoots Cayden unprovoked, at random.",
              GR + " ",
              A + "TRADE: " + W + "Emeralds for grass (2 per emerald,",
              A + "max 8) - he shorts you 1 time in 4.",
              A + "SCAM: " + W + "Mom's $500 buys off-brand pastries,",
              A + "fake weed and snow he scraped off the ground.",
              GR + " ",
              Y + "OTHERS:",
              W + "DANIEL - the lighter guy.",
              W + "MOM COBB - do not go looking for her.",
              W + "THE MANAGER - the tie. He appears at stage 7.",
              W + "NUGGET - Barbara's ginger cat. Feed her",
              W + "  nuggets or donuts and she follows you." }
        })
    };

    // flattened page list: (chapter index, page index within chapter)
    private static final List<int[]> FLAT = new ArrayList<>();
    static {
        for (int c = 0; c < CHAPTERS.length; c++) {
            for (int p = 0; p < CHAPTERS[c].pages().length; p++) {
                FLAT.add(new int[] { c, p });
            }
        }
    }

    /** -1 = contents page; otherwise an index into FLAT. */
    private int page = -1;

    public KraveManualScreen() {
        super(Component.literal("The Krave Manual"));
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int cx = this.width / 2;
        if (this.page >= FLAT.size()) {
            this.page = FLAT.size() - 1;
        }

        if (this.page < 0) {
            // contents: one button per chapter
            int top = 46;
            for (int i = 0; i < CHAPTERS.length; i++) {
                final int target = firstPageOf(i);
                addRenderableWidget(Button.builder(
                        Component.literal((i + 1) + ".  " + CHAPTERS[i].name()), b -> {
                            this.page = target;
                            rebuild();
                        }).bounds(cx - 130, top + i * 22, 260, 20).build());
            }
            return;
        }

        addRenderableWidget(Button.builder(Component.literal("< Back"), b -> {
            this.page--;
            rebuild();
        }).bounds(cx - 155, this.height - 30, 60, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Contents"), b -> {
            this.page = -1;
            rebuild();
        }).bounds(cx - 40, this.height - 30, 80, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Next >"), b -> {
            this.page++;
            rebuild();
        }).bounds(cx + 95, this.height - 30, 60, 20).build());
    }

    private static int firstPageOf(int chapter) {
        for (int i = 0; i < FLAT.size(); i++) {
            if (FLAT.get(i)[0] == chapter && FLAT.get(i)[1] == 0) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx);
        int cx = this.width / 2;

        if (this.page < 0) {
            gfx.fill(cx - 150, 18, cx + 150, 56 + CHAPTERS.length * 22, 0xD0140406);
            gfx.drawCenteredString(this.font, GO + B + "THE KRAVE MANUAL", cx, 24, 0xFFFFFF);
            gfx.drawCenteredString(this.font,
                    DR + "Rule #1: don't let Cayden Cobb die.", cx, 36, 0xFFFFFF);
            super.render(gfx, mouseX, mouseY, partial);
            return;
        }

        this.page = Math.max(0, Math.min(this.page, FLAT.size() - 1));
        int[] at = FLAT.get(this.page);
        Chapter chapter = CHAPTERS[at[0]];
        String[] lines = chapter.pages()[at[1]];

        int top = 22;
        gfx.fill(cx - 160, top - 12, cx + 160, top + 16 + lines.length * 12, 0xD0140406);

        gfx.drawCenteredString(this.font, GO + B + chapter.name() + ChatFormatting.RESET
                + GR + "  (" + (this.page + 1) + "/" + FLAT.size() + ")", cx, top - 6, 0xFFFFFF);

        int y = top + 10;
        for (String line : lines) {
            gfx.drawString(this.font, line, cx - 152, y, 0xFFFFFF);
            y += 12;
        }
        super.render(gfx, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
