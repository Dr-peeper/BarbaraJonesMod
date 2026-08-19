package com.barbarajones.cinematic.actor;

import net.minecraft.util.Mth;

/**
 * A fully articulated humanoid built from boxes: hips, torso, neck, head with a
 * hinged jaw and a mouth cavity behind it, two-segment arms with hands and
 * two-segment legs with feet.
 *
 * <p>Two segments per limb is the whole point. A one-bone arm can only point; an
 * arm with an elbow can wind up, reach and follow through, and a leg with a knee
 * can plant weight. That extra joint is the difference between a mannequin being
 * dragged around and a performance.
 *
 * <p>The model faces <b>+Z</b> and stands on y=0, about 2.4 units tall, so an
 * actor's {@code scale} reads as "height in blocks divided by 2.4". Facing +Z is
 * what makes {@link CinematicActor#faceTowards} correct.
 *
 * <p>Tracks a scene may author, each prefixed with the actor id:
 * {@code headPitch, headYaw, jaw, spine, lean, armR, armL, elbowR, elbowL,
 * legR, legL, kneeR, kneeL, bob, walk, breathe}.
 */
public abstract class HumanoidActor extends CinematicActor {

    protected HumanoidActor(int skinRgb, int hairRgb, int shirtRgb, int trouserRgb, int shoeRgb) {
        Part hips = part("hips", this.root, 0.0F, 1.15F, 0.0F);
        hips.box(-0.28F, -0.14F, -0.15F, 0.56F, 0.28F, 0.30F).colour(trouserRgb);

        Part torso = part("torso", hips, 0.0F, 0.14F, 0.0F);
        torso.box(-0.30F, 0.0F, -0.16F, 0.60F, 0.58F, 0.32F).colour(shirtRgb);

        Part neck = part("neck", torso, 0.0F, 0.58F, 0.0F);
        neck.box(-0.08F, 0.0F, -0.08F, 0.16F, 0.09F, 0.16F).colour(skinRgb);

        Part head = part("head", neck, 0.0F, 0.09F, 0.0F);
        head.box(-0.21F, 0.0F, -0.19F, 0.42F, 0.42F, 0.38F).colour(skinRgb);

        // Hair is its own cap so it can take a different colour, and the trailing
        // lock behind it is a separate joint so it can lag the head.
        part("hair", head, 0.0F, 0.0F, 0.0F)
                .box(-0.23F, 0.16F, -0.21F, 0.46F, 0.30F, 0.34F).colour(hairRgb);
        part("hairTail", head, 0.0F, 0.20F, -0.21F)
                .box(-0.14F, -0.52F, -0.10F, 0.28F, 0.52F, 0.10F).colour(hairRgb);

        part("eyeR", head, 0.10F, 0.25F, 0.20F)
                .box(-0.055F, -0.03F, -0.02F, 0.11F, 0.06F, 0.02F).colour(0x120C10);
        part("eyeL", head, -0.10F, 0.25F, 0.20F)
                .box(-0.055F, -0.03F, -0.02F, 0.11F, 0.06F, 0.02F).colour(0x120C10);

        // The dark of the mouth sits strictly inside the closed jaw, so it is
        // invisible until the jaw swings clear of it - no alpha tricks needed.
        part("mouth", head, 0.0F, 0.11F, 0.03F)
                .box(-0.15F, -0.09F, 0.0F, 0.30F, 0.09F, 0.14F).colour(0x1A0206);

        // Hinged at the back of the skull, so opening swings the chin down and
        // forward exactly the way a mandible does.
        part("jaw", head, 0.0F, 0.11F, -0.14F)
                .box(-0.19F, -0.11F, 0.0F, 0.38F, 0.11F, 0.33F).colour(skinRgb);

        buildArm("R", torso, 0.36F, skinRgb, shirtRgb);
        buildArm("L", torso, -0.36F, skinRgb, shirtRgb);
        buildLeg("R", hips, 0.15F, trouserRgb, shoeRgb);
        buildLeg("L", hips, -0.15F, trouserRgb, shoeRgb);
    }

    private void buildArm(String side, Part torso, float x, int skinRgb, int shirtRgb) {
        Part upper = part("arm" + side, torso, x, 0.50F, 0.0F);
        upper.box(-0.09F, -0.42F, -0.09F, 0.18F, 0.42F, 0.18F).colour(shirtRgb);
        Part fore = part("elbow" + side, upper, 0.0F, -0.42F, 0.0F);
        fore.box(-0.08F, -0.40F, -0.08F, 0.16F, 0.40F, 0.16F).colour(skinRgb);
        part("hand" + side, fore, 0.0F, -0.40F, 0.0F)
                .box(-0.08F, -0.15F, -0.06F, 0.16F, 0.15F, 0.13F).colour(skinRgb);
    }

