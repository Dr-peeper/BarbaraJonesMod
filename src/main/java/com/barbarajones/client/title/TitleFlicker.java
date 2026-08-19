package com.barbarajones.client.title;

import com.barbarajones.BarbaraJonesMod;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Random;

/**
 * The horror beat on the title screen.
 *
 * <p>Somewhere between every twenty and forty seconds, one of the mod's uncanny
 * faces is stretched over the whole menu for two or three frames at very low alpha.
 * At sixty frames a second that is under fifty milliseconds: long enough to register,
 * far too short to be sure of, which is the entire point.
 *
 * <p>It is deliberately restrained. A jump-scare on the main menu would be obnoxious
 * and would get the mod uninstalled; a doubt about whether anything happened at all
 * is what keeps people staring at the menu.
 */
public final class TitleFlicker {

    private static final int FACE_COUNT = 10;
    private static final ResourceLocation[] FACES = new ResourceLocation[FACE_COUNT];

    static {
        for (int i = 0; i < FACE_COUNT; i++) {
            FACES[i] = new ResourceLocation(BarbaraJonesMod.MODID,
                    "textures/gui/face_" + (i + 1) + ".png");
        }
    }

    private static final long MIN_GAP_MS = 20_000L;
    private static final long MAX_GAP_MS = 40_000L;

    private static final Random RNG = new Random();

    private static long nextAt = -1L;
    private static int framesLeft = 0;
    private static int frameCount = 0;
    private static int face = 0;
    private static float peak = 0.12F;

    private TitleFlicker() { }

    /**
     * Called whenever the title screen is (re)built, so returning to the menu never
     * lands you mid-flicker and never fires one instantly on arrival.
     */
    public static void reset(long now) {
        framesLeft = 0;
        nextAt = now + gap();
    }

    /** Draws the flicker if one is due. Safe to call every frame; usually does nothing. */
    public static void draw(GuiGraphics gfx, int w, int h, long now) {
        if (nextAt < 0L) {
            reset(now);
            return;
        }
        if (framesLeft <= 0 && now >= nextAt) {
            face = RNG.nextInt(FACE_COUNT);
            frameCount = 2 + RNG.nextInt(2);
            framesLeft = frameCount;
            peak = 0.09F + RNG.nextFloat() * 0.06F;
            nextAt = now + gap();
        }
        if (framesLeft <= 0) {
            return;
        }

        // Brightest on the first frame and falling away after: a flat two-frame block
        // reads as a rendering glitch, a decaying one reads as something moving.
        float alpha = peak * (framesLeft / (float) frameCount);
        framesLeft--;

        // The face textures are not screen shaped, so they are blitted as one full
        // texture stretched across the viewport - the same trick DreadClient uses.
        RenderSystem.enableBlend();
        gfx.setColor(1.0F, 1.0F, 1.0F, alpha);
        gfx.blit(FACES[face], 0, 0, 0, 0.0F, 0.0F, w, h, w, h);
        gfx.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static long gap() {
        return MIN_GAP_MS + RNG.nextInt((int) (MAX_GAP_MS - MIN_GAP_MS));
    }
}
