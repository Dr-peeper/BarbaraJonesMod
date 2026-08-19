package com.barbarajones.boss.manager;

import com.barbarajones.BarbaraJonesMod;

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
 * CLIENT ONLY - reached solely from ClientSetup, which is {@code Dist.CLIENT}
 * guarded, so this class is never loaded on a dedicated server.
 *
 * <p>The standard humanoid rig plus the two things that make The Manager The
 * Manager: a shirt collar and a necktie that hangs off the chest. During a
 * wind-up his arms come up like he is presenting a slide and the tie floats,
 * which is the silhouette half of the telegraph (the particle ring is the
 * other half).
 */
public class ManagerModel extends HumanoidModel<TheManager> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(BarbaraJonesMod.MODID, "the_manager"), "main");

    private final ModelPart tie;

    public ManagerModel(ModelPart root) {
        super(root);
        this.tie = root.getChild("body").getChild("tie");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition body = mesh.getRoot().getChild("body");

        // Both extras sit at z -2.99..-1.99: the back face is buried a hundredth
        // of a pixel INSIDE the torso rather than flush with it, because two
        // coplanar faces z-fight and flicker at range.
        body.addOrReplaceChild("collar",
                CubeListBuilder.create().texOffs(0, 44)
                        .addBox(-4.0F, 0.0F, -2.99F, 8.0F, 2.0F, 1.0F),
                PartPose.offset(0.0F, 0.5F, 0.0F));

        body.addOrReplaceChild("tie",
                CubeListBuilder.create().texOffs(0, 32)
                        .addBox(-1.5F, 0.0F, -2.99F, 3.0F, 9.0F, 1.0F),
                PartPose.offset(0.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(TheManager entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        float windup = entity.isWindingUp() ? entity.getWindupProgress() : 0.0F;

        // arms up and out, further the closer the attack is to landing
        float raise = windup * 2.1F;
        this.rightArm.xRot -= raise;
        this.leftArm.xRot -= raise;
        this.rightArm.zRot = -0.25F * windup;
        this.leftArm.zRot = 0.25F * windup;

        // an impatient shift of weight even when he is doing nothing at all
        this.body.yRot = Mth.sin(ageInTicks * 0.05F) * 0.06F;

        // the tie: gentle sway idle, kicked up and forward mid-telegraph
        this.tie.xRot = Mth.sin(ageInTicks * 0.11F) * 0.10F - windup * 0.9F;
        this.tie.zRot = Mth.cos(ageInTicks * 0.07F) * 0.08F;
    }
}
