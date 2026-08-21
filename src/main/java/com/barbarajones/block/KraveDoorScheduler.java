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
        var it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            if (--task.ticksRemaining <= 0) {
                it.remove();
                task.action.run();
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
