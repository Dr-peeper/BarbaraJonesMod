package com.barbarajones.item;

import com.barbarajones.quest.Quests;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;
import java.util.List;

/** The Krave Quest Book. Right-click to open; progress lives in its own NBT. */
public class QuestBookItem extends Item {

    public QuestBookItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.barbarajones.client.ClientPacketHandler.openQuestBook());
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        int stage = Quests.getStage(stack);
        tooltip.add(Component.literal(ChatFormatting.YELLOW + "Stage " + stage + ": " + Quests.TITLE[stage]));
        tooltip.add(Component.literal(ChatFormatting.GRAY + "Right-click to open"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return Quests.getStage(stack) >= Quests.DONE;   // shimmer once finished
    }
}
