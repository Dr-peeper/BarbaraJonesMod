package com.barbarajones.item;

import com.barbarajones.content.ModEntities;
import com.barbarajones.entity.KraveMeteor;
import com.barbarajones.quest.Quests;

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
 * The Computer Mouse. The download had a virus. Right-click to hurl it out the
 * front door and watch it SMASH.
 */
public class ComputerMouseItem extends Item {

    public ComputerMouseItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        player.sendSystemMessage(Component.literal(ChatFormatting.RED
                + "\"A VIRUS?! On MY computer?!\""));

        double yaw = Math.toRadians(player.getYRot());
        double pitch = Math.toRadians(player.getXRot());
        KraveMeteor mouse = ModEntities.METEOR.get().create(level);
        if (mouse != null) {
            mouse.kind(KraveMeteor.TYPE_MOUSE);
            mouse.setPos(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
            mouse.setDeltaMovement(
                    -Math.sin(yaw) * Math.cos(pitch) * 1.4D,
                    -Math.sin(pitch) * 1.4D + 0.2D,
                    Math.cos(yaw) * Math.cos(pitch) * 1.4D);
            level.addFreshEntity(mouse);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 0.7F);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        Quests.advanceTo(player, Quests.ACT2_PLUG);
        return InteractionResultHolder.consume(stack);
    }
}
