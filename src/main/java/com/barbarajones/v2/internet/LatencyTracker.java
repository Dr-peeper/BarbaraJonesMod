package com.barbarajones.v2.internet;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * LATENCY: "your inputs arrive late."
 *
 * <p>A true packet-delay would mean holding every movement/attack/interact
 * packet in a queue and replaying it N ticks after it arrived - invasive,
 * and exactly the kind of thing that fights the anti-cheat assumptions the
 * rest of the server is built on. This gets the same read a much safer way:
 * while a player is tracked here, {@link com.barbarajones.v2.internet.
 * InternetOutageEvents} repeatedly pins them back to a single remembered
 * anchor point for a few ticks out of every cycle, server side, every tick -
 * a stutter/rubber-band that reads unmistakably as "the connection just
 * hitched" without ever touching the network layer. It is enforced entirely
 * by re-snapping position and zeroing velocity, so it cannot be fought by a
 * client that ignores it; it can only desync a legitimate client's own
 * prediction, which is exactly what real lag looks like.
 *
 * <p>Static and process-lifetime only, the same way {@code KraveApocalypse}
 * keeps its active list in memory rather than in a save - a stutter that is
 * still running the instant the server saved is not a state worth resurrecting
 * across a restart, and forcing it to survive one would only risk permanently
 * freezing someone who logged out mid-effect.
 */
public final class LatencyTracker {

    private static final Map<UUID, Entry> ACTIVE = new HashMap<>();

    private LatencyTracker() { }

    private static final class Entry {
        long untilTick;
        int phase;
        int freezeLen;
        Vec3 anchor;
        boolean frozen;

        Entry(long untilTick, int freezeLen) {
            this.untilTick = untilTick;
            this.freezeLen = freezeLen;
        }
    }

    /** Ticks per stutter cycle - a freeze, then this many ticks of normal movement. */
    private static final int CYCLE = 10;

    /**
     * Starts (or refreshes/worsens) the effect on a player.
     *
     * @param durationTicks how long the whole effect lasts
     * @param freezeLen     ticks frozen out of every {@link #CYCLE} - worse in
     *                      later phases, never more than {@code CYCLE - 1} or
     *                      it would never let go at all
     */
    public static void apply(ServerPlayer player, int durationTicks, int freezeLen) {
        long until = player.level().getGameTime() + durationTicks;
        Entry e = ACTIVE.get(player.getUUID());
        int clampedFreeze = Math.max(1, Math.min(CYCLE - 1, freezeLen));
        if (e == null) {
            e = new Entry(until, clampedFreeze);
            ACTIVE.put(player.getUUID(), e);
        } else {
            e.untilTick = Math.max(e.untilTick, until);
            e.freezeLen = Math.max(e.freezeLen, clampedFreeze);
        }
        player.sendSystemMessage(Component.literal(ChatFormatting.RED + "" + ChatFormatting.BOLD
                + "⚠ HIGH PING - your moves are arriving late."));
    }

    public static boolean isActive(UUID id) {
        return ACTIVE.containsKey(id);
    }

    /** Called once per player per server tick. Does nothing for an untracked player. */
    public static void tick(ServerPlayer player) {
        Entry e = ACTIVE.get(player.getUUID());
        if (e == null) {
            return;
        }
        if (player.level().getGameTime() > e.untilTick || player.isSpectator() || !player.isAlive()) {
            ACTIVE.remove(player.getUUID());
            return;
        }
        e.phase = (e.phase + 1) % CYCLE;
        if (e.phase == 0) {
            e.anchor = player.position();
            e.frozen = true;
        }
        if (e.frozen) {
            if (e.phase < e.freezeLen) {
                player.setDeltaMovement(Vec3.ZERO);
                player.teleportTo(e.anchor.x, e.anchor.y, e.anchor.z);
                player.fallDistance = 0.0F;
                player.hurtMarked = true;
            } else {
                e.frozen = false;
            }
        }
    }

    /** Clears everyone - called when an outage ends, so a fight that is cut short does not leave a lingering stutter. */
    public static void clearAll() {
        ACTIVE.clear();
    }

    public static void clear(UUID id) {
        ACTIVE.remove(id);
    }
}
