package com.barbarajones.v2.village.client;

import com.barbarajones.client.ui.KraveTheme;
import com.barbarajones.v2.village.KraveProfession;
import com.barbarajones.v2.village.VillageOffer;
import com.barbarajones.v2.village.VillageTrades;
import com.barbarajones.v2.village.menu.KraveTradeMenu;
import com.barbarajones.v2.village.net.PacketSelectOffer;
import com.barbarajones.v2.village.net.VillageNetwork;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The trading screen.
 *
 * <p>Drawn entirely from {@link KraveTheme} primitives - no GUI atlas of its own.
 * That is deliberate: a bespoke 256x256 background would have to be regenerated
 * every time a row moved, and the theme already owns the mod's cereal-box look, so
 * this screen is guaranteed to match the codex and the quest board rather than
 * merely resembling them.
 *
 * <h2>What it has to show, and why</h2>
 * The feeding loop is the heart of this module and it is invisible unless the
 * screen says so out loud. So the right-hand column is given over to it: the
 * profession, the trade level as five pips, an XP bar with the exact number of
 * Krave still needed, and a running count of how much cereal this villager has
 * eaten. Under the offer list sits a locked row naming the next level and how many
 * new trades come with it - the player should never have to guess whether feeding
 * is doing anything.
 *
 * <h2>Selection</h2>
 * Clicking a row highlights it immediately and sends the index to the server, which
 * is the only side that may act on it. The optimistic local highlight is corrected
 * on the next container sync if the server disagreed.
 */
public class KraveTradeScreen extends AbstractContainerScreen<KraveTradeMenu> {

    private static final int LIST_X = 9;
    private static final int LIST_Y = 30;
    private static final int LIST_W = 142;
    private static final int LIST_H = 112;
    private static final int ROW_H = 22;
    private static final int VISIBLE_ROWS = LIST_H / ROW_H;

    private static final int RIGHT_X = 158;
    private static final int RIGHT_W = 109;

    private int scrollRow;

    public KraveTradeScreen(KraveTradeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 276;
        this.imageHeight = 232;
    }

    // ---- background ----------------------------------------------------------

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        KraveTheme.panel(gfx, x, y, this.imageWidth, this.imageHeight);
        KraveTheme.titleBar(gfx, this.font, x + KraveTheme.BORDER, y + KraveTheme.BORDER,
                this.imageWidth - KraveTheme.BORDER * 2,
                this.title.getString(), levelBadge());

