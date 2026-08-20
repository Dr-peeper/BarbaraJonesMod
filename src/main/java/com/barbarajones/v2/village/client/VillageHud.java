package com.barbarajones.v2.village.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.client.ui.KraveTheme;
import com.barbarajones.v2.village.KraveVillage;
import com.barbarajones.v2.village.VillageTier;
import com.barbarajones.v2.village.net.PacketVillageStatus;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The village HUD: a compact cereal-box panel in the top right showing tier,
 * population, production and defence while the player is standing in a claim.
 *
 * <p>Top <em>right</em> on purpose - the quest tracker already owns the top left,
 * and two panels fighting over the same corner is worse than either one alone.
 *
 * <p>It draws only from {@link VillageClientState}, which only ever holds what the
 * server sent. When the player walks out of the claim the server sends one "not in
 * a village" status and the panel disappears; if the server stops talking entirely
 * the state goes stale after six seconds and the panel disappears anyway, rather
 * than sitting there showing a settlement that may no longer exist.
 *
 * <p>Hooked on {@code RenderGuiOverlayEvent.Post} for the hotbar overlay, matching
 * how the rest of this mod's HUD is wired, and wrapped in a catch-all: a HUD that
 * throws once per frame takes the whole game down with it.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class VillageHud {

    private static final int WIDTH = 134;
    private static final int MARGIN = 6;

    private VillageHud() { }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        try {
            draw(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth());
        } catch (Throwable ignored) {
            // never let the HUD kill the frame
        }
    }

    private static void draw(GuiGraphics gfx, int screenWidth) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.screen != null) {
            return;
        }
        if (mc.player.isSpectator()) {
            return;
        }
        PacketVillageStatus status = VillageClientState.status();
        if (status == null || !status.inVillage) {
            return;
        }

        VillageTier tier = VillageTier.byIndex(status.tier);
        int height = 74;
        int x = screenWidth - WIDTH - MARGIN;
        int y = MARGIN;

        KraveTheme.panel(gfx, x, y, WIDTH, height);

        int inner = x + KraveTheme.BORDER + KraveTheme.PAD_S;
        int innerW = WIDTH - (KraveTheme.BORDER + KraveTheme.PAD_S) * 2;
        int row = y + KraveTheme.BORDER + KraveTheme.PAD_S;

        // Name and tier - the two things worth reading at a glance.
        String name = KraveTheme.trimTo(mc.font, status.name, innerW);
        gfx.drawString(mc.font, ChatFormatting.BOLD + name, inner, row, KraveTheme.MILK, false);
        row += 10;
        gfx.drawString(mc.font, tier.displayName().getString(), inner, row, KraveTheme.GOLD, false);
        row += 11;

        // Tier as pips, so progress up the ladder is visible without arithmetic.
        KraveTheme.pips(gfx, inner, row, VillageTier.KRAVE_CAPITAL.index(),
                status.tier, KraveTheme.BOX_RED_LIGHT, KraveTheme.CHOCOLATE);
        row += 9;

        row = stat(gfx, inner, row, innerW,
                "village.barbarajones.hud.pop",
                status.population + "/" + status.populationCap, KraveTheme.MILK);
        row = stat(gfx, inner, row, innerW,
                "village.barbarajones.hud.krave",
                status.production + "/h", KraveTheme.BOX_RED_LIGHT);
        stat(gfx, inner, row, innerW,
                "village.barbarajones.hud.defence",
                status.defence + " (-" + reductionPercent(status.defence) + "%)",
                status.defence > 0 ? KraveTheme.GRASS : KraveTheme.TEXT_DIM);
    }

    private static int stat(GuiGraphics gfx, int x, int y, int w, String key,
                            String value, int color) {
        Minecraft mc = Minecraft.getInstance();
        String label = net.minecraft.network.chat.Component.translatable(key).getString();
        gfx.drawString(mc.font, label, x, y, KraveTheme.TEXT_DIM, false);
        gfx.drawString(mc.font, value, x + w - mc.font.width(value), y, color, false);
        return y + 10;
    }

    /**
     * Drops the cached status on disconnect. Without this, joining a second world
     * would briefly draw the previous world's village in the corner - which looks
     * exactly like the settlement having followed the player, and is the sort of
     * thing that gets reported as a duplication bug.
     */
    @SubscribeEvent
    public static void onLoggingOut(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        VillageClientState.clear();
    }

    private static int reductionPercent(int defence) {
        float reduction = Math.min(KraveVillage.MAX_DAMAGE_REDUCTION,
                defence * KraveVillage.DEFENCE_TO_REDUCTION);
        return Math.round(reduction * 100.0F);
    }
}
