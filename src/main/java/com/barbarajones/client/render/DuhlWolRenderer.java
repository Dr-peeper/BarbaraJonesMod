package com.barbarajones.client.render;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.entity.DuhlWol;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Duhl Wol - rendered as a scaled humanoid with a menacing skin. */
public class DuhlWolRenderer extends MobRenderer<DuhlWol, HumanoidModel<DuhlWol>> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/duhl_wol.png");

    public DuhlWolRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.7F);
    }

    @Override
    protected void scale(DuhlWol entity, PoseStack pose, float partialTicks) {
        pose.scale(1.05F, 1.05F, 1.05F);
    }

    @Override
    public ResourceLocation getTextureLocation(DuhlWol entity) {
        return TEXTURE;
    }
}
