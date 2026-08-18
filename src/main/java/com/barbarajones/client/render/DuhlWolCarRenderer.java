package com.barbarajones.client.render;

import com.barbarajones.entity.DuhlWolCar;

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
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Duhl Wol's car: a blocky sedan built from coloured boxes.
 *
 * Every vertex here MUST supply position, colour, uv, overlay, light and
 * normal - that is the full NEW_ENTITY format {@link RenderType#entitySolid}
 * uses, and BufferBuilder throws "Not filled all elements of the vertex" the
 * moment one is missing. Skipping uv/overlay is what used to crash the game
 * as soon as the car came into view.
 */
public class DuhlWolCarRenderer extends EntityRenderer<DuhlWolCar> {

    /** A plain white 16x16 - the geometry is coloured per-vertex instead. */
    private static final ResourceLocation SKIN =
            new ResourceLocation("minecraft", "textures/misc/white.png");

    public DuhlWolCarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(DuhlWolCar entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));

        // idling shudder while parked
        if (entity.getState() == 1) {
            float bob = 0.03F * Mth.sin((entity.tickCount + partial) * 0.5F);
            pose.translate(0.0D, bob, 0.0D);
        }

        VertexConsumer buf = buffers.getBuffer(RenderType.entitySolid(SKIN));

        final float body = 0.13F, bodyG = 0.13F, bodyB = 0.16F;      // near-black paint
        final float glassR = 0.35F, glassG = 0.45F, glassB = 0.55F;  // dusty glass
        final float tyre = 0.06F;
        final float chrome = 0.55F;

        // chassis
        box(pose, buf, light, -0.9F, 0.30F, -0.75F, 1.8F, 0.45F, 1.5F, body, bodyG, bodyB);
        // cabin, set in from the sides
        box(pose, buf, light, -0.65F, 0.75F, -0.50F, 1.3F, 0.42F, 1.0F, glassR, glassG, glassB);
        // roof
        box(pose, buf, light, -0.68F, 1.17F, -0.52F, 1.36F, 0.06F, 1.04F, body, bodyG, bodyB);
        // bumpers
        box(pose, buf, light, -0.92F, 0.36F, -0.82F, 1.84F, 0.16F, 0.08F, chrome, chrome, chrome);
        box(pose, buf, light, -0.92F, 0.36F, 0.74F, 1.84F, 0.16F, 0.08F, chrome, chrome, chrome);
        // headlights
        box(pose, buf, light, -0.72F, 0.52F, -0.80F, 0.32F, 0.14F, 0.06F, 1.0F, 0.95F, 0.7F);
        box(pose, buf, light, 0.40F, 0.52F, -0.80F, 0.32F, 0.14F, 0.06F, 1.0F, 0.95F, 0.7F);
        // wheels
        for (float wx : new float[] { -0.92F, 0.62F }) {
            for (float wz : new float[] { -0.62F, 0.42F }) {
                box(pose, buf, light, wx, 0.0F, wz, 0.30F, 0.34F, 0.20F, tyre, tyre, tyre);
            }
        }

        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    /** An axis-aligned box from a corner plus size, all six faces wound outward. */
    private void box(PoseStack pose, VertexConsumer buf, int light,
                     float x, float y, float z, float w, float h, float d,
                     float r, float g, float b) {
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();
        float x0 = x, x1 = x + w;
        float y0 = y, y1 = y + h;
        float z0 = z, z1 = z + d;

        // north (-Z)
        quad(buf, m, n, light, r, g, b, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1);
        // south (+Z)
        quad(buf, m, n, light, r, g, b, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1);
        // west (-X)
        quad(buf, m, n, light, r, g, b, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0);
        // east (+X)
        quad(buf, m, n, light, r, g, b, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0);
        // top (+Y)
        quad(buf, m, n, light, r, g, b, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0);
        // bottom (-Y)
        quad(buf, m, n, light, r, g, b, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0);
    }

    /** One face. Each vertex carries the complete NEW_ENTITY attribute set. */
    private void quad(VertexConsumer buf, Matrix4f m, Matrix3f n, int light,
                      float r, float g, float b,
                      float ax, float ay, float az, float bx, float by, float bz,
                      float cx, float cy, float cz, float dx, float dy, float dz,
                      float nx, float ny, float nz) {
        vertex(buf, m, n, light, r, g, b, ax, ay, az, 0.0F, 0.0F, nx, ny, nz);
        vertex(buf, m, n, light, r, g, b, bx, by, bz, 1.0F, 0.0F, nx, ny, nz);
        vertex(buf, m, n, light, r, g, b, cx, cy, cz, 1.0F, 1.0F, nx, ny, nz);
        vertex(buf, m, n, light, r, g, b, dx, dy, dz, 0.0F, 1.0F, nx, ny, nz);
    }

    private void vertex(VertexConsumer buf, Matrix4f m, Matrix3f n, int light,
                        float r, float g, float b, float x, float y, float z,
                        float u, float v, float nx, float ny, float nz) {
        buf.vertex(m, x, y, z)
                .color(r, g, b, 1.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(n, nx, ny, nz)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(DuhlWolCar entity) {
        return SKIN;
    }
}
