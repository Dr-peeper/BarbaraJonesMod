package com.barbarajones.client.ui;

import com.barbarajones.quest.Quests;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * THE KRAVE CODEX - the mod's own dashboard, opened with the Codex key (K by
 * default) and built entirely out of {@link KraveTheme} / {@link KraveScreen}.
 *
 * <p>Three tabs. QUESTS is the live questline read off the Quest Book's NBT, branch
 * by branch, with a progress bar per branch and the objective spelled out for
 * anything currently open. CREW is the cast and how you deal with each of them.
 * RULES is Rule #1, the things that actually keep Cayden alive, and the controls.
 *
 * <p>Everything here is derived from data that is already on the client, so the
 * screen works in single player and on a server with no packets of its own.
 */
public class KraveCodexScreen extends KraveScreen {

    private static final int TAB_QUESTS = 0;
    private static final int TAB_CREW = 1;
    private static final int TAB_RULES = 2;

    /** One line of the cast list. */
    private record Crew(int icon, String name, String blurb, String key, String value) { }

    private static final Crew[] CAST = {
        new Crew(KraveTheme.ICON_GRASS, "Barbara Jones",
                "Middle-aged, smokes grass she pulled out of the ground, and goes "
                        + "completely psycho the second her stash runs dry.",
                "Recruit", "right-click, empty hand"),
        new Crew(KraveTheme.ICON_BOX, "Cayden Cobb",
                "Runaway kid, addicted to Krave cereal, and your companion. "
                        + "RULE #1 is that he must not die.",
                "Feed", "right-click with Krave Cereal"),
        new Crew(KraveTheme.ICON_KRAVE, "The Krave Monster",
                "Purple galaxy hoodie, enormous jumps, teleports like an enderman "
                        + "and trails afterimages. 100 HP and a boss bar.",
                "Summon", "right-click a Krave Box"),
        new Crew(KraveTheme.ICON_SKULL, "The Plug",
                "Black ski mask. Sells fake drugs for real money and snipes Cayden "
                        + "from forty blocks away for no reason at all.",
                "Trade", "emeralds for grass"),
        new Crew(KraveTheme.ICON_CAR, "Duhl Wol",
                "The debt collector. Pulls up in a car and demands tribute.",
                "Tribute", "pay up, or don't"),
        new Crew(KraveTheme.ICON_DONUT, "Mom Cobb",
                "Do not go looking for her. She has the adoption papers and she "
                        + "is not afraid to bring them up.",
                "Advice", "stay indoors"),
        new Crew(KraveTheme.ICON_FLAME, "Daniel",
                "The lighter guy. That is the whole role and he plays it perfectly.",
                "Drops", "Daniel's Lighter"),
        new Crew(KraveTheme.ICON_TIE, "The Manager",
                "The necktie. Turns up at death stage 7, which is exactly as bad "
                        + "as that sounds.",
                "Drops", "the Manager's Tie"),
        new Crew(KraveTheme.ICON_CAT, "Nugget",
                "Barbara's ginger cat. Barbara is forever hollering for her.",
                "Follows", "chicken nuggets and donuts"),
    };

    public KraveCodexScreen() {
        super(Component.literal("The Krave Codex"));
        // tabs change the panel layout, so they have to exist before init() runs
        setTabs("Quests", "Crew", "Rules");
    }

    @Override
    protected int preferredWidth() {
        return 360;
    }

    @Override
    protected int preferredHeight() {
        return 236;
    }

    @Override
    protected int footerHeight() {
        return 26;
    }

    @Override
    protected void initContent() {
        addRenderableWidget(new KraveButton(
                this.panelX + this.panelW / 2 - 45,
                this.panelY + this.panelH - KraveTheme.BORDER - 23,
                90, 20, Component.literal("Close"), this::onClose));
        updateSubtitle();
    }

    @Override
    protected void onTabChanged(int tab) {
        updateSubtitle();
    }

    private void updateSubtitle() {
        ItemStack book = book();
        if (activeTab() == TAB_QUESTS && book != null) {
            this.subtitle = Quests.doneCount(book) + "/" + Quests.total() + " done";
        } else if (activeTab() == TAB_QUESTS) {
            this.subtitle = "no quest book";
        } else {
            this.subtitle = "press 1-3";
        }
    }

    @Nullable
    private ItemStack book() {
        var player = Minecraft.getInstance().player;
        return player == null ? null : Quests.findBook(player);
    }

    @Override
    public void tick() {
        // the counter in the title bar tracks live progress while the screen is open
        updateSubtitle();
    }

    @Override
    protected void renderContent(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        int y = switch (activeTab()) {
            case TAB_CREW -> renderCrew(gfx, this.bodyY);
            case TAB_RULES -> renderRules(gfx, this.bodyY);
            default -> renderQuests(gfx, this.bodyY);
        };
        setContentHeight(y - this.bodyY);
    }

    // ---- QUESTS -------------------------------------------------------------

