package com.barbarajones.content.extra;

import com.barbarajones.content.ModEntities;
import com.barbarajones.content.ModItems;
import com.barbarajones.entity.DuhlWol;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A counterfeit hundred, run off on the printer in the back. Cash it and three
 * times out of four you walk away with real money - the fourth time, Duhl Wol
 * pulls up, because Duhl Wol always finds out.
 */
public class CounterfeitBillItem extends Item {

    /** One in four goes wrong. Any worse and nobody would ever risk it. */
    private static final int BUST_CHANCE = 4;

    public CounterfeitBillItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        if (level.random.nextInt(BUST_CHANCE) != 0) {
            ItemStack payout = new ItemStack(ModItems.DOLLARS.get(), 8);
            if (!player.getInventory().add(payout)) {
                player.drop(payout, false);
            }
            level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_YES,
                    SoundSource.PLAYERS, 0.8F, 1.2F);
            player.sendSystemMessage(Component.literal(ChatFormatting.GREEN
                    + "They took it. They actually took it."));
            return InteractionResultHolder.consume(stack);
        }

        player.sendSystemMessage(Component.literal(ChatFormatting.RED
                + "\"...this ain't real. DUHL! DUHL, GET OVER HERE.\""));
        level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO,
                SoundSource.PLAYERS, 1.0F, 0.8F);

        DuhlWol collector = ModEntities.DUHL_WOL.get().create(level);
        if (collector != null) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            collector.moveTo(player.getX() + Math.cos(angle) * 12.0D,
                    player.getY(),
                    player.getZ() + Math.sin(angle) * 12.0D,
                    player.getYRot(), 0.0F);
            level.addFreshEntity(collector);
            collector.setTarget(player);
        }
        return InteractionResultHolder.consume(stack);
    }
}
