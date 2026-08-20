package com.barbarajones.v2.mobs.entity.ai;

import com.barbarajones.v2.mobs.entity.KravajoEntity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * The dive: climb, hang, then drop on the target from above.
 *
 * <p>Written as a distinct goal with its own cooldown rather than folded into a
 * melee goal, because a signature move has to be legible as a move. A flyer that
 * simply pathfinds into you and attacks whenever it happens to be in range reads
 * as a mosquito bumping around; the climb and the pause before the drop are what
 * make it read as a dive, and they are the whole reason to write this at all.
 *
 * <p>Three states, and the pause matters most:
 * <ul>
 *   <li>CLIMB - get above the target, well above head height.</li>
 *   <li>HANG - hold there for a beat. This is the telegraph, and it is also what
 *       makes the mob swattable: it is briefly stationary and within reach.</li>
 *   <li>DROP - commit, aim at where the target IS, and stop steering. A dive that
 *       keeps homing all the way down never misses, and this mob is supposed to
 *       miss four times out of five.</li>
 * </ul>
 */
public class KravajoDiveGoal extends Goal {

    private static final int CLIMB_TICKS = 22;
    private static final int HANG_TICKS = 10;
    private static final int DROP_TICKS = 26;

    /** Ticks between dives. Long enough that a swarm is a nuisance, not a blender. */
    private static final int COOLDOWN = 45;

    private static final double CLIMB_ABOVE = 5.0D;
    private static final double TRIGGER_RANGE_SQR = 20.0D * 20.0D;

    private final KravajoEntity mob;

    private int phaseTicks;
    private int cooldown;
    private int state;              // 0 climb, 1 hang, 2 drop
    private Vec3 aim = Vec3.ZERO;

    public KravajoDiveGoal(KravajoEntity mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.cooldown > 0) {
            this.cooldown--;
            return false;
        }
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive()
                && this.mob.distanceToSqr(target) < TRIGGER_RANGE_SQR;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = this.mob.getTarget();
        return target != null && target.isAlive() && this.state <= 2;
    }

    @Override
    public void start() {
        this.state = 0;
        this.phaseTicks = 0;
    }

    @Override
    public void stop() {
        this.cooldown = COOLDOWN + this.mob.getRandom().nextInt(25);
        this.state = 3;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        this.phaseTicks++;
        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);

        switch (this.state) {
            case 0 -> {   // climb above it
                this.mob.getMoveControl().setWantedPosition(
                        target.getX(), target.getY() + CLIMB_ABOVE, target.getZ(), 1.2D);
                if (this.phaseTicks > CLIMB_TICKS
                        || this.mob.getY() > target.getY() + CLIMB_ABOVE - 0.8D) {
                    this.state = 1;
                    this.phaseTicks = 0;
                }
            }
            case 1 -> {   // hang, telegraph, and be swattable
                this.mob.getMoveControl().setWantedPosition(
                        this.mob.getX(), this.mob.getY(), this.mob.getZ(), 0.1D);
                this.mob.setDeltaMovement(this.mob.getDeltaMovement().scale(0.6D));
                if (this.phaseTicks >= HANG_TICKS) {
                    // Aim once, at where it is NOW. Not re-aimed on the way down:
                    // a dive that keeps homing cannot miss, and missing is the point.
                    this.aim = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                    this.state = 2;
                    this.phaseTicks = 0;
                }
            }
            case 2 -> {   // commit
                Vec3 toward = this.aim.subtract(this.mob.position());
                double len = Math.max(0.001D, toward.length());
                this.mob.setDeltaMovement(this.mob.getDeltaMovement()
                        .add(toward.scale(0.085D / len)));
                if (this.mob.distanceToSqr(target) < 1.8D) {
                    this.mob.doHurtTarget(target);
                    this.state = 3;
                } else if (this.phaseTicks > DROP_TICKS) {
                    this.state = 3;   // whiffed entirely; climb again
                }
            }
            default -> { }
        }
    }
}
