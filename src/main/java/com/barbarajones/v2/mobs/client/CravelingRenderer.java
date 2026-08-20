package com.barbarajones.v2.mobs.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.entity.CravelingEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CravelingRenderer extends MobRenderer<CravelingEntity, CravelingModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/craveling.png");

    public CravelingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CravelingModel(ctx.bakeLayer(CravelingModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CravelingEntity entity) {
        return TEXTURE;
    }
}
