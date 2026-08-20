package com.barbarajones.v2.internet;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * The rotary phone: craft it, dial it, and hope he's in a good mood. Unlike
 * {@link ServiceCallBoxBlock} - reusable, stationary, on a cooldown - the
 * phone is a portable, one-shot call: it's consumed the instant the call
 * connects, the same way you don't get the letter back after you mail it.
 *
 * <p>Both feed the exact same {@link OutageEvent#tryManualCall}, so a call
 * placed from a phone in your hand and a call placed from a box bolted to the
 * wall are, mechanically, the same event started two different ways.
 */
public class RotaryPhoneItem extends Item {

    public RotaryPhoneItem(Properties props) {
        super(props);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 26;   // a beat of dialing before the call goes through
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }
        player.startUsingItem(hand);
        level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK,
                SoundSource.PLAYERS, 1.0F, 1.4F);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity user) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel)
                || !(user instanceof Player player)) {
            return stack;
        }
        String reason = OutageEvent.tryManualCall(serverLevel, player.blockPosition(), player);
        if (reason != null) {
            player.sendSystemMessage(Component.literal(ChatFormatting.GRAY + reason));
            level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO,
                    SoundSource.PLAYERS, 1.0F, 0.8F);
            return stack;   // the call never connected - do not spend the phone
        }
        player.sendSystemMessage(Component.literal(ChatFormatting.YELLOW + "The line rings once. Someone picks up."));
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return stack;
    }
}
