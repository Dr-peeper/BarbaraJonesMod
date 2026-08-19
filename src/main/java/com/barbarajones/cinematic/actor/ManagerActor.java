package com.barbarajones.cinematic.actor;

import net.minecraft.util.Mth;

/**
 * THE INTERNET MANAGER: a faceless giant in a white shirt and a black necktie,
 * who walks in from the horizon and does not stop.
 *
 * <p>He has no eyes and no mouth on purpose - the humanoid rig's features are
 * switched off rather than removed, so the jaw joint is still there and still
 * moves the head's silhouette when he talks, which is far more unsettling than a
 * face that visibly opens.
 *
 * <p>Extra tracks: {@code tie} swings the necktie by hand when a scene wants it
 * flung; left alone it swings itself off the torso's motion.
 */
public final class ManagerActor extends HumanoidActor {

    public ManagerActor() {
        // Grey skin, black hair, white shirt, charcoal slacks, black shoes.
        super(0x8E8B86, 0x14100E, 0xF2F0EA, 0x24242A, 0x0C0B0A);

        get("eyeR").visible = false;
        get("eyeL").visible = false;
        get("mouth").visible = false;
        get("hairTail").visible = false;

        Part torso = get("torso");
        // Collar first, so the tie appears to come out from under it.
        part("collar", torso, 0.0F, 0.52F, 0.0F)
                .box(-0.22F, 0.0F, -0.18F, 0.44F, 0.07F, 0.36F).colour(0xFFFFFF);
        Part knot = part("tie", torso, 0.0F, 0.50F, 0.165F);
        knot.box(-0.045F, -0.09F, 0.0F, 0.09F, 0.09F, 0.035F).colour(0x14161C);
        part("tieBlade", knot, 0.0F, -0.09F, 0.012F)
                .box(-0.06F, -0.42F, 0.0F, 0.12F, 0.42F, 0.03F).colour(0x1B2030);

        // The lanyard badge: the only bright thing on him, and the only clue that
        // he is somebody's employee.
        part("badge", torso, 0.16F, 0.24F, 0.163F)
                .box(-0.05F, -0.07F, 0.0F, 0.10F, 0.14F, 0.02F).colour(0xE8D24A);
    }

    @Override
    protected void pose(float time) {
        super.pose(time);

        float authored = track("tie", time, Float.NaN);
        Part tie = get("tie");
        if (!Float.isNaN(authored)) {
            tie.rotX = authored;
        } else {
            // Secondary motion: the tie answers to the torso's lean a few ticks
            // ago, plus a lazy sway of its own. Nothing else on him moves, which
            // is exactly why this one detail sells the weight of him.
            float leanNow = track("lean", time, 0.0F) + track("spine", time, 0.0F);
            float leanThen = track("lean", time - 4.0F, 0.0F) + track("spine", time - 4.0F, 0.0F);
            tie.rotX = -(leanNow - leanThen) * 2.4F + 5.0F * Mth.sin(time * 0.09F) - leanNow * 0.5F;
        }
        tie.rotZ = 3.2F * Mth.sin(time * 0.062F + 0.6F);
    }
}
