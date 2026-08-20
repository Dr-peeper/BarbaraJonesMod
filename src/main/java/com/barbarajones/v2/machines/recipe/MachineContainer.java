package com.barbarajones.v2.machines.recipe;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.IItemHandler;

/**
 * A read-only {@link Container} view over a machine's three input slots.
 *
 * <p>{@code Recipe<C extends Container>} forces a Container even when the real
 * storage is a Forge {@code IItemHandler}, so this adapts one to the other. It is
 * deliberately read-only: consuming inputs is the block entity's job and goes
 * through the handler directly, because the handler is what fires
 * {@code onContentsChanged} and invalidates the recipe cache. If recipes could
 * mutate through this view those invalidations would be silently skipped.
 */
public final class MachineContainer implements Container {

    private final IItemHandler handler;
    private final int[] slots;

    public MachineContainer(IItemHandler handler, int[] slots) {
        this.handler = handler;
        this.slots = slots;
    }

    @Override
    public int getContainerSize() {
        return slots.length;
    }

    @Override
    public boolean isEmpty() {
        for (int slot : slots) {
            if (!handler.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < slots.length ? handler.getStackInSlot(slots[index]) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        // no-op: see class javadoc
    }

    @Override
    public void setChanged() {
        // no-op: this view never mutates
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        // no-op: see class javadoc
    }
}
