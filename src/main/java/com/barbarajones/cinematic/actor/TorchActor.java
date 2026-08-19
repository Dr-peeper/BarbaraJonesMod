package com.barbarajones.cinematic.actor;

import net.minecraft.util.Mth;

/**
 * Daniel's lighter, if Daniel's lighter were the size of a grain silo: a propane
 * torch hanging nose-down over the world with a flame that can be thrown a
 * hundred blocks.
 *
 * <p>Authored with the <b>nozzle at y=0</b> and the tank going up, so the actor's
 * {@code y} track is the height of the nozzle and the flame simply hangs below
 * it.
 *
 * <p>The flame is a stack of additive boxes, and each one samples the torch's own
 * sweep a little further in the past. That single trick is what makes the fire
 * trail behind the nozzle when the torch swings instead of turning with it like a
 * rigid cone - fire has no business being rigid.
 *
 * <p>Tracks: {@code flame} 0..1 for length, {@code trigger} degrees,
 * {@code sweep} for the torch's own yaw, {@code pitch}.
 */
public final class TorchActor extends CinematicActor {

    private static final int FLAME_SEGMENTS = 18;
    private static final float SEGMENT = 1.35F;

    public TorchActor() {
        Part body = part("body", this.root, 0.0F, 0.0F, 0.0F);

        part("nozzle", body, 0.0F, 0.0F, 0.0F)
                .box(-0.23F, 0.0F, -0.23F, 0.46F, 0.24F, 0.46F).colour(0x2A2E33);
        part("neck", body, 0.0F, 0.24F, 0.0F)
                .box(-0.15F, 0.0F, -0.15F, 0.30F, 0.42F, 0.30F).colour(0xB6BDC4);
        part("shroud", body, 0.0F, 0.60F, 0.0F)
                .box(-0.30F, 0.0F, -0.30F, 0.60F, 0.22F, 0.60F).colour(0x8C939A);
        part("tank", body, 0.0F, 0.80F, 0.0F)
                .box(-0.42F, 0.0F, -0.42F, 0.84F, 1.85F, 0.84F).colour(0x2F5FA8);
        part("band", body, 0.0F, 1.55F, 0.0F)
                .box(-0.44F, 0.0F, -0.44F, 0.88F, 0.16F, 0.88F).colour(0xD8D2C2);
        part("cap", body, 0.0F, 2.65F, 0.0F)
                .box(-0.30F, 0.0F, -0.30F, 0.60F, 0.22F, 0.60F).colour(0xB6BDC4);

        Part grip = part("grip", body, 0.42F, 0.88F, 0.0F);
        grip.box(0.0F, 0.0F, -0.14F, 0.52F, 0.50F, 0.28F).colour(0x22262B);
        part("trigger", grip, 0.46F, 0.10F, 0.0F)
                .box(-0.06F, -0.38F, -0.09F, 0.12F, 0.38F, 0.18F).colour(0xC0C6CC);

        Part flame = part("flame", body, 0.0F, -0.02F, 0.0F);
        for (int i = 0; i < FLAME_SEGMENTS; i++) {
            part("flame" + i, flame, 0.0F, -i * SEGMENT, 0.0F)
                    .box(-0.5F, -SEGMENT, -0.5F, 1.0F, SEGMENT, 1.0F)
                    .colour(1.0F, 0.7F, 0.2F).glow();
        }
    }

    @Override
    protected void pose(float time) {
        Part body = get("body");
        float sweep = track("sweep", time, 0.0F);
        body.rotY = sweep;
        body.rotX = track("pitch", time, 0.0F);
        get("trigger").rotZ = track("trigger", time, 0.0F);

        float length = Mth.clamp(track("flame", time, 0.0F), 0.0F, 1.0F);
        Part flame = get("flame");
        if (length <= 0.01F) {
            flame.visible = false;
            return;
        }
        flame.visible = true;
        float lit = length * FLAME_SEGMENTS;

        for (int i = 0; i < FLAME_SEGMENTS; i++) {
            Part seg = flame.children.get(i);
            float head = lit - i;
            if (head <= 0.0F) {
                seg.visible = false;
                continue;
            }
            seg.visible = true;
            float kn = (i + 0.5F) / FLAME_SEGMENTS;
            float tipFade = Mth.clamp(head, 0.0F, 1.0F);

            // Roar: every segment breathes on its own phase so the column boils.
            float roar = 0.82F + 0.30F * Mth.sin(time * 1.35F + i * 1.7F)
                    + 0.14F * Mth.sin(time * 3.1F - i * 0.9F);
            float w = (0.22F + 1.18F * Mth.sin((float) (Math.PI * Math.pow(kn, 0.6D)))) * roar;
            seg.scale(w, 1.0F, w);

            // The lag that makes it fire and not a cone: this segment answers to
            // where the nozzle was pointing a moment ago, not where it points now.
            float then = track("sweep", time - i * 0.85F, sweep);
            float drift = (then - sweep) * 0.032F * i;
            seg.offX = drift + Mth.sin(time * 0.9F + i * 2.1F) * 0.10F * kn;
            seg.offZ = Mth.cos(time * 0.8F + i * 1.6F) * 0.10F * kn;

            if (kn < 0.18F) {
                seg.colour(0.62F, 0.80F, 1.0F);
            } else if (kn < 0.55F) {
                float u = (kn - 0.18F) / 0.37F;
                seg.colour(ramp(0.62F, 1.0F, u), ramp(0.80F, 0.66F, u),
                        ramp(1.0F, 0.16F, u));
            } else {
                float u = (kn - 0.55F) / 0.45F;
                seg.colour(ramp(1.0F, 0.78F, u), ramp(0.66F, 0.16F, u),
                        ramp(0.16F, 0.02F, u));
            }
            seg.alpha = Mth.clamp((0.95F - 0.72F * kn) * roar * tipFade, 0.0F, 1.0F);
        }
    }

    /** Plain linear blend: the colour ramp inside a flame has no easing to it. */
    private static float ramp(float a, float b, float u) {
        return a + (b - a) * Mth.clamp(u, 0.0F, 1.0F);
    }
}
