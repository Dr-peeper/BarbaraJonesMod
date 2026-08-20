package com.barbarajones.v2.mobs.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.entity.MascotEntity;

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
 * The Mascot: a standard humanoid body wearing an oversized cereal-box
 * costume head - a literal rectangular box instead of a rounded head, sized
 * to actually read as a box someone is wearing over their real head, not just
 * a slightly bigger cube. Texture 64x96: rows 64-96 hold the box head, which
 * is far bigger than the vanilla head's UV allocation.
 */
public class MascotModel extends HumanoidModel<MascotEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(BarbaraJonesMod.MODID, "the_mascot"), "main");

    public MascotModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        // UV block (0,64)-(32,82): the box head. 10 wide x 12 tall x 6 deep,
        // sitting a little proud of where a normal head would be so it reads
        // as a costume box worn OVER a head, not the head itself.
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 64).addBox(-5.0F, -13.0F, -3.0F, 10.0F, 12.0F, 6.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 96);
    }

    @Override
    public void setupAnim(MascotEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // an exaggerated, unsettlingly cheerful idle bob/sway on top of
        // whatever the base rig is already doing (walk, look-at, etc.)
        this.head.y += Mth.sin(ageInTicks * 0.18F) * 0.6F;
        this.head.zRot += Mth.sin(ageInTicks * 0.09F) * 0.08F;
        float wave = Mth.sin(ageInTicks * 0.25F) * 0.6F + 0.4F;
        this.rightArm.zRot = -wave;
        this.rightArm.xRot = -0.3F;
    }
}
