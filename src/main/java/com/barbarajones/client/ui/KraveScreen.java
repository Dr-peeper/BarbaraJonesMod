package com.barbarajones.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for every Krave screen: a centred cereal-box panel with a title bar,
 * an optional tab strip, a scissored scrolling body and keyboard focus outlines.
 *
 * <p>Subclasses implement {@link #renderContent} and draw in <em>content space</em>,
 * which starts at ({@link #bodyX}, {@link #bodyY}) and is translated by the scroll
 * offset for them. They must report how tall their content is with
 * {@link #setContentHeight(int)} or scrolling has nothing to work against.
 *
 * <p>Widgets added with {@code addRenderableWidget} are <em>not</em> scrolled - they
 * render on top at absolute coordinates - so they belong in the footer, whose height
 * a subclass reserves by overriding {@link #footerHeight()}.
 *
 * <p>Keys: 1-9 jump to a tab, PageUp/PageDown/Home/End scroll. Arrows and Tab are
 * deliberately left alone so vanilla's focus navigation keeps working.
 */
public abstract class KraveScreen extends Screen {

    private static final int MARGIN = 18;
    private static final int SCROLL_STEP = 16;

    private final List<String> tabs = new ArrayList<>();
    private int activeTab;
    private int scroll;
    private int contentHeight;
    private int hoveredTab = -1;

    /** Right-aligned text in the title bar - a counter, a stage, a warning. */
    protected String subtitle = "";

    /** The panel rect, in screen space. */
    protected int panelX;
    protected int panelY;
    protected int panelW;
    protected int panelH;

    /** The scrolling viewport, in screen space. */
    protected int bodyX;
    protected int bodyY;
    protected int bodyW;
    protected int bodyH;

    protected KraveScreen(Component title) {
        super(title);
    }

    // ---- layout hooks -------------------------------------------------------

    protected int preferredWidth() {
        return 340;
    }

    protected int preferredHeight() {
        return 220;
    }

    /** Space reserved at the bottom of the panel for buttons. */
    protected int footerHeight() {
        return 0;
    }

    /** Called after the panel rect is known; add widgets here instead of in init(). */
    protected void initContent() { }

    /** Draw the body. y is already translated by the scroll offset. */
    protected abstract void renderContent(GuiGraphics gfx, int mouseX, int mouseY, float partial);

    /** Drawn last, unclipped and unscrolled - tooltips and other floaters. */
    protected void renderForeground(GuiGraphics gfx, int mouseX, int mouseY, float partial) { }

    /** Called when the player picks a different tab. Scroll is already reset. */
    protected void onTabChanged(int tab) { }

    // ---- tabs / scroll state ------------------------------------------------

    /**
     * Call this from the subclass constructor, not from {@link #initContent()} -
     * the tab strip changes the panel layout, which {@code init()} has already
     * computed by the time initContent runs.
     */
    protected void setTabs(String... labels) {
        this.tabs.clear();
        for (String s : labels) {
            this.tabs.add(s);
        }
        if (this.activeTab >= this.tabs.size()) {
            this.activeTab = 0;
        }
    }

    protected int activeTab() {
        return this.activeTab;
    }

    protected void setActiveTab(int tab) {
        if (this.tabs.isEmpty()) {
            return;
        }
        int clamped = Mth.clamp(tab, 0, this.tabs.size() - 1);
        if (clamped == this.activeTab) {
            return;
        }
        this.activeTab = clamped;
        this.scroll = 0;
        onTabChanged(clamped);
    }

    /** Subclasses report their laid-out height so the scrollbar knows its range. */
    protected void setContentHeight(int height) {
        this.contentHeight = Math.max(0, height);
    }

    protected int maxScroll() {
        return Math.max(0, this.contentHeight - this.bodyH);
    }

    protected void scrollBy(int pixels) {
        this.scroll = Mth.clamp(this.scroll + pixels, 0, maxScroll());
    }

    // ---- init ---------------------------------------------------------------

    @Override
    protected final void init() {
        this.panelW = Math.min(preferredWidth(), Math.max(160, this.width - MARGIN * 2));
        this.panelH = Math.min(preferredHeight(), Math.max(120, this.height - MARGIN * 2));
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;

        int inner = this.panelX + KraveTheme.BORDER;
        int innerW = this.panelW - KraveTheme.BORDER * 2;
        int top = this.panelY + KraveTheme.BORDER + KraveTheme.TITLE_H;
        if (!this.tabs.isEmpty()) {
            top += KraveTheme.TAB_H;
        }

        this.bodyX = inner + KraveTheme.PAD;
        this.bodyY = top + KraveTheme.PAD_S;
        // 6px on the right is the scrollbar gutter, always reserved so content
        // never reflows the moment a list grows past one screenful
        this.bodyW = innerW - KraveTheme.PAD * 2 - 6;
        this.bodyH = (this.panelY + this.panelH - KraveTheme.BORDER - KraveTheme.PAD_S
                - footerHeight()) - this.bodyY;

        initContent();
    }

    // ---- render -------------------------------------------------------------

    @Override
    public final void render(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        try {
            draw(gfx, mouseX, mouseY, partial);
        } catch (Throwable ignored) {
            // a screen that throws mid-render locks the player out of the game
        }
    }

    private void draw(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        KraveTheme.scrim(gfx, this.width, this.height);
        KraveTheme.panel(gfx, this.panelX, this.panelY, this.panelW, this.panelH);

        int inner = this.panelX + KraveTheme.BORDER;
        int innerW = this.panelW - KraveTheme.BORDER * 2;
        int y = this.panelY + KraveTheme.BORDER;
        KraveTheme.titleBar(gfx, this.font, inner, y, innerW, getTitle().getString(), this.subtitle);
        y += KraveTheme.TITLE_H;

        if (!this.tabs.isEmpty()) {
            this.hoveredTab = KraveTheme.tabStrip(gfx, this.font, inner, y, innerW,
                    this.tabs, this.activeTab, mouseX, mouseY);
            y += KraveTheme.TAB_H;
        }

        this.scroll = Mth.clamp(this.scroll, 0, maxScroll());
        if (this.bodyW > 0 && this.bodyH > 0) {
            KraveTheme.well(gfx, this.bodyX - KraveTheme.PAD_S, this.bodyY - KraveTheme.PAD_S,
                    this.bodyW + KraveTheme.PAD_S * 2 + 6, this.bodyH + KraveTheme.PAD_S * 2);
            gfx.enableScissor(this.bodyX, this.bodyY, this.bodyX + this.bodyW + 6,
                    this.bodyY + this.bodyH);
            gfx.pose().pushPose();
            gfx.pose().translate(0.0F, (float) -this.scroll, 0.0F);
            // mouseY is shifted into content space so subclasses can hit-test rows
            renderContent(gfx, mouseX, mouseY + this.scroll, partial);
            gfx.pose().popPose();
            gfx.disableScissor();
            KraveTheme.scrollbar(gfx, this.bodyX + this.bodyW + 3, this.bodyY, this.bodyH,
                    this.contentHeight, this.bodyH, this.scroll);
        }

        super.render(gfx, mouseX, mouseY, partial);

        GuiEventListener focused = getFocused();
        if (focused instanceof AbstractWidget w && w.visible && !(focused instanceof KraveButton)) {
            // KraveButton draws its own ring; this covers any vanilla widget a
            // subclass adds, so the focus caret is never invisible
            KraveTheme.focusRing(gfx, w.getX(), w.getY(), w.getWidth(), w.getHeight());
        }

        renderForeground(gfx, mouseX, mouseY, partial);
    }

    // ---- input --------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.tabs.isEmpty() && button == 0) {
            int hit = KraveTheme.tabAt(this.panelX + KraveTheme.BORDER,
                    this.panelY + KraveTheme.BORDER + KraveTheme.TITLE_H,
                    this.panelW - KraveTheme.BORDER * 2, this.tabs.size(), mouseX, mouseY);
            if (hit >= 0) {
                setActiveTab(hit);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX >= this.bodyX && mouseX < this.bodyX + this.bodyW + 6
                && mouseY >= this.bodyY && mouseY < this.bodyY + this.bodyH) {
            scrollBy((int) (-delta * SCROLL_STEP));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode >= InputConstants.KEY_1 && keyCode <= InputConstants.KEY_9 && !this.tabs.isEmpty()) {
            int want = keyCode - InputConstants.KEY_1;
            if (want < this.tabs.size()) {
                setActiveTab(want);
                return true;
            }
        }
        if (keyCode == InputConstants.KEY_PAGEUP) {
            scrollBy(-this.bodyH + SCROLL_STEP);
            return true;
        }
        if (keyCode == InputConstants.KEY_PAGEDOWN) {
            scrollBy(this.bodyH - SCROLL_STEP);
            return true;
        }
        if (keyCode == InputConstants.KEY_HOME) {
            scrollBy(-this.contentHeight);
            return true;
        }
        if (keyCode == InputConstants.KEY_END) {
            scrollBy(this.contentHeight);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** These screens are read while the world keeps running - same as the books. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Convenience for subclasses: is this row currently under the pointer? */
    protected boolean rowHovered(int mouseX, int mouseY, int y, int height) {
        return mouseX >= this.bodyX && mouseX < this.bodyX + this.bodyW
                && mouseY >= y && mouseY < y + height;
    }

    protected int hoveredTab() {
        return this.hoveredTab;
    }
}
