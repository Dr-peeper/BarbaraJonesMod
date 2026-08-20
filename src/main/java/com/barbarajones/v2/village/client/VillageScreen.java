package com.barbarajones.v2.village.client;

import com.barbarajones.client.ui.KraveScreen;
import com.barbarajones.client.ui.KraveTheme;
import com.barbarajones.v2.village.KraveVillage;
import com.barbarajones.v2.village.VillageTier;
import com.barbarajones.v2.village.net.PacketVillageStatus;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * The full settlement read-out: what the village is, what it produces, what it
 * would take to move up a rung, and whether the Krave portal is open yet.
 *
 * <p>Opened with the Village Atlas item or the village key. Everything on it comes
 * from the last {@link PacketVillageStatus}; there is no client-side village state
 * to be wrong about, and if the status has gone stale the screen says so rather
 * than showing numbers from a settlement the player has walked out of.
 *
 * <p>The tier block is the important half. A player who cannot see <em>why</em>
 * their village will not advance will assume the system is broken, so every unmet
 * requirement is listed with its current value against its target, in red, rather
 * than being summarised as "not yet".
 */
public class VillageScreen extends KraveScreen {

    public VillageScreen() {
        super(Component.translatable("village.barbarajones.screen.title"));
    }

    @Override
    protected int preferredWidth() {
        return 320;
    }

    @Override
    protected int preferredHeight() {
        return 232;
    }

    @Override
    protected void renderContent(GuiGraphics gfx, int mouseX, int mouseY, float partial) {
        PacketVillageStatus status = VillageClientState.status();
        int x = this.bodyX;
        int y = this.bodyY;
        int w = this.bodyW;

        if (status == null || !status.inVillage) {
            this.subtitle = "";
            drawNoVillage(gfx, x, y, w);
            return;
        }

        VillageTier tier = VillageTier.byIndex(status.tier);
        this.subtitle = tier.displayName().getString();

        y = drawHeadline(gfx, x, y, w, status, tier);
        y = drawStats(gfx, x, y, w, status, tier);
        y = drawNextTier(gfx, x, y, w, status, tier);
        y = drawPortalGate(gfx, x, y, w, status);

        setContentHeight(y - this.bodyY + 8);
    }

    private void drawNoVillage(GuiGraphics gfx, int x, int y, int w) {
        y = KraveTheme.sectionHeader(gfx, this.font, x, y, w,
                Component.translatable("village.barbarajones.screen.none_header").getString());
        y = KraveTheme.textBlock(gfx, this.font, x, y, w,
                Component.translatable("village.barbarajones.screen.none_body").getString(),
                KraveTheme.TEXT_DIM);
        setContentHeight(y - this.bodyY + 8);
    }

    private int drawHeadline(GuiGraphics gfx, int x, int y, int w,
                             PacketVillageStatus status, VillageTier tier) {
        gfx.drawString(this.font, ChatFormatting.BOLD + status.name, x, y, KraveTheme.MILK, false);
        y += 11;
        gfx.drawString(this.font, tier.displayName().getString(), x, y, KraveTheme.GOLD, false);
        y += 11;
        gfx.drawString(this.font,
                Component.translatable("village.barbarajones.screen.origin",
                        status.origin.getX(), status.origin.getY(), status.origin.getZ()).getString(),
                x, y, KraveTheme.TEXT_DIM, false);
        y += 14;
        KraveTheme.divider(gfx, x, y, w);
        return y + 6;
    }

