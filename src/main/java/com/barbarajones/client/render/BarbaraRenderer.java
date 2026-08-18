package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.BarbaraJones;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Barbara - swells while raging, puffs up while high. */
public class BarbaraRenderer extends MobRenderer<BarbaraJones, HumanoidModel<BarbaraJones>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/barbara.png");

    public BarbaraRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    protected void scale(BarbaraJones entity, PoseStack pose, float partialTicks) {
        float s = entity.getScale();
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(BarbaraJones entity) {
        return TEXTURE;
    }
}
