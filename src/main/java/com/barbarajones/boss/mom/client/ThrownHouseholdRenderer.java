package com.barbarajones.boss.mom.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.boss.mom.ThrownHousehold;

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
 * The airborne household object. A camera-facing quad that tumbles as it flies -
 * same billboard technique as {@code KraveLaserRenderer}, but it picks its
 * texture from the entity's synced kind so you can see WHAT is about to hit you.
 */
public class ThrownHouseholdRenderer extends EntityRenderer<ThrownHousehold> {

    private static final ResourceLocation[] TEXTURES = new ResourceLocation[] {
        tex("mom_object_remote"),
        tex("mom_object_slipper"),
        tex("mom_object_pan"),
        tex("mom_object_phone"),
        tex("mom_object_basket")
    };

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/" + name + ".png");
    }

    public ThrownHouseholdRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(ThrownHousehold entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        float age = entity.tickCount + partial;

        pose.pushPose();
        pose.mulPose(this.entityRenderDispatcher.cameraOrientation());
        // End over end. Nothing thrown across a kitchen in anger flies flat.
        pose.mulPose(Axis.ZP.rotationDegrees(age * 26.0F));

        VertexConsumer buf = buffers.getBuffer(
                RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        Matrix4f m = pose.last().pose();
        var normal = pose.last().normal();
        float h = 0.32F;
        float[][] pts = {{-h, -h}, {h, -h}, {h, h}, {-h, h}};
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
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
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownHousehold entity) {
        return TEXTURES[Math.floorMod(entity.getKind(), TEXTURES.length)];
    }
}
