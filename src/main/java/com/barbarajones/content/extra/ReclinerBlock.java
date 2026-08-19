package com.barbarajones.content.extra;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Barbara's recliner. The chair the whole documentary happens in front of.
 * Right-click to sit down; the chair takes the weight off and slowly patches
 * you up while you are in it. Right-click again (or sneak) to get up.
 */
public class ReclinerBlock extends HorizontalDirectionalBlock {

    private static final VoxelShape SEAT = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
    private static final VoxelShape BACK_NORTH = Block.box(0.0D, 8.0D, 12.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape BACK_SOUTH = Block.box(0.0D, 8.0D, 0.0D, 16.0D, 16.0D, 4.0D);
    private static final VoxelShape BACK_WEST = Block.box(12.0D, 8.0D, 0.0D, 16.0D, 16.0D, 16.0D);
    private static final VoxelShape BACK_EAST = Block.box(0.0D, 8.0D, 0.0D, 4.0D, 16.0D, 16.0D);

    public ReclinerBlock(BlockBehaviour.Properties props) {
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        // The backrest sits behind the seat, i.e. opposite the way the chair faces.
        VoxelShape back = switch (state.getValue(FACING)) {
            case SOUTH -> BACK_SOUTH;
            case WEST -> BACK_WEST;
            case EAST -> BACK_EAST;
            default -> BACK_NORTH;
        };
        return Shapes.or(SEAT, back);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isPassenger()) {
            return InteractionResult.PASS;
        }
        // One backside per chair. Searching the block's own space is enough - the
        // seat entity never moves once it has been placed.
        if (!level.getEntitiesOfClass(SeatEntity.class, new AABB(pos)).isEmpty()) {
            player.displayClientMessage(Component.literal(ChatFormatting.GRAY
                    + "Somebody's already in it."), true);
            return InteractionResult.CONSUME;
        }

        SeatEntity seat = new SeatEntity(ExtraRegistry.RECLINER_SEAT, level);
        seat.moveTo(pos.getX() + 0.5D, pos.getY() + 0.35D, pos.getZ() + 0.5D,
                state.getValue(FACING).toYRot(), 0.0F);
        level.addFreshEntity(seat);
        player.startRiding(seat);

        // Sitting down is the one thing in this mod that is purely good for you.
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200));
        level.playSound(null, pos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.7F, 0.8F);
        player.displayClientMessage(Component.literal(ChatFormatting.GOLD
                + "You sink into the recliner. The TV is right there."), true);
        return InteractionResult.CONSUME;
    }
}
