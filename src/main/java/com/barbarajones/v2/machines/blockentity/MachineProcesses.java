package com.barbarajones.v2.machines.blockentity;

import java.util.ArrayDeque;
import java.util.Deque;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.registries.RegistryObject;

import com.barbarajones.block.krave.KravePodBlock;
import com.barbarajones.block.krave.KraveWood;
import com.barbarajones.v2.machines.KraveMachines;
import com.barbarajones.v2.machines.MachineKind;
import com.barbarajones.v2.machines.MachineSlots;
import com.barbarajones.v2.machines.recipe.MachineRecipe;
import com.barbarajones.v2.machines.village.VillageLink;

/**
 * The three things a Krave machine can be doing.
 *
 * <p>A fresh instance is built per block entity rather than shared per kind,
 * because the plantation keeps a scan cache and sharing that between every
 * plantation in a world would have them all harvesting each other's pods.
 */
public final class MachineProcesses {

    private MachineProcesses() { }

    /** Drops an "output blocked" warning that no longer applies. */
    private static void clearBlocked(MachineBlockEntity machine) {
        if (machine.status() == MachineBlockEntity.STATUS_OUTPUT_FULL) {
            machine.setStatus(MachineBlockEntity.STATUS_OK);
        }
    }

    public static MachineProcess forKind(MachineKind kind) {
        return switch (kind) {
            case PLANTATION -> new PlantationProcess();
            case DEPOT -> new DepotProcess();
            default -> new RecipeProcess(kind);
        };
    }

    // =========================================================================
    // Recipe-driven: Grinder, Mixer, Extruder, Toaster, Boxer
    // =========================================================================

    /**
     * Runs whichever JSON recipe of this machine's {@code RecipeType} the inputs
     * currently satisfy.
     *
     * <p>There is no hard-coded knowledge of cocoa, dust, batter or cereal
     * anywhere in here. The Grinder is not "the machine that turns cocoa into
     * dust", it is "the machine that runs {@code barbarajones:grinding} recipes",
     * and a datapack can add a grinding recipe for anything at all without
     * touching a line of Java.
     */
    static final class RecipeProcess implements MachineProcess {

        private final MachineKind kind;

        RecipeProcess(MachineKind kind) {
            this.kind = kind;
        }

        private MachineRecipe current(MachineBlockEntity machine) {
            RegistryObject<RecipeType<MachineRecipe>> type = KraveMachines.recipeTypeFor(kind);
            return type == null ? null : machine.resolveRecipe(type.get());
        }

        @Override
        public int workTime(MachineBlockEntity machine) {
            MachineRecipe recipe = current(machine);
            if (recipe == null) {
                if (machine.status() == MachineBlockEntity.STATUS_OUTPUT_FULL) {
                    machine.setStatus(MachineBlockEntity.STATUS_OK);
                }
                return IDLE;
            }
            // Check the result fits BEFORE spending a single tick on it. A machine
            // that grinds for six seconds and then discovers it has nowhere to put
            // the dust either voids the dust or busy-loops; both are bugs players
            // notice and neither is necessary.
            if (!machine.items().pushResult(recipe.output(), true)) {
                machine.setStatus(MachineBlockEntity.STATUS_OUTPUT_FULL);
                return IDLE;
            }
            if (machine.status() == MachineBlockEntity.STATUS_OUTPUT_FULL) {
                machine.setStatus(MachineBlockEntity.STATUS_OK);
            }
            return recipe.time();
        }

        @Override
        public int fuelPerTick(MachineBlockEntity machine) {
            MachineRecipe recipe = current(machine);
            return recipe == null ? 1 : recipe.fuelPerTick();
        }

        @Override
        public void complete(MachineBlockEntity machine) {
            MachineRecipe recipe = current(machine);
            if (recipe == null) {
                return;
            }
            int[] assignment = recipe.match(machine.inputView());
            if (assignment == null) {
                return;
            }
            for (int i = 0; i < assignment.length; i++) {
                int slot = MachineSlots.INPUTS[assignment[i]];
                machine.items().extractItem(slot, recipe.inputs().get(i).count(), false);
            }
            machine.items().pushResult(recipe.output().copy(), false);
        }
    }

    // =========================================================================
    // Cocoa Plantation
    // =========================================================================

    /**
     * Harvests mature Krave Pods (and plain cocoa, which the pod block extends)
     * inside a fixed radius, and replants from its input slot.
     *
     * <p>The scan is the expensive part - a 13x9x13 box is over fifteen hundred
     * block lookups - so it runs at most once every {@value #RESCAN_INTERVAL}
     * ticks and its results are drained one pod per harvest cycle. Positions are
     * re-verified at harvest time because two seconds is long enough for a player
     * to have broken the pod, and acting on a stale scan is how a farm block ends
     * up duplicating items.
     */
    static final class PlantationProcess implements MachineProcess {

        /** Horizontal reach from the plantation block, in blocks. */
        static final int RADIUS = 6;
        /** Vertical reach, up and down. Krave trees are tall; pods are not all at foot height. */
        static final int HEIGHT = 5;
        /** Ticks between full rescans of the radius. */
        static final int RESCAN_INTERVAL = 60;
        /** Ticks to harvest one pod. */
        static final int HARVEST_TIME = 100;

        private final Deque<BlockPos> pending = new ArrayDeque<>();
        private int rescanCooldown;

