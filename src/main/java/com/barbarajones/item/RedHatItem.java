package com.barbarajones.item;

import com.barbarajones.content.ModSounds;
import com.barbarajones.entity.MomCobb;
import com.barbarajones.quest.Quests;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Backwards Red Hat. Right-click for the Krave dance-walk - a burst of
 * pure stupid energy. If his Mom is in earshot, you will hear about adoption.
 */
public class RedHatItem extends Item {

    public RedHatItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        player.sendSystemMessage(Component.literal(ChatFormatting.RED + ""
                + ChatFormatting.BOLD + "\"I KRAVE THE KRAVE!\""));
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 200, 2));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1));
        player.setDeltaMovement(player.getDeltaMovement().x, 0.45D, player.getDeltaMovement().z);
        player.hurtMarked = true;
        level.playSound(null, player.blockPosition(), ModSounds.KRAVE_LAUGH.get(),
                SoundSource.MASTER, 1.2F, 1.3F);

        if (!level.getEntitiesOfClass(MomCobb.class, player.getBoundingBox().inflate(16.0D)).isEmpty()) {
            player.sendSystemMessage(Component.literal(ChatFormatting.RED
                    + "Mom: \"Are those stupid irrelevant puns?! I KNEW I should have put "
                    + "you up for adoption.\""));
        }
        Quests.complete(player, Quests.KRAVE_DANCE);
        return InteractionResultHolder.consume(stack);
    }
}
