package com.barbarajones.boss.mom.client;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.boss.mom.MomCobbBoss;
import com.barbarajones.boss.mom.MomPhase;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Mom Cobb the boss. Rendered a size up from the ordinary NPC so she reads as a
 * boss at a glance, and she swells a little further with each act - the last one
 * gets a slow heave on top, which is the only motion in the fight that is purely
 * cosmetic.
 */
public class MomCobbBossRenderer extends MobRenderer<MomCobbBoss, MomCobbBossModel> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(BarbaraJonesMod.MODID, "textures/entity/mom_boss.png");

    public MomCobbBossRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new MomCobbBossModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.6F);
    }

    @Override
    protected void scale(MomCobbBoss entity, PoseStack pose, float partialTicks) {
        MomPhase phase = entity.getPhase();
        float s = switch (phase) {
            case QUESTIONS -> 1.12F;
            case GAME -> 1.18F;
            case KRAVE -> 1.26F;
        };
        float age = entity.tickCount + partialTicks;
        if (phase == MomPhase.KRAVE) {
            s *= 1.0F + Mth.sin(age * 0.14F) * 0.025F;
        }
        // A wind-up that also grows makes the tell readable even with the pose
        // hidden behind a doorway or a blackout cloud.
        if (entity.isWindingUp()) {
            s *= 1.0F + entity.getWindupProgress() * 0.06F;
        }
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(MomCobbBoss entity) {
        return TEXTURE;
    }
}
