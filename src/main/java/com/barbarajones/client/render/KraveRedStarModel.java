package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.KraveRedStar;

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

/**
 * Two crossed flat panes carrying the starburst texture, the same
 * "billboard cross" shape vanilla uses for flowers/saplings - simplest
 * possible geometry that still reads as a glowing disc from any angle
 * rather than vanishing edge-on the way one single flat plane would.
 */
public class KraveRedStarModel extends EntityModel<KraveRedStar> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            new ResourceLocation(BarbaraJonesMod.MODID, "krave_red_star"), "main");

    private final ModelPart root;

    public KraveRedStarModel(ModelPart root) {
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("pane1",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-16.0F, -16.0F, 0.0F, 32.0F, 32.0F, 0.0F),
                PartPose.ZERO);
        root.addOrReplaceChild("pane2",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-16.0F, -16.0F, 0.0F, 32.0F, 32.0F, 0.0F),
                PartPose.rotation(0.0F, 1.5707964F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(KraveRedStar entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        // Static geometry - all the motion is the entity's own slow yaw spin.
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay,
                               float r, float g, float b, float a) {
        this.root.render(pose, buffer, light, overlay, r, g, b, a);
    }
}
