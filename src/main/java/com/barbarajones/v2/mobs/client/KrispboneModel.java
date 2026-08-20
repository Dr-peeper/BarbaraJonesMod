package com.barbarajones.v2.mobs.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.entity.KrispboneEntity;

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

/**
 * Krispbone: same standard humanoid texture layout as
 * {@link CravelingModel} (64x64, no extra rows needed) but with the torso and
 * all four limbs replaced by genuinely narrower boxes - a real geometry
 * difference, not a reskin, reading as a dry hollow ribcage-and-bone frame
 * rather than a full body. The head keeps its normal box (painted as a
 * cracked skull face by the texture).
 */
public class KrispboneModel extends HumanoidModel<KrispboneEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(BarbaraJonesMod.MODID, "krispbone"), "main");

    public KrispboneModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        // Thin ribcage torso: 6 wide x 3 deep (vs the vanilla 8x4), still the
        // standard 12 tall. Same texOffs region as the vanilla body (16,16) -
        // it's just not fully painted, matching the smaller box.
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(16, 16).addBox(-3.0F, 0.0F, -1.5F, 6.0F, 12.0F, 3.0F),
                PartPose.ZERO);

        // Thin limbs: 2x2 (vs vanilla 4x4), full 12 length, offset in from the
        // shoulder/hip a little tighter than default so they don't float away
        // from the narrower torso.
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(40, 16).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(-4.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(32, 48).mirror().addBox(-1.0F, -2.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(4.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(0, 16).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(16, 48).mirror().addBox(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
