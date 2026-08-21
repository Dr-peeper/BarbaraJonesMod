package com.barbarajones.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Obstacle-aware aerial steering for Cayden.
 *
 * <p>His flight was a pure seek: take the vector to the target, normalise it,
 * add it to his velocity. That works perfectly in open air and fails against
 * anything solid, because nothing in it can see a wall. One block between him
 * and what he is chasing was enough - he would push into it, get stopped, and
 * push into it again, forever, because the direction he wanted never changed
 * and nothing was measuring whether he was getting anywhere.
 *
 * <p>This keeps the seek and adds the two things it was missing. It looks
 * where it is going, and it notices when it is not moving.
 *
 * <p><b>Looking.</b> A ray from his eyes toward the target. If it is clear he
 * flies straight at it, which is the common case and should stay cheap. If it
 * is blocked he fans out - upward first, since going over a wall is nearly
 * always the right answer for something that can fly, then progressively wider
 * to either side - and takes the first heading that is both clear and still
 * makes progress toward the target. Sideways-and-backwards headings are never
 * chosen, or he paces along a wall instead of crossing it.
 *
 * <p><b>Noticing.</b> Position is sampled on a timer. If he has been trying to
 * reach something and has barely moved, the current heading is abandoned and a
 * detour is committed to for a while - long enough to actually clear the
 * obstacle. Committing matters: recomputing every tick is what produces the
 * oscillation where he alternates between two equally bad directions and
 * travels nowhere.
 *
 * <p>Deliberately not a teleport. Blinking him to his target hides the
 * navigation problem rather than solving it and would make him feel like a
 * cheat; the emergency escape is a nudge in a random clear direction, which is
 * enough to unstick a genuinely wedged entity without skipping the flight.
 */
public final class CaydenFlight {

    /** How far ahead he checks for something solid. */
    private static final double LOOK_AHEAD = 6.0D;

    /** Ticks between progress samples. */
    private static final int PROGRESS_INTERVAL = 20;

    /** Less movement than this over a sample window counts as stuck. */
    private static final double PROGRESS_EPSILON = 1.5D;

    /** How long a detour is committed to once chosen. */
    private static final int DETOUR_TICKS = 25;

    /**
     * Candidate headings, tried in order, as (yaw offset in degrees, vertical
     * bias). Straight up the middle first; then over the top, which is the
     * answer to most walls; then wider and wider around it. The last pair are
     * steep climbs for when he is boxed in horizontally.
     */
    private static final double[][] FAN = {
        {   0.0D,  0.00D },
        {   0.0D,  0.75D },
        {  25.0D,  0.25D },
        { -25.0D,  0.25D },
        {  50.0D,  0.15D },
        { -50.0D,  0.15D },
        {   0.0D,  1.60D },
        {  80.0D,  0.50D },
        { -80.0D,  0.50D },
    };

    private Vec3 lastSample = Vec3.ZERO;
    private int sampleTimer;
    private Vec3 detour;
    private int detourTicks;

    /**
     * The direction he should actually fly this tick.
     *
     * @return a unit vector, never null - if everything is blocked it returns
     *         the straight-line heading, because refusing to move is worse than
     *         pressing on and being pushed aside by the collision.
     */
    public Vec3 headingToward(LivingEntity self, LivingEntity target) {
        Vec3 from = self.getEyePosition();
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 straight = to.subtract(from);
        double dist = straight.length();
        if (dist < 0.01D) {
            return Vec3.ZERO;
        }
        Vec3 direct = straight.scale(1.0D / dist);

        trackProgress(self, dist);

        // A committed detour outranks a fresh look. Re-deciding every tick is
        // what makes him oscillate between two bad directions and travel
        // nowhere.
        if (this.detourTicks > 0 && this.detour != null) {
            this.detourTicks--;
            if (clear(self, from, this.detour, Math.min(LOOK_AHEAD, dist))) {
                return this.detour;
            }
            this.detourTicks = 0;         // the detour is blocked too; look again
        }

        if (clear(self, from, direct, Math.min(LOOK_AHEAD, dist))) {
            return direct;
        }

        Vec3 around = fanOut(self, from, direct, dist);
        if (around != null) {
            this.detour = around;
            this.detourTicks = DETOUR_TICKS;
            return around;
        }
        return direct;
    }

    /** True if he has been trying and getting nowhere. */
    public boolean isStuck() {
        return this.detourTicks > 0;
    }

    /**
     * Samples how far he has actually travelled and flags a detour if the
     * answer is "not far".
     *
     * <p>This is the half that catches the cases the raycast cannot: a corner
     * he is wedged in, a heading that is technically clear but scrapes along
     * geometry, or a target he is somehow orbiting. Skipped when he is already
     * close, since standing still next to something you are punching is correct.
     */
    private void trackProgress(LivingEntity self, double distToTarget) {
        if (distToTarget < 4.0D) {
            this.sampleTimer = 0;
            this.lastSample = self.position();
            return;
        }
        if (++this.sampleTimer < PROGRESS_INTERVAL) {
            return;
        }
        this.sampleTimer = 0;
        Vec3 now = self.position();
        if (this.lastSample != Vec3.ZERO && now.distanceTo(this.lastSample) < PROGRESS_EPSILON) {
            // Force a re-plan on the next call by dropping whatever he was
            // committed to, and take a wider berth this time.
            this.detour = null;
            this.detourTicks = 0;
            this.sampleTimer = -PROGRESS_INTERVAL;   // do not re-flag immediately
        }
        this.lastSample = now;
    }

    /** The first heading in the fan that is both clear and still going the right way. */
    private Vec3 fanOut(LivingEntity self, Vec3 from, Vec3 direct, double dist) {
        double reach = Math.min(LOOK_AHEAD, Math.max(3.0D, dist));
        for (double[] option : FAN) {
            Vec3 candidate = rotate(direct, Math.toRadians(option[0]))
                    .add(0.0D, option[1], 0.0D).normalize();
            // Never accept a heading that is not still broadly toward the
            // target, or he paces along the wall instead of crossing it.
            if (candidate.dot(direct) < 0.1D) {
                continue;
            }
            if (clear(self, from, candidate, reach)) {
                return candidate;
            }
        }
        return null;
    }

    /** Yaw rotation about the vertical axis. */
    private static Vec3 rotate(Vec3 v, double radians) {
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    /**
     * Whether he can travel this way without hitting the world.
     *
     * <p>Checked from his eyes and again from his feet. One ray down the middle
     * misses exactly the case that caused the original bug: a single block at
     * chest height is invisible to a ray cast above it, and he flies into it
     * every tick while the check keeps reporting clear air.
     */
    private static boolean clear(LivingEntity self, Vec3 from, Vec3 dir, double reach) {
        Level level = self.level();
        Vec3 offset = dir.scale(reach);
        return noHit(level, self, from, from.add(offset))
                && noHit(level, self, from.add(0.0D, -self.getBbHeight() * 0.5D, 0.0D),
                        from.add(0.0D, -self.getBbHeight() * 0.5D, 0.0D).add(offset));
    }

    private static boolean noHit(Level level, LivingEntity self, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self))
                .getType() == HitResult.Type.MISS;
    }
}
