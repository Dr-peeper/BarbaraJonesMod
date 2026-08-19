package com.barbarajones.block.krave;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;

/**
 * A Krave trunk segment - the bark is set chocolate, the core is toasted cereal.
 *
 * <p>Vanilla keeps its log-to-stripped-log pairs in {@code AxeItem.STRIPPABLES},
 * a private static map a mod cannot add to without an access transformer. Forge's
 * {@code getToolModifiedState} hook is the supported route, so each log carries a
 * supplier for whatever it strips into and answers the axe itself.
 */
public class KraveLogBlock extends RotatedPillarBlock {

    /** Null on the already-stripped variants, which have nothing left to strip to. */
    @Nullable
    private final Supplier<RotatedPillarBlock> stripped;

    public KraveLogBlock(Properties props, @Nullable Supplier<RotatedPillarBlock> stripped) {
        super(props);
        this.stripped = stripped;
    }

    @Override
    @Nullable
    public BlockState getToolModifiedState(BlockState state, UseOnContext ctx,
                                           ToolAction action, boolean simulate) {
        // The item check matters: the hook is reachable from anything holding a
        // stack, not just an axe, and an unguarded strip would fire for all of them.
        if (this.stripped != null && ToolActions.AXE_STRIP.equals(action)
                && ctx.getItemInHand().canPerformAction(ToolActions.AXE_STRIP)) {
            return this.stripped.get().defaultBlockState()
                    .setValue(AXIS, state.getValue(AXIS));
        }
        return super.getToolModifiedState(state, ctx, action, simulate);
    }

    // Fire spread is registered in FireBlock.bootStrap for vanilla wood only, so
    // modded logs burn like stone unless they answer these three themselves.

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return true;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return 5;
    }
}
