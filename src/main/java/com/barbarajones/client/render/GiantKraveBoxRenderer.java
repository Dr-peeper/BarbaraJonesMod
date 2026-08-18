package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.GiantKraveBox;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** The giant Krave box, tumbling wildly on all three axes as it falls. */
public class GiantKraveBoxRenderer extends EntityRenderer<GiantKraveBox> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/giant_krave_box.png");

    public GiantKraveBoxRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(GiantKraveBox entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partial;

        pose.pushPose();
        pose.translate(0.0D, 1.5D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(age * 13.0F));
        pose.mulPose(Axis.XP.rotationDegrees(age * 9.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(age * 6.0F));
        float s = 2.6F + (float) Math.sin(age * 0.3F) * 0.15F;
        pose.scale(s, s, s);

        VertexConsumer buf = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        Matrix4f m = pose.last().pose();
        var normal = pose.last().normal();
        // a cereal box: taller than wide, shallow
        float hx = 0.8F, hy = 1.1F, hz = 0.5F;
        float[][][] faces = {
            {{-hx,-hy, hz},{ hx,-hy, hz},{ hx, hy, hz},{-hx, hy, hz}},
            {{ hx,-hy,-hz},{-hx,-hy,-hz},{-hx, hy,-hz},{ hx, hy,-hz}},
            {{ hx,-hy, hz},{ hx,-hy,-hz},{ hx, hy,-hz},{ hx, hy, hz}},
            {{-hx,-hy,-hz},{-hx,-hy, hz},{-hx, hy, hz},{-hx, hy,-hz}},
            {{-hx, hy, hz},{ hx, hy, hz},{ hx, hy,-hz},{-hx, hy,-hz}},
            {{-hx,-hy,-hz},{ hx,-hy,-hz},{ hx,-hy, hz},{-hx,-hy, hz}}
        };
        float[][] uv = {{0,1},{1,1},{1,0},{0,0}};
        for (float[][] face : faces) {
            for (int i = 0; i < 4; i++) {
                buf.vertex(m, face[i][0], face[i][1], face[i][2])
                        .color(255, 255, 255, 255).uv(uv[i][0], uv[i][1])
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
            }
        }
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(GiantKraveBox entity) {
        return TEXTURE;
    }
}
