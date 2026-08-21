package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.KraveRedStar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the red star via {@code RenderType.eyes()} - the same always-full-
 * bright layer vanilla uses for enderman/spider eyes - so it glows at full
 * brightness regardless of the dimension's own (still fairly dim) light
 * level, packed light or not. Not a real light source (nothing in vanilla
 * lets an entity cast one), but it reads as one.
 */
public class KraveRedStarRenderer extends EntityRenderer<KraveRedStar> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/krave_red_star.png");

    private static final float SCALE = 6.0F;

    private final KraveRedStarModel model;

    public KraveRedStarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new KraveRedStarModel(ctx.bakeLayer(KraveRedStarModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(KraveRedStar entity, float entityYaw, float partialTicks,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.scale(SCALE, SCALE, SCALE);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));

        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTicks, 0.0F, 0.0F);

        var vertexConsumer = buffer.getBuffer(RenderType.eyes(getTextureLocation(entity)));
        this.model.renderToBuffer(pose, vertexConsumer, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        pose.popPose();
        super.render(entity, entityYaw, partialTicks, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(KraveRedStar entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(KraveRedStar entity, net.minecraft.client.renderer.culling.Frustum frustum,
                                double camX, double camY, double camZ) {
        return true;
    }
}
