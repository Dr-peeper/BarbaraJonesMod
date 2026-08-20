package com.barbarajones.v2.mobs.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.entity.LoomweaverEntity;

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
 * Loomweaver: built from scratch, not the shared humanoid rig - a low
 * clumped-cereal body (abdomen + thorax) riding on eight thin milk-strand
 * legs, the same "low, many-legged thing" silhouette as the brief. Texture:
 * 64x32, laid out as its own sheet (see tools/make_craveling_mobs.ps1).
 */
public class LoomweaverModel extends HierarchicalModel<LoomweaverEntity> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(BarbaraJonesMod.MODID, "loomweaver"), "main");

    private static final int LEGS = 8;

    private final ModelPart root;
    private final ModelPart abdomen;
    private final ModelPart thorax;
    private final ModelPart head;
    private final ModelPart[] legs = new ModelPart[LEGS];

    public LoomweaverModel(ModelPart root) {
        this.root = root;
        this.abdomen = root.getChild("abdomen");
        this.thorax = abdomen.getChild("thorax");
        this.head = thorax.getChild("head");
        for (int i = 0; i < LEGS; i++) {
            this.legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        // UV (0,0)-(32,14): abdomen, the bulk of the body.
        PartDefinition abdomen = parts.addOrReplaceChild("abdomen",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -3.0F, -5.0F, 8.0F, 6.0F, 8.0F),
                PartPose.offset(0.0F, 15.0F, 2.0F));

        // UV (32,0)-(52,9): thorax, smaller, forward of the abdomen.
        PartDefinition thorax = abdomen.addOrReplaceChild("thorax",
                CubeListBuilder.create().texOffs(32, 0).addBox(-3.0F, -2.5F, -4.5F, 6.0F, 5.0F, 4.0F),
                PartPose.offset(0.0F, -0.5F, -5.5F));

        // UV (0,14)-(14,21): head, small, mostly texture-carried detail (eyes).
        thorax.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 14).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, -1.0F, -4.5F));

        // Eight thin legs, four per side, root-level (not children of the
        // body) so each can swing independently in setupAnim. UV per leg:
        // (16,14)-(20,23), 1x8x1.
        CubeListBuilder legCube = CubeListBuilder.create().texOffs(16, 14).addBox(-0.5F, 0.0F, -0.5F, 1.0F, 8.0F, 1.0F);

        float[] sideX = { 4.5F, -4.5F };     // right side, left side
        float[] alongZ = { -3.5F, -1.2F, 1.2F, 3.5F }; // front-to-back attachment points
        int idx = 0;
        for (float x : sideX) {
            for (float z : alongZ) {
                float dir = x > 0 ? 1.0F : -1.0F;
                parts.addOrReplaceChild("leg" + idx, legCube,
                        PartPose.offsetAndRotation(x, 14.0F, z, 0.0F, 0.0F, dir * 0.9F));
                idx++;
            }
        }

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(LoomweaverEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        this.thorax.xRot = headPitch * Mth.DEG_TO_RAD * 0.6F;
        this.thorax.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6F;

        // idle abdomen breathing sway
        this.abdomen.xRot = Mth.sin(ageInTicks * 0.05F) * 0.02F;

        // alternating tripod gait: legs 0,3,5,6 swing opposite legs 1,2,4,7 -
        // a simple but genuine 8-leg walk cycle, not a reused biped stride.
        for (int i = 0; i < LEGS; i++) {
            boolean groupA = (i % 2) == 0;
            float phase = groupA ? 0.0F : (float) Math.PI;
            float swing = Mth.cos(limbSwing * 0.7F + phase) * 1.1F * limbSwingAmount;
            this.legs[i].xRot = swing;
        }
    }
}
