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
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * CLIENT ONLY - reached solely from ClientSetup, which is {@code Dist.CLIENT}
 * guarded.
 *
 * <p>A four-drawer steel cabinet. The 32x32 sheet is quartered: drawers at
 * (0,0), a blank side panel at (16,0) and the dusty top at (0,16). Everything
 * goes through {@code entityCutoutNoCull} (NEW_ENTITY format), so each vertex
 * carries position, colour, uv, overlay, light and normal - omit any one and
 * BufferBuilder throws on the first frame this is visible.
 *
 * <p>Battered cabinets darken toward black rather than swapping texture, so a
 * player can read at a glance which piece of cover is about to give out.
 */
public class FilingCabinetRenderer extends EntityRenderer<FilingCabinet> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/filing_cabinet.png");

    private static final float HALF_W = 0.45F;
    private static final float HEIGHT = 1.40F;
    private static final float HALF_D = 0.30F;

    // uv rectangles in the 32x32 sheet, as fractions
    private static final float[] UV_FRONT = { 0.0F, 0.0F, 0.5F, 0.5F };
    private static final float[] UV_SIDE = { 0.5F, 0.0F, 1.0F, 0.5F };
    private static final float[] UV_TOP = { 0.0F, 0.5F, 0.5F, 1.0F };

    public FilingCabinetRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(FilingCabinet entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));

        float wear = Mth.clamp(entity.getIntegrity(), 0.0F, 1.0F);
        float shade = 0.45F + 0.55F * wear;

        VertexConsumer buf = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        Matrix4f m = pose.last().pose();
        Matrix3f n = pose.last().normal();

        float x0 = -HALF_W, x1 = HALF_W;
        float y0 = 0.0F, y1 = HEIGHT;
        float z0 = -HALF_D, z1 = HALF_D;

        // front (-Z) is the drawer face; the back reuses the plain side panel
        face(buf, m, n, light, shade, UV_FRONT,
                x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0.0F, 0.0F, -1.0F);
        face(buf, m, n, light, shade, UV_SIDE,
                x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0.0F, 0.0F, 1.0F);
        face(buf, m, n, light, shade, UV_SIDE,
                x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1.0F, 0.0F, 0.0F);
        face(buf, m, n, light, shade, UV_SIDE,
                x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1.0F, 0.0F, 0.0F);
        face(buf, m, n, light, shade, UV_TOP,
                x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0.0F, 1.0F, 0.0F);
        face(buf, m, n, light, shade, UV_TOP,
                x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0.0F, -1.0F, 0.0F);

        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    private static void face(VertexConsumer buf, Matrix4f m, Matrix3f n, int light, float shade,
                             float[] uv,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz) {
        vertex(buf, m, n, light, shade, ax, ay, az, uv[0], uv[3], nx, ny, nz);
        vertex(buf, m, n, light, shade, bx, by, bz, uv[2], uv[3], nx, ny, nz);
        vertex(buf, m, n, light, shade, cx, cy, cz, uv[2], uv[1], nx, ny, nz);
        vertex(buf, m, n, light, shade, dx, dy, dz, uv[0], uv[1], nx, ny, nz);
    }

    private static void vertex(VertexConsumer buf, Matrix4f m, Matrix3f n, int light, float shade,
                               float x, float y, float z, float u, float v,
                               float nx, float ny, float nz) {
        buf.vertex(m, x, y, z)
                .color(shade, shade, shade, 1.0F)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(n, nx, ny, nz)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(FilingCabinet entity) {
        return TEXTURE;
    }
}
