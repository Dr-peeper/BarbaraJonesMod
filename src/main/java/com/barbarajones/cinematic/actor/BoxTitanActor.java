package com.barbarajones.cinematic.actor;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.cinematic.Easing;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The Krave carton, forty blocks of it, falling out of the sky with its top flaps
 * still sealed.
 *
 * <p>Authored with the base at y=0 so the {@code y} track is the height of the
 * bottom of the box - the frame where y reaches the ground is the frame it
 * lands, which is the only moment in the beat that has to be exact.
 *
 * <p>The four top flaps are separate hinges so they can burst past their stop and
 * settle back, and the cereal is a real ballistic spray of tumbling pieces that
 * bounce where they land. A carton that lands and simply sits there has no
 * weight; everything that keeps moving after the impact is what supplies it.
 *
 * <p>Tracks: {@code squash} (1 = at rest, less than 1 = compressed),
 * {@code flap} (degrees), {@code spill} 0..1, {@code spin}.
 */
public final class BoxTitanActor extends CinematicActor {

    private static final ResourceLocation CARTON =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/giant_krave_box.png");

    private static final int PIECES = 30;
    private static final float W = 1.60F;
    private static final float H = 2.30F;
    private static final float D = 0.92F;

    /** World height the cereal lands on. */
    public double groundY;

    private final float[] pieceAngle = new float[PIECES];
    private final float[] pieceSpeed = new float[PIECES];
    private final float[] pieceSpin = new float[PIECES];

    public BoxTitanActor() {
        Part body = part("body", this.root, 0.0F, 0.0F, 0.0F);

        Part carton = part("carton", body, 0.0F, 0.0F, 0.0F);
        carton.box(-W * 0.5F, 0.0F, -D * 0.5F, W, H, D).skin(CARTON);

        // The inside of an opened box is dark; the Krave glow is what is coming
        // out of it, so it lives just below the rim and only shows once the flaps
        // are off it.
        part("throat", body, 0.0F, H - 0.28F, 0.0F)
                .box(-W * 0.45F, 0.0F, -D * 0.42F, W * 0.9F, 0.26F, D * 0.84F)
                .colour(0x120A1E);
        part("glow", body, 0.0F, H - 0.10F, 0.0F)
                .box(-W * 0.44F, 0.0F, -D * 0.40F, W * 0.88F, 0.08F, D * 0.80F)
                .colour(0.62F, 0.24F, 0.92F).glow().alpha(0.0F);

        part("flapFront", body, 0.0F, H, D * 0.5F)
                .box(-W * 0.5F, -0.03F, 0.0F, W, 0.03F, D * 0.55F).colour(0xC9B79A);
        part("flapBack", body, 0.0F, H, -D * 0.5F)
                .box(-W * 0.5F, -0.03F, -D * 0.55F, W, 0.03F, D * 0.55F).colour(0xC9B79A);
        part("flapRight", body, W * 0.5F, H, 0.0F)
                .box(0.0F, -0.03F, -D * 0.5F, W * 0.30F, 0.03F, D).colour(0xB9A88C);
        part("flapLeft", body, -W * 0.5F, H, 0.0F)
                .box(-W * 0.30F, -0.03F, -D * 0.5F, W * 0.30F, 0.03F, D).colour(0xB9A88C);

        Part spill = part("spill", this.root, 0.0F, H, 0.0F);
        spill.visible = false;
        for (int i = 0; i < PIECES; i++) {
            // Golden-angle fan: a deterministic scatter that never clumps, so the
            // spray looks thrown rather than emitted from a grid.
            this.pieceAngle[i] = (float) (i * 2.39996323D);
            this.pieceSpeed[i] = 0.55F + ((i * 37) % 17) / 17.0F * 0.95F;
            this.pieceSpin[i] = 380.0F + ((i * 53) % 23) * 26.0F;
            Part piece = part("piece" + i, spill, 0.0F, 0.0F, 0.0F);
            float shade = 0.78F + ((i * 29) % 11) / 11.0F * 0.30F;
            piece.box(-0.11F, -0.11F, -0.07F, 0.22F, 0.22F, 0.14F)
                    .colour(0.36F * shade, 0.20F * shade, 0.11F * shade);
        }
    }

    @Override
    protected void pose(float time) {
        Part body = get("body");
        body.rotY = track("spin", time, 0.0F);

        // Squash and stretch, with the width answering the height so the carton
        // keeps its volume. Without this a landing is just a stop.
        float squash = Mth.clamp(track("squash", time, 1.0F), 0.25F, 1.6F);
        float widen = (float) (1.0D / Math.sqrt(squash));
        body.scale(widen, squash, widen);

        float flap = track("flap", time, 0.0F);
        get("flapFront").rotX = flap;
        get("flapBack").rotX = -flap;
        get("flapRight").rotZ = -flap;
        get("flapLeft").rotZ = flap;
        get("glow").alpha = Mth.clamp((flap - 12.0F) / 70.0F, 0.0F, 0.85F)
                * (0.75F + 0.25F * Mth.sin(time * 0.4F));

        float p = track("spill", time, 0.0F);
        Part spill = get("spill");
        if (p <= 0.0F) {
            spill.visible = false;
            return;
        }
        spill.visible = true;
        spill.offY = (squash - 1.0F) * H;
        float floor = (float) ((this.groundY - this.pos.y) / Math.max(0.001F, this.scale)) - H;

        for (int i = 0; i < PIECES; i++) {
            Part piece = spill.children.get(i);
            float lag = (i % 6) * 0.045F;
            float u = Mth.clamp(p - lag, 0.0F, 1.0F);
            float r = this.pieceSpeed[i] * 7.4F * u;
            piece.pivotX = Mth.cos(this.pieceAngle[i]) * r;
            piece.pivotZ = Mth.sin(this.pieceAngle[i]) * r;

            float arc = this.pieceSpeed[i] * 3.4F * u - 9.6F * u * u;
            if (arc <= floor) {
                // Landed: one decaying bounce rather than a dead stop.
                float since = Mth.clamp((u - landingTime(i)) * 3.4F, 0.0F, 1.0F);
                arc = floor + (1.0F - Easing.BOUNCE_OUT.apply(since)) * 0.55F;
            }
            piece.pivotY = arc;
            piece.rotX = u * this.pieceSpin[i];
            piece.rotY = u * this.pieceSpin[i] * 0.6F;
            piece.alpha = 1.0F;
        }
    }

    /** Normalised time at which piece i's parabola reaches the floor. */
    private float landingTime(int i) {
        float floor = (float) ((this.groundY - this.pos.y) / Math.max(0.001F, this.scale)) - H;
        float a = -9.6F;
        float b = this.pieceSpeed[i] * 3.4F;
        float disc = b * b - 4.0F * a * (-floor);
        if (disc <= 0.0F) {
            return 1.0F;
        }
        return Mth.clamp((-b - (float) Math.sqrt(disc)) / (2.0F * a), 0.0F, 1.0F);
    }
}
