package com.barbarajones.v2.build;

import com.barbarajones.v2.build.def.StructureDef;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The build module's core: the ticking machinery that turns an enqueued
 * {@link StructureDef} into blocks appearing in the world over time, rather
 * than all at once. {@code KraveSchematicItem} (and anything else that wants
 * to stamp a structure down) calls {@link #enqueue}; this class drains a
 * few cells per server tick from whatever jobs are in flight until each one
 * finishes, spread across that structure's own {@link StructureDef#buildTicks()}.
 *
 * <p>Nothing else in the build/houses modules needs to be wired centrally -
 * this is the one call the mod constructor makes.
 */
public final class KraveBuild {

    private static final Deque<Job> QUEUE = new ArrayDeque<>();

    private KraveBuild() { }

    public static void init(IEventBus bus) {
        MinecraftForge.EVENT_BUS.register(KraveBuild.class);
    }

    /** Schedules {@code def} for gradual placement, offset so its local (0,0,0) lands at {@code worldOrigin}. */
    public static void enqueue(ServerLevel level, StructureDef def, BlockPos worldOrigin) {
        List<PendingCell> cells = new ArrayList<>(def.cells().size());
        for (StructureDef.Cell cell : def.cells()) {
            cells.add(new PendingCell(worldOrigin.offset(cell.pos()), cell.state()));
        }
        int perTick = Math.max(1, cells.size() / def.buildTicks());
        QUEUE.add(new Job(level, cells, perTick));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || QUEUE.isEmpty()) {
            return;
        }
        var it = QUEUE.iterator();
        while (it.hasNext()) {
            Job job = it.next();
            int placed = 0;
            while (placed < job.perTick && job.index < job.cells.size()) {
                PendingCell cell = job.cells.get(job.index);
                job.level.setBlock(cell.pos, cell.state, 3);
                job.index++;
                placed++;
            }
            if (job.index >= job.cells.size()) {
                it.remove();
            }
        }
    }

    private record PendingCell(BlockPos pos, BlockState state) { }

    private static final class Job {
        final ServerLevel level;
        final List<PendingCell> cells;
        final int perTick;
        int index;

        Job(ServerLevel level, List<PendingCell> cells, int perTick) {
            this.level = level;
            this.cells = cells;
            this.perTick = perTick;
        }
    }
}
