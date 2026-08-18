package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.SkyCinematic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * The stage cinematics:
 *   O_BLOWER - an 18x giant Barbara with huge smoke rings pulsing from her mouth
 *   POURER   - the same giant tilting a colossal cup, brown Pibb crashing down
 *   TORCHER  - a colossal blowtorch roaring a flame cone at the ground
 *   MANAGER  - THE INTERNET MANAGER, a 9x faceless giant striding in
 *   CLEAVER  - a 34-block cleaver that quivers, then plunges
 */
public class SkyCinematicRenderer extends EntityRenderer<SkyCinematic> {

    private static ResourceLocation tex(String n) {
        return new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/" + n + ".png");
    }

    private static final ResourceLocation BARBARA = tex("barbara");
    private static final ResourceLocation MANAGER = tex("manager");
    private static final ResourceLocation TORCH   = tex("torch3d");
    private static final ResourceLocation RING    = tex("smoke_ring");
    private static final ResourceLocation CUP     = tex("fall_pibb");
    private static final ResourceLocation CLEAVER = tex("fall_knife");

    // SkyCinematic is a plain Entity, so the model is typed against LivingEntity
    // and posed by hand - these are statues, they don't need setupAnim.
    private final HumanoidModel<net.minecraft.world.entity.LivingEntity> humanoid;

