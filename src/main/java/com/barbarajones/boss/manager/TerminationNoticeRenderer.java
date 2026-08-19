package com.barbarajones.boss.manager;

import com.barbarajones.BarbaraJonesMod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * CLIENT ONLY - reached solely from ClientSetup, which is {@code Dist.CLIENT}
 * guarded.
 *
 * <p>A single sheet of paper, tumbling end over end as it flies. Drawn as one
 * double-sided quad on {@code entityCutoutNoCull}, which uses the NEW_ENTITY
 * vertex format - every vertex therefore supplies position, colour, uv, overlay,
 * light and normal, or BufferBuilder throws "Not filled all elements of the
 * vertex" the first time a notice comes into view.
 */
public class TerminationNoticeRenderer extends EntityRenderer<TerminationNotice> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/termination_notice.png");

    private static final float HALF_W = 0.28F;
    private static final float HALF_H = 0.36F;

    public TerminationNoticeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(TerminationNotice entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partial;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(age * 17.0F));
        pose.mulPose(Axis.XP.rotationDegrees(age * 9.0F));

        VertexConsumer buf = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();

        // front, then back with the winding reversed so both sides light properly
        vertex(buf, m, n, light, -HALF_W, -HALF_H, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F);
        vertex(buf, m, n, light, HALF_W, -HALF_H, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
        vertex(buf, m, n, light, HALF_W, HALF_H, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        vertex(buf, m, n, light, -HALF_W, HALF_H, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);

        vertex(buf, m, n, light, HALF_W, -HALF_H, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F);
        vertex(buf, m, n, light, -HALF_W, -HALF_H, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, -1.0F);
        vertex(buf, m, n, light, -HALF_W, HALF_H, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F);
        vertex(buf, m, n, light, HALF_W, HALF_H, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -1.0F);

        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    private static void vertex(VertexConsumer buf, Matrix4f m, Matrix3f n, int light,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        buf.vertex(m, x, y, z)
                .color(1.0F, 1.0F, 1.0F, 1.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(n, nx, ny, nz)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(TerminationNotice entity) {
        return TEXTURE;
    }
}