        @Override
        public int workTime(MachineBlockEntity machine) {
            Level level = machine.getLevel();
            if (!(level instanceof ServerLevel server)) {
                return IDLE;
            }
            if (pending.isEmpty()) {
                // Nothing to pick means nothing is blocked, whatever the output
                // slot looks like - clear a stale warning rather than leaving the
                // player staring at "Output blocked" on an idle farm.
                clearBlocked(machine);
                if (rescanCooldown-- > 0) {
                    return IDLE;
                }
                rescanCooldown = RESCAN_INTERVAL;
                rescan(server, machine.getBlockPos());
                if (pending.isEmpty()) {
                    return IDLE;
                }
            }
            // Three beans is the largest yield a single pod can give, so checking
            // for room for three means a harvest can never be lost.
            if (!machine.items().pushResult(new ItemStack(Items.COCOA_BEANS, 3), true)) {
                machine.setStatus(MachineBlockEntity.STATUS_OUTPUT_FULL);
                return IDLE;
            }
            if (machine.status() == MachineBlockEntity.STATUS_OUTPUT_FULL) {
                machine.setStatus(MachineBlockEntity.STATUS_OK);
            }
            return HARVEST_TIME;
        }

        private void rescan(ServerLevel level, BlockPos origin) {
            pending.clear();
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    for (int dy = -HEIGHT; dy <= HEIGHT; dy++) {
                        cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                        if (!level.isLoaded(cursor)) {
                            continue;
                        }
                        if (isMaturePod(level.getBlockState(cursor))) {
                            pending.add(cursor.immutable());
                        }
                    }
                }
            }
        }

        private static boolean isMaturePod(BlockState state) {
            return state.getBlock() instanceof CocoaBlock
                    && state.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE;
        }

        @Override
        public void complete(MachineBlockEntity machine) {
            Level level = machine.getLevel();
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            while (!pending.isEmpty()) {
                BlockPos pos = pending.poll();
                if (!server.isLoaded(pos)) {
                    continue;
                }
                BlockState state = server.getBlockState(pos);
                if (!isMaturePod(state)) {
                    continue;
                }
                // Krave Pods are fatter than jungle cocoa - that is the whole
                // point of the tree, and the reason to build the plantation next
                // to one rather than shipping beans in from a jungle.
                int yield = state.getBlock() instanceof KravePodBlock ? 3 : 2;
                server.setBlock(pos, state.setValue(CocoaBlock.AGE, 0), Block.UPDATE_ALL);
                machine.items().pushResult(new ItemStack(Items.COCOA_BEANS, yield), false);
                server.playSound(null, machine.getBlockPos(), SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 0.4F, 1.4F);
                tryPlant(server, machine);
                return;
            }
        }

        /**
         * Spends one cocoa bean from the input slot to start a new pod on a bare
         * Krave trunk face, so a plantation left alone slowly fills its radius.
         */
        private void tryPlant(ServerLevel level, MachineBlockEntity machine) {
            ItemStack seeds = machine.items().getStackInSlot(MachineSlots.INPUT_0);
            if (!seeds.is(Items.COCOA_BEANS)) {
                return;
            }
            BlockPos origin = machine.getBlockPos();
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    for (int dy = -HEIGHT; dy <= HEIGHT; dy++) {
                        cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                        if (!level.isLoaded(cursor) || !level.getBlockState(cursor).isAir()) {
                            continue;
                        }
                        for (Direction dir : Direction.Plane.HORIZONTAL) {
                            BlockState pod = KraveWood.POD.defaultBlockState()
                                    .setValue(CocoaBlock.FACING, dir);
                            if (pod.canSurvive(level, cursor)) {
                                level.setBlock(cursor.immutable(), pod, Block.UPDATE_ALL);
                                machine.items().extractItem(MachineSlots.INPUT_0, 1, false);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // Krave Depot
    // =========================================================================

    /**
     * Ships Cases of Krave to the village.
     *
     * <p>The Depot never destroys a case it could not deliver. If no village
     * module is connected, {@link VillageLink#deliver} returns false, the case
     * stays in the slot and the GUI says so. An automation block that silently
     * eats the player's product because a downstream system was not wired up is
     * the worst possible failure here - hours of production vanish and nothing
     * explains why.
     */
    static final class DepotProcess implements MachineProcess {

        /** Ticks to load one case onto the cart. */
        static final int SHIP_TIME = 40;

        @Override
        public int workTime(MachineBlockEntity machine) {
            Level level = machine.getLevel();
            if (!(level instanceof ServerLevel server)) {
                return IDLE;
            }
            if (machine.items().getStackInSlot(MachineSlots.INPUT_0).isEmpty()) {
                if (machine.status() == MachineBlockEntity.STATUS_NO_VILLAGE) {
                    machine.setStatus(MachineBlockEntity.STATUS_OK);
                }
                return IDLE;
            }
            if (!VillageLink.isVillageInRange(server, machine.getBlockPos())) {
                machine.setStatus(MachineBlockEntity.STATUS_NO_VILLAGE);
                return IDLE;
            }
            return SHIP_TIME;
        }

        @Override
        public int fuelPerTick(MachineBlockEntity machine) {
            return 0;
        }

        @Override
        public void complete(MachineBlockEntity machine) {
            Level level = machine.getLevel();
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            if (machine.items().getStackInSlot(MachineSlots.INPUT_0).isEmpty()) {
                return;
            }
            if (!VillageLink.deliver(server, machine.getBlockPos(), 1)) {
                machine.setStatus(MachineBlockEntity.STATUS_NO_VILLAGE);
                return;
            }
            machine.items().extractItem(MachineSlots.INPUT_0, 1, false);
            machine.addShipped(1);
            machine.setStatus(MachineBlockEntity.STATUS_OK);
            server.playSound(null, machine.getBlockPos(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 0.8F);
        }
    }
}
