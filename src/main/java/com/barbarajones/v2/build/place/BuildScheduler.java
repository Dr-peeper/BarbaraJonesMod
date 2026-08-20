package com.barbarajones.v2.build.place;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Drives every in-progress {@link BuildJob}, one server tick at a time.
 *
 * <p>Jobs are held per level in a {@link WeakHashMap} keyed by the level object,
 * so a level that goes away cannot leak its jobs. The important guarantee lives
 * in {@link #onLevelUnload} and {@link #onServerStopping}: both force every
 * outstanding job to {@link BuildJob#finishNow()} before the world is written
 * out. A server that is killed mid-build still saves a finished building, never
 * three walls and a doorframe.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BuildScheduler {

    private static final Map<ServerLevel, List<BuildJob>> JOBS = new WeakHashMap<>();

    private BuildScheduler() { }

    static void submit(ServerLevel level, BuildJob job) {
        synchronized (JOBS) {
            JOBS.computeIfAbsent(level, key -> new ArrayList<>()).add(job);
        }
    }

    /**
     * Queues the reverse of a placement: puts the recorded snapshot back, top
     * down, over about a second. Used by the schematic refund.
     */
    public static void submitUndo(ServerLevel level, BoundingBox box,
                                  java.util.Map<net.minecraft.core.BlockPos,
                                          net.minecraft.world.level.block.state.BlockState> restore) {
        if (restore.isEmpty()) {
            return;
        }
        submit(level, BuildJob.reverse(level, box, restore));
    }

    /**
     * True if a running job already claims any of this space. Two buildings
     * growing through each other would produce a mess no undo could untangle,
     * so the second one is refused instead.
     */
    static boolean overlapsRunningJob(ServerLevel level, BoundingBox bounds) {
        synchronized (JOBS) {
            List<BuildJob> jobs = JOBS.get(level);
            if (jobs == null) {
                return false;
            }
            for (BuildJob job : jobs) {
                if (!job.isComplete() && job.bounds().intersects(bounds)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** How many buildings are going up right now in this level. Handy in debug commands. */
    public static int activeJobs(ServerLevel level) {
        synchronized (JOBS) {
            List<BuildJob> jobs = JOBS.get(level);
            if (jobs == null) {
                return 0;
            }
            int active = 0;
            for (BuildJob job : jobs) {
                if (!job.isComplete()) {
                    active++;
                }
            }
            return active;
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        List<BuildJob> snapshot;
        synchronized (JOBS) {
            List<BuildJob> jobs = JOBS.get(level);
            if (jobs == null || jobs.isEmpty()) {
                return;
            }
            snapshot = new ArrayList<>(jobs);
        }
        for (BuildJob job : snapshot) {
            job.tick();
        }
        synchronized (JOBS) {
            List<BuildJob> jobs = JOBS.get(level);
            if (jobs != null) {
                Iterator<BuildJob> it = jobs.iterator();
                while (it.hasNext()) {
                    if (it.next().isComplete()) {
                        it.remove();
                    }
                }
                if (jobs.isEmpty()) {
                    JOBS.remove(level);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            flush(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            flush(level);
        }
    }

    /** Completes every outstanding job in this level immediately. */
    public static void flush(Level level) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        List<BuildJob> jobs;
        synchronized (JOBS) {
            jobs = JOBS.remove(server);
        }
        if (jobs == null) {
            return;
        }
        for (BuildJob job : jobs) {
            job.finishNow();
        }
    }
}