    private int renderQuests(GuiGraphics gfx, int y) {
        int x = this.bodyX;
        int w = this.bodyW;
        ItemStack book = book();

        if (book == null) {
            y = KraveTheme.sectionHeader(gfx, this.font, x, y, w, "No Quest Book");
            return KraveTheme.textBlock(gfx, this.font, x, y, w,
                    "Progress lives on the Quest Book itself. Craft one - Book plus Krave "
                            + "Cereal - and carry it, or none of this can be tracked.",
                    KraveTheme.TEXT_DIM);
        }

        for (String branch : Quests.BRANCHES) {
            int total = 0;
            int done = 0;
            for (Quests.Quest q : Quests.ALL) {
                if (q.branch.equals(branch)) {
                    total++;
                    if (Quests.isDone(book, q.id)) {
                        done++;
                    }
                }
            }
            y = KraveTheme.sectionHeader(gfx, this.font, x, y, w,
                    branch + "  (" + done + "/" + total + ")");
            KraveTheme.progressBar(gfx, x, y, w, 5,
                    total == 0 ? 0.0F : done / (float) total,
                    done == total ? KraveTheme.GOLD : KraveTheme.GRASS);
            y += 5 + KraveTheme.PAD_S;

            for (Quests.Quest q : Quests.ALL) {
                if (!q.branch.equals(branch)) {
                    continue;
                }
                boolean complete = Quests.isDone(book, q.id);
                boolean open = !complete && Quests.isUnlocked(book, q);

                int pipColor = complete ? KraveTheme.GRASS
                        : open ? KraveTheme.GOLD : KraveTheme.TEXT_LOCKED;
                KraveTheme.pip(gfx, x + 2, y + 2, pipColor);

                int titleColor = complete ? KraveTheme.GRASS
                        : open ? KraveTheme.MILK : KraveTheme.TEXT_LOCKED;
                String tag = q.event ? ChatFormatting.DARK_PURPLE + " [story]" : "";
                gfx.drawString(this.font,
                        (open ? ChatFormatting.BOLD.toString() : "") + q.title + tag,
                        x + 12, y, titleColor, open);
                y += 10;

                // only the quest you can actually work on gets its instructions
                if (open) {
                    for (String line : KraveTheme.wrap(this.font, q.objective, w - 12, 0)) {
                        gfx.drawString(this.font, line, x + 12, y, KraveTheme.TEXT_DIM, false);
                        y += KraveTheme.LINE_H;
                    }
                    y += 2;
                }
            }
            y += KraveTheme.PAD;
        }
        return y;
    }

    // ---- CREW ---------------------------------------------------------------

    private int renderCrew(GuiGraphics gfx, int y) {
        int x = this.bodyX;
        int w = this.bodyW;
        for (Crew c : CAST) {
            KraveTheme.icon(gfx, c.icon(), x, y);
            gfx.drawString(this.font, ChatFormatting.BOLD + c.name(), x + 21, y,
                    KraveTheme.MILK, true);
            int textY = y + 11;
            for (String line : KraveTheme.wrap(this.font, c.blurb(), w - 21, 0)) {
                gfx.drawString(this.font, line, x + 21, textY, KraveTheme.TEXT_DIM, false);
                textY += KraveTheme.LINE_H;
            }
            textY = KraveTheme.keyValue(gfx, this.font, x + 21, textY + 2, w - 21,
                    c.key(), c.value(), KraveTheme.GOLD);
            y = Math.max(textY, y + 18) + KraveTheme.PAD_S;
            KraveTheme.divider(gfx, x, y - 3, w);
        }
        return y;
    }

    // ---- RULES --------------------------------------------------------------

    private int renderRules(GuiGraphics gfx, int y) {
        int x = this.bodyX;
        int w = this.bodyW;

        y = KraveTheme.sectionHeader(gfx, this.font, x, y, w, "Rule #1");
        gfx.drawString(this.font, ChatFormatting.BOLD + "DO NOT LET CAYDEN COBB DIE.",
                x, y, KraveTheme.DANGER, true);
        y += 11;
        y = KraveTheme.textBlock(gfx, this.font, x, y, w,
                "Everything else in this mod is optional. This is not. When a tamed Cayden "
                        + "(or a pet Barbara) dies, the Krave Apocalypse fires and you cannot "
                        + "stop it, cancel it or outrun it. You take no damage from any of it. "
                        + "That is the only mercy in the whole sequence.",
                KraveTheme.TEXT);
        y += KraveTheme.PAD;

        y = KraveTheme.sectionHeader(gfx, this.font, x, y, w, "Keeping him alive");
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "House him",
                "a real room beats following you", KraveTheme.TEXT);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Feed him",
                "Krave raises his attack", KraveTheme.TEXT);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Watch the fat",
                "every 5 boxes slows him down", KraveTheme.TEXT);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Carry the compass",
                "know where he is beforehand", KraveTheme.TEXT);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Light his room",
                "mobs spawn next to him in the dark", KraveTheme.TEXT);
        y += KraveTheme.PAD;

        y = KraveTheme.sectionHeader(gfx, this.font, x, y, w, "Controls");
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Open this codex",
                KraveKeys.openCodexKeyName(), KraveTheme.GOLD);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Switch tab",
                "1 - 3, or click", KraveTheme.GOLD);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Scroll",
                "wheel, PgUp/PgDn, Home/End", KraveTheme.GOLD);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Close",
                "Esc", KraveTheme.GOLD);
        y += KraveTheme.PAD;

        y = KraveTheme.sectionHeader(gfx, this.font, x, y, w, "The motifs");
        List<String> motifs = KraveTheme.wrap(this.font,
                "Krave cereal boxes. Mr Pibb. Chicken nuggets. Donuts. The backwards red hat. "
                        + "\"I KRAVE THE KRAVE.\" The $500 scam. The sewers. And somewhere out "
                        + "past all of it, the Krave Kosmos - liquid chocolate and floating "
                        + "islands, and something in a purple hoodie waiting on one of them.",
                w, 0);
        for (String line : motifs) {
            gfx.drawString(this.font, line, x, y, KraveTheme.TEXT_DIM, false);
            y += KraveTheme.LINE_H;
        }
        return y + KraveTheme.PAD;
    }
}
