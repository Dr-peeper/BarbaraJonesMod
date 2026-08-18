package com.barbarajones.item;

import com.barbarajones.housing.HousingResult;
import com.barbarajones.housing.HousingValidator;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The Housing Query. Right-click a block inside a room and it tells you whether
 * Cayden would live there - and if not, every single reason why.
 */
public class HousingQueryItem extends Item {

    public HousingQueryItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide || ctx.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }
        BlockPos target = ctx.getClickedPos().relative(ctx.getClickedFace());
        HousingResult result = HousingValidator.validate(level, target);

        ctx.getPlayer().sendSystemMessage(Component.literal(
                ChatFormatting.GOLD + "" + ChatFormatting.BOLD + "[Housing Check]"));
        if (result.valid) {
            ctx.getPlayer().sendSystemMessage(Component.literal(
                    ChatFormatting.GREEN + "Suitable! " + result.volume
                    + " blocks of space. Cayden will live here."));
        } else {
            for (String problem : result.problems) {
                ctx.getPlayer().sendSystemMessage(Component.literal(ChatFormatting.RED + " - " + problem));
            }
        }
        return InteractionResult.CONSUME;
    }
}
