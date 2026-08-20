package com.barbarajones.v2.machines.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import com.barbarajones.v2.machines.KraveMachines;
import com.barbarajones.v2.machines.block.KraveConveyorBlock;

/**
 * A one-block length of Krave conveyor.
 *
 * <p>Items on a belt are not teleported from one inventory to the next, they
 * physically travel: each of the four lanes holds a stack and a tick counter, and
 * a stack only becomes available to the next block once its counter reaches
 * {@link #TRANSIT_TICKS}. That is the difference between a belt and a chain of
 * hoppers, and it is why the renderer has something to draw.
 *
 * <p>Both sides tick. The server advances counters and does the hand-off; the
 * client advances counters purely so the item slides smoothly without a packet
 * per tick. Cargo changes - a stack arriving, a stack leaving - are the only
 * things that sync, which is a handful of packets per belt per second instead of
 * twenty.
 */
public class KraveConveyorBlockEntity extends BlockEntity {

    /** Ticks for an item to cross one belt block. One second, so belts read as slow and physical. */
    public static final int TRANSIT_TICKS = 20;
    /** Independent stacks a single belt block can carry. */
    public static final int LANES = 4;

    private final NonNullList<ItemStack> cargo = NonNullList.withSize(LANES, ItemStack.EMPTY);
    private final int[] progress = new int[LANES];

    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> new BeltHandler());

    public KraveConveyorBlockEntity(BlockPos pos, BlockState state) {
        super(KraveMachines.CONVEYOR_BLOCK_ENTITY.get(), pos, state);
    }

    // ---- read access for the renderer ---------------------------------------

    public ItemStack cargo(int lane) {
        return cargo.get(lane);
    }

    /** 0..1 along the belt. Fed the partial tick so the item does not step frame to frame. */
    public float laneProgress(int lane, float partialTick) {
        if (cargo.get(lane).isEmpty()) {
            return 0.0F;
        }
        float raw = (progress[lane] + partialTick) / TRANSIT_TICKS;
        return raw > 1.0F ? 1.0F : raw;
    }

    // ---- ticking ------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, KraveConveyorBlockEntity belt) {
        Direction facing = state.getValue(KraveConveyorBlock.FACING);
        boolean changed = false;
        for (int lane = 0; lane < LANES; lane++) {
            if (belt.cargo.get(lane).isEmpty()) {
                continue;
            }
            if (belt.progress[lane] < TRANSIT_TICKS) {
                belt.progress[lane]++;
                continue;
            }
            changed |= belt.handOff(level, pos, facing, lane);
        }
        if (changed) {
            belt.sync();
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, KraveConveyorBlockEntity belt) {
        for (int lane = 0; lane < LANES; lane++) {
            if (!belt.cargo.get(lane).isEmpty() && belt.progress[lane] < TRANSIT_TICKS) {
                belt.progress[lane]++;
            }
        }
    }

    /**
     * Tries to push one finished lane into whatever the belt points at.
     *
     * <p>If there is nothing there, or it is full, the stack stays put and the
     * belt backs up. Belts never spit items onto the floor: an automation line
     * that quietly carpets the base in dropped cocoa when one machine jams is
     * worse than one that visibly stops.
     */
    private boolean handOff(Level level, BlockPos pos, Direction facing, int lane) {
        BlockEntity target = level.getBlockEntity(pos.relative(facing));
        if (target == null) {
            return false;
        }
        IItemHandler sink = target.getCapability(ForgeCapabilities.ITEM_HANDLER, facing.getOpposite()).orElse(null);
        if (sink == null) {
            return false;
        }
        ItemStack moving = cargo.get(lane);
        ItemStack remaining = moving.copy();
        for (int slot = 0; slot < sink.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = sink.insertItem(slot, remaining, false);
        }
        if (remaining.getCount() == moving.getCount()) {
            return false;
        }
        if (remaining.isEmpty()) {
            cargo.set(lane, ItemStack.EMPTY);
            progress[lane] = 0;
        } else {
            cargo.set(lane, remaining);
        }
        setChanged();
        return true;
    }

    private void sync() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Everything currently riding this belt, for the block-break drop. */
    public NonNullList<ItemStack> contents() {
        return cargo;
    }

    // ---- capability ---------------------------------------------------------

    /**
     * The belt as an {@code IItemHandler}: four slots, one per lane.
     *
     * <p>Insertion starts an item at the beginning of the belt. Extraction only
     * offers lanes that have finished travelling, so a hopper under a belt takes
     * items off the end rather than plucking them out of the middle.
     */
    private class BeltHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return LANES;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot >= 0 && slot < LANES ? cargo.get(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot < 0 || slot >= LANES || stack.isEmpty() || !cargo.get(slot).isEmpty()) {
                return stack;
            }
            int accepted = Math.min(stack.getCount(), stack.getMaxStackSize());
            if (!simulate) {
                cargo.set(slot, stack.copyWithCount(accepted));
                progress[slot] = 0;
                setChanged();
                sync();
            }
            return accepted >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - accepted);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= LANES || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack held = cargo.get(slot);
            if (held.isEmpty() || progress[slot] < TRANSIT_TICKS) {
                return ItemStack.EMPTY;
            }
            int taken = Math.min(amount, held.getCount());
            ItemStack result = held.copyWithCount(taken);
            if (!simulate) {
                held.shrink(taken);
                if (held.isEmpty()) {
                    cargo.set(slot, ItemStack.EMPTY);
                    progress[slot] = 0;
                }
                setChanged();
                sync();
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= 0 && slot < LANES && cargo.get(slot).isEmpty();
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && !remove) {
            // The face items leave by is not a face items may be pushed into -
            // otherwise a machine sitting at the end of a belt would happily eject
            // back onto it and the line would run in a circle.
            if (side != null && side == getBlockState().getValue(KraveConveyorBlock.FACING)) {
                return LazyOptional.empty();
            }
            return handler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        handler.invalidate();
    }

    // ---- persistence and sync ----------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, cargo, true);
        tag.putIntArray("Progress", progress.clone());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cargo.clear();
        ContainerHelper.loadAllItems(tag, cargo);
        int[] saved = tag.getIntArray("Progress");
        for (int lane = 0; lane < LANES; lane++) {
            progress[lane] = lane < saved.length ? saved[lane] : 0;
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
