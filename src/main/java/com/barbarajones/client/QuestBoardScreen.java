package com.barbarajones.client;

import com.barbarajones.quest.Quests;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Krave Quest Book, paginated.
 *
 * <p>{@link QuestBookScreen} lays every branch out at once in two fixed columns, which
 * works fine for the original thirty-odd quests and falls straight off the bottom of the
 * screen once the expansion board is spliced in. This one shows a contents page of
 * branches with their completion counts, then two branches per page, so the board can
 * grow without the layout collapsing.
 *
 * <p>It reads the same NBT off the same carried book - no state of its own - so the two
 * screens are interchangeable. To use it, {@code ClientPacketHandler.openQuestBook()}
 * opens this instead of {@code QuestBookScreen}.
 */
public class QuestBoardScreen extends Screen {

    /** How many branch columns share one page. */
    private static final int PER_PAGE = 2;
    private static final int COL_W = 232;
    private static final int GAP = 16;

    /** -1 = contents; otherwise the page of branches. */
    private int page = -1;

    public QuestBoardScreen() {
        super(Component.literal("The Krave Quest"));
    }

    @Nullable
    private static ItemStack book() {
        var player = Minecraft.getInstance().player;
        return player == null ? null : Quests.findBook(player);
    }

    private static int pageCount() {
        return Math.max(1, (Quests.BRANCHES.size() + PER_PAGE - 1) / PER_PAGE);
    }

    @Override
    protected void init() {
        rebuild();
    }

    private void rebuild() {
        clearWidgets();
        int cx = this.width / 2;

        if (this.page < 0) {
            int top = 44;
            List<String> branches = Quests.BRANCHES;
            for (int i = 0; i < branches.size(); i++) {
                final int target = i / PER_PAGE;
                String branch = branches.get(i);
                // Two buttons per row so a dozen branches still fit on one contents page.
                int col = i % 2;
                int row = i / 2;
                addRenderableWidget(Button.builder(
                        Component.literal(branch + "  " + progressOf(branch)), b -> {
                            this.page = target;
                            rebuild();
                        }).bounds(cx - 158 + col * 160, top + row * 22, 156, 20).build());
            }
            return;
        }

        addRenderableWidget(Button.builder(Component.literal("< Back"), b -> {
            this.page = Math.max(0, this.page - 1);
            rebuild();
        }).bounds(cx - 155, this.height - 26, 60, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Branches"), b -> {
            this.page = -1;
            rebuild();
        }).bounds(cx - 40, this.height - 26, 80, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Next >"), b -> {
            this.page = Math.min(pageCount() - 1, this.page + 1);
            rebuild();
        }).bounds(cx + 95, this.height - 26, 60, 20).build());
    }

    /** "3/12" for one branch, coloured gold once the branch is finished. */
    private static String progressOf(String branch) {
        int done = 0;
        int total = 0;
        ItemStack book = book();
        for (Quests.Quest q : Quests.ALL) {
            if (!q.branch.equals(branch)) {
                continue;
            }
            total++;
            if (Quests.isDone(book, q.id)) {
                done++;
            }
        }
        String colour = done >= total ? ChatFormatting.GOLD.toString() : ChatFormatting.GRAY.toString();
        return colour + "(" + done + "/" + total + ")";
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx);
        ItemStack book = book();
        int cx = this.width / 2;
        int totalW = COL_W * PER_PAGE + GAP;

        gfx.fill(cx - totalW / 2 - 10, 6, cx + totalW / 2 + 10, this.height - 32, 0xC0100010);
        gfx.drawCenteredString(this.font,
                ChatFormatting.LIGHT_PURPLE + "" + ChatFormatting.BOLD + "THE KRAVE QUEST  "
                        + ChatFormatting.GRAY + "(" + Quests.doneCount(book) + "/"
                        + Quests.total() + ")", cx, 12, 0xFFFFFF);

        if (this.page < 0) {
            gfx.drawCenteredString(this.font,
                    ChatFormatting.YELLOW + "Pick a branch. They can be worked in any order.",
                    cx, 26, 0xFFFFFF);
            if (Quests.isDone(book, Quests.PEACE)) {
                gfx.drawCenteredString(this.font, ChatFormatting.GOLD + "" + ChatFormatting.BOLD
                        + "PEACE AT LAST - EVERY BRANCH WALKED", cx, this.height - 44, 0xFFFFFF);
            }
            super.render(gfx, mouseX, mouseY, partial);
            return;
        }

        this.page = Math.max(0, Math.min(this.page, pageCount() - 1));
        int leftX = cx - totalW / 2;
        for (int slot = 0; slot < PER_PAGE; slot++) {
            int index = this.page * PER_PAGE + slot;
            if (index >= Quests.BRANCHES.size()) {
                break;
            }
            renderBranch(gfx, book, Quests.BRANCHES.get(index),
                    leftX + slot * (COL_W + GAP), 28);
        }

        gfx.drawCenteredString(this.font, ChatFormatting.DARK_GRAY + "page "
                + (this.page + 1) + " / " + pageCount(), cx, this.height - 40, 0xFFFFFF);
        super.render(gfx, mouseX, mouseY, partial);
    }

    private void renderBranch(GuiGraphics gfx, @Nullable ItemStack book, String branch,
                              int x, int y) {
        gfx.drawString(this.font, ChatFormatting.GOLD + "" + ChatFormatting.BOLD
                + branch.toUpperCase() + "  " + progressOf(branch), x, y, 0xFFFFFF);
        y += 12;

        int floor = this.height - 46;
        for (Quests.Quest q : Quests.ALL) {
            if (!q.branch.equals(branch)) {
                continue;
            }
            if (y > floor) {
                gfx.drawString(this.font, ChatFormatting.DARK_GRAY + "...", x, y, 0xFFFFFF);
                return;
            }
            boolean done = Quests.isDone(book, q.id);
            boolean available = !done && Quests.isUnlocked(book, q);

            String mark = done ? (ChatFormatting.GREEN + "[x] ")
                    : available ? (ChatFormatting.YELLOW + "[>] ")
                    : (ChatFormatting.DARK_GRAY + "[ ] ");
            ChatFormatting colour = done ? ChatFormatting.GREEN
                    : available ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY;
            gfx.drawString(this.font, mark + colour + q.title, x, y, 0xFFFFFF);
            y += 10;

            // Only live objectives get their text: locked ones would spoil the tree and
            // finished ones no longer need instructions.
            if (available) {
                for (var line : this.font.split(
                        Component.literal(ChatFormatting.GRAY + q.objective), COL_W - 14)) {
                    if (y > floor) {
                        break;
                    }
                    gfx.drawString(this.font, line, x + 12, y, 0xBBBBBB);
                    y += 9;
                }
                y += 2;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
