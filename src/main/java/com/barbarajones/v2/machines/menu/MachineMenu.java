package com.barbarajones.v2.machines.menu;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import com.barbarajones.v2.machines.KraveMachines;
import com.barbarajones.v2.machines.MachineKind;
import com.barbarajones.v2.machines.MachineSlots;
import com.barbarajones.v2.machines.blockentity.MachineBlockEntity;

/**
 * One menu type for all seven machines.
 *
 * <p>The number and position of the slots comes from the {@link MachineKind},
 * which the client reads off the block entity at the position sent in the open
 * packet. That is the trick that keeps this to a single {@code MenuType} instead
 * of seven near-identical ones - the Mixer gets three input slots and the Grinder
 * gets one, from the same class.
 *
 * <p>Progress, fuel and status ride the vanilla {@link ContainerData} channel,
 * which resyncs only changed shorts and only to players who have the screen open.
 * No custom packet is needed for any of it.
 */
public class MachineMenu extends AbstractContainerMenu {

    /** Standard 176x166 inventory geometry - three rows at 84, hotbar at 142. */
    private static final int INV_X = 8;
    private static final int INV_Y = 84;
    private static final int HOTBAR_Y = 142;

    private static final int FUEL_X = 56;
    private static final int FUEL_Y = 53;
    private static final int OUTPUT_X = 116;
    private static final int OUTPUT_Y = 35;

    @Nullable
    private final MachineBlockEntity machine;
    private final MachineKind kind;
    private final ContainerData data;

    /** Slots belonging to the machine, i.e. everything before the player inventory. */
    private final int machineSlotCount;

    public MachineMenu(int id, Inventory playerInv, FriendlyByteBuf buf) {
        this(id, playerInv, resolve(playerInv, buf.readBlockPos()));
    }

    public MachineMenu(int id, Inventory playerInv, @Nullable MachineBlockEntity machine) {
        super(KraveMachines.MACHINE_MENU.get(), id);
        this.machine = machine;

        // A null block entity means the client opened the screen a tick before the
        // chunk data landed. Rather than crash, fall back to a detached, empty
        // Grinder; the next container sync replaces its contents.
        IItemHandler items;
        if (machine != null) {
            this.kind = machine.kind();
            items = machine.items();
            this.data = machine.data();
        } else {
            this.kind = MachineKind.GRINDER;
            items = new ItemStackHandler(MachineSlots.SIZE);
            this.data = new SimpleContainerData(MachineBlockEntity.DATA_COUNT);
        }

        int count = 0;
        for (int i = 0; i < kind.inputCount(); i++) {
            addSlot(new SlotItemHandler(items, MachineSlots.INPUTS[i], kind.inputX[i], kind.inputY));
            count++;
        }
        if (kind.hasFuel) {
            addSlot(new SlotItemHandler(items, MachineSlots.FUEL, FUEL_X, FUEL_Y));
            count++;
        }
        if (kind.hasOutput) {
            addSlot(new OutputSlot(items, MachineSlots.OUTPUT, OUTPUT_X, OUTPUT_Y));
            count++;
        }
        this.machineSlotCount = count;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, INV_X + col * 18, HOTBAR_Y));
        }

        addDataSlots(data);
    }

    @Nullable
    private static MachineBlockEntity resolve(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        return be instanceof MachineBlockEntity found ? found : null;
    }

    // ---- read access for the screen ----------------------------------------

    public MachineKind kind() {
        return kind;
    }

    /** 0..1, or 0 when idle. */
    public float progressFraction() {
        int total = data.get(MachineBlockEntity.DATA_WORK_TIME);
        if (total <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, data.get(MachineBlockEntity.DATA_PROGRESS) / (float) total);
    }

    /** 0..1 of the currently burning syrup, or 0 when nothing is lit. */
    public float fuelFraction() {
        int capacity = data.get(MachineBlockEntity.DATA_FUEL_CAPACITY);
        if (capacity <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, data.get(MachineBlockEntity.DATA_FUEL) / (float) capacity);
    }

    public int status() {
        return data.get(MachineBlockEntity.DATA_STATUS);
    }

    public int shippedTotal() {
        return machine == null ? 0 : machine.shippedTotal();
    }

    // ---- vanilla contract ---------------------------------------------------

    @Override
    public boolean stillValid(Player player) {
        if (machine == null || machine.isRemoved()) {
            return false;
        }
        return player.distanceToSqr(Vec3.atCenterOf(machine.getBlockPos())) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        int playerStart = machineSlotCount;
        int playerEnd = slots.size();

        if (index < machineSlotCount) {
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, original);
        } else {
            // Into the machine: try the inputs first, then the fuel slot. Both
            // reject anything they do not accept, so shift-clicking syrup lands in
            // the burner and shift-clicking cocoa lands in the hopper, with no
            // per-item special casing here.
            if (!moveItemStackTo(stack, 0, machineSlotCount, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, stack);
        return original;
    }

    /** Take-only. The machine writes here; nothing else may. */
    private static class OutputSlot extends SlotItemHandler {
        OutputSlot(IItemHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
