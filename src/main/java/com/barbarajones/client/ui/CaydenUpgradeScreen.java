package com.barbarajones.client.ui;

import com.barbarajones.entity.CaydenCobb;
import com.barbarajones.net.PacketCaydenUpgrade;
import com.barbarajones.progression.AscensionLadder;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attributes;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * CAYDEN'S ASCENSION LEDGER - the upgrade shop for his transformation ladder.
 *
 * <p>Opened by right-clicking him with the Ascension Ledger or the Cayden
 * Compass, or with the upgrade keybind (U by default). Two tabs: FORMS is the
 * ladder itself, one row per rung with its price, its prerequisites and the one
 * sentence that says what it actually does; CAYDEN is his live sheet - what he
 * is standing in right now, what it is doing to his numbers, and where ki comes
 * from.
 *
 * <p>Every number on this screen is read straight off the entity's synched data,
 * so it tracks live while it is open and needs no packet of its own. The single
 * outbound packet is the buy, and the server re-checks the entire purchase - see
 * {@link CaydenCobb#tryUnlock(int, net.minecraft.world.entity.player.Player)}.
 */
public class CaydenUpgradeScreen extends KraveScreen {

    private static final int TAB_FORMS = 0;
    private static final int TAB_SHEET = 1;

    /** Height of one ladder row, before its description wraps. */
    private static final int ROW_HEAD = 22;
    private static final int FOOTER = 26;

    private final CaydenCobb cayden;

    /** The rung the footer button will buy. Starts on the cheapest locked one. */
    private int selected = AscensionLadder.SSJ;
    /** Set during render, consumed by mouseClicked - see the note there. */
    private int hoverRung = -1;

    @Nullable
    private KraveButton buyButton;

    public CaydenUpgradeScreen(CaydenCobb cayden) {
        super(Component.literal("Cayden's Ascension"));
        this.cayden = cayden;
        // tabs change the panel layout, so they have to exist before init() runs
        setTabs("Forms", "Cayden");
        this.selected = firstLocked();
    }

    /** Opens the screen, guarding against a stale entity reference. */
    public static void open(CaydenCobb cayden) {
        if (cayden != null && cayden.isAlive()) {
            Minecraft.getInstance().setScreen(new CaydenUpgradeScreen(cayden));
        }
    }

    @Override
    protected int preferredWidth() {
        return 360;
    }

    @Override
    protected int preferredHeight() {
        return 238;
    }

    @Override
    protected int footerHeight() {
        return FOOTER;
    }

    @Override
    protected void initContent() {
        int y = this.panelY + this.panelH - KraveTheme.BORDER - 23;
        int left = this.panelX + KraveTheme.BORDER + KraveTheme.PAD;
        int right = this.panelX + this.panelW - KraveTheme.BORDER - KraveTheme.PAD;
        int closeW = 60;
        int buyW = Math.max(120, right - left - closeW - KraveTheme.PAD_S);

        this.buyButton = addRenderableWidget(new KraveButton(left, y, buyW, 20,
                Component.literal("Unlock"), this::buySelected).accent(KraveTheme.GOLD));
        addRenderableWidget(new KraveButton(right - closeW, y, closeW, 20,
                Component.literal("Close"), this::onClose));
        refreshFooter();
    }

    // ---- live state ---------------------------------------------------------

    private int mask() {
        return this.cayden.getUnlockMask();
    }

    private int ki() {
        return this.cayden.getKi();
    }

    private int fed() {
        return this.cayden.getKraveFed();
    }

    /** The lowest rung he has not been taught, or the top of the ladder. */
    private int firstLocked() {
        for (int t = AscensionLadder.SSJ; t <= AscensionLadder.MAX; t++) {
            if (!AscensionLadder.unlocked(mask(), t)) {
                return t;
            }
        }
        return AscensionLadder.MAX;
    }

    @Override
    public void tick() {
        // The entity can die, despawn or be dragged through a portal while this
        // is open, and a screen drawn from a stale reference reads as a bug.
        if (!this.cayden.isAlive()) {
            onClose();
            return;
        }
        refreshFooter();
    }

    private void refreshFooter() {
        if (this.buyButton == null) {
            return;
        }
        int tier = this.selected;
        AscensionLadder.Rung rung = AscensionLadder.rung(tier);
        boolean owned = AscensionLadder.unlocked(mask(), tier);
        String label = owned
                ? rung.name() + " - already learned"
                : "Unlock " + rung.name() + "  -  " + rung.kiCost() + " Ki";
        this.buyButton.setMessage(Component.literal(
                KraveTheme.trimTo(this.font, label, this.buyButton.getWidth() - 10)));
        this.buyButton.active =
                AscensionLadder.blocker(tier, mask(), ki(), fed()) == null;
        this.subtitle = ki() + " Ki  -  "
                + AscensionLadder.countUnlocked(mask()) + "/" + AscensionLadder.MAX + " forms";
    }

    private void buySelected() {
        PacketCaydenUpgrade.buy(this.cayden.getId(), this.selected);
    }

    // ---- input --------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // hoverRung is computed during renderContent, which is the only place the
        // scroll offset is applied to the mouse. Testing the raw pointer against
        // the viewport here stops a click in the footer selecting a row that
        // happens to sit at the same content-space y.
        boolean inBody = mouseX >= this.bodyX && mouseX < this.bodyX + this.bodyW + 6
                && mouseY >= this.bodyY && mouseY < this.bodyY + this.bodyH;
        if (button == 0 && inBody && activeTab() == TAB_FORMS && this.hoverRung > 0) {
            select(this.hoverRung);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void select(int tier) {
        if (tier == this.selected) {
            return;
        }
        this.selected = tier;
        refreshFooter();
    }

    @Override
    protected void onTabChanged(int tab) {
        refreshFooter();
    }

    // ---- render -------------------------------------------------------------

    @Override
    protected void renderContent(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        this.hoverRung = -1;
        int y = activeTab() == TAB_SHEET
                ? renderSheet(gfx, this.bodyY)
                : renderLadder(gfx, this.bodyY, mouseX, mouseY);
        setContentHeight(y - this.bodyY);
    }

    // ---- FORMS --------------------------------------------------------------

    private int renderLadder(GuiGraphics gfx, int y, int mouseX, int mouseY) {
        int x = this.bodyX;
        int w = this.bodyW;

        int current = this.cayden.getTier();
        y = KraveTheme.sectionHeader(gfx, this.font, x, y, w,
                "Standing in: " + AscensionLadder.nameOf(current));
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Ki banked",
                String.valueOf(ki()), KraveTheme.GOLD);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Krave eaten",
                fed() + " boxes", KraveTheme.TEXT);
        y += KraveTheme.PAD_S;

        for (int tier = AscensionLadder.SSJ; tier <= AscensionLadder.MAX; tier++) {
            y = renderRung(gfx, x, y, w, tier, mouseX, mouseY, current);
        }
        return y + KraveTheme.PAD;
    }

    private int renderRung(GuiGraphics gfx, int x, int y, int w, int tier,
                           int mouseX, int mouseY, int current) {
        AscensionLadder.Rung rung = AscensionLadder.rung(tier);
        boolean owned = AscensionLadder.unlocked(mask(), tier);
        boolean active = tier == current;
        String blocker = AscensionLadder.blocker(tier, mask(), ki(), fed());
        boolean buyable = blocker == null;

        // Measure the whole row first. The selection plate is painted behind the
        // text, so its height has to be known before a single string is drawn -
        // and a locked rung is taller, because it still owes the player a price.
        int textW = w - 24;
        List<String> edge = KraveTheme.wrap(this.font, rung.edge(), textW, 0);
        int rowH = ROW_HEAD + edge.size() * KraveTheme.LINE_H
                + (owned ? 0 : KraveTheme.LINE_H * 2 + 8) + 6;

        boolean hovered = rowHovered(mouseX, mouseY, y, rowH);
        if (hovered) {
            this.hoverRung = tier;
        }
        boolean chosen = tier == this.selected;

        if (chosen) {
            gfx.fill(x - 3, y - 2, x + w + 3, y + rowH - 3, KraveTheme.withAlpha(rung.color(), 0.16F));
            gfx.fill(x - 3, y - 2, x - 1, y + rowH - 3, rung.color());
        } else if (hovered) {
            gfx.fill(x - 3, y - 2, x + w + 3, y + rowH - 3, 0x22FFFFFF);
        }

        // the rung marker: a lock for anything he cannot reach, a check for what
        // he owns, and a breathing star for the form he is actually standing in
        int icon = owned ? (active ? KraveTheme.ICON_STAR : KraveTheme.ICON_CHECK)
                : KraveTheme.ICON_LOCK;
        KraveTheme.icon(gfx, icon, x, y, active ? 0.65F + KraveTheme.breathe() * 0.35F : 1.0F);

        int nameColor = owned ? rung.color() : KraveTheme.TEXT_LOCKED;
        String name = (active ? ChatFormatting.BOLD.toString() : "")
                + rung.name().toUpperCase(Locale.ROOT);
        gfx.drawString(this.font, name, x + 20, y + 1, nameColor, owned);

        String right = owned
                ? (active ? "ACTIVE" : "LEARNED")
                : rung.kiCost() + " Ki";
        int rightColor = owned ? (active ? KraveTheme.MILK : KraveTheme.GRASS)
                : (buyable ? KraveTheme.GOLD : KraveTheme.DANGER);
        gfx.drawString(this.font, right, x + w - this.font.width(right), y + 1, rightColor, false);

        gfx.drawString(this.font, rung.tagline(), x + 20, y + 11,
                owned ? KraveTheme.TEXT_DIM : KraveTheme.TEXT_LOCKED, false);

        int ty = y + ROW_HEAD;
        for (String line : edge) {
            gfx.drawString(this.font, line, x + 20, ty,
                    owned ? KraveTheme.TEXT : KraveTheme.TEXT_LOCKED, false);
            ty += KraveTheme.LINE_H;
        }

        // The price, the verdict and the ki bar only exist while it is still a
        // cost. Once he knows the form the row collapses to what it does.
        if (!owned) {
            String cost = "Needs " + rung.fedRequired() + " boxes eaten"
                    + (tier > AscensionLadder.SSJ
                        ? " and " + AscensionLadder.nameOf(tier - 1) : "");
            gfx.drawString(this.font, KraveTheme.trimTo(this.font, cost, textW),
                    x + 20, ty, KraveTheme.TEXT_LOCKED, false);
            ty += KraveTheme.LINE_H;

            gfx.drawString(this.font,
                    buyable ? "Ready. He can learn this now." : blocker,
                    x + 20, ty, buyable ? KraveTheme.GRASS : KraveTheme.DANGER, false);
            ty += KraveTheme.LINE_H;

            float frac = rung.kiCost() <= 0 ? 1.0F : ki() / (float) rung.kiCost();
            KraveTheme.progressBar(gfx, x + 20, ty + 1, textW, 4, frac,
                    buyable ? KraveTheme.GRASS : rung.color());
            ty += 8;
        }

        KraveTheme.divider(gfx, x, y + rowH - 5, w);
        return y + rowH;
    }

    // ---- CAYDEN -------------------------------------------------------------

    private int renderSheet(GuiGraphics gfx, int y) {
        int x = this.bodyX;
        int w = this.bodyW;
        int tier = this.cayden.getTier();
        AscensionLadder.Rung rung = AscensionLadder.rung(tier);

        y = KraveTheme.sectionHeader(gfx, this.font, x, y, w, "Right now");
        gfx.drawString(this.font, ChatFormatting.BOLD + rung.name().toUpperCase(Locale.ROOT),
                x, y, rung.color(), true);
        y += 11;
        y = KraveTheme.textBlock(gfx, this.font, x, y, w, rung.tagline(), KraveTheme.TEXT_DIM);
        y += KraveTheme.PAD_S;

        float hp = this.cayden.getHealth();
        float maxHp = Math.max(1.0F, this.cayden.getMaxHealth());
        KraveTheme.progressBar(gfx, this.font, x, y, w, KraveTheme.BAR_H, hp / maxHp,
                hp / maxHp > 0.4F ? KraveTheme.GRASS : KraveTheme.DANGER,
                (int) hp + " / " + (int) maxHp);
        y += KraveTheme.BAR_H + KraveTheme.PAD_S + 2;

        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Attack damage",
                fmt(this.cayden.getAttributeValue(Attributes.ATTACK_DAMAGE)), KraveTheme.GOLD);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Movement speed",
                fmt(this.cayden.getAttributeValue(Attributes.MOVEMENT_SPEED) * 10.0D),
                KraveTheme.GOLD);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Damage taken",
                (int) Math.round(rung.damageTaken() * 100.0D) + "%",
                rung.damageTaken() < 1.0D ? KraveTheme.GRASS : KraveTheme.TEXT);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Hits refused outright",
                rung.dodgePercent() + "%",
                rung.dodgePercent() > 0 ? KraveTheme.GRASS : KraveTheme.TEXT_DIM);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Krave eaten",
                fed() + " boxes", KraveTheme.TEXT);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Home",
                this.cayden.isHoused() ? "claimed" : "homeless",
                this.cayden.isHoused() ? KraveTheme.GRASS : KraveTheme.DANGER);
        y += KraveTheme.PAD;

        y = KraveTheme.sectionHeader(gfx, this.font, x, y, w, "Where Ki comes from");
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "A box of Krave",
                "+" + AscensionLadder.KI_PER_KRAVE, KraveTheme.GOLD);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "A Golden Krave",
                "+" + AscensionLadder.KI_PER_GOLDEN_KRAVE, KraveTheme.GOLD);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "Anything ordinary he kills",
                "+" + AscensionLadder.KI_PER_MOB, KraveTheme.GOLD);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "A Wither",
                "+" + AscensionLadder.kiForKill(AscensionLadder.SSJ), KraveTheme.GOLD);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "A Warden, or the Manager",
                "+" + AscensionLadder.kiForKill(AscensionLadder.SSJ2), KraveTheme.GOLD);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "The Ender Dragon",
                "+" + AscensionLadder.kiForKill(AscensionLadder.SSJ3), KraveTheme.GOLD);
        y = KraveTheme.keyValue(gfx, this.font, x, y, w, "The Krave Monster",
                "+" + AscensionLadder.kiForKill(AscensionLadder.GOD) + " and up",
                KraveTheme.GOLD);
        y += KraveTheme.PAD;

        y = KraveTheme.sectionHeader(gfx, this.font, x, y, w, "How the ladder is climbed");
        y = KraveTheme.textBlock(gfx, this.font, x, y, w,
                "He only reaches for a form when the fight demands it. A Wither is worth "
                        + "Super Saiyan, a Warden is worth the second, the Ender Dragon is worth "
                        + "the third, and each incarnation of the Krave Monster is worth more "
                        + "than the last.",
                KraveTheme.TEXT);
        y += KraveTheme.PAD_S;
        y = KraveTheme.textBlock(gfx, this.font, x, y, w,
                "The Krave Kosmos is divine ground: nothing is fought out there below Super "
                        + "Saiyan God, and whatever already demanded God demands Blue instead. "
                        + "That dimension is the only place the back half of this ladder is ever "
                        + "asked for.",
                KraveTheme.TEXT);
        y += KraveTheme.PAD_S;
        y = KraveTheme.textBlock(gfx, this.font, x, y, w,
                "A form he has not been taught is not available to him, whatever he is up "
                        + "against. Buy it before the fight, not during it.",
                KraveTheme.DANGER);
        return y + KraveTheme.PAD;
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
