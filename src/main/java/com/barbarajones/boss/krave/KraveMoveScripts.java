package com.barbarajones.boss.krave;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/**
 * The six finishers, written as timelines.
 *
 * <p>Each is a function of ticks-since-start that returns true when it is done.
 * Branching on {@code c.t} makes them read as storyboards - at tick 0 grab him,
 * at tick 20 launch, at tick 45 land - rather than as state machines with a
 * field per beat, and it means the shared verbs in {@link KraveCinematic} do
 * all the actual work. The choreography differs; the machinery does not.
 *
 * <p>Every script must be safe to abandon at any tick: the sequencer releases
 * the grab and restores gravity on timeout, so nothing here may leave state
 * that only its own final tick would clean up.
 */
public final class KraveMoveScripts {

    private KraveMoveScripts() { }

    /** @return true when the cinematic has finished. */
    public interface Script {
        boolean tick(KraveCinematic c);
    }

    public static Script forMove(KraveFinisherMove move) {
        return switch (move) {
            case AERIAL_THROW -> KraveMoveScripts::aerialThrow;
            case GROUND_SLAM -> KraveMoveScripts::groundSlam;
            case METEOR -> KraveMoveScripts::meteor;
            case GRINDER -> KraveMoveScripts::grinder;
            case COMBO -> KraveMoveScripts::combo;
            case FINALE -> KraveMoveScripts::finale;
        };
    }

    /** How long each has before the sequencer gives up and offers the prompt again. */
    public static int timeoutFor(KraveFinisherMove move) {
        return switch (move) {
            case AERIAL_THROW -> 140;
            case GROUND_SLAM -> 160;
            case METEOR -> 200;
            case GRINDER -> 200;
            case COMBO -> 260;
            case FINALE -> 420;
        };
    }

    // ---- 1. G: Kaiden throws the player down ---------------------------------

