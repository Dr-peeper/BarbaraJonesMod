package com.barbarajones.worldgen.feature;

import com.barbarajones.BarbaraJonesMod;
import com.barbarajones.content.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Particles and sound for chocolate that is actually falling, anywhere it is
 * found - the real waterfalls this package builds, but also every older
 * single-block "spring" KraveMountainFeature has been placing on mountain
 * ledges since v8.1.0. No marker block, no persisted list of waterfall
 * sites: it scans loaded chunks near each player for chocolate fluid with
 * open (non-solid, or also-chocolate) space below it, which is exactly what
 * "currently cascading" means, and dresses whatever it finds. Same
 * always-on tick-scanner shape as {@code apocalypse.KraveKosmosAmbience},
 * just self-subscribed instead of centrally ticked, since this package
 * cannot touch EventHandler.
 */
@Mod.EventBusSubscriber(modid = BarbaraJonesMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KraveWaterfallAmbience {

    private static final int TICK_INTERVAL = 12;
    private static final int SCAN_RADIUS_H = 16;
    private static final int SCAN_RADIUS_V = 10;
    private static final int SAMPLES_PER_PLAYER = 8;
    private static final double SOUND_CHANCE = 1.0D / 3.0D;

    private static int timer;

    private KraveWaterfallAmbience() { }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (--timer > 0) {
            return;
        }
        timer = TICK_INTERVAL;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                scanNear(level, player);
            }
        }
    }

    private static void scanNear(ServerLevel level, ServerPlayer player) {
        RandomSource random = level.random;
        BlockPos center = player.blockPosition();
        boolean soundPlayed = false;

        for (int i = 0; i < SAMPLES_PER_PLAYER; i++) {
            int bx = center.getX() + random.nextInt(SCAN_RADIUS_H * 2 + 1) - SCAN_RADIUS_H;
            int by = center.getY() + random.nextInt(SCAN_RADIUS_V * 2 + 1) - SCAN_RADIUS_V;
            int bz = center.getZ() + random.nextInt(SCAN_RADIUS_H * 2 + 1) - SCAN_RADIUS_H;
            BlockPos pos = new BlockPos(bx, by, bz);

            if (!isChocolate(level.getFluidState(pos))) {
                continue;
            }
            BlockPos below = pos.below();
            boolean cascading = !level.getBlockState(below).isSolid() || isChocolate(level.getFluidState(below));
            if (!cascading) {
                continue;   // a still pool, not a falling face - the pool doesn't need drip particles
            }

            level.sendParticles(ParticleTypes.FALLING_HONEY, bx + 0.5D, by + 0.15D, bz + 0.5D,
                    2, 0.25D, 0.1D, 0.25D, 0.0D);
            if (!level.getBlockState(below).isSolid() && level.getFluidState(below).isEmpty()) {
                // genuinely mid-air over open space one block down - a landing splash reads here
                level.sendParticles(ParticleTypes.LANDING_HONEY, bx + 0.5D, by - 0.4D, bz + 0.5D,
                        1, 0.15D, 0.02D, 0.15D, 0.0D);
            }

            if (!soundPlayed && random.nextDouble() < SOUND_CHANCE) {
                soundPlayed = true;
                level.playSound(null, pos, KraveWaterfallContent.CHOCOLATE_FLOW, SoundSource.AMBIENT,
                        0.55F, 0.85F + random.nextFloat() * 0.2F);
            }
        }
    }

    private static boolean isChocolate(FluidState fs) {
        return fs.is(ModFluids.CHOCOLATE.get()) || fs.is(ModFluids.CHOCOLATE_FLOWING.get());
    }
}
