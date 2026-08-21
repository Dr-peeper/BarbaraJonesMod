package com.barbarajones.block;

import com.barbarajones.BarbaraJonesMod;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * A tiny "run this in N ticks" queue for {@link KraveDoorBlock} - the short
 * pause before a portal trip actually fires (so the door's own close
 * animation and sound have a moment to play before the world swaps out from
 * under the player) and the longer grace period a straggling companion gets
 * to catch up before being forced through. Self-registers via the class
 * annotation, no wiring needed anywhere else.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID)
final class KraveDoorScheduler {

    private static final List<Task> TASKS = new ArrayList<>();

    private KraveDoorScheduler() { }

    static void schedule(int delayTicks, Runnable action) {
        TASKS.add(new Task(Math.max(1, delayTicks), action));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TASKS.isEmpty()) {
            return;
        }
        // Two passes on purpose: a task's own action can itself call
        // schedule() (a travel task's teleportInto() does exactly this, to
        // queue a straggler's catch-up) - running actions while still
        // iterating TASKS meant that re-entrant add() threw
        // ConcurrentModificationException. Collecting the ready ones first
        // and only running them once the TASKS iterator is done avoids that:
        // whatever a task schedules lands safely after this tick's iteration
        // has already finished.
        List<Runnable> ready = null;
        var it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (--task.ticksRemaining <= 0) {
                it.remove();
                if (ready == null) {
                    ready = new ArrayList<>();
                }
                ready.add(task.action);
            }
        }
        if (ready != null) {
            for (Runnable action : ready) {
                action.run();
            }
        }
    }

    private static final class Task {
        int ticksRemaining;
        final Runnable action;

        Task(int ticksRemaining, Runnable action) {
            this.ticksRemaining = ticksRemaining;
            this.action = action;
        }
    }
}