    private void buildLeg(String side, Part hips, float x, int trouserRgb, int shoeRgb) {
        Part thigh = part("leg" + side, hips, x, -0.14F, 0.0F);
        thigh.box(-0.11F, -0.45F, -0.11F, 0.22F, 0.45F, 0.22F).colour(trouserRgb);
        Part shin = part("knee" + side, thigh, 0.0F, -0.45F, 0.0F);
        shin.box(-0.10F, -0.45F, -0.10F, 0.20F, 0.45F, 0.20F).colour(trouserRgb);
        part("foot" + side, shin, 0.0F, -0.45F, 0.0F)
                .box(-0.11F, -0.10F, -0.18F, 0.22F, 0.10F, 0.32F).colour(shoeRgb);
    }

    @Override
    protected void pose(float time) {
        Part hips = get("hips");
        Part torso = get("torso");

        hips.rotX = track("lean", time, 0.0F);
        hips.rotZ = 0.0F;
        hips.offY = track("bob", time, 0.0F);
        torso.rotX = track("spine", time, 0.0F);
        torso.rotY = 0.0F;

        // Breathing is a swell, not a lift. A torso that slides up and down looks
        // like a lift; one that expands looks like something with lungs.
        float breathe = track("breathe", time, 1.0F);
        torso.scaleY = breathe + 0.012F * Mth.sin(time * 0.16F);
        torso.scaleZ = breathe + 0.020F * Mth.sin(time * 0.16F);

        get("head").rotX = track("headPitch", time, 0.0F);
        get("head").rotY = track("headYaw", time, 0.0F);
        get("jaw").rotX = Math.max(0.0F, track("jaw", time, 0.0F));

        get("armR").rotX = track("armR", time, 4.0F);
        get("armL").rotX = track("armL", time, 4.0F);
        get("armR").rotZ = track("armSpreadR", time, -3.0F);
        get("armL").rotZ = track("armSpreadL", time, 3.0F);
        // Elbows only bend one way. Clamping here means a scene can never author
        // a limb inside out by getting a sign wrong.
        get("elbowR").rotX = -Math.abs(track("elbowR", time, 6.0F));
        get("elbowL").rotX = -Math.abs(track("elbowL", time, 6.0F));

        get("legR").rotX = track("legR", time, 0.0F);
        get("legL").rotX = track("legL", time, 0.0F);
        get("kneeR").rotX = -Math.abs(track("kneeR", time, 2.0F));
        get("kneeL").rotX = -Math.abs(track("kneeL", time, 2.0F));

        float walk = track("walk", time, 0.0F);
        if (walk > 0.0F) {
            walkCycle(time, walk);
        }

        // Follow-through: the hair lock is driven by how much the head moved over
        // the last few ticks, so it whips when the head snaps and keeps swinging
        // after the head has stopped. Sampling the head's own track in the past is
        // the cheapest honest spring there is.
        float headNow = track("headPitch", time, 0.0F);
        float headThen = track("headPitch", time - 3.0F, 0.0F);
        get("hairTail").rotX = -(headNow - headThen) * 1.9F
                + 3.5F * Mth.sin(time * 0.11F) - headNow * 0.25F;
        get("hairTail").rotZ = 2.6F * Mth.sin(time * 0.083F + 1.1F);
    }

    /**
     * A stride, phase-locked to time. Arms counter-swing the legs, the knee only
     * folds on the recovery half, and the hips dip on every plant - that dip is
     * what puts a giant's weight into the step. Overrides whatever the limb
     * tracks said, so a scene turns walking on and off with one track.
     */
    protected void walkCycle(float time, float speed) {
        float phase = time * speed * 0.16F;
        float swing = Mth.sin(phase) * 26.0F;
        get("legR").rotX = swing;
        get("legL").rotX = -swing;
        get("kneeR").rotX = -Math.max(0.0F, Mth.sin(phase - 1.2F)) * 42.0F;
        get("kneeL").rotX = -Math.max(0.0F, Mth.sin(phase - 1.2F + Mth.PI)) * 42.0F;
        get("armR").rotX = -swing * 0.62F;
        get("armL").rotX = swing * 0.62F;
        get("elbowR").rotX = -14.0F - Math.max(0.0F, -Mth.sin(phase)) * 16.0F;
        get("elbowL").rotX = -14.0F - Math.max(0.0F, Mth.sin(phase)) * 16.0F;
        get("hips").offY -= Math.abs(Mth.sin(phase)) * 0.055F;
        get("hips").rotZ = Mth.cos(phase) * 3.0F;
        get("torso").rotY = -Mth.cos(phase) * 5.0F;
    }
}
