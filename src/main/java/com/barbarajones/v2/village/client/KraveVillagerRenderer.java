package com.barbarajones.v2.village.client;

import com.barbarajones.v2.village.KraveVillagerEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Draws a Krave Villager.
 *
 * <p>Two pieces of feedback live here rather than in the model, because both are
 * about the entity as a whole rather than about any one limb:
 *
 * <ul>
 *   <li><b>The feeding flash.</b> {@code getGlow()} runs 1 to 0 over two seconds
 *       after a bowl of Krave goes in. It brightens the villager past ambient light
 *       and gives it a short swell, so a feed from across the village still reads.
 *   <li><b>Level presence.</b> A level-5 trader is noticeably bigger than a
 *       newcomer - four percent a rung. Small enough not to look like a different
 *       mob, large enough that a player can pick the veteran out of a crowd.
 * </ul>
 *
 * <p>Sleeping needs nothing here: {@code LivingEntityRenderer.setupRotations}
 * already lays a sleeping entity down along its bed's facing, and the model tucks
 * the limbs in.
 */
public class KraveVillagerRenderer extends MobRenderer<KraveVillagerEntity, KraveVillagerModel> {

    /** How far away a villager will volunteer its name and level, squared. */
    private static final double NAME_RANGE_SQR = 100.0D;

    public KraveVillagerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new KraveVillagerModel(ctx.bakeLayer(KraveVillagerModel.LAYER)), 0.42F);
    }

    @Override
    public ResourceLocation getTextureLocation(KraveVillagerEntity entity) {
        return entity.getProfession().texture();
    }

    @Override
    protected void scale(KraveVillagerEntity entity, PoseStack pose, float partialTicks) {
        float byLevel = 1.0F + (entity.getTradeLevel() - 1) * 0.04F;
        float glow = entity.getGlow();
        // A short outward punch on the feed, easing out rather than snapping back.
        float punch = 1.0F + glow * glow * 0.14F;
        float scale = byLevel * punch;
        pose.scale(scale, scale, scale);
    }

    /**
     * Lit by what it just ate. Without this the flash is invisible in a dim house,
     * which is exactly where a player is most likely to be feeding one.
     */
    @Override
    protected int getBlockLightLevel(KraveVillagerEntity entity, BlockPos pos) {
        int ambient = super.getBlockLightLevel(entity, pos);
        int boost = Mth.floor(entity.getGlow() * 12.0F);
        return Math.min(15, ambient + boost);
    }

    /**
     * Shows the profession and trade level over any villager the player is close to
     * and looking at. The whole feeding mechanic is invisible without a way to read
     * a villager's level at a glance, and forcing the player to open the trade
     * screen to find out is a bad trade.
     */
    @Override
    protected boolean shouldShowName(KraveVillagerEntity entity) {
        if (super.shouldShowName(entity)) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.crosshairPickEntity == entity
                && mc.player != null
                && mc.player.distanceToSqr(entity) < NAME_RANGE_SQR;
    }
}
