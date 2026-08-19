package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.BarbaraJones;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Barbara - swells while raging, puffs up while high. */
public class BarbaraRenderer extends MobRenderer<BarbaraJones, BarbaraModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/barbara.png");

    public BarbaraRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new BarbaraModel(ctx.bakeLayer(BarbaraModel.LAYER)), 0.5F);
    }

    @Override
    protected void scale(BarbaraJones entity, PoseStack pose, float partialTicks) {
        float s = entity.getScale();
        if (entity.isRaging()) {
            // she visibly swells and settles as she works herself up
            s *= 1.0F + Mth.sin((entity.tickCount + partialTicks) * 0.45F) * 0.035F;
        } else if (entity.isHigh()) {
            s *= 1.0F + Mth.sin((entity.tickCount + partialTicks) * 0.06F) * 0.02F;
        }
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(BarbaraJones entity) {
        return TEXTURE;
    }
}