    private int drawStats(GuiGraphics gfx, int x, int y, int w,
                          PacketVillageStatus status, VillageTier tier) {
        y = KraveTheme.sectionHeader(gfx, this.font, x, y, w,
                Component.translatable("village.barbarajones.screen.state").getString());

        y = KraveTheme.keyValue(gfx, this.font, x, y, w,
                Component.translatable("village.barbarajones.stat.population").getString(),
                status.population + " / " + status.populationCap,
                status.population >= status.populationCap ? KraveTheme.GOLD : KraveTheme.MILK);

        y = KraveTheme.keyValue(gfx, this.font, x, y, w,
                Component.translatable("village.barbarajones.stat.buildings").getString(),
                String.valueOf(status.buildings), KraveTheme.MILK);

        y = KraveTheme.keyValue(gfx, this.font, x, y, w,
                Component.translatable("village.barbarajones.stat.defence").getString(),
                status.defence + "  (-" + damageReductionPercent(status.defence) + "%)",
                status.defence > 0 ? KraveTheme.GRASS : KraveTheme.TEXT_DIM);

        y = KraveTheme.keyValue(gfx, this.font, x, y, w,
                Component.translatable("village.barbarajones.stat.production").getString(),
                Component.translatable("village.barbarajones.stat.production_value",
                        status.production).getString(),
                KraveTheme.BOX_RED_LIGHT);

        y = KraveTheme.keyValue(gfx, this.font, x, y, w,
                Component.translatable("village.barbarajones.stat.stockpile").getString(),
                status.stockpile + " / " + status.stockpileCap, KraveTheme.MILK);

        y += 3;
        gfx.drawString(this.font,
                Component.translatable("village.barbarajones.stat.happiness").getString(),
                x, y, KraveTheme.TEXT_DIM, false);
        y += 10;
        KraveTheme.progressBar(gfx, this.font, x, y, w, 9, status.happiness / 100.0F,
                happinessColor(status.happiness), status.happiness + "%");
        y += 16;
        KraveTheme.divider(gfx, x, y, w);
        return y + 6;
    }

    /**
     * The requirements block. Each line shows current against required, and is only
     * green when that specific requirement is already met - the tier is an AND of
     * all three, and a player staring at "buildings: 40/16" with no explanation for
     * why they are still a Hamlet is a support ticket.
     */
    private int drawNextTier(GuiGraphics gfx, int x, int y, int w,
                             PacketVillageStatus status, VillageTier tier) {
        VillageTier next = tier.next();
        if (next == null) {
            y = KraveTheme.sectionHeader(gfx, this.font, x, y, w,
                    Component.translatable("village.barbarajones.screen.maxed_header").getString());
            return KraveTheme.textBlock(gfx, this.font, x, y, w,
                    Component.translatable("village.barbarajones.screen.maxed_body").getString(),
                    KraveTheme.GOLD) + 6;
        }

        y = KraveTheme.sectionHeader(gfx, this.font, x, y, w,
                Component.translatable("village.barbarajones.screen.next",
                        next.displayName()).getString());

        y = requirement(gfx, x, y, w, "village.barbarajones.stat.buildings",
                status.buildings, next.requiredBuildings());
        y = requirement(gfx, x, y, w, "village.barbarajones.stat.population",
                status.population, next.requiredPopulation());
        y = requirement(gfx, x, y, w, "village.barbarajones.stat.defence",
                status.defence, next.requiredDefence());

        y += 4;
        KraveTheme.divider(gfx, x, y, w);
        return y + 6;
    }

    private int requirement(GuiGraphics gfx, int x, int y, int w, String key, int have, int need) {
        boolean met = have >= need;
        return KraveTheme.keyValue(gfx, this.font, x, y, w,
                Component.translatable(key).getString(),
                have + " / " + need,
                met ? KraveTheme.GRASS : KraveTheme.DANGER);
    }

    private int drawPortalGate(GuiGraphics gfx, int x, int y, int w, PacketVillageStatus status) {
        boolean open = status.tier >= KraveVillage.PORTAL_TIER;
        y = KraveTheme.sectionHeader(gfx, this.font, x, y, w,
                Component.translatable("village.barbarajones.screen.portal").getString());
        KraveTheme.icon(gfx, open ? KraveTheme.ICON_CHECK : KraveTheme.ICON_LOCK, x, y - 3);
        String text = open
                ? Component.translatable("village.barbarajones.screen.portal_open").getString()
                : Component.translatable("village.barbarajones.screen.portal_locked",
                        VillageTier.byIndex(KraveVillage.PORTAL_TIER).displayName()).getString();
        gfx.drawString(this.font, text, x + 20, y + 1, open ? KraveTheme.GRASS : KraveTheme.TEXT_LOCKED, false);
        return y + 16;
    }

    private static int damageReductionPercent(int defence) {
        float reduction = Math.min(KraveVillage.MAX_DAMAGE_REDUCTION,
                defence * KraveVillage.DEFENCE_TO_REDUCTION);
        return Math.round(reduction * 100.0F);
    }

    private static int happinessColor(int happiness) {
        if (happiness >= 70) {
            return KraveTheme.GRASS;
        }
        return happiness >= 35 ? KraveTheme.GOLD : KraveTheme.DANGER;
    }
}
