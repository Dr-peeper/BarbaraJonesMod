package com.barbarajones.boss.mom.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.boss.mom.MomKraveStash;

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
 * A confiscated box of Krave, sitting on the floor waiting to be smashed. Built
 * with the same six-quad cube as {@code KraveHealingBoxRenderer} - it does not
 * spin, because a target you have to burst down in a hurry should not be a
 * moving one. It only leans slightly, like it was put down in a temper.
 */
public class MomKraveStashRenderer extends EntityRenderer<MomKraveStash> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/mom_krave_stash.png");

    public MomKraveStashRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(MomKraveStash entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partial;

        pose.pushPose();
        pose.translate(0.0D, 0.5D, 0.0D);
        // A fixed lean per entity (seeded off its id) so a stack of them doesn't
        // look like a shop display.
        pose.mulPose(Axis.YP.rotationDegrees(entity.getId() * 37.0F % 360.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(6.0F + (float) Math.sin(age * 0.05D) * 1.5F));

        VertexConsumer buf = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        Matrix4f m = pose.last().pose();
        var normal = pose.last().normal();
        float hx = 0.34F;
        float hy = 0.48F;
        float hz = 0.20F;
        float[][][] faces = {
            {{-hx, -hy,  hz}, { hx, -hy,  hz}, { hx,  hy,  hz}, {-hx,  hy,  hz}},
            {{ hx, -hy, -hz}, {-hx, -hy, -hz}, {-hx,  hy, -hz}, { hx,  hy, -hz}},
            {{ hx, -hy,  hz}, { hx, -hy, -hz}, { hx,  hy, -hz}, { hx,  hy,  hz}},
            {{-hx, -hy, -hz}, {-hx, -hy,  hz}, {-hx,  hy,  hz}, {-hx,  hy, -hz}},
            {{-hx,  hy,  hz}, { hx,  hy,  hz}, { hx,  hy, -hz}, {-hx,  hy, -hz}},
            {{-hx, -hy, -hz}, { hx, -hy, -hz}, { hx, -hy,  hz}, {-hx, -hy,  hz}}
        };
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        for (float[][] face : faces) {
            for (int i = 0; i < 4; i++) {
                buf.vertex(m, face[i][0], face[i][1], face[i][2])
                        .color(255, 255, 255, 255)
                        .uv(uv[i][0], uv[i][1])
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(light)
                        .normal(normal, 0.0F, 1.0F, 0.0F)
                        .endVertex();
            }
        }
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(MomKraveStash entity) {
        return TEXTURE;
    }
}
