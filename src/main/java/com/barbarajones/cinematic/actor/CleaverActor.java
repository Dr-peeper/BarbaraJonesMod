package com.barbarajones.cinematic.actor;

import net.minecraft.util.Mth;

/**
 * The colossal cleaver: a real slab of steel with a spine, a bolster, a riveted
 * handle and a white-hot edge, hanging point-down over the world.
 *
 * <p>Authored with the <b>edge at y=0</b> and the steel going up, so the actor's
 * {@code y} track is literally the height of the cutting edge. That makes the
 * plunge trivially authorable: the key that says "at tick 128, y = ground" is
 * the frame the blade bites.
 *
 * <p>A ring of dust boxes lives at ground height and bursts outward on the
 * {@code dust} track. It is kept in world terms via {@link #groundY} rather than
 * parented to the blade, because dust thrown up by an impact must stay where the
 * impact was while the blade recoils away from it.
 *
 * <p>Tracks: {@code tilt} (roll), {@code spin} (yaw of the steel itself),
 * {@code pitch}, {@code heat} for the edge, {@code dust} 0..1.
 */
public final class CleaverActor extends CinematicActor {

    private static final int DUST_MOTES = 22;

    /** World height the dust ring sits at - normally the ground under the blade. */
    public double groundY;

    public CleaverActor() {
        Part body = part("body", this.root, 0.0F, 0.0F, 0.0F);

        Part blade = part("blade", body, 0.0F, 0.0F, 0.0F);
        blade.box(-0.52F, 0.0F, -0.055F, 1.04F, 1.52F, 0.11F).colour(0x9AA3AC);

        // A brighter strip along the very bottom: the honed edge catches light no
        // matter how the blade is turned, and it is what glows on impact.
        part("edge", blade, 0.0F, 0.0F, 0.0F)
                .box(-0.52F, 0.0F, -0.062F, 1.04F, 0.055F, 0.124F)
                .colour(1.0F, 0.98F, 0.92F).glow();
        // Thickened spine along the top - the silhouette detail that stops the
        // blade reading as a flat card the instant the camera comes side-on.
        part("spine", blade, 0.0F, 1.40F, 0.0F)
                .box(-0.52F, 0.0F, -0.10F, 1.04F, 0.14F, 0.20F).colour(0x6E767E);
        part("bevel", blade, 0.0F, 0.055F, 0.0F)
                .box(-0.52F, 0.0F, -0.075F, 1.04F, 0.30F, 0.15F).colour(0xC3CBD3);

        Part bolster = part("bolster", body, 0.52F, 1.14F, 0.0F);
        bolster.box(0.0F, 0.0F, -0.13F, 0.13F, 0.40F, 0.26F).colour(0xD3D8DC);

        Part handle = part("handle", bolster, 0.13F, 0.02F, 0.0F);
        handle.box(0.0F, 0.0F, -0.145F, 0.96F, 0.36F, 0.29F).colour(0x3A2416);
        for (int i = 0; i < 3; i++) {
            part("rivet" + i, handle, 0.20F + i * 0.28F, 0.18F, 0.146F)
                    .box(-0.045F, -0.045F, 0.0F, 0.09F, 0.09F, 0.012F).colour(0xC9CDD1);
        }

        Part dust = part("dust", this.root, 0.0F, 0.0F, 0.0F);
        dust.visible = false;
        for (int i = 0; i < DUST_MOTES; i++) {
            double a = i * Math.PI * 2.0D / DUST_MOTES;
            Part mote = part("dust" + i, dust, (float) Math.cos(a), 0.0F, (float) Math.sin(a));
            mote.rotY = -(float) Math.toDegrees(a);
            mote.box(-0.22F, -0.10F, -0.14F, 0.44F, 0.20F, 0.28F)
                    .colour(0.62F, 0.55F, 0.44F).glow();
        }
    }

    @Override
    protected void pose(float time) {
        Part body = get("body");
        body.rotZ = track("tilt", time, 0.0F);
        body.rotY = track("spin", time, 0.0F);
        body.rotX = track("pitch", time, 0.0F);

        float heat = Mth.clamp(track("heat", time, 0.22F), 0.0F, 2.0F);
        Part edge = get("edge");
        edge.alpha = Mth.clamp(0.15F + heat * 0.85F, 0.0F, 1.0F);
        edge.colour(1.0F, Mth.clamp(0.95F - heat * 0.45F, 0.2F, 1.0F),
                Mth.clamp(0.88F - heat * 0.8F, 0.05F, 1.0F));

        float burst = track("dust", time, 0.0F);
        Part dust = get("dust");
        if (burst <= 0.0F || burst >= 1.0F) {
            dust.visible = false;
        } else {
            dust.visible = true;
            // The ring lives at the ground, not on the blade, so the recoil does
            // not drag the debris back up with the steel.
            dust.offY = (float) ((this.groundY - this.pos.y) / Math.max(0.001F, this.scale));
            float spread = 1.0F - (1.0F - burst) * (1.0F - burst);
            for (int i = 0; i < dust.children.size(); i++) {
                Part mote = dust.children.get(i);
                float lag = (i % 5) * 0.06F;
                float p = Mth.clamp(spread - lag, 0.0F, 1.0F);
                float reach = 0.6F + p * 5.4F;
                mote.pivotX = (float) Math.cos(i * Math.PI * 2.0D / DUST_MOTES) * reach;
                mote.pivotZ = (float) Math.sin(i * Math.PI * 2.0D / DUST_MOTES) * reach;
                // Thrown up, then dragged back down: dust is not a flat ripple.
                mote.pivotY = p * 2.4F - p * p * 3.0F;
                float s = 0.5F + p * 2.2F;
                mote.scale(s, s * 0.7F, s);
                mote.alpha = (1.0F - burst) * 0.55F;
            }
        }
    }
}
