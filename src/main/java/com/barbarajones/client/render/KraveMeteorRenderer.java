package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.KraveMeteor;

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
 * Every falling doom-object: molten meteors and giant KRAVE boxes tumble as
 * textured cubes; cleavers, burning grass, cups, Gatorade and the prequel's
 * computer mouse spin as camera-facing billboards.
 */
public class KraveMeteorRenderer extends EntityRenderer<KraveMeteor> {

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/" + name + ".png");
    }

    private static final ResourceLocation METEOR   = tex("meteor");
    private static final ResourceLocation BOX      = tex("giant_krave_box");
    private static final ResourceLocation KNIFE    = tex("fall_knife");
    private static final ResourceLocation GRASS    = tex("fall_grass");
    private static final ResourceLocation PIBB     = tex("fall_pibb");
    private static final ResourceLocation GATORADE = tex("fall_gatorade");
    private static final ResourceLocation MOUSE    = tex("fall_mouse");

    public KraveMeteorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(KraveMeteor entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partial;
        pose.pushPose();
        pose.translate(0.0D, 1.0D, 0.0D);

        switch (entity.getKind()) {
            case KraveMeteor.TYPE_BOX   -> cube(pose, buffers, light, BOX, age, 2.2F);
            case KraveMeteor.TYPE_KNIFE -> billboard(pose, buffers, light, KNIFE, age, 11.0F, 14.0F);
            case KraveMeteor.TYPE_GRASS -> billboard(pose, buffers, light, GRASS, age, 3.5F, 9.0F);
            case KraveMeteor.TYPE_PIBB  -> billboard(pose, buffers, light, PIBB, age, 2.5F, 11.0F);
            case KraveMeteor.TYPE_GATORADE -> billboard(pose, buffers, light, GATORADE, age, 2.5F, 12.0F);
            case KraveMeteor.TYPE_MOUSE -> billboard(pose, buffers, light, MOUSE, age, 1.2F, 20.0F);
            default -> cube(pose, buffers, light, METEOR, age, 3.0F);
        }
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    /** A tumbling textured cube. */
    private void cube(PoseStack pose, MultiBufferSource buffers, int light,
                      ResourceLocation texture, float age, float size) {
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(age * 11.0F));
        pose.mulPose(Axis.YP.rotationDegrees(age * 7.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(age * 5.0F));

        VertexConsumer buf = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        float h = size / 2.0F;
        Matrix4f m = pose.last().pose();
        var normal = pose.last().normal();

        // 6 faces, each a full-texture quad
        float[][][] faces = {
            {{-h,-h, h},{ h,-h, h},{ h, h, h},{-h, h, h}},   // +Z
            {{ h,-h,-h},{-h,-h,-h},{-h, h,-h},{ h, h,-h}},   // -Z
            {{ h,-h, h},{ h,-h,-h},{ h, h,-h},{ h, h, h}},   // +X
            {{-h,-h,-h},{-h,-h, h},{-h, h, h},{-h, h,-h}},   // -X
            {{-h, h, h},{ h, h, h},{ h, h,-h},{-h, h,-h}},   // +Y
            {{-h,-h,-h},{ h,-h,-h},{ h,-h, h},{-h,-h, h}}    // -Y
        };
        float[][] uv = {{0,1},{1,1},{1,0},{0,0}};
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
    }

    /** A camera-facing quad, spinning as it falls. */
    private void billboard(PoseStack pose, MultiBufferSource buffers, int light,
                           ResourceLocation texture, float age, float size, float spin) {
        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.ZP.rotationDegrees(age * spin));

        VertexConsumer buf = buffers.getBuffer(RenderType.entityCutoutNoCull(texture));
        Matrix4f m = pose.last().pose();
        var normal = pose.last().normal();
        float h = size / 2.0F;
        float[][] pts = {{-h,-h},{h,-h},{h,h},{-h,h}};
        float[][] uv  = {{0,1},{1,1},{1,0},{0,0}};
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
    public ResourceLocation getTextureLocation(KraveMeteor entity) {
        return METEOR;
    }
}
