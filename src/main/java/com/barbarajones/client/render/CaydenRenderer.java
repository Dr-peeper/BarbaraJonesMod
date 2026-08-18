package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.CaydenCobb;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Cayden - kid-sized, and balloons outward the more Krave he eats. */
public class CaydenRenderer extends MobRenderer<CaydenCobb, HumanoidModel<CaydenCobb>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/cayden.png");

    public CaydenRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.4F);
    }

    @Override
    protected void scale(CaydenCobb entity, PoseStack pose, float partialTicks) {
        float fat = entity.getFatScale();
        // grows outward faster than up
        float wide = 0.85F * (1.0F + (fat - 1.0F) * 1.4F);
        pose.scale(wide, 0.85F * fat, wide);
    }

    @Override
    public ResourceLocation getTextureLocation(CaydenCobb entity) {
        return TEXTURE;
    }
}