        drawOfferList(gfx, x, y, mouseX, mouseY);
        drawTradePanel(gfx, x, y);
        drawInventoryWells(gfx, x, y);
    }

    private String levelBadge() {
        return "LV " + this.menu.getTradeLevel() + "/" + VillageTrades.MAX_LEVEL;
    }

    private void drawOfferList(GuiGraphics gfx, int x, int y, int mouseX, int mouseY) {
        int listX = x + LIST_X;
        int listY = y + LIST_Y;
        KraveTheme.well(gfx, listX, listY, LIST_W, LIST_H);

        List<VillageOffer> offers = this.menu.getOffers();
        int rows = offers.size() + 1;                      // +1 for the locked teaser
        clampScroll(rows);

        gfx.enableScissor(listX, listY, listX + LIST_W, listY + LIST_H);
        for (int visible = 0; visible < VISIBLE_ROWS + 1; visible++) {
            int index = this.scrollRow + visible;
            if (index >= rows) {
                break;
            }
            int rowY = listY + visible * ROW_H;
            if (index < offers.size()) {
                drawOfferRow(gfx, listX, rowY, offers.get(index), index, mouseX, mouseY);
            } else {
                drawLockedRow(gfx, listX, rowY);
            }
        }
        gfx.disableScissor();

        KraveTheme.scrollbar(gfx, listX + LIST_W - 4, listY, LIST_H,
                rows * ROW_H, LIST_H, this.scrollRow * ROW_H);

        if (offers.isEmpty()) {
            String empty = Component.translatable("village.barbarajones.no_offers").getString();
            gfx.drawString(this.font, empty,
                    listX + (LIST_W - this.font.width(empty)) / 2, listY + LIST_H / 2 - 4,
                    KraveTheme.TEXT_DIM, false);
        }
    }

    private void drawOfferRow(GuiGraphics gfx, int rowX, int rowY, VillageOffer offer,
                              int index, int mouseX, int mouseY) {
        boolean selected = index == this.menu.getSelected();
        boolean hovered = mouseX >= rowX && mouseX < rowX + LIST_W - 5
                && mouseY >= rowY && mouseY < rowY + ROW_H;
        boolean sold = offer.isOutOfStock();

        int background = selected ? 0x80C81E24 : (hovered ? 0x40F4EDDD : 0x30000000);
        gfx.fill(rowX + 1, rowY + 1, rowX + LIST_W - 5, rowY + ROW_H - 1, background);
        if (selected) {
            gfx.fill(rowX + 1, rowY + 1, rowX + 3, rowY + ROW_H - 1, KraveTheme.GOLD);
        }

        ItemStack costA = offer.currentCostA();
        ItemStack costB = offer.costB();
        ItemStack result = offer.result();

        gfx.renderItem(costA, rowX + 5, rowY + 3);
        gfx.renderItemDecorations(this.font, costA, rowX + 5, rowY + 3);
        if (!costB.isEmpty()) {
            gfx.renderItem(costB, rowX + 25, rowY + 3);
            gfx.renderItemDecorations(this.font, costB, rowX + 25, rowY + 3);
        }

        // The arrow. Greyed when the trade is sold out, which is the fastest way to
        // read a whole list at a glance.
        int arrowColor = sold ? KraveTheme.TEXT_LOCKED : KraveTheme.MILK;
        gfx.drawString(this.font, ">", rowX + 48, rowY + 7, arrowColor, false);

        gfx.renderItem(result, rowX + 60, rowY + 3);
        gfx.renderItemDecorations(this.font, result, rowX + 60, rowY + 3);

        // Stock, as a small chocolate bar. Full is fine; empty is the story.
        float stock = 1.0F - offer.uses() / (float) Math.max(1, offer.maxUses());
        KraveTheme.progressBar(gfx, rowX + 84, rowY + 8, 44, 5, stock,
                sold ? KraveTheme.DANGER : KraveTheme.GRASS);

        if (offer.isDiscounted()) {
            gfx.drawString(this.font, ChatFormatting.GREEN + "-", rowX + 5, rowY + 12,
                    KraveTheme.GRASS, false);
        }
    }

    /**
     * The teaser row: what the next level unlocks. Without it the player has no
     * on-screen reason to believe feeding a villager does anything at all.
     */
    private void drawLockedRow(GuiGraphics gfx, int rowX, int rowY) {
        gfx.fill(rowX + 1, rowY + 1, rowX + LIST_W - 5, rowY + ROW_H - 1, 0x40000000);
        int level = this.menu.getTradeLevel();
        String text = level >= VillageTrades.MAX_LEVEL
                ? Component.translatable("village.barbarajones.trades_maxed").getString()
                : Component.translatable("village.barbarajones.trades_locked", level + 1).getString();
        KraveTheme.icon(gfx, KraveTheme.ICON_LOCK, rowX + 5, rowY + 3, 0.7F);
        gfx.drawString(this.font, text, rowX + 24, rowY + 7, KraveTheme.TEXT_LOCKED, false);
    }

    private void drawTradePanel(GuiGraphics gfx, int x, int y) {
        int px = x + RIGHT_X;
        int py = y + LIST_Y;

        KraveProfession job = this.menu.getProfession();
        gfx.drawString(this.font, ChatFormatting.BOLD + job.displayName().getString(),
                px, py, 0xFF000000 | job.accentColor(), false);
        gfx.drawString(this.font, job.tagline().getString(), px, py + 10,
                KraveTheme.TEXT_DIM, false);

        // Level pips. Five rungs, so five pips - the same read the codex uses.
        KraveTheme.pips(gfx, px, py + 22, VillageTrades.MAX_LEVEL, this.menu.getTradeLevel(),
                KraveTheme.GOLD, KraveTheme.CHOCOLATE);

        // The three trade slots sit at menu coordinates 172/198/242, y 66.
        KraveTheme.well(gfx, x + 171, y + 65, 18, 18);
        KraveTheme.well(gfx, x + 197, y + 65, 18, 18);
        KraveTheme.well(gfx, x + 241, y + 65, 18, 18);
        gfx.drawString(this.font, ">", x + 224, y + 70, KraveTheme.MILK, false);

        // XP toward the next level, in Krave.
        int level = this.menu.getTradeLevel();
        int xp = this.menu.getTradeXp();
        int floor = VillageTrades.xpForLevel(level);
        int ceiling = Math.max(floor + 1, this.menu.getXpForNextLevel());
        float progress = level >= VillageTrades.MAX_LEVEL
                ? 1.0F : (xp - floor) / (float) (ceiling - floor);

        String label = level >= VillageTrades.MAX_LEVEL
                ? Component.translatable("village.barbarajones.xp_maxed").getString()
                : Component.translatable("village.barbarajones.xp_needed",
                        Math.max(0, ceiling - xp)).getString();
        KraveTheme.progressBar(gfx, this.font, px, y + 92, RIGHT_W - 4, 9,
                progress, KraveTheme.PURPLE_LIGHT, label);

        gfx.drawString(this.font,
                Component.translatable("village.barbarajones.krave_fed", this.menu.getKraveFed())
                        .getString(),
                px, y + 106, KraveTheme.TEXT_DIM, false);
        gfx.drawString(this.font,
                Component.translatable("village.barbarajones.feed_hint").getString(),
                px, y + 118, KraveTheme.TEXT_LOCKED, false);
    }

    private void drawInventoryWells(GuiGraphics gfx, int x, int y) {
        KraveTheme.divider(gfx, x + 9, y + 144, this.imageWidth - 18);
        gfx.drawString(this.font, this.playerInventoryTitle.getString(),
                x + 57, y + 139, KraveTheme.TEXT_DIM, false);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                KraveTheme.well(gfx, x + 56 + col * 18, y + 149 + row * 18, 18, 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            KraveTheme.well(gfx, x + 56 + col * 18, y + 209, 18, 18);
        }
    }

    // ---- foreground ----------------------------------------------------------

    /** Suppressed: every label this screen wants is drawn in {@link #renderBg}. */
    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // intentionally empty
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        renderBackground(gfx);
        super.render(gfx, mouseX, mouseY, partialTick);
        renderOfferTooltip(gfx, mouseX, mouseY);
        renderTooltip(gfx, mouseX, mouseY);
    }

    private void renderOfferTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
        int index = rowAt(mouseX, mouseY);
        List<VillageOffer> offers = this.menu.getOffers();
        if (index < 0 || index >= offers.size()) {
            return;
        }
        VillageOffer offer = offers.get(index);
        List<Component> lines = new ArrayList<>();
        lines.add(offer.result().getHoverName().copy().withStyle(ChatFormatting.WHITE));
        lines.add(Component.translatable("village.barbarajones.tooltip_cost",
                offer.currentCostA().getHoverName(), offer.currentCostA().getCount())
                .withStyle(ChatFormatting.GRAY));
        if (!offer.costB().isEmpty()) {
            lines.add(Component.translatable("village.barbarajones.tooltip_cost",
                    offer.costB().getHoverName(), offer.costB().getCount())
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("village.barbarajones.tooltip_stock",
                Math.max(0, offer.maxUses() - offer.uses()), offer.maxUses())
                .withStyle(offer.isOutOfStock() ? ChatFormatting.RED : ChatFormatting.GREEN));
        if (offer.isDiscounted()) {
            lines.add(Component.translatable("village.barbarajones.tooltip_discount")
                    .withStyle(ChatFormatting.GREEN));
        }
        lines.add(Component.translatable("village.barbarajones.tooltip_xp", offer.xpReward())
                .withStyle(ChatFormatting.DARK_PURPLE));
        gfx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    // ---- input ---------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = rowAt((int) mouseX, (int) mouseY);
        if (index >= 0 && index < this.menu.getOffers().size()) {
            // Highlight now, confirm on the round trip. The server is still the only
            // side that may decide which offer is selected.
            this.menu.selectOffer(index);
            VillageNetwork.sendToServer(new PacketSelectOffer(this.menu.containerId, index));
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.playSound(SoundEvents.VILLAGER_TRADE, 0.4F, 1.3F);
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (inList((int) mouseX, (int) mouseY)) {
            int rows = this.menu.getOffers().size() + 1;
            this.scrollRow -= (int) Math.signum(delta);
            clampScroll(rows);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void clampScroll(int rows) {
        int max = Math.max(0, rows - VISIBLE_ROWS);
        this.scrollRow = Math.max(0, Math.min(this.scrollRow, max));
    }

    private boolean inList(int mouseX, int mouseY) {
        int listX = this.leftPos + LIST_X;
        int listY = this.topPos + LIST_Y;
        return mouseX >= listX && mouseX < listX + LIST_W
                && mouseY >= listY && mouseY < listY + LIST_H;
    }

    /** Offer index under the cursor, or -1. */
    private int rowAt(int mouseX, int mouseY) {
        if (!inList(mouseX, mouseY)) {
            return -1;
        }
        int listY = this.topPos + LIST_Y;
        return this.scrollRow + (mouseY - listY) / ROW_H;
    }
}
