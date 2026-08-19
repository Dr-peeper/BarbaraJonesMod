package com.barbarajones.content.extra;

import com.barbarajones.content.ModItems;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Barbara's stash box. Feed it handfuls of grass and it holds them; left alone
 * with anything in it at all, it slowly makes more. This is the mod's answer to
 * running dry - a stocked stash box is the difference between Barbara being a
 * neighbour and Barbara being a problem.
 *
 * <p>Fill level lives in the blockstate rather than a block entity: eight
 * discrete steps is all this needs, it survives chunk save/load for free, and a
 * comparator can read it.
 */
public class StashBoxBlock extends Block {

    public static final int MAX_FILL = 8;
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, MAX_FILL);

    public StashBoxBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FILL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        int fill = state.getValue(FILL);
        ItemStack held = player.getItemInHand(hand);

        if (held.is(ModItems.HANDFUL_OF_GRASS.get())) {
            if (fill >= MAX_FILL) {
                player.displayClientMessage(Component.literal(ChatFormatting.RED
                        + "It's packed. No more fits."), true);
                return InteractionResult.CONSUME;
            }
            int room = MAX_FILL - fill;
            int added = Math.min(room, held.getCount());
            if (!player.getAbilities().instabuild) {
                held.shrink(added);
            }
            level.setBlock(pos, state.setValue(FILL, fill + added), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 0.8F, 1.1F);
            player.displayClientMessage(Component.literal(ChatFormatting.GREEN
                    + "Stash: " + (fill + added) + "/" + MAX_FILL), true);
            return InteractionResult.CONSUME;
        }

        // Anything else in hand falls through so the box does not swallow an
        // attempt to place a block against it - only a bare hand empties it.
        if (!held.isEmpty()) {
            return InteractionResult.PASS;
        }

        if (fill == 0) {
            player.displayClientMessage(Component.literal(ChatFormatting.RED
                    + "EMPTY. The stash is EMPTY."), true);
            return InteractionResult.CONSUME;
        }

        popResource(level, pos, new ItemStack(ModItems.HANDFUL_OF_GRASS.get(), fill));
        level.setBlock(pos, state.setValue(FILL, 0), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.GRASS_BREAK, SoundSource.BLOCKS, 0.9F, 0.9F);
        player.displayClientMessage(Component.literal(ChatFormatting.GREEN
                + "Cleaned it out: " + fill + " handful(s)."), true);
        return InteractionResult.CONSUME;
    }

    /**
     * Grows only from a seeded box. An empty stash box is a box - it will not
     * conjure the first handful for you, so the harvesting loop still matters.
     */
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int fill = state.getValue(FILL);
        if (fill <= 0 || fill >= MAX_FILL) {
            return;
        }
        if (random.nextInt(4) == 0) {
            level.setBlock(pos, state.setValue(FILL, fill + 1), Block.UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(FILL) <= 0 || random.nextInt(8) != 0) {
            return;
        }
        level.addParticle(ParticleTypes.COMPOSTER,
                pos.getX() + 0.2D + random.nextDouble() * 0.6D,
                pos.getY() + 0.9D,
                pos.getZ() + 0.2D + random.nextDouble() * 0.6D,
                0.0D, 0.02D, 0.0D);
    }

    /** Breaking the box spills the stash instead of quietly deleting it. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) {
            int fill = state.getValue(FILL);
            if (fill > 0) {
                popResource(level, pos, new ItemStack(ModItems.HANDFUL_OF_GRASS.get(), fill));
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** Comparator reads the stash: 0 when empty, 15 when packed. */
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(FILL) * 15 / MAX_FILL;
    }
}
