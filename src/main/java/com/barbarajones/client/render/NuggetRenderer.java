package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.Nugget;

import net.minecraft.client.model.CatModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Nugget - Barbara's ginger cat, on the vanilla cat rig. */
public class NuggetRenderer extends MobRenderer<Nugget, CatModel<Nugget>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/nugget.png");

    public NuggetRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CatModel<>(ctx.bakeLayer(ModelLayers.CAT)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(Nugget entity) {
        return TEXTURE;
    }
}
