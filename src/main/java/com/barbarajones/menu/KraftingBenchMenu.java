package com.barbarajones.menu;

import com.barbarajones.content.ModBlocks;
import com.barbarajones.content.ModItems;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * Krafting Bench: three fixed, type-restricted input slots (Krave Pickaxe,
 * Axe, Shovel) and one output slot (Krave Multitool). Deliberately not
 * recipe-driven - it's the only place the multitool can be made, since a
 * normal crafting table has no way to know these three specific slots exist.
 */
public class KraftingBenchMenu extends AbstractContainerMenu {

    private static final int SLOT_PICKAXE = 0;
    private static final int SLOT_AXE = 1;
    private static final int SLOT_SHOVEL = 2;

    private final Container input = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            KraftingBenchMenu.this.slotsChanged(this);
        }
    };
    private final ResultContainer output = new ResultContainer();
    private final ContainerLevelAccess access;

    public KraftingBenchMenu(int id, Inventory playerInv) {
        this(id, playerInv, ContainerLevelAccess.NULL);
    }

    public KraftingBenchMenu(int id, Inventory playerInv, ContainerLevelAccess access) {
        super(ModMenus.KRAFTING_BENCH.get(), id);
        this.access = access;

        addSlot(new RestrictedSlot(input, SLOT_PICKAXE, 44, 17, s -> s.is(ModItems.KRAVE_PICKAXE.get())));
        addSlot(new RestrictedSlot(input, SLOT_AXE, 62, 17, s -> s.is(ModItems.KRAVE_AXE.get())));
        addSlot(new RestrictedSlot(input, SLOT_SHOVEL, 53, 35, s -> s.is(ModItems.KRAVE_SHOVEL.get())));
        addSlot(new OutputSlot(playerInv.player, output, 116, 26));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void slotsChanged(Container c) {
        ItemStack pick = input.getItem(SLOT_PICKAXE);
        ItemStack axe = input.getItem(SLOT_AXE);
        ItemStack shovel = input.getItem(SLOT_SHOVEL);
        boolean ready = pick.is(ModItems.KRAVE_PICKAXE.get())
                && axe.is(ModItems.KRAVE_AXE.get())
                && shovel.is(ModItems.KRAVE_SHOVEL.get());
        output.setItem(0, ready ? new ItemStack(ModItems.KRAVE_MULTITOOL.get()) : ItemStack.EMPTY);
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return result;
        }
        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index == 3) {
            // out of the output, into the player's inventory
            if (!moveItemStackTo(stack, 4, 40, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, result);
        } else if (index < 3) {
            // out of an input slot, into the player's inventory
            if (!moveItemStackTo(stack, 4, 40, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            // out of the player's inventory, into whichever input will take it
            if (!moveItemStackTo(stack, 0, 3, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == result.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.KRAFTING_BENCH.get());
    }

    /** Only accepts the one item this exact slot exists for - rejects everything else outright. */
    private static class RestrictedSlot extends Slot {
        private final Predicate<ItemStack> accepts;

        RestrictedSlot(Container container, int index, int x, int y, Predicate<ItemStack> accepts) {
            super(container, index, x, y);
            this.accepts = accepts;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return accepts.test(stack);
        }
    }

    /** Read-only result slot: consumes exactly one of each input the moment the multitool is taken. */
    private class OutputSlot extends Slot {
        OutputSlot(Player player, Container container, int x, int y) {
            super(container, 0, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player taker, ItemStack stack) {
            input.removeItem(SLOT_PICKAXE, 1);
            input.removeItem(SLOT_AXE, 1);
            input.removeItem(SLOT_SHOVEL, 1);
            super.onTake(taker, stack);
        }
    }
}
