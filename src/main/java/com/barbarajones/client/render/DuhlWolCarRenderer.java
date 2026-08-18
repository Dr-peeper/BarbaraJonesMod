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
import org.joml.Matrix4f;

/** Duhl Wol's car - a simple box that arrives and departs. */
public class DuhlWolCarRenderer extends EntityRenderer<DuhlWolCar> {

    public DuhlWolCarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(DuhlWolCar entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        pose.pushPose();

        // slight bob when parked
        if (entity.getState() == 1) {
            pose.translate(0.0D, 0.15D * Mth.sin(entity.tickCount * 0.05F + partial * 0.05F), 0.0D);
        }

        // a simple car shape: body (dark) + windows (semi-transparent)
        VertexConsumer buf = buffers.getBuffer(RenderType.entitySolid(new ResourceLocation("textures/block/dirt.png")));
        drawBox(pose, buf, light, 0.3F, 0.3F, 0.8F, 0.2F, 0.15F, 0.4F, 0.4F, 0.3F, 0.2F);

        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    /** Draw a simple box. */
    private void drawBox(PoseStack pose, VertexConsumer buf, int light,
                        float x, float y, float z, float w, float h, float d,
                        float r, float g, float b) {
        Matrix4f m = pose.last().pose();
        var normal = pose.last().normal();
        float x0 = x - w / 2, x1 = x + w / 2;
        float y0 = y, y1 = y + h;
        float z0 = z - d / 2, z1 = z + d / 2;

        // 6 faces (simplified: no UVs, just color)
        buf.vertex(m, x0, y0, z0).color(r, g, b, 1.0F).uv2(light).normal(normal, 0, -1, 0).endVertex();
        buf.vertex(m, x1, y0, z0).color(r, g, b, 1.0F).uv2(light).normal(normal, 0, -1, 0).endVertex();
        buf.vertex(m, x1, y0, z1).color(r, g, b, 1.0F).uv2(light).normal(normal, 0, -1, 0).endVertex();
        buf.vertex(m, x0, y0, z1).color(r, g, b, 1.0F).uv2(light).normal(normal, 0, -1, 0).endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(DuhlWolCar entity) {
        return new ResourceLocation("textures/block/dirt.png");
    }
}
