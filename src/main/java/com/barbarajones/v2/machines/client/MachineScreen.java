package com.barbarajones.v2.machines.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.machines.MachineKind;
import com.barbarajones.v2.machines.blockentity.MachineBlockEntity;
import com.barbarajones.v2.machines.menu.MachineMenu;

/**
 * The one screen behind all seven machines.
 *
 * <p>Each kind ships its own 256x256 background, laid out the way a furnace is:
 * the 176x166 panel at the origin, the filled progress arrow parked at
 * {@code u=176, v=14}, and the filled flame at {@code u=176, v=0}. That means the
 * whole screen is three {@code blit} calls with the default 256x256 assumption
 * and no atlas maths - and a texture artist can retune any machine's look without
 * a line of Java changing.
 *
 * <p>The status line under the title is the part that earns its keep. A machine
 * that has quietly stopped is the most common thing a player has to debug in an
 * automation mod, and "Out of syrup" / "Output blocked" / "No village linked"
 * answers it instantly instead of sending them to look at the wiki.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    /** Where the filled arrow and flame live in the 256x256 sheet. */
    private static final int ARROW_U = 176;
    private static final int ARROW_V = 14;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 17;

    private static final int FLAME_U = 176;
    private static final int FLAME_V = 0;
    private static final int FLAME_W = 14;
    private static final int FLAME_H = 14;

    /** Screen-space positions, matching the slot geometry in MachineMenu. */
    private static final int ARROW_X = 79;
    private static final int ARROW_Y = 34;
    private static final int FLAME_X = 56;
    private static final int FLAME_Y = 36;

    private final ResourceLocation texture;

    public MachineScreen(MachineMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
        this.texture = new ResourceLocation(BarbaraJonesMod.MODID, menu.kind().guiTexture());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Progress arrow: revealed left to right.
        int filled = Math.round(ARROW_W * menu.progressFraction());
        if (filled > 0) {
            graphics.blit(texture, leftPos + ARROW_X, topPos + ARROW_Y,
                    ARROW_U, ARROW_V, filled, ARROW_H);
        }

        // Flame: burns down from the top, so the sprite is drawn bottom-anchored.
        if (menu.kind().hasFuel) {
            int height = Math.round(FLAME_H * menu.fuelFraction());
            if (height > 0) {
                graphics.blit(texture,
                        leftPos + FLAME_X, topPos + FLAME_Y + (FLAME_H - height),
                        FLAME_U, FLAME_V + (FLAME_H - height),
                        FLAME_W, height);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        Component status = statusLine();
        if (status != null) {
            // Right-aligned on the inventory-label row. That strip is the only
            // horizontal band in a 176x166 container screen guaranteed to be free
            // of slots, and the "Inventory" label only reaches about halfway
            // across it, so the two never collide however long the status gets.
            int x = imageWidth - 8 - font.width(status);
            graphics.drawString(font, status, x, inventoryLabelY, 0x7A4A28, false);
        }
    }

    private Component statusLine() {
        int status = menu.status();
        if (status == MachineBlockEntity.STATUS_NO_FUEL) {
            return Component.translatable("gui.barbarajones.machine.no_fuel");
        }
        if (status == MachineBlockEntity.STATUS_OUTPUT_FULL) {
            return Component.translatable("gui.barbarajones.machine.output_full");
        }
        if (status == MachineBlockEntity.STATUS_NO_VILLAGE) {
            return Component.translatable("gui.barbarajones.machine.no_village");
        }
        if (menu.kind() == MachineKind.DEPOT) {
            return Component.translatable("gui.barbarajones.machine.shipped", menu.shippedTotal());
        }
        return null;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
