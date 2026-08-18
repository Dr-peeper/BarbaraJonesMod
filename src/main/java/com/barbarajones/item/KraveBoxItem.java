package com.barbarajones.item;

import com.barbarajones.content.ModEntities;
import com.barbarajones.entity.KraveMonster;
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
 * An empty Krave box. Crush it to summon THE KRAVE MONSTER - but only once
 * Barbara and Cayden are both on your side.
 */
public class KraveBoxItem extends Item {

    public KraveBoxItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (!Quests.isUnlocked(player, Quests.SUMMON_KRAVE)) {
            player.sendSystemMessage(Component.literal(ChatFormatting.LIGHT_PURPLE + "[Krave Quest] "
                    + ChatFormatting.GRAY + "Nothing happens. Recruit Barbara and stock a full bowl first."));
            return InteractionResultHolder.fail(stack);
        }

        double yaw = Math.toRadians(player.getYRot());
        KraveMonster monster = ModEntities.KRAVE_MONSTER.get().create(level);
        if (monster != null) {
            monster.moveTo(player.getX() - Math.sin(yaw) * 4.0D, player.getY() + 1.0D,
                    player.getZ() + Math.cos(yaw) * 4.0D, player.getYRot() + 180.0F, 0.0F);
            level.addFreshEntity(monster);
            monster.setTarget(player);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.2F);
        Quests.complete(player, Quests.SUMMON_KRAVE);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }
}
