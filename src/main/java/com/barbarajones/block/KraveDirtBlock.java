package com.barbarajones.block;

import com.barbarajones.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Grows into Krave Grass on its own once nothing is sitting directly on top
 * of it - the growth direction the terrain is actually supposed to have
 * (see KraveGrassBlock). Doesn't need an adjacent grass block to "spread"
 * from the way vanilla dirt does; any exposed Krave Dirt in the Kosmos
 * grows back by itself.
 */
public class KraveDirtBlock extends Block {

    public KraveDirtBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockState(pos.above()).isAir()) {
            level.setBlockAndUpdate(pos, ModBlocks.KRAVE_GRASS.get().defaultBlockState());
        }
    }
}
