package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.KraveMouthBeam;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * Same giant-glowing-orb technique as {@link KraveLaserRenderer}, sized up
 * further - the boss's own "big beam" should read as noticeably bigger and
 * heavier than Cayden's.
 */
public class KraveMouthBeamRenderer extends EntityRenderer<KraveMouthBeam> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/krave_beam.png");

    public KraveMouthBeamRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(KraveMouthBeam entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partial;
        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());

        VertexConsumer buf = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        float pulse = 1.0F + 0.08F * Mth.sin(age * 0.5F);

        orb(pose, buf, light, 2.2F * pulse, -age * 3.0F);
        orb(pose, buf, light, 1.25F * pulse, age * 7.0F);

        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    private void orb(PoseStack pose, VertexConsumer buf, int light, float size, float spinDeg) {
        pose.pushPose();
        pose.mulPose(Axis.ZP.rotationDegrees(spinDeg));

        Matrix4f m = pose.last().pose();
        var normal = pose.last().normal();
        float h = size / 2.0F;
        float[][] pts = {{-h, -h}, {h, -h}, {h, h}, {-h, h}};
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
    }

    @Override
    public ResourceLocation getTextureLocation(KraveMouthBeam entity) {
        return TEXTURE;
    }
}
