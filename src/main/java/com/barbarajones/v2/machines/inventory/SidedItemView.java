package com.barbarajones.v2.machines.inventory;

import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.IItemHandler;

/**
 * A face-specific window onto a {@link MachineItemHandler}.
 *
 * <p>Machines expose different slots on different faces, the way a furnace does,
 * so that automation reads the way players already expect:
 * <ul>
 *   <li><b>bottom</b> - the output slot, extract only. A hopper underneath collects product.</li>
 *   <li><b>every other face</b> - live inputs and the fuel slot for insertion, the
 *       output slot for extraction. A hopper on top or a conveyor at the side can
 *       feed cocoa or syrup and pull finished goods.</li>
 * </ul>
 *
 * <p>Permissions are per slot, not per view, so one object can say "you may put
 * syrup in but you may not take it back out" - which is what stops a hopper
 * pointed at a Grinder's side from steadily draining its own fuel supply back out
 * of it, a classic and maddening automation bug.
 *
 * <p>Views are built once at block-entity construction and held for its lifetime.
 * Allocating a fresh wrapper inside {@code getCapability} would mean a new object
 * per neighbour query per tick, which at a few hundred machines is real
 * garbage-collector pressure for no benefit.
 */
public class SidedItemView implements IItemHandler {

    private final MachineItemHandler backing;
    private final int[] slots;
    private final boolean[] insertMask;
    private final boolean[] extractMask;

    public SidedItemView(MachineItemHandler backing, int[] slots, boolean[] insertMask, boolean[] extractMask) {
        if (slots.length != insertMask.length || slots.length != extractMask.length) {
            throw new IllegalArgumentException("slot/permission length mismatch");
        }
        this.backing = backing;
        this.slots = slots;
        this.insertMask = insertMask;
        this.extractMask = extractMask;
    }

    private boolean inRange(int slot) {
        return slot >= 0 && slot < slots.length;
    }

    @Override
    public int getSlots() {
        return slots.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inRange(slot) ? backing.getStackInSlot(slots[slot]) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!inRange(slot) || !insertMask[slot]) {
            return stack;
        }
        return backing.insertItem(slots[slot], stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!inRange(slot) || !extractMask[slot]) {
            return ItemStack.EMPTY;
        }
        return backing.extractItem(slots[slot], amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inRange(slot) ? backing.getSlotLimit(slots[slot]) : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return inRange(slot) && insertMask[slot] && backing.isItemValid(slots[slot], stack);
    }
}
