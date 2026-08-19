package com.barbarajones.block;

import com.barbarajones.content.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Barbara's crop. Grass Seeds were a dead-end item - named like a seed,
 * registered as a plain Item, plantable on nothing - so the whole grass economy
 * was foraging only. This turns it into an actual farm.
 *
 * <p>Four growth stages rather than vanilla wheat's eight: this is grass, it is
 * not supposed to be precious, and a shorter cycle keeps the stash topped up
 * without the player standing about waiting on bone meal.
 */
public class GrassCropBlock extends CropBlock {

    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;

    private static final VoxelShape[] SHAPES = new VoxelShape[] {
        Block.box(0.0D, 0.0D, 0.0D, 16.0D, 4.0D, 16.0D),
        Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D),
        Block.box(0.0D, 0.0D, 0.0D, 16.0D, 12.0D, 16.0D),
        Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D),
    };

    public GrassCropBlock(Properties props) {
        super(props);
    }

    @Override
    public IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.GRASS_SEEDS.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[Math.min(state.getValue(AGE), MAX_AGE)];
    }

    /**
     * Grass grows on dirt and grass blocks as happily as on farmland - it is a
     * weed. Tilled soil is still faster, because vanilla only ticks growth when
     * {@link CropBlock} likes the ground beneath it.
     */
    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.FARMLAND) || state.is(Blocks.DIRT)
                || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.COARSE_DIRT)
                || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.PODZOL);
    }
}
