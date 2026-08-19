package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.KraveHealingBox;

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

/**
 * A stationary, hand-drawn rectangular prism - a genuinely "giant" cereal
 * box, sized to actually read as a box rather than a paperweight - with its
 * own texture (no longer reusing the falling apocalypse box's look). Bobs
 * gently and spins slowly, same as before. While the entity's shield is up,
 * a larger translucent shell is drawn around it that fades as the shield
 * depletes.
 */
public class KraveHealingBoxRenderer extends EntityRenderer<KraveHealingBox> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/krave_healing_box.png");
    private static final ResourceLocation SHIELD_TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/krave_shield.png");

    private static final float HALF_X = 0.6F;
    private static final float HALF_Y = 0.9F;
    private static final float HALF_Z = 0.22F;

    public KraveHealingBoxRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(KraveHealingBox entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partial;

        pose.pushPose();
        pose.translate(0.0D, HALF_Y + Math.sin(age * 0.08D) * 0.05D, 0.0D);
        pose.mulPose(Axis.YP.rotationDegrees(age * 1.2F));

        drawBox(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light,
                HALF_X, HALF_Y, HALF_Z, 255);

        int shield = entity.getShield();
        if (shield > 0) {
            VertexConsumer shieldBuf = buffers.getBuffer(RenderType.entityTranslucent(SHIELD_TEXTURE));
            float pulse = 0.55F + 0.15F * (float) Math.sin(age * 0.15D);
            int alpha = (int) (pulse * 220 * (shield / 3.0F));
            drawBox(pose, shieldBuf, light, HALF_X + 0.15F, HALF_Y + 0.15F, HALF_Z + 0.15F, alpha);
        }

        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    private void drawBox(PoseStack pose, VertexConsumer buf, int light,
                         float hx, float hy, float hz, int alpha) {
        Matrix4f m = pose.last().pose();
        var normal = pose.last().normal();
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
                        .color(255, 255, 255, alpha).uv(uv[i][0], uv[i][1])
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                        .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(KraveHealingBox entity) {
        return TEXTURE;
    }
}
