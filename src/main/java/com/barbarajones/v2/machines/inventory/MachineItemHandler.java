package com.barbarajones.v2.machines.inventory;

import java.util.function.Predicate;

import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.ItemStackHandler;

import com.barbarajones.v2.machines.KraveFuels;
import com.barbarajones.v2.machines.MachineKind;
import com.barbarajones.v2.machines.MachineSlots;

/**
 * The five-slot backing store every machine uses.
 *
 * <p>Validity is enforced here rather than in the menu because the menu is only
 * one of the ways items arrive - hoppers, conveyors, extractors and other mods'
 * pipes all come in through the capability and would otherwise bypass every
 * restriction. A slot that is dead for this {@link MachineKind} rejects
 * insertion outright, so a Depot can never be fed syrup and a Grinder can never
 * have something stuffed into the input slot the Mixer would have used.
 */
public class MachineItemHandler extends ItemStackHandler {

    private final MachineKind kind;
    private final Predicate<ItemStack> inputFilter;
    private final Runnable onChanged;

    public MachineItemHandler(MachineKind kind, Predicate<ItemStack> inputFilter, Runnable onChanged) {
        super(MachineSlots.SIZE);
        this.kind = kind;
        this.inputFilter = inputFilter;
        this.onChanged = onChanged;
    }

    @Override
    protected void onContentsChanged(int slot) {
        onChanged.run();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (slot == MachineSlots.FUEL) {
            return kind.hasFuel && KraveFuels.isFuel(stack);
        }
        if (slot == MachineSlots.OUTPUT) {
            // Only the machine itself writes results, via setStackInSlot.
            return false;
        }
        return slot < kind.inputCount() && inputFilter.test(stack);
    }

    /**
     * Machine-internal output insertion, bypassing {@link #isItemValid}.
     *
     * @param simulate when true nothing is written; used to decide whether a
     *                 recipe may start at all, so a finished craft is never
     *                 dropped on the floor for want of room.
     * @return true if the whole stack fits
     */
    public boolean pushResult(ItemStack result, boolean simulate) {
        if (result.isEmpty()) {
            return true;
        }
        ItemStack existing = getStackInSlot(MachineSlots.OUTPUT);
        if (existing.isEmpty()) {
            if (result.getCount() > Math.min(getSlotLimit(MachineSlots.OUTPUT), result.getMaxStackSize())) {
                return false;
            }
            if (!simulate) {
                setStackInSlot(MachineSlots.OUTPUT, result.copy());
            }
            return true;
        }
        if (!ItemStack.isSameItemSameTags(existing, result)) {
            return false;
        }
        int limit = Math.min(getSlotLimit(MachineSlots.OUTPUT), existing.getMaxStackSize());
        if (existing.getCount() + result.getCount() > limit) {
            return false;
        }
        if (!simulate) {
            existing.grow(result.getCount());
            onContentsChanged(MachineSlots.OUTPUT);
        }
        return true;
    }

    public MachineKind kind() {
        return kind;
    }
}
