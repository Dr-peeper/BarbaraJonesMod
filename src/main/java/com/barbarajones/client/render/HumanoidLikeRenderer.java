package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;

/**
 * Shared renderer for the mod's human-shaped NPCs (Daniel, Mom, The Plug).
 * Each just supplies its own skin.
 */
public class HumanoidLikeRenderer<T extends Mob> extends MobRenderer<T, HumanoidModel<T>> {

    private final ResourceLocation texture;

    public HumanoidLikeRenderer(EntityRendererProvider.Context ctx, String textureName, float shadow) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), shadow);
        this.texture = new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/" + textureName + ".png");
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return this.texture;
    }
}
