package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.KraveMonster;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * A four-legged, spine-spiked, tailed beast unlike anything else in the mod
 * (or the base game) - built from scratch rather than the shared humanoid rig.
 * It can stand and walk on all fours, or rear up onto its hind legs with its
 * front legs raised like arms; KraveMonster.getRearAmount() drives a smooth
 * blend between the two, and both have their own leg-swing walk cycle.
 */
public class KraveMonsterModel extends HierarchicalModel<KraveMonster> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(BarbaraJonesMod.MODID, "krave_monster"), "main");

    private final ModelPart root;
    private final ModelPart hips;
    private final ModelPart chest;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart tail1;
    private final ModelPart tail2;
    private final ModelPart tail3;

    private final ModelPart frontUpperL;
    private final ModelPart frontLowerL;
    private final ModelPart frontUpperR;
    private final ModelPart frontLowerR;
    private final ModelPart backThighL;
    private final ModelPart backShinL;
    private final ModelPart backThighR;
    private final ModelPart backShinR;

    public KraveMonsterModel(ModelPart root) {
        this.root = root;
        this.hips = root.getChild("hips");
        this.chest = this.hips.getChild("chest");
        this.neck = this.chest.getChild("neck");
        this.head = this.neck.getChild("head");
        this.tail1 = this.hips.getChild("tail1");
        this.tail2 = this.tail1.getChild("tail2");
        this.tail3 = this.tail2.getChild("tail3");

        this.frontUpperL = this.chest.getChild("front_upper_l");
        this.frontLowerL = this.frontUpperL.getChild("front_lower_l");
        this.frontUpperR = this.chest.getChild("front_upper_r");
        this.frontLowerR = this.frontUpperR.getChild("front_lower_r");
        this.backThighL = this.hips.getChild("back_thigh_l");
        this.backShinL = this.backThighL.getChild("back_shin_l");
        this.backThighR = this.hips.getChild("back_thigh_r");
        this.backShinR = this.backThighR.getChild("back_shin_r");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        // ---- spine: hips (root) -> chest -> neck -> head ---------------------
        PartDefinition hips = parts.addOrReplaceChild("hips",
                CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -4.0F, -5.0F, 10.0F, 8.0F, 10.0F),
                PartPose.offset(0.0F, 14.0F, 4.0F));

        PartDefinition chest = hips.addOrReplaceChild("chest",
                CubeListBuilder.create().texOffs(40, 0).addBox(-4.5F, -4.0F, -8.0F, 9.0F, 8.0F, 9.0F),
                PartPose.offset(0.0F, -1.0F, -6.0F));

        PartDefinition neck = chest.addOrReplaceChild("neck",
                CubeListBuilder.create().texOffs(76, 0).addBox(-2.5F, -2.5F, -4.5F, 5.0F, 5.0F, 5.0F),
                PartPose.offset(0.0F, -3.0F, -7.0F));

        PartDefinition head = neck.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 26).addBox(-3.5F, -4.0F, -6.5F, 7.0F, 6.0F, 7.0F)
                        .texOffs(28, 26).addBox(-2.0F, -1.0F, -8.5F, 4.0F, 3.0F, 6.0F)   // jaw/snout
                        .texOffs(64, 26).addBox(-3.0F, -6.5F, -3.5F, 2.0F, 4.0F, 2.0F)   // horn L
                        .texOffs(72, 26).addBox(1.0F, -6.5F, -3.5F, 2.0F, 4.0F, 2.0F),   // horn R
                PartPose.offset(0.0F, -1.0F, -2.5F));

        // ---- tail: three tapering segments off the hips -----------------------
        PartDefinition tail1 = hips.addOrReplaceChild("tail1",
                CubeListBuilder.create().texOffs(0, 64).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 5.0F),
                PartPose.offset(0.0F, -1.0F, 5.0F));
        PartDefinition tail2 = tail1.addOrReplaceChild("tail2",
                CubeListBuilder.create().texOffs(18, 64).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 0.0F, 5.0F));
        tail2.addOrReplaceChild("tail3",
                CubeListBuilder.create().texOffs(32, 64).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, 0.0F, 4.0F));

        // ---- spine spikes: alternating small/large along hips + chest ---------
        CubeListBuilder spikeSm = CubeListBuilder.create().texOffs(42, 64).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F);
        CubeListBuilder spikeLg = CubeListBuilder.create().texOffs(50, 64).addBox(-1.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F);
        hips.addOrReplaceChild("spike_hips_1", spikeLg, PartPose.offset(0.0F, -4.0F, -4.0F));
        hips.addOrReplaceChild("spike_hips_2", spikeSm, PartPose.offset(0.0F, -4.0F, 0.0F));
        chest.addOrReplaceChild("spike_chest_1", spikeLg, PartPose.offset(0.0F, -4.0F, -6.0F));
        chest.addOrReplaceChild("spike_chest_2", spikeSm, PartPose.offset(0.0F, -4.0F, -3.0F));

        // ---- front legs: shoulder (chest) -> upper -> lower -> paw -----------
        CubeListBuilder frontUpper = CubeListBuilder.create().texOffs(0, 46).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F);
        CubeListBuilder frontLower = CubeListBuilder.create().texOffs(16, 46)
                .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F)
                .texOffs(28, 46).addBox(-2.0F, 6.0F, -2.5F, 4.0F, 3.0F, 5.0F);   // paw

        PartDefinition frontUpperL = chest.addOrReplaceChild("front_upper_l",
                frontUpper, PartPose.offset(5.0F, 1.0F, -6.0F));
        frontUpperL.addOrReplaceChild("front_lower_l", frontLower, PartPose.offset(0.0F, 7.0F, 0.0F));

        PartDefinition frontUpperR = chest.addOrReplaceChild("front_upper_r",
                CubeListBuilder.create().texOffs(0, 46).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F),
                PartPose.offset(-5.0F, 1.0F, -6.0F));
        frontUpperR.addOrReplaceChild("front_lower_r",
                CubeListBuilder.create().texOffs(16, 46).mirror()
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F)
                        .texOffs(28, 46).mirror().addBox(-2.0F, 6.0F, -2.5F, 4.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, 7.0F, 0.0F));

        // ---- back legs: hip (hips) -> thigh -> shin+foot ----------------------
        CubeListBuilder backThigh = CubeListBuilder.create().texOffs(46, 46).addBox(-2.5F, 0.0F, -2.5F, 5.0F, 8.0F, 5.0F);
        CubeListBuilder backShin = CubeListBuilder.create().texOffs(66, 46)
                .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F)
                .texOffs(82, 46).addBox(-2.0F, 7.0F, -3.0F, 4.0F, 3.0F, 6.0F);   // foot

        PartDefinition backThighL = hips.addOrReplaceChild("back_thigh_l",
                backThigh, PartPose.offset(5.0F, 1.0F, 4.0F));
        backThighL.addOrReplaceChild("back_shin_l", backShin, PartPose.offset(0.0F, 8.0F, 0.0F));

        PartDefinition backThighR = hips.addOrReplaceChild("back_thigh_r",
                CubeListBuilder.create().texOffs(46, 46).mirror().addBox(-2.5F, 0.0F, -2.5F, 5.0F, 8.0F, 5.0F),
                PartPose.offset(-5.0F, 1.0F, 4.0F));
        backThighR.addOrReplaceChild("back_shin_r",
                CubeListBuilder.create().texOffs(66, 46).mirror()
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F)
                        .texOffs(82, 46).mirror().addBox(-2.0F, 7.0F, -3.0F, 4.0F, 3.0F, 6.0F),
                PartPose.offset(0.0F, 8.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(KraveMonster entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float rear = entity.getRearAmount(1.0F);

        // ---- spine posture: horizontal prowl <-> upright rear -----------------
        this.hips.xRot = Mth.lerp(rear, 0.0F, -0.55F);
        this.hips.y = Mth.lerp(rear, 14.0F, 17.5F);
        this.chest.xRot = Mth.lerp(rear, 0.05F, 0.35F);
        this.neck.xRot = Mth.lerp(rear, 0.15F, -0.25F) + headPitch * ((float) Math.PI / 180F) * 0.5F;
        this.head.xRot = Mth.lerp(rear, 0.0F, -0.15F) + headPitch * ((float) Math.PI / 180F) * 0.5F;
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);

        // idle breathing / tail sway, always on, subtler when walking fast
        float idle = Mth.sin(ageInTicks * 0.08F) * 0.03F;
        this.chest.xRot += idle;
        float tailWag = Mth.sin(ageInTicks * 0.12F) * (0.25F + limbSwingAmount * 0.4F);
        this.tail1.yRot = tailWag;
        this.tail2.yRot = tailWag * 1.3F;
        this.tail3.yRot = tailWag * 1.6F;
        this.tail1.xRot = Mth.lerp(rear, 0.1F, -0.5F);
        this.tail2.xRot = Mth.lerp(rear, 0.05F, -0.2F);

        // ---- walk cycles: quadruped trot (diagonal pairs) vs biped stride -----
        float freq = 0.6662F;
        float quadAmp = 1.1F;
        float bipedAmp = 1.3F;

        float qFrontL = Mth.cos(limbSwing * freq) * quadAmp * limbSwingAmount;
        float qFrontR = Mth.cos(limbSwing * freq + (float) Math.PI) * quadAmp * limbSwingAmount;
        float qBackL = qFrontR;
        float qBackR = qFrontL;

        float bBackL = Mth.cos(limbSwing * freq) * bipedAmp * limbSwingAmount;
        float bBackR = -bBackL;
        float bFrontL = -bBackL;   // arms counter-swing opposite the same-side leg
        float bFrontR = -bBackR;

        this.frontUpperL.xRot = Mth.lerp(rear, qFrontL, bFrontL - 1.6F);
        this.frontUpperR.xRot = Mth.lerp(rear, qFrontR, bFrontR - 1.6F);
        this.frontUpperL.zRot = Mth.lerp(rear, 0.15F, 0.35F);
        this.frontUpperR.zRot = Mth.lerp(rear, -0.15F, -0.35F);
        this.frontLowerL.xRot = Mth.lerp(rear, Math.max(0.0F, -qFrontL) * 0.8F, 0.9F);
        this.frontLowerR.xRot = Mth.lerp(rear, Math.max(0.0F, -qFrontR) * 0.8F, 0.9F);

        this.backThighL.xRot = Mth.lerp(rear, qBackL, bBackL);
        this.backThighR.xRot = Mth.lerp(rear, qBackR, bBackR);
        this.backShinL.xRot = Mth.lerp(rear, Math.max(0.0F, qBackL) * 0.9F + 0.2F, Math.max(0.0F, -bBackL) * 0.7F + 0.1F);
        this.backShinR.xRot = Mth.lerp(rear, Math.max(0.0F, qBackR) * 0.9F + 0.2F, Math.max(0.0F, -bBackR) * 0.7F + 0.1F);
    }
}
