package com.barbarajones.v2.manual.client;

import com.barbarajones.client.ui.KraveTheme;
import com.barbarajones.v2.manual.book.Icon;
import com.barbarajones.v2.manual.book.ManualBook;
import com.barbarajones.v2.manual.book.ManualChapter;
import com.barbarajones.v2.manual.book.PageElement;
import com.barbarajones.v2.manual.book.SearchIndex;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * THE KRAVE MANUAL, 2.0.
 *
 * <p>A real in-game book: eleven chapters plus a contents page, a persistent
 * chapter rail, live search, item/block-render illustrations throughout, and
 * real crafting-grid recipe displays - see {@code book.ManualBook} for the
 * content itself and {@code book.PageElement} for why every chapter is
 * authored as flowing content rather than hand-cut pages.
 *
 * <p>This class only lays pixels out and handles input; it holds no game
 * facts of its own. If something a chapter claims turns out to be wrong, fix
 * it in {@code ManualBook}, not here.
 */
public class ManualScreen extends Screen {

    private static final int MARGIN = 16;
    private static final int RAIL_W = 102;
    private static final int RAIL_ROW_H = 16;
    private static final int FOOTER_H = 18;
    private static final int ANIM_MS = 180;

    // Parchment ink. The page interior is the one LIGHT surface in the mod, so the
    // KraveTheme palette - built for pale text on a dark panel - is unreadable on
    // it. The rail and the page-turn bar are still on dark chrome and still use
    // the theme.
    private static final int INK      = 0xFF2A1C10;
    private static final int INK_DIM  = 0xFF6B5A44;
    private static final int INK_HEAD = 0xFF7A3E12;

    private static final ResourceLocation PAPER = new ResourceLocation("barbarajones", "textures/gui/manual/manual_paper.png");
    private static final ResourceLocation COVER = new ResourceLocation("barbarajones", "textures/gui/manual/manual_cover.png");

    /** -1 = contents page, 0..10 = index into {@link ManualBook#CHAPTERS}. */
    private int chapter = -1;
    private int page = 0;
    private List<Paginator.Page> pages = List.of();

    private boolean searchOpen;
    private EditBox searchBox;
    private List<SearchIndex.Hit> searchResults = List.of();

    private long animStart;
    private int animDir = 1;

    private int panelX, panelY, panelW, panelH;
    private int railX, railY, railW, railBottom;
    private int contentX, contentY, contentW, contentH;

    private ItemStack tooltipStack;

    public ManualScreen() {
        super(Component.literal("The Krave Manual"));
    }

    // ---- layout ---------------------------------------------------------------

    @Override
    protected void init() {
        this.panelW = Math.min(452, Math.max(260, this.width - MARGIN * 2));
        this.panelH = Math.min(272, Math.max(160, this.height - MARGIN * 2));
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;

        int innerX = this.panelX + KraveTheme.BORDER;
        int innerY = this.panelY + KraveTheme.BORDER;
        int innerW = this.panelW - KraveTheme.BORDER * 2;
        int innerH = this.panelH - KraveTheme.BORDER * 2;

        this.railX = innerX + KraveTheme.PAD;
        this.railY = innerY + KraveTheme.TITLE_H + KraveTheme.PAD_S;
        this.railBottom = innerY + innerH - KraveTheme.PAD_S;

        this.contentX = this.railX + RAIL_W + 10;
        this.contentY = this.railY;
        this.contentW = innerX + innerW - KraveTheme.PAD - this.contentX;
        this.contentH = this.railBottom - this.contentY - FOOTER_H;

        this.searchBox = new EditBox(this.font, this.contentX + 4, this.contentY + 4, this.contentW - 8, 14,
                Component.literal("Search"));
        this.searchBox.setMaxLength(64);
        this.searchBox.setBordered(true);
        this.searchBox.setVisible(this.searchOpen);
        this.searchBox.setResponder(this::onSearchChanged);
        addRenderableWidget(this.searchBox);

        relayout();
    }

