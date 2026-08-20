package com.barbarajones.v2.internet;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/**
 * THE SERVICE CALL BOX: a wall-mounted junction box with a handset on the
 * front. Place it in the village and use it to place the call - stationary
 * and reusable (on {@link OutageEvent}'s own cooldown), the counterpart to
 * {@link RotaryPhoneItem} being portable and one-shot.
 *
 * <p>Orientation follows {@code TelevisionBlock}'s pattern exactly: computed
 * in {@link #getStateForPlacement} from where the player was looking, not
 * guessed at in {@code setPlacedBy}.
 */
public class ServiceCallBoxBlock extends HorizontalDirectionalBlock {

    public ServiceCallBoxBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.PASS;
        }
        String reason = OutageEvent.tryManualCall(serverLevel, pos, player);
        if (reason != null) {
            player.displayClientMessage(Component.literal(ChatFormatting.GRAY + reason), true);
            level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.8F, 0.8F);
            return InteractionResult.CONSUME;
        }
        player.displayClientMessage(Component.literal(ChatFormatting.YELLOW
                + "*ring* ...*ring* ...someone picks up."), true);
        level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 1.0F, 1.2F);
        return InteractionResult.CONSUME;
    }
}
