package com.barbarajones.block.krave;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The canopy of a Krave tree: packed cereal pieces rather than foliage.
 *
 * <p>It drips. Standing under one you get slow amber drops falling out of the
 * clusters, which is the cheapest possible way to tell a player at a glance that
 * this is not an oak - the silhouette alone reads as vanilla from a distance.
 */
public class KraveLeavesBlock extends LeavesBlock {

    /** One drip attempt per block per ~12 render-random ticks keeps it a hint, not a fog. */
    private static final int DRIP_CHANCE = 12;

    public KraveLeavesBlock(Properties props) {
        super(props);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(DRIP_CHANCE) != 0) {
            return;
        }
        // Only from the underside of the canopy - a drop spawned inside the blob
        // is invisible and costs a particle slot for nothing.
        if (!level.getBlockState(pos.below()).isAir()) {
            return;
        }
        level.addParticle(ParticleTypes.FALLING_HONEY,
                pos.getX() + random.nextDouble(),
                pos.getY() - 0.05D,
                pos.getZ() + random.nextDouble(),
                0.0D, 0.0D, 0.0D);
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return true;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 60;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 30;
    }
}
