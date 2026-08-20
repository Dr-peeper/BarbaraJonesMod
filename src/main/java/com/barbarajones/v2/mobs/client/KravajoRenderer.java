package com.barbarajones.v2.mobs.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.v2.mobs.entity.KravajoEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the Kravajo. Small shadow, small hitbox, hard to lose track of anyway
 * because there are always several.
 */
public class KravajoRenderer extends MobRenderer<KravajoEntity, KravajoModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/kravajo.png");

    public KravajoRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KravajoModel(ctx.bakeLayer(KravajoModel.LAYER_LOCATION)), 0.25F);
    }

    @Override
    public ResourceLocation getTextureLocation(KravajoEntity entity) {
        return TEXTURE;
    }
}
