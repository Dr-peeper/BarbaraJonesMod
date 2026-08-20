package com.barbarajones.v2.mobs.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.entity.KravelingEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class KravelingRenderer extends MobRenderer<KravelingEntity, KravelingModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/kraveling.png");

    public KravelingRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KravelingModel(ctx.bakeLayer(KravelingModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(KravelingEntity entity) {
        return TEXTURE;
    }
}
