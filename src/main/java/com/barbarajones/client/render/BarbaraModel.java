package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.BarbaraJones;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Barbara's rig, with the lit joint welded to her face.
 *
 * <p>The 1.7.10 model had a 1x1x4 spliff poking out of the corner of her mouth,
 * mapped at UV (0,48). The port dropped it for the stock player model and she
 * has been smokeless ever since - even though the artwork for it is still
 * sitting in barbara.png untouched. This builds her own layer again so the
 * joint comes back as real geometry rather than a texture on her cheek.
 *
 * <p>Limb UVs stay on the modern 64x64 layout (left arm at 32,48, left leg at
 * 16,48) because the current skin is the photo-derived one converted to that
 * format - using the old legacy offsets would map her face onto her shins.
 *
 * <p>She also has three quite different moods and the stock animation renders
 * them identically, so each one is posed distinctly enough to read from across
 * a field.
 */
public class BarbaraModel extends HumanoidModel<BarbaraJones> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(BarbaraJonesMod.MODID, "barbara"), "main");

    private final ModelPart joint;
    private final ModelPart ember;

    public BarbaraModel(ModelPart root) {
        super(root);
        ModelPart head = root.getChild(PartNames.HEAD);
        this.joint = head.getChild("joint");
        this.ember = this.joint.getChild("ember");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.getChild(PartNames.HEAD);
        // 1x1x4 spliff out of the corner of her mouth, angled slightly down and
        // out so it does not read as a stick growing from her chin.
        PartDefinition joint = head.addOrReplaceChild("joint",
                CubeListBuilder.create().texOffs(0, 48)
                        .addBox(1.0F, -3.0F, -8.0F, 1.0F, 1.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.08F, 0.0F, 0.0F));

        // The coal. Tiny, and drawn from a single dark pixel of the sheet - it is
        // recoloured and lit in the renderer layer, never sampled for detail.
        joint.addOrReplaceChild("ember",
                CubeListBuilder.create().texOffs(0, 53)
                        .addBox(1.0F, -3.05F, -8.6F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.02F)),
                PartPose.ZERO);

        // Modern 64x64 left-side offsets. HumanoidModel.createMesh already uses
        // these, so the limbs are left exactly as vanilla builds them.
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(BarbaraJones entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        float t = ageInTicks;

        // The joint rides the head, and the coal breathes rather than glowing flat.
        this.joint.visible = true;
        float glow = 0.6F + Mth.sin(t * 0.35F) * 0.4F;
        this.ember.visible = entity.isHigh() || glow > 0.35F;

        if (entity.isRaging()) {
            // ---- PSYCHO -----------------------------------------------------
            // Hunched, arms thrown back, juddering on a fast irregular cycle.
            float judder = Mth.sin(t * 2.7F) * 0.06F + Mth.sin(t * 4.1F) * 0.03F;

            this.body.xRot = 0.42F + judder;
            this.head.xRot -= 0.30F;
            this.head.zRot = Mth.sin(t * 1.9F) * 0.16F;

            this.rightArm.xRot = 1.15F + Mth.sin(t * 1.6F) * 0.85F;
            this.leftArm.xRot = 1.15F + Mth.sin(t * 1.6F + 2.2F) * 0.85F;
            this.rightArm.zRot = 0.75F + Mth.cos(t * 1.3F) * 0.28F;
            this.leftArm.zRot = -0.75F - Mth.cos(t * 1.3F + 1.1F) * 0.28F;

            if (limbSwingAmount > 0.01F) {
                float stride = Mth.cos(limbSwing * 0.6662F) * 1.7F * limbSwingAmount;
                this.rightLeg.xRot = stride;
                this.leftLeg.xRot = -stride;
            }
            this.body.zRot = judder * 0.5F;

        } else if (entity.isHigh()) {
            // ---- HIGH -------------------------------------------------------
            // Long slow sway, lolling head, arms hanging heavy, feet barely lifting.
            float sway = Mth.sin(t * 0.11F);
            float drift = Mth.sin(t * 0.07F + 1.3F);

            this.body.zRot = sway * 0.17F;
            this.body.xRot = 0.10F + drift * 0.05F;
            this.head.zRot = sway * 0.30F;
            this.head.xRot += 0.20F + drift * 0.10F;
            this.head.yRot += drift * 0.22F;

            this.rightArm.xRot = 0.18F + sway * 0.22F;
            this.leftArm.xRot = 0.18F - sway * 0.22F;
            this.rightArm.zRot = 0.22F + sway * 0.13F;
            this.leftArm.zRot = -0.22F + sway * 0.13F;

            this.rightLeg.xRot *= 0.45F;
            this.leftLeg.xRot *= 0.45F;

        } else {
            // ---- CALM -------------------------------------------------------
            // Breathing, weight shifting, and every so often a hand up for a drag.
            float breath = Mth.sin(t * 0.09F) * 0.03F;
            this.body.xRot += breath;
            this.head.y = breath * 0.7F;

            float shift = Mth.sin(t * 0.05F);
            this.body.zRot = shift * 0.045F;
            this.head.zRot = -shift * 0.05F;

            this.rightArm.zRot += 0.06F;
            this.leftArm.zRot -= 0.06F;

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
