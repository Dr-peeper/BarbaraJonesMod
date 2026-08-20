package com.barbarajones.v2.quests;

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

/**
 * The Krave Codex - the quest book.
 *
 * <p>Note what it does NOT do: it stores nothing. The old Quest Book kept the entire
 * questline in its own ItemStack NBT, which meant the book was the save file. Lose
 * it in lava and the questline was gone; craft a second and you had two. This is
 * just a key that opens a screen; every number it displays came from the server.
 *
 * <p>Right-clicking opens the tree on the client only. No packet is needed to open
 * it, because the client already holds a full mirror pushed at login.
 */
public class QuestAtlasItem extends Item {

    public QuestAtlasItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.barbarajones.v2.quests.client.QuestScreens.open());
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.barbarajones.quest_atlas.tip")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.barbarajones.quest_atlas.rule")
                .withStyle(ChatFormatting.DARK_RED));
    }
}
