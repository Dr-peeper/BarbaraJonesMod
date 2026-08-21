package com.barbarajones.v2.airline.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.airline.entity.PlaneEntity;

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
 * The airliner rig.
 *
 * <p>Built out of {@link ModelPart}s like every other model in the mod rather than
 * hand-written vertex buffers. The first pass tried the latter and could not
 * compile - {@code com.mojang.math.Matrix4f} no longer exists in 1.20.1 (it moved
 * to JOML) - but the real argument is that a {@code ModelPart} tree gets correct
 * lighting, texturing, culling and per-part animation for free, which raw quads do
 * not.
 *
 * <p>Model units are 1/16 of a block, so the 64-unit fuselage is four blocks long
 * here and the renderer scales it up to the twelve-block airframe the hitbox
 * declares. UV offsets below match tools/make_airline.ps1 one for one.
 */
public class PlaneModel extends EntityModel<PlaneEntity> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            new ResourceLocation(BarbaraJonesMod.MODID, "plane"), "main");

    private final ModelPart root;
    private final ModelPart engineLeft;
    private final ModelPart engineRight;
    private final ModelPart gearNose;
    private final ModelPart gearLeft;
    private final ModelPart gearRight;

    public PlaneModel(ModelPart root) {
        this.root = root;
        this.engineLeft = root.getChild("engine_left");
        this.engineRight = root.getChild("engine_right");
        this.gearNose = root.getChild("gear_nose");
        this.gearLeft = root.getChild("gear_left");
        this.gearRight = root.getChild("gear_right");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Everything hangs off y = -12, which puts the fuselage centreline a little
        // under the model origin so the gear reaches the ground when it is extended.
        root.addOrReplaceChild("fuselage",
                CubeListBuilder.create().texOffs(0, 0).addBox(-32.0F, -5.0F, -5.0F, 64.0F, 10.0F, 10.0F),
                PartPose.offset(0.0F, -12.0F, 0.0F));

        root.addOrReplaceChild("cockpit",
                CubeListBuilder.create().texOffs(110, 32).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                PartPose.offset(-35.0F, -13.0F, 0.0F));

        root.addOrReplaceChild("tail_fin",
                CubeListBuilder.create().texOffs(0, 32).addBox(0.0F, -18.0F, -1.0F, 14.0F, 18.0F, 2.0F),
                PartPose.offset(20.0F, -16.0F, 0.0F));

        root.addOrReplaceChild("tail_wing",
                CubeListBuilder.create().texOffs(36, 32).addBox(0.0F, -1.0F, -12.0F, 10.0F, 2.0F, 24.0F),
                PartPose.offset(22.0F, -14.0F, 0.0F));

        root.addOrReplaceChild("wing_left",
                CubeListBuilder.create().texOffs(0, 88).addBox(-7.0F, -1.0F, 0.0F, 14.0F, 2.0F, 34.0F),
                PartPose.offset(-2.0F, -10.0F, 5.0F));

        root.addOrReplaceChild("wing_right",
                CubeListBuilder.create().texOffs(100, 88).addBox(-7.0F, -1.0F, -34.0F, 14.0F, 2.0F, 34.0F),
                PartPose.offset(-2.0F, -10.0F, -5.0F));

        root.addOrReplaceChild("engine_left",
                CubeListBuilder.create().texOffs(0, 164).addBox(-4.0F, -4.0F, -7.0F, 8.0F, 8.0F, 14.0F),
                PartPose.offset(-4.0F, -6.0F, 16.0F));

        root.addOrReplaceChild("engine_right",
                CubeListBuilder.create().texOffs(48, 164).addBox(-4.0F, -4.0F, -7.0F, 8.0F, 8.0F, 14.0F),
                PartPose.offset(-4.0F, -6.0F, -16.0F));

        root.addOrReplaceChild("gear_nose",
                CubeListBuilder.create().texOffs(96, 164).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(-24.0F, -8.0F, 0.0F));

        root.addOrReplaceChild("gear_left",
                CubeListBuilder.create().texOffs(108, 164).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(0.0F, -8.0F, 7.0F));

        root.addOrReplaceChild("gear_right",
                CubeListBuilder.create().texOffs(120, 164).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 10.0F, 2.0F),
                PartPose.offset(0.0F, -8.0F, -7.0F));

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(PlaneEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        float spin = entity.getPropellerRotation() * ((float) Math.PI / 180.0F);
        this.engineLeft.zRot = spin;
        this.engineRight.zRot = spin;

        // Gear up in the cruise. Hiding the parts is the whole retraction animation:
        // a well is not worth modelling on something seen from a hundred blocks out.
        boolean gearDown = entity.isLandingGearDown();
        this.gearNose.visible = gearDown;
        this.gearLeft.visible = gearDown;
        this.gearRight.visible = gearDown;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        this.root.render(pose, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
