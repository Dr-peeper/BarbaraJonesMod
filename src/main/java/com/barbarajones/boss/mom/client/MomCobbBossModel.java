package com.barbarajones.boss.mom.client;

import com.barbarajones.boss.mom.MomCobbBoss;
import com.barbarajones.boss.mom.MomPhase;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Mom Cobb's boss rig. Built on the stock humanoid skeleton (baked from the
 * player layer, the same trick {@code BarbaraModel} uses) because she is a woman
 * in a house, not a monster - what has to read from across the room is not her
 * silhouette but her INTENT.
 *
 * <p>So this model is almost entirely about telegraphs. Each of her four
 * attacks has its own pose that plays out over the whole wind-up window, driven
 * by {@link MomCobbBoss#getWindupProgress()}, and each idle stance is different
 * per {@link MomPhase} - arms folded while she interrogates you, one finger
 * jabbing while she yells about the game, both hands grabbing once she has
 * decided the Krave is coming with her.
 */
public class MomCobbBossModel extends HumanoidModel<MomCobbBoss> {

    public MomCobbBossModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(MomCobbBoss entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // One model instance is shared by every Mom on screen and reused frame to
        // frame. The stock humanoid rig does not zero these particular channels,
        // so a pose set in one branch would leak into the next entity and the
        // next tick. Clear them before anything below writes to them.
        this.body.xRot = 0.0F;
        this.body.yRot = 0.0F;
        this.body.zRot = 0.0F;
        this.head.zRot = 0.0F;
        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
        this.rightArm.yRot = 0.0F;
        this.leftArm.yRot = 0.0F;

        float t = ageInTicks;

        if (entity.isStaggered()) {
            staggerPose(t);
        } else {
            int windup = entity.getWindupKind();
            float p = entity.getWindupProgress();
            switch (windup) {
                case MomCobbBoss.WINDUP_SWIPE -> swipePose(p, t);
                case MomCobbBoss.WINDUP_THROW -> throwPose(p, t);
                case MomCobbBoss.WINDUP_BLACKOUT -> blackoutPose(p, t);
                case MomCobbBoss.WINDUP_DEVOUR -> devourPose(p, t);
                default -> idlePose(entity.getPhase(), t, limbSwingAmount);
            }
        }

        // The head overlay is a separate part; without this it stays on the
        // un-posed head and detaches whenever she tilts.
        this.hat.copyFrom(this.head);
    }

    /**
     * Short and sharp: the hand snaps up over about the first third of the tell
     * and then hangs there, cocked, for the rest. The hang is the readable part -
     * a pose that is still moving when it lands cannot be reacted to.
     */
    private void swipePose(float p, float t) {
        float raise = Mth.clamp(p * 3.0F, 0.0F, 1.0F);
        this.rightArm.xRot = -2.35F * raise;
        this.rightArm.zRot = -0.55F * raise;
        this.leftArm.zRot = -1.15F * raise;          // other hand goes to the hip
        this.body.xRot = -0.12F * raise;
        this.head.xRot -= 0.16F * raise;
        float tremor = Mth.sin(t * 3.3F) * 0.05F * raise;
        this.rightArm.zRot += tremor;
    }

    /**
     * The full baseball wind-up: the object goes back behind her shoulder, her
     * whole torso coils away from you, and the free arm comes up pointing. Eased
     * so most of the travel happens early and the last third is a held threat.
     */
    private void throwPose(float p, float t) {
        float coil = Mth.sin(Mth.clamp(p * 1.35F, 0.0F, 1.0F) * Mth.PI * 0.5F);

        this.rightArm.xRot = -0.35F - 3.05F * coil;
        this.rightArm.zRot = -0.45F * coil;
        this.leftArm.xRot = -1.35F * coil;
        this.leftArm.zRot = 0.55F * coil;

        this.body.yRot = 0.5F * coil;
        this.body.xRot = -0.1F * coil;
        this.head.yRot -= 0.22F * coil;

        float shudder = Mth.sin(t * 2.9F) * 0.06F * coil;
        this.rightArm.zRot += shudder;
        this.body.zRot = shudder * 0.4F;
    }

    /**
     * Both arms climb overhead and spread, head tipped back. Quadratic so it
     * starts slow and finishes fast - the acceleration is the "here it comes".
     */
    private void blackoutPose(float p, float t) {
        float rise = p * p;

        this.rightArm.xRot = -Mth.PI * 0.96F * rise;
        this.leftArm.xRot = -Mth.PI * 0.96F * rise;
        float spread = 0.40F * rise + Mth.sin(t * 2.1F) * 0.07F * rise;
        this.rightArm.zRot = -spread;
        this.leftArm.zRot = spread;

        this.head.xRot = -0.55F * rise;
        this.body.xRot = -0.18F * rise;
        this.rightLeg.xRot = -0.12F * rise;
        this.leftLeg.xRot = -0.12F * rise;
    }

    /**
     * Hunched over the box with both hands to her face, knees bent, chewing. In
     * the stock humanoid rig the arms are siblings of the body rather than
     * children of it, so the hunch has to be applied to each limb by hand.
     */
    private void devourPose(float p, float t) {
        float hunch = Mth.sin(Mth.clamp(p * 1.6F, 0.0F, 1.0F) * Mth.PI * 0.5F);
        float chew = Mth.sin(t * 2.4F) * 0.13F * hunch;

        this.body.xRot = 0.9F * hunch;
        this.head.xRot = 0.5F * hunch + chew;

        this.rightArm.xRot = -1.95F * hunch - chew;
        this.leftArm.xRot = -1.95F * hunch - chew;
        this.rightArm.zRot = 0.42F * hunch;
        this.leftArm.zRot = -0.42F * hunch;

        this.rightLeg.xRot = -0.55F * hunch;
        this.leftLeg.xRot = -0.55F * hunch;
    }

    /** Empty-handed and reeling. This is the window - it should look like one. */
    private void staggerPose(float t) {
        float reel = Mth.sin(t * 1.7F);

        this.body.xRot = 0.34F;
        this.body.zRot = reel * 0.18F;
        this.head.xRot = 0.30F + Mth.sin(t * 2.6F) * 0.1F;
        this.head.zRot = reel * 0.26F;

        this.rightArm.xRot = 0.55F + reel * 0.4F;
        this.leftArm.xRot = 0.55F - reel * 0.4F;
        this.rightArm.zRot = 0.85F;
        this.leftArm.zRot = -0.85F;

        this.rightLeg.xRot = -0.25F + reel * 0.2F;
        this.leftLeg.xRot = 0.25F - reel * 0.2F;
    }

    /** One stance per act, so you can tell which fight you are in without the bar. */
    private void idlePose(MomPhase phase, float t, float limbSwingAmount) {
        float breath = Mth.sin(t * 0.11F) * 0.035F;
        this.body.xRot += breath;

        switch (phase) {
            case QUESTIONS -> {
                // arms folded, weight on one hip, waiting for an answer
                this.rightArm.xRot = -1.35F;
                this.rightArm.zRot = 0.55F;
                this.leftArm.xRot = -1.35F;
                this.leftArm.zRot = -0.55F;
                this.body.zRot = Mth.sin(t * 0.045F) * 0.06F;
                this.head.yRot += Mth.sin(t * 0.05F) * 0.18F;   // slow scan of the room
            }
            case GAME -> {
                // one finger out, jabbing on the beat of whatever she's saying
                float jab = Mth.sin(t * 0.32F);
                this.rightArm.xRot = -1.55F + jab * 0.28F;
                this.rightArm.zRot = 0.12F;
                this.leftArm.xRot = 0.0F;
                this.leftArm.zRot = -1.30F;                     // hand planted on hip
                this.head.xRot -= 0.12F;
                this.body.xRot += 0.10F + jab * 0.03F;
            }
            case KRAVE -> {
                // both hands out and open. she is not asking for it any more
                float grasp = Mth.sin(t * 0.22F);
                this.rightArm.xRot = -1.25F + grasp * 0.13F;
                this.leftArm.xRot = -1.25F - grasp * 0.13F;
                this.rightArm.zRot = 0.34F + grasp * 0.09F;
                this.leftArm.zRot = -0.34F - grasp * 0.09F;
                this.body.xRot += 0.20F;
                this.head.xRot -= 0.22F;                        // chin down, eyes up
            }
            default -> { }
        }

        // Stock leg swing survives all of the above; just widen the stride so a
        // boss walking at you does not mince.
        if (limbSwingAmount > 0.01F) {
            this.rightLeg.xRot *= 1.25F;
            this.leftLeg.xRot *= 1.25F;
        }
    }
}
