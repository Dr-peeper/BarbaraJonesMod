package com.barbarajones.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModSounds;
import com.barbarajones.net.PacketCaydenStatus;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cayden's health bar, and the alarm that goes with it.
 *
 * Rule #1 of this mod is that Cayden must not die, so his health is not
 * something you should have to go and look for. The bar sits under the vanilla
 * boss bar slot, and once he drops into the danger zone it starts screaming.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class CaydenHudClient {

    /** Below this fraction of max health the alarm starts. */
    private static final float DANGER   = 0.35F;
    /** Below this it gets faster and louder. */
    private static final float CRITICAL = 0.20F;

    /** No heartbeat for this long and we assume he is gone/unloaded. */
    private static final int STALE_TICKS = 60;

    private static final int BAR_W = 182;

    private static float health;
    private static float maxHealth;
    private static int distance;
    private static boolean housed;
    private static int sinceUpdate = STALE_TICKS + 1;

    private static int alarmCooldown;
    private static int flashTimer;

    private CaydenHudClient() { }

    public static void accept(PacketCaydenStatus msg) {
        health = msg.health;
        maxHealth = msg.maxHealth;
        distance = msg.distance;
        housed = msg.housed;
        sinceUpdate = 0;
    }

    private static boolean live() {
        return sinceUpdate <= STALE_TICKS && maxHealth > 0.0F && health > 0.0F;
    }

    private static float fraction() {
        return maxHealth <= 0.0F ? 0.0F : Math.max(0.0F, Math.min(1.0F, health / maxHealth));
    }

    // ---- the alarm ---------------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            sinceUpdate = STALE_TICKS + 1;
            return;
        }
        sinceUpdate++;
        if (flashTimer > 0) {
            flashTimer--;
        }
        if (alarmCooldown > 0) {
            alarmCooldown--;
        }

        if (!live()) {
            return;
        }
        float frac = fraction();
        if (frac > DANGER) {
            return;
        }

        if (alarmCooldown <= 0) {
            // The clip is 2s long. At DANGER it repeats end-to-end; below
            // CRITICAL it overlaps itself, which is far worse to sit through.
            boolean critical = frac <= CRITICAL;
            alarmCooldown = critical ? 26 : 40;
            float pitch = critical ? 1.18F : 1.0F;
            float volume = critical ? 1.0F : 0.8F;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(
                    ModSounds.CAYDEN_ALARM.get(), pitch, volume));
            flashTimer = critical ? 13 : 20;
        }
    }

    // ---- the bar -----------------------------------------------------------

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post event) {
        if (!live()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.screen != null) {
            return;
        }
        try {
            draw(event.getGuiGraphics());
        } catch (Throwable ignored) {
            // the HUD must never take the game down
        }
    }

    private static void draw(GuiGraphics gfx) {
        Minecraft mc = Minecraft.getInstance();
        int w = gfx.guiWidth();
        int x = (w - BAR_W) / 2;
        int y = 22;                        // clear of the vanilla boss bar slot

        float frac = fraction();
        boolean danger = frac <= DANGER;
        boolean critical = frac <= CRITICAL;
        boolean flash = danger && flashTimer > 0 && (flashTimer / 3) % 2 == 0;

        // label
        String name = ChatFormatting.RED + "" + ChatFormatting.BOLD + "CAYDEN COBB";
        if (critical) {
            name = (flash ? ChatFormatting.WHITE : ChatFormatting.RED) + ""
                    + ChatFormatting.BOLD + "!! CAYDEN IS DYING !!";
        } else if (danger) {
            name = ChatFormatting.RED + "" + ChatFormatting.BOLD + "CAYDEN COBB - IN DANGER";
        }
        gfx.drawCenteredString(mc.font, name, w / 2, y - 10, 0xFFFFFF);

        // frame + empty track
        gfx.fill(x - 1, y - 1, x + BAR_W + 1, y + 7, flash ? 0xFFFF5555 : 0xFF000000);
        gfx.fill(x, y, x + BAR_W, y + 6, 0xFF2B0000);

        // the red fill. Deep blood red normally, bright arterial red when low.
        int filled = (int) (BAR_W * frac);
        int body = critical ? 0xFFFF1A1A : (danger ? 0xFFE01010 : 0xFFB00000);
        if (flash) {
            body = 0xFFFF6666;
        }
        if (filled > 0) {
            gfx.fill(x, y, x + filled, y + 6, body);
            // a lighter top edge so it reads as a bar and not a flat block
            gfx.fill(x, y, x + filled, y + 2, (body & 0x00FFFFFF) | 0x66FFFFFF);
        }

        // numbers + where he is
        String right = String.format("%.0f/%.0f", health, maxHealth);
        String left = housed ? "at home" : "following";
        if (distance > 8) {
            left = left + " - " + distance + "m";
        }
        gfx.drawString(mc.font, ChatFormatting.GRAY + left, x, y + 9, 0xFFFFFF);
        gfx.drawString(mc.font, (danger ? ChatFormatting.RED : ChatFormatting.GRAY) + right,
                x + BAR_W - mc.font.width(right), y + 9, 0xFFFFFF);
    }
}