    public SkyCinematicRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.humanoid = new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER));
    }

    @Override
    public void render(SkyCinematic entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partial;

        switch (entity.getKind()) {
            case SkyCinematic.POURER -> {
                giant(entity, pose, buffers, light, BARBARA, 18.0F);
                cupAndStream(entity, pose, buffers, light, age);
            }
            case SkyCinematic.TORCHER -> torch(pose, buffers, light, age);
            case SkyCinematic.MANAGER -> giant(entity, pose, buffers, light, MANAGER, 9.0F);
            case SkyCinematic.CLEAVER -> cleaver(pose, buffers, light, age);
            default -> {
                giant(entity, pose, buffers, light, BARBARA, 18.0F);
                smokeRings(pose, buffers, light, age);
            }
        }
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    private void giant(SkyCinematic e, PoseStack pose, MultiBufferSource buffers, int light,
                       ResourceLocation texture, float scale) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - e.getYRot()));
        pose.scale(-scale, -scale, scale);
        pose.translate(0.0D, -1.501D, 0.0D);
        this.humanoid.renderToBuffer(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),
                light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }

    /** Expanding smoke O's drifting up and away from her mouth. */
    private void smokeRings(PoseStack pose, MultiBufferSource buffers, int light, float age) {
        VertexConsumer buf = buffers.getBuffer(RenderType.entityTranslucent(RING));
        for (int i = 0; i < 5; i++) {
            float ringAge = (age + i * 24.0F) % 120.0F;
            float p = ringAge / 120.0F;
            float size = 2.0F + p * 16.0F;
            float alpha = 0.85F * (1.0F - p);

            pose.pushPose();
            pose.translate(0.0D, 16.0D + p * 14.0D, -4.0D - p * 20.0D);
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
            quad(pose, buf, light, size, 1.0F, 1.0F, 1.0F, alpha);
            pose.popPose();
        }
    }

    /** The colossal tilted cup and the brown Pibb waterfall beneath it. */
    private void cupAndStream(SkyCinematic e, PoseStack pose, MultiBufferSource buffers,
                              int light, float age) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - e.getYRot()));

        // the cup, tipping and sloshing
        pose.pushPose();
        pose.translate(9.0D, 13.0D, -5.0D);
        pose.mulPose(Axis.ZP.rotationDegrees(100.0F + (float) Math.sin(age * 0.05F) * 12.0F));
        quad(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CUP)), light, 10.0F,
                1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();

        // the waterfall: two crossed brown sheets falling to the ground
        VertexConsumer buf = buffers.getBuffer(RenderType.entityTranslucent(CUP));
        float groundY = -(float) (e.getY() - 4.0D) + 4.0F;
        Matrix4f m = pose.last().pose();
        var normal = pose.last().normal();
        for (int axis = 0; axis < 2; axis++) {
            float w = 2.6F + (float) Math.sin(age * 0.5F + axis) * 0.5F;
            float[][] pts = axis == 0
                    ? new float[][] {{9-w,12,-5},{9+w,12,-5},{9+w,groundY,-5},{9-w,groundY,-5}}
                    : new float[][] {{9,12,-5-w},{9,12,-5+w},{9,groundY,-5+w},{9,groundY,-5-w}};
            float[][] uv = {{0,0},{1,0},{1,1},{0,1}};
            for (int i = 0; i < 4; i++) {
                float a = i < 2 ? 0.85F : 0.4F;
                buf.vertex(m, pts[i][0], pts[i][1], pts[i][2])
                        .color(0.36F, 0.2F, 0.08F, a)
                        .uv(uv[i][0], uv[i][1])
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(light).normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
            }
        }
        pose.popPose();
    }

    /** The colossal blowtorch, sweeping and roaring flame at the world. */
    private void torch(PoseStack pose, MultiBufferSource buffers, int light, float age) {
        float sweep = (float) Math.sin(age * 0.03F) * 30.0F;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(sweep));
        pose.mulPose(Axis.XP.rotationDegrees(140.0F));
        pose.scale(12.0F, 12.0F, 12.0F);

        // the torch body as a simple textured box
        VertexConsumer buf = buffers.getBuffer(RenderType.entityCutoutNoCull(TORCH));
        Matrix4f m = pose.last().pose();
        var normal = pose.last().normal();
        float[][] pts = {{-0.6F,-1.2F,0},{0.6F,-1.2F,0},{0.6F,1.2F,0},{-0.6F,1.2F,0}};
        float[][] uv = {{0,1},{1,1},{1,0},{0,0}};
        for (int i = 0; i < 4; i++) {
            buf.vertex(m, pts[i][0], pts[i][1], pts[i][2])
                    .color(255, 255, 255, 255).uv(uv[i][0], uv[i][1])
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        }
        pose.popPose();

        // the flame cone
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(sweep));
        pose.translate(0.0D, -8.0D, 9.0D);
        pose.mulPose(Axis.XP.rotationDegrees(55.0F));
        VertexConsumer flame = buffers.getBuffer(RenderType.lightning());
        Matrix4f fm = pose.last().pose();
        for (int i = 0; i < 8; i++) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(i * 45.0F + age * 7.0F));
            float flick = 0.5F + (float) Math.sin(age * 0.8F + i * 1.7F) * 0.25F;
            Matrix4f fmm = pose.last().pose();
            flame.vertex(fmm, -0.6F, 0.0F, 0.0F).color(1.0F, 0.75F, 0.15F, flick).endVertex();
            flame.vertex(fmm, 0.6F, 0.0F, 0.0F).color(1.0F, 0.75F, 0.15F, flick).endVertex();
            flame.vertex(fmm, 4.0F, -26.0F, 0.0F).color(1.0F, 0.2F, 0.0F, 0.0F).endVertex();
            flame.vertex(fmm, -4.0F, -26.0F, 0.0F).color(1.0F, 0.2F, 0.0F, 0.0F).endVertex();
            pose.popPose();
        }
        pose.popPose();
    }

    /** A 34-block cleaver hanging in the sky, quivering, then plunging. */
    private void cleaver(PoseStack pose, MultiBufferSource buffers, int light, float age) {
        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        float quiver = age < 40.0F ? (float) Math.sin(age * 1.6F) * (age / 40.0F) * 4.0F : 0.0F;
        pose.mulPose(Axis.ZP.rotationDegrees(180.0F + quiver));   // blade points DOWN
        quad(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(CLEAVER)), light, 34.0F,
                1.0F, 1.0F, 1.0F, 1.0F);
        pose.popPose();
    }

    /** A centred square quad of the given size. */
    private void quad(PoseStack pose, VertexConsumer buf, int light, float size,
                      float r, float g, float b, float a) {
        Matrix4f m = pose.last().pose();
        var normal = pose.last().normal();
        float h = size / 2.0F;
        float[][] pts = {{-h,-h},{h,-h},{h,h},{-h,h}};
        float[][] uv = {{0,1},{1,1},{1,0},{0,0}};
        for (int i = 0; i < 4; i++) {
            buf.vertex(m, pts[i][0], pts[i][1], 0.0F)
                    .color(r, g, b, a).uv(uv[i][0], uv[i][1])
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                    .normal(normal, 0.0F, 0.0F, 1.0F).endVertex();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(SkyCinematic entity) {
        return BARBARA;
    }
}
