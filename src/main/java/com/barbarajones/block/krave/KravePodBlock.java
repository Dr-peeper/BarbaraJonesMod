package com.barbarajones.block.krave;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Krave Pod: the fat cocoa-bearing husk that grows off the side of a Krave
 * trunk. This is the whole point of the tree - it is where the mod's cocoa comes
 * from, so a player never has to sail to a jungle or grind the old four-step
 * mushroom-and-coal substitute chain.
 *
 * <p>Extends {@link CocoaBlock} on purpose: the three growth stages, the hit
 * boxes, the bone-meal handling and the "drop when the block behind me goes away"
 * logic are all identical to cocoa. Only the two methods that hard-code
 * {@code BlockTags.JUNGLE_LOGS} need replacing.
 */
public class KravePodBlock extends CocoaBlock {

    public KravePodBlock(Properties props) {
        super(props);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return isKraveTrunk(level.getBlockState(pos.relative(state.getValue(FACING))));
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = this.defaultBlockState();
        LevelReader level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        for (Direction dir : ctx.getNearestLookingDirections()) {
            if (!dir.getAxis().isHorizontal()) {
                continue;
            }
            // FACING points at the supporting trunk, matching vanilla cocoa, so
            // canSurvive can be reused as the placement test.
            state = state.setValue(FACING, dir);
            if (state.canSurvive(level, pos)) {
                return state;
            }
        }
        return null;
    }

    /** Bark or stripped, log or full-bark wood - a pod will take any of them. */
    private static boolean isKraveTrunk(BlockState state) {
        return state.is(KraveWood.LOG) || state.is(KraveWood.WOOD)
                || state.is(KraveWood.STRIPPED_LOG) || state.is(KraveWood.STRIPPED_WOOD);
    }
}
