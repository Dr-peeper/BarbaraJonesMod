package com.barbarajones.v2.machines.blockentity;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.RegistryObject;

import com.barbarajones.v2.machines.KraveFuels;
import com.barbarajones.v2.machines.KraveMachines;
import com.barbarajones.v2.machines.MachineKind;
import com.barbarajones.v2.machines.MachineSlots;
import com.barbarajones.v2.machines.block.MachineBlock;
import com.barbarajones.v2.machines.inventory.MachineItemHandler;
import com.barbarajones.v2.machines.inventory.SidedItemView;
import com.barbarajones.v2.machines.menu.MachineMenu;
import com.barbarajones.v2.machines.recipe.MachineContainer;
import com.barbarajones.v2.machines.recipe.MachineRecipe;
import com.barbarajones.v2.machines.recipe.RecipeLookup;
import com.barbarajones.v2.machines.recipe.SizedIngredient;

/**
 * One block entity type behind all seven Krave machines.
 *
 * <p>Which machine this is comes from the block, not from the block entity, so a
 * Grinder and a Toaster are the same class holding a different
 * {@link MachineKind} and a different {@link MachineProcess}. That is what keeps
 * the chain coherent: fuel, progress, capability exposure, NBT, auto-eject and
 * GUI behave identically everywhere, and adding an eighth machine is one enum
 * constant and one recipe type.
 *
 * <p>Recipe matching is cached. The matched recipe is held alongside the
 * {@link RecipeLookup} generation it was matched in, and re-resolved only when
 * the inputs change or a datapack reload bumps that generation. Re-querying the
 * recipe manager every tick is the standard way a machine mod eats a server's
 * tick budget, and it is entirely avoidable.
 */
public class MachineBlockEntity extends BlockEntity implements MenuProvider {

    /** How often a machine pushes its output onward, in ticks. */
    private static final int EJECT_INTERVAL = 8;
    /** Items moved per auto-eject attempt. */
    private static final int EJECT_AMOUNT = 8;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_WORK_TIME = 1;
    public static final int DATA_FUEL = 2;
    public static final int DATA_FUEL_CAPACITY = 3;
    public static final int DATA_STATUS = 4;
    public static final int DATA_COUNT = 5;

    /** {@link #DATA_STATUS} values, read by the screen to pick a status line. */
    public static final int STATUS_OK = 0;
    public static final int STATUS_NO_FUEL = 1;
    public static final int STATUS_OUTPUT_FULL = 2;
    public static final int STATUS_NO_VILLAGE = 3;

    private final MachineKind kind;
    private final MachineProcess process;
    private final MachineItemHandler items;

    private final LazyOptional<IItemHandler> generalCap;
    private final LazyOptional<IItemHandler> outputCap;

    private int progress;
    private int workTime;
    private int fuel;
    private int fuelCapacity;
    private int status = STATUS_OK;
    /** Depot only: lifetime cases shipped, shown in the GUI so the number is never lost. */
    private int shippedTotal;

    private int ejectCooldown;

    // ---- recipe cache -------------------------------------------------------
    @Nullable
    private MachineRecipe cachedRecipe;
    private int cachedGeneration = -1;
    private boolean inputsDirty = true;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_WORK_TIME -> workTime;
                case DATA_FUEL -> fuel;
                case DATA_FUEL_CAPACITY -> fuelCapacity;
                case DATA_STATUS -> status;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> progress = value;
                case DATA_WORK_TIME -> workTime = value;
                case DATA_FUEL -> fuel = value;
                case DATA_FUEL_CAPACITY -> fuelCapacity = value;
                case DATA_STATUS -> status = value;
                default -> { }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(KraveMachines.MACHINE_BLOCK_ENTITY.get(), pos, state);
        this.kind = state.getBlock() instanceof MachineBlock machine ? machine.kind() : MachineKind.GRINDER;
        this.process = MachineProcesses.forKind(this.kind);
        this.items = new MachineItemHandler(kind, this::acceptsInput, this::onInventoryChanged);

