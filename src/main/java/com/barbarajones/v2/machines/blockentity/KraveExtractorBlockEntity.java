package com.barbarajones.v2.machines.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import com.barbarajones.v2.machines.KraveMachines;
import com.barbarajones.v2.machines.block.KraveExtractorBlock;

/**
 * Krave Extractor: pulls from the inventory behind it and pushes into whatever is
 * in front.
 *
 * <p>This is the module's insert/extract primitive. Conveyors carry, machines
 * process, and the extractor is the thing that reaches into a chest, a barrel, a
 * machine's output slot or another mod's inventory and gets items moving. It
 * holds nothing itself - no buffer, no GUI, no NBT beyond a cooldown - which
 * means it can never be the place a player's items go missing.
 *
 * <p>It simulates the insertion before committing the extraction. Extract-then-
 * fail-to-insert is the classic item-voiding bug in transfer blocks, and the only
 * reliable defence is to never take something out until you know where it is
 * going.
 */
public class KraveExtractorBlockEntity extends BlockEntity {

    /** Ticks between transfer attempts. Two moves a second - slower than a hopper on purpose. */
    public static final int INTERVAL = 10;
    /** Items moved per attempt. */
    public static final int BATCH = 8;

    private int cooldown;

    public KraveExtractorBlockEntity(BlockPos pos, BlockState state) {
        super(KraveMachines.EXTRACTOR_BLOCK_ENTITY.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, KraveExtractorBlockEntity extractor) {
        if (--extractor.cooldown > 0) {
            return;
        }
        extractor.cooldown = INTERVAL;
        extractor.transfer(level, pos, state.getValue(KraveExtractorBlock.FACING));
    }

    private void transfer(Level level, BlockPos pos, Direction facing) {
        IItemHandler source = handlerAt(level, pos.relative(facing.getOpposite()), facing);
        if (source == null) {
            return;
        }
        IItemHandler sink = handlerAt(level, pos.relative(facing), facing.getOpposite());
        if (sink == null) {
            return;
        }
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack candidate = source.extractItem(slot, BATCH, true);
            if (candidate.isEmpty()) {
                continue;
            }
            int accepted = simulateInsert(sink, candidate);
            if (accepted <= 0) {
                continue;
            }
            ItemStack taken = source.extractItem(slot, accepted, false);
            if (taken.isEmpty()) {
                continue;
            }
            ItemStack leftover = taken;
            for (int target = 0; target < sink.getSlots() && !leftover.isEmpty(); target++) {
                leftover = sink.insertItem(target, leftover, false);
            }
            if (!leftover.isEmpty()) {
                // The sink lied about its capacity between simulate and commit -
                // rare, but another mod's dynamic handler can do it. Put it back
                // rather than dropping it.
                ItemStack returned = leftover;
                for (int back = 0; back < source.getSlots() && !returned.isEmpty(); back++) {
                    returned = source.insertItem(back, returned, false);
                }
                if (!returned.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level,
                            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, returned);
                }
            }
            return;
        }
    }

    /** How many of this stack the sink would take, without taking any. */
    private static int simulateInsert(IItemHandler sink, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < sink.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = sink.insertItem(slot, remaining, true);
        }
        return stack.getCount() - remaining.getCount();
    }

    private static IItemHandler handlerAt(Level level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        return be == null ? null : be.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cooldown = tag.getInt("Cooldown");
    }
}
