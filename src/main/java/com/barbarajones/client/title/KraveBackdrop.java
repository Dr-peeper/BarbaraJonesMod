package com.barbarajones.client.title;

import com.barbarajones.BarbaraJonesMod;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Random;

/**
 * The animated Krave backdrop that replaces the vanilla panorama: a deep-space
 * gradient, two layers of drifting purple galaxy haze, a slow star field, and
 * cereal pieces tumbling down the screen forever.
 *
 * <p>The whole field is laid out once from a fixed seed into flat arrays and then
 * animated purely as a function of elapsed seconds. Nothing is allocated or mutated
 * per frame, so a menu left open all afternoon costs exactly what the first frame
 * cost, and the layout is identical every launch rather than reshuffling on resize.
 */
public final class KraveBackdrop {

    private static final ResourceLocation HAZE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/gui/title/haze.png");
    private static final ResourceLocation CEREAL =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/gui/title/cereal.png");

    /** haze.png is a seamless tile at this size - see tools/make_title_assets.ps1. */
    private static final int HAZE_PX = 128;

    /** cereal.png is a horizontal strip of {@link #VARIANTS} pieces, each this wide. */
    private static final int SPRITE_PX = 16;
    private static final int VARIANTS = 4;

    private static final int PIECES = 72;
    private static final int STARS = 110;

    private static final int SKY_TOP = 0xFF07010F;
    private static final int SKY_BOTTOM = 0xFF2B0A47;

    // --- the cereal field, in screen-independent terms -----------------------
    /** Horizontal position as a fraction of screen width, so the field survives resizing. */
    private static final float[] pieceX = new float[PIECES];
    private static final int[] pieceSize = new int[PIECES];
    private static final float[] pieceSpeed = new float[PIECES];      // pixels per second
    private static final float[] pieceSpin = new float[PIECES];       // degrees per second
    private static final float[] piecePhase = new float[PIECES];
    private static final float[] pieceSway = new float[PIECES];
    private static final float[] pieceSwayRate = new float[PIECES];
    private static final float[] pieceAlpha = new float[PIECES];
    private static final int[] pieceVariant = new int[PIECES];

    // --- the star field ------------------------------------------------------
    private static final float[] starX = new float[STARS];
    private static final float[] starY = new float[STARS];
    private static final float[] starRate = new float[STARS];
    private static final float[] starPhase = new float[STARS];
    private static final float[] starAlpha = new float[STARS];
    private static final boolean[] starBig = new boolean[STARS];

    static {
        Random rng = new Random(0x4B5241564EL);   // "KRAVN" - fixed so the sky never changes
        for (int i = 0; i < PIECES; i++) {
            // Depth: small, slow, faint pieces sit far back; big fast ones fall past
            // the camera. Driving size, speed and alpha off one number keeps the
            // parallax honest instead of producing a soup of unrelated confetti.
            float depth = rng.nextFloat();
            pieceX[i] = rng.nextFloat();
            pieceSize[i] = 6 + Math.round(depth * 16.0F);
            pieceSpeed[i] = 14.0F + depth * 46.0F;
            pieceSpin[i] = (rng.nextBoolean() ? 1.0F : -1.0F) * (8.0F + rng.nextFloat() * 46.0F);
            piecePhase[i] = rng.nextFloat() * 2000.0F;
            pieceSway[i] = 4.0F + rng.nextFloat() * 14.0F;
            pieceSwayRate[i] = 0.25F + rng.nextFloat() * 0.7F;
            pieceAlpha[i] = 0.32F + depth * 0.58F;
            pieceVariant[i] = rng.nextInt(VARIANTS);
        }
        for (int i = 0; i < STARS; i++) {
            starX[i] = rng.nextFloat();
            starY[i] = rng.nextFloat();
            starRate[i] = 0.6F + rng.nextFloat() * 2.6F;
            starPhase[i] = rng.nextFloat() * 6.2831855F;
            starAlpha[i] = 0.25F + rng.nextFloat() * 0.7F;
            starBig[i] = rng.nextInt(9) == 0;
        }
    }

    private KraveBackdrop() { }

    /**
     * Paint the whole backdrop.
     *
     * @param t seconds since the title screen handler was first loaded
     */
    public static void draw(GuiGraphics gfx, int w, int h, float t) {
        gfx.fillGradient(0, 0, w, h, SKY_TOP, SKY_BOTTOM);
        drawHaze(gfx, w, h, t);
        drawStars(gfx, w, h, t);
        drawCereal(gfx, w, h, t);
        drawVignette(gfx, w, h);
    }

    // ---- galaxy haze --------------------------------------------------------

