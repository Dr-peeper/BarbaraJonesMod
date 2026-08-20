package com.barbarajones.v2.mobs.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.entity.SoggyEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class SoggyRenderer extends MobRenderer<SoggyEntity, SoggyModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/soggy.png");

    public SoggyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SoggyModel(ctx.bakeLayer(SoggyModel.LAYER_LOCATION)), 0.6F);
    }

    @Override
    public ResourceLocation getTextureLocation(SoggyEntity entity) {
        return TEXTURE;
    }
}
