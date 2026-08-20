package com.barbarajones.v2.mobs.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.entity.MascotEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class MascotRenderer extends MobRenderer<MascotEntity, MascotModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/the_mascot.png");

    public MascotRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MascotModel(ctx.bakeLayer(MascotModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(MascotEntity entity) {
        return TEXTURE;
    }
}
