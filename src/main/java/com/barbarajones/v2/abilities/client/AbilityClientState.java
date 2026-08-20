package com.barbarajones.v2.abilities.client;

import com.barbarajones.v2.abilities.AbilityId;

/**
 * The client's mirror of {@code AbilityData}, fed entirely by
 * {@code PacketAbilitySync}.
 *
 * <p>Deliberately free of every {@code net.minecraft.client} import: nothing
 * here is Minecraft-client-only, it is just the last snapshot of a handful of
 * longs. That is what lets {@code AbilityItem} (a common-side class, loaded
 * on the dedicated server too) read {@link #isUnlocked} straight off it for
 * the tooltip without routing through {@code DistExecutor} - there is nothing
 * client-only in this file for the server's classloader to trip on, and the
 * server simply never calls it (tooltips only ever render client-side).
 *
 * <p>{@code clockTick} is stamped from the packet's own "now" plus however
 * many client ticks have passed since, so the HUD bar keeps animating between
 * syncs instead of stair-stepping once every server round trip.
 */
public final class AbilityClientState {

    private static int mask;
    private static final long[] cooldownEnd = new long[AbilityId.COUNT];
    private static final long[] activeEnd = new long[AbilityId.COUNT];
    /** Server game time as of the last sync, advanced locally every client tick. */
    private static long clockTick;
    private static boolean everSynced;

    private AbilityClientState() { }

    public static void accept(int newMask, long serverNow, long[] cd, long[] act) {
        mask = newMask;
        clockTick = serverNow;
        int n = Math.min(cd.length, AbilityId.COUNT);
        for (int i = 0; i < n; i++) {
            cooldownEnd[i] = cd[i];
            activeEnd[i] = act[i];
        }
        everSynced = true;
    }

    /** Called once per client tick to keep the HUD's clock moving between syncs. */
    public static void advanceClock() {
        if (everSynced) {
            clockTick++;
        }
    }

    public static boolean isUnlocked(AbilityId id) {
        return (mask & (1 << id.index)) != 0;
    }

    public static long cooldownRemaining(AbilityId id) {
        return Math.max(0L, cooldownEnd[id.index] - clockTick);
    }

    public static long activeRemaining(AbilityId id) {
        return Math.max(0L, activeEnd[id.index] - clockTick);
    }

    public static boolean isActive(AbilityId id) {
        return activeRemaining(id) > 0L;
    }

    public static boolean isReady(AbilityId id) {
        return cooldownRemaining(id) == 0L;
    }

    /** True once at least one sync has arrived - the HUD stays hidden until then. */
    public static boolean ready() {
        return everSynced;
    }

    /** Cleared on logout/disconnect so a new world never inherits stale state. */
    public static void reset() {
        mask = 0;
        clockTick = 0L;
        everSynced = false;
        for (int i = 0; i < AbilityId.COUNT; i++) {
            cooldownEnd[i] = 0L;
            activeEnd[i] = 0L;
        }
    }
}
