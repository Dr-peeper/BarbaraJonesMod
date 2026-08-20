package com.barbarajones.v2.build.block;

import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * The Krave Foundation Stone. One per placed building, written into the floor
 * where the definition asked for it.
 *
 * <p>Two jobs. It is the building's identity - the block entity underneath
 * knows which structure this is, who put it there and when - and it is the
 * undo button. Break it inside the refund window and the whole building comes
 * back down and hands you the schematic. Break it after, and it is just a
 * block; the building stays.
 *
 * <p>Right-clicking reports how long is left, so the window is discoverable
 * rather than a secret the player has to read a changelog to learn.
 */
public class KraveCoreBlock extends BaseEntityBlock {

    public KraveCoreBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new KraveCoreBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // BaseEntityBlock defaults to INVISIBLE, which would make the one block
        // the player is meant to find the one block they cannot see.
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof KraveCoreBlockEntity core)) {
            return InteractionResult.PASS;
        }
        StructureDef def = core.def();
        Component name = def == null
                ? Component.translatable("barbarajones.build.unknown_building")
                : Component.translatable(def.nameKey());
        long left = core.refundTicksLeft(level.getGameTime());
        if (left > 0L) {
            player.displayClientMessage(Component.translatable(
                    "barbarajones.build.core.window", name, left / 20L), true);
        } else {
            player.displayClientMessage(Component.translatable("barbarajones.build.core.settled", name), true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server) {
            BlockEntity be = server.getBlockEntity(pos);
            if (be instanceof KraveCoreBlockEntity core) {
                Component message = core.tryRefund(server, player);
                if (message != null) {
                    player.displayClientMessage(message, true);
                }
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
