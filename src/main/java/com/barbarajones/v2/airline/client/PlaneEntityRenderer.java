package com.barbarajones.v2.airline.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.airline.entity.PlaneEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws an aircraft.
 *
 * <p>Plain {@link EntityRenderer} rather than {@code MobRenderer}: the plane is not
 * a {@code LivingEntity} and has no limb swing to feed one.
 */
public class PlaneEntityRenderer extends EntityRenderer<PlaneEntity> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/plane.png");

    /** The rig is four blocks long in model units; the hitbox says twelve. */
    private static final float SCALE = 3.0F;

    private final PlaneModel model;

    public PlaneEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new PlaneModel(ctx.bakeLayer(PlaneModel.LAYER));
        this.shadowRadius = 5.0F;
    }

    @Override
    public void render(PlaneEntity entity, float entityYaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int packedLight) {
        pose.pushPose();

        // The standard living-entity setup: lift to the pivot, yaw, flip into model
        // space (y-down), then drop back to the feet before scaling up.
        pose.translate(0.0D, 1.5D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot())));
        pose.mulPose(Axis.XP.rotationDegrees(entity.getPlanePitch()));
        pose.scale(-1.0F, -1.0F, 1.0F);
        pose.translate(0.0D, -1.501D, 0.0D);
        pose.scale(SCALE, SCALE, SCALE);

        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        VertexConsumer consumer = buffer.getBuffer(this.model.renderType(TEXTURE));
        this.model.renderToBuffer(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                1.0F, 1.0F, 1.0F, 1.0F);

        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(PlaneEntity entity) {
        return TEXTURE;
    }
}
