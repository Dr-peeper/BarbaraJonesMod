package com.barbarajones.block;

import com.barbarajones.content.ModBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Krave Grass and Dirt were plain, inert Blocks before this - a matched
 * visual pair with none of vanilla grass's actual behavior, so cleared land
 * in the Kosmos never regrew. Reimplements the two vanilla mechanics
 * (SpreadingSnowyDirtBlock.randomTick, minus the snow-layer variant - the
 * Kosmos has no snow) against Krave Dirt/Grass specifically, since vanilla's
 * own version only ever checks against Blocks.DIRT/Blocks.GRASS_BLOCK: dies
 * back to Krave Dirt when starved of light above it, and spreads onto nearby
 * Krave Dirt when it has enough.
 */
public class KraveGrassBlock extends Block {

    public KraveGrassBlock(BlockBehaviour.Properties props) {
        super(props);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!hasEnoughLight(level, pos)) {
            level.setBlockAndUpdate(pos, ModBlocks.KRAVE_DIRT.get().defaultBlockState());
            return;
        }
        if (level.getMaxLocalRawBrightness(pos.above()) < 9) {
            return;
        }
        BlockState grassState = defaultBlockState();
        for (int i = 0; i < 4; i++) {
            BlockPos target = pos.offset(random.nextInt(3) - 1, random.nextInt(5) - 3, random.nextInt(3) - 1);
            if (level.getBlockState(target).is(ModBlocks.KRAVE_DIRT.get()) && canSpreadOnto(level, target)) {
                level.setBlockAndUpdate(target, grassState);
            }
        }
    }

    private static boolean hasEnoughLight(LevelReader level, BlockPos pos) {
        return level.getMaxLocalRawBrightness(pos.above()) >= 4;
    }

    private static boolean canSpreadOnto(ServerLevel level, BlockPos pos) {
        return hasEnoughLight(level, pos) && !level.getFluidState(pos.above()).is(FluidTags.WATER);
    }
}
