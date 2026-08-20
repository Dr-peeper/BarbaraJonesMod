package com.barbarajones.v2.mobs.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.entity.SoggyEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Soggy: the standard humanoid rig with the torso blown out into a bloated
 * waterlogged barrel and a sagging belly cube tacked on the front - the
 * "bloated, waterlogged" read from the brief. Texture 64x96: rows 64-96 hold
 * the oversized body and the belly bulge, since both are bigger than the
 * vanilla body's UV allocation (see tools/make_kraveling_mobs.ps1).
 */
public class SoggyModel extends HumanoidModel<SoggyEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(BarbaraJonesMod.MODID, "soggy"), "main");

    private final ModelPart belly;

    public SoggyModel(ModelPart root) {
        super(root);
        this.belly = root.getChild("body").getChild("belly");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        // UV block (0,64)-(32,82): 10 wide x 12 tall x 6 deep bloated torso.
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 64).addBox(-5.0F, 0.0F, -3.0F, 10.0F, 12.0F, 6.0F),
                PartPose.ZERO);

        // UV block (32,64)-(44,70): 4x4x2 belly sag on the front, low on the torso.
        body.addOrReplaceChild("belly",
                CubeListBuilder.create().texOffs(32, 64).addBox(-2.0F, -1.0F, -0.5F, 4.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 8.0F, -3.0F));

        // Slightly stubbier limbs to match the wider torso - still the
        // standard UV regions, just a touch thicker (5 vs vanilla 4).
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-2.5F, -2.0F, -2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.offset(-5.5F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(32, 48).mirror().addBox(-2.5F, -2.0F, -2.5F, 5.0F, 12.0F, 5.0F),
                PartPose.offset(5.5F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 96);
    }

    @Override
    public void setupAnim(SoggyEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // heavy, slow waterlogged wobble - torso and belly lag behind the walk
        this.body.xRot = Mth.sin(ageInTicks * 0.08F) * 0.03F;
        this.belly.zRot = Mth.sin(ageInTicks * 0.15F) * 0.05F;
        this.belly.xRot = Mth.cos(ageInTicks * 0.13F) * 0.04F;
    }
}
