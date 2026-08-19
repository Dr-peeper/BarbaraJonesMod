package com.barbarajones.content.extra;

import com.barbarajones.content.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The lawn tools. One class covers both the push mower and the weed whacker -
 * the only difference is how wide a patch they take, so the radius is a
 * constructor argument.
 *
 * <p>Right-click the lawn and it harvests: every grass block in range gives up
 * a Handful of Grass and drops to dirt, and any loose grass, ferns or flowers
 * standing on it come off too. This is the bulk-harvest answer to picking blades
 * one at a time by hand, which is how Barbara ended up like this.
 */
public class LawnMowerItem extends Item {

    private final int radius;

    public LawnMowerItem(Properties props, int radius) {
        super(props);
        this.radius = radius;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip,
                                TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.GRAY + "Cuts a "
                + (radius * 2 + 1) + "x" + (radius * 2 + 1) + " patch of lawn."));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos origin = ctx.getClickedPos();
        Player player = ctx.getPlayer();

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        int harvested = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                // Follow the ground up and down a step so a mower still works on
                // the lumpy terrain Minecraft actually generates.
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    harvested += cut(level, pos);
                }
            }
        }

        if (harvested == 0) {
            return InteractionResult.CONSUME;
        }

        Block.popResource(level, origin.above(),
                new ItemStack(ModItems.HANDFUL_OF_GRASS.get(), harvested));
        level.playSound(null, origin, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, origin, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);

        if (player != null) {
            MutableComponent msg = Component.literal(ChatFormatting.GREEN
                    + "Mowed " + harvested + " handful(s) of grass.");
            player.displayClientMessage(msg, true);
            if (!player.getAbilities().instabuild) {
                EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                ctx.getItemInHand().hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(slot));
            }
        }
        return InteractionResult.CONSUME;
    }

    /** Returns how many handfuls this one block gave up. */
    private int cut(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        // Blocks.GRASS is the short grass plant in 1.20.1 (SHORT_GRASS is 1.20.3+).
        if (state.is(Blocks.GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN)
                || state.is(Blocks.LARGE_FERN) || state.is(Blocks.DEAD_BUSH)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return 1;
        }
        if (state.is(Blocks.GRASS_BLOCK)) {
            level.setBlock(pos, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
            return 1;
        }
        return 0;
    }
}
