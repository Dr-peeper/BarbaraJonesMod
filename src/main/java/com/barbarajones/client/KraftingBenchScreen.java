package com.barbarajones.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.menu.KraftingBenchMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class KraftingBenchScreen extends AbstractContainerScreen<KraftingBenchMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/gui/container/krafting_bench.png");

    public KraftingBenchScreen(KraftingBenchMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
