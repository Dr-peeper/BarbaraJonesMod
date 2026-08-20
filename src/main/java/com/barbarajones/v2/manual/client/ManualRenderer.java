package com.barbarajones.v2.manual.client;

import com.barbarajones.client.ui.KraveTheme;
import com.barbarajones.v2.manual.book.Icon;
import com.barbarajones.v2.manual.book.PageElement;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Turns one {@link PageElement} into pixels, and separately measures how many

 * pixels tall it will be. The two are kept as one method each, called from
 * exactly two places ({@code Paginator} for measuring, {@link ManualScreen}
 * for drawing) - both feed the same {@code width} in, so a wrap computed here
 * with {@link KraveTheme#wrap} always agrees with itself between the two
 * passes. There is no cached layout object; text this short is cheap enough
 * to re-wrap every call.
 *
 * <p>Colours and the word-wrapper are borrowed from {@link KraveTheme} on
 * purpose - this book is meant to look like it belongs to the same mod as
 * Cayden's own upgrade screen, not like a separate wiki bolted on beside it.
 */
final class ManualRenderer {

    // ---- ink -----------------------------------------------------------------
    //
    // The manual is the one surface in this mod that is LIGHT. Everything else -
    // the quest atlas, the upgrade ledger, the HUD - sits on a dark panel, so
    // KraveTheme's palette is built for pale text on darkness. This screen was
    // drawing that same pale cream (TEXT = 0xFFF2E9D8) onto parchment, which is
    // cream on cream: technically rendering, practically invisible.
    //
    // These are the parchment equivalents. Dark brown rather than pure black so
    // it reads as ink on paper rather than as a UI label, and the heading gold is
    // darkened well past INK_HEAD, which was chosen to glow on black and
    // simply disappears on a light page.
    private static final int INK       = 0xFF2A1C10;   // body text
    private static final int INK_DIM   = 0xFF6B5A44;   // captions, notes, counts
    private static final int INK_HEAD  = 0xFF7A3E12;   // chapter and section heads
    private static final int INK_RULE  = 0x807A3E12;   // hairlines under headings

    private ManualRenderer() { }

    static final int LINE_H = 9;
    private static final int ICON_SIZE = 16;
    private static final int SLOT = 18;
    private static final int GALLERY_CELL = 40;

    // ---- top-level dispatch ---------------------------------------------------

    static int measure(Font f, PageElement el, int w) {
        if (el instanceof PageElement.Heading h) {
            int lines = KraveTheme.wrap(f, h.text().toUpperCase(java.util.Locale.ROOT), w, 0).size();
            return lines * 11 + 9;
        }
        if (el instanceof PageElement.Sub s) {
            return 4 + KraveTheme.wrap(f, s.text(), w, 0).size() * 10 + 3;
        }
        if (el instanceof PageElement.Para p) {
            return KraveTheme.wrap(f, p.text(), w, 0).size() * LINE_H + 4;
        }
        if (el instanceof PageElement.Bullets b) {
            int h = 0;
            for (String line : b.lines()) {
                h += KraveTheme.wrap(f, line, w - 10, 0).size() * LINE_H;
            }
            return h + 3;
        }
        if (el instanceof PageElement.Steps s) {
            int h = 0;
            for (String line : s.lines()) {
                h += KraveTheme.wrap(f, line, w - 14, 0).size() * LINE_H;
            }
            return h + 3;
        }
        if (el instanceof PageElement.Divider) {
            return 7;
        }
        if (el instanceof PageElement.Spacer s) {
            return s.px();
        }
        if (el instanceof PageElement.HardBreak) {
            return 0;
        }
        if (el instanceof PageElement.Gallery g) {
            int perRow = Math.max(1, w / GALLERY_CELL);
            int rows = (g.icons().size() + perRow - 1) / perRow;
            return rows * (ICON_SIZE + 12) + 4;
        }
        if (el instanceof PageElement.Callout c) {
            int textW = w - 14;
            int lines = KraveTheme.wrap(f, c.text(), textW, 0).size();
            return 14 + lines * LINE_H + 8;
        }
        if (el instanceof PageElement.Table t) {
            return tableHeight(f, t, w);
        }
        if (el instanceof PageElement.CraftGrid) {
            return 3 * SLOT + 20;
        }
        if (el instanceof PageElement.FlowRecipe fr) {
            int noteLines = fr.note() == null || fr.note().isEmpty()
                    ? 0 : KraveTheme.wrap(f, fr.note(), w, 0).size();
            return 12 + ICON_SIZE + 4 + noteLines * LINE_H + 6;
        }
        return 0;
    }

    /** Draws the element and returns the item stack the mouse is currently hovering, if any - for the tooltip. */
    static ItemStack render(GuiGraphics g, Font f, PageElement el, int x, int y, int w, int mouseX, int mouseY) {
        if (el instanceof PageElement.Heading h) {
            List<String> lines = KraveTheme.wrap(f, upper(h.text()), w, 0);
            int hy = y + 2;
            for (String line : lines) {
                g.drawString(f, ChatFormatting.BOLD + line, x, hy, INK_HEAD, false);
                hy += 11;
            }
            int ruleY = hy + 1;
            g.fill(x, ruleY, x + w, ruleY + 1, KraveTheme.withAlpha(INK_HEAD, 0.55F));
            g.fill(x, ruleY + 1, x + w, ruleY + 2, 0x30000000);
            return null;
        }
        if (el instanceof PageElement.Sub s) {
            g.drawString(f, ChatFormatting.BOLD + s.text(), x, y + 3, INK, false);
            return null;
        }
        if (el instanceof PageElement.Para p) {
            int color = p.color() != 0 ? p.color() : INK;
            int yy = y;
            for (String line : KraveTheme.wrap(f, p.text(), w, 0)) {
                g.drawString(f, line, x, yy, color, false);
                yy += LINE_H;
            }
            return null;
        }
        if (el instanceof PageElement.Bullets b) {
            int yy = y;
            for (String entry : b.lines()) {
                List<String> lines = KraveTheme.wrap(f, entry, w - 10, 0);
                for (int i = 0; i < lines.size(); i++) {
                    if (i == 0) {
                        g.drawString(f, "\u2022", x, yy, INK_HEAD, false);
                    }
                    g.drawString(f, lines.get(i), x + 9, yy, INK, false);
                    yy += LINE_H;
                }
            }
            return null;
        }
        if (el instanceof PageElement.Steps st) {
            int yy = y;
            int n = 1;
            for (String entry : st.lines()) {
                List<String> lines = KraveTheme.wrap(f, entry, w - 14, 0);
                for (int i = 0; i < lines.size(); i++) {
                    if (i == 0) {
                        g.drawString(f, n + ".", x, yy, INK_HEAD, false);
                    }
                    g.drawString(f, lines.get(i), x + 13, yy, INK, false);
                    yy += LINE_H;
                }
                n++;
            }
            return null;
        }
        if (el instanceof PageElement.Divider) {
            KraveTheme.divider(g, x, y + 3, w);
            return null;
        }
        if (el instanceof PageElement.Gallery gal) {
            return renderGallery(g, f, gal, x, y, w, mouseX, mouseY);
        }
        if (el instanceof PageElement.Callout c) {
            renderCallout(g, f, c, x, y, w);
            return null;
        }
        if (el instanceof PageElement.Table t) {
            renderTable(g, f, t, x, y, w);
            return null;
        }
        if (el instanceof PageElement.CraftGrid cg) {
            return renderCraftGrid(g, f, cg, x, y, w, mouseX, mouseY);
        }
        if (el instanceof PageElement.FlowRecipe fr) {
            return renderFlow(g, f, fr, x, y, w, mouseX, mouseY);
        }
        return null;
    }

    // ---- gallery ----------------------------------------------------------

    private static ItemStack renderGallery(GuiGraphics g, Font f, PageElement.Gallery gal, int x, int y, int w,
                                           int mouseX, int mouseY) {
        int perRow = Math.max(1, w / GALLERY_CELL);
        int cellW = w / Math.max(1, Math.min(perRow, gal.icons().size()));
        int i = 0;
        ItemStack hovered = null;
        for (Icon icon : gal.icons()) {
            int col = i % perRow;
            int rowi = i / perRow;
            int cx = x + col * cellW + (cellW - ICON_SIZE) / 2;
            int cy = y + rowi * (ICON_SIZE + 12);
            boolean hover = mouseX >= cx && mouseX < cx + ICON_SIZE && mouseY >= cy && mouseY < cy + ICON_SIZE;
            ItemStack stack = icon.stack();
            if (hover) {
                g.fill(cx - 2, cy - 2, cx + ICON_SIZE + 2, cy + ICON_SIZE + 2, 0x26000000);
                hovered = stack;
            }
            g.renderItem(stack, cx, cy);
            if (!icon.label().isEmpty()) {
                String lbl = KraveTheme.trimTo(f, icon.label(), cellW - 2);
                g.drawString(f, lbl, x + col * cellW + (cellW - f.width(lbl)) / 2, cy + ICON_SIZE + 2,
                        INK_DIM, false);
            }
            i++;
        }
        return hovered;
    }

    // ---- callout ------------------------------------------------------------

    private static void renderCallout(GuiGraphics g, Font f, PageElement.Callout c, int x, int y, int w) {
        int accent = c.accent() == 0 ? INK_HEAD : c.accent();
        List<String> lines = KraveTheme.wrap(f, c.text(), w - 14, 0);
        int h = 14 + lines.size() * LINE_H + 4;
        g.fill(x, y, x + w, y + h, 0x50000000);
        g.fill(x, y, x + 2, y + h, accent);
        g.fill(x, y, x + w, y + 1, KraveTheme.withAlpha(accent, 0.5F));
        g.drawString(f, ChatFormatting.BOLD + "" + ChatFormatting.UNDERLINE + upper(c.label()),
                x + 6, y + 3, accent, false);
        int yy = y + 14;
        for (String line : lines) {
            g.drawString(f, line, x + 6, yy, INK, false);
            yy += LINE_H;
        }
    }

    // ---- table --------------------------------------------------------------

    private static int[] colXs(List<Integer> widths, int totalW) {
        int[] xs = new int[widths.size()];
        int cx = 0;
        int given = 0;
        for (int wid : widths) {
            given += wid;
        }
        float scale = given > 0 && given != totalW ? Math.min(1.0F, totalW / (float) given) : 1.0F;
        for (int i = 0; i < widths.size(); i++) {
            xs[i] = cx;
            cx += (int) (widths.get(i) * scale);
        }
        return xs;
    }

    private static int colW(List<Integer> widths, int i, int totalW) {
        int given = 0;
        for (int wid : widths) {
            given += wid;
        }
        float scale = given > 0 && given != totalW ? Math.min(1.0F, totalW / (float) given) : 1.0F;
        return (int) (widths.get(i) * scale) - 4;
    }

    private static int tableHeight(Font f, PageElement.Table t, int w) {
        int h = LINE_H + 3;
        for (List<String> row : t.rows()) {
            int rowLines = 1;
            for (int c = 0; c < row.size() && c < t.widths().size(); c++) {
                rowLines = Math.max(rowLines, KraveTheme.wrap(f, row.get(c), colW(t.widths(), c, w), 0).size());
            }
            h += rowLines * LINE_H + 3;
        }
        return h + 4;
    }

    private static void renderTable(GuiGraphics g, Font f, PageElement.Table t, int x, int y, int w) {
        int[] xs = colXs(t.widths(), w);
        int yy = y;
        for (int c = 0; c < t.headers().size() && c < xs.length; c++) {
            g.drawString(f, ChatFormatting.BOLD + upper(t.headers().get(c)), x + xs[c], yy, INK_HEAD, false);
        }
        yy += LINE_H + 1;
        g.fill(x, yy, x + w, yy + 1, KraveTheme.withAlpha(INK_HEAD, 0.35F));
        yy += 3;
        boolean stripe = false;
        for (List<String> row : t.rows()) {
            int rowLines = 1;
            List<List<String>> wrapped = new java.util.ArrayList<>();
            for (int c = 0; c < row.size() && c < t.widths().size(); c++) {
                List<String> wl = KraveTheme.wrap(f, row.get(c), colW(t.widths(), c, w), 0);
                wrapped.add(wl);
                rowLines = Math.max(rowLines, wl.size());
            }
            int rowH = rowLines * LINE_H + 3;
            if (stripe) {
                g.fill(x, yy - 1, x + w, yy + rowH - 3, 0x14000000);
            }
            stripe = !stripe;
            for (int c = 0; c < wrapped.size(); c++) {
                List<String> wl = wrapped.get(c);
                for (int li = 0; li < wl.size(); li++) {
                    g.drawString(f, wl.get(li), x + xs[c], yy + li * LINE_H,
                            c == 0 ? INK : INK, false);
                }
            }
            yy += rowH;
        }
    }

    // ---- crafting grid --------------------------------------------------------

    private static void slot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + SLOT, y + SLOT, 0xFF241830);
        g.fill(x, y, x + SLOT, y + 1, 0x60000000);
        g.fill(x, y, x + 1, y + SLOT, 0x60000000);
        g.fill(x + SLOT - 1, y, x + SLOT, y + SLOT, 0x30FFFFFF);
        g.fill(x, y + SLOT - 1, x + SLOT, y + SLOT, 0x30FFFFFF);
    }

    private static ItemStack renderCraftGrid(GuiGraphics g, Font f, PageElement.CraftGrid cg, int x, int y, int w,
                                             int mouseX, int mouseY) {
        int gridX = x;
        int gridY = y + 4;
        ItemStack hovered = null;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int cx = gridX + col * SLOT;
                int cy = gridY + row * SLOT;
                slot(g, cx, cy);
                ItemStack st = cg.cells().get(row * 3 + col).stack();
                if (!st.isEmpty()) {
                    g.renderItem(st, cx + 1, cy + 1);
                    if (hoverSlot(mouseX, mouseY, cx, cy)) {
                        hovered = st;
                    }
                }
            }
        }
        int arrowX = gridX + 3 * SLOT + 8;
        int arrowY = gridY + SLOT + 1;
        KraveTheme.icon(g, KraveTheme.ICON_ARROW, arrowX, arrowY);
        int outX = arrowX + 20;
        int outY = gridY + SLOT - 1;
        slot(g, outX, outY);
        ItemStack out = cg.output().stack().copy();
        if (cg.outputCount() > 1) {
            out.setCount(cg.outputCount());
        }
        g.renderItem(out, outX + 1, outY + 1);
        g.renderItemDecorations(f, out, outX + 1, outY + 1);
        if (hoverSlot(mouseX, mouseY, outX, outY)) {
            hovered = out;
        }

        String caption = cg.note() != null && !cg.note().isEmpty()
                ? cg.note() : (cg.shapeless() ? "SHAPELESS" : "SHAPED");
        g.drawString(f, ChatFormatting.ITALIC + caption,
                x, gridY + 3 * SLOT + 4, INK_DIM, false);
        return hovered;
    }

    private static boolean hoverSlot(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT;
    }

    // ---- flow recipe --------------------------------------------------------

    private static ItemStack renderFlow(GuiGraphics g, Font f, PageElement.FlowRecipe fr, int x, int y, int w,
                                        int mouseX, int mouseY) {
        g.drawString(f, ChatFormatting.BOLD + upper(fr.verb()), x, y, INK_HEAD, false);
        int rowY = y + 11;
        int cx = x;
        ItemStack hovered = null;
        List<Icon> inputs = fr.inputs();
        for (int i = 0; i < inputs.size(); i++) {
            ItemStack st = inputs.get(i).stack();
            g.renderItem(st, cx, rowY);
            if (hoverSlot(mouseX, mouseY, cx, rowY)) {
                hovered = st;
            }
            cx += ICON_SIZE + 2;
            if (i < inputs.size() - 1) {
                g.drawString(f, "+", cx, rowY + 4, INK_DIM, false);
                cx += 9;
            }
        }
        cx += 4;
        KraveTheme.icon(g, KraveTheme.ICON_ARROW, cx, rowY);
        cx += 20;
        ItemStack out = fr.output().stack().copy();
        if (fr.outputCount() > 1) {
            out.setCount(fr.outputCount());
        }
        g.renderItem(out, cx, rowY);
        g.renderItemDecorations(f, out, cx, rowY);
        if (hoverSlot(mouseX, mouseY, cx, rowY)) {
            hovered = out;
        }

        if (fr.note() != null && !fr.note().isEmpty()) {
            int yy = rowY + ICON_SIZE + 3;
            for (String line : KraveTheme.wrap(f, fr.note(), w, 0)) {
                g.drawString(f, line, x, yy, INK_DIM, false);
                yy += LINE_H;
            }
        }
        return hovered;
    }

    private static String upper(String s) {
        return s == null ? "" : s.toUpperCase(java.util.Locale.ROOT);
    }
}
