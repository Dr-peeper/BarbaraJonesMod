package com.barbarajones.client;

import com.barbarajones.quest.Quests;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The Krave Quest Book screen - reads the stage straight off the carried book. */
public class QuestBookScreen extends Screen {

    public QuestBookScreen() {
        super(Component.literal("The Krave Quest"));
    }

    private int currentStage() {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return Quests.START;
        }
        ItemStack book = Quests.findBook(player);
        return book == null ? Quests.START : Quests.getStage(book);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        renderBackground(gfx);

        int cx = this.width / 2;
        int panelW = 300;
        int left = cx - panelW / 2;
        int top = 24;
        gfx.fill(left - 8, top - 12, left + panelW + 8, top + 20 + Quests.DONE * 22, 0xC0100010);

        gfx.drawCenteredString(this.font,
                ChatFormatting.LIGHT_PURPLE + "" + ChatFormatting.BOLD + "THE KRAVE QUEST",
                cx, top - 6, 0xFFFFFF);

        int stage = currentStage();
        int y = top + 14;
        for (int i = 0; i < Quests.DONE; i++) {
            boolean done = stage > i;
            boolean active = stage == i;
            String mark = done ? (ChatFormatting.GREEN + "[x] ")
                    : active ? (ChatFormatting.YELLOW + "[>] ")
                    : (ChatFormatting.DARK_GRAY + "[ ] ");
            String title = (done ? ChatFormatting.GREEN
                    : active ? ChatFormatting.YELLOW : ChatFormatting.GRAY) + Quests.TITLE[i];
            gfx.drawString(this.font, mark + title, left, y, 0xFFFFFF);
            y += 11;
            if (active) {
                for (var line : this.font.split(
                        Component.literal(ChatFormatting.WHITE + Quests.OBJECTIVE[i]), panelW - 16)) {
                    gfx.drawString(this.font, line, left + 12, y, 0xCCCCCC);
                    y += 10;
                }
                y += 2;
            }
        }

        if (stage >= Quests.DONE) {
            gfx.drawCenteredString(this.font,
                    ChatFormatting.GOLD + "" + ChatFormatting.BOLD + "QUEST COMPLETE",
                    cx, y + 6, 0xFFFFFF);
        }
        super.render(gfx, mouseX, mouseY, partial);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
