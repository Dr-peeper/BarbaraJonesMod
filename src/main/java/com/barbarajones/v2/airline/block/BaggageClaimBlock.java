package com.barbarajones.v2.airline.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BaggageClaimBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

    public BaggageClaimBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                  net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player.isShiftKeyDown()) {
            player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§6Baggage Claim §r(Collect your luggage)"),
                false);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
