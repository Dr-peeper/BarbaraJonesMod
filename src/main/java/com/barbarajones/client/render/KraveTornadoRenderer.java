package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.KraveTornado;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * The tornado: a tall tapered funnel built from two counter-rotating ring
 * stacks (inner fast, outer slow and wider), with a wobble that grows toward
 * the top. All geometry generated here.
 */
public class KraveTornadoRenderer extends EntityRenderer<KraveTornado> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/krave_tornado.png");

    private static final int LEVELS = 26;
    private static final int SEGMENTS = 18;
    private static final float H_STEP = 0.75F;

    public KraveTornadoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(KraveTornado entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partial;
        VertexConsumer buf = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));

        drawFunnel(pose, buf, light, age, 1.0F, 26.0F, 0.55F);     // inner, fast
        drawFunnel(pose, buf, light, age, 1.45F, -14.0F, 0.30F);   // outer, counter-spin

        super.render(entity, yaw, partial, pose, buffers, light);
    }

    /** Narrow at the base, flaring wide at the top. */
    private float funnelRadius(float level) {
        float p = level / LEVELS;
        return 0.5F + 7.5F * p * p + 1.2F * p;
    }

    private void drawFunnel(PoseStack pose, VertexConsumer buf, int light,
                            float age, float radMul, float spin, float baseAlpha) {
        Matrix4f m = pose.last().pose();
        var normal = pose.last().normal();

        for (int l = 0; l < LEVELS; l++) {
            float yy = l * H_STEP;
            float r0 = funnelRadius(l) * radMul;
            float r1 = funnelRadius(l + 1) * radMul;
            float rot = age * spin + l * 21.0F;
            float alpha = baseAlpha * (1.0F - 0.65F * l / LEVELS) + 0.06F;
            float shade = 0.32F + 0.2F * (float) l / LEVELS;
            float wobX = (float) Math.sin(age * 0.11F + l * 0.4F) * 0.35F * l / LEVELS;
            float wobZ = (float) Math.cos(age * 0.09F + l * 0.4F) * 0.35F * l / LEVELS;

            for (int s = 0; s < SEGMENTS; s++) {
                double a0 = Math.toRadians(rot + s * 360.0 / SEGMENTS);
                double a1 = Math.toRadians(rot + (s + 1) * 360.0 / SEGMENTS);
                float u0 = (float) s / SEGMENTS, u1 = (float) (s + 1) / SEGMENTS;

                vert(buf, m, normal, light, wobX + (float) (Math.cos(a0) * r0), yy,
                        wobZ + (float) (Math.sin(a0) * r0), u0, 0.0F, shade, alpha);
                vert(buf, m, normal, light, wobX + (float) (Math.cos(a1) * r0), yy,
                        wobZ + (float) (Math.sin(a1) * r0), u1, 0.0F, shade, alpha);
                vert(buf, m, normal, light, wobX + (float) (Math.cos(a1) * r1), yy + H_STEP,
                        wobZ + (float) (Math.sin(a1) * r1), u1, 1.0F, shade, alpha);
                vert(buf, m, normal, light, wobX + (float) (Math.cos(a0) * r1), yy + H_STEP,
                        wobZ + (float) (Math.sin(a0) * r1), u0, 1.0F, shade, alpha);
            }
        }
    }

    private void vert(VertexConsumer buf, Matrix4f m, org.joml.Matrix3f normal, int light,
                      float x, float y, float z, float u, float v, float shade, float alpha) {
        buf.vertex(m, x, y, z)
                .color(shade, shade * 0.92F, shade * 0.82F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(KraveTornado entity) {
        return TEXTURE;
    }
}
