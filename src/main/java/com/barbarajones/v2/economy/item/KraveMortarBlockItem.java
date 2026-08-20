package com.barbarajones.v2.economy.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import javax.annotation.Nullable;

/**
 * The Krave Mortar's item form. Carries its own hover text so a brand new
 * player who has never touched a wiki can read "right-click with cocoa beans"
 * straight off the tooltip in their hotbar.
 */
public class KraveMortarBlockItem extends BlockItem {

    public KraveMortarBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.literal("Right-click with cocoa beans to grind Krave Dust by hand.")
                .withStyle(ChatFormatting.GRAY));
    }
}
