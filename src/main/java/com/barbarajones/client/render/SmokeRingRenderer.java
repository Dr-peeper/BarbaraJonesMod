package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.SmokeRing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Draws an O as a flat annulus standing square-on to its own travel direction,
 * so it always reads as a ring flying at you rather than a disc.
 *
 * <p>The ring plane is built from two basis vectors taken perpendicular to the
 * entity's velocity and the vertices are emitted straight into that plane - no
 * matrix rotation, which is what keeps a ring fired straight up from folding
 * inside out. Two bands are drawn: a solid core and a wider, fainter halo.
 */
public class SmokeRingRenderer extends EntityRenderer<SmokeRing> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/barbara_smoke_o.png");

    private static final int SEGMENTS = 24;
    /** Matches SmokeRing.LIFETIME so the fade finishes exactly as it despawns. */
    private static final float LIFETIME = 55.0F;

    public SmokeRingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(SmokeRing entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        Vec3 dir = entity.getDeltaMovement();
        if (dir.lengthSqr() < 1.0E-6D) {
            dir = new Vec3(0.0D, 0.0D, 1.0D);
        }
        dir = dir.normalize();
        // Any vector not parallel to travel will do as the seed for the plane;
        // swap it near-vertical so the cross product never collapses to zero.
        Vec3 seed = Math.abs(dir.y) > 0.9D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 u = dir.cross(seed).normalize();
        Vec3 v = dir.cross(u).normalize();

        float age = (entity.tickCount + partial) / LIFETIME;
        float fade = Mth.clamp(1.0F - age * age, 0.0F, 1.0F);
        boolean laced = entity.isLaced();
        float outer = entity.ringRadius(partial);
        float band = laced ? 0.32F : 0.24F;
        float spin = (entity.tickCount + partial) * (laced ? 0.06F : 0.035F);

        VertexConsumer buf = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();

        drawBand(buf, m, n, light, u, v, Math.max(0.04F, outer - band), outer,
                fade * (laced ? 0.90F : 0.78F), spin, laced ? 0.86F : 0.94F);
        drawBand(buf, m, n, light, u, v, outer, outer + band * 1.4F,
                fade * 0.30F, -spin * 0.6F, 1.0F);

        super.render(entity, yaw, partial, pose, buffers, light);
    }

    private void drawBand(VertexConsumer buf, Matrix4f m, Matrix3f n, int light,
                          Vec3 u, Vec3 v, float inner, float outer,
                          float alpha, float spin, float tint) {
        for (int s = 0; s < SEGMENTS; s++) {
            double a0 = s * Math.PI * 2.0D / SEGMENTS + spin;
            double a1 = (s + 1) * Math.PI * 2.0D / SEGMENTS + spin;
            float t0 = (float) s / SEGMENTS;
            float t1 = (float) (s + 1) / SEGMENTS;

            ring(buf, m, n, light, u, v, a0, inner, t0, 0.0F, alpha, tint);
            ring(buf, m, n, light, u, v, a1, inner, t1, 0.0F, alpha, tint);
            ring(buf, m, n, light, u, v, a1, outer, t1, 1.0F, alpha, tint);
            ring(buf, m, n, light, u, v, a0, outer, t0, 1.0F, alpha, tint);
        }
    }

    private void ring(VertexConsumer buf, Matrix4f m, Matrix3f n, int light,
                      Vec3 u, Vec3 v, double angle, float radius,
                      float texU, float texV, float alpha, float tint) {
        double c = Math.cos(angle) * radius;
        double s = Math.sin(angle) * radius;
        float x = (float) (u.x * c + v.x * s);
        float y = (float) (u.y * c + v.y * s);
        float z = (float) (u.z * c + v.z * s);
        // Full NEW_ENTITY layout - position, colour, uv, overlay, light, normal.
        buf.vertex(m, x, y, z)
                .color(tint, tint, tint * 0.96F, alpha)
                .uv(texU, texV)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(n, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(SmokeRing entity) {
        return TEXTURE;
    }
}