    private void relayout() {
        if (this.chapter >= 0 && this.contentW > 20 && this.contentH > 20) {
            ManualChapter c = ManualBook.CHAPTERS.get(this.chapter);
            this.pages = Paginator.paginate(this.font, elementsOf(c), this.contentW - 8, this.contentH - 4);
            this.page = Mth.clamp(this.page, 0, this.pages.size() - 1);
        } else {
            this.pages = List.of();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- navigation -------------------------------------------------------------

    private void goHome() {
        this.chapter = -1;
        this.page = 0;
        closeSearch();
        beginAnim(-1);
        pageTurnSound();
    }

    private void openChapter(int index) {
        this.chapter = Mth.clamp(index, 0, ManualBook.CHAPTERS.size() - 1);
        this.page = 0;
        closeSearch();
        relayout();
        beginAnim(1);
        pageTurnSound();
    }

    private void jumpTo(SearchIndex.Hit hit) {
        this.chapter = hit.chapterIndex();
        relayout();
        this.page = Paginator.pageOf(this.pages, hit.elementIndex());
        closeSearch();
        beginAnim(1);
        pageTurnSound();
    }

    private void turnPage(int delta) {
        if (this.pages.isEmpty()) {
            return;
        }
        int next = Mth.clamp(this.page + delta, 0, this.pages.size() - 1);
        if (next == this.page) {
            return;
        }
        this.page = next;
        beginAnim(delta > 0 ? 1 : -1);
        pageTurnSound();
    }

    private void toggleSearch() {
        this.searchOpen = !this.searchOpen;
        this.searchBox.setVisible(this.searchOpen);
        if (this.searchOpen) {
            this.searchBox.setValue("");
            this.searchResults = List.of();
            setFocused(this.searchBox);
        } else {
            setFocused(null);
        }
    }

    private void closeSearch() {
        if (this.searchOpen) {
            this.searchOpen = false;
            this.searchBox.setVisible(false);
            setFocused(null);
        }
    }

    private void onSearchChanged(String s) {
        this.searchResults = SearchIndex.search(s);
    }

    private void beginAnim(int dir) {
        this.animDir = dir;
        this.animStart = Util.getMillis();
    }

    private void pageTurnSound() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.0F + (this.random() - 0.5F) * 0.15F));
    }

    private float random() {
        return (float) Math.random();
    }

    // ---- render -------------------------------------------------------------

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        try {
            draw(g, mouseX, mouseY, partial);
        } catch (Throwable ignored) {
            // a manual that crashes the game is worse than no manual at all
        }
    }

    private void draw(GuiGraphics g, int mouseX, int mouseY, float partial) {
        this.tooltipStack = null;
        KraveTheme.scrim(g, this.width, this.height);

        int accent = this.chapter >= 0 ? ManualBook.CHAPTERS.get(this.chapter).accent() : KraveTheme.BOX_RED;
        KraveTheme.panel(g, this.panelX, this.panelY, this.panelW, this.panelH, accent);

        int innerX = this.panelX + KraveTheme.BORDER;
        int innerY = this.panelY + KraveTheme.BORDER;
        int innerW = this.panelW - KraveTheme.BORDER * 2;

        String title;
        String right;
        if (this.searchOpen) {
            title = "THE KRAVE MANUAL - SEARCH";
            right = this.searchResults.size() + " found";
        } else if (this.chapter < 0) {
            title = "THE KRAVE MANUAL";
            right = "CONTENTS";
        } else {
            ManualChapter c = ManualBook.CHAPTERS.get(this.chapter);
            title = "CH. " + c.number() + " - " + c.title();
            right = this.pages.isEmpty() ? "" : (this.page + 1) + " / " + this.pages.size();
        }
        KraveTheme.titleBar(g, this.font, innerX, innerY, innerW, title, right);

        drawRail(g, mouseX, mouseY);

        int animOffset = animOffsetPx();
        g.enableScissor(this.contentX, this.contentY, this.contentX + this.contentW, this.contentY + this.contentH + FOOTER_H);
        g.pose().pushPose();
        g.pose().translate(animOffset, 0.0F, 0.0F);
        if (this.searchOpen) {
            drawSearch(g, mouseX - animOffset, mouseY);
        } else if (this.chapter < 0) {
            drawContents(g, mouseX - animOffset, mouseY);
        } else {
            drawChapter(g, mouseX - animOffset, mouseY);
        }
        g.pose().popPose();
        g.disableScissor();

        if (!this.searchOpen && this.chapter >= 0) {
            drawFooter(g, mouseX, mouseY);
        }

        super.render(g, mouseX, mouseY, partial);

        if (this.tooltipStack != null && !this.tooltipStack.isEmpty()) {
            g.renderTooltip(this.font, this.tooltipStack, mouseX, mouseY);
        }
    }

    private int animOffsetPx() {
        long elapsed = Util.getMillis() - this.animStart;
        if (elapsed >= ANIM_MS) {
            return 0;
        }
        float t = Mth.clamp(elapsed / (float) ANIM_MS, 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
        return Math.round((1.0F - eased) * this.animDir * 22.0F);
    }

    // ---- rail ---------------------------------------------------------------

    private int railRowCount() {
        return 2 + ManualBook.CHAPTERS.size(); // home + chapters + search
    }

    private int railRowY(int i) {
        return this.railY + i * RAIL_ROW_H;
    }

    /** -2 = none, -1 = home, 0..10 = chapter, 100 = search toggle. */
    private int railRowAt(double mx, double my) {
        if (mx < this.railX || mx >= this.railX + RAIL_W) {
            return -2;
        }
        int n = railRowCount();
        for (int i = 0; i < n; i++) {
            int ry = railRowY(i);
            if (my >= ry && my < ry + RAIL_ROW_H) {
                if (i == 0) {
                    return -1;
                }
                if (i == n - 1) {
                    return 100;
                }
                return i - 1;
            }
        }
        return -2;
    }

    private void drawRail(GuiGraphics g, int mouseX, int mouseY) {
        int hit = railRowAt(mouseX, mouseY);
        int n = railRowCount();
        for (int i = 0; i < n; i++) {
            int ry = railRowY(i);
            boolean isHome = i == 0;
            boolean isSearch = i == n - 1;
            int chapterIdx = isHome || isSearch ? -1 : i - 1;
            boolean selected = !this.searchOpen && ((isHome && this.chapter < 0) || (chapterIdx == this.chapter));
            boolean hovered = (isHome && hit == -1) || (isSearch && hit == 100) || (!isHome && !isSearch && hit == chapterIdx);
            if (isSearch) {
                selected = this.searchOpen;
            }

            if (selected) {
                g.fill(this.railX, ry, this.railX + RAIL_W, ry + RAIL_ROW_H, KraveTheme.withAlpha(KraveTheme.GOLD, 0.22F));
                g.fill(this.railX, ry, this.railX + 2, ry + RAIL_ROW_H, KraveTheme.GOLD);
            } else if (hovered) {
                g.fill(this.railX, ry, this.railX + RAIL_W, ry + RAIL_ROW_H, 0x2AFFFFFF);
            }

            ItemStack iconStack;
            String label;
            if (isHome) {
                iconStack = new ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK);
                label = "Contents";
            } else if (isSearch) {
                iconStack = new ItemStack(net.minecraft.world.item.Items.COMPASS);
                label = "Search";
            } else {
                ManualChapter c = ManualBook.CHAPTERS.get(chapterIdx);
                iconStack = c.icon().stack();
                label = c.number() + ". " + c.title();
            }
            g.renderItem(iconStack, this.railX + 1, ry);
            String trimmed = KraveTheme.trimTo(this.font, label, RAIL_W - 20);
            g.drawString(this.font, trimmed, this.railX + 19, ry + 4,
                    selected ? KraveTheme.MILK : KraveTheme.TEXT_DIM, false);
        }
    }

    // ---- contents page --------------------------------------------------------

    private void drawContents(GuiGraphics g, int mouseX, int mouseY) {
        int x = this.contentX;
        int y = this.contentY;
        int w = this.contentW;

        RenderSystem.enableBlend();
        g.blit(COVER, x, y, 0, 0.0F, 0.0F, Math.min(w, 200), 40, 200, 40);
        RenderSystem.disableBlend();

        g.drawString(this.font, ChatFormatting.BOLD + "THE KRAVE MANUAL",
                x + 4, y + 46, INK_HEAD, false);
        g.drawString(this.font, ChatFormatting.ITALIC + "Rule #1: don't let Cayden Cobb die. Everything else is in here.",
                x + 4, y + 57, INK_DIM, false);

        int rowY = y + 72;
        for (ManualChapter c : ManualBook.CHAPTERS) {
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= rowY && mouseY < rowY + 22;
            if (hover) {
                g.fill(x, rowY, x + w, rowY + 22, 0x2AFFFFFF);
            }
            g.renderItem(c.icon().stack(), x + 2, rowY + 2);
            if (hover) {
                this.tooltipStack = c.icon().stack();
            }
            g.drawString(this.font, ChatFormatting.BOLD + "" + c.number() + ". " + c.title(),
                    x + 22, rowY + 1, INK_HEAD, false);
            g.drawString(this.font, KraveTheme.trimTo(this.font, c.teaser(), w - 24), x + 22, rowY + 11,
                    INK_DIM, false);
            rowY += 22;
        }
    }

    private void contentsRowClick(double mx, double my) {
        int x = this.contentX;
        int w = this.contentW;
        int rowY = this.contentY + 72;
        for (int i = 0; i < ManualBook.CHAPTERS.size(); i++) {
            if (mx >= x && mx < x + w && my >= rowY && my < rowY + 22) {
                openChapter(i);
                return;
            }
            rowY += 22;
        }
    }

    // ---- chapter page --------------------------------------------------------

    private void drawChapter(GuiGraphics g, int mouseX, int mouseY) {
        if (this.pages.isEmpty()) {
            return;
        }
        tilePaper(g, this.contentX, this.contentY, this.contentW, this.contentH);
        Paginator.Page pg = this.pages.get(Mth.clamp(this.page, 0, this.pages.size() - 1));
        ManualChapter c = ManualBook.CHAPTERS.get(this.chapter);
        List<PageElement> els = elementsOf(c);
        int x = this.contentX + 4;
        int w = this.contentW - 8;
        for (int i = 0; i < pg.elementIndices().size(); i++) {
            int idx = pg.elementIndices().get(i);
            int y = this.contentY + 2 + pg.ys().get(i);
            ItemStack hovered = ManualRenderer.render(g, this.font, els.get(idx), x, y, w, mouseX, mouseY);
            if (hovered != null) {
                this.tooltipStack = hovered;
            }
        }
    }

    private void tilePaper(GuiGraphics g, int x, int y, int w, int h) {
        RenderSystem.enableBlend();
        int tile = 32;
        for (int ty = 0; ty < h; ty += tile) {
            for (int tx = 0; tx < w; tx += tile) {
                int drawW = Math.min(tile, w - tx);
                int drawH = Math.min(tile, h - ty);
                g.blit(PAPER, x + tx, y + ty, 0, 0.0F, 0.0F, drawW, drawH, tile, tile);
            }
        }
        RenderSystem.disableBlend();
    }

    // ---- footer / page nav ------------------------------------------------------

    private void drawFooter(GuiGraphics g, int mouseX, int mouseY) {
        int y = this.contentY + this.contentH + 2;
        int cx = this.contentX + this.contentW / 2;
        boolean canPrev = this.page > 0;
        boolean canNext = this.page < this.pages.size() - 1;

        boolean hoverPrev = mouseX >= this.contentX && mouseX < this.contentX + 20 && mouseY >= y && mouseY < y + 12;
        boolean hoverNext = mouseX >= this.contentX + this.contentW - 20 && mouseX < this.contentX + this.contentW
                && mouseY >= y && mouseY < y + 12;

        g.drawString(this.font, canPrev ? (hoverPrev ? ChatFormatting.GOLD + "< PREV" : "< prev") : "",
                this.contentX, y, canPrev ? KraveTheme.GOLD : KraveTheme.TEXT_DIM, false);
        String mid = this.pages.isEmpty() ? "" : "Page " + (this.page + 1) + " / " + this.pages.size();
        g.drawString(this.font, mid, cx - this.font.width(mid) / 2, y, KraveTheme.TEXT_DIM, false);
        String nextLbl = canNext ? (hoverNext ? "NEXT >" : "next >") : "";
        g.drawString(this.font, nextLbl, this.contentX + this.contentW - this.font.width(nextLbl),
                y, canNext ? KraveTheme.GOLD : KraveTheme.TEXT_DIM, false);
    }

    // ---- search results -------------------------------------------------------

    private void drawSearch(GuiGraphics g, int mouseX, int mouseY) {
        int x = this.contentX;
        int y = this.contentY + 22;
        int w = this.contentW;
        if (this.searchBox.getValue().length() < 2) {
            g.drawString(this.font, ChatFormatting.ITALIC + "Type at least two letters - try \"Cayden\", "
                            + "\"housing\", \"Krave Syrup\", or \"Ultra Instinct\".",
                    x + 4, y, INK_DIM, false);
            return;
        }
        if (this.searchResults.isEmpty()) {
            g.drawString(this.font, "Nothing in the book matches that.", x + 4, y, INK_DIM, false);
            return;
        }
        int rowY = y;
        for (SearchIndex.Hit hit : this.searchResults) {
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= rowY && mouseY < rowY + 20;
            if (hover) {
                g.fill(x, rowY, x + w, rowY + 20, 0x2AFFFFFF);
            }
            String head = hit.titleMatch() ? ChatFormatting.GOLD + "" + ChatFormatting.BOLD + hit.chapterTitle()
                    : ChatFormatting.GOLD + hit.chapterTitle();
            g.drawString(this.font, head, x + 4, rowY + 1, INK_HEAD, false);
            g.drawString(this.font, KraveTheme.trimTo(this.font, hit.snippet(), w - 10), x + 4, rowY + 11,
                    INK_DIM, false);
            rowY += 20;
            if (rowY > this.contentY + this.contentH) {
                break;
            }
        }
    }

    private void searchRowClick(double mx, double my) {
        int x = this.contentX;
        int w = this.contentW;
        int rowY = this.contentY + 22;
        for (SearchIndex.Hit hit : this.searchResults) {
            if (mx >= x && mx < x + w && my >= rowY && my < rowY + 20) {
                jumpTo(hit);
                return;
            }
            rowY += 20;
        }
    }

    // ---- input ----------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int hit = railRowAt(mouseX, mouseY);
            if (hit == -1) {
                goHome();
                return true;
            }
            if (hit == 100) {
                toggleSearch();
                return true;
            }
            if (hit >= 0) {
                openChapter(hit);
                return true;
            }
            if (this.searchOpen && !(mouseY >= this.contentY && mouseY < this.contentY + 20)) {
                searchRowClick(mouseX, mouseY);
            } else if (!this.searchOpen && this.chapter < 0) {
                contentsRowClick(mouseX, mouseY);
            } else if (!this.searchOpen && this.chapter >= 0) {
                int y = this.contentY + this.contentH + 2;
                if (mouseY >= y && mouseY < y + 12) {
                    if (mouseX >= this.contentX && mouseX < this.contentX + 24) {
                        turnPage(-1);
                        return true;
                    }
                    if (mouseX >= this.contentX + this.contentW - 24 && mouseX < this.contentX + this.contentW) {
                        turnPage(1);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.searchOpen && this.chapter >= 0) {
            if (delta < 0) {
                turnPage(1);
            } else if (delta > 0) {
                turnPage(-1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchOpen) {
            if (keyCode == 256) { // GLFW_KEY_ESCAPE
                closeSearch();
                return true;
            }
            if (super.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (!this.searchOpen) {
            if (keyCode == 263 || keyCode == 266) { // LEFT / PAGE_UP
                turnPage(-1);
                return true;
            }
            if (keyCode == 262 || keyCode == 267) { // RIGHT / PAGE_DOWN
                turnPage(1);
                return true;
            }
            if (keyCode == 47) { // '/'
                toggleSearch();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    /**
     * The contents of a chapter. Everything in the book is static except the
     * recipe index, which is generated from the live recipe manager each time it
     * is opened. It cannot be built with the rest of the book, because that
     * happens at class-init, long before any recipe exists to be read.
     */
    private static List<PageElement> elementsOf(ManualChapter c) {
        return RecipeIndex.CHAPTER_ID.equals(c.id()) ? RecipeIndex.build() : c.elements();
    }
}
