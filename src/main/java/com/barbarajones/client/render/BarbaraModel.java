package com.barbarajones.client.render;

import com.barbarajones.entity.BarbaraJones;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Barbara's rig. She has three quite different states and the stock humanoid
 * animation renders all of them identically, so you cannot tell at a glance
 * whether she is calm, high or about to kill you. This makes each one read
 * from across a field.
 */
public class BarbaraModel extends HumanoidModel<BarbaraJones> {

    public BarbaraModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(BarbaraJones entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        float t = ageInTicks;

        if (entity.isRaging()) {
            // ---- PSYCHO -----------------------------------------------------
            // Hunched forward, arms thrown out behind, head jutting. The whole
            // body judders on a fast cycle that never quite repeats cleanly.
            float judder = Mth.sin(t * 2.7F) * 0.06F + Mth.sin(t * 4.1F) * 0.03F;

            this.body.xRot = 0.42F + judder;
            this.head.xRot -= 0.30F;                 // chin out
            this.head.zRot = Mth.sin(t * 1.9F) * 0.16F;

            // arms flung back and flailing out of phase with each other
            this.rightArm.xRot = 1.15F + Mth.sin(t * 1.6F) * 0.85F;
            this.leftArm.xRot = 1.15F + Mth.sin(t * 1.6F + 2.2F) * 0.85F;
            this.rightArm.zRot = 0.75F + Mth.cos(t * 1.3F) * 0.28F;
            this.leftArm.zRot = -0.75F - Mth.cos(t * 1.3F + 1.1F) * 0.28F;

            // a wide, over-driven stride
            if (limbSwingAmount > 0.01F) {
                float stride = Mth.cos(limbSwing * 0.6662F) * 1.7F * limbSwingAmount;
                this.rightLeg.xRot = stride;
                this.leftLeg.xRot = -stride;
            }
            this.body.zRot = judder * 0.5F;

        } else if (entity.isHigh()) {
            // ---- HIGH -------------------------------------------------------
            // Everything slows and loosens: a long sway, a lolling head, arms
            // hanging heavy and drifting.
            float sway = Mth.sin(t * 0.11F);
            float drift = Mth.sin(t * 0.07F + 1.3F);

            this.body.zRot = sway * 0.17F;
            this.body.xRot = 0.10F + drift * 0.05F;
            this.head.zRot = sway * 0.30F;           // head rolls with the sway
            this.head.xRot += 0.20F + drift * 0.10F;
            this.head.yRot += drift * 0.22F;

            this.rightArm.xRot = 0.18F + sway * 0.22F;
            this.leftArm.xRot = 0.18F - sway * 0.22F;
            this.rightArm.zRot = 0.22F + sway * 0.13F;
            this.leftArm.zRot = -0.22F + sway * 0.13F;

            // she does not pick her feet up
            this.rightLeg.xRot *= 0.45F;
            this.leftLeg.xRot *= 0.45F;

        } else {
            // ---- CALM -------------------------------------------------------
            // Slow breathing, weight shifting foot to foot, one arm drifting up
            // toward her mouth now and then as if taking a drag.
            float breath = Mth.sin(t * 0.09F) * 0.03F;
            this.body.xRot += breath;
            this.head.y = breath * 0.7F;

            float shift = Mth.sin(t * 0.05F);
            this.body.zRot = shift * 0.045F;
            this.head.zRot = -shift * 0.05F;

            this.rightArm.zRot += 0.06F;
            this.leftArm.zRot -= 0.06F;

            // the drag: a slow rise and fall on a long cycle, only while standing
            float dragCycle = (t * 0.012F) % 1.0F;
            if (limbSwingAmount < 0.05F && dragCycle < 0.30F) {
                float lift = Mth.sin(dragCycle / 0.30F * Mth.PI);
                this.rightArm.xRot -= 1.55F * lift;
                this.rightArm.zRot += 0.32F * lift;
                this.head.xRot -= 0.10F * lift;
            }
        }
    }
}
