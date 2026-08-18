package com.barbarajones.client;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The Krave Kosmos's actual tension cue: a brief red vignette when you're
 * hit by Krave Monster (melee or his mouth beam), instead of the generic
 * ambient Dread system that used to run there permanently maxed-out (see
 * DreadClient.tick()'s Kosmos suppression). Same coarse-band draw technique
 * as DreadClient.vignette(), tinted red, decaying linearly instead of
 * breathing/building.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, value = Dist.CLIENT)
public final class KraveHitClient {

    private static final int PEAK_TICKS = 60;

    private static int timer;

    private KraveHitClient() { }

    public static void accept() {
        timer = PEAK_TICKS;
    }

    @SubscribeEvent
    public static void onOverlay(RenderGuiOverlayEvent.Post event) {
        if (timer <= 0) {
            return;
        }
        timer--;
        try {
            render(event.getGuiGraphics());
        } catch (Throwable ignored) {
            // the hit flash must never take the game down
        }
    }

    private static void render(GuiGraphics gfx) {
        int w = gfx.guiWidth(), h = gfx.guiHeight();
        float strength = timer / (float) PEAK_TICKS;
        vignette(gfx, w, h, strength);
    }

    /** Same coarse-band frame as DreadClient.vignette(), tinted red. */
    private static void vignette(GuiGraphics gfx, int w, int h, float s) {
        int band = (int) (Math.min(w, h) * 0.28F);
        int maxA = (int) (Mth.clamp(s, 0.0F, 1.0F) * 170);
        int steps = 16;
        int stepSize = Math.max(1, band / steps);
        for (int i = 0; i < band; i += stepSize) {
            int a = maxA * (band - i) / band;
            if (a <= 0) {
                continue;
            }
            int col = (a << 24) | 0x990000;
            int t = Math.min(stepSize, band - i);
            gfx.fill(0, i, w, i + t, col);
            gfx.fill(0, h - i - t, w, h - i, col);
            gfx.fill(i, 0, i + t, h, col);
            gfx.fill(w - i - t, 0, w - i, h, col);
        }
    }
}
