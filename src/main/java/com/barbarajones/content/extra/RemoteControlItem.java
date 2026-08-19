package com.barbarajones.content.extra;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The remote. Finds the nearest television and flips it without anyone having
 * to get out of the recliner, which is the entire reason remotes exist.
 */
public class RemoteControlItem extends Item {

    private static final int RANGE_H = 10;
    private static final int RANGE_V = 5;

    public RemoteControlItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        BlockPos origin = player.blockPosition();
        BlockPos nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-RANGE_H, -RANGE_V, -RANGE_H),
                origin.offset(RANGE_H, RANGE_V, RANGE_H))) {
            if (!(level.getBlockState(pos).getBlock() instanceof TelevisionBlock)) {
                continue;
            }
            double dist = pos.distSqr(origin);
            if (dist < bestDist) {
                bestDist = dist;
                // betweenClosed reuses one mutable BlockPos for the whole walk -
                // keeping the reference would leave us pointing at the last cell
                // scanned rather than the winner.
                nearest = pos.immutable();
            }
        }

        if (nearest == null) {
            player.displayClientMessage(Component.literal(ChatFormatting.GRAY
                    + "*click* *click* ...nothing. Where's the TV?"), true);
            level.playSound(null, origin, SoundEvents.WOODEN_BUTTON_CLICK_OFF, SoundSource.PLAYERS, 0.5F, 1.8F);
            return InteractionResultHolder.fail(stack);
        }

        BlockState tv = level.getBlockState(nearest);
        boolean nowOn = TelevisionBlock.toggle(level, nearest, tv);
        player.displayClientMessage(Component.literal(nowOn
                ? ChatFormatting.WHITE + "*click* The set comes on."
                : ChatFormatting.DARK_GRAY + "*click* The set goes dark."), true);
        player.getCooldowns().addCooldown(this, 10);
        return InteractionResultHolder.consume(stack);
    }
}
