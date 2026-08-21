package com.barbarajones.behavior;

import com.barbarajones.BarbaraJonesMod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Runs something on a player after a delay.
 *
 * <p>Exists for the grass brownie, where the delay IS the joke - the effects are
 * supposed to arrive long after you have decided they were never coming and
 * eaten three more. An instant effect would be a completely different item.
 *
 * <p>Deliberately tiny: a list walked once per server tick, nothing persisted.
 * If the server stops with something pending, it is simply dropped, which is the
 * right call for a joke effect - restoring a queued brownie across a restart is
 * more machinery than the gag is worth.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID)
public final class DelayedEffects {

    private record Pending(ServerPlayer player, Consumer<ServerPlayer> action, long dueAt) { }

    /** A delayed action with no player attached - boss attacks, mostly. */
    private record PendingWorld(Runnable action, long dueAt) { }

    private static final List<PendingWorld> WORLD_QUEUE = new ArrayList<>();

    private static final List<Pending> QUEUE = new ArrayList<>();
    private static long tick;

    private DelayedEffects() { }

    /** Run {@code action} on this player after {@code delayTicks}. */
    public static void schedule(ServerPlayer player, int delayTicks, Consumer<ServerPlayer> action) {
        QUEUE.add(new Pending(player, action, tick + delayTicks));
    }

    /**
     * Run something after a delay with no player involved.
     *
     * <p>Telegraphed boss attacks need this: the warning ring is drawn now and
     * the hit lands later, and the gap between them is the entire fairness of
     * the attack. The level is taken to prove a caller is on the server, not
     * because the queue needs it.
     */
    public static void scheduleWorld(net.minecraft.server.level.ServerLevel level,
                                     int delayTicks, Runnable action) {
        WORLD_QUEUE.add(new PendingWorld(action, tick + delayTicks));
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        tick++;

        if (!WORLD_QUEUE.isEmpty()) {
            for (PendingWorld p : new ArrayList<>(WORLD_QUEUE)) {
                if (tick < p.dueAt()) {
                    continue;
                }
                WORLD_QUEUE.remove(p);
                try {
                    p.action().run();
                } catch (Exception ignored) {
                    // A boss attack must never be the reason a tick dies.
                }
            }
        }

        if (QUEUE.isEmpty()) {
            return;
        }
        // Iterate a copy: an action is free to schedule another one.
        for (Pending p : new ArrayList<>(QUEUE)) {
            if (tick < p.dueAt()) {
                continue;
            }
            QUEUE.remove(p);
            // A player who logged out or died in the meantime is skipped rather
            // than crashing the tick loop for everyone else.
            if (p.player().isRemoved() || !p.player().isAlive()) {
                continue;
            }
            try {
                p.action().accept(p.player());
            } catch (Exception ignored) {
                // A joke effect must never be the reason a server tick dies.
            }
        }
    }
}
