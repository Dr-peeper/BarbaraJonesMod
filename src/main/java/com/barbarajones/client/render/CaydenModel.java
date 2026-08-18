package com.barbarajones.client.render;

import com.barbarajones.entity.CaydenCobb;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Cayden's rig. The stock humanoid animation is a flat pendulum swing; this
 * adds the things that make him read as a person rather than a mannequin -
 * breathing, a heavier stride the fatter he gets, arms that actually wind up
 * when he attacks, and a distinct airborne pose once he ascends.
 */
public class CaydenModel extends HumanoidModel<CaydenCobb> {

    public CaydenModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(CaydenCobb entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        boolean ssj = entity.isSuperSaiyan();
        float t = ageInTicks;

        // ---- breathing ------------------------------------------------------
        // Faster and deeper when ascended: he is running hot.
        float breathRate = ssj ? 0.42F : 0.12F;
        float breathDepth = ssj ? 0.055F : 0.022F;
        float breath = Mth.sin(t * breathRate) * breathDepth;
        this.body.xRot += breath * 0.5F;
        this.head.y = breath * 0.6F;
        this.body.y = breath * 0.4F;

        // ---- weight: the more Krave he has eaten, the heavier the stride ----
        // Arms get pushed out by his belly and the walk turns into a waddle.
        float fat = Mth.clamp(entity.getFatScale() - 1.0F, 0.0F, 1.2F);
        this.rightArm.zRot += 0.12F + fat * 0.55F;
        this.leftArm.zRot -= 0.12F + fat * 0.55F;
        if (fat > 0.02F && limbSwingAmount > 0.01F) {
            float waddle = Mth.cos(limbSwing * 0.6662F) * fat * 0.22F * limbSwingAmount;
            this.body.zRot = waddle;
            this.head.zRot = -waddle * 0.6F;
        } else {
            this.body.zRot = 0.0F;
        }

        // ---- attack wind-up -------------------------------------------------
        // Vanilla's attackTime is a single quick chop. Give it an arc: he leans
        // in, swings across, and the off arm counterbalances behind him.
        if (this.attackTime > 0.0F) {
            float a = this.attackTime;
            float arc = Mth.sin(a * (float) Math.PI);
            this.rightArm.xRot = -2.2F * arc - 0.3F;
            this.rightArm.yRot = -0.5F * arc;
            this.leftArm.xRot = 0.6F * arc;
            this.body.yRot = 0.35F * arc;
            this.head.yRot += 0.25F * arc;
        }

        // ---- ascended pose --------------------------------------------------
        if (ssj) {
            // Arms swept back and down, chest open, legs trailing - the classic
            // hovering silhouette. Blend it in over the ordinary walk cycle.
            float hover = 0.75F;
            this.rightArm.xRot = Mth.lerp(hover, this.rightArm.xRot, 0.55F);
            this.leftArm.xRot = Mth.lerp(hover, this.leftArm.xRot, 0.55F);
            this.rightArm.zRot = Mth.lerp(hover, this.rightArm.zRot, 0.85F);
            this.leftArm.zRot = Mth.lerp(hover, this.leftArm.zRot, -0.85F);

            this.rightLeg.xRot = Mth.lerp(hover, this.rightLeg.xRot, -0.28F);
            this.leftLeg.xRot = Mth.lerp(hover, this.leftLeg.xRot, -0.16F);
            this.rightLeg.zRot = 0.10F;
            this.leftLeg.zRot = -0.10F;

            // constant low-amplitude tremor: the power will not sit still
            float tremor = Mth.sin(t * 1.9F) * 0.02F;
            this.body.xRot += tremor;
            this.head.xRot += tremor * 0.5F;
            this.rightArm.zRot += tremor;
            this.leftArm.zRot -= tremor;

            // forward lean while actually moving
            if (limbSwingAmount > 0.05F) {
                this.body.xRot += 0.22F * limbSwingAmount;
                this.head.xRot -= 0.18F * limbSwingAmount;
            }
        }
    }
}
