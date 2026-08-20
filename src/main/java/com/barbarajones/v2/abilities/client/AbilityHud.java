package com.barbarajones.v2.abilities.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.abilities.AbilityId;
import com.barbarajones.v2.abilities.AbilityItems;
import com.barbarajones.client.ApocalypseClient;
import com.barbarajones.client.ui.KraveTheme;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The row of ability slots above the hotbar: one 20x20 tile per ability the
 * player has actually unlocked (locked ones simply do not take up space -
 * the row itself grows as the player earns more, which reads as its own
 * little progress trophy case).
 *
 * <p>Each tile shows the real item icon, a top-down dark wipe for cooldown
 * (shrinks to nothing exactly when the ability comes off cooldown), and a
 * gold pulsing frame plus a countdown while an active window (Charm's
 * flight, Band's dodge, God Core's aura) is actually running. All of it
 * reads off {@link AbilityClientState}, which is fed purely by
 * {@code PacketAbilitySync} - there is no reach into inventory or world
 * state here at all.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class AbilityHud {

    private static final int SLOT = 20;
    private static final int GAP = 3;

    private AbilityHud() { }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null || mc.level == null || !AbilityClientState.ready()) {
            return;
        }
        if (mc.screen != null || ApocalypseClient.isActive()) {
            return;
        }
        try {
            draw(event.getGuiGraphics(), mc.font);
        } catch (Throwable ignored) {
            // the HUD must never take the game down
        }
    }

    private static void draw(GuiGraphics gfx, Font font) {
        int unlocked = 0;
        for (AbilityId id : AbilityId.VALUES) {
            if (AbilityClientState.isUnlocked(id)) {
                unlocked++;
            }
        }
        if (unlocked == 0) {
            return;
        }

        int totalW = unlocked * SLOT + (unlocked - 1) * GAP;
        int screenW = gfx.guiWidth();
        int screenH = gfx.guiHeight();
        int x = (screenW - totalW) / 2;
        int y = screenH - 22 - SLOT - 6;

        for (AbilityId id : AbilityId.VALUES) {
            if (!AbilityClientState.isUnlocked(id)) {
                continue;
            }
            drawSlot(gfx, font, id, x, y);
            x += SLOT + GAP;
        }
    }

    private static void drawSlot(GuiGraphics gfx, Font font, AbilityId id, int x, int y) {
        boolean active = AbilityClientState.isActive(id);
        boolean ready = AbilityClientState.isReady(id);

        int frameColor = active
                ? KraveTheme.lerpColor(KraveTheme.GOLD, 0xFFFFFFFF, KraveTheme.breathe())
                : (ready ? KraveTheme.BOX_RED_DARK : KraveTheme.TEXT_LOCKED);
        gfx.fill(x - 1, y - 1, x + SLOT + 1, y + SLOT + 1, 0xFF000000);
        gfx.fill(x, y, x + SLOT, y + SLOT, frameColor);
        gfx.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xE0161018);

        ItemStack stack = new ItemStack(AbilityItems.itemFor(id).get());
        int ix = x + (SLOT - 16) / 2;
        int iy = y + (SLOT - 16) / 2;
        gfx.renderItem(stack, ix, iy);

        if (!ready && !active) {
            long remaining = AbilityClientState.cooldownRemaining(id);
            float frac = Math.min(1.0F, remaining / (float) id.cooldownTicks);
            int wipeH = Math.round((SLOT - 2) * frac);
            gfx.fill(x + 1, y + 1, x + SLOT - 1, y + 1 + wipeH, 0xB0000000);
            String secs = String.valueOf((remaining / 20L) + 1L);
            gfx.drawString(font, secs, x + SLOT - font.width(secs) - 2, y + SLOT - 9, KraveTheme.TEXT, true);
        } else if (active) {
            long remaining = AbilityClientState.activeRemaining(id);
            String secs = String.valueOf((remaining / 20L) + 1L);
            gfx.drawString(font, secs, x + SLOT - font.width(secs) - 2, y + SLOT - 9, KraveTheme.GOLD, true);
        }
    }
}
