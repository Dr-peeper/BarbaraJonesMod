package com.barbarajones.dimension;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Chocolate rain for the Krave Kosmos - the same state machine shape
 * vanilla's own weather cycle uses (a single countdown timer that, on
 * reaching zero, flips the state and rolls a fresh random duration for
 * whichever state comes next - see {@code ServerLevel#advanceWeatherCycle}),
 * reimplemented here rather than hooked into it directly since the actual
 * rain-droplet rendering vanilla uses is hardcoded client-side with no data-
 * driven recolor hook - there is no way to make vanilla's own rain particles
 * render brown short of a client rendering patch. What IS real here: the
 * dimension's actual weather state ({@link ServerLevel#setWeatherParameters})
 * changes for real, and the visible "rain" itself is spawned by hand each
 * tick it's active, as falling honey-colored particles standing in for
 * chocolate droplets (the biome's own has_precipitation stays false
 * specifically so vanilla's blue/white rain streaks never also try to
 * render on top of this and look wrong).
 */
public final class KraveChocolateWeather {

    private static final int MIN_RAIN_TICKS = 6000;     // 5 min
    private static final int MAX_RAIN_TICKS = 18000;    // 15 min
    private static final int MIN_CLEAR_TICKS = 12000;   // 10 min
    private static final int MAX_CLEAR_TICKS = 30000;   // 25 min
    private static final int PARTICLE_INTERVAL = 4;
    private static final double SCATTER_RADIUS = 20.0D;

    private KraveChocolateWeather() { }

    public static void tick() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        ServerLevel kosmos = server.getLevel(KraveDimensions.KRAVE_KOSMOS);
        if (kosmos == null) {
            return;
        }

        KraveKosmosData data = KraveKosmosData.get(kosmos);
        int timer = data.getChocolateWeatherTimer() - 1;
        if (timer <= 0) {
            boolean nowRaining = !data.isChocolateRaining();
            data.setChocolateRaining(nowRaining);
            var random = ThreadLocalRandom.current();
            timer = nowRaining
                    ? MIN_RAIN_TICKS + random.nextInt(MAX_RAIN_TICKS - MIN_RAIN_TICKS)
                    : MIN_CLEAR_TICKS + random.nextInt(MAX_CLEAR_TICKS - MIN_CLEAR_TICKS);
            kosmos.setWeatherParameters(nowRaining ? 0 : timer, nowRaining ? timer : 0, nowRaining, false);
        }
        data.setChocolateWeatherTimer(timer);

        if (data.isChocolateRaining() && kosmos.getGameTime() % PARTICLE_INTERVAL == 0) {
            spawnRainParticles(kosmos);
        }
    }

    private static void spawnRainParticles(ServerLevel kosmos) {
        for (var player : kosmos.players()) {
            for (int i = 0; i < 5; i++) {
                double x = player.getX() + (kosmos.random.nextDouble() - 0.5D) * SCATTER_RADIUS * 2.0D;
                double z = player.getZ() + (kosmos.random.nextDouble() - 0.5D) * SCATTER_RADIUS * 2.0D;
                double y = player.getY() + 10.0D + kosmos.random.nextDouble() * 8.0D;
                kosmos.sendParticles(ParticleTypes.FALLING_HONEY, x, y, z, 1, 0.0D, -0.4D, 0.0D, 0.0D);
            }
        }
    }
}