    /**
     * The original, kept as it was.
     *
     * <p>Kaiden is already at his launch point when this starts - the preparing
     * phase put him there - so this only has to hold the player, release, and
     * steer the dive until it connects.
     */
    private static boolean aerialThrow(KraveCinematic c) {
        c.protect(60);
        if (c.t == 0) {
            c.say("KAIDEN HAS YOU.", ChatFormatting.GOLD);
        }
        // Held under Kaiden long enough to see the boss below.
        if (c.t < 22) {
            Vec3 hold = c.cayden.position().add(0.0D, -1.6D, 0.0D);
            c.player.teleportTo(hold.x, hold.y, hold.z);
            c.player.setDeltaMovement(Vec3.ZERO);
            c.player.hurtMarked = true;
            c.faceBoss(c.player);
            c.level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    hold.x, hold.y, hold.z, 8, 0.5D, 0.5D, 0.5D, 0.15D);
            return false;
        }
        if (c.t == 22) {
            c.say("GO.", ChatFormatting.GOLD);
        }
        return diveIntoBoss(c, 2.8D, 1);
    }

    /**
     * Steers the player into the boss and lands the blow.
     *
     * <p>Re-aimed every tick rather than fired once: a dead-reckoned shot misses
     * the moment he shifts, which is the bug the first version of this had.
     */
    private static boolean diveIntoBoss(KraveCinematic c, double speed, int power) {
        Vec3 aim = c.boss.position().add(0.0D, c.boss.getBbHeight() * 0.5D, 0.0D)
                .subtract(c.player.position());
        double d = aim.length();
        if (d > 0.01D) {
            Vec3 steer = aim.scale(speed / d);
            c.player.setDeltaMovement(c.player.getDeltaMovement().scale(0.45D).add(steer.scale(0.55D)));
            c.player.hurtMarked = true;
        }
        c.trail(c.player, ParticleTypes.FLAME, 10);
        if (c.player.getBoundingBox().inflate(2.5D).intersects(c.boss.getBoundingBox())) {
            c.slam(c.boss.position(), power);
            c.player.setDeltaMovement(c.player.getDeltaMovement().scale(-0.25D).add(0.0D, 0.6D, 0.0D));
            c.player.hurtMarked = true;
            return true;
        }
        return false;
    }

    // ---- 2. K: the player picks him up and slams him -------------------------

    private static boolean groundSlam(KraveCinematic c) {
        c.protect(80);
        // Close on him.
        if (c.t < 30 && !c.player.getBoundingBox().inflate(3.0D).intersects(c.boss.getBoundingBox())) {
            c.fly(c.player, c.boss.position().add(0.0D, c.boss.getBbHeight() * 0.4D, 0.0D), 1.6D);
            c.faceBoss(c.player);
            c.trail(c.player, ParticleTypes.CRIT, 6);
            return false;
        }
        if (!KraveGrab.isHeld(c.boss)) {
            KraveGrab.grab(c.boss, c.player, new Vec3(0.0D, 2.0D, 0.0D));
            c.say("UP HE GOES.", ChatFormatting.GOLD);
            c.stagger(2);
        }
        KraveGrab.follow(c.boss);

        Vec3 up = c.origin.add(0.0D, 26.0D, 0.0D);
        if (c.t < 55) {
            c.fly(c.player, up, 1.5D);           // carry him skyward
            c.trail(c.boss, ParticleTypes.LARGE_SMOKE, 6);
            return false;
        }
        if (c.t < 68) {
            c.player.setDeltaMovement(c.player.getDeltaMovement().scale(0.5D));
            c.faceBoss(c.player);                // the pause at the top
            return false;
        }
        // And down.
        if (c.t == 68) {
            KraveGrab.release(c.boss, new Vec3(0.0D, -3.4D, 0.0D));
            c.say("DOWN.", ChatFormatting.RED);
        }
        c.boss.setDeltaMovement(c.boss.getDeltaMovement().add(0.0D, -0.45D, 0.0D));
        c.boss.hurtMarked = true;
        c.trail(c.boss, ParticleTypes.FLAME, 12);
        c.fly(c.player, c.boss.position().add(0.0D, 3.0D, 0.0D), 1.8D);

        if (c.boss.onGround() || c.boss.getY() <= c.origin.y + 0.5D) {
            c.slam(c.boss.position(), 2);
            return true;
        }
        return false;
    }

    // ---- 3. H: launched into the sky, struck back down -----------------------

    private static boolean meteor(KraveCinematic c) {
        c.protect(110);
        if (c.t < 24) {
            c.fly(c.player, c.boss.position().add(0.0D, 1.0D, 0.0D), 1.7D);
            c.faceBoss(c.player);
            return false;
        }
        if (c.t == 24) {
            // The uppercut.
            KraveGrab.release(c.boss, new Vec3(0.0D, 3.6D, 0.0D));
            c.boss.setNoGravity(true);
            c.say("INTO THE SKY.", ChatFormatting.GOLD);
            c.slam(c.boss.position(), 1);
        }
        if (c.t < 70) {
            // He rises; the player follows just behind, so the climb is visible.
            c.boss.setDeltaMovement(0.0D, 2.2D, 0.0D);
            c.boss.hurtMarked = true;
            c.trail(c.boss, ParticleTypes.LARGE_SMOKE, 8);
            c.fly(c.player, c.boss.position().add(0.0D, -3.0D, 0.0D), 2.4D);
            c.faceBoss(c.player);
            return false;
        }
        if (c.t < 88) {
            // Overtaken and turned around above him.
            c.boss.setDeltaMovement(0.0D, 0.1D, 0.0D);
            c.fly(c.player, c.boss.position().add(0.0D, 7.0D, 0.0D), 2.8D);
            c.faceBoss(c.player);
            return false;
        }
        if (c.t == 88) {
            c.boss.setNoGravity(false);
            c.say("METEOR.", ChatFormatting.RED);
        }
        // Driven down.
        c.boss.setDeltaMovement(c.boss.getDeltaMovement().scale(0.6D).add(0.0D, -3.2D, 0.0D));
        c.boss.hurtMarked = true;
        c.trail(c.boss, ParticleTypes.FLAME, 16);
        c.fly(c.player, c.boss.position().add(0.0D, 5.0D, 0.0D), 3.0D);

        if (c.boss.onGround() || c.boss.getY() <= c.origin.y + 0.5D) {
            c.slam(c.boss.position(), 3);
            return true;
        }
        return false;
    }

    // ---- 4. J: carried through the terrain at speed --------------------------

    private static boolean grinder(KraveCinematic c) {
        c.protect(110);
        if (c.t < 20) {
            c.fly(c.player, c.boss.position().add(0.0D, 1.0D, 0.0D), 1.8D);
            c.faceBoss(c.player);
            return false;
        }
        if (!KraveGrab.isHeld(c.boss)) {
            KraveGrab.grab(c.boss, c.player, new Vec3(0.0D, 0.0D, 0.0D));
            c.say("THROUGH IT.", ChatFormatting.GOLD);
        }
        KraveGrab.follow(c.boss);

        if (c.t < 78) {
            // Driven along a fixed heading just above the ground, ploughing a
            // furrow. The distance is bounded on purpose: this is a finisher,
            // not permission to level the dimension.
            double angle = Math.toRadians((c.t - 20) * 3.0D);
            Vec3 along = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle)).scale(1.9D);
            c.player.setDeltaMovement(along.x, 0.05D, along.z);
            c.player.setNoGravity(true);
            c.player.hurtMarked = true;
            c.trail(c.boss, ParticleTypes.LARGE_SMOKE, 14);
            if (c.t % 4 == 0) {
                KraveDemolition.carve(c.level, c.boss, c.boss.position(), 3.5D, 3, 1,
                        KraveDemolition.BUDGET_LIGHT);
                c.level.sendParticles(ParticleTypes.CRIT,
                        c.boss.getX(), c.boss.getY(), c.boss.getZ(), 20, 1.5D, 0.5D, 1.5D, 0.1D);
            }
            return false;
        }
        if (c.t == 78) {
            KraveGrab.release(c.boss, new Vec3(0.0D, 2.6D, 0.0D));
            c.say("UP.", ChatFormatting.GOLD);
        }
        if (c.t < 104) {
            c.fly(c.player, c.boss.position().add(0.0D, 8.0D, 0.0D), 2.6D);
            c.faceBoss(c.player);
            return false;
        }
        c.boss.setDeltaMovement(c.boss.getDeltaMovement().scale(0.6D).add(0.0D, -3.0D, 0.0D));
        c.boss.hurtMarked = true;
        c.trail(c.boss, ParticleTypes.FLAME, 14);
        c.fly(c.player, c.boss.position().add(0.0D, 4.0D, 0.0D), 2.8D);
        if (c.boss.onGround() || c.boss.getY() <= c.origin.y + 0.5D) {
            c.slam(c.boss.position(), 4);
            return true;
        }
        return false;
    }

    // ---- 5. V: Kaiden and the player, alternating ----------------------------

    private static boolean combo(KraveCinematic c) {
        c.protect(160);
        Vec3 high = c.origin.add(0.0D, 30.0D, 0.0D);

        if (c.t == 0) {
            c.say("TOGETHER.", ChatFormatting.GOLD);
            c.boss.setNoGravity(true);
        }
        // Six exchanges: he is knocked up, down, up, down between the two of
        // them, each hit swapping who is above him.
        final int perHit = 22;
        final int hits = 6;
        if (c.t < perHit * hits) {
            int hit = c.t / perHit;
            boolean caydenTurn = hit % 2 == 0;
            int phase = c.t % perHit;

            // The striker closes; the other waits on the far side.
            var striker = caydenTurn ? (net.minecraft.world.entity.Entity) c.cayden : c.player;
            var waiting = caydenTurn ? (net.minecraft.world.entity.Entity) c.player : c.cayden;
            double side = caydenTurn ? 5.0D : -5.0D;

            c.fly(striker, c.boss.position().add(0.0D, side, 0.0D), 2.2D);
            c.fly(waiting, c.boss.position().add(0.0D, -side, 0.0D), 1.2D);
            c.faceBoss(c.player);

            if (phase == perHit - 1) {
                // Contact: he is punted the other way.
                double dir = caydenTurn ? -1.0D : 1.0D;
                c.boss.setDeltaMovement(0.0D, dir * 1.9D, 0.0D);
                c.boss.hurtMarked = true;
                c.level.sendParticles(ParticleTypes.EXPLOSION,
                        c.boss.getX(), c.boss.getY() + c.boss.getBbHeight() * 0.5D, c.boss.getZ(),
                        4, 1.0D, 1.0D, 1.0D, 0.0D);
                c.shake(2);
                c.stagger(3);
            } else {
                // Held roughly in the middle so the volley stays on screen.
                c.boss.setDeltaMovement(c.boss.getDeltaMovement().scale(0.8D));
                if (c.boss.getY() < high.y - 24.0D) {
                    c.boss.setDeltaMovement(c.boss.getDeltaMovement().add(0.0D, 0.35D, 0.0D));
                }
            }
            c.trail(c.boss, ParticleTypes.CRIT, 8);
            return false;
        }

        // Kaiden throws him at the player, who charges.
        int after = c.t - perHit * hits;
        if (after == 0) {
            c.say("HE IS COMING TO YOU.", ChatFormatting.RED);
        }
        if (after < 30) {
            Vec3 toPlayer = c.player.position().subtract(c.boss.position()).normalize().scale(1.8D);
            c.boss.setDeltaMovement(toPlayer);
            c.boss.hurtMarked = true;
            c.fly(c.player, c.boss.position(), 2.6D);
            c.faceBoss(c.player);
            c.trail(c.boss, ParticleTypes.FLAME, 12);
            if (c.player.getBoundingBox().inflate(2.5D).intersects(c.boss.getBoundingBox())) {
                c.slam(c.boss.position(), 4);
                c.boss.setNoGravity(false);
                c.boss.setDeltaMovement(0.0D, -3.0D, 0.0D);
            }
            return false;
        }
        c.boss.setNoGravity(false);
        c.boss.setDeltaMovement(c.boss.getDeltaMovement().add(0.0D, -0.5D, 0.0D));
        c.trail(c.boss, ParticleTypes.FLAME, 14);
        if (c.boss.onGround() || c.boss.getY() <= c.origin.y + 0.5D) {
            c.slam(c.boss.position(), 5);
            return true;
        }
        return false;
    }

    // ---- 6. B: the end --------------------------------------------------------

    private static boolean finale(KraveCinematic c) {
        c.protect(300);
        Vec3 veryHigh = c.origin.add(0.0D, 46.0D, 0.0D);

        if (c.t == 0) {
            c.say("IT ENDS HERE.", ChatFormatting.DARK_RED);
            c.boss.setNoGravity(true);
        }

        // 1. Both of them go in at once; Kaiden connects first.
        if (c.t < 26) {
            c.fly(c.cayden, c.boss.position().add(0.0D, -2.0D, 0.0D), 2.4D);
            c.fly(c.player, c.boss.position().add(2.0D, -2.0D, 0.0D), 2.2D);
            c.faceBoss(c.player);
            return false;
        }
        if (c.t == 26) {
            c.boss.setDeltaMovement(0.0D, 2.6D, 0.0D);
            c.slam(c.boss.position(), 2);
            c.say("KAIDEN FIRST.", ChatFormatting.GOLD);
        }
        // 2. The player follows him up and hits him higher.
        if (c.t < 60) {
            c.boss.setDeltaMovement(0.0D, 1.9D, 0.0D);
            c.boss.hurtMarked = true;
            c.fly(c.player, c.boss.position().add(0.0D, -3.0D, 0.0D), 2.8D);
            c.faceBoss(c.player);
            c.trail(c.boss, ParticleTypes.CRIT, 10);
            return false;
        }
        // 3. Kaiden gets above and knocks him back down.
        if (c.t < 86) {
            c.fly(c.cayden, c.boss.position().add(0.0D, 8.0D, 0.0D), 3.0D);
            c.boss.setDeltaMovement(0.0D, 0.2D, 0.0D);
            c.fly(c.player, c.boss.position().add(0.0D, -6.0D, 0.0D), 2.4D);
            c.faceBoss(c.player);
            return false;
        }
        if (c.t == 86) {
            c.boss.setDeltaMovement(0.0D, -2.4D, 0.0D);
            c.say("BACK DOWN.", ChatFormatting.RED);
            c.shake(4);
        }
        // 4. The player catches him before he lands, and carries him up.
        if (c.t < 120) {
            c.fly(c.player, c.boss.position().add(0.0D, -2.5D, 0.0D), 3.2D);
            c.trail(c.boss, ParticleTypes.LARGE_SMOKE, 12);
            if (c.player.getBoundingBox().inflate(3.0D).intersects(c.boss.getBoundingBox())
                    && !KraveGrab.isHeld(c.boss)) {
                KraveGrab.grab(c.boss, c.player, new Vec3(0.0D, 2.2D, 0.0D));
                c.say("CAUGHT.", ChatFormatting.GOLD);
            }
            if (KraveGrab.isHeld(c.boss)) {
                KraveGrab.follow(c.boss);
            }
            return false;
        }
        if (c.t < 180) {
            KraveGrab.follow(c.boss);
            c.fly(c.player, veryHigh, 2.2D);
            c.fly(c.cayden, veryHigh.add(0.0D, 12.0D, 0.0D), 2.4D);
            c.faceBoss(c.player);
            c.trail(c.player, ParticleTypes.END_ROD, 6);
            return false;
        }
        // 5. Thrown up to Kaiden, who hammers him back down.
        if (c.t == 180) {
            KraveGrab.release(c.boss, new Vec3(0.0D, 3.0D, 0.0D));
            c.say("UP. TO HIM.", ChatFormatting.GOLD);
        }
        if (c.t < 206) {
            c.boss.setDeltaMovement(0.0D, 2.4D, 0.0D);
            c.boss.hurtMarked = true;
            c.trail(c.boss, ParticleTypes.CRIT, 10);
            return false;
        }
        if (c.t == 206) {
            c.boss.setNoGravity(false);
            c.boss.setDeltaMovement(0.0D, -4.2D, 0.0D);
            c.say("KAIDEN SENDS HIM BACK.", ChatFormatting.DARK_RED);
            c.shake(5);
        }
        // 6. The player races the fall, overtakes, and turns to meet him.
        c.boss.setDeltaMovement(c.boss.getDeltaMovement().scale(0.75D).add(0.0D, -1.4D, 0.0D));
        c.boss.hurtMarked = true;
        c.trail(c.boss, ParticleTypes.FLAME, 20);
        c.fly(c.player, c.boss.position().add(0.0D, -6.0D, 0.0D), 4.0D);
        c.faceBoss(c.player);

        if (c.boss.onGround() || c.boss.getY() <= c.origin.y + 1.0D
                || c.player.getBoundingBox().inflate(3.0D).intersects(c.boss.getBoundingBox())) {
            Vec3 at = c.boss.position();
            // Everything, at once.
            c.slam(at, 6);
            c.slam(at, 6);
            KraveAttacks.dome(c.level, at, 22.0D, ParticleTypes.FLAME);
            KraveDemolition.carveWave(c.level, c.boss, at, 20.0D, 6, 10, 3);
            c.say("THE KRAVE MONSTER FALLS.", ChatFormatting.GREEN);
            return true;
        }
        return false;
    }
}
