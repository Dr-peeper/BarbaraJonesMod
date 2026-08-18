package com.barbarajones.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Shared "blink" teleport - originally Krave Monster-only, extracted so
 * Kosmonauts (KraveMinion) can use the exact same escape mechanism and not
 * get stranded in the new mountain/valley terrain either. Column-scans
 * downward from a candidate point for solid ground, same technique
 * KraveLanding uses for the portal/ambient-spawn landing search.
 */
public final class KraveBlink {

    private KraveBlink() { }

    public static boolean blinkTo(Mob mob, double x, double y, double z, SoundEvent sound) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        while (pos.getY() > mob.level().getMinBuildHeight()
                && !mob.level().getBlockState(pos).blocksMotion()) {
            pos.move(0, -1, 0);
        }
        if (!mob.level().getBlockState(pos).blocksMotion()) {
            return false;
        }
        Vec3 from = mob.position();
        boolean ok = mob.randomTeleport(x, pos.getY() + 1.0D, z, false);
        if (ok) {
            mob.level().playSound(null, from.x, from.y, from.z, sound, mob.getSoundSource(), 0.8F, 1.4F);
            mob.playSound(sound, 0.8F, 1.4F);
            // Level.addParticle() is a no-op on the server (the base class
            // body is empty - only ClientLevel renders it) - sendParticles()
            // is the actual server->client broadcast API, which is what this
            // trail needs since blinkTo only ever runs server-side.
            if (mob.level() instanceof ServerLevel sl) {
                RandomSource random = mob.getRandom();
                for (int i = 0; i < 48; i++) {
                    double t = i / 47.0D;
                    sl.sendParticles(ParticleTypes.PORTAL,
                            from.x + (mob.getX() - from.x) * t + (random.nextDouble() - 0.5D) * 2.0D,
                            from.y + (mob.getY() - from.y) * t + random.nextDouble() * mob.getBbHeight(),
                            from.z + (mob.getZ() - from.z) * t + (random.nextDouble() - 0.5D) * 2.0D,
                            1,
                            (random.nextFloat() - 0.5F) * 0.2F,
                            (random.nextFloat() - 0.5F) * 0.2F,
                            (random.nextFloat() - 0.5F) * 0.2F,
                            0.0D);
                }
            }
        }
        return ok;
    }

    /**
     * Tries a bounded number of random nearby points and blinks to the first
     * one that lands on solid ground. Vertical range is deliberately biased
     * upward and wider than the original monster-only version (-8/+20 vs the
     * old flat +-6) so a blink can actually clear a deep valley/crevasse
     * instead of just relocating within it.
     */
    public static boolean tryRandomBlink(Mob mob, RandomSource random, double horizontalRange,
                                         int belowRange, int aboveRange, int attempts, SoundEvent sound) {
        for (int i = 0; i < attempts; i++) {
            double x = mob.getX() + (random.nextDouble() - 0.5D) * horizontalRange;
            double y = mob.getY() + (random.nextInt(belowRange + aboveRange + 1) - belowRange);
            double z = mob.getZ() + (random.nextDouble() - 0.5D) * horizontalRange;
            if (blinkTo(mob, x, y, z, sound)) {
                return true;
            }
        }
        return false;
    }
}