        this.generalCap = LazyOptional.of(this::buildGeneralView);
        this.outputCap = LazyOptional.of(this::buildOutputView);
    }

    // ---- identity -----------------------------------------------------------

    public MachineKind kind() {
        return kind;
    }

    public MachineItemHandler items() {
        return items;
    }

    public ContainerData data() {
        return data;
    }

    public int shippedTotal() {
        return shippedTotal;
    }

    public void addShipped(int cases) {
        shippedTotal += cases;
    }

    public void setStatus(int newStatus) {
        this.status = newStatus;
    }

    public int status() {
        return status;
    }

    // ---- recipe cache -------------------------------------------------------

    /** A read-only container view over exactly the live input slots. */
    public MachineContainer inputView() {
        int count = kind.inputCount();
        int[] slots = new int[count];
        System.arraycopy(MachineSlots.INPUTS, 0, slots, 0, count);
        return new MachineContainer(items, slots);
    }

    /**
     * The recipe currently matched by the inputs, re-scanning only when the
     * inputs changed or the recipe index was rebuilt.
     */
    @Nullable
    public MachineRecipe resolveRecipe(RecipeType<MachineRecipe> type) {
        if (level == null) {
            return null;
        }
        int generation = RecipeLookup.generation();
        if (!inputsDirty && generation == cachedGeneration) {
            return cachedRecipe;
        }
        cachedRecipe = RecipeLookup.find(level, type, inputView());
        cachedGeneration = generation;
        inputsDirty = false;
        return cachedRecipe;
    }

    private void onInventoryChanged() {
        inputsDirty = true;
        setChanged();
    }

    /**
     * Whether an input slot will take this stack at all.
     *
     * <p>Recipe machines only accept items some loaded recipe of their type
     * actually uses. Without that, a hopper cheerfully fills a Grinder with
     * cobblestone and the player has to dig it back out by hand.
     */
    private boolean acceptsInput(ItemStack stack) {
        switch (kind) {
            case DEPOT:
                return stack.is(KraveMachines.BOXED_KRAVE.get());
            case PLANTATION:
                // Beans only. A slot that accepts bone meal and then does nothing
                // with it is worse than a slot that refuses it.
                return stack.is(Items.COCOA_BEANS);
            default:
                RegistryObject<RecipeType<MachineRecipe>> type = KraveMachines.recipeTypeFor(kind);
                if (level == null || type == null) {
                    return true;
                }
                for (MachineRecipe recipe : RecipeLookup.all(level, type.get())) {
                    for (SizedIngredient input : recipe.inputs()) {
                        if (input.ingredient().test(stack)) {
                            return true;
                        }
                    }
                }
                return false;
        }
    }

    // ---- ticking ------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        boolean running = machine.runOnce();

        if (state.getValue(MachineBlock.RUNNING) != running) {
            level.setBlock(pos, state.setValue(MachineBlock.RUNNING, running), Block.UPDATE_ALL);
        }
        if (machine.kind.hasOutput && --machine.ejectCooldown <= 0) {
            machine.ejectCooldown = EJECT_INTERVAL;
            machine.ejectOutput(state);
        }
    }

    /**
     * One tick of work.
     *
     * @return whether the machine should render as running this tick
     */
    private boolean runOnce() {
        int needed = process.workTime(this);
        if (needed == MachineProcess.IDLE) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            workTime = 0;
            return false;
        }

        workTime = needed;
        int cost = kind.hasFuel ? process.fuelPerTick(this) : 0;
        if (cost > 0 && fuel < cost && !consumeFuelItem()) {
            status = STATUS_NO_FUEL;
            return false;
        }
        if (cost > 0) {
            fuel -= cost;
        }
        if (status == STATUS_NO_FUEL) {
            status = STATUS_OK;
        }

        progress++;
        if (progress >= workTime) {
            progress = 0;
            process.complete(this);
            inputsDirty = true;
            // Only mark dirty on a completed operation. Calling setChanged() on
            // every tick of progress would add every running machine in the world
            // to the chunk-save set twenty times a second to persist a counter
            // that costs at most one craft if it is lost to a crash.
            setChanged();
        }
        return true;
    }

    /**
     * Burns one item out of the fuel slot.
     *
     * @return true if anything was lit
     */
    private boolean consumeFuelItem() {
        ItemStack stack = items.getStackInSlot(MachineSlots.FUEL);
        int units = KraveFuels.burnUnits(stack);
        if (units <= 0) {
            return false;
        }
        items.extractItem(MachineSlots.FUEL, 1, false);
        fuel += units;
        fuelCapacity = units;
        return true;
    }

    /**
     * Pushes finished product into whatever the machine faces.
     *
     * <p>Machines eject out the front - the face with the chute on it - so the
     * layout a player builds is the layout they can see: machine, conveyor,
     * machine. Nothing is ever ejected onto the ground; if the target is full or
     * absent the product simply stays in the output slot, where the player can
     * find it.
     */
    private void ejectOutput(BlockState state) {
        if (level == null || items.getStackInSlot(MachineSlots.OUTPUT).isEmpty()) {
            return;
        }
        Direction facing = state.getValue(MachineBlock.FACING);
        BlockEntity target = level.getBlockEntity(worldPosition.relative(facing));
        if (target == null) {
            return;
        }
        IItemHandler sink = target.getCapability(ForgeCapabilities.ITEM_HANDLER, facing.getOpposite()).orElse(null);
        if (sink == null) {
            return;
        }
        ItemStack moving = items.extractItem(MachineSlots.OUTPUT, EJECT_AMOUNT, true);
        if (moving.isEmpty()) {
            return;
        }
        ItemStack leftover = ItemHandlerHelper.insertItem(sink, moving.copy(), false);
        int moved = moving.getCount() - leftover.getCount();
        if (moved > 0) {
            items.extractItem(MachineSlots.OUTPUT, moved, false);
        }
    }

    // ---- capabilities -------------------------------------------------------

    private SidedItemView buildGeneralView() {
        int inputs = kind.inputCount();
        int size = inputs + (kind.hasFuel ? 1 : 0) + (kind.hasOutput ? 1 : 0);
        int[] slots = new int[size];
        boolean[] insert = new boolean[size];
        boolean[] extract = new boolean[size];
        int i = 0;
        for (int input = 0; input < inputs; input++) {
            slots[i] = MachineSlots.INPUTS[input];
            insert[i] = true;
            extract[i] = false;
            i++;
        }
        if (kind.hasFuel) {
            slots[i] = MachineSlots.FUEL;
            insert[i] = true;
            // Insert-only on purpose: a hopper pointed at a machine must not be
            // able to steadily drain back out the syrup it just put in.
            extract[i] = false;
            i++;
        }
        if (kind.hasOutput) {
            slots[i] = MachineSlots.OUTPUT;
            insert[i] = false;
            extract[i] = true;
        }
        return new SidedItemView(items, slots, insert, extract);
    }

    private SidedItemView buildOutputView() {
        if (!kind.hasOutput) {
            return new SidedItemView(items, new int[0], new boolean[0], new boolean[0]);
        }
        return new SidedItemView(items,
                new int[] { MachineSlots.OUTPUT },
                new boolean[] { false },
                new boolean[] { true });
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && !remove) {
            return (side == Direction.DOWN ? outputCap : generalCap).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        generalCap.invalidate();
        outputCap.invalidate();
    }

    // ---- persistence --------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.putInt("Progress", progress);
        tag.putInt("WorkTime", workTime);
        tag.putInt("Fuel", fuel);
        tag.putInt("FuelCapacity", fuelCapacity);
        tag.putInt("Shipped", shippedTotal);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Items")) {
            items.deserializeNBT(tag.getCompound("Items"));
        }
        progress = tag.getInt("Progress");
        workTime = tag.getInt("WorkTime");
        fuel = tag.getInt("Fuel");
        fuelCapacity = tag.getInt("FuelCapacity");
        shippedTotal = tag.getInt("Shipped");
        inputsDirty = true;
    }

    // ---- menu ---------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return Component.translatable(kind.menuTranslationKey());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInv, Player player) {
        return new MachineMenu(id, playerInv, this);
    }
}
