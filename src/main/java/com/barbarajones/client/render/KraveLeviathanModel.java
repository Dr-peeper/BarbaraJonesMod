package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.KraveLeviathan;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
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
 * A Glaucus atlanticus silhouette blown up to the size of a castle: one long
 * tapering body in five chained segments, a fanned pair of big frontal
 * cerata standing in for wings, and two smaller fanned cerata clusters
 * trailing off the middle segments - the "sea slug" read comes entirely from
 * those fans, not from any actual articulated fingers (this thing is never
 * seen up close - see KraveLeviathan's whole reason for existing - so
 * detail finer than the silhouette and the fan shapes themselves would
 * never actually be visible).
 *
 * <p>The swim is a travelling sine wave down the five body segments (a
 * phase-delayed copy of the same wave at each segment, so it reads as one
 * ripple passing back along the body, the way an eel or a real Glaucus
 * actually moves) with the cerata fans rippling on their own faster cycle
 * layered on top.
 */
public class KraveLeviathanModel extends EntityModel<KraveLeviathan> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(BarbaraJonesMod.MODID, "krave_leviathan"), "main");

    private final ModelPart seg1;
    private final ModelPart seg2;
    private final ModelPart seg3;
    private final ModelPart seg4;
    private final ModelPart seg5;
    private final ModelPart wingLeft;
    private final ModelPart wingRight;
    private final ModelPart cerataLeftA;
    private final ModelPart cerataRightA;
    private final ModelPart cerataLeftB;
    private final ModelPart cerataRightB;

    public KraveLeviathanModel(ModelPart root) {
        this.seg1 = root.getChild("seg1");
        this.seg2 = this.seg1.getChild("seg2");
        this.seg3 = this.seg2.getChild("seg3");
        this.seg4 = this.seg3.getChild("seg4");
        this.seg5 = this.seg4.getChild("seg5");
        this.wingLeft = this.seg1.getChild("wing_left");
        this.wingRight = this.seg1.getChild("wing_right");
        this.cerataLeftA = this.seg2.getChild("cerata_left_a");
        this.cerataRightA = this.seg2.getChild("cerata_right_a");
        this.cerataLeftB = this.seg3.getChild("cerata_left_b");
        this.cerataRightB = this.seg3.getChild("cerata_right_b");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition seg1 = root.addOrReplaceChild("seg1",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-4.0F, -3.0F, -10.0F, 8.0F, 6.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, -8.0F));

        PartDefinition seg2 = seg1.addOrReplaceChild("seg2",
                CubeListBuilder.create().texOffs(0, 20)
                        .addBox(-3.5F, -2.5F, 0.0F, 7.0F, 5.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, -10.0F));

        PartDefinition seg3 = seg2.addOrReplaceChild("seg3",
                CubeListBuilder.create().texOffs(0, 39)
                        .addBox(-3.0F, -2.0F, 0.0F, 6.0F, 4.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 10.0F));

        PartDefinition seg4 = seg3.addOrReplaceChild("seg4",
                CubeListBuilder.create().texOffs(0, 57)
                        .addBox(-2.0F, -1.5F, 0.0F, 4.0F, 3.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 10.0F));

        seg4.addOrReplaceChild("seg5",
                CubeListBuilder.create().texOffs(0, 72)
                        .addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 8.0F),
                PartPose.offset(0.0F, 0.0F, 8.0F));

        // The primary wings: three broad fingers fanned around the front
        // segment, standing in for both the gliding surface and the
        // creature's biggest cerata. Positioned rather than individually
        // rotated - a fixed fan shape, animated as one rigid unit.
        seg1.addOrReplaceChild("wing_left",
                CubeListBuilder.create().texOffs(40, 0)
                        .addBox(0.0F, -1.0F, -6.0F, 14.0F, 1.0F, 5.0F)
                        .addBox(0.0F, -1.0F, -1.5F, 16.0F, 1.0F, 5.0F)
                        .addBox(0.0F, -1.0F, 3.0F, 13.0F, 1.0F, 5.0F),
                PartPose.offset(4.0F, -1.0F, -4.0F));
        seg1.addOrReplaceChild("wing_right",
                CubeListBuilder.create().texOffs(40, 0).mirror()
                        .addBox(-14.0F, -1.0F, -6.0F, 14.0F, 1.0F, 5.0F)
                        .addBox(-16.0F, -1.0F, -1.5F, 16.0F, 1.0F, 5.0F)
                        .addBox(-13.0F, -1.0F, 3.0F, 13.0F, 1.0F, 5.0F),
                PartPose.offset(-4.0F, -1.0F, -4.0F));

        // Two smaller trailing cerata clusters further back, same fanned-box
        // trick at a smaller scale.
        seg2.addOrReplaceChild("cerata_left_a",
                CubeListBuilder.create().texOffs(0, 90)
                        .addBox(0.0F, -0.5F, -3.0F, 8.0F, 1.0F, 3.0F)
                        .addBox(0.0F, -0.5F, 0.5F, 9.0F, 1.0F, 3.0F),
                PartPose.offset(3.5F, 0.0F, 3.0F));
        seg2.addOrReplaceChild("cerata_right_a",
                CubeListBuilder.create().texOffs(0, 90).mirror()
                        .addBox(-8.0F, -0.5F, -3.0F, 8.0F, 1.0F, 3.0F)
                        .addBox(-9.0F, -0.5F, 0.5F, 9.0F, 1.0F, 3.0F),
                PartPose.offset(-3.5F, 0.0F, 3.0F));

        seg3.addOrReplaceChild("cerata_left_b",
                CubeListBuilder.create().texOffs(0, 96)
                        .addBox(0.0F, -0.5F, -2.5F, 6.0F, 1.0F, 2.5F)
                        .addBox(0.0F, -0.5F, 0.5F, 7.0F, 1.0F, 2.5F),
                PartPose.offset(3.0F, 0.0F, 3.0F));
        seg3.addOrReplaceChild("cerata_right_b",
                CubeListBuilder.create().texOffs(0, 96).mirror()
                        .addBox(-6.0F, -0.5F, -2.5F, 6.0F, 1.0F, 2.5F)
                        .addBox(-7.0F, -0.5F, 0.5F, 7.0F, 1.0F, 2.5F),
                PartPose.offset(-3.0F, 0.0F, 3.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(KraveLeviathan entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // One slow travelling wave down the chain - each segment a fixed
        // phase further behind the last, so it reads as a single ripple
        // passing back along the body rather than each segment wagging on
        // its own.
        float speed = 0.035F;
        float amp = 0.12F;
        this.seg1.yRot = Mth.sin(ageInTicks * speed) * amp;
        this.seg2.yRot = Mth.sin(ageInTicks * speed - 0.6F) * amp;
        this.seg3.yRot = Mth.sin(ageInTicks * speed - 1.2F) * amp;
        this.seg4.yRot = Mth.sin(ageInTicks * speed - 1.8F) * amp;
        this.seg5.yRot = Mth.sin(ageInTicks * speed - 2.4F) * amp;

        // A slow, wide glide - not a flap. This is what makes it read as
        // gliding rather than flying.
        float glide = Mth.sin(ageInTicks * 0.025F) * 0.35F;
        this.wingLeft.zRot = -0.15F - glide;
        this.wingRight.zRot = 0.15F + glide;

        // Faster, smaller ripple on the trailing cerata, like feathery
        // projections trailing in a current rather than rigid fins.
        float flutter = Mth.sin(ageInTicks * 0.08F) * 0.25F;
        this.cerataLeftA.zRot = -0.2F - flutter;
        this.cerataRightA.zRot = 0.2F + flutter;
        this.cerataLeftB.zRot = -0.15F - flutter * 0.8F;
        this.cerataRightB.zRot = 0.15F + flutter * 0.8F;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float r, float g, float b, float a) {
        this.seg1.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
