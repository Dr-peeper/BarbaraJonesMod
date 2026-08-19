package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.EmberCherry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * The flicked cherry: a camera-facing ember, drawn at full brightness because a
 * burning coal does not get darker for flying through a shadow. Same billboard
 * technique as KraveLaserRenderer, with a soft halo quad behind the core.
 */
public class EmberCherryRenderer extends EntityRenderer<EmberCherry> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/barbara_cherry.png");

    public EmberCherryRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(EmberCherry entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());

        VertexConsumer buf = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float core = entity.isHot() ? 0.26F : 0.19F;
        // A slow flicker so a cherry in flight never looks like a static sprite.
        float pulse = 1.0F + 0.18F * (float) Math.sin((entity.tickCount + partial) * 0.9D);

        quad(buf, m, n, core * pulse, 1.0F);
        quad(buf, m, n, core * pulse * 2.3F, 0.35F);

        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    private void quad(VertexConsumer buf, Matrix4f m, Matrix3f n, float half, float alpha) {
        float[][] pts = {{-half, -half}, {half, -half}, {half, half}, {-half, half}};
        float[][] uv = {{0.0F, 1.0F}, {1.0F, 1.0F}, {1.0F, 0.0F}, {0.0F, 0.0F}};
        for (int i = 0; i < 4; i++) {
            buf.vertex(m, pts[i][0], pts[i][1], 0.0F)
                    .color(1.0F, 1.0F, 1.0F, alpha)
                    .uv(uv[i][0], uv[i][1])
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LightTexture.FULL_BRIGHT)
                    .normal(n, 0.0F, 0.0F, 1.0F)
                    .endVertex();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(EmberCherry entity) {
        return TEXTURE;
    }
}
