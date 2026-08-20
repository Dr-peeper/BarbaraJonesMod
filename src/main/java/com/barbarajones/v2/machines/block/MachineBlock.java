package com.barbarajones.v2.machines.block;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.network.NetworkHooks;

import com.barbarajones.v2.machines.KraveMachines;
import com.barbarajones.v2.machines.MachineKind;
import com.barbarajones.v2.machines.MachineSlots;
import com.barbarajones.v2.machines.blockentity.MachineBlockEntity;

/**
 * The block half of every Krave machine. One class, seven registrations, each
 * carrying a different {@link MachineKind}.
 *
 * <p>{@code RUNNING} is a real blockstate property rather than block-entity data
 * because that is what lets the model swap to the animated "working" texture with
 * no renderer and no packet - the block update that flips it is already being
 * sent, and vanilla's model system does the rest. It also means a running machine
 * still looks running to a player who logs in next to it, which block-entity-only
 * state would not.
 */
public class MachineBlock extends BaseEntityBlock {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");

    private final MachineKind kind;

    public MachineBlock(MachineKind kind, Properties props) {
        super(props);
        this.kind = kind;
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(RUNNING, Boolean.FALSE));
    }

    public MachineKind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RUNNING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ---- placement and rotation --------------------------------------------

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Front faces the player who placed it, exactly like a furnace. The front
        // is also the ejection face, so "place machine, then put the belt where
        // you are standing" is the natural build order.
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    // ---- interaction --------------------------------------------------------

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MachineBlockEntity machine && player instanceof ServerPlayer serverPlayer) {
            // The position rides along so the client-side menu can find the block
            // entity and read its kind - that is what lets one MenuType serve all
            // seven machines with seven different slot layouts.
            NetworkHooks.openScreen(serverPlayer, machine, buf -> buf.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    // ---- lifecycle ----------------------------------------------------------

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, KraveMachines.MACHINE_BLOCK_ENTITY.get(), MachineBlockEntity::serverTick);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MachineBlockEntity machine) {
                for (int slot = 0; slot < MachineSlots.SIZE; slot++) {
                    ItemStack stack = machine.items().getStackInSlot(slot);
                    if (!stack.isEmpty()) {
                        net.minecraft.world.Containers.dropItemStack(level,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                    }
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    // ---- comparator ---------------------------------------------------------

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /**
     * Comparator output from the output slot's fill level, so a player can build
     * "stop the grinder when the buffer is full" without a mod-specific block.
     * The Depot reports its input slot instead, since that is the only slot it has.
     */
    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MachineBlockEntity machine)) {
            return 0;
        }
        int slot = kind.hasOutput ? MachineSlots.OUTPUT : MachineSlots.INPUT_0;
        ItemStack stack = machine.items().getStackInSlot(slot);
        if (stack.isEmpty()) {
            return 0;
        }
        int limit = Math.min(machine.items().getSlotLimit(slot), stack.getMaxStackSize());
        return 1 + (int) (14.0F * stack.getCount() / limit);
    }
}
