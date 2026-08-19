package com.barbarajones.cinematic.actor;

import net.minecraft.util.Mth;

/**
 * Sky-Barbara: the giant who rises over the horizon, drags on a joint the size of
 * a bus and exhales colossal smoke O's straight down the lens.
 *
 * <p>The O's are real rings of glowing boxes rather than a texture on a card, so
 * they still read as rings when the camera orbits past them, and they can be
 * flown THROUGH - the moment a ring swallows the camera is the whole point of
 * the beat and a billboard can never sell it.
 *
 * <p>Tracks on top of the humanoid set: {@code ember} for the coal in the joint,
 * and {@code ring0..ring2} where each is a 0..1 life for one exhaled O (values at
 * or below zero mean that ring has not been blown yet).
 */
public final class SmokerActor extends HumanoidActor {

    private static final int RINGS = 3;
    private static final int SEGMENTS = 20;

    /** Where the O's leave her face, in model units. */
    private static final float MOUTH_Y = 2.03F;
    private static final float MOUTH_Z = 0.22F;

    public SmokerActor() {
        // Barbara: weathered skin, dyed-out brown hair, the purple tank top, jeans.
        super(0xC98F6E, 0x3A2317, 0x5B2E86, 0x2B3A63, 0x1A1512);

        Part hand = get("handR");
        // The joint is held pinched between the fingers, angled away from the palm.
        Part joint = part("joint", hand, 0.03F, -0.13F, 0.09F);
        joint.rot(-24.0F, 0.0F, 0.0F);
        joint.box(-0.028F, -0.34F, -0.028F, 0.056F, 0.34F, 0.056F).colour(0xEDE3CB);
        part("ember", joint, 0.0F, -0.34F, 0.0F)
                .box(-0.036F, -0.055F, -0.036F, 0.072F, 0.055F, 0.072F)
                .colour(1.0F, 0.42F, 0.08F).glow();

        for (int r = 0; r < RINGS; r++) {
            Part ring = part("ring" + r, this.root, 0.0F, MOUTH_Y, MOUTH_Z);
            ring.visible = false;
            for (int s = 0; s < SEGMENTS; s++) {
                double a = s * Math.PI * 2.0D / SEGMENTS;
                Part seg = part("ring" + r + "_" + s, ring,
                        (float) Math.cos(a), (float) Math.sin(a), 0.0F);
                seg.rotZ = (float) Math.toDegrees(a) + 90.0F;
                seg.box(-0.19F, -0.115F, -0.115F, 0.38F, 0.23F, 0.23F)
                        .colour(0.86F, 0.83F, 0.90F).glow();
            }
        }
    }

    @Override
    protected void pose(float time) {
        super.pose(time);

        float ember = Mth.clamp(track("ember", time, 0.35F), 0.0F, 1.6F);
        Part coal = get("ember");
        // A coal does not glow steadily; it breathes with the drag on it.
        float flicker = ember * (0.85F + 0.15F * Mth.sin(time * 0.9F));
        coal.colour(1.0F, 0.26F + 0.30F * flicker, 0.04F + 0.12F * flicker);
        coal.alpha = Mth.clamp(0.25F + flicker, 0.0F, 1.0F);
        float bloom = 1.0F + flicker * 0.8F;
        coal.scale(bloom, bloom, bloom);

        for (int r = 0; r < RINGS; r++) {
            Part ring = get("ring" + r);
            float p = track("ring" + r, time, -1.0F);
            if (p <= 0.0F || p >= 1.0F) {
                ring.visible = false;
                continue;
            }
            ring.visible = true;
            // Expand fast at first then coast: a smoke ring loses energy to the
            // air, it does not grow at a constant rate.
            float grow = 1.0F - (1.0F - p) * (1.0F - p);
            float s = 0.30F + grow * 3.1F;
            ring.scale(s, s, 1.0F - 0.35F * grow);
            ring.offZ = grow * 2.3F;
            ring.offY = grow * 1.35F;
            ring.rotZ = p * 46.0F;
            ring.rotY = Mth.sin(time * 0.07F + r) * 6.0F;

            float alpha = (1.0F - p) * (1.0F - p) * 0.85F;
            for (int s2 = 0; s2 < ring.children.size(); s2++) {
                Part seg = ring.children.get(s2);
                // Each segment wavers on its own phase so the ring buckles as it
                // ages instead of staying a perfect maths circle.
                float wob = Mth.sin(time * 0.23F + s2 * 0.9F + r) * 0.06F * grow;
                seg.offX = wob;
                seg.offY = Mth.cos(time * 0.19F + s2 * 0.7F) * 0.06F * grow;
                seg.alpha = alpha;
            }
        }
    }
}
