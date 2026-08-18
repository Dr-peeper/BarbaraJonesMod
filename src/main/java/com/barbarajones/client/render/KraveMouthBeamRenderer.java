package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.KraveMouthBeam;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Same camera-facing billboard technique as KraveLaserRenderer, but wider - a "big beam" feel. */
public class KraveMouthBeamRenderer extends EntityRenderer<KraveMouthBeam> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/krave_beam.png");

    public KraveMouthBeamRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(KraveMouthBeam entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());

        VertexConsumer buf = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        Matrix4f m = pose.last().pose();
        var normal = pose.last().normal();
        float w = 0.28F;
        float h = 1.3F;
        float[][] pts = {{-w, -h}, {w, -h}, {w, h}, {-w, h}};
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (int i = 0; i < 4; i++) {
            buf.vertex(m, pts[i][0], pts[i][1], 0.0F)
                    .color(255, 255, 255, 255)
                    .uv(uv[i][0], uv[i][1])
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(light)
                    .normal(normal, 0.0F, 0.0F, 1.0F)
                    .endVertex();
        }
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(KraveMouthBeam entity) {
        return TEXTURE;
    }
}
