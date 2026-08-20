package com.barbarajones.v2.mobs.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.entity.LoomweaverEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class LoomweaverRenderer extends MobRenderer<LoomweaverEntity, LoomweaverModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/loomweaver.png");

    public LoomweaverRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new LoomweaverModel(ctx.bakeLayer(LoomweaverModel.LAYER_LOCATION)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(LoomweaverEntity entity) {
        return TEXTURE;
    }
}
