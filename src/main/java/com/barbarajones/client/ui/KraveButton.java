package com.barbarajones.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A button that looks like the side of a cereal box instead of the vanilla stone slab.
 *
 * <p>Extends {@link AbstractButton} rather than reimplementing a widget, so click,
 * space/enter activation, tab focus order and narration all keep working exactly as
 * players and screen readers expect - only {@code renderWidget} is replaced.
 */
public class KraveButton extends AbstractButton {

    private final Runnable action;
    private int accent = KraveTheme.BOX_RED;

    public KraveButton(int x, int y, int width, int height, Component message, Runnable action) {
        super(x, y, width, height, message);
        this.action = action;
    }

    /** Recolours the band. Returns this so it can be chained into addRenderableWidget. */
    public KraveButton accent(int argb) {
        this.accent = argb;
        return this;
    }

    @Override
    public void onPress() {
        try {
            if (this.action != null) {
                this.action.run();
            }
        } catch (Throwable ignored) {
            // a broken button must never take the screen - or the game - down
        }
    }

    @Override
    protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        try {
            draw(gfx);
        } catch (Throwable ignored) {
            // ditto: drawing is never allowed to throw out of a render pass
        }
    }

    private void draw(GuiGraphics gfx) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        boolean hot = isHoveredOrFocused() && this.active;

        gfx.fill(x, y, x + w, y + h, KraveTheme.BORDER_OUT);
        int band = !this.active ? 0xFF39323F : (hot ? this.accent : KraveTheme.dim(this.accent, 0.62F));
        gfx.fill(x + 1, y + 1, x + w - 1, y + h - 1, band);
        gfx.fillGradient(x + 2, y + 2, x + w - 2, y + h - 2,
                hot ? 0xFF35123C : KraveTheme.PANEL_BODY,
                hot ? 0xFF1E0722 : KraveTheme.PANEL_BODY_LOW);
        // a single milk highlight along the top of the face sells the bevel
        gfx.fill(x + 2, y + 2, x + w - 2, y + 3, KraveTheme.withAlpha(KraveTheme.MILK, hot ? 0.28F : 0.14F));

        int text = !this.active ? KraveTheme.TEXT_LOCKED : (hot ? KraveTheme.MILK : KraveTheme.TEXT);
        gfx.drawCenteredString(Minecraft.getInstance().font, getMessage(),
                x + w / 2, y + (h - 8) / 2, text);

        if (isFocused()) {
            KraveTheme.focusRing(gfx, x, y, w, h);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
