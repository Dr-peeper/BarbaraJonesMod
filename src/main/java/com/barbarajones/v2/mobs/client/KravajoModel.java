package com.barbarajones.v2.mobs.client;

import com.barbarajones.v2.mobs.entity.KravajoEntity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * A cereal flake with wings and a grudge.
 *
 * <p>Built flat and wide rather than as a small animal, because the silhouette
 * has to read from directly below - that is the angle you see it from for most
 * of its life, hanging over your head before it drops. A rounded body would be
 * an unidentifiable dot up there; a flat flake with two paddle wings still says
 * "cereal" at that distance.
 *
 * <p>Parts hang off the mesh root directly rather than under a wrapper part. The
 * villager model in this mod wrapped everything in a "root" and then read its
 * children off the mesh root, which does not exist there, and that took the
 * client down at model bake. Not repeating it.
 */
public class KravajoModel extends EntityModel<KravajoEntity> {

    public static final net.minecraft.client.model.geom.ModelLayerLocation LAYER_LOCATION =
            new net.minecraft.client.model.geom.ModelLayerLocation(
                    new net.minecraft.resources.ResourceLocation(
                            com.barbarajones.BarbaraJonesMod.MODID, "kravajo"), "main");


    private final ModelPart body;
    private final ModelPart wingLeft;
    private final ModelPart wingRight;
    private final ModelPart snout;

    public KravajoModel(ModelPart meshRoot) {
        this.body = meshRoot.getChild("body");
        this.wingLeft = this.body.getChild("wing_left");
        this.wingRight = this.body.getChild("wing_right");
        this.snout = this.body.getChild("snout");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition parts = mesh.getRoot();

        // A squat flake: wider and longer than it is tall.
        PartDefinition body = parts.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.0F, -2.0F, -3.5F, 6.0F, 3.0F, 7.0F),
                PartPose.offset(0.0F, 19.0F, 0.0F));

        // The bite. Small, blunt, and pointed forward so the dive has a business end.
        body.addOrReplaceChild("snout",
                CubeListBuilder.create().texOffs(0, 11)
                        .addBox(-1.5F, -1.0F, -2.0F, 3.0F, 2.0F, 2.0F),
                PartPose.offset(0.0F, 0.0F, -3.5F));

        // Paddles, hinged at the body edge so the flap pivots where it should.
        body.addOrReplaceChild("wing_left",
                CubeListBuilder.create().texOffs(0, 16)
                        .addBox(0.0F, -0.5F, -3.0F, 7.0F, 1.0F, 6.0F),
                PartPose.offset(3.0F, -1.0F, 0.0F));
        body.addOrReplaceChild("wing_right",
                CubeListBuilder.create().texOffs(0, 16).mirror()
                        .addBox(-7.0F, -0.5F, -3.0F, 7.0F, 1.0F, 6.0F),
                PartPose.offset(-3.0F, -1.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(KravajoEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Fast, shallow flapping. A flea's wingbeat should be a blur rather than
        // a bird's stroke, so this runs several times faster than a vanilla bat.
        float beat = Mth.cos(ageInTicks * 1.8F) * 0.6F;
        this.wingLeft.zRot = -0.25F + beat;
        this.wingRight.zRot = 0.25F - beat;

        // It noses down when it is descending, which is the only cue that a dive
        // has started before it reaches you.
        double fall = entity.getDeltaMovement().y;
        this.body.xRot = (float) Mth.clamp(-fall * 1.4D, -0.9D, 0.9D);
        this.snout.xRot = this.body.xRot * 0.3F;

        // A slight roll off the horizontal drift keeps it from looking rigid.
        this.body.zRot = Mth.cos(ageInTicks * 0.6F) * 0.08F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float r, float g, float b, float a) {
        this.body.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
