package com.barbarajones.content.extra;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The watering can. Water the lawn, then mow the lawn - that is the loop.
 *
 * <p>On bare dirt it grows the grass back, on an established grass block it
 * sprouts fresh blades to cut, and on a Stash Box it tops the stash up by one.
 * Everything it does is deliberately hand-rolled rather than routed through
 * bone meal, so it works the same on Krave Kosmos ground as it does at home.
 */
public class WateringCanItem extends Item {

    public WateringCanItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        BlockState state = level.getBlockState(pos);

        if (level.isClientSide) {
            for (int i = 0; i < 12; i++) {
                level.addParticle(ParticleTypes.FALLING_WATER,
                        pos.getX() + level.random.nextDouble(),
                        pos.getY() + 1.1D,
                        pos.getZ() + level.random.nextDouble(),
                        0.0D, 0.0D, 0.0D);
            }
            return InteractionResult.SUCCESS;
        }

        boolean did = false;
        String message = null;

        if (state.getBlock() instanceof StashBoxBlock) {
            int fill = state.getValue(StashBoxBlock.FILL);
            if (fill > 0 && fill < StashBoxBlock.MAX_FILL) {
                level.setBlock(pos, state.setValue(StashBoxBlock.FILL, fill + 1), Block.UPDATE_ALL);
                did = true;
                message = ChatFormatting.GREEN + "Stash: " + (fill + 1) + "/" + StashBoxBlock.MAX_FILL;
            } else {
                message = ChatFormatting.GRAY + (fill == 0
                        ? "Nothing in there to water."
                        : "It's already packed.");
            }
        } else if (state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) {
            level.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
            did = true;
            message = ChatFormatting.GREEN + "The lawn comes back.";
        } else if (state.is(Blocks.GRASS_BLOCK)) {
            did = sprout(level, pos);
            message = did
                    ? ChatFormatting.GREEN + "Fresh blades. Get the mower."
                    : ChatFormatting.GRAY + "No room for it to grow.";
        }

        if (did) {
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5F, 1.6F);
            if (player != null && !player.getAbilities().instabuild) {
                EquipmentSlot slot = ctx.getHand() == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                ctx.getItemInHand().hurtAndBreak(1, player, broken -> broken.broadcastBreakEvent(slot));
            }
        }
        if (player != null && message != null) {
            player.displayClientMessage(Component.literal(message), true);
        }
        return did ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    /** Scatters short grass over the watered block and its neighbours. */
    private boolean sprout(Level level, BlockPos pos) {
        boolean any = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos ground = pos.offset(dx, 0, dz);
                BlockPos above = ground.above();
                if (level.getBlockState(ground).is(Blocks.GRASS_BLOCK)
                        && level.getBlockState(above).isAir()) {
                    level.setBlock(above, Blocks.GRASS.defaultBlockState(), Block.UPDATE_ALL);
                    any = true;
                }
            }
        }
        return any;
    }
}