    private static void drawHaze(GuiGraphics gfx, int w, int h, float t) {
        RenderSystem.enableBlend();
        // Two layers crossing each other at different speeds and scales: a single
        // scrolling layer reads as wallpaper, two read as depth.
        hazeLayer(gfx, w, h, t, 3.00F, 9.0F, -4.0F, 0.86F, 0.50F, 1.00F, 0.17F);
        hazeLayer(gfx, w, h, t, 1.75F, -13.0F, 6.0F, 0.55F, 0.28F, 0.92F, 0.12F);
        gfx.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private static void hazeLayer(GuiGraphics gfx, int w, int h, float t, float scale,
                                  float driftX, float driftY,
                                  float r, float g, float b, float a) {
        int tile = Math.max(16, Math.round(HAZE_PX * scale));
        // Scroll by translating the pose rather than the blit coordinates: blits take
        // integer positions, and a 1px-per-step haze would visibly stutter.
        float ox = wrap(t * driftX, tile);
        float oy = wrap(t * driftY, tile);

        PoseStack pose = gfx.pose();
        pose.pushPose();
        pose.translate(-ox, -oy, 0.0F);
        gfx.setColor(r, g, b, a);
        for (int x = 0; x < w + tile; x += tile) {
            for (int y = 0; y < h + tile; y += tile) {
                gfx.blit(HAZE, x, y, tile, tile, 0.0F, 0.0F, HAZE_PX, HAZE_PX, HAZE_PX, HAZE_PX);
            }
        }
        pose.popPose();
    }

    /** Positive remainder, so an offset drifting the negative way still lands inside one tile. */
    private static float wrap(float v, int period) {
        float m = v % period;
        return m < 0.0F ? m + period : m;
    }

    // ---- stars --------------------------------------------------------------

    private static void drawStars(GuiGraphics gfx, int w, int h, float t) {
        RenderSystem.enableBlend();
        for (int i = 0; i < STARS; i++) {
            float twinkle = 0.35F + 0.65F * (0.5F + 0.5F * Mth.sin(t * starRate[i] + starPhase[i]));
            int alpha = (int) (twinkle * starAlpha[i] * 255.0F);
            if (alpha <= 4) {
                continue;
            }
            int sx = (int) (starX[i] * w);
            int sy = (int) (starY[i] * h);
            int size = starBig[i] ? 2 : 1;
            gfx.fill(sx, sy, sx + size, sy + size, (alpha << 24) | 0xFFEFD6);
        }
        RenderSystem.disableBlend();
    }

    // ---- the falling cereal -------------------------------------------------

    private static void drawCereal(GuiGraphics gfx, int w, int h, float t) {
        RenderSystem.enableBlend();
        PoseStack pose = gfx.pose();
        for (int i = 0; i < PIECES; i++) {
            int size = pieceSize[i];
            float span = h + size * 2.0F;
            float y = ((t * pieceSpeed[i] + piecePhase[i]) % span) - size;
            float x = pieceX[i] * w + Mth.sin(t * pieceSwayRate[i] + piecePhase[i]) * pieceSway[i];
            float angle = (t * pieceSpin[i] + piecePhase[i] * 20.0F) % 360.0F;

            pose.pushPose();
            pose.translate(x, y, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(angle));
            pose.translate(-size / 2.0F, -size / 2.0F, 0.0F);
            gfx.setColor(1.0F, 1.0F, 1.0F, pieceAlpha[i]);
            gfx.blit(CEREAL, 0, 0, size, size,
                    pieceVariant[i] * (float) SPRITE_PX, 0.0F, SPRITE_PX, SPRITE_PX,
                    VARIANTS * SPRITE_PX, SPRITE_PX);
            pose.popPose();
        }
        gfx.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    // ---- edge darkening -----------------------------------------------------

    /**
     * A soft dark frame around the screen. Without it the cereal storm competes with
     * the menu text at the edges, and the branding in the corners becomes unreadable
     * whenever a bright haze blob drifts underneath it.
     */
    private static void drawVignette(GuiGraphics gfx, int w, int h) {
        final int bands = 20;
        final int step = 3;
        for (int i = 0; i < bands; i++) {
            int alpha = (int) (120.0F * (1.0F - i / (float) bands));
            if (alpha <= 2) {
                continue;
            }
            int colour = alpha << 24;
            int off = i * step;
            gfx.fill(0, off, w, off + step, colour);
            gfx.fill(0, h - off - step, w, h - off, colour);
            gfx.fill(off, 0, off + step, h, colour);
            gfx.fill(w - off - step, 0, w - off, h, colour);
        }
    }
}
