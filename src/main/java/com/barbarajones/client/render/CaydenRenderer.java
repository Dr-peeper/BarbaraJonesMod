package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.CaydenCobb;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Cayden - kid-sized, balloons outward on Krave, and blazes once he ascends. */
public class CaydenRenderer extends MobRenderer<CaydenCobb, CaydenModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/cayden.png");

    public CaydenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CaydenModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.4F);
        addLayer(new SsjAuraLayer(this));
    }

    @Override
    protected void scale(CaydenCobb entity, PoseStack pose, float partialTicks) {
        float fat = entity.getFatScale();
        // grows outward faster than up
        float wide = 0.85F * (1.0F + (fat - 1.0F) * 1.4F);
        float tall = 0.85F * fat;

        if (entity.isSuperSaiyan()) {
            // A permanent step up in size the moment he ascends - he should
            // read as bigger even standing still, not just brighter.
            wide *= 1.18F;
            tall *= 1.18F;

            // A hard swell on transformation that settles back over ~1s, so the
            // ascension lands as a physical event rather than a texture swap.
            int since = entity.ticksSinceAscension();
            if (since >= 0 && since < 20) {
                float burst = 1.0F - since / 20.0F;
                float punch = 1.0F + burst * burst * 0.35F;
                wide *= punch;
                tall *= punch;
            }
            // and a permanent low hum of a pulse afterwards
            float hum = 1.0F + Mth.sin((entity.tickCount + partialTicks) * 0.35F) * 0.022F;
            wide *= hum;
            tall *= hum;
        }
        pose.scale(wide, tall, wide);
    }

    @Override
    public void render(CaydenCobb entity, float yaw, float partial, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        // Ascended, he hovers and rolls slightly into his own movement.
        if (entity.isSuperSaiyan()) {
            float t = entity.tickCount + partial;
            pose.pushPose();
            pose.translate(0.0D, 0.12D + Mth.sin(t * 0.14F) * 0.09D, 0.0D);
            super.render(entity, yaw, partial, pose, buffers, light);
            pose.popPose();
            return;
        }
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    /** Ascended he is his own light source, so stop the world dimming him. */
    @Override
    protected int getBlockLightLevel(CaydenCobb entity, net.minecraft.core.BlockPos pos) {
        return entity.isSuperSaiyan() ? 15 : super.getBlockLightLevel(entity, pos);
    }

    @Override
    public ResourceLocation getTextureLocation(CaydenCobb entity) {
        return TEXTURE;
    }
}
