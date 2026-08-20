package com.barbarajones.v2.mobs.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.entity.KravelingEntity;

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
 * Kraveling: the standard humanoid rig (so it walks/attacks like a person)
 * PLUS three blocky "chunk" cubes glued onto the shoulders/chest and one
 * small detached crumb floating just above the head - the silhouette tell
 * that this is a body built out of loose cereal pieces, not a person in a
 * costume. Texture is 64x80: the extra 16 rows below the standard 64x64 skin
 * layout hold the four chunk cubes (see tools/make_kraveling_mobs.ps1, which
 * mirrors every texOffs/size pair below exactly).
 */
public class KravelingModel extends HumanoidModel<KravelingEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(BarbaraJonesMod.MODID, "kraveling"), "main");

    private final ModelPart floatingCrumb;

    public KravelingModel(ModelPart root) {
        super(root);
        this.floatingCrumb = root.getChild("head").getChild("floating_crumb");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.getChild("body");
        PartDefinition head = root.getChild("head");

        // UV block (0,64)-(8,68): 2x2x2 cube.
        body.addOrReplaceChild("chunk_shoulder_l",
                CubeListBuilder.create().texOffs(0, 64).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(4.5F, -1.0F, -2.0F, 0.0F, 0.0F, 0.35F));

        // UV block (8,64)-(16,68): 2x2x2 cube, mirrored to the other shoulder.
        body.addOrReplaceChild("chunk_shoulder_r",
                CubeListBuilder.create().texOffs(8, 64).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offsetAndRotation(-4.5F, -1.0F, -2.0F, 0.0F, 0.0F, -0.35F));

        // UV block (16,64)-(24,67): 3x2x1 cube, chest.
        body.addOrReplaceChild("chunk_chest",
                CubeListBuilder.create().texOffs(16, 64).addBox(-1.5F, -1.0F, -0.5F, 3.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 4.0F, -2.5F));

        // UV block (24,64)-(32,68): 2x2x2 cube, a loose crumb hovering just off
        // the top of the head - the "crumbs falling as it walks" detail read
        // from a distance.
        head.addOrReplaceChild("floating_crumb",
                CubeListBuilder.create().texOffs(24, 64).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                PartPose.offset(3.5F, -5.5F, 1.0F));

        return LayerDefinition.create(mesh, 64, 80);
    }

    @Override
    public void setupAnim(KravelingEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        // the loose crumb drifts independently of the head's look rotation
        this.floatingCrumb.y = -5.5F + Mth.sin(ageInTicks * 0.1F) * 0.4F;
        this.floatingCrumb.xRot = ageInTicks * 0.05F;
        this.floatingCrumb.yRot = ageInTicks * 0.07F;
    }
}
