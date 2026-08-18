package com.barbarajones.client;

import com.barbarajones.quest.Quests;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The Krave Quest Book screen - now a branching board. Reads completion straight
 * off the carried book's NBT, groups quests by branch into two columns, and marks
 * each as done / available / locked. Available quests show their objective inline.
 */
public class QuestBookScreen extends Screen {

    // Which branches sit in the left column; everything else goes right.
    private static final List<String> LEFT = List.of(Quests.B_STORY, Quests.B_GRASS, Quests.B_KRAVE);

    public QuestBookScreen() {
        super(Component.literal("The Krave Quest"));
    }

    private ItemStack book() {
        var player = Minecraft.getInstance().player;
        return player == null ? null : Quests.findBook(player);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx);
        ItemStack book = book();

        int cx = this.width / 2;
        int colW = 232;
        int gap = 16;
        int totalW = colW * 2 + gap;
        int leftX = cx - totalW / 2;
        int rightX = leftX + colW + gap;
        int top = 18;

        gfx.fill(leftX - 10, top - 12, leftX + totalW + 10, this.height - 12, 0xC0100010);

        int done = Quests.doneCount(book);
        int total = Quests.total();
        gfx.drawCenteredString(this.font,
                ChatFormatting.LIGHT_PURPLE + "" + ChatFormatting.BOLD + "THE KRAVE QUEST  "
                        + ChatFormatting.GRAY + "(" + done + "/" + total + ")",
                cx, top - 6, 0xFFFFFF);

        List<String> leftBranches = new ArrayList<>();
        List<String> rightBranches = new ArrayList<>();
        for (String b : Quests.BRANCHES) {
            (LEFT.contains(b) ? leftBranches : rightBranches).add(b);
        }

        int startY = top + 12;
        renderColumn(gfx, book, leftBranches, leftX, startY, colW);
        renderColumn(gfx, book, rightBranches, rightX, startY, colW);

        if (Quests.isDone(book, Quests.PEACE)) {
            gfx.drawCenteredString(this.font,
                    ChatFormatting.GOLD + "" + ChatFormatting.BOLD + "PEACE AT LAST - EVERY ITEM COLLECTED",
                    cx, this.height - 22, 0xFFFFFF);
        }
        super.render(gfx, mouseX, mouseY, partial);
    }

    private void renderColumn(GuiGraphics gfx, ItemStack book, List<String> branches,
                              int x, int y, int colW) {
        for (String branch : branches) {
            gfx.drawString(this.font,
                    ChatFormatting.GOLD + "" + ChatFormatting.BOLD + branch.toUpperCase(),
                    x, y, 0xFFFFFF);
            y += 11;
            for (Quests.Quest q : Quests.ALL) {
                if (!q.branch.equals(branch)) {
                    continue;
                }
                boolean qDone = Quests.isDone(book, q.id);
                boolean available = !qDone && Quests.isUnlocked(book, q);

                String mark = qDone ? (ChatFormatting.GREEN + "[x] ")
                        : available ? (ChatFormatting.YELLOW + "[>] ")
                        : (ChatFormatting.DARK_GRAY + "[ ] ");
                ChatFormatting col = qDone ? ChatFormatting.GREEN
                        : available ? ChatFormatting.YELLOW : ChatFormatting.DARK_GRAY;
                gfx.drawString(this.font, mark + col + q.title, x, y, 0xFFFFFF);
                y += 10;

                if (available) {
                    for (var line : this.font.split(
                            Component.literal(ChatFormatting.GRAY + q.objective), colW - 14)) {
                        gfx.drawString(this.font, line, x + 12, y, 0xBBBBBB);
                        y += 9;
                    }
                    y += 1;
                }
            }
            y += 6;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
