package com.barbarajones.dimension;

import com.barbarajones.content.ModBlocks;

import net.minecraft.core.particles.BlockParticleOption;
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
 * tick it's active - a dense grid of falling particles colored straight off
 * an actual chocolate block's own texture (not a fixed/guessed color), so it
 * reads as brown rain rather than the amber "honey" tint an earlier version
 * used. The biome's own has_precipitation stays false specifically so
 * vanilla's blue/white rain streaks never also try to render on top of this
 * and look wrong.
 */
public final class KraveChocolateWeather {

    private static final int MIN_RAIN_TICKS = 6000;     // 5 min
    private static final int MAX_RAIN_TICKS = 18000;    // 15 min
    private static final int MIN_CLEAR_TICKS = 12000;   // 10 min
    private static final int MAX_CLEAR_TICKS = 30000;   // 25 min
    private static final int PARTICLE_INTERVAL = 2;
    private static final int DROPS_PER_PASS = 30;
    private static final double SPREAD_RADIUS = 16.0D;

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
        var random = ThreadLocalRandom.current();

        // The very first time this ever runs on a world, start with a real
        // clear stretch rather than reading the fresh/default timer of 0 as
        // "expired, flip to raining right now" - that bug is exactly why
        // chocolate rain looked like it never turned off.
        if (!data.isChocolateWeatherInitialized()) {
            data.setChocolateWeatherInitialized(true);
            data.setChocolateRaining(false);
            data.setChocolateWeatherTimer(MIN_CLEAR_TICKS + random.nextInt(MAX_CLEAR_TICKS - MIN_CLEAR_TICKS));
            kosmos.setWeatherParameters(data.getChocolateWeatherTimer(), 0, false, false);
            return;
        }

        int timer = data.getChocolateWeatherTimer() - 1;
        if (timer <= 0) {
            boolean nowRaining = !data.isChocolateRaining();
            data.setChocolateRaining(nowRaining);
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
        var drop = new BlockParticleOption(ParticleTypes.FALLING_DUST,
                ModBlocks.CHOCOLATE_PLANKS.get().defaultBlockState());
        for (var player : kosmos.players()) {
            for (int i = 0; i < DROPS_PER_PASS; i++) {
                double x = player.getX() + (kosmos.random.nextDouble() - 0.5D) * SPREAD_RADIUS * 2.0D;
                double z = player.getZ() + (kosmos.random.nextDouble() - 0.5D) * SPREAD_RADIUS * 2.0D;
                double y = player.getY() + 14.0D + kosmos.random.nextDouble() * 6.0D;
                kosmos.sendParticles(drop, x, y, z, 1, 0.0D, -0.9D, 0.0D, 0.0D);
            }
        }
    }
}
