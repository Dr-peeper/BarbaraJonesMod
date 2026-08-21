package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.KraveLeviathan;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws {@link KraveLeviathan} - a plain {@link net.minecraft.world.entity.Entity},
 * not a {@code Mob}, so this is a hand-rolled {@link EntityRenderer} rather
 * than a {@code MobRenderer}: no living-entity machinery (no health, no
 * hurt-flash, no death animation) exists here to lean on.
 *
 * <p>{@link #SCALE} is large and deliberate - the model's own geometry is
 * only a few blocks long; this is what actually makes it "the size of the
 * den" the way {@code KraveMonsterRenderer} scales its own model up well
 * past its (much smaller, collision-only) hitbox.
 */
public class KraveLeviathanRenderer extends EntityRenderer<KraveLeviathan> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/krave_leviathan.png");

    private static final float SCALE = 18.0F;

    private final KraveLeviathanModel model;

    public KraveLeviathanRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new KraveLeviathanModel(ctx.bakeLayer(KraveLeviathanModel.LAYER_LOCATION));
        this.shadowRadius = 0.0F;   // it is never close enough to the ground to cast one
    }

    @Override
    public void render(KraveLeviathan entity, float entityYaw, float partialTicks,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.scale(SCALE, SCALE, SCALE);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));

        float ageInTicks = entity.tickCount + partialTicks;
        this.model.setupAnim(entity, 0.0F, 0.0F, ageInTicks, 0.0F, 0.0F);

        var vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(entity)));
        this.model.renderToBuffer(pose, vertexConsumer, packedLight,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        pose.popPose();
        super.render(entity, entityYaw, partialTicks, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(KraveLeviathan entity) {
        return TEXTURE;
    }

    @Override
    public boolean shouldRender(KraveLeviathan entity, net.minecraft.client.renderer.culling.Frustum frustum,
                                double camX, double camY, double camZ) {
        return true;   // huge and always far away - never trust vanilla's ordinary frustum distance cutoffs for it
    }
}
