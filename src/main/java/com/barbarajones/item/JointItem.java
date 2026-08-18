package com.barbarajones.item;

import com.barbarajones.content.ModEntities;
import com.barbarajones.entity.ThePlug;
import com.barbarajones.quest.Quests;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * A rolled joint of burnt, diced grass. Hold right-click to smoke it: it blows
 * a couple of O's, then the high lands. Smoking one deep underground during the
 * right quest stage triggers THE SEWER realization.
 */
public class JointItem extends Item {

    public JointItem(Properties props) {
        super(props);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 40;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(
            Level level, Player player, net.minecraft.world.InteractionHand hand) {
        player.startUsingItem(hand);
        return net.minecraft.world.InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!level.isClientSide) {
            return;
        }
        double yaw = Math.toRadians(entity.getYRot());
        double px = entity.getX() - Math.sin(yaw) * 0.4D;
        double pz = entity.getZ() + Math.cos(yaw) * 0.4D;
        double py = entity.getY() + entity.getEyeHeight() - 0.1D;
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3.0D;
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    px + Math.cos(a) * 0.12D, py, pz + Math.sin(a) * 0.12D, 0.0D, 0.03D, 0.0D);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300));
            entity.addEffect(new MobEffectInstance(MobEffects.JUMP, 300, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 240));
            entity.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120));

            if (entity instanceof Player player) {
                barbaraDisapproves(level, player);

                if (player.getY() < 45.0D && Quests.getStage(player) == Quests.ACT2_SEWER) {
                    sewerRealization(level, player);
                }
            }
        }
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }

    /**
     * It is HER stash and HER whole thing. Barbara does not appreciate you
     * getting high on it - any Barbara who sees you do it turns on you.
     */
    private void barbaraDisapproves(Level level, Player player) {
        var seen = level.getEntitiesOfClass(com.barbarajones.entity.BarbaraJones.class,
                player.getBoundingBox().inflate(16.0D));
        boolean scolded = false;
        for (var barbara : seen) {
            barbara.takeOffenceAt(player);
            if (!scolded) {
                player.sendSystemMessage(Component.literal(ChatFormatting.RED
                        + "Barbara: \"That's MINE. Who told you you could touch MY stash?!\""));
                scolded = true;
            }
        }
    }

    /** THE SEWER: the weed was fake, the "cocaine" was snow, and he is coming. */
    private void sewerRealization(Level level, Player player) {
        player.sendSystemMessage(Component.literal(ChatFormatting.RED
                + "...wait. This is just GRASS. THE WEED WAS FAKE. THE 'COCAINE' WAS SNOW. HE SCAMMED YOU."));

        double ang = level.random.nextDouble() * Math.PI * 2.0D;
        ThePlug plug = ModEntities.PLUG.get().create(level);
        if (plug != null) {
            plug.setPos(player.getX() + Math.cos(ang) * 28.0D, player.getY() + 12.0D,
                    player.getZ() + Math.sin(ang) * 28.0D);
            level.addFreshEntity(plug);
            plug.setTarget(player);
        }
        player.sendSystemMessage(Component.literal(ChatFormatting.DARK_GRAY
                + "Somewhere above you, a scope glints."));
        Quests.advanceTo(player, Quests.ACT2_REVENGE);
    }
}
