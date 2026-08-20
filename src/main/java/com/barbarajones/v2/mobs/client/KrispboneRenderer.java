package com.barbarajones.v2.mobs.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.entity.KrispboneEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class KrispboneRenderer extends MobRenderer<KrispboneEntity, KrispboneModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/krispbone.png");

    public KrispboneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KrispboneModel(ctx.bakeLayer(KrispboneModel.LAYER_LOCATION)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(KrispboneEntity entity) {
        return TEXTURE;
    }
}
