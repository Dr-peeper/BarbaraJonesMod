package com.barbarajones.diag;

import com.mojang.logging.LogUtils;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

import org.slf4j.Logger;

/**
 * Catches a hung server tick and writes down where it hung.
 *
 * <p>A freeze is the one failure mode that leaves no evidence. A crash gets a
 * report with a stack trace; a hang just stops, the log ends mid-sentence, and
 * the only thing left to do is guess at which loop never terminated. Singleplayer
 * has no watchdog of its own - vanilla only runs one on dedicated servers - so
 * nothing was ever going to tell us.
 *
 * <p>This is that watchdog. A daemon thread checks whether the server tick has
 * advanced; if it has not moved for {@link #STALL_MS} it grabs the server
 * thread's stack and logs it. The top frame of that dump is the loop that is not
 * finishing, which turns "it froze when Duhl Wol showed up" into a line number.
 *
 * <p>Two things keep it from crying wolf:
 *
 * <ul>
 *   <li><b>A parked thread is not a hung one.</b> Singleplayer pauses the
 *       integrated server whenever the menu is open, and alt-tabbing away for a
 *       minute is normal. A paused server thread is sitting in a park, sleep or
 *       wait, so those stacks are recognised and ignored.</li>
 *   <li><b>One dump per stall.</b> It reports the stall once and then stays quiet
 *       until ticks resume, so a genuine permanent hang leaves exactly one stack
 *       trace rather than filling the log with the same one every two seconds.</li>
 * </ul>
 *
 * <p>It never intervenes. It cannot fix a hang and does not try to - it only
 * makes sure the next one is diagnosable instead of invisible.
 */
public final class ServerStallWatchdog {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** How long the tick counter may sit still before we treat it as a stall. */
    private static final long STALL_MS = 10_000L;

    /** How often to look. Cheap enough that this can be tight. */
    private static final long POLL_MS = 2_000L;

    /** Bumped by the server tick handler; the only signal that the tick is alive. */
    private static volatile long lastTick = System.currentTimeMillis();

    private static volatile Thread serverThread;
    private static volatile Thread watcher;
    private static volatile boolean running;

    /** True once a stall has been reported, so it is reported only once. */
    private static volatile boolean reported;

    private ServerStallWatchdog() { }

    /** Called every server tick. Must stay trivial - it runs 20x a second. */
    public static void heartbeat() {
        lastTick = System.currentTimeMillis();
        reported = false;
    }

    public static void start(ServerStartedEvent event) {
        start(event.getServer());
    }

    public static void start(MinecraftServer server) {
        stop();
        serverThread = server.getRunningThread();
        lastTick = System.currentTimeMillis();
        reported = false;
        running = true;

        Thread t = new Thread(ServerStallWatchdog::loop, "BarbaraJones stall watchdog");
        // Daemon: this must never be the reason the JVM stays alive on shutdown.
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        watcher = t;
        t.start();
    }

    public static void stop(ServerStoppingEvent event) {
        stop();
    }

    public static void stop() {
        running = false;
        Thread t = watcher;
        if (t != null) {
            t.interrupt();
            watcher = null;
        }
    }

    private static void loop() {
        while (running) {
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }

            Thread target = serverThread;
            if (target == null || !running) {
                continue;
            }

            long stalled = System.currentTimeMillis() - lastTick;
            if (stalled < STALL_MS || reported) {
                continue;
            }

            StackTraceElement[] stack = target.getStackTrace();
            if (stack.length == 0 || isIdle(stack)) {
                // Paused or between ticks, not stuck. Say nothing.
                continue;
            }

            reported = true;
            StringBuilder out = new StringBuilder();
            out.append("Server tick has not advanced for ").append(stalled / 1000L)
               .append("s - the server thread appears to be stuck. Stack follows;")
               .append(" the top frame is the code that is not returning.\n");
            for (StackTraceElement frame : stack) {
                out.append("\tat ").append(frame).append('\n');
            }
            LOGGER.error("{}", out);
        }
    }

    /**
     * A server thread that is merely paused or waiting for its next tick sits in
     * a park, sleep or wait, and that is not what we are hunting. Checking the
     * top few frames rather than only the first keeps it robust against the
     * couple of wrapper frames the JDK puts above the actual park.
     */
    private static boolean isIdle(StackTraceElement[] stack) {
        int depth = Math.min(4, stack.length);
        for (int i = 0; i < depth; i++) {
            String method = stack[i].getMethodName();
            if (method.equals("park") || method.equals("sleep") || method.equals("wait")
                    || method.equals("waitUntilNextTick") || method.equals("park0")
                    || method.equals("poll") || method.equals("take")) {
                return true;
            }
        }
        return false;
    }
}
