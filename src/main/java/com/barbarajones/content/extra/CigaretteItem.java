package com.barbarajones.content.extra;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A cigarette. Barbara's fallback when the grass runs out - it takes the edge
 * off for a minute and costs you half a heart, which is the whole deal with
 * cigarettes. Hold right-click to smoke it.
 */
public class CigaretteItem extends Item {

    public CigaretteItem(Properties props) {
        super(props);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!level.isClientSide) {
            return;
        }
        double yaw = Math.toRadians(entity.getYRot());
        level.addParticle(ParticleTypes.SMOKE,
                entity.getX() - Math.sin(yaw) * 0.35D,
                entity.getY() + entity.getEyeHeight() - 0.05D,
                entity.getZ() + Math.cos(yaw) * 0.35D,
                0.0D, 0.02D, 0.0D);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300));
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200));
            // The cost. Damage source is deliberately generic so it cannot be
            // blamed on a player and start a feud.
            entity.hurt(entity.damageSources().magic(), 1.0F);
            if (entity instanceof Player player) {
                player.sendSystemMessage(Component.literal(ChatFormatting.GRAY
                        + "It's not grass, but it'll hold."));
            }
        }
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }
}
