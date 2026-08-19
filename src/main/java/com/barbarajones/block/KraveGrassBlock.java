package com.barbarajones.block;

import com.barbarajones.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dies back to Krave Dirt once something sits directly on top of it. The
 * Krave Kosmos dimension type has {@code has_skylight: false} (it's built on
 * the End's noise settings) - the original version of this gated on
 * {@code getMaxLocalRawBrightness}, which is sky light and block light
 * combined, so without sky light it read as permanently dark and the grass
 * died back everywhere regardless of what was actually above it. This just
 * checks the block directly above instead - no light math, works the same
 * in every dimension. See {@link KraveDirtBlock} for the reverse direction.
 */
public class KraveGrassBlock extends Block {

    public KraveGrassBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.above()).isAir()) {
            level.setBlockAndUpdate(pos, ModBlocks.KRAVE_DIRT.get().defaultBlockState());
        }
    }
}
